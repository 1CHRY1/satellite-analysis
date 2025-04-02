package nnu.mnr.satellite.utils.security;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/28 10:23
 * @Description:
 */
public class JwtUtil {

    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor("NNU-satellite-analysis-center-for-MNR".getBytes(StandardCharsets.UTF_8));
    private static final long ACCESS_TOKEN_EXPIRATION = 15 * 60 * 1000; // 15 Min
    private static final long REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60 * 1000; // 7 Day

    public static String generateAccessToken(String userId, String username, Collection<? extends GrantedAuthority> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("role", roles);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION))
                .signWith(SECRET_KEY)
                .compact();
    }

    public static String generateRefreshToken(String userId) {
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION))
                .signWith(SECRET_KEY)
                .compact();
    }

    public static Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public static String getUserIdFromToken(String token) {
        return parseToken(token).getSubject();
    }

    public static String getUsernameFromToken(String token) {
        return parseToken(token).get("username", String.class);
    }

    @SuppressWarnings("unchecked")
    public static List<String> getRolesFromToken(String token) {
        return (List<String>) parseToken(token).get("roles");
    }

    public static boolean isTokenExpired(String token) {
        try {
            return parseToken(token).getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true; // 过期 token 返回 true
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid token", e);
        }
    }

}
