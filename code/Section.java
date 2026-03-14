import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class Section {

	private String category;
	private ConcurrentLinkedQueue<Box> boxes = new ConcurrentLinkedQueue<>();

  private final ReentrantReadWriteLock sectionReadWriteLock = new ReentrantReadWriteLock(true);
  private final Lock readLock = sectionReadWriteLock.readLock();
  private final Lock writeLock = sectionReadWriteLock.writeLock();

  private final Condition notEmpty = writeLock.newCondition();
  private final Condition notFull = writeLock.newCondition();

	private final int capacity = 10;

	public Section(String category){
		this.category = category;
	}

	/* public ReentrantLock getStockerLock(){
        return this.sectionStockerLock;
  }

	public void acquireForPicker() throws InterruptedException {  
        sectionPickerLock.lock();
  }

  public void releaseForPicker(){
        sectionPickerLock.unlock();
  } */

	public int getSize() {
		return this.boxes.size();
	}

	public String getCategory() {
		return this.category;
	}

	public boolean isEmpty() {
		return this.boxes.isEmpty();
	}

	public void addBox(Box box) throws InterruptedException {

		writeLock.lock();
		System.out.println("Section " + this.category + " write lock acquired for adding box. Current size: " + this.getSize());
		try {
			while (this.getSize() >= capacity) {
				System.out.println("Section " + this.category + " is full, waiting for boxes to be removed.");
				notFull.await();
			}

		  if (box.getCategory().equals(this.category)){
				System.out.println("Section " + this.category + " is adding a box. Current size: " + this.getSize());
			  boxes.add(box);
				System.out.println("Section " + this.category + " added a box. New size: " + this.getSize());
				notEmpty.signalAll();
		  }

			else {
				System.out.println("Box category does not match section category.");
			}
		} finally {
			System.out.println("Section " + this.category + " write lock released after adding box. Current size: " + this.getSize());
			writeLock.unlock();
		}
}
	public Box removeBox() throws InterruptedException {

    readLock.lock();
		System.out.println("Section " + this.category + " read lock acquired for removing box. Current size: " + this.getSize());
		try {
			while (this.isEmpty()) {
				readLock.unlock();
				writeLock.lock();
				try {
					while (this.isEmpty()) {
						System.out.println("Section " + this.category + " is empty, waiting for boxes to be added.");
						notEmpty.await();
					}
				} finally {
					writeLock.unlock();
				}

				readLock.lock();
			}

			System.out.println("Section " + this.category + " is removing a box. Current size: " + this.getSize());
			return this.boxes.poll();
		} finally {
			System.out.println("Section " + this.category + " read lock released after removing box. Current size: " + this.getSize());
			readLock.unlock();
		}

		/*if (!this.isEmpty()) {
			return this.boxes.poll();
		}
		return null;*/
	}
}
