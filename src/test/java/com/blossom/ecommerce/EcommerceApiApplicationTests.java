package com.blossom.ecommerce;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class EcommerceApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
