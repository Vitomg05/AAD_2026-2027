package com.vmacgar473.aad;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class MultiLanguageDataGenerator implements CommandLineRunner {

    private static final String BASE_PATH = System.getProperty("user.dir") + "/workspace/store_data";
    private static final String AES_KEY = "AadTexSecret2026"; // Clave de 16 bytes para AES-128


    private void generarArchivosTexto(Path inputPath, Path keysPath) {
        try {
            // 1. Clave secreta
            Path keyFile = keysPath.resolve("AadTexSecret.key");
            if (!Files.exists(keyFile)) Files.writeString(keyFile, AES_KEY, StandardCharsets.UTF_8);

            // 2. Lote España
            Path spainCsv = inputPath.resolve("daily_sales_spain.csv");
            String spainContent = "StoreID,StoreName,SalesAmount,Category,TransactionDate\n" +
                    "ZARA_ES_001,Zara Puerta del Sol Madrid,14500.50,WOMEN,2026-09-13\n" +
                    "ZARA_ES_001,Zara Puerta del Sol Madrid,3600.30,MEN,2026-09-13\n" +
                    "ZARA_ES_001,Zara Gran Vía Madrid,2000.00,KIDS,2026-09-13";
            if (!Files.exists(spainCsv)) Files.writeString(spainCsv, spainContent, StandardCharsets.UTF_8);

            // 3. Lote China
            Path chinaCsv = inputPath.resolve("daily_sales_china.csv");
            String chinaContent = "StoreID,StoreName,SalesAmount,Category,TransactionDate\n" +
                    "ZARA_CH_001,Zara 上海南京东路店,28900.00,WOMEN,2026-09-13\n" +
                    "ZARA_CH_001,Zara 上海南京东路店,12400.80,MEN,2026-09-13";
            if (!Files.exists(chinaCsv)) Files.writeString(chinaCsv, chinaContent, StandardCharsets.UTF_8);

            // 4. Archivo defectuoso
            Path brokenCsv = inputPath.resolve("daily_sales_broken_store.csv");
            if (!Files.exists(brokenCsv)) Files.createFile(brokenCsv);

        } catch (IOException e) {
            System.err.println("Error al generar textos: " + e.getMessage());
        }
    }

    private void generarArchivosBinarios(Path inputPath) {
        try {
            // 1. JPGs simulados (Tienen los Magic Bytes reales de un JPG)
            crearJpgSimulado(inputPath.resolve("signature_spain.jpg"));
            crearJpgSimulado(inputPath.resolve("signature_china.jpg"));
            crearJpgSimulado(inputPath.resolve("signature_financial.jpg"));

            // 2. Archivo Cifrado Financiero (AES-128)
            Path encryptedPath = inputPath.resolve("daily_sales_financial.enc");
            if (!Files.exists(encryptedPath)) {
                String financialContent = "StoreID,StoreName,SalesAmount,Category,TransactionDate\n" +
                        "ZARA_CENTRAL_FIN,Central Financial Office,95000.00,FINANCIAL_REPORT,2026-09-13";

                SecretKeySpec secretKey = new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), "AES");
                Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
                byte[] encryptedBytes = cipher.doFinal(financialContent.getBytes(StandardCharsets.UTF_8));

                Files.write(encryptedPath, encryptedBytes);
            }

            // 3. Contenedor ZIP de USA
            Path zipPath = inputPath.resolve("batch_usa.zip");
            if (!Files.exists(zipPath)) {
                try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
                    // CSV dentro del ZIP
                    zos.putNextEntry(new ZipEntry("daily_sales_usa.csv"));
                    String usaContent = "StoreID,StoreName,SalesAmount,Category,TransactionDate\nZARA_US_001,Zara 5th Ave NY,45000.00,WOMEN,2026-09-13";
                    zos.write(usaContent.getBytes(StandardCharsets.UTF_8));
                    zos.closeEntry();

                    // JPG dentro del ZIP
                    zos.putNextEntry(new ZipEntry("signature_usa.jpg"));
                    zos.write(getJpgMagicBytes());
                    zos.closeEntry();
                }
            }
        } catch (Exception e) {
            System.err.println("Error al generar binarios: " + e.getMessage());
        }
    }

    private void crearJpgSimulado(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.write(path, getJpgMagicBytes());
        }
    }

    private byte[] getJpgMagicBytes() {
        // Generamos un archivo de unos 5KB para que el buffer de 4KB tenga que trabajar en la copia
        byte[] fakeImage = new byte[5000];
        fakeImage[0] = (byte) 0xFF; // Magic Byte JPG
        fakeImage[1] = (byte) 0xD8; // Magic Byte JPG
        fakeImage[2] = (byte) 0xFF; // Magic Byte JPG
        fakeImage[3] = (byte) 0xE0; // Magic Byte JPG
        return fakeImage;
    }

    @Override
    public void run(String... args) {
        System.out.println("Generando todos los datos de prueba en /input...");
        Path inputPath = Paths.get(BASE_PATH, "input");
        Path keysPath = Paths.get(BASE_PATH, "keys");

        generarArchivosTexto(inputPath, keysPath);
        generarArchivosBinarios(inputPath);

        System.out.println("✅ Todos los archivos de prueba (Texto, ZIP, JPG y Cifrados) han sido generados.");
    }
}