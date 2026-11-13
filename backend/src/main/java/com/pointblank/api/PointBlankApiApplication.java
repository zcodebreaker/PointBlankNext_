package com.pointblank.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.pointblank.api.config.AppProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class PointBlankApiApplication {

	/**
	 * Inicializa a aplicação Spring Boot.
	 *
	 * @param args argumentos recebidos pela linha de comando
	 */
	public static void main(String[] args) {
		SpringApplication.run(PointBlankApiApplication.class, args);
	}

}
