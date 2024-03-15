package com.fish.loadbalance;

import java.util.List;
import java.util.Random;

/**
 * 随机负载均衡
 */
public class RandomLoadBalance implements  LoadBalance{
    @Override
    public String balance(List<String> addressList) {

        Random random = new Random();
        if (addressList.size() == 0) {
            throw new RuntimeException("没有可用的服务");
        }
        int choose = random.nextInt(addressList.size());
        String address = addressList.get(choose);
        System.out.println("address: " + address);
        return address;
    }
}
