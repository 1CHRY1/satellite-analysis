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
        WsServerEndPoint websocketServerEndpoint = matchServerEndpoint(uri);
        if (Objects.nonNull(websocketServerEndpoint)) {
            Method method = null;
            Object obj = websocketServerEndpoint.getObject();
            switch (action) {
                case HAND_SHAKE:
                    method = websocketServerEndpoint.getOnHandShake();
                    break;
                case OPEN:
                    method = websocketServerEndpoint.getOnOpen();
                    break;
                case CLOSE:
                    method = websocketServerEndpoint.getOnClose();
                    break;
                case MESSAGE:
                    method = websocketServerEndpoint.getOnMessage();
                    break;
                case EVENT:
                    method = websocketServerEndpoint.getOnEvent();
                    break;
                case ERROR:
                    method = websocketServerEndpoint.getOnError();
                    break;
                default:
                    break;
            }
            if (Objects.nonNull(method)) {
                Object[] args = new MethodParamsBuild().getMethodArgumentValues(method,channel);
                ReflectionUtils.invokeMethod(method,obj,args);
            }
        }
    }

    public Map<String,String> getUriTemplateVariables(String lookupPath) {
        WsServerEndPoint websocketServerEndpoint = matchServerEndpoint(lookupPath);
        return antPathMatcher.extractUriTemplateVariables(websocketServerEndpoint.getPath(), lookupPath);
    }

}
