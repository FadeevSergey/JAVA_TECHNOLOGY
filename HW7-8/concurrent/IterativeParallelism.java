package ru.ifmo.rain.fadeev.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ScalarIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class IterativeParallelism implements ScalarIP {
    private ParallelMapper mapper;

    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }

    public IterativeParallelism() {
        this.mapper = null;
    }

    private void joinWorkers(List<Thread> workers, InterruptedException interruptedException) throws InterruptedException {
        workers.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                interruptedException.addSuppressed(e);
            }
        });

        if (interruptedException.getSuppressed().length > 0) {
            throw interruptedException;
        }
    }

    private <T, B, R> R monoid(int threads, List<? extends T> values,
                               Function<List<? extends T>, B> partFunction,
                               Function<List<? extends B>, R> resultFunction) throws InterruptedException {

        int numberOfThreads = Math.min(threads, values.size());

        if (numberOfThreads == values.size()) {
            numberOfThreads = Math.max(numberOfThreads / 2, 1);
        }
        int length = values.size() / numberOfThreads;

        if (mapper == null) {
            List<B> resultOnSublists = new ArrayList<>(Collections.nCopies(numberOfThreads, null));
            List<Thread> workers = new ArrayList<>();

            InterruptedException interruptedException = new InterruptedException();

            for (int i = 0; i < numberOfThreads; i++) {
                final int leftBound = i * length;
                final int rightBound;

                if(i == numberOfThreads - 1) {
                    rightBound = values.size();
                } else {
                    rightBound = leftBound + length;
                }

                Thread thread = new Thread(() ->
                        resultOnSublists.set(leftBound / length, partFunction.apply(values.subList(leftBound, rightBound)))
                );
                workers.add(thread);
                thread.start();
            }

            joinWorkers(workers, interruptedException);
            return resultFunction.apply(resultOnSublists);
        } else {

            List<List<? extends T>> listOfSublists = new ArrayList<>(Collections.nCopies(numberOfThreads, null));

            for (int i = 0; i < numberOfThreads; i++) {
                final int leftBound = i * length;
                final int rightBound;

                if (i == numberOfThreads - 1) {
                    rightBound = values.size();
                } else {
                    rightBound = leftBound + length;
                }

                listOfSublists.set(leftBound / length, values.subList(leftBound, rightBound));
            }

            R result = resultFunction.apply(mapper.map(partFunction, listOfSublists));
            return result;
        }
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return monoid(threads, values, list -> list.stream().max(comparator).orElseThrow(),
                list -> list.stream().max(comparator).orElseThrow());
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return monoid(threads, values, list -> list.stream().min(comparator).orElseThrow(),
                list -> list.stream().min(comparator).orElseThrow());
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return monoid(threads, values, list -> list.stream().allMatch(predicate),
                list -> list.stream().allMatch(Predicate.isEqual(true)));
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return monoid(threads, values, list -> list.stream().anyMatch(predicate),
                list -> list.stream().anyMatch(Predicate.isEqual(true)));
    }
}