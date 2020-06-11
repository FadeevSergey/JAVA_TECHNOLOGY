package ru.ifmo.rain.fadeev.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {

    private final Queue<Runnable> tasks = new ArrayDeque<>();
    private final List<Thread> workers = new ArrayList<>();

    private static class Result<R> {
        List<R> result;
        RuntimeException exception;
        int count;
        boolean wasException;

        Result(int count) {
            result = new ArrayList<>(Collections.nCopies(count, null));
        }

        synchronized List<R> getResult() throws InterruptedException {
            while (count != result.size()) {
                wait();
            }
            if (wasException) {
                throw exception;
            }

            return result;
        }

        synchronized void setResult(int position, R newData) {
            result.set(position, newData);
            synchronized (this) {
                count++;
                if (count == result.size()) {
                    notify();
                }
            }
        }
    }
    public ParallelMapperImpl(int threads) {
        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(() -> {
                try {
                    Runnable task;
                    while (!Thread.interrupted()) {
                        synchronized (tasks) {
                            while (tasks.isEmpty()) {
                                tasks.wait();
                            }
                            task = tasks.poll();
                        }
                        task.run();
                    }
                } catch (InterruptedException e) {
                    System.err.println("InterruptedException in ParallelMapperImpl(int)");
                }
            });
            workers.add(t);
            t.start();
        }
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> function, List<? extends T> list) throws InterruptedException {
        ru.ifmo.rain.fadeev.concurrent.ParallelMapperImpl.Result<R> result = new ru.ifmo.rain.fadeev.concurrent.ParallelMapperImpl.Result<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            int finalI = i;
            Runnable task = () -> {
                try {
                    result.setResult(finalI, function.apply(list.get(finalI)));
                } catch (RuntimeException e) {
                    result.exception = e;
                    result.wasException = true;
                }
            };
            synchronized (tasks) {
                tasks.add(task);
                tasks.notify();
            }
        }
        return result.getResult();
    }

    @Override
    public void close() {
        for (var thread : workers) {
            thread.interrupt();
            try {
                thread.join();
            } catch (InterruptedException e) {
                System.err.println("InterruptedException in close");
            }
        }
    }
}
