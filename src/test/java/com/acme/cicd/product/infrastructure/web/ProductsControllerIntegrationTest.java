package com.acme.cicd.product.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.cicd.Application;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = Application.class)
@Testcontainers
class ProductsControllerIntegrationTest {

  @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @LocalServerPort int port;

  @Test
  void shouldCreateAndRetrieveProductUsingRestApi() {
    RestClient client = RestClient.create("http://localhost:" + port);
    CreateProductRequest request =
        new CreateProductRequest("SKU-001", "Integration Product", new BigDecimal("15.00"), "USD");

    ResponseEntity<ProductResponse> createResponse =
        client
            .post()
            .uri("/products")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .toEntity(ProductResponse.class);

    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    ProductResponse created = createResponse.getBody();
    assertThat(created).isNotNull();
    assertThat(created.id()).isNotNull();
    assertThat(created.sku()).isEqualTo("SKU-001");
    assertThat(created.name()).isEqualTo("Integration Product");
    assertThat(created.price()).isEqualByComparingTo("15.00");
    assertThat(created.currency()).isEqualTo("USD");

    ProductResponse byId =
        client.get().uri("/products/{id}", created.id()).retrieve().body(ProductResponse.class);
    assertThat(byId).isEqualTo(created);

    ProductResponse bySku =
        client
            .get()
            .uri("/products?sku={sku}", created.sku())
            .retrieve()
            .body(ProductResponse.class);
    assertThat(bySku).isEqualTo(created);
  }
}
