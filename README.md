# 防重复提交工具

基于 Redis 的防重复提交工具

## 1. 前提
> 项目已接入 Redis

## 2. 导入依赖

**JDK17 + SpringBoot 3.0+**

maven
```xml
<dependency>
  <groupId>io.github.ingrowthly</groupId>
  <artifactId>no-duplicate-spring-boot-starter</artifactId>
  <version>2.0.0</version>
</dependency>
```

gradle
```groovy
implementation 'io.github.ingrowthly:no-duplicate-spring-boot-starter:2.0.0'
```

**JDK8 + SpringBoot 2.0+**

maven
```xml
<dependency>
  <groupId>io.github.ingrowthly</groupId>
  <artifactId>no-duplicate-spring-boot-starter</artifactId>
  <version>1.0.0</version>
</dependency>
```

gradle
```groovy
implementation 'io.github.ingrowthly:no-duplicate-spring-boot-starter:1.0.0'
```
## 3. 使用

### 3.1 注解说明

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NoDuplicate {

    /**
     * 重复提交判定间隔，单位秒
     *
     * <p>两次相同参数的请求，如果间隔时间大于该参数，系统不会认定为重复提交的数据
     */
    int ttl() default 2;

    /**
     * 校验 key，Spring EL 表达式
     *
     * <p>如果为空，则默认取请求参数；如果不为空，则取 Spring EL 表达式的值
     */
    String key() default "";

    /**
     * 是否拼接上 URI
     */
    boolean uri() default true;

    /**
     * 重复提交时的提示信息
     */
    String message() default "请勿重复提交";
}

```
**参数说明**
1. ttl：重复提交判定间隔，单位秒，默认 2s
2. key：SpEL 表达式，留空表示使用参数签名，有值使用 SpEL 表达式值签名，签名用于判断请求是否重复
3. uri：是否拼接上请求的 URI，默认开启
4. message：重复提交时的报错信息，默认为 "请勿重复提交"

**注意事项**
1. 重复提交时抛出 DuplicateException 异常，注意是否需要在全局异常中处理该异常
2. 该注解仅针对 Web 请求方法，只用于重复提交的判断

### 3.2 自动根据参数拦截

直接在 web 方法上面添加 `@NoDuplicate` 注解即可，会根据参数值生成签名，默认时间间隔为 2s，2s 内重复的签名会拦截

```java
@RestController
@RequestMapping("/test")
public class FoobarController {

    @GetMapping
    @NoDuplicate
    public Object get(String foo, String bar) {
        return foo + bar;
    }

    @PostMapping
    @NoDuplicate
    public Object post(@RequestBody Foobar foobar) {
        return foobar;
    }

    @GetMapping("/no-params")
    @NoDuplicate
    public Object noParams() {
        return "no-params";
    }
}

```

### 3.3 SpEL 表达式拦截

> 自定义 SpEl 表达式，根据表达式值的签名进行拦截，SpEL 表达式参考 https://docs.spring.io/spring-framework/docs/3.0.x/reference/expressions.html
> 
> 也可以参考 @Cacheable 注解的 SpEL 表达式

```java
@RestController
@RequestMapping("/test")
public class FoobarController {

    @GetMapping("/spel")
    @NoDuplicate(key = "#foo")
    public Object getSpel(String foo, String bar) {
        return foo + bar;
    }

    @PostMapping("/spel")
    @NoDuplicate(key = "#foobar.bar")
    public Object postSpel(@RequestBody Foobar foobar) {
        return foobar;
    }

}

```

## 附录
1. https://docs.spring.io/spring-framework/docs/3.0.x/reference/expressions.html
