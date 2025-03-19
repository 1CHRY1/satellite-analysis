package nnu.mnr.satellite.websocket.support;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import nnu.mnr.satellite.annotations.websocket.OnMessage;
import nnu.mnr.satellite.websocket.netty.AttributeKeyConstant;
import org.springframework.core.MethodParameter;

import java.util.Objects;


public class TextMethodArgumentResolver implements MethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getMethod().isAnnotationPresent(OnMessage.class)
                && Objects.equals(parameter.getParameterType(),String.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, Channel channel) {
        TextWebSocketFrame text = channel.attr(AttributeKeyConstant.textWebSocketFrame).get();
        return text.text();
    }
}
