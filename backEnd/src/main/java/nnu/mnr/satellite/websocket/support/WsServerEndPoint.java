package nnu.mnr.satellite.websocket.support;

import lombok.Getter;
import nnu.mnr.satellite.annotations.websocket.*;
import nnu.mnr.satellite.exception.WsDeploymentException;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/19 9:51
 * @Description:
 */

@Getter
public class WsServerEndPoint {

    private String path;
    private Method onHandShake;
    private Method onClose;
    private Method onEvent;
    private Method onOpen;
    private Method onMessage;
    private Method onError;
    private Object object;

    public WsServerEndPoint(Class<?> pojoClazz,Object obj,String path) {
        this.object = obj;
        this.path = path;
        AtomicReference<Method> handShake = new AtomicReference<>();
        AtomicReference<Method> close = new AtomicReference<>();
        AtomicReference<Method> event = new AtomicReference<>();
        AtomicReference<Method> open = new AtomicReference<>();
        AtomicReference<Method> message = new AtomicReference<>();
        AtomicReference<Method> error = new AtomicReference<>();

        Method[] pojoClazzMethods = null;
        Class<?> currentClazz = pojoClazz;
        while (!currentClazz.equals(Object.class)) {
            Method[] currentClazzMethods = currentClazz.getDeclaredMethods();
            if (currentClazz == pojoClazz) {
                pojoClazzMethods = currentClazzMethods;
            }
            for (Method method : currentClazzMethods) {
                if (Objects.nonNull(method.getAnnotation(HandShakeBefore.class))) {
                    methodFill(currentClazz,method,pojoClazz,handShake, HandShakeBefore.class);
                } else if (Objects.nonNull(method.getAnnotation(OnClose.class))) {
                    methodFill(currentClazz,method,pojoClazz,close,OnClose.class);
                } else if (Objects.nonNull(method.getAnnotation(OnEvent.class))) {
                    methodFill(currentClazz,method,pojoClazz,event,OnEvent.class);
                } else if (Objects.nonNull(method.getAnnotation(OnOpen.class))) {
                    methodFill(currentClazz,method,pojoClazz,open,OnOpen.class);
                } else if (Objects.nonNull(method.getAnnotation(OnMessage.class))) {
                    methodFill(currentClazz,method,pojoClazz,message,OnMessage.class);
                } else if (Objects.nonNull(method.getAnnotation(OnError.class))) {
                    methodFill(currentClazz,method,pojoClazz,error,OnError.class);
                }
            }
            currentClazz = currentClazz.getSuperclass();
            this.onHandShake = handShake.get();
            this.onClose = close.get();
            this.onEvent = event.get();
            this.onOpen = open.get();
            this.onMessage = message.get();
            this.onError = error.get();
        }
    }

    private void methodFill(Class<?> currentClazz, Method method, Class<?> pojoClazz, AtomicReference<Method> point, Class annotation) {
        checkPublic(method);
        if (Objects.isNull(point.get())) {
            point.set(method);
        } else {
            if (currentClazz == pojoClazz ||
                    !isMethodOverride(point.get(), method)) {
                throw new WsDeploymentException(
                        "wsServerEndpoint.duplicateAnnotation " + annotation.getSimpleName());
            }
        }
    }

    private void checkPublic(Method m) throws WsDeploymentException {
        if (!Modifier.isPublic(m.getModifiers())) {
            throw new WsDeploymentException(
                    "pojoMethodMapping.methodNotPublic " + m.getName());
        }
    }

    private boolean isMethodOverride(Method method1, Method method2) {
        return (method1.getName().equals(method2.getName())
                && method1.getReturnType().equals(method2.getReturnType())
                && Arrays.equals(method1.getParameterTypes(), method2.getParameterTypes()));
    }

}
