package ru.ifmo.rain.korobkov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IterativeParallelism implements AdvancedIP {

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

    private <T, R> R getValueByFunction(int threads, List<T> values,
                                        Function<Stream<T>, R> function,
                                        Function<Stream<R>, R> collector) throws InterruptedException {
        threads = Math.min(threads, values.size());
        int block = values.size() / threads;
        int add = values.size() % threads;

        List<Stream<T>> parts = new ArrayList<>();
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
        return maximum_impl(threads, values, comparator);
    }

    private <T> T maximum_impl(int threads, List<T> values, Comparator<? super T> comparator) throws InterruptedException {
        Function<Stream<T>, T> maxFunction = stream -> stream.max(comparator).orElse(null);
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

    private <T> Function<Stream<T>, T> getReduceFunction(Monoid<T> monoid) {
        return tStream -> tStream.reduce(monoid.getIdentity(), monoid.getOperator());
    }

    @Override
    public <T> T reduce(int threads, List<T> values, Monoid<T> monoid) throws InterruptedException {
        Function<Stream<T>, T> reduceFunction = getReduceFunction(monoid);
        return getValueByFunction(threads, values, reduceFunction, reduceFunction);
    }

    @Override
    public <T, R> R mapReduce(int threads, List<T> values, Function<T, R> lift, Monoid<R> monoid) throws InterruptedException {
        Function<Stream<T>, R> mapReduceFunction = tStream -> tStream.reduce(monoid.getIdentity(),
                (identity, val) -> monoid.getOperator().apply(identity, lift.apply(val)),
                monoid.getOperator());
        return getValueByFunction(threads, values, mapReduceFunction, getReduceFunction(monoid));
    }
}
