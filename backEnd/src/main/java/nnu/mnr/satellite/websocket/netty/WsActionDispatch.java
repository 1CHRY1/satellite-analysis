package nnu.mnr.satellite.websocket.netty;

import io.netty.channel.Channel;
import nnu.mnr.satellite.enums.websocket.WsAction;
import nnu.mnr.satellite.websocket.support.MethodParamsBuild;
import nnu.mnr.satellite.websocket.support.WsServerEndPoint;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/19 9:40
 * @Description:
 */
public class WsActionDispatch {

    private AntPathMatcher antPathMatcher = new AntPathMatcher();

    private final static Map<String, WsServerEndPoint> endpointMap = new ConcurrentHashMap<>(16);

    // Verify if Url is Legal
    protected boolean verifyUri(String uri) {
        return endpointMap.keySet().stream().anyMatch(e -> antPathMatcher.match(e, uri));
    }

    // Add Websocket Service
    public void addWsServerEndpoint(WsServerEndPoint endpoint) {
        endpointMap.putIfAbsent(endpoint.getPath(),endpoint);
    }

    // Match Websocket Service with Url
    protected WsServerEndPoint matchServerEndpoint(String uri) {
        for (Map.Entry<String, WsServerEndPoint> entry : endpointMap.entrySet()) {
            if (antPathMatcher.match(entry.getKey(),uri)) {
                return entry.getValue();
            }
        }
        return null;
    }

    // Dispatch Action to Method
    protected void dispatch(String uri, WsAction action, Channel channel) {
        WsServerEndPoint wsServerEndpoint = matchServerEndpoint(uri);
        if (Objects.nonNull(wsServerEndpoint)) {
            Method method = null;
            Object obj = wsServerEndpoint.getObject();
            switch (action) {
                case HAND_SHAKE:
                    method = wsServerEndpoint.getOnHandShake();
                    break;
                case OPEN:
                    method = wsServerEndpoint.getOnOpen();
                    break;
                case CLOSE:
                    method = wsServerEndpoint.getOnClose();
                    break;
                case MESSAGE:
                    method = wsServerEndpoint.getOnMessage();
                    break;
                case EVENT:
                    method = wsServerEndpoint.getOnEvent();
                    break;
                case ERROR:
                    method = wsServerEndpoint.getOnError();
                    break;
                default:
                    break;
            }
            if (Objects.nonNull(method)) {
                Object[] args = new MethodParamsBuild().getMethodArgumentValues(method, channel);
                ReflectionUtils.invokeMethod(method,obj,args);
            }
        }
    }

    public Map<String,String> getUriTemplateVariables(String lookupPath) {
        WsServerEndPoint wsServerEndpoint = matchServerEndpoint(lookupPath);
        return antPathMatcher.extractUriTemplateVariables(wsServerEndpoint.getPath(), lookupPath);
    }

}
