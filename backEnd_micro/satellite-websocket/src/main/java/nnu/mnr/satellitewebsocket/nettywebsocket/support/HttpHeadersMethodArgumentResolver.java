package nnu.mnr.satellitewebsocket.nettywebsocket.support;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;

import nnu.mnr.satellitewebsocket.nettywebsocket.netty.AttributeKeyConstant;
import org.springframework.core.MethodParameter;

import java.util.Objects;


public class HttpHeadersMethodArgumentResolver implements MethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return Objects.equals(parameter.getParameterType(), HttpHeaders.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, Channel channel) {
        FullHttpRequest fullHttpRequest = channel.attr(AttributeKeyConstant.fullHttpRequest).get();
        return Objects.nonNull(fullHttpRequest) ? fullHttpRequest.headers() : null;
    }
}
