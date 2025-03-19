package nnu.mnr.satellite.config;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.model.po.common.SftpConn;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/17 13:43
 * @Description:
 */

@Component
@Slf4j
public class JSchConnectionManager {

    private final SftpConn defaultSftpConn;

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
    }

    private Session session;

    @PostConstruct
    public void init() {
        getSession(defaultSftpConn);
    }

    public Session getSession(SftpConn sftpConn) {
        String host = sftpConn.getHost();
        String username = sftpConn.getUsername();
        String password = sftpConn.getPassword();
        if ( session == null || !Objects.equals(session.getHost(), host)) {
            try {
                JSch jSch = new JSch();
                session = jSch.getSession(username, host, 22);
                session.setPassword(password);

                Properties config = new Properties();
                config.put("StrictHostKeyChecking", "no");
                config.put("PreferredAuthentications", "publickey,keyboard-interactive,password");
                session.setConfig(config);

                session.connect();
                log.info("JSch " + host + " Session connected.");

            } catch (Exception e) {
                log.error("JSch " + host + " Session Error " + e);
            }
        }
        return session;
    }

    @PreDestroy
    public void cleanup() {
        if (session != null && session.isConnected()) {
            session.disconnect();
            System.out.println("JSch " + session.getHost() + " Session disconnected.");
        }
    }

}
