package nnu.mnr.satelliteopengmp.model.pojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ModelItemData {
    List<ModelItemState> input = new ArrayList<>();//输入数据
    List<ModelItemState> output = new ArrayList<>();//输出数据
}
