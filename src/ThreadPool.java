import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.sleep;

/*=========================================================================================*/
/*==================================== POOL =================================================*/
/*=========================================================================================*/
class ThreadPool implements Executor {
    private final AtomicInteger numOfThreads;
    private final SemWaitableQueue<Task<?>> pq;
    private final Semaphore pausedSemaphore;

//    private int NumOfThreadsToRelease = 0;
    private final AtomicBoolean isShutDown;
    private final int maxPriority = Priority.HIGH.value + 1;
    private final int minPriority = Priority.LOW.value - 1;
    private final AtomicBoolean isPause;

    public ThreadPool(int numOfThreads) {
        this.numOfThreads = new AtomicInteger(0);
        pq = new SemWaitableQueue<>(numOfThreads);
        this.pausedSemaphore = new Semaphore(0);
        this.isShutDown = new AtomicBoolean(false);
        this.isPause = new AtomicBoolean(false);
        createWorkingThreads(numOfThreads);
    }


    private <V> Future<V> submit(Callable<V> command, int priority) {
        if(this.isShutDown.get()){
            return null;
        }
        Task<V> taskToInsert = new Task<>(command, priority);
        if (!pq.enqueue(taskToInsert)) {
            throw new RuntimeException("Memory problem");
        }
        return taskToInsert.getFuture();
    }

    public <V> Future<V> submit(Callable<V> command, Priority priority) {
        return this.submit(command, priority.getValue());
    }

    public <V> Future<V> submit(Callable<V> command) {
        return this.submit(command, Priority.DEFAULT.getValue());
    }

    public <Void> Future<Void> submit(Runnable command, Priority priority) {
        return this.submit(command, priority, null);
    }

    public <V> Future<V> submit(Runnable command, Priority priority, V returnValue) {
        Callable<V> callable = () -> {
            command.run();
            return returnValue;
        };
        return this.submit(callable, priority.getValue());
    }

    @Override
    public void execute(Runnable command) {
        this.submit(command, Priority.DEFAULT, null);
    }

    public void setNumOfThreads(int numOfThreads) {
        int diff = numOfThreads - this.numOfThreads.get();

        createWorkingThreads(diff);
        killWorkingThreads((-diff),this.maxPriority);
    }

    public void pause() {
//        int oldNumOfThreadsToRelease = this.NumOfThreadsToRelease;
//        this.NumOfThreadsToRelease = this.numOfThreads.get();
        pauseThreads(this.numOfThreads.get() /*- oldNumOfThreadsToRelease*/);
        isPause.set(true);
    }

    public void resume() {
//        this.pausedSemaphore.release(this.NumOfThreadsToRelease);
//        this.NumOfThreadsToRelease = 0;
        this.pausedSemaphore.release(this.numOfThreads.get());
    }

    public void shutDown() {
        this.resume();
        killWorkingThreads(this.numOfThreads.get(),this.minPriority);
        this.isShutDown.set(true);
    }

    public void awaitTermination(long l, TimeUnit timeUnit) throws TimeoutException {
        long timeOut = System.currentTimeMillis() + timeUnit.toMillis(l);
        while (this.numOfThreads.get() != 0 && System.currentTimeMillis() < timeOut) {
            try {
                sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        if (this.numOfThreads.get() != 0) {
            throw new TimeoutException();
        }
    }

    public void awaitTermination() {
        while (this.numOfThreads.get() != 0) {
            try {
                sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }

    /*=========================================================================================*/
    /*----------------------------------Working Thread-----------------------------------------*/
    private class WorkingThread extends Thread {
        private boolean isStopped = false;
        {
            numOfThreads.incrementAndGet();
        }
        @Override
        public void run() {
            while (!isStopped) {
                try {
                    pq.dequeue().execute(Thread.currentThread());
                }catch (InterruptedException e){
                    boolean ignored = Thread.interrupted();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            numOfThreads.decrementAndGet();
            System.out.println("i got stop");
        }
    }
    /*=========================================================================================*/
    /*-------------------------------------Priority-------------------------------------------*/
    public enum Priority {
        LOW(1),
        DEFAULT(5),
        HIGH(10);

        private final int value;

        Priority(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /*=========================================================================================*/
    /*----------------------------------Task--------------------------------------------------*/
    private class Task<T> implements Comparable<Task<T>> {
        private final int priority;
        private final TaskFuture<T> future;

        public Task(Callable<T> callable, int priority) {
            this.priority = priority;
            this.future = new TaskFuture<>(callable,this);
        }

        public void execute(Thread thread) throws Exception {
            this.future.run(thread);
        }

        public TaskFuture<T> getFuture() {
            return this.future;
        }

        @Override
        public int compareTo(Task<T> task) {
            return task.priority - this.priority;
        }
    }

    /*=========================================================================================*/
    /*-----------------------------TaskFuture--------------------------------------------------*/
    private class TaskFuture<V> implements Future<V> {
        private V value;
        private final Task<V> task;
        private final Callable<V> callable;
        private Thread thread;
        private boolean isCancelled;
        private boolean isDone;
        ExecutionException executionException;

        public TaskFuture(Callable<V> callable, Task<V> task) {
            this.callable = callable;
            this.task = task;
        }

        private void run(Thread thread) throws Exception {
            if (!isCancelled) {
                 this.thread = thread;
                 try {
                     this.value = this.callable.call();
                 }catch (ExecutionException e){
                     this.executionException = e;
                 }
                isDone = true;
            }
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (isDone || isCancelled) {
                return false;
            }else if(pq.remove(task)){
                this.isCancelled = true;
            }else if(mayInterruptIfRunning){
                this.thread.interrupt();
                this.isCancelled = true;
            }else {
                this.isCancelled = true;
            }

            return true;
        }

        @Override
        public boolean isCancelled() {
            return this.isCancelled;
        }

        @Override
        public boolean isDone() {
            return this.isDone;
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            if (this.isCancelled) {
                throw new CancellationException();
            }
            while (!this.isDone()) {
                sleep(10);
            }
            if (this.executionException != null){
                throw executionException;
            }
            return this.value;
        }

        @Override
        public V get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
            if (this.isCancelled) {
                throw new CancellationException();
            }
            long timeOut = timeUnit.toMillis(l) - System.currentTimeMillis();
            while (!this.isDone() && System.currentTimeMillis() < timeOut) {
                sleep(10);
            }
            if (this.executionException != null){
                throw executionException;
            }
            if (!this.isDone()) {
                throw new TimeoutException();
            }

            return this.value;
        }
    }
    /*=========================================================================================*/
    /*----------------------------------- Private ---------------------------------------------*/
    private void createWorkingThreads(int numOfThreads){
        if (isPause.get()) {
            pauseThreads(numOfThreads);
        }
        for (int i = 0; i < numOfThreads; ++i) {
            new WorkingThread().start();
        }
    }
    private void killWorkingThreads(int numOfThreads, int priority){
        for (int i = 0; i < numOfThreads; ++i) {
            this.submit(() -> {
                ((WorkingThread)(Thread.currentThread())).isStopped = true;
                return null;
            }, priority);
        }
    }

    private void pauseThreads(int numOfThreadsToPause){
        for (int i = 0; i < numOfThreadsToPause; ++i) {
            this.submit(() -> {
                try {
                    this.pausedSemaphore.acquire();
                    return null;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }, this.maxPriority);
        }
    }

}
