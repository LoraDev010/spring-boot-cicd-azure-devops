package com.acme.cicd.product.application;

import com.acme.cicd.product.application.port.in.CreateProductCommand;
import com.acme.cicd.product.application.port.in.CreateProductUseCase;
import com.acme.cicd.product.application.port.out.ProductRepositoryPort;
import com.acme.cicd.product.domain.Product;
import java.util.UUID;

public class CreateProductService implements CreateProductUseCase {

  private final ProductRepositoryPort repository;

  public CreateProductService(ProductRepositoryPort repository) {
    this.repository = repository;
  }

  @Override
  public UUID create(CreateProductCommand command) {
    repository
        .findBySku(command.sku())
        .ifPresent(
            p -> {
              throw new DuplicateSkuException("sku already exists");
            });

    UUID id = UUID.randomUUID();
    repository.save(
        Product.createNew(id, command.sku(), command.name(), command.price(), command.currency(), command.stock()));
    return id;
  }
}
