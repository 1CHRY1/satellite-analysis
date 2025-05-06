package nnu.mnr.satellitemodeling.model.pojo.common;

import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import nnu.mnr.satellitemodeling.client.WebsocketClient;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/21 17:24
 * @Description:
 */
public class ModelResultCallBack extends ExecStartResultCallback {

    WebsocketClient websocketClient;

    private String projectId;

    private String userId;

    public ModelResultCallBack(String userId, String projectId, WebsocketClient websocketClient) {
        super(System.out, System.err);
        this.projectId = projectId;
        this.userId = userId;
        this.websocketClient = websocketClient;
    }

    @Override
    public void onNext(Frame frame) {
        try {
            if (frame != null) {
                if (frame.getStreamType() == StreamType.STDOUT || frame.getStreamType() == StreamType.STDERR) {
                    String message = new String(frame.getPayload());
                    if (websocketClient != null) {
                        websocketClient.sendMessage(userId, projectId, message);
                    } else {
                        System.err.println("webSocketService is not initialized.");
                    }
                }
            } else {
                System.out.println("frame is null");
            }
            super.onNext(frame);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
