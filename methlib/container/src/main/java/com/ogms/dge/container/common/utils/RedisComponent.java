package com.ogms.dge.container.common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ogms.dge.container.modules.fs.entity.dto.DownloadFileDto;
import com.ogms.dge.container.modules.fs.entity.dto.SysSettingsDto;
import com.ogms.dge.container.modules.fs.entity.dto.UserSpaceDto;
import com.ogms.dge.container.modules.fs.mapper.FileInfoMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class RedisComponent {

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private FileInfoMapper fileInfoMapper;

    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取系统设置
     *
     * @return
     */
    public SysSettingsDto getSysSettingsDto() {
        SysSettingsDto sysSettingsDto = (SysSettingsDto) redisUtils.get(Constant.REDIS_KEY_SYS_SETTING, SysSettingsDto.class);
        if (sysSettingsDto == null) {
            sysSettingsDto = new SysSettingsDto();
            redisUtils.set(Constant.REDIS_KEY_SYS_SETTING, sysSettingsDto);
        }
        return sysSettingsDto;
    }

    /**
     * 保存邮箱设置
     *
     * @param sysSettingsDto
     */
    public void saveSysSettingsDto(SysSettingsDto sysSettingsDto) {
        redisUtils.set(Constant.REDIS_KEY_SYS_SETTING, sysSettingsDto);
    }

    /**
     * 保存下载码
     * @param code
     * @param downloadFileDto
     */
    public void saveDownloadCode(String code, DownloadFileDto downloadFileDto) {
        redisUtils.setex(Constant.REDIS_KEY_DOWNLOAD + code, downloadFileDto, Constant.REDIS_KEY_EXPIRES_FIVE_MIN);
    }

    /**
     * 获取下载码
     * @param code
     * @return
     */
    public DownloadFileDto getDownloadCode(String code) {
        return (DownloadFileDto) redisUtils.get(Constant.REDIS_KEY_DOWNLOAD + code, DownloadFileDto.class);
    }

    /**
     * 获取用户使用的空间
     *
     * @param userId
     * @return
     */
    public UserSpaceDto getUserSpaceUse(Long userId) {
        UserSpaceDto spaceDto = (UserSpaceDto) redisUtils.get(Constant.REDIS_KEY_USER_SPACE_USE + userId, UserSpaceDto.class);
        if (null == spaceDto) {
            spaceDto = new UserSpaceDto();
            Long useSpace = this.fileInfoMapper.selectUseSpace(userId);
            spaceDto.setUseSpace(useSpace);
            spaceDto.setTotalSpace(getSysSettingsDto().getUserInitUseSpace() * Constant.MB);
            redisUtils.setex(Constant.REDIS_KEY_USER_SPACE_USE + userId, spaceDto, Constant.REDIS_KEY_EXPIRES_DAY);
        }
        return spaceDto;
    }

    /**
     * 保存已使用的空间
     *
     * @param userId
     */
    public void saveUserSpaceUse(String userId, UserSpaceDto userSpaceDto) {
        redisUtils.setex(Constant.REDIS_KEY_USER_SPACE_USE + userId,
                userSpaceDto, Constant.REDIS_KEY_EXPIRES_DAY);
    }

//    /**
//     * 重置用户用户空间大小
//     * @param userId
//     * @return
//     */
//    public UserSpaceDto resetUserSpaceUse(Long userId) {
//        UserSpaceDto spaceDto = new UserSpaceDto();
//        Long useSpace = this.fileInfoMapper.selectUseSpace(userId);
//        spaceDto.setUseSpace(useSpace);
//
//        UserInfo userInfo = this.userInfoMapper.selectByUserId(userId);
//        spaceDto.setTotalSpace(userInfo.getTotalSpace());
//        redisUtils.setex(Constant.REDIS_KEY_USER_SPACE_USE + userId, spaceDto, Constant.REDIS_KEY_EXPIRES_DAY);
//        return spaceDto;
//    }

    /**
     * 保存文件临时大小
     * @param userId
     * @param fileId
     * @param fileSize
     */
    public void saveFileTempSize(String userId, String fileId, Long fileSize) {
        Long currentSize = getFileTempSize(userId, fileId);
        redisUtils.setex(Constant.REDIS_KEY_USER_FILE_TEMP_SIZE + userId + fileId,
                currentSize + fileSize,
                Constant.REDIS_KEY_EXPIRES_ONE_HOUR);
    }

    /**
     * 获取文件临时大小
     * @param userId
     * @param fileId
     * @return
     */
    public Long getFileTempSize(String userId, String fileId) {
        Long currentSize = getFileSizeFromRedis(Constant.REDIS_KEY_USER_FILE_TEMP_SIZE + userId + fileId);
        return currentSize;
    }

    /**
     * 从Redis中获取文件大小
     * @param key
     * @return
     */
    private Long getFileSizeFromRedis(String key) {
        Object sizeObj = redisUtils.get(key, Object.class);
        if (sizeObj == null) {
            return 0L;
        }
        if (sizeObj instanceof Integer) {
            return ((Integer) sizeObj).longValue();
        } else if (sizeObj instanceof Long) {
            return (Long) sizeObj;
        }

        return 0L;
    }
}
