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
		// Source: https://www.browserstack.com/guide/config-file-in-java
		Properties config = new Properties();
		try (FileInputStream input = new FileInputStream("config.properties")) {
			config.load(input);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		final int DELIVERERS_COUNT = Integer.parseInt(config.getProperty("DELIVERERS_COUNT"));
		final int DELIVERY_SIZE = Integer.parseInt(config.getProperty("DELIVERY_SIZE")); // How many boxes per delivery?
		final int STOCKERS_COUNT = Integer.parseInt(config.getProperty("STOCKERS_COUNT"));
		final int PICKERS_COUNT = Integer.parseInt(config.getProperty("PICKERS_COUNT"));
		final int THREAD_COUNT = DELIVERERS_COUNT + STOCKERS_COUNT + PICKERS_COUNT;
		final int TICK_TIME_SIZE = Integer.parseInt(config.getProperty("TICK_TIME_SIZE")); // in milliseconds, how long a tick lasts
		final int TOTAL_RUNTIME_TICKS = Integer.parseInt(config.getProperty("TOTAL_RUNTIME_TICKS")); // in ticks, how many ticks the programme executes
		final int BEGINNING_BOXES_PER_SECTION = Integer.parseInt(config.getProperty("BEGINNING_BOXES_PER_SECTION")); // How many boxes each section starts with
		final int SECTION_CAPACITY = Integer.parseInt(config.getProperty("SECTION_CAPACITY")); // How many boxes can each section hold at once
		final long SEED = Long.parseLong(config.getProperty("SEED")); // Seed for replicable behaviour

		// Probability Control

		final double DELIVERY_PROBABILITY = Double.parseDouble(config.getProperty("DELIVERY_PROBABILITY")); // Chance a deliverer attempts delivery each tick
		final double PICKER_PROBABILITY = Double.parseDouble(config.getProperty("PICKER_PROBABILITY")); // Chance a picker attempts to pick each tick

		final boolean VERBOSE = Boolean.parseBoolean(config.getProperty("VERBOSE")); // Extra logging for debugging

		String stringK = config.getProperty("K");
		int K;

		if (stringK == null || stringK.equals("")) {
			// Default K value if not provided in config
			K = (int) Math.floor((STOCKERS_COUNT + PICKERS_COUNT) / 2.0);
		} else {
			K = Integer.parseInt(stringK);
		}

		// Adjust categories array to determine available sections
		String categoriesString = config.getProperty("CATEGORIES");
		String[] categories = categoriesString.split(",");

		ArrayList<Worker> workers = new ArrayList<>(); // All Deliverers, Stockers and Pickers
		StagingArea stagingArea = new StagingArea(); // Assuming single staging area
		HashMap<String, Section> sections = new HashMap<>(); // category -> section object
		Trolley trolley = new Trolley(K); // Assuming single pool of trollies

		// Generate sections from available categories
		for (String category : categories) {
			Section section = new Section(category, SECTION_CAPACITY, VERBOSE);
			sections.putIfAbsent(category, section);

			// Per spec: You can assume each section of the warehouse begins with 5 boxes
			for (int i = 0; i < BEGINNING_BOXES_PER_SECTION; i++){
				try {
					section.addBox(new Box(category));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		// Generate workers, passing through config parameters as needed
		for (int i = 0; i < DELIVERERS_COUNT; i++){
			Deliverer newDeliverer = new Deliverer(categories, stagingArea, SEED, DELIVERY_SIZE, DELIVERY_PROBABILITY);
			workers.add(newDeliverer);
		}

		for (int i = 0; i < STOCKERS_COUNT; i++){
			Stocker newStocker = new Stocker(stagingArea, sections);
			workers.add(newStocker);
		}

		for (int i = 0; i < PICKERS_COUNT; i++){
			Picker newPicker = new Picker(sections, SEED, PICKER_PROBABILITY, trolley);
			workers.add(newPicker);
		}

		// Generate & start ticker
		Ticker.initialise(TICK_TIME_SIZE, TOTAL_RUNTIME_TICKS, workers, THREAD_COUNT);
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

		System.out.println("Starting Stock: " + (categories.length * BEGINNING_BOXES_PER_SECTION));
		System.out.println("Total Deliveries: " + totalDeliveries);
		System.out.println("Staging Area: " + stagingArea.getSize());

		for (String category : categories) {
			int size = sections.get(category).getSize();
			totalSectionBoxes += size;
			System.out.println(category + ": " + size);
		}

		System.out.println("Stockers Combined Inventories: " + leftoverInventory);

		System.out.println("Total Processed (Stocked/Picked) Boxes: " + (totalSectionBoxes + totalPickedBoxes + leftoverInventory + stagingArea.getSize()));
		System.out.println("Total Picked Boxes (For 1000 ticks = Around 100): " + totalPickedBoxes);
		System.out.println("Total Boxes Left in Sections: " + totalSectionBoxes);
		System.out.println("Total Processed should match (Total Picked) + (Total Left in Sections) + (Leftover Stocker Inventory) + (Staging Area Stock)");
		System.out.println("Total Processed should match (10 * Deliveries) + Starting Stock!");
	}
}