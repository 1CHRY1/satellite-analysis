package nnu.mnr.satellite.model.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/11 21:16
 * @Description:
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(buildMethodName = "buildImage")
@TableName("image_table")
public class Image {
    @TableId
    private String imageId;
    private String sceneId;
    private String band;
    private String tifPath;
    private String bucket;
}
