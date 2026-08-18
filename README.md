# No Duplicate Spring Boot Starter

基于 Redis 的 Spring Boot 防重复提交组件。通过原子 `SET NX` 获取请求占用标记，并使用 SHA-256 参数指纹识别重复请求。主动释放时会校验唯一令牌，不会误删其他请求的 Redis 键。

## 环境要求

- Java 17+
- Spring Boot 4.1+
- Redis（仅应用运行时需要，项目测试不依赖 Redis）

当前开发版本为 `3.0.0-SNAPSHOT`。历史上的 Spring Boot 3 版本可继续使用 `2.0.0`。

```groovy
implementation 'io.github.ingrowthly:no-duplicate-spring-boot-starter:3.0.0-SNAPSHOT'
```

## 快速使用

在需要保护的 Spring Web 方法上添加 `@NoDuplicate`：

```java
@PostMapping("/orders")
@NoDuplicate(ttl = 3, message = "订单正在处理中")
public Order create(@RequestBody CreateOrderRequest request) {
    return orderService.create(request);
}
```

默认使用“Java 方法签名 + 全部参数 + 请求 URI”生成指纹，并在 2 秒内拒绝相同调用，重复时抛出 `DuplicateSubmitException`。

只选择业务字段时使用 SpEL。构建启用了 `-parameters`，参数名和 `#p0` 别名均可使用：

```java
@NoDuplicate(key = "#request.orderNo", ttl = 500,
        timeUnit = TimeUnit.MILLISECONDS)
```

当限制只需覆盖方法执行期间时，设置 `termination = true`。无论方法正常返回还是抛出异常，占用标记都会安全释放：

```java
@NoDuplicate(key = "#orderId", termination = true)
public void process(String orderId) {
    // ...
}
```

## 配置

```yaml
spring:
  application:
    name: order-service

no-duplicate:
  enabled: true
  key-prefix: no-duplicate
  namespace: order-api # 可选，默认使用 spring.application.name
```

Redis 连接沿用 Spring Boot 的 `spring.data.redis.*` 配置。可通过 `no-duplicate.enabled=false` 完全关闭自动配置。

## 扩展存储

默认在存在 `StringRedisTemplate` 时注册 Redis 实现。应用可以声明自己的 `DuplicateRequestStore` Bean，自动配置会优先使用它，便于接入其他具备原子“获取 + 过期 + 令牌释放”能力的存储。

## 本地开发

```bash
./gradlew clean build
./gradlew test
```

测试使用 Mockito 和内存存储替身，不需要启动 Redis。覆盖率报告生成在 `build/reports/jacoco/test/html/index.html`，构建要求行覆盖率不低于 80%。

版本升级和迁移说明见 [CHANGELOG.md](CHANGELOG.md)。

维护者发布正式版本前，请按 [Maven Central 发布指南](.github/RELEASING.md) 配置 Central Portal、GPG 签名和 GitHub Secrets。

## 许可证

本项目基于 [Apache License 2.0](LICENSE) 发布。
