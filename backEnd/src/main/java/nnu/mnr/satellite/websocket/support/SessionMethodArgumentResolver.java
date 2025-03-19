package nnu.mnr.satellite.websocket.support;

import io.netty.channel.Channel;
import nnu.mnr.satellite.model.pojo.websocket.WsSession;
import org.springframework.core.MethodParameter;

import java.util.Objects;


public class SessionMethodArgumentResolver implements MethodArgumentResolver{

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return Objects.equals(WsSession.class,parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, Channel channel) {
        return new WsSession(channel);
    }
}
