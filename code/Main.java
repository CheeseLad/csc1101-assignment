// import java.util.concurrent.Executors;
// import java.util.concurrent.ExecutorService;
// import java.util.Random;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {
	public static void main(String[] args) {
		final int DELIVERERS_COUNT = 1;
		final int DELIVERY_SIZE = 10; // How many boxes per delivery?
		final int STOCKERS_COUNT = 3;
		final int PICKERS_COUNT = 3;
		final int THREAD_COUNT = DELIVERERS_COUNT + STOCKERS_COUNT + PICKERS_COUNT;
		final int TICK_DURATION = 50; // in milliseconds, how long a tick lasts
		final int TOTAL_RUNTIME_TICKS = 1000; // in ticks, how many ticks the programme executes
		final long SEED = 1232245L; // Seed for replicable behaviour

		// Adjust categories array to determine available sections
		String[] categories = {"electronics", "books", "medicines", "clothes", "tools"};

		ArrayList<Worker> workers = new ArrayList<>(); // All Deliverers, Stockers and Pickers
		StagingArea stagingArea = new StagingArea(); // Assuming single staging area
		HashMap<String, Section> sections = new HashMap<>(); // category -> section object

		// Generate sections from available categories
		for (String category : categories) {
			Section section = new Section(category);
			sections.putIfAbsent(category, section);
		}

		// Generate workers
		for (int i = 0; i < DELIVERERS_COUNT; i++){
			Deliverer newDeliverer = new Deliverer(categories, stagingArea, SEED, DELIVERY_SIZE);
			workers.add(newDeliverer);
		}

		for (int i = 0; i < STOCKERS_COUNT; i++){
			Stocker newStocker = new Stocker(stagingArea, sections);
			workers.add(newStocker);
		}

		for (int i = 0; i < PICKERS_COUNT; i++){
			Picker newPicker = new Picker(sections, TOTAL_RUNTIME_TICKS, SEED);
			workers.add(newPicker);
		}

		// Generate & start ticker
		Ticker.initialise(TICK_DURATION, TOTAL_RUNTIME_TICKS, workers, THREAD_COUNT);
		Logging.logMessage("MAIN","starting simulation");
		Ticker.getInstance().start();

		while(Ticker.getInstance().isRunning()){
			Thread.yield();
		}

		System.out.println("\n--- FINAL WAREHOUSE STATE ---");

		int totalDeliveries = 0;
		int totalSectionBoxes = 0;
		int leftoverInventory = 0;
		int totalPickedBoxes = 0;
		int totalPicks = 0;
		for (Worker worker : workers) {
			if (worker instanceof Deliverer) {
				totalDeliveries += ((Deliverer) worker).getTotalDeliveries();
			}
			else if (worker instanceof Stocker) {
				leftoverInventory += ((Stocker) worker).getInventorySize();
			}
			else if (worker instanceof Picker) {
				totalPickedBoxes += ((Picker) worker).getTotalPickedBoxes();
				totalPicks += ((Picker) worker).getTotalPicks();
			}
		}
		System.out.println("Total Deliveries: " + totalDeliveries);
		System.out.println("Staging Area: " + stagingArea.getSize());

		for (String category : categories) {
			int size = sections.get(category).getSize();
			totalSectionBoxes += size;
			System.out.println(category + ": " + size);
		}

		System.out.println("Stockers Combined Inventories: " + leftoverInventory);

		System.out.println("Total Processed Boxes: " + (totalSectionBoxes + leftoverInventory + stagingArea.getSize()));
		System.out.println("Total Picked Boxes: " + totalPickedBoxes);
		System.out.println("Total Picks: " + totalPicks);
		System.out.println("Total Processed should match 10 * Deliveries!");
	}
}