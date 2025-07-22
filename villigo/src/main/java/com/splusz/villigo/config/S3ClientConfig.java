package com.splusz.villigo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3ClientConfig {
	
    @Value("${aws.s3.region}")
    private String region; // application.properties에서 AWS_S3_REGION 값을 주입받도록 변경

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.AP_NORTHEAST_2) // 서울 리전
                .credentialsProvider(DefaultCredentialsProvider.create()) // EC2 Role 기반 인증
                .build();
    }
}
