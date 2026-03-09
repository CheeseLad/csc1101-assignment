import java.util.Random;
import java.util.ArrayList;

public class Deliverer extends Worker {
	private static final int DELIVERY_SIZE = 10; // How many boxes per delivery?
	private static final long SEED = 12345L; // Seed for replicatable behaviour
	private static final Random RNG = new Random(SEED); // RNG for box categories per delivery
	private String[] categories;
	private StagingArea stagingArea;
	private static int nextId = 1;
	private final String id;
	private static int totalDeliveries = 0;

  	public Deliverer(String[] categories, StagingArea stagingArea) {
		this.categories = categories;
		this.stagingArea = stagingArea;
		this.id = "D" + nextId++;
	}

	public String getId() {
		return id;
	}

	public int getTotalDeliveries() {
		return totalDeliveries;
	}

	public void deliver(){
		int amountOfChoices = categories.length;
		ArrayList<Box> delivered = new ArrayList<>();
		stagingArea.getLock().lock(); // Ensure atomic delivery of 10 boxes at once (despite thread-safe queue)
		try {
			for (int i = 0; i < DELIVERY_SIZE; i++){
				int randomIndex = RNG.nextInt(amountOfChoices);
				Box newBox = new Box(categories[randomIndex]);
				stagingArea.addBox(newBox);
				delivered.add(newBox);
			}
			totalDeliveries++;

			// log the arrival event with counts of delivered boxes
			Logging.logEventWithBoxes(id, "delivery_arrived", delivered, false);
		}
		finally {
			stagingArea.getLock().unlock();
		}
	}

  	public void advance(){
		if (RNG.nextFloat() <= 0.01){ // Peform a delivery every 100 ticks on average
			// Ticker.logMessage(id, "staging_stock=" + stagingArea.getSize());
			deliver();			
		}
  	}
}