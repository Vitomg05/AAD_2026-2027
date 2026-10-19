package com.vmacgar473.aad;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
@EnableScheduling
public class AadApplication implements CommandLineRunner {

    private static final String BASE_PATH = System.getProperty("user.dir") + "/workspace/store_data";

    private void inicializarDirectorios() {
        Path basePath = Paths.get(BASE_PATH);

        try {
            Files.createDirectories(basePath.resolve("keys"));
            Files.createDirectories(basePath.resolve("input"));
            Files.createDirectories(basePath.resolve("output/processed/signatures"));
            Files.createDirectories(basePath.resolve("output/error"));
            Files.createDirectories(basePath.resolve("output/bucket/aadtex-daily-reports/2026/09"));

            System.out.println("Estructura de directorios verificada/creada con éxito en: " + basePath.toAbsolutePath());

        } catch (IOException e) {
            System.err.println("Error crítico al crear la estructura de directorios: " + e.getMessage());
        }
    }
    

    public static void main(String[] args) {
        SpringApplication.run(AadApplication.class, args);
    }


    public void run(String... args) {

        System.out.println("Inicializando motor de ingestión AadTex...");

        // Llamamos al metodo especializado
        inicializarDirectorios();

    }


}
