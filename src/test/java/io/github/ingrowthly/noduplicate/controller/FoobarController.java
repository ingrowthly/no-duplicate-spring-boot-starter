package io.github.ingrowthly.noduplicate.controller;

import io.github.ingrowthly.noduplicate.annotation.NoDuplicate;
import io.github.ingrowthly.noduplicate.model.Foobar;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @since 2023/4/3
 */
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

  @GetMapping("/no-params")
  @NoDuplicate
  public Object noParams() {
    return "no-params";
  }

  @GetMapping("/termination")
  @NoDuplicate(termination = true)
  public Object getTermination(String foo, String bar) {
    return foo + bar;
  }

  @GetMapping("/termination/exception")
  @NoDuplicate(termination = true, ttl = 200)
  public Object getTerminationException(String foo, String bar) {
    throw new NullPointerException("发生 NullPointerException 异常");
  }

  @GetMapping("/termination/longTTL")
  @NoDuplicate(termination = true, ttl = 200)
  public Object getTerminationLongTTL(String foo, String bar) {
    return foo + bar;
  }
}
