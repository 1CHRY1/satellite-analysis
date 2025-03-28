package nnu.mnr.satellite.config.web;

import com.jcraft.jsch.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.model.pojo.common.SftpConn;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/17 13:43
 * @Description:
 */

@Component
@Slf4j
@Getter
public class JSchConnectionManager {

    private final SftpConn defaultSftpConn;

    private final Map<SftpConn, GenericObjectPool<Session>> sessionPools;

    private final int minSessions = 2;

    @PostConstruct
    public void init() {
        GenericObjectPool<Session> defaultPool = addSessionPool(defaultSftpConn);
        CompletableFuture.runAsync(() -> {
            try {
                defaultPool.preparePool();
                log.info("Preloaded sessions for default host: " + defaultSftpConn.getHost());
            } catch (Exception e) {
                log.error("Failed to preload sessions", e);
            }
        });
    }

    public JSchConnectionManager(
            @Value("${docker.defaultServer.host}") String defaultHost,
            @Value("${docker.defaultServer.username}") String defaultUsername,
            @Value("${docker.defaultServer.password}") String defaultPassword
    ) {
        defaultSftpConn = SftpConn.builder()
                .host(defaultHost)
                .username(defaultUsername)
                .password(defaultPassword)
                .BuildSftpConn();
        sessionPools = new HashMap<>();
    }

    private synchronized GenericObjectPool<Session> addSessionPool(SftpConn sftpConn) {
        return sessionPools.computeIfAbsent(sftpConn, conn -> {
            GenericObjectPoolConfig<Session> poolConfig = new GenericObjectPoolConfig<>();
            poolConfig.setMaxTotal(10);
            poolConfig.setMaxIdle(5);
            poolConfig.setMinIdle(minSessions);
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestWhileIdle(true);

            GenericObjectPool<Session> pool = new GenericObjectPool<>(new SessionFactory(conn), poolConfig);
            log.info("Created new Session pool for host: " + conn.getHost() + " of " + minSessions + " Sessions");
            return pool;
        });
    }

    public Session getSession(SftpConn sftpConn) {
        GenericObjectPool<Session> pool = addSessionPool(sftpConn);
        try {
            return pool.borrowObject();
        } catch (Exception e) {
            log.error("Failed to borrow Session from pool for host: " + sftpConn.getHost(), e);
            throw new RuntimeException("Unable to get SFTP session", e);
        }
    }

    public Session getSession() {
        return getSession(defaultSftpConn);
    }

    public void returnSession(SftpConn sftpConn, Session session) {
        GenericObjectPool<Session> pool = sessionPools.get(sftpConn);
        if (pool != null && session != null) {
            pool.returnObject(session);
        }
    }

    public void returnSession(Session session) {
        returnSession(defaultSftpConn, session);
    }

    private static class SessionFactory extends BasePooledObjectFactory<Session> {
        private final SftpConn sftpConn;

        public SessionFactory(SftpConn sftpConn) {
            this.sftpConn = sftpConn;
        }

        @Override
        public Session create() throws Exception {
            JSch jSch = new JSch();
            Session session = jSch.getSession(sftpConn.getUsername(), sftpConn.getHost(), 22);
            session.setPassword(sftpConn.getPassword());

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            config.put("PreferredAuthentications", "publickey,keyboard-interactive,password");
            session.setConfig(config);
            session.setTimeout(30000);
            session.connect();

            log.info("New JSch Session created for host: " + sftpConn.getHost());
            return session;
        }

        @Override
        public PooledObject<Session> wrap(Session session) {
            return new DefaultPooledObject<>(session);
        }

        @Override
        public void destroyObject(PooledObject<Session> p) throws Exception {
            Session session = p.getObject();
            if (session != null && session.isConnected()) {
                session.disconnect();
                log.info("JSch Session destroyed for host: " + session.getHost());
            }
        }

        @Override
        public boolean validateObject(PooledObject<Session> p) {
            Session session = p.getObject();
            try {
                return session.isConnected();
            } catch (Exception e) {
                log.warn("Session validation failed: " + e.getMessage());
                return false;
            }
        }
    }

    @PreDestroy
    public void cleanup() {
        for (GenericObjectPool<Session> pool : sessionPools.values()) {
            pool.close();
            log.info("JSch Session pool closed for host.");
        }
        sessionPools.clear();
    }

}
