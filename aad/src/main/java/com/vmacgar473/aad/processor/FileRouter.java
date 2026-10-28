package com.vmacgar473.aad.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class FileRouter {
    private static final Logger log = LoggerFactory.getLogger(FileRouter.class);

    private final CsvProcessor csvProcessor;
    private final EncryptedProcessor encryptedProcessor;
    private final ZipProcessor zipProcessor;
    private final ImageProcessor imageProcessor;

    public FileRouter(CsvProcessor csvProcessor, EncryptedProcessor encryptedProcessor,
                      ZipProcessor zipProcessor, ImageProcessor imageProcessor) {
        this.csvProcessor = csvProcessor;
        this.encryptedProcessor = encryptedProcessor;
        this.zipProcessor = zipProcessor;
        this.imageProcessor = imageProcessor;
    }

    public double routeFile(Path file, Path errorPath) throws IOException {
        String fileName = file.getFileName().toString();

        if (Files.size(file) == 0) {
            log.error("[ALERT EMAIL SENT -> soporte@aadtex.com] CRITICAL: Empty/Corrupt batch file: {}", fileName);
            Files.move(file, errorPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            return -1;
        }

        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".csv")) {
            return csvProcessor.process(file);
        } else if (lowerName.endsWith(".enc")) {
            return encryptedProcessor.process(file);
        } else if (lowerName.endsWith(".zip")) {
            return zipProcessor.process(file);
        } else if (lowerName.endsWith(".jpg")) {
            imageProcessor.process(file);
            return 0.0;
        }
        return -1;
    }
}