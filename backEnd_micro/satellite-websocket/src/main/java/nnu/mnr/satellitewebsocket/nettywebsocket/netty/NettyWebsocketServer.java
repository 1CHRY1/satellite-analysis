package nnu.mnr.satellitewebsocket.nettywebsocket.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator;
import io.netty.handler.timeout.IdleStateHandler;
import nnu.mnr.satellitewebsocket.nettywebsocket.WebsocketProperties;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;


public class NettyWebsocketServer {

    private final WebsocketActionDispatch websocketActionDispatch;

    private WebsocketProperties websocketProperties;

    public NettyWebsocketServer(WebsocketActionDispatch websocketActionDispatch,WebsocketProperties websocketProperties) {
        this.websocketActionDispatch = websocketActionDispatch;
        this.websocketProperties = websocketProperties;
    }

    public void start() throws InterruptedException {
        NioEventLoopGroup boss = new NioEventLoopGroup(websocketProperties.getBossThreadNums());
        NioEventLoopGroup worker = new NioEventLoopGroup(websocketProperties.getWorkerThreadNums());
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(boss,worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel channel) {
                        ChannelPipeline pipeline = channel.pipeline();
                        pipeline.addLast(new HttpServerCodec())
                                .addLast(new HttpObjectAggregator(websocketProperties.getMaxContentLength()))
                                .addLast(new IdleStateHandler(websocketProperties.getReaderIdleTimeSeconds()
                                        ,websocketProperties.getWriterIdleTimeSeconds()
                                        ,websocketProperties.getAllIdleTimeSeconds()))
                                .addLast(new HttpRequestHandler(websocketActionDispatch))
                                .addLast(new WebSocketFrameAggregator(Integer.MAX_VALUE))
                                .addLast(new GenericHandler(websocketActionDispatch))
                                .addLast(new WebSocketServerHandler(websocketActionDispatch));
                    }
                })
                // 连接超时时间
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,websocketProperties.getConnectTimeout())
                // TCP 连接的请求队列的最大长度
                .option(ChannelOption.SO_BACKLOG,websocketProperties.getBackLog())
                // 消息是否立即发送
                .option(ChannelOption.TCP_NODELAY,websocketProperties.isTcpNoDelay())
                // TCP 建立连接后，每隔一段时间就会对连接做一次探测
                .childOption(ChannelOption.SO_KEEPALIVE,Boolean.TRUE);

        ChannelFuture channelFuture = bootstrap.bind(websocketProperties.getPort()).sync();
        Channel serverChannle = channelFuture.channel();
        serverChannle.closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                boss.shutdownGracefully();
                worker.shutdownGracefully();
            }
        });
    }

    private static String getIpAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                if (!networkInterface.isLoopback() && networkInterface.isUp()) {
                    Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress address = addresses.nextElement();
                        if (address instanceof Inet4Address) {
                            return address.getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }
}
