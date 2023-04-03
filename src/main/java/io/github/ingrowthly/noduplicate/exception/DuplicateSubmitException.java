package io.github.ingrowthly.noduplicate.exception;

/**
 * 重复提交异常
 *
 * @since 2023/4/3
 */
public class DuplicateSubmitException extends RuntimeException {

  public DuplicateSubmitException(String message) {
    super(message);
  }
}
