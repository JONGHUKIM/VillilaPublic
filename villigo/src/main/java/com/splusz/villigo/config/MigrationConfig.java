package com.splusz.villigo.config;

import com.splusz.villigo.service.ChatService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MigrationConfig {

    @Bean
    public CommandLineRunner runMigration(ChatService chatService) {
        return args -> {
            chatService.migrateReadByData();
        };
    }
}