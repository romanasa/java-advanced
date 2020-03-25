package ru.ifmo.rain.korobkov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IterativeParallelism implements ListIP {

    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return getValueByFunction(threads, values,
                stream -> stream.map(Object::toString).collect(Collectors.joining()),
                stream -> stream.collect(Collectors.joining()));
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return getValueByFunction(threads, values,
                stream -> stream.filter(predicate).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList()));
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return getValueByFunction(threads, values,
                stream -> stream.map(f).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList()));
    }

    private <T, R> R getValueByFunction(int threads, List<? extends T> values,
                                        Function<Stream<? extends T>, ? extends R> function,
                                        Function<Stream<? extends R>, ? extends R> collector) throws InterruptedException {
        threads = Math.min(threads, values.size());
        int block = values.size() / threads;
        int add = values.size() % threads;

        List<Stream<? extends T>> parts = new ArrayList<>();
        for (int i = 0; i * block + Math.min(i, add) < values.size(); i++) {
            int l = i * block + Math.min(i, add);
            int r = l + block + (i < add ? 1 : 0);
            parts.add(values.subList(l, r).stream());
        }

        List<Thread> workers = new ArrayList<>();
        List<R> result = new ArrayList<>(Collections.nCopies(threads, null));
        for (int i = 0; i < threads; i++) {
            int finalI = i;
            Thread thread = new Thread(() -> result.set(finalI, function.apply(parts.get(finalI))));
            workers.add(thread);
            thread.start();
        }
        for (int i = 0; i < threads; i++) {
            workers.get(i).join();
        }
        return collector.apply(result.stream());
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        Function<Stream<? extends T>, ? extends T> maxFunction = stream -> stream.max(comparator).orElse(null);
        return getValueByFunction(threads, values, maxFunction, maxFunction);
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, Collections.reverseOrder(comparator));
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return getValueByFunction(threads, values, stream -> stream.anyMatch(predicate),
                stream -> stream.anyMatch(Boolean::booleanValue));
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return !any(threads, values, Predicate.not(predicate));
    }
}
