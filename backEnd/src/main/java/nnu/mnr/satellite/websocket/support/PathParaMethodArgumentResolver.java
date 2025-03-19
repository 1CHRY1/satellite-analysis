package nnu.mnr.satellite.websocket.support;

import io.netty.channel.Channel;
import nnu.mnr.satellite.annotations.websocket.PathParam;
import nnu.mnr.satellite.websocket.netty.AttributeKeyConstant;
import org.springframework.core.MethodParameter;
import org.springframework.util.StringUtils;

import java.util.Map;


public class PathParaMethodArgumentResolver implements MethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(PathParam.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, Channel channel) {
        Map<String, String> uriTemplateVariables = channel.attr(AttributeKeyConstant.uriTemplateVariables).get();
        String name = parameter.getParameterName();
        PathParam annotation = parameter.getParameterAnnotation(PathParam.class);
        if (StringUtils.hasLength(annotation.value())) {
            name = annotation.value();
        }
        return uriTemplateVariables.get(name);
    }
}
