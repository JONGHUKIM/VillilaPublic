package com.splusz.villigo;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class VilligoApplication {

	public static void main(String[] args) {
//	    Dotenv dotenv = Dotenv.configure()
//	            .ignoreIfMissing()
//	            .load();
//
//	    // DB
//	    System.setProperty("DB_URL", dotenv.get("DB_URL"));
//	    System.setProperty("DB_USERNAME", dotenv.get("DB_USERNAME"));
//	    System.setProperty("DB_PASSWORD", dotenv.get("DB_PASSWORD"));
//
//	    // 구글 OAuth
//	    System.setProperty("GOOGLE_CLIENT_ID", dotenv.get("GOOGLE_CLIENT_ID"));
//	    System.setProperty("GOOGLE_CLIENT_SECRET", dotenv.get("GOOGLE_CLIENT_SECRET"));
//
//	    // 카카오 REST API
//	    System.setProperty("KAKAO_RESTAPI", dotenv.get("KAKAO_RESTAPI"));
//
//	    // 파일 업로드 경로
//	    System.setProperty("UPLOAD_PATH", dotenv.get("UPLOAD_PATH"));

	    SpringApplication.run(VilligoApplication.class, args);
	}

}
