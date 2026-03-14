import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.HashMap;
import java.util.List;

enum PickerStatus {PICKING, WAITING, DONE}

public class Picker extends Worker {

	private static final int INVENTORY_LIMIT = 10;

	private final String id;
	private static int nextId = 1;
  private static int pickId = 1;
	private ConcurrentHashMap<String, ConcurrentLinkedQueue<Box>> inventory = new ConcurrentHashMap<>();
	private int inventorySize = 0;
	private String position = "sections";
	private PickerStatus status = PickerStatus.WAITING;
	private int readyTick = 0;
	private HashMap<String, Section> sections;
	private String nextMoveTarget = null;
  private Box currentBox;
  private static int totalRuntimeTicks;

	// bookkeeping for current stocking session
	private int currentPickTarget = 0;  // how many we intended to stock this trip
	//private int currentPicked = 0;      // how many we've already moved

	private static Random RNG = null;

  private int totalPickedBoxes = 0; // For logging purposes, how many boxes we've picked in total
  
  public Picker(HashMap<String, Section> sections, int totalRuntimeTicks, long seed) {
    this.sections = sections;
    this.id = "P" + nextId++;
    this.currentBox = null;
    this.totalRuntimeTicks = totalRuntimeTicks;
    this.RNG = new Random(seed); // RNG for random section picking
  }

  public synchronized int nextPickId() {
    return pickId++;
  }

  public synchronized int getTotalPicks() {
    return pickId - 1;
  }

  // GETTERS
	public String getId(){
		return id;
	}

	public int getInventorySize(){
		return inventorySize;
	}

	private ArrayList<Box> getInventoryItems(){
		ArrayList<Box> items = new ArrayList<>();
		for (ConcurrentLinkedQueue<Box> queue: inventory.values()){
			items.addAll(queue);
		}
		return items;
	}

	// INVENTORY HANDLING
	private void addToInventory(Box box){
		inventory.putIfAbsent(box.getCategory(), new ConcurrentLinkedQueue<>());
		inventory.get(box.getCategory()).add(box);
		inventorySize++;
	}

	private Box removeFromInventory(String category){
		Box box = inventory.get(category).poll();
		if (box != null) inventorySize--;
		return box;
	}

	private boolean inventoryContainsCategory(String category){
		return inventory.containsKey(category) && !inventory.get(category).isEmpty();
	}

  // BEHAVIOUR HANDLING
	public void pick() {
    try {

      int randomCategory = RNG.nextInt(5);
      int space = INVENTORY_LIMIT - inventorySize;
      int newPickId = nextPickId();

      // REPLACE LATER
      List<Section> sectionList = new ArrayList<>(sections.values());
      Section section = sectionList.get(RNG.nextInt(sectionList.size()));
      // REPLACE LATER

      int currentSectionSize = section.getSize();

			if (status != PickerStatus.PICKING) { // Not yet picking, try to acquire lock
				section.acquireForPicker();
        System.out.println(this.id + " acquired lock for section " + section.getCategory() + " to pick.");
				status = PickerStatus.PICKING;
			}

			if (currentSectionSize == 0) { // Cannot take anything, wait instead
        section.releaseForPicker();
        System.out.println(this.id + " found no boxes to pick in section " + section.getCategory() + " and released lock.");
        readyTick = Ticker.getCurrentTick() + totalRuntimeTicks / 5;
        status = PickerStatus.WAITING;
				return;
			}

      Logging.logPickerEvent(this.id, "pick_start", newPickId, section.getCategory());

      for (int i = 0; i < space && !section.isEmpty(); i++) {
				Box box = section.removeBox();
				addToInventory(box);
        totalPickedBoxes++;
			} 

      System.out.println(this.id + " finished picking from section " + section.getCategory() + ". Current inventory size: " + inventorySize + ". Section size: (before/after) " + currentSectionSize +"/" + (section.getSize()));

      Logging.logPickerEvent(this.id, "pick_done", newPickId, section.getCategory());
    
      readyTick = Ticker.getCurrentTick() + totalRuntimeTicks / 5;
      section.releaseForPicker();
      this.status = PickerStatus.WAITING;
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

		switch(status){

			case WAITING:
				pick();
        break;
      case PICKING:
        break;
		}
	}
}