package com.template.batch.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class KeepAliveRunner implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=========================================");
        System.out.println("Aplicação Spring Batch iniciada!");
        System.out.println("Console H2 disponível em: http://localhost:8080/h2-console");
        System.out.println("JDBC URL: jdbc:h2:mem:batchdb");
        System.out.println("User: sa | Password: (vazio)");
        System.out.println("=========================================");
        System.out.println("Aplicação rodando... Pressione Ctrl+C para encerrar.");
    }
}
