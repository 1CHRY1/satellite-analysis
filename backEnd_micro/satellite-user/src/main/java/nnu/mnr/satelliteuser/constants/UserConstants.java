package nnu.mnr.satelliteuser.constants;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/27 22:31
 * @Description:
 */
public class UserConstants {

    public static final int SUCCESS_STATUS = 1;
    public static final int FAILURE_STATUS = -1;

    public static final String USER_CREATED = "User %s has been Created Successfully";
    public static final String USER_LOGIN = "User %s Login Successfully";
    public static final String USER_LOGIN_FAILED = "User %s Login Failed";
    public static final String USER_UPDATED = "User %s has been updated successfully";
    public static final String USER_DELETED = "User %s has been deleted successfully";
    public static final String USER_NOT_FOUND = "User %s Not Found";
    public static final String USER_INFO_UPDATE = "User %s Info Updated Successfully";
    public static final String USER_ALREADY_EXISTS = "User %s Already Exists";
    public static final String EMAIL_ALREADY_EXISTS = "Email %s Already Exists";
    public static final String USER_AUTH_FAILED = "Authentication failed for user %s";
    public static final String USER_NO_PERMISSION = "User %s has no permission to perform this operation";
    public static final String USER_OPERATION_FAILED = "Operation failed for user %s: %s";

    private UserConstants() {
        throw new IllegalStateException("Utility class");
    }

}
