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

            WsEndPoint wsServiceAnnotation = targetClass.getAnnotation(WsEndPoint.class);
            WsServerEndPoint wsServerEndpoint = new WsServerEndPoint(targetClass
                    ,beanFactory.getBean(targetClass),wsServiceAnnotation.value());
            actionDispatch.addWsServerEndpoint(wsServerEndpoint);
        }
        NettyWebsocketServer websocketServer = new NettyWebsocketServer(actionDispatch, wsProperties);
        // 启动websocket
        websocketServer.start();
    }


    // Scanning @WsServerEndPoint
    private void scanWebsocketServiceBeans(String packagesToScan, BeanDefinitionRegistry registry) {

        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry, false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(WsEndPoint.class));

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
