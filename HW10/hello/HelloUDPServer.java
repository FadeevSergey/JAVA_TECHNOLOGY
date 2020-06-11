package ru.ifmo.rain.fadeev.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HelloUDPServer implements HelloServer {
    private DatagramSocket socket;
    private ExecutorService rootExecutorService;
    private ExecutorService interiorExecutorService;

    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("Invalid input arguments");
        } else {
            try {
                new HelloUDPServer().start(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
            } catch (Exception e) {
                System.err.println("Exception in main()");
            }
        }
    }

    @Override
    public void start(int port, int threads) {
        try {
            socket = new DatagramSocket(port);
            interiorExecutorService = Executors.newFixedThreadPool(threads);
            rootExecutorService = Executors.newSingleThreadExecutor();
            rootExecutorService.submit(this::taskForRootExecutorService);
        } catch (Exception e) {
            System.err.println("Exception in start()");
        }
    }

    @Override
    public void close() {
        try {
            socket.close();
            rootExecutorService.shutdown();
            interiorExecutorService.shutdown();
            interiorExecutorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("Exception in close()");
        }
    }

    private void taskForRootExecutorService() {
        try {
            while (!Thread.interrupted() && !socket.isClosed()) {
                DatagramPacket task = new DatagramPacket(
                        new byte[socket.getReceiveBufferSize()], socket.getReceiveBufferSize());
                socket.receive(task);

                interiorExecutorService.submit(() -> this.taskForInteriorExecutorService(task));
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private void taskForInteriorExecutorService(DatagramPacket task) {
        try {
            String message = new String(task.getData(), task.getOffset(), task.getLength(), StandardCharsets.UTF_8);
            message = "Hello, " + message;
            var resultMessage = message.getBytes(StandardCharsets.UTF_8);
            task.setData(resultMessage);
            socket.send(task);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}