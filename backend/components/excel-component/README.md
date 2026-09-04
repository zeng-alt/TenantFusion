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
  再用 `DataBufferUtils.read` 分块推给响应（内存占用与文件大小无关），
  响应终结时删文件。直接往响应的 `DataBuffer` 上挤既会阻塞事件循环、也拿不到背压。
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

Web 层的 `Flowable` 形状（两种栈都支持）经 `ExcelReactiveSupport` 适配：有 rxjava
时自动装 `RxJavaExcelReactiveSupport`，没有则装 `NoOpExcelReactiveSupport`，
两者按 classpath 严格二选一。WebFlux 下即使不引 rxjava，也还有 Reactor 的
`Flux`/`Mono` 形状可用。

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

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `alt.excel.auto-trim` | `true` | 单元格文本去首尾空白 |
| `alt.excel.locale` | JVM 默认 | 默认 Locale |
| `alt.excel.field-cache-location` | `THREAD_LOCAL` | 字段元数据缓存位置 |
| `alt.excel.binding` | `AUTO` | 实体绑定方式，见下方「Spring Native」|
| `alt.excel.read.head-row-number` | `1` | 表头行数 |
| `alt.excel.read.validate` | `true` | 逐行 Bean Validation |
| `alt.excel.read.skip-invalid-rows` | `true` | 坏行跳过并记账；`false` 则首个坏行即停止 |
| `alt.excel.read.max-errors` | `1000` | 失败明细上限，到顶即停止解析 |
| `alt.excel.read.i18n-head` | `false` | 按国际化文本匹配表头 |
| `alt.excel.write.auto-width` | `true` | 列宽自适应 |
| `alt.excel.write.i18n-head` | `true` | 表头国际化替换 |
| `alt.excel.write.in-memory` | `false` | 全内存生成，大数据量务必保持 `false` |
| `alt.excel.write.batch-size` | `2000` | 从 `Flowable` 写出时的分批大小 |
| `alt.excel.web.enabled` | `true` | `@ExcelImport` / `@ExcelExport` 的 Web 集成（两栈共用） |
| `alt.excel.web.temp-dir` | 系统临时目录 | 流式上传落盘的目录 |

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
- `RxExcel.stream(...)` 默认在 `Schedulers.io()` 上解析。**ThreadLocal 上下文
  （`SecurityContext`、租户上下文）不会跨调度器传递**，需要的值请在订阅前取出。
- 流式上传会先落临时文件再解析——multipart 的原始存储在请求结束时就被容器回收了，
  懒订阅拿不到；临时文件在流终结（完成、出错、取消）时删除。
- WebFlux 集成不在事件循环上做任何阻塞动作，解析与写出都在 `boundedElastic` 上。

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
