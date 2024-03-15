package com.fish;

import com.fish.common.URL;
import com.fish.protocol.HttpServer;
import com.fish.register.LocalRegister;
import com.fish.register.MapRemoteRegister;

public class Provider {
    public static void main(String[] args) {
        LocalRegister.register(HelloService.class.getName(),"1.0",HelloServiceImpl.class);
        URL url = new URL("localhost", 8080);
        MapRemoteRegister.register(HelloService.class.getName(), url);

        HttpServer server = new HttpServer();
        server.start(url.getHostname(), 8080);
    }
}
