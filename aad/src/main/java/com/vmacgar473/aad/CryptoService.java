package com.vmacgar473.aad;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Service
public class CryptoService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    public byte[] decryptAes(byte[] encryptedBytes, String secretKeyStr) throws Exception {
        // Configuramos la clave de 16 bytes (AES-128)
        SecretKeySpec secretKey = new SecretKeySpec(secretKeyStr.getBytes(StandardCharsets.UTF_8), ALGORITHM);

        // Iniciamos el descifrador
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        // Devolvemos los bytes descifrados directamente en memoria RAM
        return cipher.doFinal(encryptedBytes);
    }
}