package nnu.mnr.satellite.service.common;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.model.pojo.common.SftpConn;
import nnu.mnr.satellite.config.web.JSchConnectionManager;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Vector;
import java.util.function.Consumer;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/17 13:40
 * @Description:
 */

@Service
@Slf4j
public class SftpDataService {

    @Autowired
    JSchConnectionManager jschConnectionManager;

    private void executeWithChannel(Consumer<ChannelSftp> operation) {
        Session session = jschConnectionManager.getSession();
        try {
            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            operation.accept(channelSftp);
            channelSftp.disconnect();
        } catch (JSchException e) {
            log.error("Failed to execute SFTP operation", e);
            throw new RuntimeException("SFTP operation failed", e);
        } finally {
            jschConnectionManager.returnSession(session);
        }
    }

    public String readRemoteFile(String remoteFilePath) {
        StringBuilder content = new StringBuilder();
        executeWithChannel(channelSftp -> {
            try (InputStream inputStream = channelSftp.get(remoteFilePath);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            } catch (SftpException | IOException e) {
                log.error("Failed to read remote file: " + remoteFilePath, e);
                throw new RuntimeException("Read operation failed", e);
            }
        });
        return content.toString();
    }

    public void writeRemoteFile(String remoteFilePath, String content) {
        executeWithChannel(channelSftp -> {
            try (OutputStream outputStream = channelSftp.put(remoteFilePath)) {
                outputStream.write(content.getBytes());
                outputStream.flush();
            } catch (SftpException | IOException e) {
                log.error("Failed to write remote file: " + remoteFilePath, e);
                throw new RuntimeException("Write operation failed", e);
            }
        });
    }

    public void createRemoteDir(String remoteDirPath, int permissions) {
        executeWithChannel(channelSftp -> {
            try {
                createDirRecursive(channelSftp, remoteDirPath, permissions);
                log.info("Directory created successfully: " + remoteDirPath);
            } catch (SftpException e) {
                log.error("Failed to create directory: " + remoteDirPath, e);
                throw new RuntimeException("Failed to create directory: " + remoteDirPath, e);
            }
        });
    }

    private void createDirRecursive(ChannelSftp channelSftp, String remoteDirPath, int permissions) throws SftpException {
        String normalizedPath = remoteDirPath.replaceAll("/+", "/").replaceAll("/$", "");
        if (normalizedPath.isEmpty() || "/".equals(normalizedPath)) {
            return;
        }

        try {
            channelSftp.ls(normalizedPath);
        } catch (SftpException e) {
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                int lastSlashIndex = normalizedPath.lastIndexOf('/');
                if (lastSlashIndex > 0) {
                    String parentPath = normalizedPath.substring(0, lastSlashIndex);
                    createDirRecursive(channelSftp, parentPath, permissions);
                }
                channelSftp.mkdir(normalizedPath);
                if (permissions != -1) {
                    channelSftp.chmod(permissions, normalizedPath);
                }
            } else {
                throw e;
            }
        }
    }

    public void uploadFileFromStream(InputStream inputStream, String remoteFilePath) {
        executeWithChannel(channelSftp -> {
            try (OutputStream outputStream = channelSftp.put(remoteFilePath)) {
                IOUtils.copy(inputStream, outputStream); // 使用 Apache Commons IO 工具
                outputStream.flush();
            } catch (SftpException | IOException e) {
                log.error("Failed to upload file to " + remoteFilePath, e);
                throw new RuntimeException("Upload from stream operation failed", e);
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        log.error("Failed to close input stream", e);
                    }
                }
            }
        });
    }

    public void uploadFile(String localFilePath, String remoteFilePath) {
        executeWithChannel(channelSftp -> {
            try (FileInputStream fis = new FileInputStream(localFilePath)) {
                channelSftp.put(fis, remoteFilePath);
            } catch (SftpException | IOException e) {
                log.error("Failed to upload file from " + localFilePath + " to " + remoteFilePath, e);
                throw new RuntimeException("Upload operation failed", e);
            }
        });
    }

    public void deleteFolder(String folderPath) {
        executeWithChannel(channelSftp -> {
            try {
                deleteFolderRecursive(channelSftp, folderPath);
            } catch (SftpException e) {
                log.error("Failed to delete folder: " + folderPath, e);
                throw new RuntimeException("Delete folder operation failed", e);
            }
        });
    }

    public void deleteFolderContents(String folderPath) {
        executeWithChannel(channelSftp -> {
            try {
                deleteFolderContentsRecursive(channelSftp, folderPath);
            } catch (SftpException e) {
                log.error("Failed to delete contents of folder: " + folderPath, e);
                throw new RuntimeException("Delete folder contents operation failed", e);
            }
        });
    }

    private void deleteFolderContentsRecursive(ChannelSftp channelSftp, String folderPath) throws SftpException {
        try {
            channelSftp.cd(folderPath);
        } catch (SftpException e) {
            log.warn("Folder not found, skipping: " + folderPath);
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
                // 递归删除子文件夹内容
                deleteFolderContentsRecursive(channelSftp, fullPath);
                // 删除空的子文件夹
                channelSftp.rmdir(fullPath);
            } else {
                // 删除文件
                channelSftp.rm(fullPath);
            }
        }
    }

    private void deleteFolderRecursive(ChannelSftp channelSftp, String folderPath) throws SftpException {
        try {
            channelSftp.cd(folderPath);
        } catch (SftpException e) {
            log.warn("Folder not found, skipping: " + folderPath);
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
                deleteFolderRecursive(channelSftp, fullPath);
            } else {
                channelSftp.rm(fullPath);
            }
        }
        channelSftp.cd("..");
        channelSftp.rmdir(folderPath);
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

    public void createRemoteDirAndFile(SftpConn sftpConn, String localProjectPath, String volumePath) {
        Session session = jschConnectionManager.getSession(sftpConn);
        try {
            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            // 创建工作目录
            channelSftp.mkdir(volumePath);
            channelSftp.chmod(0777, volumePath);

            // 创建数据目录
            String dataPath = volumePath + "data/";
            channelSftp.mkdir(dataPath);
            channelSftp.chmod(0777, dataPath);

            // 创建输出目录
            String outputPath = volumePath + "output/";
            channelSftp.mkdir(outputPath);
            channelSftp.chmod(0777, outputPath);

            int lastSlashIndex = localProjectPath.lastIndexOf('/');
            // 上传 TransferEngine 目录
            String packagePath = volumePath + "TransferEngine/";
            uploadDirectory(channelSftp, localProjectPath.substring(0, lastSlashIndex) + "/devCli/TransferEngine", packagePath);
            channelSftp.chmod(0777, packagePath);

            // 上传 main.py 文件
            String mainPath = localProjectPath.substring(0, lastSlashIndex) + "/devCli/main.py";
            try (FileInputStream fis = new FileInputStream(mainPath)) {
                channelSftp.put(fis, volumePath + "/main.py");
            }
            // 上传 watcher.py 文件
            String watchPath = localProjectPath.substring(0, lastSlashIndex) + "/devCli/watcher.py";
            try (FileInputStream fis = new FileInputStream(watchPath)) {
                channelSftp.put(fis, volumePath + "/watcher.py");
            }

            channelSftp.disconnect();
        } catch (SftpException | IOException | JSchException e) {
            log.error("Failed to create remote dir and file for path: " + volumePath, e);
            throw new RuntimeException("Create operation failed", e);
        } finally {
            jschConnectionManager.returnSession(session);
        }
    }
//    public String readRemoteFile(String remoteFilePath) throws JSchException, SftpException, IOException {
//        Session session = null;
//        ChannelSftp channel = null;
//        InputStream inputStream = null;
//        BufferedReader reader = null;
//
//        try {
//            // 获取 SFTP 会话和通道
//            session = jschConnectionManager.getSession();
//            channel = (ChannelSftp) session.openChannel("sftp");
//            channel.connect();
//
//            // 获取远程文件的输入流
//            inputStream = channel.get(remoteFilePath);
//
//            // 使用 BufferedReader 读取文本内容
//            reader = new BufferedReader(new InputStreamReader(inputStream));
//            StringBuilder content = new StringBuilder();
//            String line;
//            while ((line = reader.readLine()) != null) {
//                content.append(line).append("\n");
//            }
//
//            // 返回文件内容（去掉末尾多余的换行符）
//            return content.toString().trim();
//
//        } finally {
//            // 关闭资源
//            if (reader != null) {
//                try {
//                    reader.close();
//                } catch (IOException e) {
//                    // 日志记录或忽略
//                }
//            }
//            if (inputStream != null) {
//                try {
//                    inputStream.close();
//                } catch (IOException e) {
//                    // 日志记录或忽略
//                }
//            }
//            if (channel != null && channel.isConnected()) {
//                channel.disconnect();
//            }
//            // 注意：这里不关闭 session，因为它由 jschConnectionManager 管理
//            jschConnectionManager.returnSession(session);
//        }
//    }
//
//    public void writeRemoteFile(String remoteFilePath, String content) throws JSchException, SftpException, IOException {
//        Session session = null;
//        ChannelSftp channel = null;
//        OutputStream outputStream = null;
//
//        try {
//            // 获取 SFTP 会话和通道
//            session = jschConnectionManager.getSession();
//            channel = (ChannelSftp) session.openChannel("sftp");
//            channel.connect();
//
//            // 将字符串内容写入远程文件
//            outputStream = channel.put(remoteFilePath);
//            outputStream.write(content.getBytes());
//            outputStream.flush();
//
//        } finally {
//            // 关闭资源
//            if (outputStream != null) {
//                try {
//                    outputStream.close();
//                } catch (IOException e) {
//                    // 可以添加日志
//                }
//            }
//            if (channel != null && channel.isConnected()) {
//                channel.disconnect();
//            }
//            // 不关闭 session，交给 jschConnectionManager 管理
//            jschConnectionManager.returnSession(session);
//        }
//    }
//
//    public void uploadFile(String localFilePath, String remoteFilePath) throws Exception {
//        Session session = jschConnectionManager.getSession();
//        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
//        channel.connect();
//        Path filePath = Paths.get(localFilePath);
//        FileInputStream fis = new FileInputStream(filePath.toFile());
//        channel.put(fis, remoteFilePath);
//        channel.disconnect();
//        jschConnectionManager.returnSession(session);
//    }
//
//    public void deleteFolder(String folderPath) {
//        Session session = jschConnectionManager.getSession();
//        try {
//            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
//            channelSftp.connect();
//
//            try {
//                try {
//                    channelSftp.cd(folderPath);
//                } catch (SftpException e) {
//                    return;
//                }
//
//                Vector<ChannelSftp.LsEntry> fileList = channelSftp.ls("*");
//                for (ChannelSftp.LsEntry entry : fileList) {
//                    String fileName = entry.getFilename();
//                    if (".".equals(fileName) || "..".equals(fileName)) {
//                        continue;
//                    }
//
//                    String fullPath = folderPath + "/" + fileName;
//                    if (entry.getAttrs().isDir()) {
//                        deleteFolder(fullPath);
//                    } else {
//                        channelSftp.rm(fullPath);
//                    }
//                }
//                channelSftp.cd("..");
//                channelSftp.rmdir(folderPath);
//
//            } catch (SftpException e) {
//
//            } finally {
//                if (channelSftp != null && channelSftp.isConnected()) {
//                    channelSftp.disconnect();
//                }
//            }
//        } catch (JSchException e) {
//
//        } finally {
//            jschConnectionManager.returnSession(session);
//        }
//    }
//
//    private void uploadDirectory(ChannelSftp channelSftp, String resourcePath, String remotePath) throws SftpException, IOException {
//        File localPackage = new File(resourcePath);
//        try {
//            channelSftp.cd(remotePath);
//        } catch (SftpException e) {
//            channelSftp.mkdir(remotePath);
//            channelSftp.cd(remotePath);
//        }
//        File[] files = localPackage.listFiles();
//        if (files != null) {
//            for (File file : files) {
//                String remoteFilePath = remotePath + "/" + file.getName();
//                if (file.isDirectory()) {
//                    uploadDirectory(channelSftp, file.getAbsolutePath(), remoteFilePath);
//                } else {
//                    try (FileInputStream fis = new FileInputStream(file)) {
//                        channelSftp.put(fis, remoteFilePath);
//                    }
//                }
//            }
//        }
//    }
//
//    public void createRemoteDirAndFile(SftpConn sftpConn, String localProjectPath, String volumePath) throws Exception {
//        Session session = jschConnectionManager.getSession(sftpConn);
//        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
//        channel.connect();
//
//        // Creating Working Dir in Container
//        channel.mkdir(volumePath);
//        channel.chmod(0777, volumePath);
//
//        // Creating Data Dir in Container
//        String dataPath = volumePath + "data/";
//        channel.mkdir(dataPath);
//        channel.chmod(0777, dataPath);
//
//        // Creating Output Dir in Container
//        String outputPath = volumePath + "output/";
//        channel.mkdir(outputPath);
//        channel.chmod(0777, outputPath);
//
//        int lastSlashIndex = localProjectPath.lastIndexOf('/');
//        // Copying Package in Container
//        String packagePath = volumePath + "TransferEngine/";
//        uploadDirectory(channel, localProjectPath.substring(0, lastSlashIndex) + "/devCli/TransferEngine", packagePath);
//        channel.chmod(0777, packagePath);
//
//        // Copying Local File
//        String mainPath = localProjectPath.substring(0, lastSlashIndex) + "/devCli/main.py";
//        uploadFile(mainPath, volumePath + "/main.py");
//
//        channel.disconnect();
//        jschConnectionManager.returnSession(session);
//
//    }

}
