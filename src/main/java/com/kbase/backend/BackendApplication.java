package com.kbase.backend;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		// Ép JVM sử dụng múi giờ UTC hoặc Asia/Ho_Chi_Minh trước khi nạp Spring context
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

		SpringApplication.run(BackendApplication.class, args);
	}

}