package com.vmacgar473.aad.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class CsvProcessor {
    private static final Logger log = LoggerFactory.getLogger(CsvProcessor.class);

    public double process(Path sourceFile) throws IOException {
        double totalSales = 0.0;

        try (InputStream is = Files.newInputStream(sourceFile);
             InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {

            String line;
            boolean isFirstLine = true;

            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                String[] columns = line.split(",");
                if (columns.length >= 3) {
                    try {
                        totalSales += Double.parseDouble(columns[2]);
                    } catch (NumberFormatException e) {
                        log.warn("Línea ignorada por formato numérico inválido");
                    }
                }
            }
        }
        log.info("✅ CSV Procesado: " + sourceFile.getFileName() + " | Total Ventas: " + totalSales);
        Files.delete(sourceFile);
        return totalSales;
    }
}