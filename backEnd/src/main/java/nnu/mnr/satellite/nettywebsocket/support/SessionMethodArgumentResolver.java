package nnu.mnr.satellite.nettywebsocket.support;

import io.netty.channel.Channel;
import nnu.mnr.satellite.nettywebsocket.socket.Session;
import org.springframework.core.MethodParameter;

import java.util.Objects;


public class SessionMethodArgumentResolver implements MethodArgumentResolver{

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return Objects.equals(Session.class,parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, Channel channel) {
        return new Session(channel);
    }
}
