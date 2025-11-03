package com.ogms.dge.container.modules.common.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ogms.dge.container.common.utils.FileUtils;
import com.ogms.dge.container.common.utils.PageUtils;
import com.ogms.dge.container.common.utils.Query;
import com.ogms.dge.container.modules.common.converter.UploadRecordConverter;
import com.ogms.dge.container.modules.common.dao.UploadRecordDao;
import com.ogms.dge.container.modules.common.entity.UploadRecordEntity;
import com.ogms.dge.container.modules.common.enums.UploadRecordStatusEnum;
import com.ogms.dge.container.modules.common.enums.UploadRecordTypeEnum;
import com.ogms.dge.container.modules.common.service.UploadRecordService;
import com.ogms.dge.container.modules.sys.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;


@Service("uploadRecordService")
public class UploadRecordServiceImpl extends ServiceImpl<UploadRecordDao, UploadRecordEntity> implements UploadRecordService {

    @Autowired
    private SysUserService sysUserService;

    @Resource
    private UploadRecordConverter uploadRecordConverter;

    @Value("${container.data.dsd}")
    private String data_dsd;

    @Value("${container.method.pd}")
    private String method_pd;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        Long userId = (Long) params.get("userId");
        IPage<UploadRecordEntity> page = this.page(
                new Query<UploadRecordEntity>().getPage(params),
                new QueryWrapper<UploadRecordEntity>()
                        .orderByDesc("upload_time")
                        .eq(userId != null, "user_id", userId)
        );
        PageUtils pageUtils = new PageUtils(page);
        pageUtils.setList(uploadRecordConverter.poList2VoList(page.getRecords(), sysUserService));

        return pageUtils;
    }

    @Override
    public boolean save(UploadRecordEntity entity) {
        entity.setUploadTime(new Date());
        return super.save(entity);
    }

    @Override
    public UploadRecordEntity saveOrUpdate(Long id, Long userId, String packageName, UploadRecordTypeEnum type, UploadRecordStatusEnum status, String uuid, String description) {
        UploadRecordEntity entity = new UploadRecordEntity();
        entity.setId(id);
        entity.setPackageName(packageName);
        entity.setUserId(userId);
        entity.setType(type.getCode());
        entity.setStatus(status.getCode());
        entity.setPackageUuid(uuid);
        entity.setDescription(description);
        entity.setUploadTime(new Date());
        super.saveOrUpdate(entity);
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long[] ids) {
        for (Long id : ids) {
            UploadRecordEntity record = getById(id);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    if (record.getType() == UploadRecordTypeEnum.DATA.getCode())
                        FileUtils.deleteDirectory(new File(data_dsd + record.getPackageUuid()).getAbsolutePath());
                    else
                        FileUtils.deleteDirectory(new File(method_pd + record.getPackageUuid()).getAbsolutePath());
                }
            });
        }
        removeByIds(Arrays.asList(ids));
    }
}