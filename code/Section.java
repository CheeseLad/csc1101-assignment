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

	public void addBox(Box box) {
		synchronized (this) {
			if (box.getCategory().equals(this.category)){
				boxes.add(box);
			}
			else {
				System.out.println("[ERROR] Box category does not match section category.");
			}
		}
	}

	public Box removeBox() {
		synchronized (this) {
			if (!this.isEmpty()) {
				return this.boxes.poll();
			}
			return null;
		}
	}
}
