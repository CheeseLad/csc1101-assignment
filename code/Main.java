import java.util.ArrayList;
import java.util.HashMap;

public class Main {
	public static void main(String[] args) {
		final int DELIVERERS_COUNT = 1;
		final int STOCKERS_COUNT = 2;
		final int PICKERS_COUNT = 0;  // not yet implemented
		final int TICK_DURATION = 50; // in miliseconds, how long a tick lasts
		final int TOTAL_RUNTIME_DURATION = 300; // in ticks, how many ticks the programme executes
		
		final int THREAD_POOL_SIZE = 4;  // limited thread pool, regardless of worker count

		String[] categories = {"electronics", "books", "medicines", "clothes", "tools"};
		ArrayList<Worker> workers = new ArrayList<>();

		StagingArea stagingArea = new StagingArea();
		HashMap<String, Section> sections = new HashMap<>();

		for (String category : categories) {
			Section section = new Section(category);
			sections.putIfAbsent(category, section);
		}

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

		Ticker ticker = new Ticker(TICK_DURATION, TOTAL_RUNTIME_DURATION, workers, THREAD_POOL_SIZE);

		// quick demonstration of logging helpers; the thread id can be
		// any identifier you prefer (e.g. "MAIN" or worker-specific)
		Ticker.logMessage("MAIN","starting simulation");

		ticker.start();

		while (ticker.isRunning());
	}
}