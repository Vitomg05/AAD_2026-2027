package com.vmacgar473.aad.processor;

import com.vmacgar473.aad.config.AppConstants;
import com.vmacgar473.aad.service.CryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class EncryptedProcessor {
    private static final Logger log = LoggerFactory.getLogger(EncryptedProcessor.class);
    private final CryptoService cryptoService;

    public EncryptedProcessor(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public double process(Path sourceFile) {
        double totalSales = 0.0;
        try {
            byte[] encryptedBytes = Files.readAllBytes(sourceFile);
            Path keyPath = Paths.get(AppConstants.BASE_PATH, "keys/AadTexSecret.key");
            String secretKey = Files.readString(keyPath, StandardCharsets.UTF_8).trim();

            byte[] decryptedBytes = cryptoService.decryptAes(encryptedBytes, secretKey);

            try (ByteArrayInputStream bais = new ByteArrayInputStream(decryptedBytes);
                 InputStreamReader isr = new InputStreamReader(bais, StandardCharsets.UTF_8);
                 BufferedReader br = new BufferedReader(isr)) {

                String line;
                boolean isFirstLine = true;
                while ((line = br.readLine()) != null) {
                    if (isFirstLine) {
                        isFirstLine = false;
                        continue;
                    }
                    String[] columns = line.split(",");
                    if (columns.length >= 3) totalSales += Double.parseDouble(columns[2]);
                }
            }
            log.info("🔐 Archivo FINANCIERO desencriptado en RAM. Total: " + totalSales);
            Files.delete(sourceFile);
        } catch (Exception e) {
            log.error("Error al desencriptar: {}", sourceFile.getFileName(), e);
        }
        return totalSales;
    }
}