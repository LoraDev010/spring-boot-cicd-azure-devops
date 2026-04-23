package com.acme.cicd.product.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "products", uniqueConstraints = @UniqueConstraint(name = "uq_products_sku", columnNames = "sku"))
public class ProductJpaEntity {

  @Id
  private UUID id;

  @Column(nullable = false, length = 64)
  private String sku;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal price;

  @Column(nullable = false, length = 3)
  private String currency;

  protected ProductJpaEntity() {}

  public ProductJpaEntity(UUID id, String sku, String name, BigDecimal price, String currency) {
    this.id = id;
    this.sku = sku;
    this.name = name;
    this.price = price;
    this.currency = currency;
  }

  public UUID getId() { return id; }
  public String getSku() { return sku; }
  public String getName() { return name; }
  public BigDecimal getPrice() { return price; }
  public String getCurrency() { return currency; }
}
