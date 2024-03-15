package com.fish.config;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Myconfig {
    // 服务地址,获取一下
    public static String server_address;

    static {
        try {
            server_address = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    // 注册中心的地址
    public static String zk_address_port = "localhost:2181";
    // 服务提供者端口
    public static int port = 8900;
    // 均衡策略 random，round，hash
    public static String loadBalance = "random";
    // 重试次数
    public static int retry_time = 3;
    // request超时时间
    public static int requestTimeout = 1000;

}
