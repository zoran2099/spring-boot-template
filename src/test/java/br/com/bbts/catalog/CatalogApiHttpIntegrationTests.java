package br.com.bbts.catalog;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import br.com.bbts.catalog.domain.Category;
import br.com.bbts.catalog.domain.Product;
import br.com.bbts.catalog.repository.CategoryRepository;
import br.com.bbts.catalog.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"app.security.reader.username=reader",
		"app.security.reader.password=reader-password",
		"app.security.admin.username=admin",
		"app.security.admin.password=admin-password",
		"app.cors.allowed-origins=http://frontend.example"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CatalogApiHttpIntegrationTests {
	private static final String ALLOWED_ORIGIN = "http://frontend.example";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private ProductRepository productRepository;

	private Product notebook;
	private Category hardware;

	@BeforeEach
	void setUp() {
		productRepository.deleteAll();
		categoryRepository.deleteAll();

		hardware = categoryRepository.save(new Category("HARDWARE", "Hardware"));
		Category books = categoryRepository.save(new Category("BOOKS", "Books"));
		notebook = productRepository.save(new Product(
				"NOTEBOOK-01", "Notebook Pro", "Notebook corporativo", new BigDecimal("7500.00"), true, hardware));
		productRepository.save(new Product(
				"BOOK-01", "Architecture Patterns", "Livro técnico", new BigDecimal("250.00"), false, books));
	}

	@Test
	void requiresAuthenticationForCatalog() throws Exception {
		mockMvc.perform(get("/api/v1/products"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void allowsCorsPreflightAndAuthenticatedRequestForConfiguredOrigin() throws Exception {
		mockMvc.perform(options("/api/v1/products")
					.header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
					.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
					.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));

		mockMvc.perform(get("/api/v1/products")
					.header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
					.with(httpBasic("reader", "reader-password")))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
	}

	@Test
	void rejectsCorsPreflightForUnconfiguredOrigin() throws Exception {
		mockMvc.perform(options("/api/v1/products")
					.header(HttpHeaders.ORIGIN, "https://untrusted.example")
					.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	void returnsPagedSortedHalAndProjectionToReader() throws Exception {
		mockMvc.perform(get("/api/v1/products")
						.param("page", "0")
						.param("size", "1")
						.param("sort", "name,asc")
						.with(httpBasic("reader", "reader-password")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.products[0].name").value("Architecture Patterns"))
				.andExpect(jsonPath("$.page.size").value(1))
				.andExpect(jsonPath("$.page.totalElements").value(2));

		mockMvc.perform(get("/api/v1/products/{id}", notebook.getId())
						.param("projection", "productDetails")
						.with(httpBasic("reader", "reader-password")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sku").value("NOTEBOOK-01"))
				.andExpect(jsonPath("$.category.code").value("HARDWARE"))
				.andExpect(jsonPath("$.category.name").value("Hardware"));
	}

	@Test
	void exposesReadOnlyFilters() throws Exception {
		mockMvc.perform(get("/api/v1/products/search/by-name")
						.param("name", "note")
						.with(httpBasic("reader", "reader-password")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.products[0].sku").value("NOTEBOOK-01"));

		mockMvc.perform(get("/api/v1/products/search/by-active")
						.param("active", "false")
						.with(httpBasic("reader", "reader-password")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.products[0].sku").value("BOOK-01"));

		mockMvc.perform(get("/api/v1/products/search/by-category")
						.param("code", "hardware")
						.with(httpBasic("reader", "reader-password")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.products[0].sku").value("NOTEBOOK-01"));
	}

	@Test
	void rejectsWritesEvenForAdmin() throws Exception {
		mockMvc.perform(post("/api/v1/products")
						.with(httpBasic("admin", "admin-password"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "sku": "NEW-01",
								  "name": "New product",
								  "description": "Must not be created",
								  "price": 10.00,
								  "active": true,
								  "category": "/api/v1/categories/%d"
								}
								""".formatted(hardware.getId())))
				.andExpect(status().isMethodNotAllowed());
	}

	@Test
	void appliesOperationalAccessRules() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk());

		mockMvc.perform(get("/actuator/metrics")
						.with(httpBasic("reader", "reader-password")))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/actuator/metrics")
						.with(httpBasic("admin", "admin-password")))
				.andExpect(status().isOk());
	}

	@Test
	void protectsAndPublishesOpenApiWithBasicScheme() throws Exception {
		mockMvc.perform(get("/v3/api-docs")
						.with(httpBasic("reader", "reader-password")))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/v3/api-docs")
						.with(httpBasic("admin", "admin-password")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.components.securitySchemes.basicAuth.type").value("http"))
				.andExpect(jsonPath("$.components.securitySchemes.basicAuth.scheme").value("basic"))
				.andExpect(jsonPath("$.paths['/api/v1/products'].get").exists())
				.andExpect(jsonPath("$.paths['/api/v1/products'].post").doesNotExist());
	}

}
