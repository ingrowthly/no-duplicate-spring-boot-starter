# Changelog

本文件记录项目的重要变更。格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，版本遵循[语义化版本](https://semver.org/lang/zh-CN/)。

## [Unreleased]

当前开发版本：`3.0.0-SNAPSHOT`。

### Added

- 新增 `DuplicateRequestStore` 扩展接口及默认 Redis 实现，允许应用替换底层存储。
- `@NoDuplicate` 新增 `timeUnit`，支持毫秒等 TTL 单位。
- 新增 `no-duplicate.enabled`、`key-prefix` 和 `namespace` 配置项及配置元数据。
- 新增完全不依赖 Redis、Docker 或真实等待的切面、指纹、存储和自动配置测试。
- 新增 JaCoCo 80% 行覆盖率门槛、GitHub Actions、源码包和 Javadoc 包。
- 新增 Maven Central Portal 自动签名发布工作流、发布手册和 Apache 2.0 许可证文件。
- 新增 Gradle 与 GitHub Actions 的 Dependabot 更新配置。

### Changed

- 升级至 Gradle 9.7.0、Spring Boot 4.1.0，并继续以 Java 17 为最低版本。
- 将项目重构为标准 `java-library`，使用 Boot 4 自动配置导入机制和可复现构建。
- 使用单个 `@Around` 通知完成获取、执行和释放，避免重复计算请求指纹。
- 请求指纹改用 SHA-256，并缓存 SpEL 表达式和摘要实例。
- Redis key 改为 `no-duplicate:<namespace>:<uri>:<sha256>` 格式。

### Fixed

- 主动释放改为 Lua 原子校验唯一令牌后删除，避免误删其他请求的占用标记。
- 业务方法抛出异常时仍会释放 `termination = true` 的标记，释放失败不会覆盖业务异常。
- SpEL 指纹加入方法身份，避免相同 URI 下不同方法因表达式结果相同而互相拦截。
- 自动配置显式在 Spring Boot Redis 自动配置之后加载。

### Removed

- 移除 Guava 和 Lombok 依赖。
- 移除 `NoDuplicateUtils`；自定义代码应改用 `RequestFingerprintGenerator` 或实现 `DuplicateRequestStore`。
- 移除依赖外部 Redis 和 `Thread.sleep` 的旧集成测试夹具。

### Migration

- 3.x 面向 Spring Boot 4.1+ 和 Java 17+；Spring Boot 3 项目应继续使用 2.x。
- Redis key 和指纹算法已经变化，升级后不会复用 2.x 创建的短期防重 key。
- `@NoDuplicate` 的 `ttl`、`key`、`uri`、`message` 和 `termination` 参数保持兼容。

## [2.0.0] - 2024-11-21

### Added

- 新增 `termination` 参数，支持在请求正常完成或异常结束后主动解除防重限制。

### Changed

- 最低运行环境升级至 Java 17 和 Spring Boot 3。
- Servlet API 从 `javax.servlet` 迁移至 `jakarta.servlet`。

### Fixed

- 修正 Spring Boot 自动配置注册问题。

## [1.0.0] - 2023-04-20

### Added

- 提供基于 Redis 和 Spring AOP 的 `@NoDuplicate` 防重复提交能力。
- 支持按完整请求参数或 SpEL 表达式生成防重标记。
- 提供 Spring Boot 2、Java 8 基线及 Maven 发布配置。

[Unreleased]: https://github.com/ingrowthly/no-duplicate-spring-boot-starter/compare/aeb7d52...HEAD
[2.0.0]: https://github.com/ingrowthly/no-duplicate-spring-boot-starter/compare/v1.0.0...aeb7d52
[1.0.0]: https://github.com/ingrowthly/no-duplicate-spring-boot-starter/releases/tag/v1.0.0
