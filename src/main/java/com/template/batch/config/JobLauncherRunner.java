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
            
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();
            
            jobLauncher.run(job, jobParameters);
        } else {
            logger.info("Nenhum job especificado. Aplicação será encerrada.");
        }
    }
}
