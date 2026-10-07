package com.vmacgar473.aad;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@SpringBootApplication
@Slf4j // Esta anotación genera automáticamente el campo de log 'log' usando la librería SLF4J
public class AadApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(AadApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        processTextFile();
        processBinaryFile("students.csv");
    }

    public void processTextFile() {
        Path path = Paths.get("students.csv");

        try {
            String csvData = """
                    ID,Name,Role
                    1,Sophia,Developer
                    2,Marcus,Project Manager
                    """;

            Files.writeString(path, csvData, StandardCharsets.UTF_8);
            System.out.println("Fichero escrito correctamente: " + path.toAbsolutePath());

            String retrievedContent = Files.readString(path, StandardCharsets.UTF_8);
            System.out.println("--- Contenido recuperado ---");
            System.out.println(retrievedContent);

        } catch (IOException e) {
            System.err.println("Error al procesar el fichero de texto: " + e.getMessage());
        }
    }

    public void processBinaryFile(String filename) {
        File file = new File(filename);

        if (!file.exists()) {
            System.err.println("No se encuentra el fichero binario: " + filename);
            return;
        }

        try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(file))) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = input.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }

            String hash = HexFormat.of().formatHex(digest.digest());
            System.out.println("Fichero: " + file.getName());
            System.out.println("Tamaño: " + (file.length() / 1024) + " KB");
            System.out.println("SHA-256: " + hash);

        } catch (IOException | NoSuchAlgorithmException e) {
            System.err.println("Error procesando fichero binario: " + e.getMessage());
        }
    }

}
