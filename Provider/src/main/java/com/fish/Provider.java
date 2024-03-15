package com.fish;

import com.fish.rpc_server.NettyRPCServer;
import com.fish.rpc_server.RPCServer;
import com.fish.rpc_server.ServiceProvider;

import static com.fish.config.Myconfig.port;

public class Provider {
    public static void main(String[] args) {
        HelloService helloService = new HelloServiceImpl();
        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.provideServiceInterface(helloService);

        RPCServer RPCServer = new NettyRPCServer(serviceProvider);
        RPCServer.start(port);
    }
}
