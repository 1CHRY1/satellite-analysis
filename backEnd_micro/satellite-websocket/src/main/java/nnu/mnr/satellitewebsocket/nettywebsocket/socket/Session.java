package nnu.mnr.satellitewebsocket.nettywebsocket.socket;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.io.Serializable;



public class Session implements Serializable {
    private final Channel channel;

    public Session(Channel channel) {
        this.channel = channel;
    }

    public void sendText(String text) {
        channel.writeAndFlush(new TextWebSocketFrame(text));
    }

    public void close() {
        channel.close();
    }

    public String getId() {
        return channel.id().asShortText();
    }

    public boolean isOpen() {
        return channel.isOpen();
    }
}
