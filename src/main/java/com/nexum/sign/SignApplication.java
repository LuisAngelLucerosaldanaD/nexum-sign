package com.nexum.sign;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SignApplication {

	public static void main(String[] args) {
		SpringApplication.run(SignApplication.class, args);
	}

	@Bean
	public OpenAPI springShopOpenAPI() {
		return new OpenAPI()
				.info(new Info().title("Nexum API Sign")
						.description("API que permite firmar eletrónicamente los documentos generados por el sistema NEXUM")
						.version("v0.0.1")
						.license(new License().name("Licencia").url("https://www.bjungle.net")))
				.externalDocs(new ExternalDocumentation()
						.description("Información adicional")
						.url("https://www.bjungle.net"));
	}

}
