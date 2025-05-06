package nnu.mnr.satelliteopengmp.service;

import nnu.mnr.satelliteopengmp.model.dto.ResourcePageDTO;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/31 14:55
 * @Description:
 */

@Service
public class CommonService {

    public Pageable getResourcePageable(ResourcePageDTO pageDTO){
        return PageRequest.of(pageDTO.getPage()-1, pageDTO.getPageSize(), Sort.by(pageDTO.getAsc()? Sort.Direction.ASC: Sort.Direction.DESC,pageDTO.getSortField()));
    }

}
