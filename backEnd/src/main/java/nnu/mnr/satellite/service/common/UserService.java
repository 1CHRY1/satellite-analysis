package nnu.mnr.satellite.service.common;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import nnu.mnr.satellite.constants.UserConstants;
import nnu.mnr.satellite.model.dto.user.UserLoginDTO;
import nnu.mnr.satellite.model.dto.user.UserRegisterDTO;
import nnu.mnr.satellite.model.po.common.User;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.repository.modeling.IUserRepo;
import nnu.mnr.satellite.utils.common.IdUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/27 17:30
 * @Description:
 */

@Service
public class UserService {

    private final IUserRepo userRepo;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(IUserRepo userRepo) {
        this.userRepo = userRepo;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public CommonResultVO userRegister(UserRegisterDTO userRegisterDTO) {
        String email = userRegisterDTO.getEmail();
        String name = userRegisterDTO.getUserName();
        if (getUserByName(name) != null) {
            return CommonResultVO.builder().status(UserConstants.FAILURE_STATUS)
                    .message(String.format(UserConstants.USER_ALREADY_EXISTS, name)).build();
        } else if (getUserByEmail(email) != null) {
            return CommonResultVO.builder().status(UserConstants.FAILURE_STATUS)
                    .message(String.format(UserConstants.EMAIL_ALREADY_EXISTS, email)).build();
        }
        String userId = IdUtil.generateUserId();
        String encryptedPassword = passwordEncoder.encode(userRegisterDTO.getPassword());
        User user = User.builder()
                .userId(userId).userName(name).email(email).password(encryptedPassword)
                .organization(userRegisterDTO.getOrganization()).title(userRegisterDTO.getTitle())
                .createdTime(LocalDateTime.now())
                .build();
        userRepo.insert(user);
        return CommonResultVO.builder().status(UserConstants.SUCCESS_STATUS)
                .message(String.format(UserConstants.USER_CREATED, name)).build();
    }

    private User getUserByName(String name) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", name);
        return userRepo.selectOne(queryWrapper);
    }

    private User getUserByEmail(String email) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", email);
        return userRepo.selectOne(queryWrapper);
    }

    public CommonResultVO userLogin(UserLoginDTO userLoginDTO) {
        String email = userLoginDTO.getEmail();
        String name = userLoginDTO.getUserName();
        String password = userLoginDTO.getPassword();
        User user = name == null ? getUserByEmail(email) : getUserByName(name);
        if (getUserByName(name) == null || getUserByEmail(email) == null) {
            return CommonResultVO.builder()
                    .status(UserConstants.FAILURE_STATUS)
                    .message(String.format(UserConstants.USER_NOT_FOUND, name))
                    .build();
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return CommonResultVO.builder()
                    .status(UserConstants.FAILURE_STATUS)
                    .message(String.format(UserConstants.USER_AUTH_FAILED, name))
                    .build();
        }
        return CommonResultVO.builder()
                .status(UserConstants.SUCCESS_STATUS)
                .message(String.format(UserConstants.USER_LOGIN, name))
                .data(user.getUserId())
                .build();
    }

    /**
     * 根据 ID 查询用户信息
     */
    public CommonResultVO getUserById(String userId) {
        User user = userRepo.selectById(userId);
        if (user == null) {
            return CommonResultVO.builder()
                    .status(UserConstants.FAILURE_STATUS)
                    .message(String.format(UserConstants.USER_NOT_FOUND, userId))
                    .build();
        }

        return CommonResultVO.builder()
                .status(UserConstants.SUCCESS_STATUS)
                .message("User retrieved successfully")
                .data(user)
                .build();
    }


}
