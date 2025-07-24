package nnu.mnr.satellite.model.pojo.modeling;
import lombok.Data;
import java.util.Map;

@Data
public abstract class BaseModelServerProperties {

    private String address;
    private Map<String, String> apis;
    private Map<String, Integer> interval;

}