package com.unitrovee.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    // constructor injection of config values from application.properties
    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    // build a signed token: subject = email, plus a role claim and an expiry
    public String generateToken(UserDetails user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        String role = user.getAuthorities().stream()
                .findFirst().map(GrantedAuthority::getAuthority).orElse("ROLE_STUDENT");
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    // read the subject (email) out of a token - signature is verified first
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // generic claim reader: verify signature, then apply the resolver to the claim
    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }

    // valid = parses cleanly (signature ok + not expired) AND belongs to this user
    public boolean isTokenValid(String token, UserDetails user) {
        try {
            return extractUsername(token).equals(user.getUsername());
        } catch (Exception e) {
            return false;
        }
    }
}