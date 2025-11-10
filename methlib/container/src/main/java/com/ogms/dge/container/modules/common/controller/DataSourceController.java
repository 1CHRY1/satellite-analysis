package com.ogms.dge.container.modules.common.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.ValidationMessage;
import com.ogms.dge.container.common.utils.JsonValidator;
import com.ogms.dge.container.common.utils.PageUtils;
import com.ogms.dge.container.common.utils.R;
import com.ogms.dge.container.modules.common.converter.DataSourceConverter;
import com.ogms.dge.container.modules.common.entity.DataSourceEntity;
import com.ogms.dge.container.modules.common.service.DataSourceService;
import com.ogms.dge.container.modules.method.controller.AbstractController;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * 数据源类型
 *
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2024-09-23 15:23:47
 */
@RestController
@RequestMapping("common/datasource")
public class DataSourceController extends AbstractController {
    @Autowired
    private DataSourceService dataSourceService;

    @Resource
    private DataSourceConverter dataSourceConverter;

    /**
     * 列表
     */
    @RequestMapping("/list")
//    @RequiresPermissions("common:datasource:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = dataSourceService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
//    @RequiresPermissions("common:datasource:info")
    public R info(@PathVariable("id") Long id) throws JsonProcessingException {
        DataSourceEntity dataSource = dataSourceService.getById(id);

        return R.ok().put("dataSource", dataSourceConverter.po2Vo(dataSource));
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
//    @RequiresPermissions("common:datasource:save")
    public R save(@RequestBody DataSourceEntity dataSource) {
        dataSourceService.save(dataSource);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
//    @RequiresPermissions("common:datasource:update")
    public R update(@RequestBody DataSourceEntity dataSource) {
        dataSourceService.updateById(dataSource);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
//    @RequiresPermissions("common:datasource:delete")
    public R delete(@RequestBody Long[] ids) {
        dataSourceService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

    @RequestMapping("/validateJson")
    public R validateJson(@RequestBody Map<String, Object> json) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        List<ValidationMessage> list = JsonValidator.validateJson(objectMapper.writeValueAsString(json), "");
        if (list.isEmpty()) {
            return R.ok();
        } else {
            return R.error(list.toString());
        }
    }

}
