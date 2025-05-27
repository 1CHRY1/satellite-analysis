package nnu.mnr.satellite.repository.geo;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import nnu.mnr.satellite.model.po.geo.GeoLocation;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/27 9:25
 * @Description:
 */

@Repository
public class LocationRepoImpl implements LocationRepo{

    private final ElasticsearchOperations elasticsearchOperations;

    public LocationRepoImpl(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @Override
    public List<GeoLocation> searchByName(String keyword) {
        Query query = Query.of(q -> q
                .match(m -> m
                        .field("name")
                        .query(keyword)
                )
        );
        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(query)
                .build();
        SearchHits<GeoLocation> hits = elasticsearchOperations.search(nativeQuery, GeoLocation.class);
        return hits.get().map(SearchHit::getContent).toList();
    }

    @Override
    public GeoLocation searchById(String id) {
        return elasticsearchOperations.get(id, GeoLocation.class);
    }

}
