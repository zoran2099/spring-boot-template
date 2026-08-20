package br.com.bbts.catalog;

import org.springframework.boot.SpringApplication;

public class TestCatalogApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(CatalogApiApplication::main)
				.with(TestcontainersConfiguration.class)
				.run(args);
	}

}
