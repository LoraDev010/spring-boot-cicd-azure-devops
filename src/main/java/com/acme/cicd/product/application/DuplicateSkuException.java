package com.acme.cicd.product.application;

public class DuplicateSkuException extends RuntimeException {
  public DuplicateSkuException(String message) {
    super(message);
  }
}
