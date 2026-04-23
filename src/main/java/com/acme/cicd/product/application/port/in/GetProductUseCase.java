package com.acme.cicd.product.application.port.in;

import com.acme.cicd.product.domain.Product;
import java.util.UUID;

public interface GetProductUseCase {
  Product getById(UUID id);
  Product getBySku(String sku);
}
