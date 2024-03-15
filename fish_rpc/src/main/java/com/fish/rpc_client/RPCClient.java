package com.fish.rpc_client;


import com.fish.codec.RPCRequest;
import com.fish.codec.RPCResponse;

public interface RPCClient {
    RPCResponse sendRequest(RPCRequest request);
}
