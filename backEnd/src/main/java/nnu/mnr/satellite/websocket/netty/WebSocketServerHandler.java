package nnu.mnr.satellite.websocket.netty;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.*;
import nnu.mnr.satellite.enums.websocket.WsAction;


@ChannelHandler.Sharable
public class WebSocketServerHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private final WsActionDispatch wsActionDispatch;

    public WebSocketServerHandler(WsActionDispatch wsActionDispatch) {
        this.wsActionDispatch = wsActionDispatch;
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
            wsActionDispatch.dispatch(uri, WsAction.MESSAGE,ctx.channel());
            return;
        }
        if (frame instanceof PingWebSocketFrame) {
            ctx.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        if (frame instanceof CloseWebSocketFrame) {
            wsActionDispatch.dispatch(uri, WsAction.CLOSE,ctx.channel());
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
