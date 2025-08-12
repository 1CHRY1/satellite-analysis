package nnu.mnr.satellite.model.dto.user;

import lombok.Data;

@Data
public class RecordPageFetchDTO {
    private String userId;
    private int page;
    private int pageSize;
    private Boolean asc = false;
    private String sortField = "actionTime";
}
