package br.com.bbts.catalog.config;

import java.util.List;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

	private static final String BASIC_AUTH = "basicAuth";

	@Bean
	OpenAPI catalogOpenApi() {
		Paths paths = new Paths()
				.addPathItem("/api/v1/products", get("Lista produtos", page(), size(), sort(), projection()))
				.addPathItem("/api/v1/products/{id}", get("Consulta um produto", id(), projection()))
				.addPathItem("/api/v1/products/search/by-name", get("Busca produtos pelo nome", query("name"), page(), size(), sort()))
				.addPathItem("/api/v1/products/search/by-active", get("Busca produtos pelo status", query("active"), page(), size(), sort()))
				.addPathItem("/api/v1/products/search/by-category", get("Busca produtos pelo código da categoria", query("code"), page(), size(), sort()))
				.addPathItem("/api/v1/categories", get("Lista categorias", page(), size(), sort()))
				.addPathItem("/api/v1/categories/{id}", get("Consulta uma categoria", id()));

		return new OpenAPI()
				.info(new Info()
						.title("Catalog API")
						.description("API de referência de catálogo, somente leitura e baseada em HAL")
						.version("v1"))
				.components(new Components().addSecuritySchemes(BASIC_AUTH, new SecurityScheme()
						.type(SecurityScheme.Type.HTTP)
						.scheme("basic")))
				.addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH))
				.paths(paths);
	}

	private PathItem get(String summary, Parameter... parameters) {
		Operation operation = new Operation()
				.summary(summary)
				.tags(List.of("Catalog"))
				.responses(new ApiResponses()
						.addApiResponse("200", new ApiResponse().description("Consulta realizada"))
						.addApiResponse("401", new ApiResponse().description("Credenciais ausentes ou inválidas"))
						.addApiResponse("403", new ApiResponse().description("Acesso negado")))
				.addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH));

		for (Parameter parameter : parameters) {
			operation.addParametersItem(parameter);
		}

		return new PathItem().get(operation);
	}

	private Parameter id() {
		return new Parameter().name("id").in("path").required(true).description("Identificador do recurso");
	}

	private Parameter query(String name) {
		return new Parameter().name(name).in("query").required(true);
	}

	private Parameter page() {
		return new Parameter().name("page").in("query").description("Página baseada em zero");
	}

	private Parameter size() {
		return new Parameter().name("size").in("query").description("Itens por página, máximo 100");
	}

	private Parameter sort() {
		return new Parameter().name("sort").in("query").description("Campo e direção, por exemplo name,asc");
	}

	private Parameter projection() {
		return new Parameter().name("projection").in("query").description("Use productDetails para enriquecer produtos");
	}

}
