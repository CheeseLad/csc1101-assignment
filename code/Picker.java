import java.util.ArrayList;
import java.util.Random;
// import java.util.concurrent.ConcurrentHashMap;
// import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.HashMap;
import java.util.List;

enum PickerStatus {MOVING, PICKING, WAITING}

public class Picker extends Worker {
  private PickerStatus status = PickerStatus.WAITING;
	private final String id;
	private static int nextId = 1;
  private static int pickId = 1;
	private int readyTick = 0;
	private HashMap<String, Section> sections;
	private double PICKER_PROBABILITY;
  private TrolleyPool trolleyPool;
  private Trolley trolley = null;
  private int trolleyWaitStartTick = -1;

  private int newPickId;
  private String category;
  private int waitedTicks;

	private Random RNG = null;

  private int totalPickedBoxes = 0;
  
  public Picker(HashMap<String, Section> sections, long seed, double pickerProbability, TrolleyPool trolleyPool) {
    this.sections = sections;
    this.id = "P" + nextId++;
    this.PICKER_PROBABILITY = pickerProbability;
    this.RNG = new Random(seed); // RNG for random section picking
    this.trolleyPool = trolleyPool;
  }

	// Needs to be synchronized to ensure the pick id's increment properly across threads
  public synchronized int nextPickId() {
    return pickId++;
  }

  public synchronized int getTotalPicks() {
    return pickId - 1; // because we start pickId at 1
  }

  // BEHAVIOUR HANDLING
	public void pickStart() {
    try {

      status = PickerStatus.PICKING;
      int newPickId = nextPickId();


      // REPLACE LATER
      List<Section> sectionList = new ArrayList<>(sections.values());
      Section section = sectionList.get(RNG.nextInt(sectionList.size()));
      // REPLACE LATER

      // int currentSectionSize = section.getSize();

      if (trolley == null){
        if (trolleyWaitStartTick == -1) {
          trolleyWaitStartTick = Ticker.getCurrentTick();
        }

        trolley = trolleyPool.tryAcquireTrolley(this);
        if (trolley == null) return;

        int waited = Ticker.getCurrentTick() - trolleyWaitStartTick;
        Logging.logTrolleyEvent(this.id, "acquire_trolley", trolley.getId(), waited);
        trolleyWaitStartTick = -1;
      }

      this.newPickId = newPickId;
			int waitedTicks = Ticker.getCurrentTick();
      this.waitedTicks = waitedTicks;
      String category = section.getCategory();
      this.category = category;
      Logging.logPickerEvent(this.id, "pick_start", newPickId, category, trolley.getId());

			section.removeBox();
      totalPickedBoxes++;
     
      return;
    } catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

  public void pickDone() {
      readyTick = Ticker.getCurrentTick() + 1;
      status = PickerStatus.WAITING;

      // - 1 to log the time the picker had to wait, not how long the pick took
      Logging.logPickerEvent(this.id, "pick_done", newPickId, category, Ticker.getCurrentTick() - waitedTicks -1, trolley.getId());

      if (trolley != null){
        trolleyPool.releaseTrolley(this, trolley);
        Logging.logTrolleyEvent(this.id, "release_trolley", trolley.getId());
        trolley = null;
      }

      return;
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

    switch(status){

			case WAITING:
				if (RNG.nextFloat() <= PICKER_PROBABILITY) { // Perform a pick every 10 ticks on average
          pickStart();			
        }
				break;

      case PICKING:
        pickDone();
        break;

		}
	}
}