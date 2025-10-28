package nnu.mnr.satellite.service.user;

import com.baomidou.dynamic.datasource.annotation.DS;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.mapper.admin.IRoleRepo;
import nnu.mnr.satellite.model.po.admin.Role;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@DS("mysql-ard-iam")
public class RoleService {

    @Autowired
    private IRoleRepo roleRepo;

    public CommonResultVO getRoleInfoById(String roleId){
        Role role = roleRepo.selectById(roleId);
        return CommonResultVO.builder()
                .status(1)
                .message("角色信息获取成功")
                .data(role)
                .build();
    }
}
