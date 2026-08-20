package br.com.bbts.catalog.projection;

import java.math.BigDecimal;

import br.com.bbts.catalog.domain.Product;
import org.springframework.data.rest.core.config.Projection;

@Projection(name = "productDetails", types = Product.class)
public interface ProductDetailsProjection {

	Long getId();

	String getSku();

	String getName();

	String getDescription();

	BigDecimal getPrice();

	boolean isActive();

	CategorySummary getCategory();

	interface CategorySummary {

		String getCode();

		String getName();

	}

}
