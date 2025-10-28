package nnu.mnr.satellite.service.admin;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.mapper.resources.IVectorRepo;
import nnu.mnr.satellite.model.dto.admin.vector.VectorPageDTO;
import nnu.mnr.satellite.model.dto.admin.vector.VectorUpdateDTO;
import nnu.mnr.satellite.model.po.resources.Vector;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@DS("pg-satellite")
public class AdminVectorService {

    @Autowired
    private IVectorRepo vectorRepo;

    public CommonResultVO getVectorInfoPage(VectorPageDTO vectorPageDTO){
        // 构造分页对象
        Page<Vector> page = new Page<>(vectorPageDTO.getPage(), vectorPageDTO.getPageSize());

        String sortField = vectorPageDTO.getSortField();
        Boolean asc = vectorPageDTO.getAsc();
        LambdaQueryWrapper<Vector> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        // 筛选时间
        LocalDateTime startTime = vectorPageDTO.getStartTime();
        LocalDateTime endTime = vectorPageDTO.getEndTime();
        if (startTime != null) {
            lambdaQueryWrapper.ge(Vector::getTime, startTime);
        }
        if (endTime != null) {
            lambdaQueryWrapper.le(Vector::getTime, endTime);
        }
        String searchText = vectorPageDTO.getSearchText();
        if (searchText != null && !searchText.trim().isEmpty()) {
            String trimmedSearchText = searchText.trim();
            lambdaQueryWrapper.and(wrapper ->
                    wrapper.like(Vector::getVectorName, trimmedSearchText)
            );
        }

        lambdaQueryWrapper.select(
                Vector::getId,
                Vector::getVectorName,
                Vector::getTableName,
                Vector::getSrid,
                Vector::getTime,
                Vector::getCount
        );
        // 排序
        if (sortField != null && !sortField.isEmpty()) {
            // 使用 sortField 对应的数据库字段进行排序
            switch (sortField) {
                case "time":
                    lambdaQueryWrapper.orderBy(true, asc, Vector::getTime);
                    break;
                case "vectorName":
                    lambdaQueryWrapper.orderBy(true, asc, Vector::getVectorName);
                    break;
                case "id":
                    lambdaQueryWrapper.orderBy(true, asc, Vector::getId);
                    break;
                case "count":
                    lambdaQueryWrapper.orderBy(true, asc, Vector::getCount);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported sort field: " + sortField);
            }
        }
        // 查询，lambdaQueryWrapper没有显式指定selecet，默认select *
        IPage<Vector> vectorPage = vectorRepo.selectPage(page, lambdaQueryWrapper);

        return CommonResultVO.builder()
                .status(1)
                .message("矢量信息获取成功")
                .data(vectorPage)
                .build();
    }

    public CommonResultVO updateVector(VectorUpdateDTO vectorUpdateDTO){
        if (vectorUpdateDTO.getId() == null){
            return CommonResultVO.builder()
                    .status(-1)
                    .message("更新矢量信息失败，id不能为空")
                    .build();
        }
        Vector vector = new Vector();
        BeanUtils.copyProperties(vectorUpdateDTO, vector);
        System.out.println("Vector object: " + vector);
        vectorRepo.updateById(vector);
        return CommonResultVO.builder()
                .status(1)
                .message("更新矢量信息成功")
                .build();
    }
}
