package com.pcmanager;

import com.pcmanager.infrastructure.bootstrap.ServerBootstrap;

/**
 * 서버 프로세스 시작점이다.
 *
 * 부트스트랩으로 서비스/스토어/소켓 서버를 구성한 뒤,
 * 메인 스레드를 유지해 프로세스가 바로 종료되지 않게 한다.
 */
public class ServerMain {
    public static void main(String[] args) throws InterruptedException {
        int port = 5050;
        ServerBootstrap bootstrap = ServerBootstrap.create(port);
        bootstrap.socketServer().start();
        System.out.println("PC Manager server started on 127.0.0.1:" + port);
        // 소켓 서버는 별도 스레드에서 동작하므로 메인 스레드를 붙잡아 프로세스를 유지한다.
        Thread.currentThread().join();
    }
}
