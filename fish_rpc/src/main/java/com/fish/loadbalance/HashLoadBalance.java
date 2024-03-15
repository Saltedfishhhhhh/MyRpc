package com.fish.loadbalance;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class HashLoadBalance implements LoadBalance{
    @Override
    public String balance(List<String> addressList) {
        ConsistentHash ch = new ConsistentHash(addressList, 100);
        HashMap<String,String> map = ch.map;
        // 获取本地ip地址
        String server;
        String address;
        try {
            address = InetAddress.getLocalHost().getHostAddress();
            server= ch.getServer(address);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        System.out.println("address: " + address);
        return map.get(server);
    }
}

class ConsistentHash {
    //TreeMap中的key表示服务器的hash值，value表示服务器。模拟一个哈希环
    private static TreeMap<Integer, String> Nodes = new TreeMap();
    private static int VIRTUAL_NODES = 160;//虚拟节点个数，用户指定，默认160
    private static List<String> urls = new ArrayList<>();//真实物理节点集合
    public static HashMap<String, String> map = new HashMap<>();//将服务实例与url地址一一映射

    public ConsistentHash(List<String> urls, int VIRTUAL_NODES) {
        this.urls = urls;
        this.VIRTUAL_NODES = VIRTUAL_NODES;
        //初始化，将所有的服务器加入hash环中
        for (String url : urls) {
            Nodes.put(getHash(url), url);
            map.put(url, url);
            for (int i = 0; i < VIRTUAL_NODES; i++) {
                int hash = getHash(url + "#" + i);
                Nodes.put(hash, url);
            }
        }
    }


    //得到Ip地址
    public String getServer(String clientInfo) {
        int hash = getHash(clientInfo);
        //得到大于该Hash值的子红黑树
        SortedMap<Integer, String> subMap = Nodes.tailMap(hash);
        //获取该子树最小元素
        Integer nodeIndex = subMap.firstKey();
        //没有大于该元素的子树 取整树的第一个元素
        if (nodeIndex == null) {
            nodeIndex = Nodes.firstKey();
        }
        return Nodes.get(nodeIndex);
    }

    //使用FNV1_32_HASH算法计算服务器的Hash值,这里不使用重写hashCode的方法，最终效果没区别
    private static int getHash(String str) {
        final int p = 16777619;
        int hash = (int) 2166136261L;
        for (int i = 0; i < str.length(); i++) {
            hash = (hash ^ str.charAt(i)) * p;
            hash += hash << 13;
            hash ^= hash >> 7;
            hash += hash << 3;
            hash ^= hash >> 17;
            hash += hash << 5;
            //如果算出来的值为负数 取其绝对值
            if (hash < 0) {
                hash = Math.abs(hash);
            }
        }
        return hash;
    }
}