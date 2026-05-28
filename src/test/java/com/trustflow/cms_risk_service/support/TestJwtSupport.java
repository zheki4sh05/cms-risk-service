package com.trustflow.cms_risk_service.support;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public final class TestJwtSupport {
    private static final KeyPair KEY_PAIR = generateRsaKeyPair();
    private static final JwtEncoder JWT_ENCODER = createEncoder();

    private TestJwtSupport() {
    }

    public static String publicKeyPem() {
        byte[] encoded = ((RSAPublicKey) KEY_PAIR.getPublic()).getEncoded();
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(encoded);
        return "-----BEGIN PUBLIC KEY-----\n" + base64 + "\n-----END PUBLIC KEY-----";
    }

    public static String bearerToken(UUID userId, UUID companyId) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("cms-risk-service-test")
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("userId", userId.toString())
                .claim("companyId", companyId.toString())
                .build();
        return JWT_ENCODER.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    private static JwtEncoder createEncoder() {
        try {
            RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) KEY_PAIR.getPublic())
                    .privateKey((RSAPrivateKey) KEY_PAIR.getPrivate())
                    .keyID("test-key")
                    .build();
            JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new com.nimbusds.jose.jwk.JWKSet(rsaKey));
            return new NimbusJwtEncoder(jwkSource);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to initialize test JWT encoder", exception);
        }
    }

    private static KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to generate RSA key pair for tests", exception);
        }
    }
}
