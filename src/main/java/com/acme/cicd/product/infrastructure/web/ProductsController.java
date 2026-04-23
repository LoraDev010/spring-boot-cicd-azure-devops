package com.acme.cicd.product.infrastructure.web;

import com.acme.cicd.product.application.port.in.CreateProductCommand;
import com.acme.cicd.product.application.port.in.CreateProductUseCase;
import com.acme.cicd.product.application.port.in.GetProductUseCase;
import com.acme.cicd.product.domain.Product;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
public class ProductsController {

  private final CreateProductUseCase createProductUseCase;
  private final GetProductUseCase getProductUseCase;

  public ProductsController(CreateProductUseCase createProductUseCase, GetProductUseCase getProductUseCase) {
    this.createProductUseCase = createProductUseCase;
    this.getProductUseCase = getProductUseCase;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ProductResponse create(@RequestBody @Valid CreateProductRequest request) {
    UUID id = createProductUseCase.create(
      new CreateProductCommand(request.sku(), request.name(), request.price(), request.currency())
    );
    return toResponse(getProductUseCase.getById(id));
  }

  @GetMapping("/{id}")
  public ProductResponse getById(@PathVariable UUID id) {
    return toResponse(getProductUseCase.getById(id));
  }

  @GetMapping
  public ProductResponse getBySku(@RequestParam("sku") String sku) {
    return toResponse(getProductUseCase.getBySku(sku));
  }

  private static ProductResponse toResponse(Product p) {
    return new ProductResponse(p.id(), p.sku(), p.name(), p.price(), p.currency());
  }
}
