import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class StagingArea {

    private final ConcurrentLinkedQueue<Box> boxes = new ConcurrentLinkedQueue<>();

    private final ReentrantLock stagingAreaLock = new ReentrantLock(true);

    private final Condition boxesAvailable = stagingAreaLock.newCondition();
    private final Condition stagingFree = stagingAreaLock.newCondition();

    private boolean stockerPresent = false;

    public ReentrantLock getLock(){
        return stagingAreaLock;
    }

    public void addBox(Box box){
        boxes.add(box);
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

    public void signalBoxesAvailable(){
        boxesAvailable.signalAll();
    }

    public void acquireForStocker() throws InterruptedException {    
        stagingAreaLock.lock();
        while(stockerPresent){
            stagingFree.await();
        }
        stockerPresent = true;
    }

    public void releaseForStocker(){
        stockerPresent = false;
        stagingFree.signal();
        stagingAreaLock.unlock();
    }

    public void waitForBoxes() throws InterruptedException {
        while(boxes.isEmpty()){
            boxesAvailable.await();
        }
    }
}