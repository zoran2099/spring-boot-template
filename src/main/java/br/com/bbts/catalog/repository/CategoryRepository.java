package br.com.bbts.catalog.repository;

import br.com.bbts.catalog.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "categories", collectionResourceRel = "categories", itemResourceRel = "category")
public interface CategoryRepository extends JpaRepository<Category, Long> {
}
