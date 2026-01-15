//package com.blueWhale.Rahwan.security.jwt;
//
//import io.jsonwebtoken.*;
//import io.jsonwebtoken.security.Keys;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.security.core.Authentication;
//import org.springframework.stereotype.Component;
//
//import java.security.Key;
//import java.util.Date;
//import java.util.UUID;
//
//@Component
//public class JwtTokenProvider {
//
//    @Value("${jwt.secret}")
//    private String jwtSecret;
//
//    @Value("${jwt.expiration}")
//    private long jwtExpirationMs;
//
//    private Key getSigningKey() {
//        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
//    }
//
//    public String generateToken(UUID userId, String phone, String type) {
//        Date now = new Date();
//        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);
//
//        return Jwts.builder()
//                .setSubject(userId.toString())
//                .claim("phone", phone)
//                .claim("type", type)
//                .setIssuedAt(now)
//                .setExpiration(expiryDate)
//                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
//                .compact();
//    }
//
//    public UUID getUserIdFromToken(String token) {
//        Claims claims = Jwts.parserBuilder()
//                .setSigningKey(getSigningKey())
//                .build()
//                .parseClaimsJws(token)
//                .getBody();
//
//        return UUID.fromString(claims.getSubject());
//    }
//
//    public String getPhoneFromToken(String token) {
//        Claims claims = Jwts.parserBuilder()
//                .setSigningKey(getSigningKey())
//                .build()
//                .parseClaimsJws(token)
//                .getBody();
//
//        return claims.get("phone", String.class);
//    }
//
//    public String getTypeFromToken(String token) {
//        Claims claims = Jwts.parserBuilder()
//                .setSigningKey(getSigningKey())
//                .build()
//                .parseClaimsJws(token)
//                .getBody();
//
//        return claims.get("type", String.class);
//    }
//
//    public boolean validateToken(String token) {
//        try {
//            Jwts.parserBuilder()
//                    .setSigningKey(getSigningKey())
//                    .build()
//                    .parseClaimsJws(token);
//            return true;
//        } catch (SecurityException ex) {
//            System.err.println("Invalid JWT signature");
//        } catch (MalformedJwtException ex) {
//            System.err.println("Invalid JWT token");
//        } catch (ExpiredJwtException ex) {
//            System.err.println("Expired JWT token");
//        } catch (UnsupportedJwtException ex) {
//            System.err.println("Unsupported JWT token");
//        } catch (IllegalArgumentException ex) {
//            System.err.println("JWT claims string is empty");
//        }
//        return false;
//    }
//}