package nnu.mnr.satellite.websocket.support;

import lombok.SneakyThrows;
import nnu.mnr.satellite.BackEndApplication;
import nnu.mnr.satellite.annotations.websocket.WsEndPoint;
import nnu.mnr.satellite.model.pojo.websocket.WsProperties;
import nnu.mnr.satellite.websocket.netty.NettyWebsocketServer;
import nnu.mnr.satellite.websocket.netty.WsActionDispatch;
import nnu.mnr.satellite.websocket.support.WsServerEndPoint;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/19 9:34
 * @Description:
 */

public class WsAnnotationPostProcesser implements SmartInitializingSingleton {

    @Autowired
    private DefaultListableBeanFactory beanFactory;

    @Autowired
    private WsProperties wsProperties;

    @Override
    public void afterSingletonsInstantiated() {
        String[] beanNamesForAnnotation = beanFactory.getBeanNamesForAnnotation(SpringBootApplication.class);
        String applicationStartBean = beanNamesForAnnotation[0];
        Object bean = beanFactory.getBean(applicationStartBean);
        String basePackage = ClassUtils.getPackageName(bean.getClass());
        scanWebsocketServiceBeans(basePackage,beanFactory);
        registerServerEndpoints();
    }

    @SneakyThrows
    private void registerServerEndpoints() {
        String[] beanNamesForAnnotation = beanFactory.getBeanNamesForAnnotation(WsEndPoint.class);
        WsActionDispatch actionDispatch = new WsActionDispatch();
        for (String beanName : beanNamesForAnnotation) {
            Class<?> beanType = beanFactory.getType(beanName);
            Class<?> targetClass = getTargetClass(beanType);

            WsEndPoint wsServerEndpoint = targetClass.getAnnotation(WsEndPoint.class);
            WsServerEndPoint websocketServerEndpoint = new WsServerEndPoint(targetClass
                    ,beanFactory.getBean(targetClass),wsServerEndpoint.value());
            actionDispatch.addWsServerEndpoint(websocketServerEndpoint);
        }
        NettyWebsocketServer websocketServer = new NettyWebsocketServer(actionDispatch, wsProperties);
        // 启动websocket
        websocketServer.start();
    }


    // Scanning @WsServerEndPoint
    private void scanWebsocketServiceBeans(String packagesToScan, BeanDefinitionRegistry registry) {

        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry);
        scanner.addIncludeFilter(new AnnotationTypeFilter(WsEndPoint.class));

        //TODO: 后面修改这里，加入websocket的时候不知道为何不排除主类就报错
        scanner.addExcludeFilter((metadata, factory) ->
                metadata.getClassMetadata().getClassName().equals(BackEndApplication.class.getName()));

        scanner.scan(packagesToScan);
    }

    // Getting Target Class
    public Class<?> getTargetClass(Class<?> clazz) {
        if (AopUtils.isCglibProxy(clazz)) {
            return clazz.getSuperclass();
        }
        return clazz;
    }

}
