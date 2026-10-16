package com.blossom.ecommerce;

import org.springframework.boot.SpringApplication;

public class TestEcommerceApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(EcommerceApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
