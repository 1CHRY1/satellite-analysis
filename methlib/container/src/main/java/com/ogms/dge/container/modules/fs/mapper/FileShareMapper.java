package com.ogms.dge.container.modules.fs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ogms.dge.container.modules.fs.entity.FileShare;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 * 分享信息 Mapper 接口
 * </p>
 *
 * @author jin wang
 * @since 2023-09-07
 */
@Mapper
public interface FileShareMapper extends BaseMapper<FileShare> {

    String colms = "share_id, file_id, user_id, valid_type, expire_time, share_time, code, show_count";

    /**
     * 根据分享id获取分享信息
     * @param shareId
     * @return
     */
    @Select("SELECT "+ colms +" from file_share where share_id = #{shareId}")
    FileShare selectByShareId(String shareId);


    /**
     * 更新【浏览次数】
     * @param shareId
     */
    @Update("update file_share set show_count = show_count + 1 where share_id = #{shareId}")
    void updateShareShowCount(String shareId);
}
