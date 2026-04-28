package com.acme.cicd.product.infrastructure.persistence.jpa;

import com.acme.cicd.product.application.port.out.ProductRepositoryPort;
import com.acme.cicd.product.domain.Product;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ProductPersistenceAdapter implements ProductRepositoryPort {

  private final ProductJpaRepository repository;

  public ProductPersistenceAdapter(ProductJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<Product> findById(UUID id) {
    return repository.findById(id).map(this::toDomain);
  }

  @Override
  public Optional<Product> findBySku(String sku) {
    return repository.findBySku(sku).map(this::toDomain);
  }

  @Override
  public Product save(Product product) {
    ProductJpaEntity saved = repository.save(toEntity(product));
    return toDomain(saved);
  }

  private Product toDomain(ProductJpaEntity e) {
    return Product.restore(
        e.getId(), e.getSku(), e.getName(), e.getPrice(), e.getCurrency(), e.getStock());
  }

  private ProductJpaEntity toEntity(Product p) {
    return new ProductJpaEntity(p.id(), p.sku(), p.name(), p.price(), p.currency(), p.stock());
  }
}
