package com.vmacgar473.aad.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class CloudBucketService {

    private static final Logger log = LoggerFactory.getLogger(CloudBucketService.class);
    private static final String BASE_PATH = System.getProperty("user.dir") + "/workspace/store_data";

    /**
     * Requisito 8: Resumen JSON Preliminar y Bucket Local
     */
    public void generateDailyReport(double totalSales, int processedFiles) {
        if (processedFiles == 0) return; // Si no hay nada nuevo, no generamos reporte

        String batchId = UUID.randomUUID().toString().substring(0, 8);
        String fileName = "summary_batch_" + batchId + ".json";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // Construcción del JSON manualmente (sin usar librerías externas complejas para mantenerlo sencillo)
        String jsonContent = "{\n" +
                "  \"batch_id\": \"" + batchId + "\",\n" +
                "  \"timestamp\": \"" + timestamp + "\",\n" +
                "  \"total_files_processed\": " + processedFiles + ",\n" +
                "  \"total_sales_accumulated\": " + totalSales + "\n" +
                "}";

        Path bucketPath = Paths.get(BASE_PATH, "output/bucket/aadtex-daily-reports/2026/09", fileName);

        try {
            Files.writeString(bucketPath, jsonContent, StandardCharsets.UTF_8);
            log.info("☁️ REPORTE CLOUD GENERADO: {} -> Ventas Totales: {}", fileName, totalSales);
        } catch (IOException e) {
            log.error("Error al escribir el reporte JSON en el bucket local", e);
        }
    }
}
