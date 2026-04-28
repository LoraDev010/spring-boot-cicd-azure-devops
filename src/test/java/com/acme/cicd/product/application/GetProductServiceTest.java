package com.acme.cicd.product.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.acme.cicd.product.application.port.out.ProductRepositoryPort;
import com.acme.cicd.product.domain.Product;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetProductService - Unit Tests")
class GetProductServiceTest {

  @Mock private ProductRepositoryPort repositoryPort;

  private GetProductService service;

  @BeforeEach
  void setUp() {
    service = new GetProductService(repositoryPort);
  }

  @Test
  @DisplayName("debería retornar producto cuando existe por ID")
  void shouldReturnProductWhenExistsById() {
    // Arrange
    UUID productId = UUID.randomUUID();
    Product product = new Product(productId, "SKU-001", "Laptop", new BigDecimal("999.99"), "USD");
    when(repositoryPort.findById(productId)).thenReturn(Optional.of(product));

    // Act
    Product result = service.getById(productId);

    // Assert
    assertThat(result).isEqualTo(product);
    verify(repositoryPort, times(1)).findById(productId);
  }

  @Test
  @DisplayName("debería lanzar ProductNotFoundException cuando no existe por ID")
  void shouldThrowExceptionWhenNotFoundById() {
    // Arrange
    UUID productId = UUID.randomUUID();
    when(repositoryPort.findById(productId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> service.getById(productId))
        .isInstanceOf(ProductNotFoundException.class)
        .hasMessageContaining("product not found");

    verify(repositoryPort, times(1)).findById(productId);
  }

  @Test
  @DisplayName("debería retornar producto cuando existe por SKU")
  void shouldReturnProductWhenExistsBySku() {
    // Arrange
    String sku = "SKU-001";
    Product product =
        new Product(UUID.randomUUID(), sku, "Laptop", new BigDecimal("999.99"), "USD");
    when(repositoryPort.findBySku(sku)).thenReturn(Optional.of(product));

    // Act
    Product result = service.getBySku(sku);

    // Assert
    assertThat(result).isEqualTo(product);
    verify(repositoryPort, times(1)).findBySku(sku);
  }

  @Test
  @DisplayName("debería lanzar ProductNotFoundException cuando no existe por SKU")
  void shouldThrowExceptionWhenNotFoundBySku() {
    // Arrange
    String sku = "SKU-NOTFOUND";
    when(repositoryPort.findBySku(sku)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> service.getBySku(sku))
        .isInstanceOf(ProductNotFoundException.class)
        .hasMessageContaining("product not found");

    verify(repositoryPort, times(1)).findBySku(sku);
  }
}
