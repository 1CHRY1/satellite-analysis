package nnu.mnr.satellite.service.user;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.config.security.CustomUserDetailsService;
import nnu.mnr.satellite.constants.UserConstants;
import nnu.mnr.satellite.mapper.user.IRecordRepo;
import nnu.mnr.satellite.model.dto.user.*;
import nnu.mnr.satellite.model.po.user.Record;
import nnu.mnr.satellite.model.vo.user.RecordInfoVO;
import nnu.mnr.satellite.model.po.user.User;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.model.vo.user.UserVO;
import nnu.mnr.satellite.mapper.user.IUserRepo;
import nnu.mnr.satellite.utils.common.IdUtil;
import nnu.mnr.satellite.utils.dt.MinioUtil;
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
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/27 17:30
 * @Description:
 */

@Service
@Slf4j
@DS("mysql_ard_iam")
public class UserService {

    private final IUserRepo userRepo;
    private final IRecordRepo recordRepo;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Autowired
    ModelMapper userModelMapper;

    @Autowired
    CustomUserDetailsService userDetailsService;

    @Autowired
    MinioUtil minioUtil;

    public UserService(IUserRepo userRepo, IRecordRepo recordRepo, BCryptPasswordEncoder passwordEncoder, AuthenticationManager authenticationManager) {
        this.userRepo = userRepo;
        this.recordRepo = recordRepo;
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

    public User getUserById(String userId) {
        return userRepo.selectById(userId);
    }

    public CommonResultVO updateUserInfoById(UserInfoDTO userInfoDTO) {
        String userId = userInfoDTO.getUserId();
        String userName = userInfoDTO.getUserName();
        User user = userRepo.selectById(userId);
        if (user == null) {
            return CommonResultVO.builder()
                    .message(String.format(UserConstants.USER_NOT_FOUND, userName)).status(-1).build();
        }
        if (getUserByName(userName).isPresent() && !userName.equals(user.getUserName())) {
            return CommonResultVO.builder().status(UserConstants.FAILURE_STATUS)
                    .message(String.format(UserConstants.USER_ALREADY_EXISTS, userName)).build();
        }
        String email = userInfoDTO.getEmail();
        if (getUserByEmail(email).isPresent() && !email.equals(user.getEmail())) {
            return CommonResultVO.builder().status(UserConstants.FAILURE_STATUS)
                    .message(String.format(UserConstants.EMAIL_ALREADY_EXISTS, email)).build();
        }
        String phone = userInfoDTO.getPhone();
        if (getUserByPhone(phone).isPresent() && !phone.equals(user.getPhone())) {
            return CommonResultVO.builder().status(UserConstants.FAILURE_STATUS)
                    .message(String.format(UserConstants.PHONE_ALREADY_EXISTS, phone)).build();
        }

        user.setUserName(userInfoDTO.getUserName());
        user.setPhone(userInfoDTO.getPhone());
        user.setProvince(userInfoDTO.getProvince());
        user.setCity(userInfoDTO.getCity());
        user.setEmail(userInfoDTO.getEmail());
        user.setTitle(userInfoDTO.getTitle());
        user.setOrganization(userInfoDTO.getOrganization());
        user.setIntroduction(userInfoDTO.getIntroduction());

        userRepo.updateById(user);
        return CommonResultVO.builder()
                .message(String.format(UserConstants.USER_INFO_UPDATE, userName)).status(1).build();
    }

    public CommonResultVO updateUserPasswordById(UserPasswordDTO userPasswordDTO) {
        String userId = userPasswordDTO.getUserId();
        String userName = userPasswordDTO.getUserName();
        User user = userRepo.selectById(userId);
        if (user == null) {
            return CommonResultVO.builder()
                    .message(String.format(UserConstants.USER_NOT_FOUND, userName)).status(-1).build();
        }
        if (!passwordEncoder.matches(userPasswordDTO.getOldPassword(), user.getPassword())){
            return CommonResultVO.builder()
                    .message("旧密码不正确").status(-1).build();
        } else if (Objects.equals(userPasswordDTO.getOldPassword(), userPasswordDTO.getNewPassword())) {
            return CommonResultVO.builder()
                    .message("新密码不能与旧密码相同").status(-1).build();
        } else {
            String encryptedPassword = passwordEncoder.encode(userPasswordDTO.getNewPassword());
            user.setPassword(encryptedPassword);
            userRepo.updateById(user);
            return CommonResultVO.builder()
                    .message("密码修改成功").status(1).build();
        }
    }

    public CommonResultVO getAvatar(String userId){
        User user = userRepo.selectById(userId);
        String avatarPath = user.getAvatarPath();
        JSONObject avatarPathJSON = new JSONObject();
        avatarPathJSON.put("avatarPath", avatarPath);
        return CommonResultVO.builder()
                .status(1)
                .message("头像获取成功")
                .data(avatarPathJSON)
                .build();
    }

    public CommonResultVO uploadAvatar(AvatarUploadDTO avatarUploadDTO) {
        String userId = avatarUploadDTO.getUserId();
        String userName = avatarUploadDTO.getUserName();
        MultipartFile file = avatarUploadDTO.getFile();

        // 1. 检查用户是否存在
        User user = userRepo.selectById(userId);
        if (user == null) {
            return CommonResultVO.builder()
                    .message(String.format(UserConstants.USER_NOT_FOUND, userName))
                    .status(-1)
                    .build();
        }
        // 2. 检查文件是否为空
        if (file.isEmpty()) {
            return CommonResultVO.builder()
                    .message("头像不能为空")
                    .status(-1)
                    .build();
        }
        // 3. 检查文件类型（仅允许 JPEG 或 PNG）
        String fileType = file.getContentType();
        if (!"image/jpeg".equals(fileType) && !"image/png".equals(fileType)) {
            return CommonResultVO.builder()
                    .message("头像格式不支持，仅支持 JPEG 或 PNG")
                    .status(-1)
                    .build();
        }
        // 4. 检查文件大小（< 1MB）
        long maxSize = 1024 * 1024; // 1MB = 1024 * 1024 bytes
        if (file.getSize() > maxSize) {
            return CommonResultVO.builder()
                    .message("头像大小不能超过 1MB")
                    .status(-1)
                    .build();
        }

        // 5. 文件校验通过，执行上传逻辑
        String bucketName = "user";
        if (minioUtil.existBucket(bucketName)){
            String avatarPath = user.getAvatarPath();
            String filePath = null;
            // 判断是否已有头像路径，有的话直接覆盖，没有的话生成一个
            if (avatarPath == null || avatarPath.isEmpty()) {
                String OriginalFilename = file.getOriginalFilename();
                String suffix = null;
                if (OriginalFilename != null) {
                    suffix = OriginalFilename.substring(OriginalFilename.lastIndexOf("."));
                }else {
                    return CommonResultVO.builder()
                            .message("头像文件名不能为空")
                            .status(-1)
                            .build();
                }
                String fileName = UUID.randomUUID().toString().replace("-", "") + suffix;
                filePath = "/user-avatars/" + fileName;
                minioUtil.upload(file, filePath, bucketName);

                // 更新用户头像信息（示例）
                user.setAvatarPath(bucketName + filePath);
                userRepo.updateById(user);
            }else {
                filePath = avatarPath.substring(avatarPath.indexOf("/"));
                minioUtil.upload(file, filePath, bucketName);
            }
            JSONObject avatarPathJSON = new JSONObject();
            avatarPath = bucketName + filePath;
            avatarPathJSON.put("avatarPath", avatarPath);
            return CommonResultVO.builder()
                    .message("头像上传成功")
                    .status(1)
                    .data(avatarPathJSON)
                    .build();

        }else {
            return CommonResultVO.builder()
                    .message("保存路径不存在，多半是崩了")
                    .status(-1)
                    .build();
        }
    }

    public CommonResultVO getRecordPage(RecordPageFetchDTO recordPageFetchDTO){
        // 构造分页对象
        Page<Record> page = new Page<>(recordPageFetchDTO.getPage(), recordPageFetchDTO.getPageSize());
        // 调用 Mapper 方法
        IPage<Record> recordPage = getRecordsWithCondition(page, recordPageFetchDTO, recordPageFetchDTO.getUserId());
        return CommonResultVO.builder()
                .status(1)
                .message("分页查询成功")
                .data(mapPage(recordPage))
                .build();
    }

    private IPage<Record> getRecordsWithCondition(Page<Record> page, RecordPageFetchDTO recordPageFetchDTO, String userId) {
        String sortField = recordPageFetchDTO.getSortField();
        Boolean asc = recordPageFetchDTO.getAsc();

        LambdaQueryWrapper<Record> lambdaQueryWrapper = new LambdaQueryWrapper<>();

        // 按用户筛选
        lambdaQueryWrapper.eq(Record::getUserId, userId);

        // 添加排序条件
        if (sortField != null && !sortField.isEmpty()) {
            // 使用 sortField 对应的数据库字段进行排序
            switch (sortField) {
                case "actionTime":
                    lambdaQueryWrapper.orderBy(true, asc, Record::getActionTime);
                    break;
                case "actionType":
                    lambdaQueryWrapper.orderBy(true, asc, Record::getActionType);
                    break;
                // 可以根据需要添加更多的字段
                default:
                    throw new IllegalArgumentException("Unsupported sort field: " + sortField);
            }
        }
        return recordRepo.selectPage(page, lambdaQueryWrapper);
    }

    private IPage<RecordInfoVO> mapPage(IPage<Record> recordPage) {
        // 手动映射，自动映射全是null
        userModelMapper.typeMap(Record.class, RecordInfoVO.class)
                .addMappings(mapper -> {
                    mapper.map(Record::getActionId, RecordInfoVO::setActionId);
                    mapper.map(Record::getActionType, RecordInfoVO::setActionType);
                    mapper.map(Record::getActionDetail, RecordInfoVO::setActionDetail);
                    mapper.map(Record::getActionTime, RecordInfoVO::setActionTime);
                });

        // 执行映射
        List<RecordInfoVO> recordInfoVOList = recordPage.getRecords().stream()
                .map(record -> userModelMapper.map(record, RecordInfoVO.class))
                .toList();

        // 创建一个新的 Page 对象
        Page<RecordInfoVO> resultPage = new Page<>();
        resultPage.setRecords(recordInfoVOList);
        resultPage.setTotal(recordPage.getTotal());
        resultPage.setSize(recordPage.getSize());
        resultPage.setCurrent(recordPage.getCurrent());

        return resultPage;
    }

    public CommonResultVO saveRecord(RecordSaveDTO recordSaveDTO){
        if (recordSaveDTO == null || recordSaveDTO.getUserId() == null) {
            return CommonResultVO.builder()
                    .status(-1)
                    .message("参数错误")
                    .build();
        }
        try {
            String userId = recordSaveDTO.getUserId();
            String actionType = recordSaveDTO.getActionType();
            JSONObject actionDetail = recordSaveDTO.getActionDetail();
            String actionTime = recordSaveDTO.getActionTime();
            Record record = Record.builder()
                    .userId(userId)
                    .actionType(actionType)
                    .actionDetail(actionDetail)
                    .build();
            recordRepo.insert(record);
            return CommonResultVO.builder()
                    .status(1)
                    .message("上传记录成功")
                    .build();
        } catch (Exception e) {
            return CommonResultVO.builder()
                    .status(-1)
                    .message("写入数据库失败")
                    .build();
        }
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

    private Optional<User> getUserByPhone(String phone) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone", phone);
        return Optional.ofNullable(userRepo.selectOne(queryWrapper));
    }

}
