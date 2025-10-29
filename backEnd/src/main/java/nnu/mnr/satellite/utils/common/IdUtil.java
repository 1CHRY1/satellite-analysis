package nnu.mnr.satellite.utils.common;

import nnu.mnr.satellite.utils.security.JwtUtil;

import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/21 22:58
 * @Description:
 */
public class IdUtil {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static final int PRO_NUM = 17;
    private static final int USR_NUM = 17;
    private static final int TOL_NUM = 17;
    private static final int SE_NUM = 5;
    private static final int P_NUM = 8;
    private static final int EOC_NUM = 17;

    public static String generateProjectId() {
        StringBuilder sb = new StringBuilder("PRJ");
        Random random = new Random();
        for (int i = 0; i < PRO_NUM; i++) {
            int index = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }

    public static String generateUserId() {
        StringBuilder sb = new StringBuilder("USR");
        Random random = new Random();
        for (int i = 0; i < USR_NUM; i++) {
            int index = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }

    public static String generateToolId() {
        StringBuilder sb = new StringBuilder("Tol");
        Random random = new Random();
        for (int i = 0; i < TOL_NUM; i++) {
            int index = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }

    public static String generateSensorId() {
        StringBuilder sb = new StringBuilder("SE");
        Random random = new Random();
        for (int i = 0; i < SE_NUM; i++) {
            int index = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }

    public static String generateProductId() {
        StringBuilder sb = new StringBuilder("P");
        Random random = new Random();
        for (int i = 0; i < P_NUM; i++) {
            int index = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }

    public static String generateEOCubeCacheKey(String userId) {
        StringBuilder sb = new StringBuilder(userId).append("_");
        sb.append("EOC");
        Random random = new Random();
        for (int i = 0; i < EOC_NUM; i++) {
            int index = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }

        return sb.toString();
    }

    public static String parseUserIdFromAuthHeader(String authorizationHeader) {
        // 1. 检查 Token 是否存在
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header is missing or invalid");
        }

        // 2. 分割字符串并提取 Token
        String[] parts = authorizationHeader.split(" ");
        if (parts.length != 2 || !"Bearer".equals(parts[0])) {
            throw new IllegalArgumentException("Invalid Authorization header format");
        }
        String token = parts[1];

        // 3. 解析 Token 获取 userId
        return JwtUtil.getUserIdFromToken(token); // 假设 JwtUtil 已存在
    }

}
