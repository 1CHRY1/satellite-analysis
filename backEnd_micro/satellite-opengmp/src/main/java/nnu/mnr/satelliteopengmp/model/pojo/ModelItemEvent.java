package nnu.mnr.satelliteopengmp.model.pojo;

import lombok.Data;

@Data
public class ModelItemEvent {
    String eventName;//事件名称
    String eventDescription;//事件描述
    Boolean optional;//事件是否可选
    String eventType;//事件类型 response为输入 noresponse为输出
    ModelItemEventData eventData;
}
