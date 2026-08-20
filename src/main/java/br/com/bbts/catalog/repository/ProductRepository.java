package br.com.bbts.catalog.repository;

import java.util.Optional;

import br.com.bbts.catalog.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@RepositoryRestResource(path = "products", collectionResourceRel = "products", itemResourceRel = "product")
public interface ProductRepository extends JpaRepository<Product, Long> {

	@Override
	@EntityGraph(attributePaths = "category")
	Page<Product> findAll(Pageable pageable);

	@Override
	@EntityGraph(attributePaths = "category")
	Optional<Product> findById(Long id);

	@EntityGraph(attributePaths = "category")
	@RestResource(path = "by-name", rel = "by-name")
	Page<Product> findByNameContainingIgnoreCase(@Param("name") String name, Pageable pageable);

	@EntityGraph(attributePaths = "category")
	@RestResource(path = "by-active", rel = "by-active")
	Page<Product> findByActive(@Param("active") boolean active, Pageable pageable);

	@EntityGraph(attributePaths = "category")
	@RestResource(path = "by-category", rel = "by-category")
	Page<Product> findByCategoryCodeIgnoreCase(@Param("code") String code, Pageable pageable);

}
