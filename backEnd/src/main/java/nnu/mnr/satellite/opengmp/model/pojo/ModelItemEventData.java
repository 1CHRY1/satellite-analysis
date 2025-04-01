package nnu.mnr.satellite.opengmp.model.pojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ModelItemEventData {
    String eventDataType;//external表示外部文件输入 internal表示内部变量输入
    String eventDataText;
    String eventDataDesc;
    List<ModelItemEventDataNode> nodeList = new ArrayList<>();
}
