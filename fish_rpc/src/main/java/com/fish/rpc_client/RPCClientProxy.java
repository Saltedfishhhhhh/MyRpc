package com.fish.rpc_client;


import com.fish.codec.RPCRequest;
import com.fish.codec.RPCResponse;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@AllArgsConstructor
@NoArgsConstructor
public class RPCClientProxy implements InvocationHandler {
    private RPCClient client;

    // jdk 动态代理， 每一次代理对象调用方法，会经过此方法增强（反射获取request对象，发送至客户端）
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // request的构建，使用了lombok中的builder，代码简洁
        // 接口名
        String interfaceName = method.getDeclaringClass().getName();
        RPCRequest request = RPCRequest.builder().interfaceName(interfaceName)
                .methodName(method.getName())
                .params(args)
                .paramsTypes(method.getParameterTypes()).build();
        //数据传输,默认使用netty
        if(client == null){
             client = new NettyRPCClient(interfaceName);
        }

        RPCResponse response = client.sendRequest(request);

        //System.out.println(response);
        if (response == null || response.getCode() == 500) {
            throw new RuntimeException("调用服务失败");
        }
        return response.getData();
    }
    public <T>T getProxy(Class<T> clazz){
        Object o = Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, this);
        return (T)o;
    }
}
