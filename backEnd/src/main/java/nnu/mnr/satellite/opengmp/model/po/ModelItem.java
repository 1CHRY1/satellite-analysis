package nnu.mnr.satellite.opengmp.model.po;

import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import nnu.mnr.satellite.opengmp.model.pojo.ModelItemData;
import nnu.mnr.satellite.opengmp.model.pojo.Param;
import org.springframework.data.annotation.Id;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @Description
 * @Auther wyjq
 * @Date 2022/4/6
 **/

@Data
public class ModelItem {

    @Id
    String id;

    String name; //模型名称
    String description; //概述
    String author; //作者

    String image; // 模型图片

    String problemTags="";  //地理问题标签
    List<String> normalTags; //地理数据常规标签

    Boolean publicBoolean=true; //是否在资源中心可见

    String modelType; //模型方法(model) or 数据方法 (method)

    //模型方法属性
    String md5; //模型的md5
    String mdl;  //模型的mdl xml直接存储为string
    Object mdlJson; //模型mdl的json格式
    ModelItemData data; //模型数据

    //数据方法属性
    String uuid;
    List<Param> params;
    List<Param> inputParams;
    List<Param> outputParams;

    List<JSONObject> testDataList = new ArrayList<>(); //模型绑定的测试数据

    String imgStoreName; //图像文件名字
    String imgWebAddress; //图像文件下载请求的地址
    String imgRelativePath;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    Date createTime=new Date(); //创建时间

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    Date updateTime=new Date(); //创建时间
}
