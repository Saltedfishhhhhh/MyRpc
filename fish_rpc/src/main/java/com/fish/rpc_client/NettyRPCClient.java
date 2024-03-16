package com.fish.rpc_client;

import com.fish.codec.RPCRequest;
import com.fish.codec.RPCResponse;
import com.fish.config.Myconfig;
import com.fish.factory.SingletonFactory;
import com.fish.register.ServiceRegister;
import com.fish.register.ZkServiceRegister;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;
import java.util.concurrent.*;

/**
 * 实现RPCClient接口
 */
public class NettyRPCClient implements RPCClient {
    private static final Bootstrap bootstrap;
    private static final EventLoopGroup eventLoopGroup;
    private String host;
    private int port;
    private ServiceRegister serviceRegister;
    private final UnprocessedRequests unprocessedRequests;
    public NettyRPCClient(String interfaceName) {
        this.serviceRegister = new ZkServiceRegister(interfaceName);
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
    }
    // netty客户端初始化，重复使用
    static {

        eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class)
                .handler(new NettyClientInitializer());
    }

    /**
     * netty的传输都是异步的，发送request，会立刻返回一个值， 不是想要的相应的response
     */
    @Override
    public RPCResponse sendRequest(RPCRequest request) {
        InetSocketAddress address = serviceRegister.serviceDiscovery(request.getInterfaceName());
        host = address.getHostName();
        port = address.getPort();
        int maxRetryTimes = Myconfig.retry_time;
        int retryCount = 0;
        while (retryCount < maxRetryTimes) {
            try {

                ChannelFuture channelFuture = bootstrap.connect(host, port).sync();
                Channel channel = channelFuture.channel();
                channel.writeAndFlush(request);
//                channel.closeFuture().sync();
//                // 阻塞的获得结果，通过给channel设计别名，获取特定名字下的channel中的内容（这个在hanlder中设置）
//                // 实际上不应通过阻塞，可通过回调函数，后面可以再进行优化
//                AttributeKey<RPCResponse> key = AttributeKey.valueOf("RPCResponse");
//                RPCResponse response = channel.attr(key).get();
//                return response;

                CompletableFuture<RPCResponse> resultFuture = new CompletableFuture<>();
                unprocessedRequests.put(request.getRequestId(), resultFuture);
                channel.closeFuture().addListener((ChannelFutureListener) future -> {
                    if (resultFuture.isDone()) {
                        return;
                    }
                    resultFuture.completeExceptionally(new IllegalStateException());
                });
                return resultFuture.get();

            } catch (InterruptedException e) {
                retryCount++;
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("Failed to send request after " + maxRetryTimes + " retries.");
        return null;
    }


}







