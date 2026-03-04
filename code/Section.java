import java.util.concurrent.ConcurrentLinkedQueue;

public class Section {

	private String category;
	private ConcurrentLinkedQueue<Box> boxes = new ConcurrentLinkedQueue<>();

	public Section(String category){
		this.category = category;
	}

	public Box[] getBoxes() {
		return this.boxes.toArray(new Box[boxes.size()]);
	}

	public String getCategory() {
		return this.category;
	}

	public boolean isEmpty() {
		return this.boxes.isEmpty();
	}

	/**
	 * Add a box to this section. Validates that the box's category matches.
	 * Synchronized to protect the category check + add compound operation.
	 */
	public void addBox(Box box) {
		synchronized (this) {
			if (box.getCategory().equals(this.category)){
				boxes.add(box);
			}
			else {
				Ticker.logMessage("SECTION", "[ERROR] Box category does not match section category.");
			}
		}
	}

	/**
	 * Remove and return a box from this section (or null if empty).
	 * ConcurrentLinkedQueue.poll() is already atomic.
	 */
	public Box removeBox() {
		return this.boxes.poll();
	}
}
