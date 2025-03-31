package nnu.mnr.satellite.service.user;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.config.security.CustomUserDetailsService;
import nnu.mnr.satellite.constants.UserConstants;
import nnu.mnr.satellite.model.dto.user.UserInfoDTO;
import nnu.mnr.satellite.model.dto.user.UserLoginDTO;
import nnu.mnr.satellite.model.dto.user.UserRegisterDTO;
import nnu.mnr.satellite.model.po.user.User;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.model.vo.modeling.ProjectVO;
import nnu.mnr.satellite.model.vo.user.UserVO;
import nnu.mnr.satellite.repository.modeling.IUserRepo;
import nnu.mnr.satellite.utils.common.IdUtil;
import nnu.mnr.satellite.utils.data.RedisUtil;
import nnu.mnr.satellite.utils.security.JwtUtil;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/27 17:30
 * @Description:
 */

@Service
@Slf4j
public class UserService {

    private final IUserRepo userRepo;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Autowired
    ModelMapper userModelMapper;

    @Autowired
    CustomUserDetailsService userDetailsService;

    public UserService(IUserRepo userRepo, BCryptPasswordEncoder passwordEncoder, AuthenticationManager authenticationManager) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    public Boolean ifUserExist(String userId) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        return userRepo.exists(queryWrapper);
    }

    public CommonResultVO userRegister(UserRegisterDTO userRegisterDTO) {
        String email = userRegisterDTO.getEmail();
        String name = userRegisterDTO.getUserName();
        if (getUserByName(name).isPresent()) {
            return CommonResultVO.builder().status(UserConstants.FAILURE_STATUS)
                    .message(String.format(UserConstants.USER_ALREADY_EXISTS, name)).build();
        } else if (getUserByEmail(email).isPresent()) {
            return CommonResultVO.builder().status(UserConstants.FAILURE_STATUS)
                    .message(String.format(UserConstants.EMAIL_ALREADY_EXISTS, email)).build();
        }
        String userId = IdUtil.generateUserId();
        String encryptedPassword = passwordEncoder.encode(userRegisterDTO.getPassword());
        User user = User.builder()
                .userId(userId).userName(name).email(email).password(encryptedPassword)
                .organization(userRegisterDTO.getOrganization()).title(userRegisterDTO.getTitle())
                .createTime(LocalDateTime.now()).role("USER")
                .build();
        userRepo.insert(user);
        return CommonResultVO.builder().status(UserConstants.SUCCESS_STATUS)
                .message(String.format(UserConstants.USER_CREATED, name)).build();
    }

    public CommonResultVO userLogin(UserLoginDTO userLoginDTO) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                userLoginDTO.getUserName() == null ?
                    new UsernamePasswordAuthenticationToken(userLoginDTO.getEmail(), userLoginDTO.getPassword()) :
                    new UsernamePasswordAuthenticationToken(userLoginDTO.getUserName(), userLoginDTO.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String email = userLoginDTO.getEmail();
            String name = userLoginDTO.getUserName();
            Optional<User> optionalUser = name == null ? getUserByEmail(email) : getUserByName(name);
            User user = optionalUser.get();
            String accessToken = JwtUtil.generateAccessToken(user.getUserId(), user.getUserName(), authentication.getAuthorities());
            String refreshToken = JwtUtil.generateRefreshToken(user.getUserId());
            Map<String, String> tokens = new HashMap<>();
            tokens.put("accessToken", accessToken);
            tokens.put("refreshToken", refreshToken);
            tokens.put("userId", user.getUserId());
            log.info("User logged in: {}", user.getUserName());
            return CommonResultVO.builder()
                    .status(1)
                    .message(String.format(UserConstants.USER_LOGIN, user.getUserName()))
                    .data(tokens)
                    .build();
        } catch (Exception e) {
            log.warn("Login failed for user: {}", userLoginDTO.getUserName(), e);
            return CommonResultVO.builder()
                    .status(-1)
                    .message(String.format(UserConstants.USER_LOGIN_FAILED, userLoginDTO.getUserName()))
                    .build();
        }
    }

    public CommonResultVO refreshToken(String refreshToken) {
        try {
            // 检查 Refresh Token 是否过期
            if (JwtUtil.isTokenExpired(refreshToken)) {
                log.warn("Refresh token expired");
                return CommonResultVO.builder()
                        .status(-1)
                        .message("Refresh token has expired")
                        .build();
            }

            String userId = JwtUtil.getUserIdFromToken(refreshToken);

            if (userId == null ) {
                log.warn("Invalid refresh token: missing userId or username");
                return CommonResultVO.builder()
                        .status(-1)
                        .message("Invalid refresh token")
                        .build();
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(userId);
            String newAccessToken = JwtUtil.generateAccessToken(userId, userDetails.getUsername(), userDetails.getAuthorities());

            Map<String, String> tokens = new HashMap<>();
            tokens.put("accessToken", newAccessToken);

            log.info("Access token refreshed successfully for user: {}", userId);
            return CommonResultVO.builder()
                    .status(1)
                    .message("Access token refreshed successfully")
                    .data(tokens)
                    .build();
        } catch (Exception e) {
            log.error("Failed to refresh token", e);
            return CommonResultVO.builder()
                    .status(-1)
                    .message("Failed to refresh token: " + e.getMessage())
                    .build();
        }
    }

    public UserVO getUserVOById(String userId) {
        User user = userRepo.selectById(userId);
        return userModelMapper.map(user, new TypeToken<UserVO>() {}.getType());
    }

    public CommonResultVO updateUserInfoById(UserInfoDTO userInfoDTO) {
        String userId = userInfoDTO.getUserId();
        User user = userRepo.selectById(userId);
        if (user == null) {
            return CommonResultVO.builder()
                    .message(String.format(UserConstants.USER_NOT_FOUND, userId)).status(-1).build();
        }
        user.setCity(userInfoDTO.getCity()); user.setIntroduction(userInfoDTO.getIntroduction());
        user.setOrganization(userInfoDTO.getOrganization()); user.setProvince(userInfoDTO.getProvince());
        user.setPhone(userInfoDTO.getPhone()); user.setTitle(userInfoDTO.getTitle());
        userRepo.updateById(user);
        return CommonResultVO.builder()
                .message(String.format(UserConstants.USER_INFO_UPDATE, userId)).status(1).build();
    }

    private Optional<User> getUserByName(String name) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", name);
        return Optional.ofNullable(userRepo.selectOne(queryWrapper));
    }

    private Optional<User> getUserByEmail(String email) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", email);
        return Optional.ofNullable(userRepo.selectOne(queryWrapper));
    }

}
