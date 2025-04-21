package nnu.mnr.satellitegateway.utils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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

    public static Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
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
