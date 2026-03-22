import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class StagingArea {

    private final ConcurrentLinkedQueue<Box> boxes = new ConcurrentLinkedQueue<>();

    private final ReentrantLock stagingAreaLock = new ReentrantLock(true);

    private final Condition stagingFree = stagingAreaLock.newCondition();
    private volatile boolean delivererWaiting = false;

    private boolean stockerPresent = false;

    public void addBox(Box box){
        boxes.add(box);
    }

    public Box removeBox(){
        return boxes.poll();
    }

    public Box removeBox(String category){
        for (Box box: boxes){
            if (box.getCategory().equals(category)) {
                if (boxes.remove(box)) {
                    return box;
                }
            }
        }
        return null;
    }

    public boolean isEmpty(){
        return boxes.isEmpty();
    }

    public int getSize(){
        return boxes.size();
    }

    public ConcurrentLinkedQueue<Box> getBoxes(){
        return boxes;
    }

    public boolean tryAcquireStagingLock() {
        if (!stagingAreaLock.tryLock()) return false;

        while(stockerPresent){
            stagingAreaLock.unlock();
            return false;
        }
        stockerPresent = true;
        return true;
    }

    public void releaseStagingLock(){
        stockerPresent = false;
        stagingFree.signal();
        stagingAreaLock.unlock();
    }

    public void setDelivererWaiting(boolean waiting) {
        delivererWaiting = waiting;
    }

    public boolean isDelivererWaiting() {
        return delivererWaiting;
    }
}