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

三个终结步骤按数据量选：

| 终结步骤 | 适用场景 | 返回 |
| --- | --- | --- |
| `execute()` | 小文件（万级以内） | `ExcelReadResult<T>`：成功行 + 失败明细 |
| `stream()` | 大文件，逐行下发 | `Flowable<T>`，默认跑在 `Schedulers.io()` |
| `consume(Consumer<T>)` | 大文件，只要副作用 | `Try<Long>`，成功消费的行数 |

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

大数据量导出从 `Flowable` 走，按 `alt.excel.write.batch-size` 分批写，内存占用与
数据量无关：

```java
excelTemplate.write(UserVO.class).to(outputStream).write(userService.streamAll());
```

### Web 集成

`@ExcelImport` 把上传文件直接解析成方法参数，支持 `List<T>`、`ExcelReadResult<T>`、
`Flowable<T>` 三种形状：

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
| `alt.excel.read.head-row-number` | `1` | 表头行数 |
| `alt.excel.read.validate` | `true` | 逐行 Bean Validation |
| `alt.excel.read.skip-invalid-rows` | `true` | 坏行跳过并记账；`false` 则首个坏行即停止 |
| `alt.excel.read.max-errors` | `1000` | 失败明细上限，到顶即停止解析 |
| `alt.excel.read.i18n-head` | `false` | 按国际化文本匹配表头 |
| `alt.excel.write.auto-width` | `true` | 列宽自适应 |
| `alt.excel.write.i18n-head` | `true` | 表头国际化替换 |
| `alt.excel.write.in-memory` | `false` | 全内存生成，大数据量务必保持 `false` |
| `alt.excel.write.batch-size` | `2000` | 从 `Flowable` 写出时的分批大小 |
| `alt.excel.web.enabled` | `true` | `@ExcelImport` / `@ExcelExport` 的 MVC 集成 |
| `alt.excel.web.temp-dir` | 系统临时目录 | `Flowable` 形状上传落盘的目录 |

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
- `stream()` 默认在 `Schedulers.io()` 上解析。**ThreadLocal 上下文
  （`SecurityContext`、租户上下文）不会跨调度器传递**，需要的值请在订阅前取出。
- `Flowable` 形状的上传会先落临时文件再解析——multipart 的原始存储在请求结束时就被
  容器回收了，懒订阅拿不到；临时文件在流终结（完成、出错、取消）时删除。

## 本次重写去掉了什么

旧版本（46 个类）存在下列问题，均已在重写中消除：

- 缺 `META-INF/spring/...AutoConfiguration.imports`，两个 `@AutoConfiguration` **从未被加载**，整个模块是死代码。
- `ExcelTemplate` 24 个重载里 `dynamicRead*` / `dynamicWrite*` 全部原样委派给 `read*` / `write*`；接口 294 行、实现 507 行，双双超尺寸硬上限。
- `ExcelHelper` / `RxjavaExcelHelper` / `ValidaHelper` 用 `BeanFactoryPostProcessor` + `getBean()` 往 static 字段塞单例。
- `exportDynamicExcel` 算完表头就丢掉、`.sheet()` 后没有 `doWrite`，实际什么都不写；`ImportExcelHelper.importExcel` 直接 `return null`；`ExcelHandlerManger`（拼写错误）三个方法全是空壳。
- `@ExcelExport` 没有任何 `HandlerMethodReturnValueHandler`，导出功能完全不存在。
- `@AliasFor` 用在非 Spring 元注解上，别名不生效。
- `Flowable.create` 没有调度器声明，且读的是请求结束即关闭的 multipart 流；`merge` 分支在参数解析器里 `blockingStream()`。
- `DynamicValidatorManagerImpl` 对注解类型调 `BeanUtils.getResolvableConstructor` —— 注解没有构造器，必抛异常；`DynamicAttributeService` 全仓无实现 bean。整个动态校验子系统不可用，已整体删除（动态列的读写保留）。
- `AbstractReadListener` 复制了近 200 行 fesod 内部的模型构建逻辑，只为支持国际化表头匹配；现改为可选的 `i18nHead` 路径，用 Spring `ConversionService` 绑定，不再依赖引擎内部。
