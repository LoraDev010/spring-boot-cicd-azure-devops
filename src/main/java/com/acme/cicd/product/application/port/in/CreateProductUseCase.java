package com.acme.cicd.product.application.port.in;

import java.util.UUID;

public interface CreateProductUseCase {
  UUID create(CreateProductCommand command);
}
