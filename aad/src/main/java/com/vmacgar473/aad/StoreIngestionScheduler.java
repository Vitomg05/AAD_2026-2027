package com.vmacgar473.aad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class StoreIngestionScheduler {

    // Instanciamos el Logger para emitir alertas por consola (ej. avisos a soporte)
    private static final Logger log = LoggerFactory.getLogger(StoreIngestionScheduler.class);

    // Ruta base dinámica adaptada al directorio actual de ejecución (garantiza portabilidad)
    private static final String BASE_PATH = System.getProperty("user.dir") + "/workspace/store_data";

    // Inyección de dependencias para los servicios externos
    private final CryptoService cryptoService;
    private final CloudBucketService cloudBucketService;

    public StoreIngestionScheduler(CryptoService cryptoService, CloudBucketService cloudBucketService) {
        this.cryptoService = cryptoService;
        this.cloudBucketService = cloudBucketService;
    }

    /**
     * REQUISITO 1: Ejecución automatizada con Spring Scheduler.
     * Se ejecuta de forma automática en segundo plano cada 5000 ms (5 segundos).
     */
    @Scheduled(fixedRate = 5000)
    public void processStoreBatches() {
        Path inputPath = Paths.get(BASE_PATH, "input");
        Path errorPath = Paths.get(BASE_PATH, "output/error");

        // Si la carpeta input aún no existe (porque la app acaba de arrancar), abortamos el ciclo
        if (!Files.exists(inputPath)) return;

        double cycleTotalSales = 0.0;
        int processedFilesCount = 0;

        // REQUISITO 2: Control de flujo con NIO.2 escaneando el directorio
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputPath)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    // Procesamos cada fichero y recogemos el importe de ventas que contenga
                    double salesExtracted = procesarFichero(file, errorPath);

                    // Si devuelve >= 0, significa que se procesó con éxito (no estaba corrupto)
                    if (salesExtracted >= 0) {
                        cycleTotalSales += salesExtracted;
                        processedFilesCount++;
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error crítico al escanear el directorio de entrada", e);
        }

        // REQUISITO 8: Resumen JSON Preliminar.
        // Si en este ciclo se ha procesado algún archivo, generamos el reporte en la "nube"
        if (processedFilesCount > 0) {
            cloudBucketService.generateDailyReport(cycleTotalSales, processedFilesCount);
        }
    }

    /**
     * Enrutador principal. Decide qué hacer con cada archivo según su extensión
     * y devuelve las ventas calculadas para sumarlas al reporte final.
     */
    private double procesarFichero(Path file, Path errorPath) throws IOException {
        String fileName = file.getFileName().toString();

        // REQUISITO 2: Aislamiento de Ficheros de 0 Bytes / Corruptos
        if (Files.size(file) == 0) {
            log.error("[ALERT EMAIL SENT -> soporte@aadtex.com] CRITICAL: Empty/Corrupt batch file: {}", fileName);
            // Mueve el fichero atómicamente a la carpeta de errores reemplazando si ya existe
            Files.move(file, errorPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            return -1; // -1 indica al bucle principal que este archivo no suma ventas ni cuenta como procesado
        }

        // Convertimos a minúsculas para asegurar que la extensión coincida
        String fileNameLower = fileName.toLowerCase();

        if (fileNameLower.endsWith(".csv")) {
            return procesarCsv(file);
        } else if (fileNameLower.endsWith(".enc")) {
            return procesarArchivoCifrado(file);
        } else if (fileNameLower.endsWith(".zip")) {
            return procesarZip(file);
        } else if (fileNameLower.endsWith(".jpg")) {
            copiarFirmaConBuffer(file);
            return 0.0; // Las firmas se copian bien, pero no aportan dinero a las ventas
        }

        return -1; // Extensión desconocida
    }

    /**
     * REQUISITO 3: Descompresión de Paquetes ZIP en caliente.
     * Extrae archivos directamente desde el ZIP sin guardarlos temporalmente en el disco.
     */
    private double procesarZip(Path sourceFile) {
        double totalSales = 0.0;

        try (InputStream is = Files.newInputStream(sourceFile);
             ZipInputStream zis = new ZipInputStream(is)) {

            ZipEntry entry;

            // Iteramos sobre todos los archivos contenidos dentro del ZIP
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                if (entryName.endsWith(".csv")) {
                    // CUIDADO: Instanciamos el reader directamente sin "try-with-resources"
                    // para evitar que Java cierre automáticamente el ZipInputStream y rompa el bucle.
                    InputStreamReader isr = new InputStreamReader(zis, StandardCharsets.UTF_8);
                    BufferedReader br = new BufferedReader(isr);

                    String line;
                    boolean isFirstLine = true;

                    while ((line = br.readLine()) != null) {
                        if (isFirstLine) { isFirstLine = false; continue; } // Saltamos cabecera

                        String[] columns = line.split(",");
                        if (columns.length >= 3) {
                            try {
                                totalSales += Double.parseDouble(columns[2]);
                            } catch (NumberFormatException e) {
                                // Ignoramos fallos puntuales en el parseo numérico
                            }
                        }
                    }
                    System.out.println("📦 CSV extraído del ZIP: " + entryName + " | Total Ventas: " + totalSales);

                } else if (entryName.endsWith(".jpg")) {
                    // Procesamos la imagen de la firma empaquetada usando buffer de 4KB
                    Path destDir = Paths.get(BASE_PATH, "output/processed/signatures");
                    Path destFile = destDir.resolve(entryName);

                    try (BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(destFile))) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = zis.read(buffer)) != -1) {
                            bos.write(buffer, 0, bytesRead);
                        }
                    }
                    System.out.println("📦 Firma extraída del ZIP y copiada: " + entryName);
                }

                // Cerramos la entrada actual del ZIP para avanzar a la siguiente iteración
                zis.closeEntry();
            }

            // Cerramos el flujo del ZIP y eliminamos el fichero comprimido original de input
            zis.close();
            Files.delete(sourceFile);
            System.out.println("✅ ZIP Procesado y eliminado: " + sourceFile.getFileName());

        } catch (IOException e) {
            log.error("Error al procesar el archivo ZIP: {}", sourceFile.getFileName(), e);
        }

        return totalSales;
    }

    /**
     * REQUISITO 4: Desencriptación Simétrica AES-128 en RAM.
     * Desencripta balances financieros sin dejar rastro del texto claro en el disco duro.
     */
    private double procesarArchivoCifrado(Path sourceFile) {
        double totalSales = 0.0;
        try {
            // 1. Cargamos los bytes cifrados y la clave AES a memoria
            byte[] encryptedBytes = Files.readAllBytes(sourceFile);
            Path keyPath = Paths.get(BASE_PATH, "keys/AadTexSecret.key");
            String secretKey = Files.readString(keyPath, StandardCharsets.UTF_8).trim();

            // 2. Ejecutamos el descifrado a través de nuestro CryptoService
            byte[] decryptedBytes = cryptoService.decryptAes(encryptedBytes, secretKey);

            // 3. Volcamos los bytes descifrados a un flujo RAM (ByteArrayInputStream) para leerlo como CSV
            try (ByteArrayInputStream bais = new ByteArrayInputStream(decryptedBytes);
                 InputStreamReader isr = new InputStreamReader(bais, StandardCharsets.UTF_8);
                 BufferedReader br = new BufferedReader(isr)) {

                String line;
                boolean isFirstLine = true;
                while ((line = br.readLine()) != null) {
                    if (isFirstLine) { isFirstLine = false; continue; }

                    String[] columns = line.split(",");
                    if (columns.length >= 3) {
                        totalSales += Double.parseDouble(columns[2]);
                    }
                }
            }

            System.out.println("🔐 Archivo FINANCIERO desencriptado en RAM: " + sourceFile.getFileName() + " | Total Ventas: " + totalSales);

            // Eliminamos el archivo .enc de la entrada
            Files.delete(sourceFile);

        } catch (Exception e) {
            log.error("Error al desencriptar el archivo financiero: {}", sourceFile.getFileName(), e);
        }

        return totalSales;
    }

    /**
     * REQUISITO 5: Lectura CSV Multi-Idioma en UTF-8.
     * Fuerza la codificación UTF-8 para evitar corrupción de texto (Mojibake) en caracteres chinos y eñes.
     */
    private double procesarCsv(Path sourceFile) throws IOException {
        double totalSales = 0.0;

        try (InputStream is = Files.newInputStream(sourceFile);
             InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8); // <- Clave del UTF-8
             BufferedReader br = new BufferedReader(isr)) {

            String line;
            boolean isFirstLine = true;

            while ((line = br.readLine()) != null) {
                if (isFirstLine) { isFirstLine = false; continue; } // Omitir cabeceras

                String[] columns = line.split(",");
                if (columns.length >= 3) {
                    try {
                        totalSales += Double.parseDouble(columns[2]);
                    } catch (NumberFormatException e) {
                        log.warn("Error al leer el importe en la línea: {}", line);
                    }
                }
            }
        }

        System.out.println("✅ CSV Procesado: " + sourceFile.getFileName() + " | Total Ventas: " + totalSales);

        // Eliminamos el CSV original tras el procesado
        Files.delete(sourceFile);

        return totalSales;
    }

    /**
     * REQUISITO 6: Copia Binaria de Firmas con Buffers (4 KB).
     * Mueve imágenes en bloque de 4096 bytes para no colapsar la RAM con archivos gigantes.
     */
    private void copiarFirmaConBuffer(Path sourceFile) throws IOException {
        Path destDir = Paths.get(BASE_PATH, "output/processed/signatures");
        Path destFile = destDir.resolve(sourceFile.getFileName());

        try (InputStream in = Files.newInputStream(sourceFile);
             BufferedInputStream bis = new BufferedInputStream(in);
             OutputStream out = Files.newOutputStream(destFile);
             BufferedOutputStream bos = new BufferedOutputStream(out)) {

            byte[] buffer = new byte[4096]; // Buffer estricto de 4KB exigido
            int bytesRead;

            while ((bytesRead = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
        }

        // Eliminamos la imagen de la entrada para evitar bucles infinitos en el Scheduler
        Files.delete(sourceFile);
        System.out.println("✅ Firma copiada con éxito y eliminada de input: " + sourceFile.getFileName());
    }
}