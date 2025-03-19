package nnu.mnr.satellite.model.pojo.websocket;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/18 21:54
 * @Description:
 */
public class WsSession implements Serializable {

    private final Channel channel;

    public WsSession(Channel channel) {this.channel = channel;}

    public void sendText(String text) {
        channel.writeAndFlush(new TextWebSocketFrame(text));
    }

    public void close() {
        channel.close();
    }

    public String getId() {
        return channel.id().asShortText();
    }

}
