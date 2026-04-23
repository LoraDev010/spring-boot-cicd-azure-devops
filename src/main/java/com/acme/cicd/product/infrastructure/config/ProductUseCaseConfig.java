package com.acme.cicd.product.infrastructure.config;

import com.acme.cicd.product.application.CreateProductService;
import com.acme.cicd.product.application.GetProductService;
import com.acme.cicd.product.application.port.in.CreateProductUseCase;
import com.acme.cicd.product.application.port.in.GetProductUseCase;
import com.acme.cicd.product.application.port.out.ProductRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProductUseCaseConfig {

  @Bean
  CreateProductUseCase createProductUseCase(ProductRepositoryPort repository) {
    return new CreateProductService(repository);
  }

  @Bean
  GetProductUseCase getProductUseCase(ProductRepositoryPort repository) {
    return new GetProductService(repository);
  }
}

