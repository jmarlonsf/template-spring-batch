package com.template.batch.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class JobLauncherRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(JobLauncherRunner.class);

    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;

    @Value("${spring.batch.job.name:}")
    private String jobName;

    @Value("${spring.batch.job.processDate:}")
    private String processDateProperty;

    public JobLauncherRunner(JobLauncher jobLauncher, JobRegistry jobRegistry) {
        this.jobLauncher = jobLauncher;
        this.jobRegistry = jobRegistry;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("JobLauncherRunner executado. jobName: '{}'", jobName);
        
        if (jobName != null && !jobName.isEmpty()) {
            logger.info("Buscando job '{}' no JobRegistry...", jobName);
            Job job = jobRegistry.getJob(jobName);
            logger.info("Job '{}' encontrado. Iniciando execução...", jobName);
            
            // Extrai parâmetro de data: primeiro tenta propriedade, depois argumentos
            String processDate = processDateProperty != null && !processDateProperty.isEmpty() 
                ? processDateProperty 
                : extractProcessDateFromArgs(args);
            
            JobParametersBuilder jobParametersBuilder = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis());
            
            // Adiciona parâmetro de data se fornecido
            if (processDate != null && !processDate.isEmpty()) {
                logger.info("Parâmetro de data encontrado: '{}' (formato yyyyMMdd)", processDate);
                // Tenta adicionar como Long primeiro (se for número)
                try {
                    Long dateAsLong = Long.parseLong(processDate);
                    jobParametersBuilder.addLong("processDate", dateAsLong);
                    logger.debug("Data adicionada como Long: {}", dateAsLong);
                } catch (NumberFormatException e) {
                    // Se não for número, adiciona como String
                    jobParametersBuilder.addString("processDate", processDate);
                    logger.debug("Data adicionada como String: {}", processDate);
                }
            } else {
                logger.info("Parâmetro de data não fornecido. Usará LocalDateTime.now() no processamento.");
            }
            
            JobParameters jobParameters = jobParametersBuilder.toJobParameters();
            jobLauncher.run(job, jobParameters);
        } else {
            logger.info("Nenhum job especificado. Aplicação será encerrada.");
        }
    }

    /**
     * Extrai o parâmetro de data dos argumentos da linha de comando
     * Formato esperado: --processDate=yyyyMMdd ou --processDate yyyyMMdd
     * 
     * @param args Argumentos da linha de comando
     * @return Data no formato yyyyMMdd ou null se não encontrado
     */
    private String extractProcessDateFromArgs(String... args) {
        if (args == null || args.length == 0) {
            return null;
        }

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            
            // Formato: --processDate=yyyyMMdd
            if (arg.startsWith("--processDate=")) {
                return arg.substring("--processDate=".length());
            }
            
            // Formato: --processDate yyyyMMdd
            if (arg.equals("--processDate") && i + 1 < args.length) {
                return args[i + 1];
            }
        }

        return null;
    }
}
