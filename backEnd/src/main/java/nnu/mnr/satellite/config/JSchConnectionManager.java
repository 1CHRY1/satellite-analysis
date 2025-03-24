package nnu.mnr.satellite.config;

import com.jcraft.jsch.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.model.pojo.common.SftpConn;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Properties;
import java.util.Vector;

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

    private Session session;

    @PostConstruct
    public void init() {
        putSession(defaultSftpConn);
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
    }

    public Session putSession(SftpConn sftpConn) {
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

    public void deleteFolder(String folderPath) {
        try {
            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            try {
                try {
                    channelSftp.cd(folderPath);
                } catch (SftpException e) {
                    log.info("Folder does not exist: " + folderPath);
                    return;
                }

                Vector<ChannelSftp.LsEntry> fileList = channelSftp.ls("*");
                for (ChannelSftp.LsEntry entry : fileList) {
                    String fileName = entry.getFilename();
                    if (".".equals(fileName) || "..".equals(fileName)) {
                        continue;
                    }

                    String fullPath = folderPath + "/" + fileName;
                    if (entry.getAttrs().isDir()) {
                        deleteFolder(fullPath);
                    } else {
                        channelSftp.rm(fullPath);
                    }
                }
                channelSftp.cd("..");
                channelSftp.rmdir(folderPath);

            } catch (SftpException e) {
                log.error("Error deleting folder: " + folderPath, e);
            } finally {
                if (channelSftp != null && channelSftp.isConnected()) {
                    channelSftp.disconnect();
                }
            }
        } catch (JSchException e) {
            log.error("Failed to open SFTP channel for folder deletion: " + folderPath, e);
        }
    }

    @PreDestroy
    public void cleanup() {
        if (session != null && session.isConnected()) {
            session.disconnect();
            System.out.println("JSch " + session.getHost() + " Session disconnected.");
        }
    }

}
