import java.util.concurrent.ConcurrentLinkedQueue;

public class StagingArea {

	private ConcurrentLinkedQueue<Box> boxes = new ConcurrentLinkedQueue<>();

	public Box[] getBoxes() {
		return this.boxes.toArray(new Box[boxes.size()]);
	}

	public boolean isEmpty() {
		return this.boxes.isEmpty();
	}

	public boolean addBox(Box box) {
		synchronized (this) {
			return this.boxes.add(box);
		}
	}

	public Box removeBox() {
		synchronized (this) {
			return this.boxes.poll();
		}
	}
}
