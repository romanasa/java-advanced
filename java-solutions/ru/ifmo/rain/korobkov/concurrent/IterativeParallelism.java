package ru.ifmo.rain.korobkov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Solution Class for HW 7
 * @see info.kgeorgiy.java.advanced.concurrent.AdvancedIP
 */
public class IterativeParallelism implements AdvancedIP {

    /**
     * Join values to string.
     * @param threads number of concurrent threads.
     * @param values values to join.
     *
     * @return list of joined result of {@link #toString()} call on each value.
     * @throws InterruptedException if threads were interrupted.
     */
    @Override
    public String join(final int threads, final List<?> values) throws InterruptedException {
        return getValueByFunction(threads, values,
                stream -> stream.map(Object::toString).collect(Collectors.joining()),
                stream -> stream.collect(Collectors.joining()));
    }

    /**
     *
     * Filters values by predicate.
     *
     * @param threads number of concurrent threads.
     * @param values values to filter.
     * @param predicate filter predicate.
     *
     * @return list of values satisfying given predicated. Order of values is preserved.
     * @throws InterruptedException if threads were interrupted.
     */
    @Override
    public <T> List<T> filter(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return getValueByFunction(threads, values,
                stream -> stream.filter(predicate).collect(Collectors.toList()),
                this::collectListFunction);
    }

    /**
     * Maps values.
     *
     * @param threads number of concurrent threads.
     * @param values values to filter.
     * @param f mapper function.
     *
     * @return list of values mapped by given function.
     * @throws InterruptedException if threads were interrupted.
     */
    @Override
    public <T, U> List<U> map(final int threads, final List<? extends T> values, final Function<? super T, ? extends U> f) throws InterruptedException {
        return getValueByFunction(threads, values,
                stream -> stream.map(f).collect(Collectors.toList()),
                this::collectListFunction);
    }

    private<T> List<T> collectListFunction(final Stream<List<T>> stream) {
        return stream.flatMap(Collection::stream).collect(Collectors.toList());
    }

    private <T, R> R getValueByFunction(int threads, final List<? extends T> values,
                                        final Function<Stream<? extends T>, ? extends R> function,
                                        final Function<Stream<? extends R>, R> collector) throws InterruptedException {
        threads = Math.min(threads, values.size());
        final List<Stream<? extends T>> parts = getParts(values, threads);
        final List<R> result = new ArrayList<>(Collections.nCopies(threads, null));

        final List<Thread> workers = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final int finalI = i;
            final Thread thread = new Thread(() -> result.set(finalI, function.apply(parts.get(finalI))));
            workers.add(thread);
            thread.start();
        }

        waitThreads(workers);
        return collector.apply(result.stream());
    }

    private <T> List<Stream<? extends T>> getParts(final List<? extends T> values, final int threads) {
        final int block = values.size() / threads;
        final int add = values.size() % threads;
        final List<Stream<? extends T>> parts = new ArrayList<>(threads);
        for (int i = 0; i * block + Math.min(i, add) < values.size(); i++) {
            final int l = i * block + Math.min(i, add);
            final int r = l + block + (i < add ? 1 : 0);
            parts.add(values.subList(l, r).stream());
        }
        return parts;
    }

    private void waitThreads(final List<Thread> workers) throws InterruptedException {
        for (int i = 0; i < workers.size(); i++) {
            try {
                workers.get(i).join();
            } catch (final InterruptedException exception) {
                for (int j = i + 1; j < workers.size(); j++) {
                    workers.get(j).interrupt();
                }
                for (int j = i + 1; j < workers.size(); j++) {
                    try {
                        workers.get(j).join();
                    } catch (final InterruptedException ignored) {
                    }
                }
                throw exception;
            }
        }
    }

    /**
     * Computes the maximum value for <var>? extends T</var> type.
     *
     * @param threads number or concurrent threads.
     * @param values values to get maximum of.
     * @param comparator value comparator.
     * @return maximum value
     * @throws InterruptedException if threads were interrupted.
     */
    @Override
    public <T> T maximum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator) throws InterruptedException {
        return maximum_impl(threads, values, comparator);
    }

    /**
     * Computes the maximum value
     *
     * @param threads number or concurrent threads.
     * @param values values to get maximum of.
     * @param comparator value comparator.
     * @return maximum value.
     * @throws InterruptedException if threads were interrupted.
     */
    private <T> T maximum_impl(final int threads, final List<T> values, final Comparator<? super T> comparator) throws InterruptedException {
        final Function<Stream<? extends T>, T> maxFunction = stream -> stream.max(comparator).orElse(null);
        return this.<T, T>getValueByFunction(threads, values, maxFunction, maxFunction);
    }

    /**
     * Computes the minimum value
     *
     * @param threads number or concurrent threads.
     * @param values values to get minimum of.
     * @param comparator value comparator.
     * @param <T> value type
     * @return minimum of given values
     * @throws InterruptedException if threads were interrupted.
     */
    @Override
    public <T> T minimum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, Collections.reverseOrder(comparator));
    }

    /**
     * Returns whether any of values satisfies predicate.
     *
     * @param threads number or concurrent threads.
     * @param values values to test.
     * @param predicate test predicate.
     * @return whether any value satisfies predicate or {@code false}, if no values are given.
     * @throws InterruptedException if threads were interrupted.
     */
    @Override
    public <T> boolean any(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return getValueByFunction(threads, values, stream -> stream.anyMatch(predicate),
                stream -> stream.anyMatch(Boolean::booleanValue));
    }

    /**
     * Returns whether all values satisfies predicate.
     *
     * @param threads number or concurrent threads.
     * @param values values to test.
     * @param predicate test predicate.
     * @return whether all values satisfies predicate or {@code true}, if no values are given.
     * @throws InterruptedException if threads were interrupted.
     */
    @Override
    public <T> boolean all(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return !any(threads, values, Predicate.not(predicate));
    }

    /**
     * Reduces values using monoid.
     *
     * @param threads number of concurrent threads.
     * @param values values to reduce.
     * @param monoid monoid to use.
     *
     * @return values reduced by provided monoid or {@link Monoid#getIdentity() identity} if not values specified.
     * @throws InterruptedException if threads were interrupted.
     */
    @Override
    public <T> T reduce(final int threads, final List<T> values, final Monoid<T> monoid) throws InterruptedException {
        return mapReduce(threads, values, Function.identity(), monoid);
    }

    /**
     * Maps and reduces values using monoid.
     * @param threads number of concurrent threads.
     * @param values values to reduce.
     * @param lift mapping function.
     * @param monoid monoid to use.
     *
     * @return values reduced by provided monoid or {@link Monoid#getIdentity() identity} if not values specified.
     * @throws InterruptedException if threads were interrupted
     */
    @Override
    public <T, R> R mapReduce(final int threads, final List<T> values, final Function<T, R> lift, final Monoid<R> monoid) throws InterruptedException {
        final Function<Stream<T>, R> mapReduceFunction = tStream -> tStream.reduce(monoid.getIdentity(),
                (partial, val) -> monoid.getOperator().apply(partial, lift.apply(val)), monoid.getOperator());
        return getValueByFunction(threads, values, mapReduceFunction,
                tStream -> tStream.reduce(monoid.getIdentity(), monoid.getOperator()));
    }
}
