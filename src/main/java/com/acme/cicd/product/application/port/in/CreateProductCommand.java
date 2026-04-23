package com.acme.cicd.product.application.port.in;

import java.math.BigDecimal;

public record CreateProductCommand(String sku, String name, BigDecimal price, String currency) {}
