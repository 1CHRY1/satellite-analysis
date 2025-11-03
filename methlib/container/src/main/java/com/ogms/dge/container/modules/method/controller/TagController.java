package com.ogms.dge.container.modules.method.controller;

import com.ogms.dge.container.common.utils.PageUtils;
import com.ogms.dge.container.common.utils.R;
import com.ogms.dge.container.common.validator.ValidatorUtils;
import com.ogms.dge.container.modules.method.entity.TagEntity;
import com.ogms.dge.container.modules.method.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


/**
 * 标签
 */
@RestController
@RequestMapping("tag")
public class TagController extends AbstractController {
    @Autowired
    private TagService tagService;

    /**
     * 列表
     */
    @RequestMapping("/list")
//    @RequiresPermissions("container:tag:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = tagService.queryPage(params);

        return R.ok().put("page", page);
    }

    /**
     * 不分页的列表, 用于前端tag select列表
     */
    @RequestMapping("/listNoPage")
//    @RequiresPermissions("container:tag:listNoPage")
    public R listNoPage() {
        return R.ok().put("tagList", tagService.queryAll(getUserId()));
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
//    @RequiresPermissions("container:tag:info")
    public R info(@PathVariable("id") Long id) {
        TagEntity tag = tagService.getById(id);

        return R.ok().put("tag", tag);
    }

    /**
     * 根据 idList 获取多个 TagEntity 信息
     */
    @RequestMapping("/infoByIds")
    public R infoList(@RequestBody List<Long> idList) {
        List<TagEntity> tags = tagService.listByIds(idList);

        return R.ok().put("tags", tags);
    }


    /**
     * 通过方法id得到tag信息
     */
    @RequestMapping("/infoByMethodId/{id}")
//    @RequiresPermissions("container:tag:infoByMethodId")
    public R infoByMethodId(@PathVariable("id") Long id) {
        return R.ok().put("tagList", tagService.getTagListByMethodId(id));
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
//    @RequiresPermissions("container:tag:save")
    public R save(@RequestBody TagEntity tag) {
        ValidatorUtils.validateEntity(tag);
        tag.setCreateUserId(getUserId());
        tagService.saveTag(tag);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
//    @RequiresPermissions("container:tag:update")
    public R update(@RequestBody TagEntity tag) {
        tagService.updateById(tag);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
//    @RequiresPermissions("container:tag:delete")
    public R delete(@RequestBody Long[] ids) {
        tagService.deleteBatch(ids);

        return R.ok();
    }

}
