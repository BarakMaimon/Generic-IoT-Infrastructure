import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.Semaphore;

class SemWaitableQueue<E> {
    private final Semaphore numOfElementsSemaphore;
    private  PriorityQueue<E> queue;
    private final Object accessesLock;

    public SemWaitableQueue(int capacity) {
        this(null,capacity);
    }
    public SemWaitableQueue(Comparator<E> comparator, int capacity) {
        this.numOfElementsSemaphore = new Semaphore(0);
        this.queue = new PriorityQueue<>(capacity,comparator);
        this.accessesLock = new Object();
    }

    public boolean enqueue(E element){
        boolean returnValue;
        synchronized (this.accessesLock) {
           returnValue = this.queue.add(element);
        }
        if (returnValue) {
            this.numOfElementsSemaphore.release();
        }
        return returnValue;
    }

    public E dequeue(){
        try {
            this.numOfElementsSemaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        synchronized (this.accessesLock) {
            return this.queue.poll();
        }
    }

    public boolean remove(E element){
        boolean returnValue;
        synchronized (this.accessesLock) {
            returnValue = this.queue.remove(element);
        }
        if(returnValue){
            try {
                this.numOfElementsSemaphore.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return returnValue;
    }

    public int size(){
        synchronized (this.accessesLock) {
            return this.queue.size();
        }
    }

    public E peek(){
        synchronized (this.accessesLock) {
            return this.queue.peek();
        }
    }

    public boolean isEmpty(){
        synchronized (this.accessesLock) {
            return this.queue.isEmpty();
        }
    }
}