package com.vmacgar473.aad.bootstrap;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class TextFileGenerator {

    public void generate(Path input, Path keys) {
        try {
            Path keyFile = keys.resolve("AadTexSecret.key");
            if (!Files.exists(keyFile)) Files.writeString(keyFile, "AadTexSecret2026", StandardCharsets.UTF_8);

            Path spainCsv = input.resolve("daily_sales_spain.csv");
            if (!Files.exists(spainCsv)) {
                String content = "StoreID,StoreName,SalesAmount,Category,TransactionDate\nZARA_ES_001,Zara Puerta del Sol,14500.50,WOMEN,2026-09-13\nZARA_ES_001,Zara Puerta del Sol,3600.30,MEN,2026-09-13\nZARA_ES_001,Zara Gran Vía,2000.00,KIDS,2026-09-13";
                Files.writeString(spainCsv, content, StandardCharsets.UTF_8);
            }

            Path chinaCsv = input.resolve("daily_sales_china.csv");
            if (!Files.exists(chinaCsv)) {
                String content = "StoreID,StoreName,SalesAmount,Category,TransactionDate\nZARA_CH_001,Zara 上海南京东路店,28900.00,WOMEN,2026-09-13\nZARA_CH_001,Zara 上海南京东路店,12400.80,MEN,2026-09-13";
                Files.writeString(chinaCsv, content, StandardCharsets.UTF_8);
            }

            Path brokenCsv = input.resolve("daily_sales_broken_store.csv");
            if (!Files.exists(brokenCsv)) Files.createFile(brokenCsv);

        } catch (Exception e) {
            System.err.println("Error generando textos: " + e.getMessage());
        }
    }
}