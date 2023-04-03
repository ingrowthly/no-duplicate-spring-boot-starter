package io.github.ingrowthly.noduplicate.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.ingrowthly.noduplicate.exception.DuplicateSubmitException;
import io.github.ingrowthly.noduplicate.model.Foobar;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * @since 2023/4/3
 */
@SpringBootTest
@ActiveProfiles(value = "local")
class FoobarControllerTest {

    @Autowired private FoobarController foobarController;

    @Test
    void get() throws InterruptedException {
        assertEquals("foobar", foobarController.get("foo", "bar"));
        assertThrows(DuplicateSubmitException.class, () -> foobarController.get("foo", "bar"));
        assertDoesNotThrow(() -> foobarController.get("foo", "bar1"));
        Thread.sleep(2000);
        assertEquals("foobar", foobarController.get("foo", "bar"));
    }

    @Test
    void post() throws InterruptedException {
        Foobar foo = new Foobar("foo11", "bar11");
        assertDoesNotThrow(() -> foobarController.post(foo));
        assertThrows(DuplicateSubmitException.class, () -> foobarController.post(foo));
        assertDoesNotThrow(() -> foobarController.post(new Foobar("foo111", "bar11")));
        Thread.sleep(2000);
        assertDoesNotThrow(() -> foobarController.post(foo));
    }

    @Test
    void getSpel() throws InterruptedException {
        assertDoesNotThrow(() -> foobarController.getSpel("foo111", "bar111"));
        assertThrows(DuplicateSubmitException.class, () -> foobarController.getSpel("foo111", "bar111"));
        assertDoesNotThrow(() -> foobarController.getSpel("foo1111", "bar111"));
        Thread.sleep(2000);
        assertDoesNotThrow(() -> foobarController.getSpel("foo111", "bar111"));
    }

    @Test
    void postSpel() throws InterruptedException {
        Foobar foo = new Foobar("foo", "bar");
        assertDoesNotThrow(() -> foobarController.post(foo));
        assertThrows(DuplicateSubmitException.class, () -> foobarController.post(foo));
        assertDoesNotThrow(() -> foobarController.post(new Foobar("foo", "bar1")));
        Thread.sleep(2000);
        assertDoesNotThrow(() -> foobarController.post(foo));
    }

    @Test
    void noParam() throws InterruptedException {
        assertDoesNotThrow(() -> foobarController.noParams());
        assertThrows(DuplicateSubmitException.class, () -> foobarController.noParams());
        Thread.sleep(2000);
        assertDoesNotThrow(() -> foobarController.noParams());
    }
}
