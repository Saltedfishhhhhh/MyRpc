package com.fish;


public class HelloServiceImpl implements HelloService {
    public String sayHello(String name) {
        return "Hello " + name;
    }

    public Integer add(Integer a, Integer b) {
        Integer result = a + b;
        return result;
    }


}
