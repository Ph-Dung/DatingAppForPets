package com.petmatch.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@org.springframework.context.annotation.Bean
	public org.springframework.boot.CommandLineRunner fixDbConstraint(
			org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
		return args -> {
			try {
				jdbcTemplate.execute("ALTER TABLE messages DROP CONSTRAINT IF EXISTS messages_type_check");
				jdbcTemplate.execute("ALTER TABLE messages DROP CONSTRAINT IF EXISTS messages_type_check1");
				System.out.println("Đã xoá bỏ giới hạn enum cũ trên bảng messages thành công!");
			} catch (Exception e) {
				System.out.println("Không thể xoá constraint tự động: " + e.getMessage());
			}
		};
	}
}
