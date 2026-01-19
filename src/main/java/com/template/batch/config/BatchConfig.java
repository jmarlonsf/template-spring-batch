package com.template.batch.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class BatchConfig {
    // Spring Boot 3.x já faz autoconfiguração do Spring Batch automaticamente
    // Não precisa de @EnableBatchProcessing - isso desativa a autoconfiguração
}
