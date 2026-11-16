# excel-component

Excel 导入导出组件。对外只有一个入口 `ExcelTemplate`，Excel 引擎（当前是
[Apache fesod](https://github.com/apache/fesod) 的 `fesod-sheet`）只出现在 `fesod` 包里，
业务代码不接触引擎类型。

## 快速开始

组件是自动配置的，把模块放到 classpath 上即可（`admin` 模块的 `build.gradle.kts`）：

```kotlin
implementation(project(":backend:components:excel-component"))
```

### 读取

```java
@RequiredArgsConstructor
public class UserImportService {

    private final ExcelTemplate excelTemplate;

    public Either<String, Long> importUsers(InputStream input) {
        return excelTemplate.read(UserImportCmd.class)
                .from(input)
                .sheet(0)
                .execute()
                .toEither()
                .mapLeft(errors -> errors.stream()
                        .map(ExcelRowError::describe)
                        .collect(Collectors.joining("\n")))
                .map(userRepository::batchInsert);
    }
}
```

终结步骤按数据量选：

| 终结步骤 | 适用场景 | 返回 |
| --- | --- | --- |
| `execute()` | 小文件（万级以内） | `ExcelReadResult<T>`：成功行 + 失败明细 |
| `consume(Consumer<T>)` | 大文件，只要副作用 | `Try<Long>`，成功消费的行数 |
| `consumeWhile(Predicate<T>)` | 同上，但要能提前收工 | `Try<Long>`，实际消费的行数 |

要 `Flowable` 用 `RxExcel.stream(spec)`，见下方「RxJava 是可选依赖」。

`execute()` 不会因为单行校验不过就抛异常——批量导入的常态是部分成功，所以成功行与
失败明细一起交出来，由调用方决定整单驳回还是先入库好行。整份文件级别的失败
（文件损坏、密码错误、未指定数据源）才抛 `ExcelReadException`。

### record 行模型

导入支持不可变 `record`，不需要无参构造器或 setter：

```java
public record UserImportDto(
        @ExcelProperty("姓名") @NotBlank String name,
        @ExcelProperty("年龄") @Min(1) int age
) {
}

ExcelReadResult<UserImportDto> result = excelTemplate.read(UserImportDto.class)
        .from(uploadPath)
        .execute();
```

record 自动走规范构造器绑定（即使指定 `ENGINE`），支持表头别名、国际化表头、固定列索引、
`@ExcelIgnore` 和 Bean Validation。列顺序不影响构造器参数顺序；缺失的引用值为 `null`，
基本类型使用 Java 默认值。构造器抛出的异常作为坏行报告。
类型转换沿用 Spring `ConversionService`，不执行 fesod 自定义 Converter 或格式注解；
特殊日期/数值格式应注册 Spring Converter。注解端点的 record 构造器与访问器参与 AOT 注册。

### 批量导入

`consumeBatch` 攒够一批才回调一次，内存占用是「一批」而不是整个文件。批量导入的
瓶颈几乎总在下游写库，逐行 insert 一万次和五百行一批 insert 二十次差一到两个数量级：

```java
excelTemplate.read(UserImportCmd.class)
        .from(file)
        .batchSize(500)              // 不传则用 alt.excel.read.batch-size
        .maxRows(100_000)            // 不传则用 alt.excel.read.max-rows
        .consumeBatch(userService::batchCreate);
```

末批不足 `batchSize` 也会回调（空文件不回调）；每批是独立快照，下游可以安全地
异步持有。事务边界由调用方自己控制——把 `@Transactional` 标在接收批次的 service
方法上，一批一个事务。

### 错误 Excel 回写

通过一次读取的报告，在原工作簿副本上高亮错误单元格、追加错误批注，并新建错误明细表：

```java
ExcelReadResult<UserImportDto> result = excelTemplate.read(UserImportDto.class)
        .from(uploadPath)
        .sheet("用户")
        .execute();

excelTemplate.writeErrors(result.toReport("users.xlsx"))
        .from(uploadPath)
        .sheet("用户")
        .to(errorPath)
        .execute()
        .getOrElseThrow(cause -> new ExcelWriteException("错误文件生成失败", cause));
```

- 输入输出不能是同一个文件；原始值、公式、行列位置和原样式保留，高亮样式由原样式克隆。
- 错误表包含工作表、行号、列号、字段、原始值、原因和错误代码；与已有表重名时追加序号。
- 多条单元格错误合并到批注，已有批注保留。行级错误标记该行首格；无法定位行的错误只写入明细表。
- 报告必须对应一个文件的一个工作表，`.sheet(...)` 与读取时一致；跨文件合并后的报告不能用于回写。
- 使用 POI 在内存中打开原工作簿，适用于上传大小受控的反馈文件。输出沿用原工作簿格式；
  `.password(...)` 用于打开加密输入，输出不保留输入文件加密。
- 输入流执行后关闭；调用方输出流默认关闭，`.autoCloseStream(false)` 可保留。

配置与单次链式覆盖均可用：

```yaml
alt:
  excel:
    error-report:
      sheet-name: 错误明细
      highlight: true
      comments: true
```

也可贡献 `ExcelErrorWriteCustomizer` Bean，按 `@Order` 在属性默认值之后应用：

```java
@Bean
ExcelErrorWriteCustomizer errorWorkbookDefaults() {
    return spec -> spec.summarySheet("导入问题").comments(true);
}
```

单次 `.summarySheet(...)` / `.highlight(...)` / `.comments(...)` 调用拥有最终优先级。

### 导出

```java
excelTemplate.write(UserVO.class)
        .to(outputStream)
        .sheet("用户")
        .excludeColumns(List.of("password"))
        .write(users)
        .getOrElseThrow(e -> new ExcelWriteException("导出失败", e));
```

大数据量导出从游标走，按 `alt.excel.write.batch-size` 分批写，内存占用与数据量无关：

```java
excelTemplate.write(UserVO.class).to(outputStream).write(userService.cursorAll());
```

### Web 集成

`@ExcelImport` 把上传文件直接解析成方法参数：

```java
@PostMapping("/import")
public RestResponse<Void> importUsers(@ExcelImport("file") ExcelReadResult<UserImportCmd> result) {
    return userService.batchCreate(result).fold(RestResponse::fail, RestResponse::success);
}
```

`@ExcelExport` 把返回值写成附件下发，方法体里不用碰 `HttpServletResponse`：

```java
@GetMapping("/export")
@ExcelExport(fileName = "用户清单", sheetName = "用户")
public List<UserVO> exportUsers(UserQry qry) {
    return userService.list(qry);
}
```

两个注解在 **Servlet(WebMVC)** 与 **WebFlux** 两种栈上都生效，非 Web 应用里自然不生效
（`ExcelTemplate` 照常可用）。支持的形状按栈略有差别，见下方「三种 Web 形态」。

### 模板填充

模板放在启动模块的 `resources` 下，占位符 `{属性}`（单值）/ `{.属性}`（列表）：

```java
excelTemplate.fill("excel/user-template.xlsx").to(outputStream).fill(users);
```

### 动态列

列数与列名由运行期数据决定（例如按月份铺开的考核表）：实体继承
`AbstractDynamicColumn<DynamicCell>` 并照常声明固定列字段，能对上字段的列正常绑定，
其余列进 `getDynamicCells()`。

```java
@Getter @Setter
public class ScoreRow extends AbstractDynamicColumn<DynamicCell> {
    @ExcelProperty("{score.userName}")
    private String userName;
}

List<ScoreRow> rows = excelTemplate.readDynamic(ScoreRow.class).from(input).execute().rows();

// 反向导出
excelTemplate.writeHead(rows.getFirst().dynamicHead())
        .to(outputStream)
        .write(rows.stream().map(DynamicColumn::dynamicRow).toList());
```

## 三种 Web 形态

组件按运行形态自动装配，使用方不需要做任何选择：

| 形态 | 生效的自动配置 | `@ExcelImport` / `@ExcelExport` |
| --- | --- | --- |
| 非 Web 应用 | 只有 `ExcelAutoConfiguration` | 不生效（`ExcelTemplate` 照常可用） |
| Servlet / WebMVC | `+ ExcelWebAutoConfiguration`、`ExcelWebMvcAutoConfiguration` | 生效 |
| WebFlux | `+ ExcelWebAutoConfiguration`、`ExcelWebFluxAutoConfiguration` | 生效 |

`spring-webmvc`、`spring-webflux`、`jakarta.servlet-api` 全都是 `compileOnly`，
一个都不会被本模块拖进你的应用。整套集成可用 `alt.excel.web.enabled=false` 关掉。

### 支持的形状

| | Servlet | WebFlux |
| --- | --- | --- |
| `@ExcelImport List<T>` | ✅ | ✅ |
| `@ExcelImport ExcelReadResult<T>` | ✅ | ✅ |
| `@ExcelImport Flux<T>` | — | ✅ |
| `@ExcelImport Flowable<T>` | ✅（需 rxjava） | ✅（需 rxjava） |
| `@ExcelExport Collection<T>` / `Iterator<T>` | ✅ | ✅ |
| `@ExcelExport Flux<T>` / `Mono<Collection<T>>` | — | ✅ |
| `@ExcelExport Flowable<T>` | ✅（需 rxjava） | ✅（需 rxjava） |

### 两栈的实现差别

- **返回值抢占**。MVC 里 `List<T>` 会被 `RequestResponseBodyMethodProcessor` 先接走，
  所以 `ExcelExportReturnValueHandler` 必须被插到 `RequestMappingHandlerAdapter`
  处理器列表的 0 号位（`WebMvcConfigurer#addReturnValueHandlers` 是追加到内置之后的，
  拿不到）。WebFlux 里 `ExcelExportResultHandler` 实现 `Ordered` 取 0 即可抢在
  `ResponseBodyResultHandler`（order 100）之前。
- **不阻塞事件循环**。Excel 读写是阻塞动作，WebFlux 集成把它们全部放到
  `Schedulers.boundedElastic()` 上。
- **导出走临时文件**。WebFlux 下先在 `boundedElastic` 上把工作簿写到临时文件，
  `Flux` / `Flowable` 通过预取 64 行的游标分批写出，不收集完整列表；集合也遵守分批与分 sheet 上限。
  再用 `DataBufferUtils.read` 分块推给响应，完成、失败或取消时关闭游标并删除临时文件。
  临时目录同样使用 `alt.excel.web.temp-dir`。
- **上传落盘**。两栈的流式形状都先把上传落到临时文件——原始存储在请求结束时就被
  容器回收，而流是懒执行的。目录用 `alt.excel.web.temp-dir` 配。

WebFlux 集成层用的是 Reactor 而不是 RxJava：WebFlux 的扩展点签名本身就是
`Mono`/`Flux`，属于「框架强加的 Reactor 留在框架层」，业务代码拿到的仍是集合或
自己选的流类型。

## RxJava 是可选依赖

核心 SPI（`ExcelReadSpec` / `ExcelWriteSpec`）的签名里**没有任何响应式类型**。
原因是硬性的：`Flowable` 一旦出现在接口签名上，没引 rxjava 的应用只要反射枚举
实现类的方法（Spring、AOT、Jackson 都会做）就会抛 `NoClassDefFoundError`。

不引 rxjava 时组件完全可用（有 `FilteredClassLoader` 测试钉住）：读写、`consume`、
`consumeWhile`、`write(Iterator)`、`@ExcelImport` 的 `List<T>` / `ExcelReadResult<T>`
形状、`@ExcelExport` 的 `Collection<T>` 形状全部正常。只有响应式形状会给出
「请引入 rxjava」的明确报错。

要用响应式，自己声明依赖：

```kotlin
implementation("io.reactivex.rxjava3:rxjava")
```

然后经 `RxExcel` 使用：

```java
// 读：逐行下发，下游取消即停止解析剩余行
Flowable<UserImportCmd> rows = RxExcel.stream(
        excelTemplate.read(UserImportCmd.class).from(inputStream));

// 写：按 batch-size 分批阻塞拉取，阻塞发生在链路最外层的终结步骤
RxExcel.write(excelTemplate.write(UserVO.class).to(outputStream), userService.streamAll());
```

流式读取在 IO 线程等待下游需求，最多额外持有一条未请求的行；取消订阅会唤醒等待并结束读取。
写出侧在成功、失败时均释放响应式游标，避免上游继续生产。

Web 层的 `Flowable` 形状（两种栈都支持）经 `ExcelReactiveSupport` 适配：有 rxjava
时自动装 `RxJavaExcelReactiveSupport`，没有则装 `NoOpExcelReactiveSupport`，
两者按 classpath 严格二选一。WebFlux 下即使不引 rxjava，也还有 Reactor 的
`Flux`/`Mono` 形状可用。

## 校验与错误处理

### 逐行 Bean Validation

实体上标 `jakarta.validation` 约束即可，读取时逐行校验：

```java
public class UserImportCmd {
    @NotBlank(message = "姓名不能为空")
    @ExcelProperty("姓名")
    private String userName;

    @Min(value = 1, message = "年龄必须大于 0")
    @ExcelProperty("年龄")
    private Integer age;
}
```

`jakarta.validation-api` 是 `compileOnly`，`ExcelRowValidator` 只在 classpath 上有
Bean Validation 且容器里确实有 `Validator` 时才装配；没有的话读写照常工作，
只是不做校验（有测试钉住这条降级路径）。

支持校验分组，同一个实体在「新增导入」和「更新导入」下可以有不同的必填项：

```java
excelTemplate.read(UserImportCmd.class).from(file)
        .validationGroups(OnCreate.class)   // 指定分组即隐式打开校验
        .execute();
```

注解上同样可用：`@ExcelImport(validationGroups = OnCreate.class)`。

### 三种坏行策略

`onError` / `alt.excel.read.on-error` / `@ExcelImport(onError = ...)`：

| 策略 | 读到哪 | 好行 | 适用 |
| --- | --- | --- | --- |
| `SKIP_ROW`（默认） | 整个文件 | 照常返回 | 部分成功，让用户订正坏行后补传 |
| `FAIL_FAST` | **首个坏行即停** | 已解析行保留在结果中，文件判为失败 | 尽早停止并返回首个错误 |
| `COLLECT_ALL` | 整个文件（受读取上限约束） | 已解析行保留在结果中，存在错误时文件判为失败 | 一次列出所有问题 |

后两者在有坏行时标记文件校验失败。`execute()` 对坏行返回错误明细；文件损坏、IO 失败等仍抛异常。
校验结局在 `result.isAborted()` 里，调用方应检查它再执行写库：

```java
ExcelReadResult<UserImportCmd> result = excelTemplate.read(UserImportCmd.class)
        .from(file)
        .onError(ExcelErrorPolicy.COLLECT_ALL)
        .execute();

return result.toEither("users.xlsx").fold(this::renderReport, userService::batchCreate);
```

`consume` / `consumeWhile` / `consumeBatch` 及流式导入采用以下语义：

- `COLLECT_ALL` 先预检整个文件，通过后再重读并触发业务回调。坏文件不触发回调；输入流会暂存到临时文件，用完删除，内存不缓存整份数据。
- `SKIP_ROW` 和 `FAIL_FAST` 边读边消费，已经执行的回调不会撤销。
- 业务回调异常立即返回失败，不按坏行跳过。组件不控制数据库事务，第二阶段的业务失败也不承诺自动回滚。

`maxErrors` 限制明细条数（默认 1000，防坏文件刷爆内存），触顶会在
`summary().truncated()` 上打标记。

### 给前端的错误报告

每个违反的约束是**一条**明细，不是把一行拼成一句话——前端要按字段高亮单元格
就必须拿到结构化数据：

| 字段 | 用途 |
| --- | --- |
| `rowNumber` / `columnNumber` | 定位单元格（Excel 里肉眼看到的编号，表头是第 1 行） |
| `header` | 显示列名 |
| `field` | 对应表单字段，前端好联动 |
| `rejectedValue` | 回显用户到底填了什么 |
| `code` | 约束注解简单名（`NotBlank`/`Min`/`PARSE`），按类型分类提示 |
| `message` | 兜底文案 |

`result.toReport(fileName)` 汇总成 `ExcelErrorReport`，同一份数据给了三种形态：

- `errors()` —— 平铺明细，按行号列号排序（顺序稳定，表格刷新不跳），渲染成错误表
- `rows()` —— 按行号分组，用于「展开某行看全部问题」和预览表格里加红框
- `codes()` —— 各约束码出现次数，用于「3 处缺必填项、2 处格式错误」这类分类摘要
- `summary()` —— 总行数 / 出错行数 / 有效行数 / 是否截断 / 是否整单驳回
- `headline()` —— 解析/校验摘要，不表示已入库或已回滚

Web 层的形状决定错误怎么传出去：

| 参数形状 | 整单驳回时 |
| --- | --- |
| `ExcelReadResult<T>` | 不抛，明细在结果里，方法自己决定 |
| `List<T>` / `Flux<T>` / `Flowable<T>` | 抛 `ExcelValidationException`，`getReport()` 拿报告 |

建议在应用的 `@RestControllerAdvice` 里把 `ExcelValidationException` 渲染成
RFC 9457 ProblemDetail，把报告放进扩展字段。

## 表头国际化

`@ExcelProperty("{user.name}")` 里的 `{key}` 会在**导出**时替换成当前 Locale 的文本
（`alt.excel.write.i18n-head`，默认开）。**导入**方向默认关闭
（`alt.excel.read.i18n-head`），走 fesod 原生的字面量匹配，自定义 `Converter` 全部生效。

> **两端要成对开启。** 导出开着 i18n 表头，落盘的是「姓名」；此时导入端若不开，
> fesod 会拿 `{user.name}` 去字面量匹配「姓名」，一列都对不上、读出来全是空对象。
> 「导出模板给用户填、再导回来」的场景请把读取端也打开：
> `.i18nHead(true)` 或 `alt.excel.read.i18n-head=true`。

开启导入端国际化后，绑定改走 Spring `ConversionService`（按解析后的表头文本定位列），
代价是不走 fesod 的自定义 `Converter`。

消息键写不带花括号的形式：

```properties
# excel.properties 或业务模块自己的 basename
user.name=姓名
```

## 配置

前缀 `alt.excel`（旧版本是 `fast.excel`，且直接绑在 fesod 的 `GlobalConfiguration` 上，
没有配置元数据）。

全部配置项都有 `spring-boot-configuration-processor` 生成的元数据，IDE 里有补全和文档。

### 全局

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `alt.excel.enabled` | `true` | 组件总开关，关掉连 `ExcelTemplate` 都不装配 |
| `alt.excel.auto-trim` | `true` | 单元格文本去首尾空白 |
| `alt.excel.use1904windowing` | `false` | 按 1904 日期系统解释日期（Mac 版旧行为） |
| `alt.excel.use-scientific-format` | `false` | 读数字时保留科学计数法原样 |
| `alt.excel.locale` | JVM 默认 | 默认 Locale |
| `alt.excel.field-cache-location` | `THREAD_LOCAL` | 字段元数据缓存位置 |
| `alt.excel.binding` | `AUTO` | 实体绑定方式，见下方「Spring Native」|

### 读取

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `alt.excel.read.head-row-number` | `1` | 表头行数 |
| `alt.excel.read.validate` | `true` | 逐行 Bean Validation |
| `alt.excel.read.on-error` | `SKIP_ROW` | 坏行策略：`SKIP_ROW` 部分成功 / `FAIL_FAST` 马上中断 / `COLLECT_ALL` 收齐所有错误再整单驳回 |
| `alt.excel.read.max-errors` | `1000` | 失败明细上限，到顶即停止解析并标记截断 |
| `alt.excel.read.batch-size` | `500` | **批量导入每批条数**，`consumeBatch` 攒够这么多才回调一次 |
| `alt.excel.read.max-rows` | `-1` | 单次读取行数上限，`-1` 不限；防超大文件打爆内存和数据库 |
| `alt.excel.read.i18n-head` | `false` | 按国际化文本匹配表头 |
| `alt.excel.read.ignore-empty-row` | `true` | 忽略全空行 |

### 写出

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `alt.excel.write.auto-width` | `true` | 列宽自适应 |
| `alt.excel.write.i18n-head` | `true` | 表头国际化替换 |
| `alt.excel.write.in-memory` | `false` | 全内存生成，大数据量务必保持 `false` |
| `alt.excel.write.batch-size` | `2000` | 集合及游标写出的分批大小 |
| `alt.excel.write.max-rows-per-sheet` | `1000000` | 单 sheet 行数上限，超出自动开新 sheet；`-1` 不分 |
| `alt.excel.write.file-name-timestamp-pattern` | `yyyyMMddHHmmss` | 下载文件名的时间戳格式 |

### Web

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `alt.excel.web.enabled` | `true` | `@ExcelImport` / `@ExcelExport` 的 Web 集成（两栈共用） |
| `alt.excel.web.temp-dir` | 系统临时目录 | 流式上传落盘的目录 |

### 三层覆盖

`@ExcelImport.validate` 采用三态开关：`-1` 继承配置，`0` 关闭，`1` 开启。
旧调用中的 `validate = false/true` 需改为 `validate = 0/1`。
导入、导出注解统一写 `i18nHead = false/true`，不填写时继承各自的全局配置。
`i18nHead` 内部用空数组表示未指定，显式设置只接受一个布尔值；旧的 `0/1` 写法需改为 `false/true`。
`onError` 未指定时继承配置；显式覆盖仍写 `onError = ExcelErrorPolicy.SKIP_ROW`，只能指定一种策略。
`validationGroups` 非空时仍隐式开启校验。

每个行为都能在三个层次上表达，越靠内层优先级越高：

```java
// 1. 配置文件给全局默认：alt.excel.read.batch-size=500
// 2. 注解按接口覆盖
@ExcelImport(value = "file", onError = ExcelErrorPolicy.COLLECT_ALL)
// 3. 链式方法按次覆盖
excelTemplate.read(UserImportCmd.class).from(file).batchSize(1000).maxRows(50_000)
```

## Spring Native / GraalVM

### 一句话结论

组件默认配置（`alt.excel.binding=AUTO`）在 JVM 和 native image 下都能用，**不需要改代码**。
AUTO 在 native image 里自动切到 `REFLECTIVE` 绑定。

### 为什么需要切绑定方式

fesod 的实体绑定路径用 cglib **在运行期生成字节码**：

| fesod 路径 | 底层机制 | native |
| --- | --- | --- |
| `ModelBuildEventListener#buildUserModel`（读实体） | `BeanMap.Generator.create()` | ❌ |
| `ExcelWriteAddExecutor#addJavaObjectToExcel`（写实体） | 同上 | ❌ |
| `ExcelWriteFillExecutor`（模板填充） | 同上 | ❌ |
| `buildNoModel`（无模型读，返回 `Map<列下标, 字符串>`） | 纯 Map | ✅ |
| 行是 `Collection` / `Map` 的写出（`CollectionRowData`） | 纯集合 | ✅ |

GraalVM 不支持运行期生成字节码，**这不是缺 reflection hints，注册多少 hints 都无解**。
所以本组件在 fesod 的无模型路径之上自建了一层反射绑定
（`ExcelRowAccessor` / `ExcelRowBinder`），native 下走这条路。

### 三种绑定方式

| `alt.excel.binding` | 行为 | 适用 |
| --- | --- | --- |
| `AUTO`（默认） | native image 里用 `REFLECTIVE`，JVM 里用 `ENGINE` | 一般情况 |
| `ENGINE` | fesod 自己绑定。更快，`@ExcelProperty(converter=)`、`@DateTimeFormat`、`@NumberFormat` 全部生效 | 只跑 JVM，且依赖自定义 `Converter` |
| `REFLECTIVE` | 组件自建反射绑定，值转换走 Spring `ConversionService` | native；或想在 JVM 上先验证 native 行为 |

也能在链上逐次覆盖：`.binding(ExcelBindingMode.ENGINE)`。

**`REFLECTIVE` 的限制**：不支持 fesod 的自定义 `Converter` 与 `@DateTimeFormat` /
`@NumberFormat`；表头国际化、列筛选（`includeColumns` / `excludeColumns`）、列顺序
（`@ExcelProperty` 的 `index` / `order`）、逐行校验都照常工作，两种绑定产出的文件
可以互相读写（有测试钉住）。**模板填充（`fill`）只有 cglib 一条路，native 下不可用。**

### 反射用量

- 扫字段、读注解、找 getter/setter 只在每个行类型**首次**使用时做一次，
  连同 `Method` 句柄缓存在 `ExcelRowAccessor` 里；逐行读写是直接的
  `Method#invoke`，没有任何查找。
- `@ExcelImport` / `@ExcelExport` 的参数与返回值泛型解析（`ResolvableType`）
  按 handler method 缓存，不是每请求解析。

### 可达性注册

两部分，都是自动的：

1. **组件自身**：`ExcelRuntimeHints`（`@ImportRuntimeHints` 挂在
   `ExcelAutoConfiguration` 上）——`DynamicCell`、写处理器、POI 的
   `WorkbookFactory`、`excel*.properties` 资源。
2. **业务行类型**：`ExcelModelAotProcessor`（`META-INF/spring/aot.factories`
   注册的 `BeanFactoryInitializationAotProcessor`）在构建期扫所有 bean 的方法，
   从 `@ExcelExport` 的返回值泛型、`@ExcelImport` 的参数泛型反推行类型，
   用 Spring 的 `BindingReflectionHintsRegistrar` 登记（`@ExcelExport(type=)`
   显式声明时优先用它）。

**覆盖不到的场景**：不经过注解、直接调 `excelTemplate.read(Xxx.class)` 的行类型
扫不出来。给那个类加 `@RegisterReflectionForBinding(Xxx.class)`，或在自己模块的
`RuntimeHintsRegistrar` 里登记：

```java
@Configuration
@RegisterReflectionForBinding({UserImportCmd.class, ScoreRow.class})
public class MyExcelHints {
}
```

构建后可以在 `build/generated/aotSources/.../*.json` 里核对登记结果，
`ExcelModelAotProcessor` 也会在构建日志里打出扫到的行类型。

## 扩展：customizer DSL

需要摸底层 fesod builder 时（注册自定义 `Converter`、统一样式、冻结首行……），
声明 `ExcelReadCustomizer` / `ExcelWriteCustomizer` bean 即可，作用于所有读写操作。
组件用 `ObjectProvider.orderedStream()` 收集：容忍零贡献者、尊重 `@Order`，
**贡献者先应用、组件默认值最后应用**。

```java
@Bean
@Order(0)
ExcelWriteCustomizer frozenHeaderCustomizer() {
    return builder -> builder.registerWriteHandler(new FrozenHeaderHandler());
}
```

链式配置面（`ExcelReadSpec` / `ExcelWriteSpec`）本身保持引擎无关，不暴露 fesod 类型；
customizer 是唯一的逃生舱。

## 线程与上下文

- `ExcelTemplate` 是无状态单例，可并发使用；每次调用现场造一段链，可变配置只落在那段链上。
- 一段链只服务一次操作：终结步骤消费掉数据源后不要复用（输入流读完即废）。
- `RxExcel.stream(spec)` 在 `Schedulers.io()` 上恢复创建读取链时的上下文，解析和同步下游回调完成、失败或取消后还原工作线程原状态。
- 自动传播可用的租户 ID、数据库、schema、完整 `TenantRouting`、已有 `Authentication` 及语言/时区。
  `UserContextHolder` 与 `SecurityContextHolder` 读取同一个恢复后的认证；每次恢复创建独立的 `SecurityContext`。
- WebFlux 注解集成还会读取 `ReactiveSecurityContextHolder`，在解析和写出边界恢复认证；租户及自定义贡献者在请求入口捕获。
- `alt.excel.context.enabled=false` 可关闭 Spring 管理的传播。租户和 Spring Security API 是可选依赖，不会由 Excel 组件强制引入。
- 流式上传会先落临时文件再解析——multipart 的原始存储在请求结束时就被容器回收了，
  懒订阅拿不到；临时文件在流终结（完成、出错、取消）时删除。
- WebFlux 集成不在事件循环上做任何阻塞动作，解析与写出都在 `boundedElastic` 上。

### 自定义上下文与额外调度切换

贡献 `ExcelContextHandler` Bean 即可参与 `ExcelContextTemplate` 捕获，按 `@Order` 恢复、逆序还原。
`capture()` 在调用线程执行，返回可重复恢复的快照；`restore()` 返回的作用域必须在同一线程关闭，
负责还原之前的值，不能仅清空，也不能在 `close()` 抛异常。

组件不安装全局调度 hook。业务自己增加的 `observeOn` / `publishOn` 或异步数据源，应在实际使用上下文的边界显式恢复：

```java
final ExcelContextHandler.Snapshot snapshot = excelTemplate.captureContext();

// RxJava：在新增的 observeOn 后恢复下游回调上下文。
RxExcel.withContext(RxExcel.stream(spec).observeOn(Schedulers.io()), snapshot)
        .doOnNext(userService::create)
        .subscribe();

// WebFlux：在业务自行调度后的处理函数内恢复。
rows.publishOn(reactor.core.scheduler.Schedulers.boundedElastic())
        .map(row -> snapshot.call(() -> userService.create(row)));

// 普通任务：交给已有线程池执行，不更改全局线程池配置。
Runnable task = snapshot.decorate(() -> userService.importUsers(command));
```

`RxExcel.stream(factory)` 是非 Spring 静态入口，在调用时捕获内置上下文。
需要遵守 Spring 配置开关或使用自定义贡献者时，使用 `RxExcel.stream(factory, excelTemplate.captureContext())`。
快照仅供进程内执行，不序列化认证，也不从 Excel 内容构造租户或用户身份。

## 本次重写去掉了什么

旧版本（46 个类）存在下列问题，均已在重写中消除：

- 缺 `META-INF/spring/...AutoConfiguration.imports`，两个 `@AutoConfiguration` **从未被加载**，整个模块是死代码。
- `ExcelTemplate` 24 个重载里 `dynamicRead*` / `dynamicWrite*` 全部原样委派给 `read*` / `write*`；接口 294 行、实现 507 行，双双超尺寸硬上限。
- `ExcelHelper` / `RxjavaExcelHelper` / `ValidaHelper` 用 `BeanFactoryPostProcessor` + `getBean()` 往 static 字段塞单例。
- `exportDynamicExcel` 算完表头就丢掉、`.sheet()` 后没有 `doWrite`，实际什么都不写；`ImportExcelHelper.importExcel` 直接 `return null`；`ExcelHandlerManger`（拼写错误）三个方法全是空壳。
- `@ExcelExport` 没有任何 `HandlerMethodReturnValueHandler`，导出功能完全不存在。
- 只有 Servlet 集成，WebFlux 应用里两个注解静默失效；且 `jakarta.servlet-api` 是 `api` 依赖，会被拖进所有下游模块（包括非 Web 的）。
- `@AliasFor` 用在非 Spring 元注解上，别名不生效。
- `Flowable.create` 没有调度器声明，且读的是请求结束即关闭的 multipart 流；`merge` 分支在参数解析器里 `blockingStream()`。rxjava 还是硬依赖，且 `Flowable` 出现在核心接口签名上，没引它的应用会在反射枚举方法时炸。
- `DynamicValidatorManagerImpl` 对注解类型调 `BeanUtils.getResolvableConstructor` —— 注解没有构造器，必抛异常；`DynamicAttributeService` 全仓无实现 bean。整个动态校验子系统不可用，已整体删除（动态列的读写保留）。
- `AbstractReadListener` 复制了近 200 行 fesod 内部的模型构建逻辑（含 cglib `BeanMap`），只为支持国际化表头匹配；现改为 `ExcelRowAccessor` + `ExcelRowBinder`，反射元数据按类型缓存一次，且是 native 下唯一可用的绑定路径。
