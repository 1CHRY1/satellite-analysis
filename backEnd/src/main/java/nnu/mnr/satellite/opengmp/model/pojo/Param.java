package nnu.mnr.satellite.opengmp.model.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Param {
        String name;
        List<String> flags = new ArrayList<>();
        Boolean optional;
        String description;
        String default_value;
        Object parameter_type;
}
