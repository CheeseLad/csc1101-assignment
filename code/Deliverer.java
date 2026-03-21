import java.util.Random;
import java.util.ArrayList;

public class Deliverer extends Worker {
	private static int nextId = 1;
	private static int totalDeliveries = 0;
	
	private int deliverySize;
	private double deliveryProbability;
	private Random RNG = null;
	private String[] categories;
	private StagingArea stagingArea;
	private final String id;

  	public Deliverer(String[] categories, StagingArea stagingArea, long seed, int deliverySize, double deliveryProbability) {
		this.categories = categories;
		this.stagingArea = stagingArea;
		this.id = "D" + nextId++;
		this.deliverySize = deliverySize;
		this.deliveryProbability = deliveryProbability;
		if (seed != 0)
			this.RNG = new Random(seed);
		else
			this.RNG = new Random(seed);
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
			for (int i = 0; i < deliverySize; i++){
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
		if (RNG.nextFloat() <= deliveryProbability){ // Peform a delivery every 100 ticks on average
			// Ticker.logMessage(id, "staging_stock=" + stagingArea.getSize());
			deliver();			
		}
  	}
}