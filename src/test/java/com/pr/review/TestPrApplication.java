package com.pr.review;

import org.springframework.boot.SpringApplication;

public class TestPrApplication {

	public static void main(String[] args) {
		SpringApplication.from(PrApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
