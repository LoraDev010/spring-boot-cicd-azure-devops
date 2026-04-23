package com.acme.cicd.product.infrastructure.web;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateProductRequest(
  @NotBlank @Size(max = 64) String sku,
  @NotBlank @Size(max = 200) String name,
  @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
  @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency
) {}
