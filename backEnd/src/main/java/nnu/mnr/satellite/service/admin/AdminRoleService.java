package nnu.mnr.satellite.service.admin;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.mapper.admin.IRoleRepo;
import nnu.mnr.satellite.mapper.user.IUserRepo;
import nnu.mnr.satellite.model.dto.admin.role.RoleDeleteDTO;
import nnu.mnr.satellite.model.dto.admin.role.RoleInsertDTO;
import nnu.mnr.satellite.model.dto.admin.role.RolePageDTO;
import nnu.mnr.satellite.model.dto.admin.role.RoleUpdateDTO;
import nnu.mnr.satellite.model.po.admin.Role;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@DS("mysql-ard-iam")
public class AdminRoleService {

    @Autowired
    private IRoleRepo roleRepo;

    @Autowired
    private IUserRepo userRepo;

    public CommonResultVO getAllRoleInfo(){

        List<Role> role = roleRepo.selectList(null);
        return CommonResultVO.builder()
                .status(1)
                .message("角色信息获取成功")
                .data(role)
                .build();
    }

    public CommonResultVO getRoleInfoPage(RolePageDTO rolePageDTO){
        // 构造分页对象
        Page<Role> page = new Page<>(rolePageDTO.getPage(), rolePageDTO.getPageSize());

        String sortField = rolePageDTO.getSortField();
        Boolean asc = rolePageDTO.getAsc();
        String searchText = rolePageDTO.getSearchText();
        LambdaQueryWrapper<Role> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        // 筛选
        if (searchText != null && !searchText.trim().isEmpty()) {
            String trimmedSearchText = searchText.trim();
            lambdaQueryWrapper.and(wrapper ->
                    wrapper.like(Role::getName, trimmedSearchText)
            );
        }

        // 排序
        if (sortField != null && !sortField.isEmpty()) {
            // 使用 sortField 对应的数据库字段进行排序
            switch (sortField) {
                case "roleId":
                    lambdaQueryWrapper.orderBy(true, asc, Role::getRoleId);
                    break;
                case "name":
                    lambdaQueryWrapper.orderBy(true, asc, Role::getName);
                    break;
                case "description":
                    lambdaQueryWrapper.orderBy(true, asc, Role::getDescription);
                    break;
                case "maxCpu":
                    lambdaQueryWrapper.orderBy(true, asc, Role::getMaxCpu);
                    break;
                case "maxStorage":
                    lambdaQueryWrapper.orderBy(true, asc, Role::getMaxStorage);
                    break;
                case "maxJob":
                    lambdaQueryWrapper.orderBy(true, asc, Role::getMaxJob);
                    break;
                case "isSuperAdmin":
                    lambdaQueryWrapper.orderBy(true, asc, Role::getIsSuperAdmin);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported sort field: " + sortField);
            }
        }
        // 查询，lambdaQueryWrapper没有显式指定select，默认select *
        IPage<Role> rolePage = roleRepo.selectPage(page, lambdaQueryWrapper);

        return CommonResultVO.builder()
                .status(1)
                .message("角色信息获取成功")
                .data(rolePage)
                .build();
    }

    public CommonResultVO getRoleInfoById(String roleId){
        Role role = roleRepo.selectById(roleId);
        if(role == null){
            return CommonResultVO.builder()
                    .status(1)
                    .message("角色信息获取失败，角色不存在")
                    .build();
        }else {
            return CommonResultVO.builder()
                    .status(1)
                    .message("角色信息获取成功")
                    .data(role)
                    .build();
        }

    }

    public CommonResultVO insertRole(RoleInsertDTO roleInsertDTO){
        Role role = new Role();
        BeanUtils.copyProperties(roleInsertDTO, role);
        roleRepo.insert(role);
        return CommonResultVO.builder()
                .status(1)
                .message("新增角色成功")
                .build();
    }

    public CommonResultVO updateRole(RoleUpdateDTO roleUpdateDTO){
        Role role = new Role();
        BeanUtils.copyProperties(roleUpdateDTO, role);
        roleRepo.updateById(role);
        return CommonResultVO.builder()
                .status(1)
                .message("更新角色成功")
                .build();
    }

    public CommonResultVO deleteRole(RoleDeleteDTO roleDeleteDTO){
        // 1. 检查是否有用户关联这些角色
        List<Integer> roleIds = roleDeleteDTO.getRoleIds();
        boolean hasUsers = userRepo.existsByRoleIds(roleIds);

        if (hasUsers) {
            return CommonResultVO.builder()
                    .status(-1)
                    .message("删除失败：仍有用户属于该角色，请先解除关联")
                    .build();
        }

        // 2. 如果没有关联用户，执行删除
        roleRepo.deleteByIds(roleIds);
        return CommonResultVO.builder()
                .status(1)
                .message("删除角色成功")
                .build();
    }

}
