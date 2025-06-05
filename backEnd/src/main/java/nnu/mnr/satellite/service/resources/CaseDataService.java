package nnu.mnr.satellite.service.resources;

import nnu.mnr.satellite.mapper.resources.ICaseRepo;
import org.springframework.stereotype.Service;

/**
 * @name: CaseDataService
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 6/4/2025 3:34 PM
 * @version: 1.0
 */
@Service("CaseDataService")
public class CaseDataService {

    private final ICaseRepo caseRepo;

    public CaseDataService(ICaseRepo caseRepo) {
        this.caseRepo = caseRepo;
    }


}
