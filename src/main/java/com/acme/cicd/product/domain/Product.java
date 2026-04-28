package com.acme.cicd.product.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record Product(UUID id, String sku, String name, BigDecimal price, String currency) {
  public static Product createNew(
      UUID id, String sku, String name, BigDecimal price, String currency) {
    return new Product(id, sku, name, price, currency);
  }

  public static Product restore(
      UUID id, String sku, String name, BigDecimal price, String currency) {
    return new Product(id, sku, name, price, currency);
  }
}
