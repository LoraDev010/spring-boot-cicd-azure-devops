package com.acme.cicd.product.application.port.out;

import com.acme.cicd.product.domain.Product;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepositoryPort {
  Optional<Product> findById(UUID id);

  Optional<Product> findBySku(String sku);

  Product save(Product product);
}
