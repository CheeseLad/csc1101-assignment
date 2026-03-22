import java.util.Random;
import java.util.ArrayList;

enum DelivererStatus {DELIVERING, WAITING}

public class Deliverer extends Worker {
	private static int nextId = 1;
	private static int totalDeliveries = 0;
	private DelivererStatus status = DelivererStatus.WAITING;

	private int deliverySize;
	private double deliveryProbability;
	private Random RNG = null;
	private String[] categories;
	private final int categoriesCount;
	private StagingArea stagingArea;
	private final String id;

  	public Deliverer(String[] categories, StagingArea stagingArea, long seed, int deliverySize, double deliveryProbability) {
		this.categories = categories;
		this.categoriesCount = categories.length;
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

	public void deliverAttempt(){
		// Locking ensures atomic delivery of 10 boxes at once (despite thread-safe queue)
		stagingArea.setDelivererWaiting(true);

		if (!stagingArea.tryAcquireStagingLock()) return;

		ArrayList<Box> delivered = new ArrayList<>();
		
		try {
			for (int i = 0; i < deliverySize; i++){
				int randomIndex = RNG.nextInt(categoriesCount);
				Box newBox = new Box(categories[randomIndex]);
				stagingArea.addBox(newBox);
				delivered.add(newBox);
			}
			totalDeliveries++;

			// log the arrival event with counts of delivered boxes
			Logging.logEventWithBoxes(id, "delivery_arrived", delivered, false);
			
			status = DelivererStatus.WAITING;
		}
		finally {
			stagingArea.setDelivererWaiting(false);
			stagingArea.releaseStagingLock();
		}
	}

  	public void advance(){
		if (!Ticker.getInstance().isRunning()) return;

		switch (status) {
			
			case WAITING:
				// Peform a delivery every 100 ticks on average (for 0.01 deliveryProbability)
				if (RNG.nextFloat() <= deliveryProbability) {
					status = DelivererStatus.DELIVERING;
					deliverAttempt();
				}
				break;
			
			case DELIVERING:
				deliverAttempt();
				break;
		}
		
  	}
}