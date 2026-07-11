# JSON 组件模块 (json-component)

Jackson 序列化定制模块，提供企业级自定义注解和 SPI 扩展，包括数据脱敏、字典翻译、字段加密、精度控制、Null 值处理等。兼容 GraalVM Native Image。

## 模块结构

```
json-component/
├── annotation/          -- 自定义 Jackson 序列化注解
│   ├── Sensitive.java         统一脱敏注解
│   ├── SensitiveType.java     脱敏类型枚举
│   ├── DictFormat.java        字典翻译注解（支持枚举/数据库）
│   ├── Encrypt.java / Decrypt.java   字段加密/解密
│   ├── DecimalFormat.java     BigDecimal 精度控制
│   ├── NullToEmpty.java       null → ""
│   ├── NullToDefault.java     null → 指定默认值
│   ├── NullToZero.java        null → 0
│   ├── PhoneDesensitize.java  手机号脱敏（保留兼容）
│   └── NameDesensitize.java   姓名脱敏（保留兼容）
├── serialize/           -- 序列化/反序列化器
│   ├── BigNumberConverter.java        JS 安全大数处理
│   ├── SensitiveSerializer.java       统一脱敏序列化器
│   ├── DictFormatSerializer.java      字典翻译序列化器
│   ├── EncryptSerializer.java / DecryptDeserializer.java
│   ├── DecimalFormatSerializer.java
│   ├── NullToEmptySerializer.java
│   ├── NullToDefaultSerializer.java
│   ├── NullToZeroSerializer.java
│   ├── PhoneDesensitizeSerializer.java
│   └── NameDesensitizeSerializer.java
├── spi/                 -- SPI 扩展接口（用户自行实现）
│   ├── IDictEnum.java             枚举翻译接口
│   ├── IDictTranslateService.java 数据库字典翻译接口
│   ├── DictServiceHolder.java     字典服务持有者
│   ├── IEncryptService.java       字段加解密接口
│   └── EncryptServiceHolder.java  加密服务持有者
└── (root)
    ├── JsonConfiguration.java     自动配置入口
    └── JacksonHelper.java         JSON 工具类
```

## 快速开始

### 引入依赖

```kotlin
// build.gradle.kts
implementation(project(":backend:components:json-component"))
```

模块自动注册，无需手动 `@Import` 或 `@EnableXxx`。

### 全局效果

- Long/BigInteger 超出 JS 安全范围时自动转为字符串
- BigDecimal 序列化为字符串（保留精度）
- LocalDateTime / LocalDate / LocalTime 格式化为 `yyyy-MM-dd HH:mm:ss` / `yyyy-MM-dd` / `HH:mm:ss`

---

## 自定义注解参考

### @Sensitive — 数据脱敏

统一脱敏注解，支持 7 种脱敏类型，替代独立的 `@PhoneDesensitize` / `@NameDesensitize`。

```java
@Sensitive(type = SensitiveType.PHONE)
private String phone;

@Sensitive(type = SensitiveType.EMAIL, placeholder = "#")
private String email;

@Sensitive(type = SensitiveType.ID_CARD)
private String idCard;
```

| 类型 | 原始值 | 脱敏后 |
|------|--------|--------|
| `PHONE` | `13812345678` | `138****5678` |
| `NAME` | `张三` | `张*` |
| `EMAIL` | `test@example.com` | `t***@example.com` |
| `ID_CARD` | `110101199001011234` | `110***********1234` |
| `BANK_CARD` | `6222021234561234` | `6222*******1234` |
| `ADDRESS` | `北京市海淀区中关村大街1号` | `北京市海淀区****` |
| `PASSWORD` | `any` | `******` |

### @DictFormat — 字典翻译

支持两种翻译来源，详见 [docs/json-dict-format.md](../../docs/json-dict-format.md)。

```java
// 数据库字典模式
@DictFormat(dictType = "user_status")
private String status;

// Java 枚举模式
@DictFormat(enumClass = StatusEnum.class)
private String status;
```

### @Encrypt / @Decrypt — 字段加解密

```java
@Encrypt
private String idCard;    // 序列化时自动加密

@Decrypt
private String idCard;    // 反序列化时自动解密
```

需注册 `IEncryptService` Bean，未注册时原样输出/输入。

### @DecimalFormat — 精度控制

```java
@DecimalFormat(scale = 2, roundingMode = "HALF_UP")
private BigDecimal amount;
```

### @NullToEmpty / @NullToDefault / @NullToZero — Null 值处理

```java
@NullToEmpty
private String name;       // null → ""

@NullToDefault("N/A")
private String remark;     // null → "N/A"

@NullToZero
private BigDecimal price;  // null → 0
```

---

## SPI 扩展

### 枚举翻译 — IDictEnum

```java
public enum StatusEnum implements IDictEnum {
    ACTIVE("1", "启用"),
    INACTIVE("0", "禁用");

    private final String code;
    private final String label;

    StatusEnum(String code, String label) { this.code = code; this.label = label; }
    @Override public String getCode() { return code; }
    @Override public String getLabel() { return label; }
}
```

`getCode()` 返回 `null` 时自动使用 `Enum.name()` 匹配。也可直接使用项目中已有的 `BaseEnum` 接口。

### 数据库字典翻译 — IDictTranslateService

```java
@Component
public class MyDictService implements IDictTranslateService {
    @Override
    public String translate(String dictType, String code) {
        // 从数据库 / 缓存查询
        return label;
    }
}
```

### 字段加解密 — IEncryptService

```java
@Component
public class MyEncryptService implements IEncryptService {
    @Override
    public String encrypt(String plainText) {
        // 加密逻辑（如 AES / SM4）
        return cipherText;
    }

    @Override
    public String decrypt(String cipherText) {
        // 解密逻辑
        return plainText;
    }
}
```

---

## GraalVM Native Image 支持

- 所有序列化器继承 `StdSerializer`，标注 `@JacksonStdImpl`，Jackson 可静态分析
- `ContextualSerializer` 在 `createContextual` 阶段预构建映射（如 `code→label`），运行时零反射
- 全局序列化器（`BigNumberConverter`、日期格式）直接注册到 `JavaTimeModule`
- SPI holder 使用 `ObjectProvider.ifAvailable` 可选注入，无实现时不会导致启动失败
- 无需额外 RuntimeHints 注册
