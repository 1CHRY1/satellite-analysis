package nnu.mnr.satellite.constants;

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

      // 英文版
//    public static final String USER_CREATED = "User %s has been Created Successfully";
//    public static final String USER_LOGIN = "User %s Login Successfully";
//    public static final String USER_LOGIN_FAILED = "User %s Login Failed";
//    public static final String USER_UPDATED = "User %s has been updated successfully";
//    public static final String USER_DELETED = "User %s has been deleted successfully";
//    public static final String USER_NOT_FOUND = "User %s Not Found";
//    public static final String USER_INFO_UPDATE = "User %s Info Updated Successfully";
//    public static final String USER_ALREADY_EXISTS = "User %s Already Exists";
//    public static final String EMAIL_ALREADY_EXISTS = "Email %s Already Exists";
//    public static final String PHONE_ALREADY_EXISTS = "Phone %s Already Exists";
//    public static final String USER_AUTH_FAILED = "Authentication failed for user %s";
//    public static final String USER_NO_PERMISSION = "User %s has no permission to perform this operation";
//    public static final String USER_OPERATION_FAILED = "Operation failed for user %s: %s";

    // 中文版
    public static final String USER_CREATED = "用户 %s 创建成功";
    public static final String USER_LOGIN = "用户 %s 登录成功";
    public static final String USER_LOGIN_FAILED = "用户 %s 登录失败";
    public static final String USER_UPDATED = "用户 %s 数据更新成功";
    public static final String USER_DELETED = "用户 %s 删除成功";
    public static final String USER_NOT_FOUND = "用户 %s 不存在";
    public static final String USER_INFO_UPDATE = "用户 %s 信息更新成功";
    public static final String USER_ALREADY_EXISTS = "用户 %s 已存在";
    public static final String EMAIL_ALREADY_EXISTS = "邮箱 %s 已存在";
    public static final String PHONE_ALREADY_EXISTS = "手机号 %s 已存在";
    public static final String USER_AUTH_FAILED = "用户 %s 认证失败";
    public static final String USER_NO_PERMISSION = "用户 %s 没有权限执行该操作";
    public static final String USER_OPERATION_FAILED = "用户 %s 操作失败";

    private UserConstants() {
        throw new IllegalStateException("Utility class");
    }

}
