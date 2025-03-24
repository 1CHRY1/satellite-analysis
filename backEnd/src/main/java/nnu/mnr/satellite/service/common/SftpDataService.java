package nnu.mnr.satellite.service.common;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import nnu.mnr.satellite.model.pojo.common.SftpConn;
import nnu.mnr.satellite.config.JSchConnectionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/17 13:40
 * @Description:
 */

@Service
public class SftpDataService {

    @Autowired
    JSchConnectionManager jschConnectionManager;

    public void uploadFile(String localFilePath, String remoteFilePath) throws Exception {
        Session session = jschConnectionManager.getSession();
        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();
        Path filePath = Paths.get(localFilePath);
        FileInputStream fis = new FileInputStream(filePath.toFile());
        channel.put(fis, remoteFilePath);
        channel.disconnect();
    }

    private void uploadDirectory(ChannelSftp channelSftp, String resourcePath, String remotePath) throws SftpException, IOException {
        File localPackage = new File(resourcePath);
        try {
            channelSftp.cd(remotePath);
        } catch (SftpException e) {
            channelSftp.mkdir(remotePath);
            channelSftp.cd(remotePath);
        }
        File[] files = localPackage.listFiles();
        if (files != null) {
            for (File file : files) {
                String remoteFilePath = remotePath + "/" + file.getName();
                if (file.isDirectory()) {
                    uploadDirectory(channelSftp, file.getAbsolutePath(), remoteFilePath);
                } else {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        channelSftp.put(fis, remoteFilePath);
                    }
                }
            }
        }
    }

    public void createRemoteDirAndFile(SftpConn sftpConn, String localProjectPath, String volumePath) throws JSchException, SftpException, IOException {
        Session session = jschConnectionManager.putSession(sftpConn);
        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();

        // Creating Working Dir in Container
        channel.mkdir(volumePath);
        channel.chmod(0777, volumePath);

        // Creating Data Dir in Container
        String dataPath = volumePath + "/data";
        channel.mkdir(dataPath);
        channel.chmod(0777, dataPath);

        // Creating Output Dir in Container
        String outputPath = volumePath + "/output";
        channel.mkdir(outputPath);
        channel.chmod(0777, outputPath);

        // Copying Package in Container
        String packagePath = volumePath + "package/";
        int lastSlashIndex = localProjectPath.lastIndexOf('/');
        uploadDirectory(channel, localProjectPath.substring(0, lastSlashIndex) + "/devCli/package", packagePath);
        channel.chmod(0777, packagePath);

        // Copying Local File
        Path mainPath = Paths.get(localProjectPath + "/main.py");
        FileInputStream fis = new FileInputStream(mainPath.toFile());
        channel.put(fis, volumePath + "/main.py");

        channel.disconnect();

    }

}
