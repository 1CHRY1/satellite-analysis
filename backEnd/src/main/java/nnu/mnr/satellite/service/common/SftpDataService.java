package nnu.mnr.satellite.service.common;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import nnu.mnr.satellite.model.pojo.common.SftpConn;
import nnu.mnr.satellite.config.JSchConnectionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

    public void transferFileToRemoteDockerContainer(String localFilePath, String remoteFilePath) throws Exception {
        Session session = jschConnectionManager.getSession();
        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();
        Path filePath = Paths.get(localFilePath);
        FileInputStream fis = new FileInputStream(filePath.toFile());
        channel.put(fis, remoteFilePath);
        channel.disconnect();
    }

    public void createRemoteDirAndFile(SftpConn sftpConn, String localPath, String volumePath) throws JSchException, SftpException, FileNotFoundException {
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

        // Reading Local File
        Path mainPath = Paths.get(localPath + "/main.py");
        FileInputStream fis = new FileInputStream(mainPath.toFile());
        channel.put(fis, volumePath + "/main.py");

        channel.disconnect();

    }

}
