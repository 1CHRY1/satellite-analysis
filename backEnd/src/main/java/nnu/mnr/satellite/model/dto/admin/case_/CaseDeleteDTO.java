package nnu.mnr.satellite.model.dto.admin.case_;

import lombok.Data;

import java.util.List;

@Data
public class CaseDeleteDTO {
    private List<String> caseIds;
}
