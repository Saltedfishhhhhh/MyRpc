package com.fish.loadbalance;

import java.util.List;

/**
 * 轮询负载均衡
 */
public class RoundLoadBalance implements LoadBalance{
    private int choose = -1;
    @Override
    public String balance(List<String> addressList) {
        choose++;
        String address = addressList.get(choose);
        System.out.println("address: " + address);
        return address;
    }
}
