package com.example.secure.share;

import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.error.ErrorAttributeOptions.Include;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.request.WebRequest;

@SpringBootApplication
public class SecureShareApplication {

	public static void main(String[] args) {
		SpringApplication.run(SecureShareApplication.class, args);
	}

	@Bean
	public ErrorAttributes errorAttributes() {
		return new DefaultErrorAttributes() {

			public Map<String, Object> getErrorAttributes(WebRequest request, ErrorAttributeOptions options) {
				Map<String, Object> errorAttributes = super.getErrorAttributes(request,
						options.excluding(Include.STACK_TRACE));
				// Customize the default entries in errorAttributes to suit your needs
				return errorAttributes;
			}

		};
	}

}
