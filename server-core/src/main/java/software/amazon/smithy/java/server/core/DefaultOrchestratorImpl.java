/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.core;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import software.amazon.smithy.java.server.Service;

public class DefaultOrchestratorImpl implements Orchestrator {

    private final List<Handler> handlers;
    private final BlockingQueue<Work> queue;
    private final int numberOfWorkers;

    public DefaultOrchestratorImpl(Service service, int numberOfWorkers) {
        this.handlers = assembleHandlers(service);
        this.numberOfWorkers = numberOfWorkers;
        this.queue = new LinkedBlockingQueue<>();
        var es = Executors.newFixedThreadPool(numberOfWorkers);
        for (int i = 0; i < numberOfWorkers; i++) {
            es.submit(new ConsumerTask(queue));
        }

    }

    @Override
    public CompletableFuture<Job> enqueue(Job job) {
        CompletableFuture<Job> future = new CompletableFuture<>();
        queue.add(new Work(job, handlers, queue, future));
        return future;
    }

    private static List<Handler> assembleHandlers(Service service) {
        return List.of(new OperationHandler(service));
    }

    private final class Work {
        private final Job job;
        private final Queue<Handler> queue;
        private final BlockingQueue<Work> workQueue;
        private final CompletableFuture<Job> signal;
        private final Queue<Handler> soFar;
        private State state = State.BEFORE;

        private Work(Job job, List<Handler> handlers, BlockingQueue<Work> workQueue, CompletableFuture<Job> signal) {
            this.job = job;
            this.queue = new ArrayDeque<>(handlers);
            this.workQueue = workQueue;
            this.signal = signal;
            this.soFar = new ArrayDeque<>();
        }

        public void work() {
            if ((job.isDone() || job.getFailure().isPresent()) && state == State.BEFORE) {
                state = State.AFTER;
            }

            while (state == State.BEFORE) {
                if (queue.isEmpty()) {
                    state = State.AFTER;
                    break;
                }
                Handler handler = queue.poll();
                soFar.add(handler);
                CompletableFuture<Void> cf = handler.before(job);
                if (!cf.isDone()) {
                    cf.whenComplete((e, t) -> DefaultOrchestratorImpl.this.queue.add(this));
                    return;
                }
            }
            if (state == State.AFTER) {
                while (!soFar.isEmpty()) {
                    Handler handler = soFar.poll();
                    CompletableFuture<Void> cf = handler.after(job);
                    if (!cf.isDone()) {
                        cf.whenComplete((e, t) -> DefaultOrchestratorImpl.this.queue.add(this));
                        return;
                    }
                }
                state = State.DONE;
                signal.complete(job);
            }
        }

        public boolean isDone() {
            return state == State.DONE;
        }

        private enum State {
            BEFORE,
            AFTER,
            DONE;
        }

    }

    private static final class ConsumerTask implements Runnable {
        private final BlockingQueue<Work> workQueue;

        private ConsumerTask(BlockingQueue<Work> workQueue) {
            this.workQueue = workQueue;
        }


        @Override
        public void run() {
            try {
                while (true) {
                    Work work = workQueue.take();
                    work.work();
                }
            } catch (InterruptedException ignored) {

            }
        }
    }


}
