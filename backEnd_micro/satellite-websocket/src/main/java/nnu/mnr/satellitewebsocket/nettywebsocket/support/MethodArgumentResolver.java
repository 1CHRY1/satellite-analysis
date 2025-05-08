package nnu.mnr.satellitewebsocket.nettywebsocket.support;

import io.netty.channel.Channel;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;


public interface MethodArgumentResolver {


    boolean supportsParameter(MethodParameter parameter);

    @Nullable
    Object resolveArgument(MethodParameter parameter, Channel channel);
}
