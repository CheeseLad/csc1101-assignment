import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class StagingArea {

    private final ConcurrentLinkedQueue<Box> boxes = new ConcurrentLinkedQueue<>();

    private final ReentrantLock lock = new ReentrantLock(true);

    private final Condition boxesAvailable = lock.newCondition();
    private final Condition stagingFree = lock.newCondition();

    private boolean stockerPresent = false;

    public ReentrantLock getLock(){
        return lock;
    }

    public void addBox(Box box){
        lock.lock();
        try{
            boxes.add(box);
            boxesAvailable.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public Box removeBox(){
        return boxes.poll();
    }

    public boolean isEmpty(){
        return boxes.isEmpty();
    }

    public int getSize(){
        return boxes.size();
    }

    public void acquireForStocker() throws InterruptedException {
        lock.lock();
        while(stockerPresent){
            stagingFree.await();
        }
        stockerPresent = true;
    }

    public void releaseForStocker(){
        stockerPresent = false;
        stagingFree.signal();
        lock.unlock();
    }

    public void waitForBoxes() throws InterruptedException {
        while(boxes.isEmpty()){
            boxesAvailable.await();
        }
    }
}