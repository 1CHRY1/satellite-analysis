package nnu.mnr.satellite.nettywebsocket.netty;

import io.netty.channel.*;
import io.netty.handler.codec.http.websocketx.*;

import java.util.concurrent.ConcurrentHashMap;


@ChannelHandler.Sharable
public class WebSocketServerHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private final WebsocketActionDispatch websocketActionDispatch;

    public WebSocketServerHandler(WebsocketActionDispatch websocketActionDispatch) {
        this.websocketActionDispatch = websocketActionDispatch;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame msg) throws Exception {
        handleWebSocketFrame(ctx, msg);
    }

    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // 获取上下文传递过来的uri，给分发器分发任务使用
        String uri = ctx.channel().attr(AttributeKeyConstant.PATH_KEY).get();
        if (frame instanceof TextWebSocketFrame) {
            ctx.channel().attr(AttributeKeyConstant.textWebSocketFrame).set((TextWebSocketFrame) frame);
            websocketActionDispatch.dispatch(uri, WebsocketActionDispatch.Action.MESSAGE,ctx.channel());
            return;
        }
        if (frame instanceof PingWebSocketFrame) {
            ctx.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        if (frame instanceof CloseWebSocketFrame) {
            websocketActionDispatch.dispatch(uri, WebsocketActionDispatch.Action.CLOSE,ctx.channel());
            ctx.writeAndFlush(frame.retainedDuplicate()).addListener(ChannelFutureListener.CLOSE);
            return;
        }
        if (frame instanceof BinaryWebSocketFrame) {
            return;
        }
        if (frame instanceof PongWebSocketFrame) {
            return;
        }
    }
}
