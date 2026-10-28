package com.vmacgar473.aad.scheduler;

import com.vmacgar473.aad.config.AppConstants;
import com.vmacgar473.aad.processor.FileRouter;
import com.vmacgar473.aad.service.CloudBucketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class StoreIngestionScheduler {
    private static final Logger log = LoggerFactory.getLogger(StoreIngestionScheduler.class);

    private final FileRouter fileRouter;
    private final CloudBucketService cloudBucketService;

    public StoreIngestionScheduler(FileRouter fileRouter, CloudBucketService cloudBucketService) {
        this.fileRouter = fileRouter;
        this.cloudBucketService = cloudBucketService;
    }

    //@Scheduled(cron = "0 0 3 * * *") // A las 03:00 AM
    @Scheduled(fixedRate = 50000)
    public void processStoreBatches() {
        Path inputPath = Paths.get(AppConstants.BASE_PATH, "input");
        Path errorPath = Paths.get(AppConstants.BASE_PATH, "output/error");

        if (!Files.exists(inputPath)) return;

        double cycleTotalSales = 0.0;
        int processedFilesCount = 0;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputPath)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    double sales = fileRouter.routeFile(file, errorPath); // Delega la responsabilidad
                    if (sales >= 0) {
                        cycleTotalSales += sales;
                        processedFilesCount++;
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error crítico al escanear /input", e);
        }

        if (processedFilesCount > 0) {
            cloudBucketService.generateDailyReport(cycleTotalSales, processedFilesCount);
        }
    }
}