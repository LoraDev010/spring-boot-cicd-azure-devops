package com.acme.cicd.product.infrastructure.web;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
    UUID id, String sku, String name, BigDecimal price, String currency) {}
