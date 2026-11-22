import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Section {

    private String category;
    public int maxCapacity;
	private ConcurrentLinkedQueue<Box> boxes = new ConcurrentLinkedQueue<>();
    private final ReentrantReadWriteLock sectionLock = new ReentrantReadWriteLock(true);

	public Section(String category, int capacity) {
		this.category = category;
		this.maxCapacity = capacity;
	}

	public int getSize() {
		return boxes.size();
	}

	public String getCategory() {
		return category;
	}

	public boolean isEmpty() {
        return boxes.isEmpty();
	}

    public boolean isFull() {
		return boxes.size() == maxCapacity;

    }

    public synchronized boolean tryStockerLock() {
        return sectionLock.writeLock().tryLock();
    }

    public void releaseStockerLock() {
        sectionLock.writeLock().unlock();
    }

    public boolean stockerTryAddBox(Box box) {
        if (boxes.size() >= maxCapacity) return false;

        if (box.getCategory().equals(category)) {
            return boxes.add(box);
        }
        return false;
    }

    public boolean tryPickerLock() {
        return sectionLock.readLock().tryLock();
    }

    public void releasePickerLock() {
        sectionLock.readLock().unlock();
    }

    public int pickerRemoveBox() {
        if (boxes.isEmpty()) return -1;

        Box box = boxes.poll();
        if (box == null) return -1;
        return boxes.size();
    }
}