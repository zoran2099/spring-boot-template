package br.com.bbts.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import br.com.bbts.catalog.domain.Category;
import br.com.bbts.catalog.domain.Product;
import br.com.bbts.catalog.repository.CategoryRepository;
import br.com.bbts.catalog.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ProductRepositoryIntegrationTests {

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private ProductRepository productRepository;

	private Category hardware;

	@BeforeEach
	void setUp() {
		hardware = categoryRepository.saveAndFlush(new Category("HARDWARE", "Hardware"));
		Category books = categoryRepository.saveAndFlush(new Category("BOOKS", "Books"));

		productRepository.save(new Product(
				"NOTEBOOK-01", "Notebook Pro", "Notebook corporativo", new BigDecimal("7500.00"), true, hardware));
		productRepository.save(new Product(
				"MOUSE-01", "Mouse sem fio", "Mouse ergonômico", new BigDecimal("180.00"), false, hardware));
		productRepository.save(new Product(
				"BOOK-01", "Architecture Patterns", "Livro técnico", new BigDecimal("250.00"), true, books));
		productRepository.flush();
	}

	@Test
	void filtersAndSortsProducts() {
		var byName = productRepository.findByNameContainingIgnoreCase(
				"note", PageRequest.of(0, 20, Sort.by("name").ascending()));
		var byActive = productRepository.findByActive(true, PageRequest.of(0, 20));
		var byCategory = productRepository.findByCategoryCodeIgnoreCase("hardware", PageRequest.of(0, 20));

		assertThat(byName).extracting(Product::getSku).containsExactly("NOTEBOOK-01");
		assertThat(byActive).extracting(Product::getSku).containsExactlyInAnyOrder("NOTEBOOK-01", "BOOK-01");
		assertThat(byCategory).extracting(Product::getSku).containsExactlyInAnyOrder("NOTEBOOK-01", "MOUSE-01");
	}

	@Test
	void rejectsDuplicateSku() {
		assertThatThrownBy(() -> productRepository.saveAndFlush(new Product(
				"NOTEBOOK-01", "Duplicated", null, BigDecimal.ONE, true, hardware)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

}
