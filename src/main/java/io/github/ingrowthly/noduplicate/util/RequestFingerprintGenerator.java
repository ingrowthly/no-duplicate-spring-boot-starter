package io.github.ingrowthly.noduplicate.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Creates deterministic SHA-256 request fingerprints and caches parsed SpEL expressions.
 */
public final class RequestFingerprintGenerator {

    private static final DefaultParameterNameDiscoverer PARAMETER_NAMES = new DefaultParameterNameDiscoverer();
    private static final SpelExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

    private final ConcurrentMap<String, Expression> expressions = new ConcurrentHashMap<>();
    private final ObjectWriter objectWriter;
    private final ThreadLocal<MessageDigest> digest = ThreadLocal.withInitial(RequestFingerprintGenerator::sha256);

    public RequestFingerprintGenerator(ObjectMapper objectMapper) {
        ObjectMapper mapper = objectMapper.copy()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectWriter = mapper.writer();
    }

    /**
     * Generates a fingerprint scoped to the invoked method.
     *
     * @param method invoked method
     * @param arguments invocation arguments
     * @param keyExpression optional SpEL expression selecting the fingerprint input
     * @return lowercase SHA-256 fingerprint
     */
    public String generate(Method method, Object[] arguments, String keyExpression) {
        Object input = arguments;
        String identity = method.toGenericString();
        if (StringUtils.hasText(keyExpression)) {
            MethodBasedEvaluationContext context =
                    new MethodBasedEvaluationContext(null, method, arguments, PARAMETER_NAMES);
            input = expressions.computeIfAbsent(keyExpression, EXPRESSION_PARSER::parseExpression).getValue(context);
        }

        MessageDigest messageDigest = digest.get();
        messageDigest.reset();
        messageDigest.update(identity.getBytes(StandardCharsets.UTF_8));
        messageDigest.update((byte) 0);
        messageDigest.update(serialize(input));
        return HexFormat.of().formatHex(messageDigest.digest());
    }

    private byte[] serialize(Object value) {
        try {
            return objectWriter.writeValueAsBytes(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Unable to serialize @NoDuplicate key input", ex);
        }
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
