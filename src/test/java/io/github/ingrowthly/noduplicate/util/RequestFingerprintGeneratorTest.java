package io.github.ingrowthly.noduplicate.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class RequestFingerprintGeneratorTest {

    private final RequestFingerprintGenerator generator = new RequestFingerprintGenerator(new ObjectMapper());

    @Test
    void producesStableFingerprintForEquivalentArguments() throws NoSuchMethodException {
        Method method = Fixture.class.getMethod("handle", String.class, Map.class);
        Map<String, Integer> first = new LinkedHashMap<>();
        first.put("b", 2);
        first.put("a", 1);
        Map<String, Integer> second = new LinkedHashMap<>();
        second.put("a", 1);
        second.put("b", 2);

        String firstFingerprint = generator.generate(method, new Object[]{"id", first}, "");
        String secondFingerprint = generator.generate(method, new Object[]{"id", second}, "");

        assertEquals(firstFingerprint, secondFingerprint);
    }

    @Test
    void distinguishesDifferentArguments() throws NoSuchMethodException {
        Method method = Fixture.class.getMethod("handle", String.class, Map.class);

        String first = generator.generate(method, new Object[]{"one", Map.of()}, "");
        String second = generator.generate(method, new Object[]{"two", Map.of()}, "");

        assertNotEquals(first, second);
    }

    @Test
    void evaluatesParameterNameAndIndexAliases() throws NoSuchMethodException {
        Method method = Fixture.class.getMethod("handle", String.class, Map.class);
        Object[] arguments = {"id", Map.of("value", 1)};

        assertEquals(generator.generate(method, arguments, "#id"),
                generator.generate(method, arguments, "#p0"));
    }

    @Test
    void scopesExpressionFingerprintsToTheInvokedMethod() throws NoSuchMethodException {
        Method firstMethod = Fixture.class.getMethod("handle", String.class, Map.class);
        Method secondMethod = Fixture.class.getMethod("replace", String.class, Map.class);
        Object[] arguments = {"id", Map.of()};

        String first = generator.generate(firstMethod, arguments, "#id");
        String second = generator.generate(secondMethod, arguments, "#id");

        assertNotEquals(first, second);
    }

    static class Fixture {

        public void handle(String id, Map<String, Integer> payload) {
        }

        public void replace(String id, Map<String, Integer> payload) {
        }
    }
}
