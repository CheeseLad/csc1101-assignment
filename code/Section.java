import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

public class Section {

	private String category;
	private ConcurrentLinkedQueue<Box> boxes = new ConcurrentLinkedQueue<>();
	private final ReentrantLock sectionStockerLock = new ReentrantLock(true);
	private final ReentrantLock sectionPickerLock = new ReentrantLock(true);

	public Section(String category){
		this.category = category;
	}

	public ReentrantLock getStockerLock(){
        return this.sectionStockerLock;
  }

	public void acquireForPicker() throws InterruptedException {  
        sectionPickerLock.lock();
  }

  public void releaseForPicker(){
        sectionPickerLock.unlock();
  }

	public int getSize() {
		return this.boxes.size();
	}

	public String getCategory() {
		return this.category;
	}

	public boolean isEmpty() {
		return this.boxes.isEmpty();
	}

	public void addBox(Box box) {
		if (box.getCategory().equals(this.category)){
			boxes.add(box);
		}
		else {
			System.out.println("[ERROR] Box category does not match section category.");
		}
	}

	public Box removeBox() {
		if (!this.isEmpty()) {
			return this.boxes.poll();
		}
		return null;
	}
}
