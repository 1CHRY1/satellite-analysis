package com.ogms.dge.computing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/***
 *
 * t_user
 *
 * id，name，password，email，delete
 *
 * info信息表
 *
 * id，type，
 *
 * data service 数据服务表
 *
 * id，
 * name：数据服务的名字
 * description：数据服务的描述
 * avatar：头像
 * createtime：创建时间
 * available：是否可用（外部是否可用访问）
 * delete：是否删除
 *
 * 访问记录表
 *
 *
 *
 *
 * data process service 数据处理服务表
 *
 *  id，
 *  name：数据服务的名字
 *  description：数据服务的描述
 *  avatar：头像
 *  createtime：创建时间
 *  available：是否可用（外部是否可用访问）
 *  delete：是否删除
 *
 * 运行记录表
 *
 */

@SpringBootApplication
public class ComputingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ComputingApplication.class, args);
    }


    // 上传文件


    // 下载文件


    //



}
