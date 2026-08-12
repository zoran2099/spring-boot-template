package com.example.template;

import org.springframework.boot.SpringApplication;

public class TestSpringBootTemplateApplication {

	public static void main(String[] args) {
		SpringApplication.from(SpringBootTemplateApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
