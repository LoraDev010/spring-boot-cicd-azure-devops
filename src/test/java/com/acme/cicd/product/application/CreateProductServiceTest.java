package com.acme.cicd.product.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.acme.cicd.product.application.port.in.CreateProductCommand;
import com.acme.cicd.product.application.port.out.ProductRepositoryPort;
import com.acme.cicd.product.domain.Product;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateProductService - Unit Tests")
class CreateProductServiceTest {

  @Mock private ProductRepositoryPort repositoryPort;

  private CreateProductService service;

  @BeforeEach
  void setUp() {
    service = new CreateProductService(repositoryPort);
  }

  @Test
  @DisplayName("debería crear producto exitosamente cuando SKU no existe")
  void shouldCreateProductWhenSkuDoesNotExist() {
    // Arrange
    String sku = "SKU-NEW-001";
    String name = "Laptop Pro";
    BigDecimal price = new BigDecimal("1299.99");
    String currency = "USD";

    CreateProductCommand command = new CreateProductCommand(sku, name, price, currency, 50);

    when(repositoryPort.findBySku(sku)).thenReturn(Optional.empty());

    // Act
    UUID productId = service.create(command);

    // Assert
    assertThat(productId).isNotNull();

    // Verifica que se llamó a findBySku para validar
    verify(repositoryPort, times(1)).findBySku(sku);

    // Verifica que se llamó a save una sola vez
    ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
    verify(repositoryPort, times(1)).save(productCaptor.capture());

    Product savedProduct = productCaptor.getValue();
    assertThat(savedProduct.sku()).isEqualTo(sku);
    assertThat(savedProduct.name()).isEqualTo(name);
    assertThat(savedProduct.price()).isEqualByComparingTo(price);
    assertThat(savedProduct.currency()).isEqualTo(currency);
    assertThat(savedProduct.stock()).isEqualTo(50);
  }

  @Test
  @DisplayName("debería lanzar DuplicateSkuException cuando SKU ya existe")
  void shouldThrowExceptionWhenSkuAlreadyExists() {
    // Arrange
    String sku = "SKU-EXISTING";
    Product existingProduct =
        new Product(UUID.randomUUID(), sku, "Old Product", new BigDecimal("100"), "USD", 5);

    CreateProductCommand command =
        new CreateProductCommand(sku, "New Product", new BigDecimal("200"), "USD", 10);

    when(repositoryPort.findBySku(sku)).thenReturn(Optional.of(existingProduct));

    // Act & Assert
    assertThatThrownBy(() -> service.create(command))
        .isInstanceOf(DuplicateSkuException.class)
        .hasMessageContaining("sku already exists");

    // Verifica que findBySku fue llamado
    verify(repositoryPort, times(1)).findBySku(sku);

    // Verifica que NUNCA se llamó a save
    verify(repositoryPort, never()).save(any());
  }

  @Test
  @DisplayName("debería generar UUID único para cada producto")
  void shouldGenerateUniqueUuidForEachProduct() {
    // Arrange
    CreateProductCommand command1 =
        new CreateProductCommand("SKU-1", "Product 1", new BigDecimal("100"), "USD", 10);
    CreateProductCommand command2 =
        new CreateProductCommand("SKU-2", "Product 2", new BigDecimal("200"), "USD", 20);

    when(repositoryPort.findBySku(anyString())).thenReturn(Optional.empty());

    // Act
    UUID id1 = service.create(command1);
    UUID id2 = service.create(command2);

    // Assert
    assertThat(id1).isNotEqualTo(id2);
  }
}
