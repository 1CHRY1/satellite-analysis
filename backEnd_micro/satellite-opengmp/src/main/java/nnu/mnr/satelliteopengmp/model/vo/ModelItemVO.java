package nnu.mnr.satelliteopengmp.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import nnu.mnr.satelliteopengmp.model.pojo.Param;

import java.util.Date;
import java.util.List;

/**
 * @Description
 * @Auther wyjq
 * @Date 2022/4/6
 **/

@Data
public class ModelItemVO {

    String name; //模型名称
    String description; //概述
    String author; //作者
    String problemTags="";  //地理问题标签
    List<String> normalTags; //地理数据常规标签
    String modelType; //模型方法(model) or 数据方法 (method)
    List<Param> params;
    List<Param> inputParams;
    List<Param> outputParams;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    Date createTime=new Date(); //创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    Date updateTime=new Date(); //创建时间

}
