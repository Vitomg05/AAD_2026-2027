package com.vmacgar473.aad.bootstrap;

import com.vmacgar473.aad.config.AppConstants;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class MultiLanguageDataGenerator implements CommandLineRunner {

    private final TextFileGenerator textGenerator;
    private final BinaryFileGenerator binaryGenerator;

    public MultiLanguageDataGenerator(TextFileGenerator t, BinaryFileGenerator b) {
        this.textGenerator = t;
        this.binaryGenerator = b;
    }

    @Override
    public void run(String... args) throws Exception {
        Path base = Paths.get(AppConstants.BASE_PATH);
        Path input = base.resolve("input");
        Path keys = base.resolve("keys");

        // 1. Crear directorios
        Files.createDirectories(keys);
        Files.createDirectories(input);
        Files.createDirectories(base.resolve("output/processed/signatures"));
        Files.createDirectories(base.resolve("output/error"));
        Files.createDirectories(base.resolve("output/bucket/aadtex-daily-reports/2026/09"));

        // 2. Delegar la generación de ficheros
        textGenerator.generate(input, keys);
        binaryGenerator.generate(input);

        System.out.println("✅ Directorios y datos de prueba listos en: " + AppConstants.BASE_PATH);
    }
}