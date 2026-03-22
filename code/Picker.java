import java.util.ArrayList;
import java.util.Random;
// import java.util.concurrent.ConcurrentHashMap;
// import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.HashMap;
import java.util.List;

enum PickerStatus {PICKING, PICKED, WAITING}

public class Picker extends Worker {

    private final String id;
	private static int nextId = 1;
    private static int pickId = 1;
    private static Random RNG = null;

    // Internal State
    private PickerStatus status = PickerStatus.WAITING;
    // private static int nextPickId = 1;
	private int readyTick = 0;
    private int totalPickedBoxes = 0;
	
	private double PICKER_PROBABILITY;
    private TrolleyPool trolleyPool;
    private Trolley trolley = null;
    private int trolleyWaitStartTick = -1;

    // Environment
    private HashMap<String, Section> sections;
    private String category;
    private Section section;

    // Picking Trackers
    private int newPickId;
    private int pickWaitStartTick;
    private int sizeAfterCurrentPick;
  
    public Picker(HashMap<String, Section> sections, TrolleyPool trolleyPool, double pickerProbability, long seed) {
        this.id = "P" + nextId++;
        this.sections = sections;
        this.trolleyPool = trolleyPool;
        this.PICKER_PROBABILITY = pickerProbability;
        if (seed != 0)
            RNG = new Random(seed);
        else
            RNG = new Random();
    }

	// Needs to be synchronized to ensure the pick id's increment properly across threads
    public synchronized int nextPickId() {
        return pickId++;
    }

    public synchronized int getTotalPicks() {
        return pickId;
    }

    public int getTotalPickedBoxes() {
        return totalPickedBoxes;
    }

    // BEHAVIOUR HANDLING
	public void pickStart() {
        status = PickerStatus.PICKING;
        newPickId = nextPickId();

        // REPLACE LATER
        List<Section> sectionList = new ArrayList<>(sections.values());
        section = sectionList.get(RNG.nextInt(sectionList.size()));
        category = section.getCategory();
        // REPLACE LATER

        // int currentSectionSize = section.getSize();

        // this.newPickId = newPickId;
        
        pickAttempt();
	}

    public void pickAttempt() {
        
        if (trolley == null){
            if (trolleyWaitStartTick == -1) {
                trolleyWaitStartTick = Ticker.getCurrentTick();
            }

            trolley = trolleyPool.tryAcquireTrolley(this);
            if (trolley == null) return;

            int waited = Ticker.getCurrentTick() - trolleyWaitStartTick;
            Logging.logTrolleyEvent(this.id, "acquire_trolley", trolley.getId(), waited);
            trolleyWaitStartTick = -1;

            // Trolley acquired, start counting waiting time to pick
            pickWaitStartTick = Ticker.getCurrentTick();
            Logging.logPickerEvent(this.id, "pick_start", newPickId, category, section.getSize(), trolley.getId());
        }

        if (!section.tryPickerLock()) return;
        try {
            int newSectionSize = section.pickerRemoveBox();
            if (newSectionSize == -1) return; // Box remove failed

            sizeAfterCurrentPick = newSectionSize;
            totalPickedBoxes++;

            readyTick = Ticker.getCurrentTick() + 1;
            status = PickerStatus.PICKED;
            }
            finally {
                section.releasePickerLock();
            }
        }

    public void pickDone() {
        // - 1 to log the time the picker had to wait, not how long the pick took
        Logging.logPickerEvent(this.id, "pick_done", newPickId, category, Ticker.getCurrentTick() - pickWaitStartTick - 1, sizeAfterCurrentPick, trolley.getId());

        if (trolley != null){
            trolleyPool.releaseTrolley(this, trolley);
            Logging.logTrolleyEvent(this.id, "release_trolley", trolley.getId());
            trolley = null;
        }

        status = PickerStatus.WAITING;
    }

    public void advance(){
        if (!Ticker.getInstance().isRunning()) return;

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
                pickAttempt();
                break;
      
            case PICKED:
                pickDone();
                break;
        }
    }
}