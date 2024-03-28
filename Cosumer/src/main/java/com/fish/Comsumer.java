package com.fish;


import com.fish.rpc_client.NettyRPCClient;
import com.fish.rpc_client.RPCClientProxy;

public class Comsumer {
    public static void main(String[] args) {
        // 创建一个代理客户端
        NettyRPCClient nettyRPCClient = new NettyRPCClient();
        // 把这个客户端传入代理客户端
        RPCClientProxy rpcClientProxy = new RPCClientProxy(nettyRPCClient);
        // 代理客户端根据不同的服务，获得一个代理类， 并且这个代理类的方法以或者增强（封装数据，发送请求）
        HelloService helloService = rpcClientProxy.getProxy(HelloService.class);
        // 调用代理类的方法，实际上是调用了invoke方法
        String res = helloService.sayHello("fish");
        System.out.println(res);
    }
}
