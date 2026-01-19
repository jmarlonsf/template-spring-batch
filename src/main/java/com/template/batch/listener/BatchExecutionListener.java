package com.template.batch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;


@Component
public class BatchExecutionListener implements JobExecutionListener, StepExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(BatchExecutionListener.class);

    // ========== JobExecutionListener ==========

    @Override
    public void beforeJob(JobExecution jobExecution) {
        logger.info("=========================================");
        logger.info("Job iniciado: {}", jobExecution.getJobInstance().getJobName());
        logger.info("Job Instance ID: {}", jobExecution.getJobInstance().getInstanceId());
        logger.info("Job Execution ID: {}", jobExecution.getId());
        logger.info("=========================================");
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        long durationMs = 0;
        if (jobExecution.getEndTime() != null && jobExecution.getStartTime() != null) {
            LocalDateTime startTime = jobExecution.getStartTime();
            LocalDateTime endTime = jobExecution.getEndTime();
            durationMs = Duration.between(startTime, endTime).toMillis();
        }
        
        logger.info("=========================================");
        logger.info("Job finalizado: {}", jobExecution.getJobInstance().getJobName());
        logger.info("Status: {}", jobExecution.getStatus());
        logger.info("Exit Status: {}", jobExecution.getExitStatus());
        logger.info("Tempo de execução: {} ms", durationMs);
        logger.info("=========================================");
    }

    // ========== StepExecutionListener ==========

    @Override
    public void beforeStep(StepExecution stepExecution) {
        logger.info("--- Step iniciado: {} ---", stepExecution.getStepName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        long itensProcessados = stepExecution.getReadCount() - stepExecution.getReadSkipCount();
        
        logger.info("--- Step finalizado: {} ---", stepExecution.getStepName());
        logger.info("  Itens lidos: {}", stepExecution.getReadCount());
        logger.info("  Itens processados: {}", itensProcessados);
        logger.info("  Itens escritos: {}", stepExecution.getWriteCount());
        logger.info("  Itens ignorados (skip): {}", stepExecution.getSkipCount());
        logger.info("  Status: {}", stepExecution.getStatus());
        
        return stepExecution.getExitStatus();
    }
}
