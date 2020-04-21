package ru.ifmo.rain.korobkov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

public class ParallelMapperImpl implements ParallelMapper {

    private final List<Thread> workers = new ArrayList<>();
    private final SynchronizedQueue<Task<?, ?>> taskQueue = new SynchronizedQueue<>();


    /**
     * Constructor for ParallelMapper
     * @param threads Mapper will use {@code threads} threads.
     */
    public ParallelMapperImpl(final int threads) {
        IntStream.range(0, threads).mapToObj(i -> new Thread(() -> {
            try {
                while (!Thread.interrupted()) {
                    final Task<?, ?> task;
                    final Runnable runnable;
                    synchronized (taskQueue) {
                        task = taskQueue.peek();
                        runnable = task.poll();
                    }
                    runnable.run();
                    task.finish();
                }
            } catch (final InterruptedException e) {
//                e.printStackTrace();
            } finally {
                Thread.currentThread().interrupt();
            }
        })).forEach(workers::add);
        workers.forEach(Thread::start);
    }

    private static class SynchronizedQueue<T> {
        private final Queue<T> queue = new ArrayDeque<>();

        public synchronized void add(final T e) {
            queue.add(e);
            notifyAll();
        }

        public synchronized void poll() {
            queue.poll();
        }

        public synchronized T peek() throws InterruptedException {
            while (queue.isEmpty()) {
                wait();
            }
            return queue.peek();
        }
    }

    private class Task<T, R> {

        private final Queue<Runnable> queue = new ArrayDeque<>();
        private final SynchronizedMapList result;
        private final Function<? super T, ? extends R> f;
        private volatile boolean terminated = false;

        private int runned;
        private int completed;

        public Task(final Function<? super T, ? extends R> f, final List<? extends T> args) {
            result = new SynchronizedMapList(args.size());
            this.f = f;
            runned = 0;
            completed = 0;

            IntStream.range(0, args.size())
                    .<Runnable>mapToObj(i -> () -> result.set(i, args.get(i)))
                    .forEach(queue::add);
        }

        public synchronized Runnable poll() {
            final Runnable runnable = queue.poll();
            runned++;
            if (runned == result.data.size()) {
                taskQueue.poll();
            }
            return runnable;
        }

        public synchronized void finish() {
            completed++;
            if (completed == result.data.size()) {
                terminate();
            }
        }

        private void terminate() {
            terminated = true;
            notify();
        }

        public synchronized List<R> getResult() throws InterruptedException {
            while (!terminated) {
                wait();
            }
            return result.get();
        }

        private class SynchronizedMapList {
            private final List<R> data;
            private RuntimeException runtimeException = null;

            SynchronizedMapList(final int length) {
                data = new ArrayList<>(Collections.nCopies(length, null));
            }

            private void set(final int pos, final T value) {
//                if (terminated) {
//                    return;
//                }
                try {
                    setByIndex(pos, f.apply(value));
                } catch (final RuntimeException e) {
                    addException(e);
                }
            }

            private synchronized void setByIndex(final int pos, final R value) {
//                if (terminated) {
//                    return;
//                }
                data.set(pos, value);
            }

            private synchronized void addException(final RuntimeException e) {
                if (terminated) {
                    return;
                }
                if (runtimeException == null) {
                    runtimeException = e;
                } else {
                    runtimeException.addSuppressed(e);
                }
            }

            public synchronized List<R> get() {
                if (runtimeException != null) {
                    throw runtimeException;
                }
                return data;
            }
        }
    }


    /**
     * Maps function {@code f} over specified {@code args}.
     *
     * @param f mapping function.
     * @param args arguments for {@code f}.
     * @param <T> source type.
     * @param <R> result type.
     * @return list with results of applying f function.
     * @throws InterruptedException if threads were interrupted.
     */
    @Override
    public <T, R> List<R> map(final Function<? super T, ? extends R> f, final List<? extends T> args) throws InterruptedException {
        final Task<T, R> task = new Task<>(f, args);
        taskQueue.add(task);
        return task.getResult();
    }

    /**
     * Terminates all created threads
     */
    @Override
    public void close() {
        workers.forEach(Thread::interrupt);
        taskQueue.queue.forEach(Task::terminate);
        try {
            IterativeParallelism.waitThreads(workers, false);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }
}
