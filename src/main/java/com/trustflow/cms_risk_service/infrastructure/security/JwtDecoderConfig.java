package com.trustflow.cms_risk_service.infrastructure.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
@EnableConfigurationProperties(JwtConfigProperties.class)
public class JwtDecoderConfig {
    @Bean
    public JwtDecoder jwtDecoder(JwtConfigProperties jwtConfigProperties) {
        if (jwtConfigProperties.publicKey() == null || jwtConfigProperties.publicKey().isBlank()) {
            throw new IllegalStateException("JWT public key is not configured. Set JWT_PUBLIC_KEY in .env");
        }
        return NimbusJwtDecoder.withPublicKey(parseRsaPublicKey(jwtConfigProperties.publicKey())).build();
    }

    private RSAPublicKey parseRsaPublicKey(String pemKey) {
        try {
            String normalized = pemKey
                    .replace("\\n", "\n")
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] encoded = Base64.getDecoder().decode(normalized);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) keyFactory.generatePublic(keySpec);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("JWT public key has invalid format", exception);
        }
    }
}
