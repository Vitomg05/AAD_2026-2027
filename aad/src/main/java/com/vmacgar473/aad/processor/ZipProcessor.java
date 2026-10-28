package com.vmacgar473.aad.processor;

import com.vmacgar473.aad.config.AppConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ZipProcessor {
    private static final Logger log = LoggerFactory.getLogger(ZipProcessor.class);

    public double process(Path sourceFile) {
        double totalSales = 0.0;
        try (InputStream is = Files.newInputStream(sourceFile);
             ZipInputStream zis = new ZipInputStream(is)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                if (entryName.endsWith(".csv")) {
                    InputStreamReader isr = new InputStreamReader(zis, StandardCharsets.UTF_8);
                    BufferedReader br = new BufferedReader(isr);
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
                            } catch (Exception e) {
                            }
                        }
                    }
                    log.info("📦 CSV extraído del ZIP: " + entryName);

                } else if (entryName.endsWith(".jpg")) {
                    Path destFile = Paths.get(AppConstants.BASE_PATH, "output/processed/signatures").resolve(entryName);
                    try (BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(destFile))) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = zis.read(buffer)) != -1) bos.write(buffer, 0, bytesRead);
                    }
                    log.info("📦 Firma extraída del ZIP: " + entryName);
                }
                zis.closeEntry();
            }
            zis.close();
            Files.delete(sourceFile);
            log.info("✅ ZIP Procesado: " + sourceFile.getFileName());
        } catch (IOException e) {
            log.error("Error procesando ZIP", e);
        }
        return totalSales;
    }
}