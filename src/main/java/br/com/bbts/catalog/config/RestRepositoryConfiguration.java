package br.com.bbts.catalog.config;

import br.com.bbts.catalog.domain.Category;
import br.com.bbts.catalog.domain.Product;
import br.com.bbts.catalog.projection.ProductDetailsProjection;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@Configuration(proxyBeanMethods = false)
public class RestRepositoryConfiguration implements RepositoryRestConfigurer {

	@Override
	public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {
		config.setBasePath("/api/v1");
		config.setDefaultPageSize(20);
		config.setMaxPageSize(100);
		config.setRepositoryDetectionStrategy(RepositoryDetectionStrategy.RepositoryDetectionStrategies.ANNOTATED);
		config.exposeIdsFor(Category.class, Product.class);
		config.getProjectionConfiguration().addProjection(ProductDetailsProjection.class);
		config.useHalAsDefaultJsonMediaType(true);

		config.getExposureConfiguration()
				.withCollectionExposure((metadata, methods) -> methods.disable(
						HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE))
				.withItemExposure((metadata, methods) -> methods.disable(
						HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE));
	}

}
