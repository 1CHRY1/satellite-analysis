package nnu.mnr.satellite.websocket.support;

import io.netty.channel.Channel;
import nnu.mnr.satellite.annotations.websocket.OnEvent;
import nnu.mnr.satellite.websocket.netty.AttributeKeyConstant;
import org.springframework.core.MethodParameter;

import java.util.Objects;


public class IdleEventMethodArgumentResolver implements MethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getMethod().isAnnotationPresent(OnEvent.class) && Objects.equals(parameter.getParameterType(),Object.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, Channel channel) {
        return channel.attr(AttributeKeyConstant.idleStateEvent).get();
    }
}
