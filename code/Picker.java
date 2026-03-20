import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.HashMap;
import java.util.List;

public class Picker extends Worker {
	private final String id;
	private static int nextId = 1;
  private static int pickId = 1;
	private int readyTick = 0;
	private HashMap<String, Section> sections;
	private double PICKER_PROBABILITY;
  private Trolley trolley;

	private static Random RNG = null;

  private int totalPickedBoxes = 0;
  
  public Picker(HashMap<String, Section> sections, long seed, double pickerProbability, Trolley trolley) {
    this.sections = sections;
    this.id = "P" + nextId++;
    this.PICKER_PROBABILITY = pickerProbability;
    this.RNG = new Random(seed); // RNG for random section picking
    this.trolley = trolley;
  }

	// Needs to be synchronized to ensure the pick id's increment properly across threads
  public synchronized int nextPickId() {
    return pickId++;
  }

  public synchronized int getTotalPicks() {
    return pickId - 1; // because we start pickId at 1
  }

  // BEHAVIOUR HANDLING
	public void pick() {
    try {

      int newPickId = nextPickId();
			int waitedTicks = Ticker.getCurrentTick();

      // REPLACE LATER
      List<Section> sectionList = new ArrayList<>(sections.values());
      Section section = sectionList.get(RNG.nextInt(sectionList.size()));
      // REPLACE LATER

      int currentSectionSize = section.getSize();

      int trolleyId = trolley.acquireTrolley();
      Logging.logTrolleyEventWait(this.id, "acquire_trolley", trolleyId, Ticker.getCurrentTick() - waitedTicks);

      Logging.logPickerEvent(this.id, "pick_start", newPickId, section.getCategory(), trolleyId);

			Box box = section.removeBox();
      totalPickedBoxes++;
      readyTick = Ticker.getCurrentTick() + 1;

      Logging.logPickerEventWait(this.id, "pick_done", newPickId, section.getCategory(), Ticker.getCurrentTick() - waitedTicks, trolleyId);

      trolley.releaseTrolley();
      Logging.logTrolleyEventWait(this.id, "release_trolley", trolleyId, Ticker.getCurrentTick() - waitedTicks);

      return;
    } catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

  public int getTotalPickedBoxes() {
    return totalPickedBoxes;
  }

  // MAIN PER-TICK RUNNER
	public void advance(){

		int tick = Ticker.getCurrentTick();

    if (tick < readyTick){
			return;
		}

		if (RNG.nextFloat() <= PICKER_PROBABILITY) { // Perform a pick every 10 ticks on average
			pick();			
		}
	}
}