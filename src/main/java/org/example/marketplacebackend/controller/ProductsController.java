package org.example.marketplacebackend.controller;

import com.amazonaws.SdkClientException;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.example.marketplacebackend.DTO.incoming.ProductCategoryDTO;
import org.example.marketplacebackend.DTO.incoming.ProductDTO;
import org.example.marketplacebackend.DTO.outgoing.ProfileResponseDTO;
import org.example.marketplacebackend.DTO.outgoing.productDTOs.ActiveListingDTO;
import org.example.marketplacebackend.DTO.outgoing.productDTOs.ActiveListingsDTO;
import org.example.marketplacebackend.DTO.outgoing.productDTOs.GetAllSoldProductsResponseDTO;
import org.example.marketplacebackend.DTO.outgoing.productDTOs.GetSoldProductResponseDTO;
import org.example.marketplacebackend.DTO.outgoing.productDTOs.ProductGetAllResponseDTO;
import org.example.marketplacebackend.DTO.outgoing.productDTOs.ProductGetResponseDTO;
import org.example.marketplacebackend.DTO.outgoing.productDTOs.ProductRegisteredResponseDTO;
import org.example.marketplacebackend.model.Account;
import org.example.marketplacebackend.model.Product;
import org.example.marketplacebackend.model.ProductCategory;
import org.example.marketplacebackend.model.ProductImage;
import org.example.marketplacebackend.model.ProductStatus;
import org.example.marketplacebackend.service.CategoryService;
import org.example.marketplacebackend.service.ProductImageService;
import org.example.marketplacebackend.service.ProductService;
import org.example.marketplacebackend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping("v1/products")
@CrossOrigin(origins = {
    "http://localhost:3000, https://marketplace.johros.dev"}, allowCredentials = "true")
@Controller
public class ProductsController {

  private final CategoryService categoryService;
  private final ProductService productService;
  private final UserService userService;
  private final ProductImageService productImageService;
  private final SSEController sseController;

  public ProductsController(CategoryService categoryService,
      ProductService productService,
      UserService userService, ProductImageService productImageService,
      SSEController sseController) {
    this.categoryService = categoryService;
    this.productService = productService;
    this.userService = userService;
    this.productImageService = productImageService;
    this.sseController = sseController;
  }

  @PostMapping("")
  public ResponseEntity<?> uploadProduct(Principal principal,
      @RequestPart(value = "json") ProductDTO product,
      @RequestParam(value = "data") MultipartFile[] files
  ) throws Exception {

    String username = principal.getName();
    Account authenticatedUser = userService.getAccountOrException(username);

    Product productModel = new Product();
    productModel.setName(product.name());

    // User will grab existing product types from a list on the frontend
    ProductCategory productCategoryDB = categoryService.getReferenceById(product.productCategory());
    productModel.setProductCategory(productCategoryDB);

    productModel.setPrice(product.price());
    productModel.setCondition(product.condition());
    productModel.setStatus(ProductStatus.AVAILABLE.ordinal());
    productModel.setDescription(product.description());
    productModel.setSeller(authenticatedUser);
    productModel.setColor(product.color());
    productModel.setProductionYear(product.productionYear());

    // Save all data to DB
    Product productDB = productService.saveProduct(productModel);

    // Upload images and add to product model
    List<ProductImage> uploadedImages;
    try {
      uploadedImages = productImageService.saveFiles(productDB.getId(),
          files);
    } catch (IOException | SdkClientException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    } catch (MaxUploadSizeExceededException e) {
      return ResponseEntity.status(413).build();
    }
    productModel.setProductImages(uploadedImages);

    // Get all image urls from all image objects
    String[] imageUrls = productImageService.productImagesToImageUrls(uploadedImages);

    ProductRegisteredResponseDTO response = new ProductRegisteredResponseDTO(
        productDB.getId(),
        productDB.getName(), productDB.getProductCategory().getId(), productDB.getPrice(),
        productDB.getCondition(),
        productDB.getDescription(), productDB.getSeller().getId(), imageUrls,
        productDB.getColor() != null ? productDB.getColor() : null,
        productDB.getProductionYear() != null ? productDB.getProductionYear() : null
    );

    sseController.pushNewProduct(productDB);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("")
  public ResponseEntity<?> getProducts(
      @RequestParam(name = "category", required = false) String category,
      @RequestParam(name = "minPrice", required = false) Integer minPrice,
      @RequestParam(name = "maxPrice", required = false) Integer maxPrice,
      @RequestParam(name = "condition", required = false) Integer condition,
      @RequestParam(name = "sort", required = false) Integer sort,
      @RequestParam(name = "query", required = false) String query
      ) {
    ProductGetAllResponseDTO products;
    // top 20
    if (category == null && minPrice == null && maxPrice == null && condition == null
        && sort == null && query == null) {
      products = productService.findTop20ByOrderByCreatedAtDesc();
      return ResponseEntity.status(HttpStatus.OK).body(products);
    }

    if (query != null) {
      products = productService.findBySearchQuery(query);
      return ResponseEntity.status(HttpStatus.OK).body(products);
    }

    // condition + category + price + sort
    if (condition != null && category != null && minPrice != null && maxPrice != null
        && sort != null) {
      products = productService.getAllByConditionAndCategoryAndPriceAndSort(condition, category,
          minPrice,
          maxPrice, sort);
      return ResponseEntity.status(HttpStatus.OK).body(products);
    }

    // condition + category + price
    if (condition != null && category != null && minPrice != null && maxPrice != null
        && sort == null) {
      products = productService.getAllByConditionAndCategoryAndPrice(condition, category, minPrice,
          maxPrice);
      return ResponseEntity.status(HttpStatus.OK).body(products);
    }

    // category + price + sort
    if (minPrice != null && maxPrice != null && category != null && sort != null
        && condition == null) {
      if (minPrice > maxPrice) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
      }
      products = productService.getAllByProductPriceAndCategoryAndSort(category, minPrice, maxPrice,
          sort);
      return ResponseEntity.status(HttpStatus.OK).body(products);
    }

    // minPrice + condition + sort
    if (minPrice != null && condition != null && sort != null && category == null
        && maxPrice == null) {
      products = productService.getAllByProductMinPriceAndConditionAndSort(minPrice, condition,
          sort);
      return ResponseEntity.status(HttpStatus.OK).body(products);
    }

    // maxPrice + condition + sort
    if (maxPrice != null && condition != null && sort != null && category == null
        && minPrice == null) {
      products = productService.getAllByProductMaxPriceAndConditionAndSort(maxPrice, condition,
          sort);
      return ResponseEntity.status(HttpStatus.OK).body(products);
    }

    // category + price
    if (minPrice != null && maxPrice != null && category != null && condition == null
        && sort == null) {
      if (minPrice > maxPrice) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
      }
      products = productService.getAllByProductPriceAndCategory(category, minPrice, maxPrice);
      return ResponseEntity.status(HttpStatus.OK).body(products);
    }

    // condition + category + sort
    if (condition != null && category != null && sort != null && maxPrice == null
        && minPrice == null) {
      products = productService.getAllByConditionAndCategoryAndSort(condition, category, sort);
      return ResponseEntity.status(HttpStatus.OK).body(products);
    }

    // condition + category
    if (condition != null && category != null && minPrice == null && maxPrice == null
        && sort == null) {
      products = productService.getAllByConditionAndCategory(condition, category);
      return ResponseEntity.status(HttpStatus.OK).body(products);
    }

    // condition + price + sort
    if (condition != null && minPrice != null && maxPrice != null && sort != null
        && category == null) {
      if (minPrice > maxPrice) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
      }
      products = productService.getAllByConditionAndPriceAndSort(condition, minPrice, maxPrice,
          sort);
      return ResponseEntity.status(HttpStatus.OK).body(products);
    }

    // condition + price
    if (condition != null && minPrice != null && maxPrice != null && category == null
        && sort == null) {
      if (minPrice > maxPrice) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
      }
      products = productService.getAllByConditionAndPrice(condition, minPrice, maxPrice);
      return ResponseEntity.status(HttpStatus.OK).body(products);
    }

    // condition + sort
    if (condition != null && sort != null && category == null && minPrice == null
        && maxPrice == null
    ) {
      products = productService.getAllByConditionAndSort(condition, sort);
      return ResponseEntity.status(HttpStatus.OK).body(products);
    }

    // condition
    if (condition != null && category == null && minPrice == null && maxPrice == null
        && sort == null) {
      products = productService.getAllByCondition(condition);
      return ResponseEntity.status(HttpStatus.OK).body(products);
    }

    // min price
    if (minPrice != null && maxPrice == null && category == null && condition == null
        && sort == null) {
      products = productService.getAllByMinPrice(minPrice);
      return ResponseEntity.status(HttpStatus.OK).body(products);
    }

    // min price + sort
    if (minPrice != null && sort != null && category == null && condition == null
        && maxPrice == null) {
      products = productService.getAllByMinPriceAndSort(minPrice, sort);
      return ResponseEntity.status(HttpStatus.OK).body(products);
    }

    // minPrice + condition
    if (minPrice != null && condition != null && maxPrice == null && category == null
        && sort == null) {
      products = productService.getAllByProductMinPriceAndCondition(minPrice, condition);
      return ResponseEntity.status(HttpStatus.OK).body(products);
    }

    // maxPrice + condition
    if (maxPrice != null && condition != null && minPrice == null && category == null
        && sort == null) {
      products = productService.getAllByProductMaxPriceAndCondition(maxPrice, condition);
      return ResponseEntity.status(HttpStatus.OK).body(products);
    }

    // max price
    if (maxPrice != null && minPrice == null && category == null && condition == null
        && sort == null) {
      products = productService.getAllByMaxPrice(maxPrice);
      return ResponseEntity.status(HttpStatus.OK).body(products);
    }

    // max price + sort
    if (maxPrice != null && sort != null && minPrice == null && category == null
        && condition == null
    ) {
      products = productService.getAllByMaxPriceAndSort(maxPrice, sort);
      return ResponseEntity.status(HttpStatus.OK).body(products);
    }

    // price + sort
    if (minPrice != null && maxPrice != null && sort != null && category == null
        && condition == null) {
      if (minPrice > maxPrice) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
      }
      products = productService.getAllByProductPriceAndSort(minPrice, maxPrice, sort);
      return ResponseEntity.status(HttpStatus.OK).body(products);
    }

    // sort
    if (sort != null && minPrice == null && maxPrice == null && category == null
        && condition == null) {
      products = productService.getAllBySort(sort);
      return ResponseEntity.status(HttpStatus.OK).body(products);
    }

    // price
    if (minPrice != null && maxPrice != null && condition == null && category == null
        && sort == null) {
      if (minPrice > maxPrice) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
      }
      products = productService.getAllByProductPrice(minPrice, maxPrice);
      return ResponseEntity.status(HttpStatus.OK).body(products);
    }

    ProductCategory productCategory = categoryService.findProductCategoryByNameOrNull(category);
    if (productCategory == null) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body("That product category does not exist");
    }

    // category + sort
    if (sort != null && category != null && condition == null && minPrice == null
        && maxPrice == null) {
      products = productService.getAllByProductCategoryAndSort(productCategory, sort);
      return ResponseEntity.status(HttpStatus.OK).body(products);
    }

    // category
    products = productService.getAllByProductCategory(productCategory);
    return ResponseEntity.status(HttpStatus.OK).body(products);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> deleteProduct(Principal principal, @PathVariable UUID id) {
    Account authenticatedUser = userService.getAccountOrException(principal.getName());

    Product product = productService.findProductByIdAndSeller(id,
        authenticatedUser);

    if (product == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    // If there are images we need to delete them first
    if (product.getProductImages() != null) {
      for (ProductImage image : product.getProductImages()) {
        productImageService.deleteImage(image);
      }
    }

    productService.deleteProductOrNull(id);
    return ResponseEntity.status(HttpStatus.OK).body("Product deleted successfully");
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> getProduct(@PathVariable UUID id) {
    Product product = productService.getProductOrNull(id);

    if (product == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    ProductCategory productCategory = product.getProductCategory();
    ProductCategoryDTO productCategoryDTO = new ProductCategoryDTO(productCategory.getId(),
        productCategory.getName());
    ProductGetResponseDTO response = new ProductGetResponseDTO(product.getId(),
        product.getName(), productCategoryDTO, product.getPrice(), product.getCondition(),
        product.getStatus(), product.getDescription(), product.getSeller().getId(),
        product.getBuyer() != null ? product.getBuyer().getId() : null,
        product.getColor(), product.getProductionYear(), product.getCreatedAt(),
        productImageService.productImagesToImageUrls(product.getProductImages())
    );

    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @GetMapping("/my-active-listings")
  public ResponseEntity<?> getMyListings(Principal principal) {
    Account authenticatedUser = userService.getAccountOrException(principal.getName());

    List<Product> activeListings = productService.getActiveListings(authenticatedUser);

    ActiveListingsDTO listings = new ActiveListingsDTO(activeListings
        .stream()
        .map(product -> new ActiveListingDTO(
                product.getId(),
                product.getName(),
                product.getProductCategory().getName(),
                product.getPrice(),
                product.getStatus(),
                product.getCreatedAt(),
                product.getBuyer() != null ?
                    new ProfileResponseDTO(
                        product.getBuyer().getFirstName(),
                        product.getBuyer().getLastName(),
                        product.getBuyer().getUsername())
                    : null
            )
        )
        .toList()
    );

    return ResponseEntity.status(HttpStatus.OK).body(listings);
  }

  @GetMapping("/my-sold-products")
  public ResponseEntity<?> getAllSoldProducts(Principal principal) {
    String username = principal.getName();

    Account authenticatedUser = userService.getAccountOrException(username);
    List<Product> products = productService.getSoldProducts(authenticatedUser);

    List<GetSoldProductResponseDTO> soldProducts = new ArrayList<>();
    for (Product product : products) {
      GetSoldProductResponseDTO soldProduct = new GetSoldProductResponseDTO(
          product.getId(), product.getName(), product.getProductCategory().getName(),
          product.getPrice(), product.getBuyer().getId(), product.getCreatedAt()
      );
      soldProducts.add(soldProduct);
    }
    GetAllSoldProductsResponseDTO response = new GetAllSoldProductsResponseDTO(soldProducts);

    return ResponseEntity.status(HttpStatus.OK).body(response);
  }
}
