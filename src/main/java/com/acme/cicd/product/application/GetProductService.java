package com.acme.cicd.product.application;

import com.acme.cicd.product.application.port.in.GetProductUseCase;
import com.acme.cicd.product.application.port.out.ProductRepositoryPort;
import com.acme.cicd.product.domain.Product;
import java.util.UUID;

public class GetProductService implements GetProductUseCase {

  private final ProductRepositoryPort repository;

  public GetProductService(ProductRepositoryPort repository) {
    this.repository = repository;
  }

  @Override
  public Product getById(UUID id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new ProductNotFoundException("product not found"));
  }

  @Override
  public Product getBySku(String sku) {
    return repository
        .findBySku(sku)
        .orElseThrow(() -> new ProductNotFoundException("product not found"));
  }
}
