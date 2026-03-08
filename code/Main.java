// import java.util.concurrent.Executors;
// import java.util.concurrent.ExecutorService;
// import java.util.Random;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {
	public static void main(String[] args) {
		final int DELIVERERS_COUNT = 1;
		final int STOCKERS_COUNT = 3;
		final int PICKERS_COUNT = 0;
		final int THREAD_COUNT = DELIVERERS_COUNT + STOCKERS_COUNT + PICKERS_COUNT;
		final int TICK_DURATION = 50; // in miliseconds, how long a tick lasts
		final int TOTAL_RUNTIME_TICKS = 1000; // in ticks, how many ticks the programme executes

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
			Deliverer newDeliverer = new Deliverer(categories, stagingArea);
			workers.add(newDeliverer);
		}

		for (int i = 0; i < STOCKERS_COUNT; i++){
			Stocker newStocker = new Stocker(stagingArea, sections);
			workers.add(newStocker);
		}

		for (int i = 0; i < PICKERS_COUNT; i++){
			// Deliverer newPicker = new Picker();
			// workers.add(newPicker);
		}

		// Generate & start ticker
		Ticker.initialise(TICK_DURATION, TOTAL_RUNTIME_TICKS, workers, THREAD_COUNT);
		Logging.logMessage("MAIN","starting simulation");
		Ticker.getInstance().start();
	}
}