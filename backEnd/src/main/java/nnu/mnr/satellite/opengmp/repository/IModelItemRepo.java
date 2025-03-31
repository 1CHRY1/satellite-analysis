package nnu.mnr.satellite.opengmp.repository;

import nnu.mnr.satellite.opengmp.model.po.ModelItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/31 10:15
 * @Description:
 */

@Repository
public interface IModelItemRepo extends MongoRepository<ModelItem, String> {

    Page<ModelItem> findAllByModelTypeAndNameLikeIgnoreCaseAndNormalTagsLikeIgnoreCase(String modelType, String searchText, String tagName, Pageable pageable);

    Page<ModelItem> findAllByModelTypeAndNameLikeIgnoreCaseAndNormalTagsContainingIgnoreCase(String modelType, String searchText, List<String> tagName, Pageable pageable);

    Page<ModelItem> findAllByModelTypeAndNameLikeIgnoreCaseAndProblemTagsLikeIgnoreCase(String modelType, String searchText, String tagName, Pageable pageable);

    Page<ModelItem> findAllByNameLikeIgnoreCase(String name, Pageable pageable);

}
