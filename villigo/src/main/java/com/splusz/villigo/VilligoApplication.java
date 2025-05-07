package com.splusz.villigo;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class VilligoApplication {

    public static void main(String[] args) {
        // .env 파일 로딩
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing() // .env 없을 경우 무시
                .load();

        // 환경변수 → 시스템 프로퍼티로 설정 (Spring Boot가 인식하게)
        System.setProperty("DB_URL", dotenv.get("DB_URL"));
        System.setProperty("DB_USERNAME", dotenv.get("DB_USERNAME"));
        System.setProperty("DB_PASSWORD", dotenv.get("DB_PASSWORD"));

        SpringApplication.run(VilligoApplication.class, args);
    }
}
