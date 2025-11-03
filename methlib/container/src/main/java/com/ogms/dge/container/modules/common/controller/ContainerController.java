package com.ogms.dge.container.modules.common.controller;

import com.ogms.dge.container.common.utils.PageUtils;
import com.ogms.dge.container.common.utils.R;
import com.ogms.dge.container.modules.app.entity.UserEntity;
import com.ogms.dge.container.modules.common.converter.ContainerConverter;
import com.ogms.dge.container.modules.common.entity.ContainerEntity;
import com.ogms.dge.container.modules.common.service.ContainerService;
import com.ogms.dge.container.modules.data.converter.ConfigConverter;
import com.ogms.dge.container.modules.method.controller.AbstractController;
import com.ogms.dge.container.modules.sys.entity.SysUserEntity;
import com.ogms.dge.container.modules.sys.service.SysInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Map;


/**
 *
 *
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2025-04-02 15:07:30
 */
@RestController
@RequestMapping("common/container")
public class ContainerController extends AbstractController {
    @Autowired
    private ContainerService containerService;

    @Autowired
    private SysInfoService sysInfoService;

    @Autowired
    private ContainerConverter containerConverter;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = containerService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Integer id){
		ContainerEntity container = containerService.getById(id);

        return R.ok().put("container", container);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody ContainerEntity container){
		containerService.save(container);

        return R.ok();
    }

    /**
     * 注册
     */
    @RequestMapping("/register")
    public R register() throws SocketException, UnknownHostException {
        Map<String, Object> hostInfo = sysInfoService.getHost();
        ContainerEntity container = containerConverter.map2Po(hostInfo);
        SysUserEntity user = getUser();
        container.setUsername(user.getUsername());
        containerService.save(container);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody ContainerEntity container){
		containerService.updateById(container);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Integer[] ids){
		containerService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
