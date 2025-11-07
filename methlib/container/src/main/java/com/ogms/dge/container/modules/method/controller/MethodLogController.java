package com.ogms.dge.container.modules.method.controller;

import com.ogms.dge.container.common.utils.Constant;
import com.ogms.dge.container.common.utils.PageUtils;
import com.ogms.dge.container.common.utils.R;
import com.ogms.dge.container.modules.method.converter.MethodLogConverter;
import com.ogms.dge.container.modules.method.entity.MethodLogEntity;
import com.ogms.dge.container.modules.method.service.MethodLogService;
import com.ogms.dge.container.modules.method.vo.MethodLogVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;


/**
 * 方法调用记录表
 *
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2024-08-23 20:06:48
 */
@RestController
@RequestMapping("method/log")
public class MethodLogController extends AbstractController {
    @Autowired
    private MethodLogService methodLogService;

    @Autowired
    private MethodLogConverter methodLogConverter;

    /**
     * 列表
     */
    @RequestMapping("/list")
//    @RequiresPermissions("method:methodlog:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = methodLogService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
//    @RequiresPermissions("method:methodlog:info")
    public R info(@PathVariable("id") Long id){
		MethodLogVo methodLogVo = methodLogConverter.po2Vo(methodLogService.getById(id));

        return R.ok().put("methodLog", methodLogVo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
//    @RequiresPermissions("method:methodlog:save")
    public R save(@RequestBody MethodLogEntity methodLog){
		methodLogService.save(methodLog);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
//    @RequiresPermissions("method:methodlog:update")
    public R update(@RequestBody MethodLogEntity methodLog){
		methodLogService.updateById(methodLog);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
//    @RequiresPermissions("method:methodlog:delete")
    public R delete(@RequestBody Long[] ids){
		methodLogService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
