package nnu.mnr.satellitewebsocket.nettywebsocket.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;


public class GenericHandler extends ChannelInboundHandlerAdapter {

    private final WebsocketActionDispatch websocketActionDispatch;

    public GenericHandler(WebsocketActionDispatch websocketActionDispatch) {
        this.websocketActionDispatch = websocketActionDispatch;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        String uri = ctx.channel().attr(AttributeKeyConstant.PATH_KEY).get();
        ctx.channel().attr(AttributeKeyConstant.idleStateEvent).set(evt);
        websocketActionDispatch.dispatch(uri, WebsocketActionDispatch.Action.EVENT,ctx.channel());
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        String uri = ctx.channel().attr(AttributeKeyConstant.PATH_KEY).get();
        ctx.channel().attr(AttributeKeyConstant.throwable).set(cause);
        websocketActionDispatch.dispatch(uri, WebsocketActionDispatch.Action.ERROR,ctx.channel());
        super.exceptionCaught(ctx, cause);
    }
}
