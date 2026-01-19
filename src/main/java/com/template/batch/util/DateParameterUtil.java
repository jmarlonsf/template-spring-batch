package com.template.batch.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobParameters;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utilitário para converter parâmetro de data do JobParameters em LocalDateTime
 * 
 * SUPORTA:
 * - Formato: yyyyMMdd (ex: 20260119)
 * - Pode ser String ou Long
 * - Se não fornecido, retorna null (para usar LocalDateTime.now())
 */
public class DateParameterUtil {

    private static final Logger logger = LoggerFactory.getLogger(DateParameterUtil.class);
    private static final String DATE_PARAMETER_NAME = "processDate";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Extrai a data do JobParameters e converte para LocalDateTime
     * 
     * @param jobParameters Parâmetros do job
     * @return LocalDateTime no início do dia (00:00:00) ou null se não fornecido
     */
    public static LocalDateTime getProcessDate(JobParameters jobParameters) {
        if (jobParameters == null) {
            logger.debug("JobParameters é null, retornando null (usará LocalDateTime.now())");
            return null;
        }

        // Tenta obter como Long primeiro (JobLauncherRunner adiciona como Long quando é número)
        Long dateLong = jobParameters.getLong(DATE_PARAMETER_NAME);
        if (dateLong != null) {
            try {
                String dateStringFromLong = String.valueOf(dateLong);
                LocalDate date = LocalDate.parse(dateStringFromLong, DATE_FORMATTER);
                LocalDateTime dateTime = date.atStartOfDay();
                logger.debug("Data extraída do parâmetro '{}' (Long): {} -> {}", 
                    DATE_PARAMETER_NAME, dateLong, dateTime);
                return dateTime;
            } catch (DateTimeParseException e) {
                logger.warn("Erro ao parsear data '{}' (Long) do parâmetro '{}': {}. Retornando null.", 
                    dateLong, DATE_PARAMETER_NAME, e.getMessage());
                return null;
            } catch (IllegalArgumentException e) {
                // Se não for Long, continua para tentar como String
                logger.debug("Parâmetro '{}' não é Long, tentando como String...", DATE_PARAMETER_NAME);
            }
        }

        // Tenta obter como String (se não foi Long ou se foi fornecido como String)
        // getString() lança IllegalArgumentException se o parâmetro não for String
        try {
            String dateString = jobParameters.getString(DATE_PARAMETER_NAME);
            if (dateString != null && !dateString.isEmpty()) {
                try {
                    LocalDate date = LocalDate.parse(dateString, DATE_FORMATTER);
                    LocalDateTime dateTime = date.atStartOfDay();
                    logger.debug("Data extraída do parâmetro '{}': {} -> {}", DATE_PARAMETER_NAME, dateString, dateTime);
                    return dateTime;
                } catch (DateTimeParseException e) {
                    logger.warn("Erro ao parsear data '{}' do parâmetro '{}': {}. Retornando null.", 
                        dateString, DATE_PARAMETER_NAME, e.getMessage());
                    return null;
                }
            }
        } catch (IllegalArgumentException e) {
            // Se não for String (ex: é Long), o parâmetro já foi processado acima ou não existe
            logger.debug("Parâmetro '{}' não é String (provavelmente é Long ou não existe): {}", 
                DATE_PARAMETER_NAME, e.getMessage());
        }

        logger.debug("Parâmetro '{}' não encontrado nos JobParameters. Retornando null (usará LocalDateTime.now())", 
            DATE_PARAMETER_NAME);
        return null;
    }

    /**
     * Obtém a data de processamento, usando o parâmetro se disponível, senão usa LocalDateTime.now()
     * 
     * @param jobParameters Parâmetros do job
     * @return LocalDateTime do parâmetro ou LocalDateTime.now() se não fornecido
     */
    public static LocalDateTime getProcessDateOrDefault(JobParameters jobParameters) {
        LocalDateTime processDate = getProcessDate(jobParameters);
        if (processDate != null) {
            return processDate;
        }
        return LocalDateTime.now();
    }
}
