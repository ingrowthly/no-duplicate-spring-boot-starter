package io.github.ingrowthly.noduplicate.model;

import java.util.Objects;

/**
 * @since 2023/4/3
 */
public class Foobar {

  private String foo;

  private String bar;

  public Foobar(String foo, String bar) {
    this.foo = foo;
    this.bar = bar;
  }

  public String getFoo() {
    return foo;
  }

  public void setFoo(String foo) {
    this.foo = foo;
  }

  public String getBar() {
    return bar;
  }

  public void setBar(String bar) {
    this.bar = bar;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Foobar foobar = (Foobar)o;

    if (!Objects.equals(foo, foobar.foo)) {
      return false;
    }
    return Objects.equals(bar, foobar.bar);
  }

  @Override
  public int hashCode() {
    int result = foo != null ? foo.hashCode() : 0;
    result = 31 * result + (bar != null ? bar.hashCode() : 0);
    return result;
  }
}
