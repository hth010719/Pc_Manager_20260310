package com.pcmanager;

import com.pcmanager.infrastructure.bootstrap.ServerBootstrap;

public class ServerMain {
    public static void main(String[] args) throws InterruptedException {
        int port = 5050;
        ServerBootstrap bootstrap = ServerBootstrap.create(port);
        bootstrap.socketServer().start();
        System.out.println("PC Manager server started on 127.0.0.1:" + port);
        Thread.currentThread().join();
    }
}
