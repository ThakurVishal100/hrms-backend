package com.hrms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EntityScan(basePackages = "com.hrms.entity")
@EnableJpaRepositories(basePackages = "com.hrms.repository")
@ComponentScan(basePackages = "com.hrms")
@EnableScheduling
public class HrmsApplication {

	public static void main(String[] args) {
		SpringApplication.run(HrmsApplication.class, args);
	}

	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		// Register JavaTimeModule to handle LocalDate/LocalDateTime automatically
		mapper.registerModule(new JavaTimeModule());
		return mapper;
	}
}

