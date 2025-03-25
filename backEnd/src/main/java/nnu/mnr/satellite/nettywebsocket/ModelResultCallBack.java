package nnu.mnr.satellite.nettywebsocket;

import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import nnu.mnr.satellite.service.websocket.ModelSocketService;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/21 17:24
 * @Description:
 */
public class ModelResultCallBack extends ExecStartResultCallback {

    ModelSocketService modelSocketService;

    private String projectId;

    private String userId;

    public ModelResultCallBack(String userId, String projectId, ModelSocketService modelSocketService) {
        super(System.out, System.err);
        this.projectId = projectId;
        this.userId = userId;
        this.modelSocketService = modelSocketService;
    }

    @Override
    public void onNext(Frame frame) {
        try {
            if (frame != null) {
                if (frame.getStreamType() == StreamType.STDOUT || frame.getStreamType() == StreamType.STDERR) {
                    String message = new String(frame.getPayload());
                    if (modelSocketService != null) {
                        modelSocketService.sendMessageByProject(userId, projectId, message);
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
