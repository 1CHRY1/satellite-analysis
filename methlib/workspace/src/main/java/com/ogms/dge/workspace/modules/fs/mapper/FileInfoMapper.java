package com.ogms.dge.workspace.modules.fs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ogms.dge.workspace.modules.fs.entity.FileInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * 文件信息 Mapper 接口
 * </p>
 *
 * @author jin wang
 * @since 2023-08-30
 */
@Mapper
public interface FileInfoMapper extends BaseMapper<FileInfo> {
    String colms = "file_id, user_id, file_md5, file_pid, file_size, file_name, file_cover, file_path, create_time, last_update_time, folder_type, file_category, file_type, status, recovery_time, del_flag";

    /**
     * 查询用户已用存储空间
     * @param userId
     * @return
     */
    @Select("SELECT IFNULL(SUM(file_size), 0) FROM workspace_file WHERE user_id = #{userId}")
    Long selectUseSpace(Long userId);

    /**
     * 根据 fileId 和 userId 获取文件信息
     * @param fileId
     * @param userId
     * @return
     */
    @Select("select "+ colms +" from workspace_file where user_id = #{userId} and file_id = #{fileId}")
    FileInfo getFileInfoByFileIdAndUserId(String fileId, Long userId);
}
