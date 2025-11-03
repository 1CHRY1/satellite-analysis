package com.ogms.dge.container.modules.common.controller;

import com.ogms.dge.container.common.utils.Constant;
import com.ogms.dge.container.common.utils.PageUtils;
import com.ogms.dge.container.common.utils.R;
import com.ogms.dge.container.modules.common.converter.UploadRecordConverter;
import com.ogms.dge.container.modules.common.entity.UploadRecordEntity;
import com.ogms.dge.container.modules.common.service.UploadRecordService;
import com.ogms.dge.container.modules.method.controller.AbstractController;
import com.ogms.dge.container.modules.sys.service.SysUserService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Map;


/**
 * 上传记录
 *
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2024-09-27 14:33:42
 */
@RestController
@RequestMapping("common/uploadrecord")
public class UploadRecordController extends AbstractController {
    @Autowired
    private UploadRecordService uploadRecordService;

    @Resource
    private UploadRecordConverter uploadRecordConverter;

    @Autowired
    private SysUserService sysUserService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        // 只有超级管理员能够查看所有记录信息
        if (getUserId() != Constant.SUPER_ADMIN)
            params.put("user_id", getUserId());
        PageUtils page = uploadRecordService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id) {
        UploadRecordEntity uploadRecord = uploadRecordService.getById(id);

        return R.ok().put("uploadRecord", uploadRecordConverter.po2Vo(uploadRecord, sysUserService));
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody UploadRecordEntity uploadRecord) {
        uploadRecord.setUserId(getUserId());
        uploadRecordService.save(uploadRecord);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody UploadRecordEntity uploadRecord) {
        uploadRecordService.updateById(uploadRecord);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids) {
        uploadRecordService.delete(ids);
        uploadRecordService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
