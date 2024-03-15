package com.fish;


import com.fish.proxy.ProxyFactory;

public class Comsumer {
    public static void main(String[] args) {
        HelloService helloService = ProxyFactory.getProxy(HelloService.class);

        Integer add = helloService.add(1, 2);
        String result = helloService.sayHello("fishhh");

        System.out.println(result);
        System.out.println(add);

    }
}
