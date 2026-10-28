package com.vmacgar473.aad.processor;

import com.vmacgar473.aad.config.AppConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ImageProcessor {
    private static final Logger log = LoggerFactory.getLogger(ImageProcessor.class);


    public void process(Path sourceFile) throws IOException {
        Path destDir = Paths.get(AppConstants.BASE_PATH, "output/processed/signatures");
        Path destFile = destDir.resolve(sourceFile.getFileName());

        try (InputStream in = Files.newInputStream(sourceFile);
             BufferedInputStream bis = new BufferedInputStream(in);
             OutputStream out = Files.newOutputStream(destFile);
             BufferedOutputStream bos = new BufferedOutputStream(out)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
        }
        Files.delete(sourceFile);
        log.info("✅ Firma copiada con éxito: " + sourceFile.getFileName());
    }
}