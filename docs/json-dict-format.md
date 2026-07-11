# @DictFormat 使用指南

`@DictFormat` 是 `json-component` 提供的 Jackson 序列化注解，用于在 JSON 输出时将编码（code）自动翻译为展示文本（label）。

## 一、Java 枚举翻译模式

### 1.1 实现 `IDictEnum` 接口（推荐，code 可为字符串）

```java
public enum StatusEnum implements IDictEnum {
    ACTIVE("1", "启用"),
    INACTIVE("0", "禁用");

    private final String code;
    private final String label;

    StatusEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    @Override
    public String getCode() { return code; }
    @Override
    public String getLabel() { return label; }
}
```

字段上使用：

```java
@DictFormat(enumClass = StatusEnum.class)
private String status;  // 存储值 "1" → 输出 "启用"
```

### 1.2 使用 `Enum.name()` 匹配（不实现 `getCode()`）

```java
public enum ColorEnum implements IDictEnum {
    RED("红色"),
    GREEN("绿色");

    private final String label;

    ColorEnum(String label) { this.label = label; }

    @Override
    public String getLabel() { return label; }
    // getCode() 返回 null，自动使用 name() → "RED"/"GREEN"
}
```

字段上使用：

```java
@DictFormat(enumClass = ColorEnum.class)
private String color;  // 存储值 "RED" → 输出 "红色"
```

### 1.3 使用 `BaseEnum` 接口（code 为 Integer）

如果项目中已有的枚举已实现 `BaseEnum`，可直接复用：

```java
// 假设已有枚举
public enum LevelEnum implements BaseEnum {
    VIP(1, "VIP 用户"),
    NORMAL(2, "普通用户");

    @Override public Integer getCode() { return code; }
    @Override public String getLabel() { return label; }
}

// 字段上直接使用
@DictFormat(enumClass = LevelEnum.class)
private Integer level;  // 存储值 1 → 输出 "VIP 用户"
```

> `code → label` 映射在应用启动时预构建，运行时无反射调用。

---

## 二、数据库字典翻译模式

### 2.1 实现 `IDictTranslateService`

```java
@Component
public class MyDictService implements IDictTranslateService {

    @Autowired
    private DictMapper dictMapper;

    @Override
    public String translate(String dictType, String code) {
        return dictMapper.selectLabel(dictType, code);
    }
}
```

### 2.2 字段上使用

```java
@DictFormat(dictType = "user_status")
private String status;  // 存储值 "1" → 输出 "启用"
```

> 替代方案：也可以注入 Redis 缓存加速字典查询。

---

## 三、优先级规则

1. `enumClass` 指定了有效的枚举类 → 使用枚举映射，忽略 `dictType`
2. `dictType` 不为空且 `enumClass` 为 `NoDictEnum` → 使用 `IDictTranslateService`
3. 无匹配 → 输出原始值

---

## 四、示例：实体类中的综合使用

```java
public class UserVO {

    @DictFormat(enumClass = UserStatusEnum.class)
    private String status;

    @DictFormat(dictType = "dept_name")
    private String deptCode;

    @Sensitive(type = SensitiveType.PHONE)
    private String phone;

    private String name;
}
```

序列化结果：

```json
{
  "status": "启用",
  "deptCode": "研发部",
  "phone": "138****5678",
  "name": "张三"
}
```

---

## 五、注意事项

- `enumClass` 和 `dictType` 支持同时不指定，此时原样输出
- `IDictTranslateService` 未注册时，数据库字典模式回退为输出原始值
- `getCode()` 返回 `null` 时使用枚举常量的 `name()` 作为匹配 key
- `code → label` 映射在 `createContextual` 时预构建，运行时零反射，兼容 GraalVM Native Image
