package nnu.mnr.satellite.websocket.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import nnu.mnr.satellite.enums.websocket.WsAction;


public class GenericHandler extends ChannelInboundHandlerAdapter {

    private final WsActionDispatch wsActionDispatch;

    public GenericHandler(WsActionDispatch wsActionDispatch) {
        this.wsActionDispatch = wsActionDispatch;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        String uri = ctx.channel().attr(AttributeKeyConstant.PATH_KEY).get();
        ctx.channel().attr(AttributeKeyConstant.idleStateEvent).set(evt);
        wsActionDispatch.dispatch(uri, WsAction.EVENT,ctx.channel());
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        String uri = ctx.channel().attr(AttributeKeyConstant.PATH_KEY).get();
        ctx.channel().attr(AttributeKeyConstant.throwable).set(cause);
        wsActionDispatch.dispatch(uri, WsAction.ERROR,ctx.channel());
        super.exceptionCaught(ctx, cause);
    }
}
