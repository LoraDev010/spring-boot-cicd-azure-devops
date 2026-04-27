package com.acme.cicd.product.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.cicd.Application;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = Application.class)
@Testcontainers
class ProductsControllerIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
    .withDatabaseName("testdb")
    .withUsername("test")
    .withPassword("test");

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired
  WebTestClient webTestClient;

  @Test
  void shouldCreateAndRetrieveProductUsingRestApi() {
    CreateProductRequest request = new CreateProductRequest(
      "SKU-001",
      "Integration Product",
      new BigDecimal("15.00"),
      "USD"
    );

    // Create product
    ProductResponse created = webTestClient
      .post()
      .uri("/products")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus().isCreated()
      .expectBody(ProductResponse.class)
      .consumeWith(response -> {
        assertThat(response.getResponseBody()).isNotNull();
        assertThat(response.getResponseBody().id()).isNotNull();
        assertThat(response.getResponseBody().sku()).isEqualTo("SKU-001");
        assertThat(response.getResponseBody().name()).isEqualTo("Integration Product");
        assertThat(response.getResponseBody().price()).isEqualByComparingTo("15.00");
        assertThat(response.getResponseBody().currency()).isEqualTo("USD");
      })
      .returnResult()
      .getResponseBody();

    // Get by ID
    webTestClient
      .get()
      .uri("/products/{id}", created.id())
      .exchange()
      .expectStatus().isOk()
      .expectBody(ProductResponse.class)
      .isEqualTo(created);

    // Get by SKU
    webTestClient
      .get()
      .uri("/products?sku={sku}", created.sku())
      .exchange()
      .expectStatus().isOk()
      .expectBody(ProductResponse.class)
      .isEqualTo(created);
  }
}
