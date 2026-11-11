package com.chat.modules.auth.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class SessionTokenGenerator {
    private static final int TOKEN_BYTES = 32;
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public boolean isValidFormat(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) {
            return false;
        }
        byte[] bytes = Base64.getUrlDecoder().decode(token);
        return bytes.length == TOKEN_BYTES
                && Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).equals(token);
    }

    public String hash(String token) {
        if (!isValidFormat(token)) {
            throw new IllegalArgumentException("Invalid session token format");
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
