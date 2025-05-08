package nnu.mnr.satellitemodeling.utils.common;

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

}
