/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import software.amazon.smithy.java.server.Service;

public class DefaultOrchestratorImpl implements Orchestrator {
    private static final System.Logger LOGGER = System.getLogger(DefaultOrchestratorImpl.class.getName());

    private final List<Handler> handlers;
    private final BlockingQueue<Runnable> queue;
    private final int numberOfWorkers;

    public DefaultOrchestratorImpl(Service service, int numberOfWorkers, List<Handler> endpointHandlers) {
        this.handlers = Collections.unmodifiableList(assembleHandlers(service, endpointHandlers));
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
        queue.add(new JobWork(job, handlers, queue, future));
        return future;
    }

    private static List<Handler> assembleHandlers(Service service, List<Handler> endpointHandlers) {
        List<Handler> handlers = new ArrayList<>(endpointHandlers);
        handlers.add(new HttpHandler());
        handlers.add(new ServerProtocolHandler());
        handlers.add(new OperationHandler(service));
        return handlers;
    }

    @Override
    public void execute(Runnable command) {
        queue.add(command);
    }

    private final class JobWork implements Runnable {
        private final Job job;
        private final Queue<Handler> queue;
        private final BlockingQueue<Runnable> workQueue;
        private final CompletableFuture<Job> signal;
        private final Deque<Handler> soFar;
        private State state = State.BEFORE;

        private JobWork(
            Job job,
            List<Handler> handlers,
            BlockingQueue<Runnable> workQueue,
            CompletableFuture<Job> signal
        ) {
            this.job = job;
            this.queue = new ArrayDeque<>(handlers);
            this.workQueue = workQueue;
            this.signal = signal;
            this.soFar = new ArrayDeque<>();
        }

        @Override
        public void run() {
            try {
                if ((job.isDone() || job.getFailure().isPresent()) && state == State.BEFORE) {
                    state = State.AFTER;
                }

                while (state == State.BEFORE) {
                    if (queue.isEmpty() || job.getFailure().isPresent()) {
                        state = State.AFTER;
                        break;
                    }
                    Handler handler = queue.poll();
                    soFar.push(handler);
                    CompletableFuture<Void> cf = handler.before(job);
                    if (!cf.isDone()) {
                        cf.whenComplete((e, t) -> {
                            if (t != null) {
                                job.setFailure(t);
                            }
                            DefaultOrchestratorImpl.this.queue.add(this);
                        });
                        break;
                    }
                    if (cf.isCompletedExceptionally()) {
                        cf.exceptionally(t -> {
                            job.setFailure(t);
                            return null;
                        });
                        state = State.AFTER;
                        break;
                    }
                }
                if (state == State.AFTER) {
                    while (!soFar.isEmpty()) {
                        Handler handler = soFar.pop();
                        CompletableFuture<Void> cf = handler.after(job);
                        if (!cf.isDone()) {
                            cf.whenComplete((e, t) -> {
                                if (t != null) {
                                    job.setFailure(t);
                                }
                                DefaultOrchestratorImpl.this.queue.add(this);
                            });
                            break;
                        }
                        if (cf.isCompletedExceptionally()) {
                            cf.exceptionally(t -> {
                                job.setFailure(t);
                                return null;
                            });
                        }
                    }
                    state = State.DONE;
                    signal.complete(job);
                }
            } catch (Exception e) {
                signal.completeExceptionally(e);
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
        private final BlockingQueue<Runnable> workQueue;

        private ConsumerTask(BlockingQueue<Runnable> workQueue) {
            this.workQueue = workQueue;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Runnable work = workQueue.take();
                    work.run();
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } catch (Throwable t) {
                LOGGER.log(System.Logger.Level.ERROR, "Orchestrator task threw an exception", t);
            }
        }
    }
}
