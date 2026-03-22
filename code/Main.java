// import java.util.concurrent.Executors;
// import java.util.concurrent.ExecutorService;
// import java.util.Random;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Main {
	public static void main(String[] args) {
		
		///// EXTRACTION OF config.properties /////
		// Source: https://www.browserstack.com/guide/config-file-in-java
		Properties config = new Properties();
		try (FileInputStream input = new FileInputStream("config.properties")) {
			config.load(input);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// See config.properties for detailed explanations of below config variables.
		
		///// CONFIGURATIONS /////
		
		// WAREHOUSE ENVIRONMENT 
		final int DELIVERERS_COUNT = Integer.parseInt(config.getProperty("DELIVERERS_COUNT", "1"));
		final int STOCKERS_COUNT = Integer.parseInt(config.getProperty("STOCKERS_COUNT", "1"));
		final int PICKERS_COUNT = Integer.parseInt(config.getProperty("PICKERS_COUNT", "1"));
		final String[] CATEGORIES = config.getProperty("CATEGORIES", "electronics,books,medicines,clothes,tools").split(",");

		final int TOTAL_THREAD_COUNT = DELIVERERS_COUNT + STOCKERS_COUNT + PICKERS_COUNT;

		// SIZE RESTRICTIONS
		final int DELIVERY_SIZE = Integer.parseInt(config.getProperty("DELIVERY_SIZE", "10"));
		final int TROLLEY_POOL_SIZE = Integer.parseInt(config.getProperty("TROLLEY_POOL_SIZE", "0"));
		final int SECTION_CAPACITY = Integer.parseInt(config.getProperty("SECTION_CAPACITY", "0"));
		final int INITIAL_SECTION_SIZES = Integer.parseInt(config.getProperty("INITIAL_SECTION_SIZES", "5"));
		
		// TICK CONTROL
		final int TICK_TIME_SIZE = Integer.parseInt(config.getProperty("TICK_TIME_SIZE", "50"));
		final int TOTAL_RUNTIME_TICKS = Integer.parseInt(config.getProperty("TOTAL_RUNTIME_TICKS", "1000"));

		// PROBABILITY CONTROL
		final long SEED = Long.parseLong(config.getProperty("SEED", "0")); // Seed for replicable behaviour
		final double DELIVERY_PROBABILITY = Double.parseDouble(config.getProperty("DELIVERY_PROBABILITY", "0.01"));
		final double PICKER_PROBABILITY = Double.parseDouble(config.getProperty("PICKER_PROBABILITY", "0.1"));

		final boolean VERBOSE = Boolean.parseBoolean(config.getProperty("VERBOSE", "false"));
		

		///// ENVIRONMENT PREPARATION /////
		
		// TROLLEY POOL
		TrolleyPool trolleyPool;

		if (TROLLEY_POOL_SIZE <= 0) {
			int defaultTrolleyPoolSize = (int) Math.floor((STOCKERS_COUNT + PICKERS_COUNT) / 2.0);
			trolleyPool = new TrolleyPool(defaultTrolleyPoolSize);
		} else {
			trolleyPool = new TrolleyPool(TROLLEY_POOL_SIZE);
		}

		// STAGING AREA
		StagingArea stagingArea = new StagingArea(); // Assuming single staging area, but theoretically extensible

		// CATEGORY SECTIONS
		HashMap<String, Section> sections = new HashMap<>(); // category -> section object

		for (String category : CATEGORIES) {
			// Generate sections from available categories
			Section section = new Section(category, SECTION_CAPACITY, VERBOSE);
			sections.putIfAbsent(category, section);

			// Per spec: You can assume each section of the warehouse begins with 5 boxes
			for (int i = 0; i < INITIAL_SECTION_SIZES; i++){
				try {
					section.addBox(new Box(category));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		// WORKERS (DELIVERERS, STOCKERS, PICKERS)
		ArrayList<Worker> workers = new ArrayList<>(); // All Deliverers, Stockers and Pickers

		// Generate workers, passing through config parameters as needed
		for (int i = 0; i < DELIVERERS_COUNT; i++){
			Deliverer newDeliverer = new Deliverer(CATEGORIES, stagingArea, SEED, DELIVERY_SIZE, DELIVERY_PROBABILITY);
			workers.add(newDeliverer);
		}

		for (int i = 0; i < STOCKERS_COUNT; i++){
			Stocker newStocker = new Stocker(stagingArea, sections, trolleyPool, SEED);
			workers.add(newStocker);
		}

		for (int i = 0; i < PICKERS_COUNT; i++){
			Picker newPicker = new Picker(sections, SEED, PICKER_PROBABILITY, trolleyPool);
			workers.add(newPicker);
		}

		// MAIN TICK HANDLER (TICKER)
		Ticker.initialise(TICK_TIME_SIZE, TOTAL_RUNTIME_TICKS, workers, TOTAL_THREAD_COUNT);
		
		// START SIMULATION
		Logging.logMessage("MAIN","starting simulation");
		Ticker.getInstance().start();

		while(Ticker.getInstance().isRunning()){
			Thread.yield(); // Yield reduced busy waiting costs
		}

		// POST-SIMULATION SUMMARY

		Logging.logMessage("MAIN","ending simulation");
		System.out.println();
		System.out.println("--- FINAL WAREHOUSE STATE ---");
		System.out.println();

		int totalDeliveries = 0;
		int totalSectionBoxes = 0;
		int leftoverInventory = 0;
		int totalPickedBoxes = 0;

		for (Worker worker : workers) {
			if (worker instanceof Deliverer) {
				totalDeliveries += ((Deliverer) worker).getTotalDeliveries();
			}
			else if (worker instanceof Stocker) {
				leftoverInventory += ((Stocker) worker).getInventorySize();
			}
			else if (worker instanceof Picker) {
				totalPickedBoxes += ((Picker) worker).getTotalPickedBoxes();
			}
		}

		System.out.println("Ending on day (starting from 0): " + Ticker.getCurrentDay());
		System.out.println("Total ticks: " + Ticker.getCurrentTick());
		System.out.println("Starting Stock: " + (CATEGORIES.length * INITIAL_SECTION_SIZES));
		System.out.println();
		System.out.println("Total Deliveries: " + totalDeliveries);
		System.out.println("Final Staging Area Size: " + stagingArea.getSize());
		System.out.println();
		System.out.println("Final Section Sizes:");
		for (String category : CATEGORIES) {
			int size = sections.get(category).getSize();
			totalSectionBoxes += size;
			System.out.println(category + ": " + size);
		}
		System.out.println();
		System.out.println("Total Boxes Left in Sections: " + totalSectionBoxes);
		System.out.println("Stockers Leftover Inventories: " + leftoverInventory);
		System.out.println("Total Picked Boxes (Approx. 0.1 per tick): " + totalPickedBoxes);
		System.out.println();
		System.out.println("Total Processed (Delivered/Stocked/Picked) Boxes: " + (totalSectionBoxes + totalPickedBoxes + leftoverInventory + stagingArea.getSize()));
		System.out.println();
		System.out.println("Total Processed should match (Total Picked) + (Total Left in Sections) + (Leftover Stocker Inventory) + (Staging Area Stock)");
		System.out.println("Total Processed should match (10 * Deliveries) + Starting Stock!");
	}
}