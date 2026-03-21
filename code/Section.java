import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
// import java.util.concurrent.locks.Lock;

public class Section {

    public static int CAPACITY;
	private String category;
    private boolean verbose;
	private ConcurrentLinkedQueue<Box> boxes = new ConcurrentLinkedQueue<>();

    private final ReentrantLock sectionLock = new ReentrantLock(true);
    private final Condition notEmpty = sectionLock.newCondition();
    private final Condition notFull = sectionLock.newCondition();

	public Section(String category, int capacity, boolean verbose) {
		this.category = category;
		CAPACITY = capacity;
		this.verbose = verbose;
	}

	public int getSize() {
		sectionLock.lock();
		try {
			return this.boxes.size();
		} finally {
			sectionLock.unlock();
		}
	}

	public String getCategory() {
		return this.category;
	}

	public boolean isEmpty() {
		sectionLock.lock();
		try {
			return this.boxes.isEmpty();
		} finally {
			sectionLock.unlock();
		}
	}

	public void stockerDoneStocking() {
		sectionLock.lock();
		try {
				notEmpty.signalAll(); // Notify pickers that new boxes are available
		} finally {
				sectionLock.unlock();
		}
	}

    public void addBox(Box box) throws InterruptedException {
        sectionLock.lock();
        try {
            while (boxes.size() >= CAPACITY) {
                if (verbose) {
                    System.out.println("Section " + category + " is full, waiting...");
                }
                notFull.await();
            }

            if (box.getCategory().equals(category)) {
                boxes.add(box);
                if (verbose) {
                    System.out.println("Section " + category + " added a box. New size: " + boxes.size());
                }
                //notEmpty.signalAll();
            } else {
                if (verbose) {
                    System.out.println("Box category does not match section category.");
                }
            }
        } finally {
            sectionLock.unlock();
        }
    }

    public boolean tryAddBox(Box box) {
        sectionLock.lock();
        try {
            if (boxes.size() >= CAPACITY) return false;

            if (box.getCategory().equals(category)) {
                boxes.add(box);
                if (verbose) {
                    System.out.println("Section " + category + " added a box. New size: " + boxes.size());
                }
                return true;
            }
            return false;
        } finally {
            sectionLock.unlock();
        }
    }

    public Box removeBox() throws InterruptedException {
        sectionLock.lock();
        try {
            while (boxes.isEmpty()) {
                if (verbose) {
                    System.out.println("Section " + category + " is empty, waiting...");
                }
                notEmpty.await();
            }
            Box box = boxes.poll();
            if (verbose) {
                System.out.println("Section " + category + " removed a box. New size: " + boxes.size());
            }
            notFull.signalAll();
            return box;
        } finally {
            sectionLock.unlock();
        }
    }
}