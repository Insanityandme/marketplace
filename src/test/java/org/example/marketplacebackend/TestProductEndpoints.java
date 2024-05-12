package org.example.marketplacebackend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.marketplacebackend.DTO.incoming.ProductDTO;
import org.example.marketplacebackend.service.ProductImageService;
import org.example.marketplacebackend.service.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class TestProductEndpoints {
  @Container
  private static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>(
      "postgres:16-alpine"
  )
      .withInitScript("schema.sql");
  @DynamicPropertySource
  private static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", DB::getJdbcUrl);
    registry.add("spring.datasource.username", DB::getUsername);
    registry.add("spring.datasource.password", DB::getPassword);
  }

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Autowired
  private ProductService productService;

  @Autowired
  private ProductImageService productImageService;

  private ArrayList<String> imageUrlsStrings;

  @BeforeEach
  public void setup() {
    // init MockMvc Object and build
    imageUrlsStrings = new ArrayList<>();
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
  }

  @Test
  @Sql(executionPhase = ExecutionPhase.BEFORE_TEST_METHOD,
      statements = """
      INSERT INTO account (id, first_name, last_name, date_of_birth, email, password, username) VALUES ('3a45dc5e-2a30-41ba-b488-ca4b113ea5ee', 'Ken', 'Thompson', '1943-02-04', 'ken@example.com', '$2a$10$gIwb60Eio1J1UYWqCrV4je9kAzsqra0kzwg5fcKRCauzGUQ2xmx3q', 'ken');
      INSERT INTO product_category VALUES ('d5509745-450f-4760-8bdd-ddc88d376b37', 'electronics');
      """)
  @Sql(executionPhase = ExecutionPhase.AFTER_TEST_METHOD,
      statements = """
      DELETE FROM account WHERE id = '3a45dc5e-2a30-41ba-b488-ca4b113ea5ee';
      DELETE FROM product_category WHERE id = 'd5509745-450f-4760-8bdd-ddc88d376b37';
      """)
  public void uploadProductSuccess() throws Exception {
    UUID productCategory = UUID.fromString("d5509745-450f-4760-8bdd-ddc88d376b37");
    ProductDTO product = new ProductDTO("test", productCategory,
        500, 0, "wow amazing",
        null, null);

    ObjectMapper objectMapper = new ObjectMapper();
    String json = objectMapper.writeValueAsString(product);
    Resource file = new ClassPathResource("test_data/test.jpg");
    Resource file2 = new ClassPathResource("test_data/test2.jpg");

    MockMultipartFile jsonProduct = new MockMultipartFile("json", "", "application/json",
        json.getBytes());
    MockMultipartFile imageFile = new MockMultipartFile("data", file.getFilename(),
        "multipart/form-data", file.getContentAsByteArray());
    MockMultipartFile imageFile2 = new MockMultipartFile("data", file2.getFilename(),
        "multipart/form-data", file2.getContentAsByteArray());

    ResultActions createProduct = mockMvc.perform(MockMvcRequestBuilders.multipart("/v1/products")
        .file(jsonProduct)
        .file(imageFile)
        .file(imageFile2)
        .principal(() -> "ken")
    );

    String response = createProduct
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode jsonNode = objectMapper.readTree(response);
    JsonNode imageUrls = jsonNode.get("imageUrls");

    for (int i = 0; i < imageUrls.size(); i++) {
      imageUrlsStrings.add(i, imageUrls.get(i).toString());
    }
  }

  @Test
  void getAllProducts() throws Exception {
    ResultActions getProducts = mockMvc.perform(get("/v1/products"));

    getProducts
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }

  @Test
  @Sql(executionPhase = ExecutionPhase.BEFORE_TEST_METHOD,
      statements = """
      INSERT INTO account (id, first_name, last_name, date_of_birth, email, password, username) VALUES ('3a45dc5e-2a30-41ba-b488-ca4b113ea5ee', 'Ken', 'Thompson', '1943-02-04', 'ken@example.com', '$2a$10$gIwb60Eio1J1UYWqCrV4je9kAzsqra0kzwg5fcKRCauzGUQ2xmx3q', 'ken');
      INSERT INTO product_category (id, name) VALUES ('4205756e-6f31-4c34-b8ed-52aca7a64fbf', 'kebab');
      INSERT INTO product (id, name, product_category, price, condition, is_purchased, description, seller, buyer, color, production_year) VALUES ('3ce17658-9107-4154-9ead-e22c5d6508a5', 'name' ,'4205756e-6f31-4c34-b8ed-52aca7a64fbf', 500, 0, false, 'description', '3a45dc5e-2a30-41ba-b488-ca4b113ea5ee', null, 0, 2024);
      """)
  @Sql(executionPhase = ExecutionPhase.AFTER_TEST_METHOD,
      statements = """
      DELETE FROM product WHERE id = '3ce17658-9107-4154-9ead-e22c5d6508a5';
      DELETE FROM product_category WHERE id = '4205756e-6f31-4c34-b8ed-52aca7a64fbf';
      DELETE FROM account WHERE id = '3a45dc5e-2a30-41ba-b488-ca4b113ea5ee';
      """)
  public void getAllProductsByCategorySuccess() throws Exception {
    ResultActions getProducts = mockMvc.perform(get("/v1/products?category=kebab"));

    String response = getProducts
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andReturn().getResponse().getContentAsString();
    System.out.println(response);
  }

  @Test
  void getAllProductsByCategoryFail() throws Exception {
    ResultActions getProducts = mockMvc.perform(get("/v1/products?category=asdasdasdasdasd"));

    getProducts.andExpect(status().isBadRequest());
  }

  @Test
  @Sql(executionPhase = ExecutionPhase.BEFORE_TEST_METHOD,
      statements = """
      INSERT INTO account (id, first_name, last_name, date_of_birth, email, password, username) VALUES ('3a45dc5e-2a30-41ba-b488-ca4b113ea5ee', 'Ken', 'Thompson', '1943-02-04', 'ken@example.com', '$2a$10$gIwb60Eio1J1UYWqCrV4je9kAzsqra0kzwg5fcKRCauzGUQ2xmx3q', 'ken');
      INSERT INTO product_category (id, name) VALUES ('4205756e-6f31-4c34-b8ed-52aca7a64fbf', 'kebab');
""")
  @Sql(executionPhase = ExecutionPhase.AFTER_TEST_METHOD,
      statements = """
      DELETE FROM product_category;
      DELETE FROM product;
      DELETE FROM account;
      """)
  public void deleteProductSuccess() throws Exception {
    String responseCreateProduct = Utils.createProduct(mockMvc,
        "4205756e-6f31-4c34-b8ed-52aca7a64fbf", "ken");

    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(responseCreateProduct);
    JsonNode imageUrls = jsonNode.get("imageUrls");
    for (int i = 0; i < imageUrls.size(); i++) {
      imageUrlsStrings.add(i, imageUrls.get(i).toString());
    }

    String id = jsonNode.get("id").toString();
    String endPoint = "/v1/products/" + id.substring(1, id.length() - 1);

    ResultActions getProducts = mockMvc.perform(delete(endPoint)
        .principal(() -> "ken"));
    getProducts.andExpect(status().isOk());
  }

  @Test
  @Sql(executionPhase = ExecutionPhase.BEFORE_TEST_METHOD,
      statements = """
      INSERT INTO account (id, first_name, last_name, date_of_birth, email, password, username) VALUES ('3a45dc5e-2a30-41ba-b488-ca4b113ea5ee', 'Ken', 'Thompson', '1943-02-04', 'ken@example.com', '$2a$10$gIwb60Eio1J1UYWqCrV4je9kAzsqra0kzwg5fcKRCauzGUQ2xmx3q', 'ken');
      INSERT INTO product_category (id, name) VALUES ('4205756e-6f31-4c34-b8ed-52aca7a64fbf', 'kebab');
      INSERT INTO product (id, name, product_category, price, condition, is_purchased, description, seller, buyer, color, production_year) VALUES ('798bdcaf-03c7-4fec-8b87-482ed7cac83d', 'name' ,'4205756e-6f31-4c34-b8ed-52aca7a64fbf', 500, 0, false, 'description', '3a45dc5e-2a30-41ba-b488-ca4b113ea5ee', null, 0, 2024);
      """)
  @Sql(executionPhase = ExecutionPhase.AFTER_TEST_METHOD,
      statements = """
      DELETE FROM product WHERE id = '798bdcaf-03c7-4fec-8b87-482ed7cac83d';
      DELETE FROM product_category WHERE id = '4205756e-6f31-4c34-b8ed-52aca7a64fbf';
      DELETE FROM account WHERE id = '3a45dc5e-2a30-41ba-b488-ca4b113ea5ee';
      """)
  public void getProductByIdSuccessful() throws Exception {
    String id = "798bdcaf-03c7-4fec-8b87-482ed7cac83d";
    String endPoint = "/v1/products/" + id;

    ResultActions getProducts = mockMvc.perform(get(endPoint));
    getProducts
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }

  @Test
  @WithMockUser
  public void getProductByIdFail() throws Exception {
    UUID id = UUID.randomUUID();
    String endPoint = "/v1/products/" + id;

    ResultActions getProducts = mockMvc.perform(get(endPoint));
    getProducts.andExpect(status().isNotFound());
  }

  @AfterEach
  void tearDown() {
    // NOTE: Remove all code in here to check the actual values in the DB
    // and the images in src/main/resources/images/
    for (String url : imageUrlsStrings) {
      String cleanUrl = url.substring(1, url.length() - 1);
      File file = new File("src/main/resources/images/" + cleanUrl);
      if (file.delete()) {
        System.out.println("Deleted the file: " + file.getName());
        productImageService.deleteProductImageByImageUrl(cleanUrl);
      } else {
        System.out.println("Failed to delete the file: " + file.getName());
      }
    }

    productService.deleteByName("test");
  }

}
