# OSS 组件模块

统一对象存储服务组件，支持多种存储后端：**本地文件系统**、**MinIO**、**AWS S3**、**阿里云 OSS**、**腾讯云 COS**、**华为云 OBS**。提供**文件上传下载**、**分片上传与断点续传**、**图片缩略图生成**、**自动桶策略**等能力。

## 模块结构

```
oss-component/
├── api-oss-component/          -- API 接口层（接口、DTO、配置属性）
├── core-oss-component/         -- 核心实现层
│   ├── core/                   --   公共基础（连接管理、桶策略、缩略图）
│   │   ├── s3/                 --   S3 协议实现（S3OssTemplate）
│   │   └── local/              --   本地文件系统实现（FileSystemOssTemplate）
├── jpa-oss-component/          -- JPA 持久化层（文件记录管理、MD5 去重）
└── jdbc-oss-component/         -- JDBC 持久化层（JPA 轻量替代）
```

### 模块依赖关系

```
业务代码
    │
    ├── jpa-oss-component ──┬── core-oss-component ──┬── api-oss-component ──┬── api-component
    │                        │                        │
    └── jdbc-oss-component ──┘                        └── AWS SDK (S3)
```

> **提示**: 只需引入 `jpa-oss-component` 或 `jdbc-oss-component` 一个实现模块即可，二者选一。

---

## 快速集成

### 1. 添加依赖

```kotlin
// backend/admin/build.gradle.kts
dependencies {
    // JPA 版本（推荐，带文件记录管理 + MD5 去重 + 缩略图）
    implementation(project(":backend:components:oss-component:jpa-oss-component"))

    // 或 JDBC 版本（轻量，无 JPA）
    // implementation(project(":backend:components:oss-component:jdbc-oss-component"))
}
```

### 2. 基础配置

```yaml
# application.yml
oss:
  s3:
    enabled: true
    endpoint: http://localhost:9000           # MinIO / S3 服务地址
    access-key: your-access-key
    secret-key: your-secret-key
    bucket-name: my-bucket                     # 默认桶名
    region: us-east-1
    path-style-access: true                    # MinIO 等需 true，AWS S3 为 false
    auto-create-bucket: true                   # 启动时自动创建默认桶
```

### 3. 快速开始

```java
@RestController
@RequestMapping("/demo")
public class OssDemoController {

    @Autowired
    private OssTemplate ossTemplate;

    @PostMapping("/upload")
    public OssFileInfo upload(@RequestParam("file") MultipartFile file) throws IOException {
        return ossTemplate.upload(file.getInputStream(), file.getOriginalFilename());
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam String fileName) {
        InputStream inputStream = ossTemplate.download(fileName);
        // 封装为 Resource 返回...
    }

    @DeleteMapping("/delete")
    public void delete(@RequestParam String fileName) {
        ossTemplate.delete(fileName);
    }
}
```

---

### 3. 配置存储类型

通过 `oss.s3.storage-type` 选择存储后端：

```yaml
oss:
  s3:
    storage-type: minio        # 可选值：file / minio / aws / aliyun / tencent / huawei
```

---

## 存储后端支持

组件支持以下存储后端，通过 `oss.s3.storage-type` 切换：

| 存储类型 | 配置值 | endpoint 示例 | 说明 |
|---------|--------|--------------|------|
| **本地文件系统** | `file` | `file:///d:/data/oss` | 无需额外服务，适合开发测试 |
| **MinIO** | `minio` | `http://localhost:9000` | 自建 S3 兼容服务 |
| **AWS S3** | `aws` | `https://s3.amazonaws.com` | AWS 官方 S3 |
| **阿里云 OSS** | `aliyun` | `https://oss-cn-hangzhou.aliyuncs.com` | - |
| **腾讯云 COS** | `tencent` | `https://cos.ap-guangzhou.myqcloud.com` | - |
| **华为云 OBS** | `huawei` | `https://obs.cn-north-4.myhuaweicloud.com` | - |

> **注意**: 切换存储类型后，`pathStyleAccess`、`region` 等参数需根据目标服务调整。

### 本地文件系统

**endpoint 格式**: `file:///` 后跟绝对路径
- Windows: `file:///D:/data/oss` 或 `file:///C:/storage`
- Linux/Mac: `file:///data/oss` 或 `file:///home/user/oss`

**目录结构**:
```
{basePath}/
├── {bucketName}/
│   ├── path/to/file1.jpg
│   ├── path/to/file2.pdf
│   └── ...
└── ...
```

**配置示例**:
```yaml
oss:
  s3:
    storage-type: file
    endpoint: file:///D:/data/oss
    bucket-name: my-bucket
    auto-create-bucket: true
```

> **注意**:
> - 本地文件系统不支持预签名 URL（`presignedGetUrl` 返回文件直接 URI）
> - 分片上传/断点续传端点在 FILE 模式下自动禁用
> - 文件操作直接使用 java.nio API，无网络开销

### MinIO / AWS / 阿里云 / 腾讯云 / 华为云

这些 S3 兼容类型共享同一套实现（`S3OssTemplate`），仅 endpoint 和配置不同：

```yaml
oss:
  s3:
    storage-type: minio         # 或 aws / aliyun / tencent / huawei
    endpoint: http://localhost:9000
    access-key: your-access-key
    secret-key: your-secret-key
    bucket-name: my-bucket
    path-style-access: true     # MinIO=true, AWS/Aliyun/Tencent/Huawei=false
    region: us-east-1
```

### OssTemplate — 统一操作模板

```java
// ==================== 上传 ====================
OssFileInfo upload(InputStream inputStream, String fileName);
OssFileInfo upload(InputStream inputStream, String fileName, String contentType);
OssFileInfo upload(byte[] data, String fileName);
OssFileInfo upload(byte[] data, String fileName, String contentType);
OssFileInfo upload(File file, String fileName);

// ==================== 下载 ====================
InputStream download(String fileName);

// ==================== 删除 ====================
void delete(String fileName);
void delete(List<String> fileNames);

// ==================== 查询 ====================
boolean exists(String fileName);
OssFileInfo getFileInfo(String fileName);
String getUrl(String fileName);
List<OssFileInfo> listFiles(String prefix);

// ==================== 其他 ====================
void copy(String sourceFileName, String targetFileName);
void move(String sourceFileName, String targetFileName);
String presignedGetUrl(String fileName, int expiration);

// ==================== 桶感知操作 ====================
OssFileInfo upload(String bucketName, InputStream inputStream, String fileName);
OssFileInfo upload(String bucketName, byte[] data, String fileName, String contentType);
boolean exists(String bucketName, String fileName);
void delete(String bucketName, String fileName);
void ensureBucketExists(String bucketName);
```

### OssFileInfo — 文件信息

| 字段 | 类型 | 说明 |
|------|------|------|
| `fileName` | `String` | 文件名（含存储路径） |
| `originalFileName` | `String` | 原始文件名 |
| `url` | `String` | 文件访问 URL |
| `etag` | `String` | 文件 ETag |
| `size` | `Long` | 文件大小（字节） |
| `contentType` | `String` | MIME 类型 |
| `bucketName` | `String` | 存储桶名 |
| `lastModified` | `LocalDateTime` | 最后修改时间 |
| `md5` | `String` | MD5 哈希（32 位小写十六进制） |

---

## 功能详解

### 1. 分片上传与断点续传

适用于大文件上传（>100MB）：将文件拆分为多个分片独立上传，传输中断后只需重新上传未完成的分片。

#### 客户端流程

```
POST   /api/oss/upload/init                     → 初始化，获取 uploadId
POST   /api/oss/upload/{uploadId}/parts/{1..N}  → 逐片上传（可并发）
GET    /api/oss/upload/{uploadId}/parts          → 查询已上传分片（断点续传）
POST   /api/oss/upload/{uploadId}/complete       → 完成上传，合并文件
DELETE /api/oss/upload/{uploadId}                → 取消上传（可选）
```

#### REST API 详情

**初始化上传**

```http
POST /api/oss/upload/init
Content-Type: application/json

{
  "fileName": "videos/demo.mp4",
  "contentType": "video/mp4",
  "totalSize": 2147483648
}
```

响应：
```json
{
  "success": true,
  "data": {
    "uploadId": "aWVsdGhpc2lzYS10ZXN0LXVwbG9hZElE",
    "fileName": "videos/demo.mp4",
    "totalSize": 2147483648,
    "partSize": 5242880
  }
}
```

**上传分片**

```http
POST /api/oss/upload/{uploadId}/parts/1
Content-Type: multipart/form-data

file: <分片二进制数据>
```

响应：
```json
{
  "success": true,
  "data": {
    "uploadId": "aWVsdGhpc2lzYS10ZXN0LXVwbG9hZElE",
    "partNumber": 1,
    "etag": "\"etag-value-1\"",
    "size": 5242880
  }
}
```

**查询已上传分片（断点续传核心）**

```http
GET /api/oss/upload/{uploadId}/parts
```

响应：返回已成功上传的所有分片列表，客户端对比缺失的分片重新上传。

```json
{
  "success": true,
  "data": [
    { "uploadId": "...", "partNumber": 1, "etag": "\"etag-1\"", "size": 5242880 },
    { "uploadId": "...", "partNumber": 2, "etag": "\"etag-2\"", "size": 5242880 }
  ]
}
```

**完成上传**

```http
POST /api/oss/upload/{uploadId}/complete
Content-Type: application/json

{
  "fileName": "videos/demo.mp4"
}
```

#### 配置

```yaml
oss:
  s3:
    upload:
      enabled: true                    # 启用分片上传（默认启用）
    multipart-part-size: 5242880       # 分片大小 5MB
```

#### 代码示例（服务端分片）

```java
@Autowired
private MultipartUploadService multipartUploadService;

// 服务端自动分片上传大文件（适用于服务端转存场景）
public OssFileInfo uploadLargeFile(InputStream inputStream, String fileName) {
    String uploadId = multipartUploadService.initiateUpload(fileName, "application/octet-stream", null);
    try {
        byte[] buffer = new byte[5 * 1024 * 1024]; // 5MB 分片
        int partNumber = 1;
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            byte[] partData = bytesRead == buffer.length ? buffer : Arrays.copyOf(buffer, bytesRead);
            try (InputStream partStream = new ByteArrayInputStream(partData)) {
                multipartUploadService.uploadPart(uploadId, partNumber, bytesRead, partStream);
            }
            partNumber++;
        }
        String bucketName = "my-bucket";
        return multipartUploadService.completeUpload(uploadId, bucketName, fileName, null);
    } catch (Exception e) {
        multipartUploadService.abortUpload(uploadId);
        throw new RuntimeException("Upload failed", e);
    }
}
```

---

### 2. 图片缩略图生成

图片上传时自动生成缩略图，缩略图会上传到对象存储中与原文件关联。

#### 工作原理

```
上传图片 (photo.jpg)
    │
    ├── 原始文件 → S3: images/2026/07/photo.jpg
    │
    └── 缩略图   → S3: thumbnails/2026/07/photo_thumb.jpg
                        （OssFileInfo 中记录 thumbnailUrl）
```

#### 配置

```yaml
oss:
  thumbnail:
    enabled: true                    # 启用缩略图生成（默认启用）
    width: 200                       # 缩略图宽度
    height: 200                      # 缩略图高度
    keep-aspect-ratio: true          # 保持原始宽高比
    quality: 0.8                     # 图片质量 (0.0 ~ 1.0)
    format: jpeg                     # 输出格式
    suffix: _thumb                   # 文件名后缀
    path-prefix: thumbnails          # 存储路径前缀
    max-original-width: 0            # 超过此宽度才生成（0=始终生成）
    max-original-height: 0           # 超过此高度才生成（0=始终生成）
```

#### 支持格式

JPEG、PNG、GIF、BMP、WEBP（需 JDK 版本支持）、TIFF

#### 代码示例

```java
@Autowired
private OssTemplate ossTemplate;

// 上传图片自动生成缩略图（引入 jpa-oss-component 后自动集成）
public void uploadImage(MultipartFile file) throws IOException {
    // 引入 jpa-oss-component 后，upload 方法自动：
    // 1. 计算 MD5 (去重)
    // 2. 检测文件类型 (IMAGE)
    // 3. 按桶策略确定桶和路径
    // 4. 上传原始文件
    // 5. @生成缩略图并上传@
    // 6. 保存文件记录
    OssFileInfo info = ossTemplate.upload(file.getInputStream(), file.getOriginalFilename());

    // 缩略图信息
    String thumbnailUrl = info.getUrl(); // 原始文件 URL
    // 如需缩略图 URL，可从数据库记录中查询
}
```

> **注意**: 缩略图生成依赖 `jpa-oss-component`（需要 `PersistingOssTemplate` 装饰器自动触发），纯 `core-oss-component` 不会自动生成。

---

### 3. 自动桶策略

按文件类型自动分配到不同存储桶，并按 `/年/月/` 划分路径，避免单桶/单目录下文件过多导致性能下降。

#### 文件类型分类

| 类型 | 默认桶后缀 | 匹配扩展名 |
|------|-----------|-----------|
| `IMAGE` | `images` | jpg, jpeg, png, gif, bmp, webp, svg, ico |
| `DOCUMENT` | `documents` | pdf, doc, docx, xls, xlsx, ppt, pptx, txt, csv |
| `ARCHIVE` | `archives` | zip, rar, 7z, tar, gz, bz2, xz |
| `AUDIO` | `audio` | mp3, wav, ogg, flac, aac, wma, m4a |
| `VIDEO` | `video` | mp4, avi, mkv, wmv, mov, flv, webm |
| `OTHER` | `others` | 其他类型 |

#### 配置

```yaml
oss:
  s3:
    bucket-strategy-enabled: true      # 启用自动桶策略
    bucket-prefix: app                 # 桶名前缀 → app-images, app-documents...
    date-path-enabled: true             # 日期路径划分 → 2026/07/
    bucket-suffix-override:
      IMAGE: pictures                  # 自定义类型 → 桶后缀映射
      DOCUMENT: files
```

#### 桶命名规则

```
{bucket-prefix}-{fileType.getBucketSuffix()}
例如: app-images, app-documents, app-archives, app-audio, app-video, app-others
```

#### 路径规则

```
{year}/{month}/{originalFileName}
例如: 2026/07/photo.jpg, 2026/07/report.pdf
```

当 `date-path-enabled: false` 时，文件直接存放在桶根目录。

#### 自定义桶策略

```java
@Component
public class CustomBucketStrategy implements BucketStrategy {

    @Override
    public String determineBucketName(String originalFileName, String contentType, FileType fileType) {
        // 自定义桶名逻辑
        return "custom-" + fileType.getBucketSuffix();
    }

    @Override
    public String determinePathPrefix(String originalFileName, FileType fileType) {
        // 自定义路径逻辑
        LocalDate now = LocalDate.now();
        return fileType.getBucketSuffix() + "/" +
               DateTimeFormatter.ofPattern("yyyy/MM/dd").format(now) + "/";
    }
}
```

自定义策略后，无需其他配置，`PersistingOssTemplate` 会自动使用。

---

## 持久化支持

### JPA 版本（推荐）

引入 `jpa-oss-component` 后自动获得以下增强：

| 能力 | 说明 |
|------|------|
| **文件记录管理** | 上传/删除操作自动写入 `sys_oss_file` 表 |
| **MD5 去重** | 同一用户上传相同 MD5 的文件时直接返回已有记录，避免重复存储 |
| **缩略图生成** | 图片上传后自动生成缩略图并记录 |
| **桶策略集成** | 启用桶策略后自动按类型分桶 + 按日期分路径 |
| **CRUD 接口** | 内置 `GET /oss-files` 等文件记录管理 API |

#### sys_oss_file 表结构

| 字段 | 类型 | 说明 |
|------|------|------|
| `file_id` | BIGINT (PK) | 主键 |
| `file_name` | VARCHAR(500) | 存储文件名（含路径） |
| `original_file_name` | VARCHAR(255) | 原始文件名 |
| `file_suffix` | VARCHAR(50) | 文件后缀 |
| `file_size` | BIGINT | 文件大小 |
| `content_type` | VARCHAR(127) | MIME 类型 |
| `bucket_name` | VARCHAR(255) | 存储桶 |
| `etag` | VARCHAR(255) | 文件 ETag |
| `md5` | VARCHAR(32) | MD5 哈希 |
| `url` | VARCHAR(2048) | 访问 URL |
| `storage_type` | VARCHAR(50) | 存储类型 (s3/minio/cos/oss) |
| `file_type` | VARCHAR(20) | 文件类型分类 |
| `thumbnail_name` | VARCHAR(500) | 缩略图文件名 |
| `thumbnail_url` | VARCHAR(2048) | 缩略图 URL |
| `thumbnail_width` | INT | 缩略图宽度 |
| `thumbnail_height` | INT | 缩略图高度 |
| `upload_id` | VARCHAR(255) | 分片上传会话 ID |
| `upload_status` | VARCHAR(20) | 上传状态 |
| `status` | TINYINT | 0-正常，1-已删除 |
| `created_by` / `created_date` | - | 审计字段 |
| `last_modified_by` / `last_modified_date` | - | 审计字段 |

### JDBC 版本

轻量替代方案，不依赖 JPA / Hibernate，直接使用 `NamedParameterJdbcTemplate`，表结构与 JPA 版本完全兼容。

---

## 高级功能

### 热刷新 OSS 连接

支持在运行时动态刷新 OSS 连接配置（更换 endpoint、accessKey、bucketName），不影响正在进行的文件操作。

```http
POST /api/oss/refresh
```

通过 `oss.s3.management.enabled=true` 启用此端点。

也支持 Spring Cloud Config / Nacos / Apollo 配置中心的自动刷新：配置中心更新 `oss.s3.*` 后自动触发连接切换。

### 预签名 URL

生成临时授权访问链接，适用于前端直传、临时分享等场景。

```java
// 生成 5 分钟有效的下载链接
String url = ossTemplate.presignedGetUrl("documents/report.pdf", 300);
```

### 文件复制与移动

```java
ossTemplate.copy("source/file.pdf", "target/file.pdf");
ossTemplate.move("source/file.pdf", "target/file.pdf"); // 复制后删除源文件
```

### 连接管理

```java
@Autowired
private OssConnectionManager connectionManager;

// 手动刷新连接
connectionManager.refresh();

// 获取当前模板
OssTemplate template = connectionManager.getTemplate();

// 销毁连接（释放资源）
connectionManager.destroy();
```

---

## 配置参考

### 完整配置项

```yaml
oss:
  s3:
    enabled: true                       # 是否启用 OSS
    storage-type: minio                 # 存储类型：file/minio/aws/aliyun/tencent/huawei
    endpoint: http://localhost:9000      # S3 服务地址（file:///path 用于本地文件系统）
    region: us-east-1                   # 区域
    access-key:                         # Access Key
    secret-key:                         # Secret Key
    bucket-name: default-bucket         # 默认桶名
    base-path: ""                       # 基础路径前缀
    path-style-access: true             # 路径风格（MinIO）=true，虚拟主机风格（AWS）=false
    max-upload-size: 104857600          # 最大上传大小（100MB）
    presigned-url-expiration: 600       # 预签名 URL 过期秒数
    connection-timeout: 5000            # 连接超时（ms）
    read-timeout: 30000                 # 读取超时（ms）
    multipart-part-size: 5242880        # 分片大小（5MB）
    auto-create-bucket: true            # 启动时自动创建默认桶

    # 自动桶策略（按文件类型分桶）
    bucket-strategy-enabled: false       # 启用桶策略
    bucket-prefix: app                   # 桶名前缀
    date-path-enabled: true              # /年/月/ 路径划分
    bucket-suffix-override: {}           # 桶后缀覆盖

    # 管理端点
    management:
      enabled: false                     # 启用 /api/oss/refresh 端点

    # 分片上传
    upload:
      enabled: true                      # 启用分片上传端点

    # 文件记录 CRUD
    crud:
      enabled: true                      # 启用 /oss-files CRUD 端点

  thumbnail:
    enabled: true                        # 启用缩略图生成
    width: 200                           # 缩略图宽度
    height: 200                          # 缩略图高度
    keep-aspect-ratio: true              # 保持宽高比
    quality: 0.8                         # 图片质量
    format: jpeg                         # 输出格式
    suffix: _thumb                       # 文件名后缀
    path-prefix: thumbnails              # 存储路径前缀
    max-original-width: 0                # 最大原始宽度阈值
    max-original-height: 0               # 最大原始高度阈值
```

### 环境变量（推荐生产使用）

```yaml
oss:
  s3:
    access-key: ${OSS_ACCESS_KEY}
    secret-key: ${OSS_SECRET_KEY}
    endpoint: ${OSS_ENDPOINT}
    bucket-name: ${OSS_BUCKET_NAME}
```

---

## 兼容性说明

| 存储服务 | endpoint 示例 | pathStyleAccess | 备注 |
|---------|--------------|----------------|------|
| **本地文件系统** | `file:///d:/data/oss` | `file` | 自动 | 开发测试首选 |
| **MinIO** | `http://localhost:9000` | `minio` | 自动=true | 开发首选 |
| **AWS S3** | `https://s3.amazonaws.com` | `aws` | 自动=false | - |
| **阿里云 OSS** | `https://oss-cn-hangzhou.aliyuncs.com` | `aliyun` | 自动=false | - |
| **腾讯云 COS** | `https://cos.ap-guangzhou.myqcloud.com` | `tencent` | 自动=false | - |
| **华为云 OBS** | `https://obs.cn-north-4.myhuaweicloud.com` | `huawei` | 自动=false | - |
| **京东云 OSS** | `https://s3.cn-north-1.jdcloud-oss.com` | `false` | - |

---

## GraalVM Native Image 支持

本组件已通过 `OssRuntimeHints` 注册了所有需要在运行时反射访问的类型，包括：
- 所有 API / Core / 控制器的类
- ImageIO SPI（Reader、Writer、Stream 实现）
- AWT 渲染类型（RenderingHints、Graphics2D、BufferedImage 等）

native-image 编译时无需额外配置。

---

## 常见问题

**Q: 上传文件时提示 Bucket 不存在？**
A: 设置 `oss.s3.auto-create-bucket=true` 或在启动前手动创建桶。

**Q: 分片上传时如何确定每片大小？**
A: 推荐 5MB ~ 50MB。S3 要求除最后一片外每片 ≥ 5MB，最大 10000 片。

**Q: 缩略图生成失败怎么办？**
A: 检查是否为图片格式（支持 jpg/png/gif/bmp/webp），在 GraalVM 下需确保 ImageIO SPI 已注册。

**Q: 如何关闭分片上传端点？**
A: 设置 `oss.s3.upload.enabled=false`。

**Q: JPA 和 JDBC 版本能否共存？**
A: 不能，二者映射同一张 `sys_oss_file` 表，请择一引入。
