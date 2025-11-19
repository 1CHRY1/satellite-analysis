package nnu.mnr.satellite.service.admin;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.minio.admin.UserInfo;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.constants.UserConstants;
import nnu.mnr.satellite.mapper.user.IUserRepo;
import nnu.mnr.satellite.model.dto.admin.user.UserDeleteDTO;
import nnu.mnr.satellite.model.dto.admin.user.UserInsertDTO;
import nnu.mnr.satellite.model.dto.admin.user.UserPageDTO;
import nnu.mnr.satellite.model.dto.admin.user.UserUpdateDTO;
import nnu.mnr.satellite.model.dto.user.AvatarUploadDTO;
import nnu.mnr.satellite.model.po.user.User;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.model.vo.user.UserVO;
import nnu.mnr.satellite.service.user.UserService;
import nnu.mnr.satellite.utils.common.IdUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@DS("mysql-ard-iam")
public class AdminUserService {

    @Autowired
    private IUserRepo userRepo;

    @Autowired
    private UserService userService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public CommonResultVO getUserInfoPage(UserPageDTO userPageDTO){
        // 构造分页对象
        Page<User> page = new Page<>(userPageDTO.getPage(), userPageDTO.getPageSize());

        LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();

        // 筛选
        String searchText = userPageDTO.getSearchText();
        List<Integer> roleIds = userPageDTO.getRoleIds();

        if (roleIds != null && !roleIds.isEmpty()) {
            lambdaQueryWrapper.in(User::getRoleId, roleIds);
        }

        if (searchText != null && !searchText.trim().isEmpty()) {
            String trimmedSearchText = searchText.trim();
            lambdaQueryWrapper.and(wrapper ->
                    wrapper.like(User::getUserName, trimmedSearchText)
                            .or().like(User::getEmail, trimmedSearchText)
                            .or().like(User::getPhone, trimmedSearchText)
                            .or().like(User::getProvince, trimmedSearchText)
                            .or().like(User::getCity, trimmedSearchText)
                            .or().like(User::getTitle, trimmedSearchText)
                            .or().like(User::getOrganization, trimmedSearchText)
            );
        }

        // 排序
        String sortField = userPageDTO.getSortField();
        Boolean asc = userPageDTO.getAsc();
        if (sortField != null && !sortField.isEmpty()) {
            // 使用 sortField 对应的数据库字段进行排序
            switch (sortField) {
                case "createTime":
                    lambdaQueryWrapper.orderBy(true, asc, User::getCreateTime);
                    break;
                case "userName":
                    lambdaQueryWrapper.orderBy(true, asc, User::getUserName);
                    break;
                case "email":
                    lambdaQueryWrapper.orderBy(true, asc, User::getEmail);
                    break;
                case "phone":
                    lambdaQueryWrapper.orderBy(true, asc, User::getPhone);
                    break;
                case "province":
                    lambdaQueryWrapper.orderBy(true, asc, User::getProvince);
                    break;
                case "city":
                    lambdaQueryWrapper.orderBy(true, asc, User::getCity);
                    break;
                case "title":
                    lambdaQueryWrapper.orderBy(true, asc, User::getTitle);
                    break;
                case "organization":
                    lambdaQueryWrapper.orderBy(true, asc, User::getOrganization);
                    break;
                case "roleId":
                    lambdaQueryWrapper.orderBy(true, asc, User::getRoleId);
                    break;
                case "role":
                    lambdaQueryWrapper.orderBy(true, asc, User::getRole);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported sort field: " + sortField);
            }
        }
        // 查询，lambdaQueryWrapper没有显式指定selecet，默认select *
        IPage<User> userPage = userRepo.selectPage(page, lambdaQueryWrapper);
        // 映射，匿掉password字段
        IPage<UserVO> userVOPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        List<UserVO> userVOList = userPage.getRecords().stream().map(user -> {
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            return userVO;
        }).collect(Collectors.toList());
        userVOPage.setRecords(userVOList);

        return CommonResultVO.builder()
                .status(1)
                .message("用户信息获取成功")
                .data(userVOPage)
                .build();
    }

    public CommonResultVO getUserInfoById(String userId){
        User user = userRepo.selectById(userId);
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return CommonResultVO.builder()
                .status(1)
                .message("用户信息获取成功")
                .data(userVO)
                .build();
    }

    public CommonResultVO insertUser(UserInsertDTO userInsertDTO){
        String email = userInsertDTO.getEmail();
        String name = userInsertDTO.getUserName();
        if (userService.getUserByName(name).isPresent()) {
            return CommonResultVO.builder().status(UserConstants.FAILURE_STATUS)
                    .message(String.format(UserConstants.USER_ALREADY_EXISTS, name)).build();
        } else if (userService.getUserByEmail(email).isPresent()) {
            return CommonResultVO.builder().status(UserConstants.FAILURE_STATUS)
                    .message(String.format(UserConstants.EMAIL_ALREADY_EXISTS, email)).build();
        }
        String userId = IdUtil.generateUserId();
        String encryptedPassword = passwordEncoder.encode(userInsertDTO.getPassword());
        User user = User.builder()
                .userId(userId).userName(name).email(email).password(encryptedPassword)
                .organization(userInsertDTO.getOrganization()).title(userInsertDTO.getTitle())
                .createTime(LocalDateTime.now()).role("USER").roleId(userInsertDTO.getRoleId())
                .build();
        userRepo.insert(user);
        return CommonResultVO.builder().status(UserConstants.SUCCESS_STATUS)
                .message(String.format(UserConstants.USER_CREATED, name))
                .data(user.getUserId()).build();
    }

    public CommonResultVO updateUser(UserUpdateDTO userUpdateDTO){
        String userId = userUpdateDTO.getUserId();
        String userName = userUpdateDTO.getUserName();
        User user = userRepo.selectById(userId);
        if (user == null) {
            return CommonResultVO.builder()
                    .message(String.format(UserConstants.USER_NOT_FOUND, userName)).status(-1).build();
        }
        if (userService.getUserByName(userName).isPresent() && !userName.equals(user.getUserName())) {
            return CommonResultVO.builder().status(UserConstants.FAILURE_STATUS)
                    .message(String.format(UserConstants.USER_ALREADY_EXISTS, userName)).build();
        }
        String email = userUpdateDTO.getEmail();
        if (userService.getUserByEmail(email).isPresent() && !email.equals(user.getEmail())) {
            return CommonResultVO.builder().status(UserConstants.FAILURE_STATUS)
                    .message(String.format(UserConstants.EMAIL_ALREADY_EXISTS, email)).build();
        }
        String phone = userUpdateDTO.getPhone();
        if (userService.getUserByPhone(phone).isPresent() && !phone.equals(user.getPhone())) {
            return CommonResultVO.builder().status(UserConstants.FAILURE_STATUS)
                    .message(String.format(UserConstants.PHONE_ALREADY_EXISTS, phone)).build();
        }
        if (userUpdateDTO.getPassword()!= null){
            String encryptedPassword = passwordEncoder.encode(userUpdateDTO.getPassword());
            user.setPassword(encryptedPassword);
        }

        user.setUserName(userUpdateDTO.getUserName());
        user.setPhone(userUpdateDTO.getPhone());
        user.setProvince(userUpdateDTO.getProvince());
        user.setCity(userUpdateDTO.getCity());
        user.setEmail(userUpdateDTO.getEmail());
        user.setTitle(userUpdateDTO.getTitle());
        user.setOrganization(userUpdateDTO.getOrganization());
        user.setIntroduction(userUpdateDTO.getIntroduction());
        user.setRoleId(userUpdateDTO.getRoleId());

        userRepo.updateById(user);
        return CommonResultVO.builder()
                .message(String.format(UserConstants.USER_INFO_UPDATE, userRepo.selectById(userId).getUserName())).status(1).build();
    }

    public CommonResultVO uploadAvatar(AvatarUploadDTO avatarUploadDTO){
        return userService.uploadAvatar(avatarUploadDTO);
    }

    public CommonResultVO deleteUser(UserDeleteDTO userDeleteDTO){
        List<String> userIds = userDeleteDTO.getUserIds();
        userRepo.deleteByIds(userIds);
        return CommonResultVO.builder()
                .status(1)
                .message("删除用户成功")
                .build();
    }
}
