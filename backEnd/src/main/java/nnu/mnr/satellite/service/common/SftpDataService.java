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
import java.util.Vector;

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

    public String readRemoteFile(String remoteFilePath) throws JSchException, SftpException, IOException {
        Session session = null;
        ChannelSftp channel = null;
        InputStream inputStream = null;
        BufferedReader reader = null;

        try {
            // 获取 SFTP 会话和通道
            session = jschConnectionManager.getSession();
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();

            // 获取远程文件的输入流
            inputStream = channel.get(remoteFilePath);

            // 使用 BufferedReader 读取文本内容
            reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }

            // 返回文件内容（去掉末尾多余的换行符）
            return content.toString().trim();

        } finally {
            // 关闭资源
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // 日志记录或忽略
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // 日志记录或忽略
                }
            }
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            // 注意：这里不关闭 session，因为它由 jschConnectionManager 管理
            jschConnectionManager.returnSession(session);
        }
    }

    public void writeRemoteFile(String remoteFilePath, String content) throws JSchException, SftpException, IOException {
        Session session = null;
        ChannelSftp channel = null;
        OutputStream outputStream = null;

        try {
            // 获取 SFTP 会话和通道
            session = jschConnectionManager.getSession();
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();

            // 将字符串内容写入远程文件
            outputStream = channel.put(remoteFilePath);
            outputStream.write(content.getBytes());
            outputStream.flush();

        } finally {
            // 关闭资源
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    // 可以添加日志
                }
            }
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            // 不关闭 session，交给 jschConnectionManager 管理
            jschConnectionManager.returnSession(session);
        }
    }

    public void uploadFile(String localFilePath, String remoteFilePath) throws Exception {
        Session session = jschConnectionManager.getSession();
        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();
        Path filePath = Paths.get(localFilePath);
        FileInputStream fis = new FileInputStream(filePath.toFile());
        channel.put(fis, remoteFilePath);
        channel.disconnect();
        jschConnectionManager.returnSession(session);
    }

    public void deleteFolder(String folderPath) {
        Session session = jschConnectionManager.getSession();
        try {
            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            try {
                try {
                    channelSftp.cd(folderPath);
                } catch (SftpException e) {
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

            } finally {
                if (channelSftp != null && channelSftp.isConnected()) {
                    channelSftp.disconnect();
                }
            }
        } catch (JSchException e) {

        } finally {
            jschConnectionManager.returnSession(session);
        }
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

    public void createRemoteDirAndFile(SftpConn sftpConn, String localProjectPath, String volumePath) throws Exception {
        Session session = jschConnectionManager.getSession(sftpConn);
        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();

        // Creating Working Dir in Container
        channel.mkdir(volumePath);
        channel.chmod(0777, volumePath);

        // Creating Data Dir in Container
        String dataPath = volumePath + "data/";
        channel.mkdir(dataPath);
        channel.chmod(0777, dataPath);

        // Creating Output Dir in Container
        String outputPath = volumePath + "output/";
        channel.mkdir(outputPath);
        channel.chmod(0777, outputPath);

        int lastSlashIndex = localProjectPath.lastIndexOf('/');
        // Copying Package in Container
        String packagePath = volumePath + "TransferEngine/";
        uploadDirectory(channel, localProjectPath.substring(0, lastSlashIndex) + "/devCli/TransferEngine", packagePath);
        channel.chmod(0777, packagePath);

        // Copying Local File
        String mainPath = localProjectPath.substring(0, lastSlashIndex) + "/devCli/main.py";
        uploadFile(mainPath, volumePath + "/main.py");

        channel.disconnect();
        jschConnectionManager.returnSession(session);

    }

}
