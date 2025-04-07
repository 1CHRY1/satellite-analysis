package nnu.mnr.satellite.opengmp.model.pojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ModelItemState {
    String stateName;//state名称
    String stateDescription;//state描述
    List<ModelItemEvent> events = new ArrayList<>();//事件列表
}
