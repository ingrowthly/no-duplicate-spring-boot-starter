package io.github.ingrowthly.noduplicate.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import io.github.ingrowthly.noduplicate.annotation.NoDuplicate;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * 防止重复提交工具类
 *
 * @since 2023/4/3
 */
@Slf4j
public class NoDuplicateUtils {

    private static final DefaultParameterNameDiscoverer DISCOVERER = new DefaultParameterNameDiscoverer();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HashFunction HASHING = Hashing.goodFastHash(32);
    private static final String EMPTY_STRING_HASHING = HASHING.hashBytes(new byte[0]).toString();
    /**
     * SpEL 表达式解析器
     */
    private static final SpelExpressionParser PARSER = new SpelExpressionParser();
    /**
     * 方法签名缓存
     */
    private static final LoadingCache<MethodSignature, String[]> SIGNATURE_TO_PARAMETER_NAMES_CACHE =
        CacheBuilder.newBuilder().maximumSize(1000).expireAfterWrite(10, TimeUnit.MINUTES).build(new CacheLoader<>() {
            @Override
            public String[] load(MethodSignature signature) {
                return DISCOVERER.getParameterNames(signature.getMethod());
            }
        });

    static {
        // 允许属性名称没有引号
        OBJECT_MAPPER.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

        // 允许单引号
        OBJECT_MAPPER.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);

        // 忽略在json字符串中存在,但是在java对象中不存在对应属性的情况
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 所有的日期格式都统一为以下的样式(yyyy-MM-dd HH:mm:ss)
        OBJECT_MAPPER.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private NoDuplicateUtils() {
    }

    /**
     * 获取参数签名
     *
     * @param method 方法
     * @param args   参数
     * @return 签名
     */
    public static String getMethodSign(Method method, Object... args) {
        StringBuilder sb = new StringBuilder(method.toString());
        for (Object arg : args) {
            sb.append(toString(arg));
        }
        return HASHING.hashBytes(sb.toString().getBytes()).toString();
    }

    /**
     * 获取 SpEL 表达式值
     *
     * @param pjp         ProceedingJoinPoint
     * @param noDuplicate NoRepeatSubmit
     * @return SpEL 表达式值 SHA1 签名
     */
    public static String getSpelKey(ProceedingJoinPoint pjp, NoDuplicate noDuplicate) {
        String key = noDuplicate.key();
        if (Strings.isNullOrEmpty(key)) {
            return EMPTY_STRING_HASHING;
        }
        MethodSignature signature = (MethodSignature)pjp.getSignature();
        String[] parameterNames = getParameterNames(signature);
        if (parameterNames == null || parameterNames.length == 0) {
            return EMPTY_STRING_HASHING;
        }
        EvaluationContext context = new StandardEvaluationContext();
        Object[] args = pjp.getArgs();
        IntStream.range(0, args.length).forEach(i -> context.setVariable(parameterNames[i], args[i]));
        Object value = PARSER.parseExpression(key).getValue(context);
        String spelValue = value == null ? "" : value.toString();
        return HASHING.hashBytes(spelValue.getBytes()).toString();
    }

    /**
     * 获取方法参数名
     *
     * @param signature 方法签名
     * @return 参数名
     */
    private static String[] getParameterNames(MethodSignature signature) {
        String[] parameterNames = null;
        try {
            parameterNames = SIGNATURE_TO_PARAMETER_NAMES_CACHE.get(signature);
        } catch (ExecutionException e) {
            log.error("Failed to get parameter names from cache", e);
        }
        if (parameterNames == null) {
            parameterNames = DISCOVERER.getParameterNames(signature.getMethod());
            if (parameterNames != null) {
                SIGNATURE_TO_PARAMETER_NAMES_CACHE.put(signature, parameterNames);
            }
        }
        return parameterNames;
    }

    /**
     * 拼接 redis key
     *
     * @param namespace 命名空间
     * @param uri       请求地址
     * @param sign      签名
     * @return redis key
     */
    public static String buildRedisKey(String namespace, String uri, String sign) {
        StringBuilder redisKey = new StringBuilder();
        if (namespace != null && !namespace.isEmpty()) {
            redisKey.append(namespace).append(":");
        }
        if (uri != null && !uri.isEmpty()) {
            redisKey.append(uri).append(":");
        }
        if (sign != null && !sign.isEmpty()) {
            redisKey.append(sign);
        }
        int len = redisKey.length();
        if (len > 0 && redisKey.charAt(len - 1) == ':') {
            redisKey.deleteCharAt(len - 1);
        }
        return redisKey.toString();
    }

    /**
     * 将参数转换为字符串
     *
     * @param arg 参数
     * @return 字符串
     */
    private static String toString(Object arg) {
        if (arg == null) {
            return "null";
        }
        if (arg instanceof Number) {
            return arg.toString();
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(arg);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert arg to json string, arg: {}", arg, e);
            return "null";
        }
    }
}
