package com.fish.register;

import com.fish.config.Myconfig;
import com.fish.loadbalance.HashLoadBalance;
import com.fish.loadbalance.LoadBalance;
import com.fish.loadbalance.RandomLoadBalance;
import com.fish.loadbalance.RoundLoadBalance;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.fish.config.Myconfig.zk_address_port;

public class ZkServiceRegister implements ServiceRegister {
    // curator 提供的zookeeper客户端
    private CuratorFramework client;
    // zookeeper根路径节点
    private static final String ROOT_PATH = "MyRPC";
    private LoadBalance loadBalance = new RandomLoadBalance();
    private Map<String, List<String>> cachedServiceAddresses = new HashMap<>();

    // 这里负责zookeeper客户端的初始化，并与zookeeper服务端建立连接
    public ZkServiceRegister() {
        // 指数时间重试
        RetryPolicy policy = new ExponentialBackoffRetry(1000, 3);
        // zookeeper的地址固定，不管是服务提供者还是，消费者都要与之建立连接
        // sessionTimeoutMs 与 zoo.cfg中的tickTime 有关系，
        // zk还会根据minSessionTimeout与maxSessionTimeout两个参数重新调整最后的超时值。默认分别为tickTime 的2倍和20倍
        // 使用心跳监听状态
        this.client = CuratorFrameworkFactory.builder().connectString(zk_address_port)
                .sessionTimeoutMs(40000).retryPolicy(policy).namespace(ROOT_PATH).build();
        this.client.start();
    }
    public ZkServiceRegister(String interfaceName) {
        RetryPolicy policy = new ExponentialBackoffRetry(1000, 3);
        this.client = CuratorFrameworkFactory.builder().connectString(zk_address_port)
                .sessionTimeoutMs(40000).retryPolicy(policy).namespace(ROOT_PATH).build();
        this.client.start();

        // 监听服务提供者状态变化，更新缓存
        String servicePath = "/" + interfaceName;
        try {
            client.getChildren().usingWatcher(new CuratorWatcher() {
                @Override
                public void process(WatchedEvent event) throws Exception {
                    List<String> children = client.getChildren().usingWatcher(this).forPath(servicePath);
                    updateServiceAddresses(interfaceName,children);
                    client.getChildren().usingWatcher(this).forPath(servicePath);
                }
            }).forPath(servicePath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void register(String serviceName, InetSocketAddress serverAddress) {
        try {
            // serviceName创建成永久节点，服务提供者下线时，不删服务名，只删地址
            if (client.checkExists().forPath("/" + serviceName) == null) {
                client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath("/" + serviceName);
            }
            // 路径地址，一个/代表一个节点
            String path = "/" + serviceName + "/" + getServiceAddress(serverAddress);
            // 临时节点，服务器下线就删除节点
            client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 根据服务名返回地址,服务发现
    @Override
    public InetSocketAddress serviceDiscovery(String serviceName) {
        String loadMethod = Myconfig.loadBalance;
        switch (loadMethod) {
            case "hash":
                loadBalance = new HashLoadBalance();
                break;
            case "round":
                loadBalance = new RoundLoadBalance();
                break;
            default:
                loadBalance = new RandomLoadBalance();
        }

        // 从缓存中选择服务地址
        List<String> serviceAddresses = cachedServiceAddresses.get(serviceName);
        if (serviceAddresses != null && !serviceAddresses.isEmpty()) {
            System.out.print("使用缓存");
            String selectedAddress = loadBalance.balance(serviceAddresses);
            return parseAddress(selectedAddress);
        }

        try {
            // 缓存中没有地址，从ZooKeeper中获取服务地址列表
            List<String> addresses = client.getChildren().forPath("/" + serviceName);
            updateServiceAddresses(serviceName, addresses);
            // 选择一个服务地址
            String selectedAddress = loadBalance.balance(cachedServiceAddresses.get(serviceName));
            return parseAddress(selectedAddress);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    // 更新缓存中的服务地址列表
    private void updateServiceAddresses(String serviceName,List<String> addresses) {
        synchronized (cachedServiceAddresses) {
            // 添加新的服务地址到缓存
            cachedServiceAddresses.put(serviceName, addresses);
            System.out.print("检测到"+serviceName+"节点变化，缓存同步成功");
        }
    }

    private String getServiceAddress(InetSocketAddress serverAddress) {
        return serverAddress.getAddress().getHostAddress()+
                ":" +
                serverAddress.getPort();
    }

    // 字符串解析为地址
    private InetSocketAddress parseAddress(String address) {
        String[] result = address.split(":");
        return new InetSocketAddress(result[0], Integer.parseInt(result[1]));
    }
}