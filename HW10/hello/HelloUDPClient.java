package ru.ifmo.rain.fadeev.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HelloUDPClient implements HelloClient {

    public static void main(String[] args) {
        try {
            if (args == null || args.length != 5 || args[0] == null || args[1] == null || args[2] == null || args[3] == null || args[4] == null) {
                System.err.println("Invalid input arguments");
            } else {

                var host = args[0];
                var port = Integer.parseInt(args[1]);
                var prefix = args[2];
                var threads = Integer.parseInt(args[3]);
                var requests = Integer.parseInt(args[4]);

                new HelloUDPClient().run(host, port, prefix, threads, requests);

            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        try {
            var addr = InetAddress.getByName(host);
            InetSocketAddress address = new InetSocketAddress(addr, port);

            ExecutorService service = Executors.newFixedThreadPool(threads);
            for (int i = 0; i < threads; i++) {
                int finalI = i;
                service.submit(() -> task(address, prefix, finalI, requests));
            }

            service.shutdown();
            service.awaitTermination(5 * requests * threads, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private void task(SocketAddress address, String prefix, int threadId, int requests) {
        try (DatagramSocket socket = new DatagramSocket();) {
            socket.setSoTimeout(500);
            var len = socket.getReceiveBufferSize();
            var buf = new byte[len];
            DatagramPacket request = new DatagramPacket(buf, len, address);

            for (int i = 0; i < requests; i++) {
                var message = prefix + threadId + "_" + i;
                addMessage(request, socket, message);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private void addMessage(DatagramPacket request, DatagramSocket socket, String message) {
        boolean answerExpected = true;
        while (!socket.isClosed() && !Thread.interrupted() && answerExpected) {
            try {
                var data = message.getBytes(StandardCharsets.UTF_8);
                request.setData(data);
                socket.send(request);

                var len = socket.getReceiveBufferSize();
                request.setData(new byte[len]);
                socket.receive(request);

                var bytes = request.getData();
                var offset = request.getOffset();
                var length = request.getLength();

                String resultOfRequest = new String(bytes, offset, length, StandardCharsets.UTF_8);

                if (resultOfRequest.contains(message)) {
                    answerExpected = false;
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }
}