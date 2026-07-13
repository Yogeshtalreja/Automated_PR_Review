package com.pr.review.reviewbot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Component
public class HmacValidator {

    private static final String ALGORITHM = "HmacSHA256";


    public boolean isValid(String payload, String signature, String secret) {
        if (signature == null || !signature.startsWith("sha256=")) {
            log.warn("Missing or malformed signature header");
            return false;
        }

        try {
            String expected = "sha256=" + computeHmac(payload, secret);

            // Use MessageDigest.isEqual to prevent timing attacks
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("HMAC validation failed: {}", e.getMessage());
            return false;
        }
    }

    private String computeHmac(String payload, String secret)
            throws NoSuchAlgorithmException, InvalidKeyException {

        Mac mac = Mac.getInstance(ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), ALGORITHM
        );
        mac.init(keySpec);
        byte[] hash = mac.doFinal(
                payload.getBytes(StandardCharsets.UTF_8)
        );

        // Convert bytes to hex string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
