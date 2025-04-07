package com.splusz.villigo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class VilligoApplication {

	public static void main(String[] args) {
		SpringApplication.run(VilligoApplication.class, args);
	}

}
