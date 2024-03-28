package com.fish.rpc_client;

import com.fish.codec.RPCResponse;
import com.fish.factory.SingletonFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyClientHandler extends SimpleChannelInboundHandler<RPCResponse> {
    private final UnprocessedRequests unprocessedRequests;

    public NettyClientHandler(){
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
    }
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RPCResponse msg) throws Exception {
//        // 接收到response, 给channel设计别名，让sendRequest里读取response
//        AttributeKey<RPCResponse> key = AttributeKey.valueOf("RPCResponse");
//        ctx.channel().attr(key).set(msg);
//        ctx.channel().close();

        System.out.println("client receive msg: " + msg);
        unprocessedRequests.complete(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
