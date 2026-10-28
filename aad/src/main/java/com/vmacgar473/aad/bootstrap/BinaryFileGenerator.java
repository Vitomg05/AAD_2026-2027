package com.vmacgar473.aad.bootstrap;

import com.vmacgar473.aad.service.CryptoService;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class BinaryFileGenerator {
    private final CryptoService cryptoService;

    public BinaryFileGenerator(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public void generate(Path input) {
        try {
            byte[] fakeJpg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x00, 0x00};
            if (!Files.exists(input.resolve("signature_spain.jpg")))
                Files.write(input.resolve("signature_spain.jpg"), fakeJpg);
            if (!Files.exists(input.resolve("signature_china.jpg")))
                Files.write(input.resolve("signature_china.jpg"), fakeJpg);
            if (!Files.exists(input.resolve("signature_financial.jpg")))
                Files.write(input.resolve("signature_financial.jpg"), fakeJpg);

            Path encPath = input.resolve("daily_sales_financial.enc");
            if (!Files.exists(encPath)) {
                String plain = "StoreID,StoreName,SalesAmount,Category,TransactionDate\nZARA_CENTRAL_FIN,Central,95000.00,FIN,2026-09-13";
                byte[] enc = cryptoService.encryptAes(plain.getBytes(StandardCharsets.UTF_8), "AadTexSecret2026");
                Files.write(encPath, enc);
            }

            Path zipPath = input.resolve("batch_usa.zip");
            if (!Files.exists(zipPath)) {
                try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
                    zos.putNextEntry(new ZipEntry("daily_sales_usa.csv"));
                    zos.write("StoreID,StoreName,SalesAmount,Category,TransactionDate\nZARA_US_001,Zara NY,45000.00,WOMEN,2026-09-13".getBytes(StandardCharsets.UTF_8));
                    zos.closeEntry();
                    zos.putNextEntry(new ZipEntry("signature_usa.jpg"));
                    zos.write(fakeJpg);
                    zos.closeEntry();
                }
            }
        } catch (Exception e) {
            System.err.println("Error generando binarios: " + e.getMessage());
        }
    }
}