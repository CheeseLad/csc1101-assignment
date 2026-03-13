import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.HashMap;
import java.util.List;

enum PickerStatus {PICKING, WAITING, START, DONE}

public class Picker extends Worker {

	private static final int INVENTORY_LIMIT = 10;

	private final String id;
	private static int nextId = 1;
  private int pickId = 1;
	private ConcurrentHashMap<String, ConcurrentLinkedQueue<Box>> inventory = new ConcurrentHashMap<>();
	private int inventorySize = 0;
	private String position = "sections";
	private PickerStatus status = PickerStatus.WAITING;
	private int readyTick = 0;
	private HashMap<String, Section> sections;
	private String nextMoveTarget = null;
  private Box currentBox;

	// bookkeeping for current stocking session
	private int currentPickTarget = 0;  // how many we intended to stock this trip
	//private int currentPicked = 0;      // how many we've already moved

  private static final long SEED = 12345L; // Seed for replicatable behaviour
	private static final Random RNG = new Random(SEED); // RNG for random section picking

  private int waitedTicks = 0; // For logging purposes, how many ticks we've been waiting for
  private int totalPickedBoxes = 0; // For logging purposes, how many boxes we've picked in total
  
  public Picker(HashMap<String, Section> sections) {
    this.sections = sections;
    this.id = "P" + nextId++;
    this.currentBox = null;
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
      this.status = PickerStatus.START;
      int randomCategory = RNG.nextInt(5);

      // REPLACE LATER
      List<Section> sectionList = new ArrayList<>(sections.values());
      Section section = sectionList.get(RNG.nextInt(sectionList.size()));
      // REPLACE LATER

			if (status != PickerStatus.PICKING) { // Not yet picking, try to acquire lock
				section.acquireForPicker();
        System.out.println(this.id + " acquired lock for section " + section.getCategory() + " to pick.");
				status = PickerStatus.PICKING;
			}

			if (section.getSize() == 0) { // Cannot take anything, wait instead
        this.waitedTicks = 0;
        System.out.println(this.id + " found no boxes to pick in section " + section.getCategory() + " and released lock.");
        status = PickerStatus.WAITING;
        section.releaseForPicker();
        readyTick = Ticker.getCurrentTick() + 500 / 5;
				return;
			}



      
      int currentSectionSize = section.getSize();
      int space = INVENTORY_LIMIT - inventorySize;

      /*if (section.getSize() == 0) {
        System.out.println(this.pickId + " found no boxes to pick in section " + section.getCategory());
        this.status = PickerStatus.WAITING;
        System.out.println(this.pickId + " finished picking from section " + section.getCategory() + ". Inventory size: " + inventorySize);
        pickId++;
        readyTick = Ticker.getCurrentTick() + 500 / 5;
        return;
      }*/

      Logging.logPickerEvent(this.id, "pick_start", pickId, section.getCategory());

      for (int i = 0; i < space && !section.isEmpty(); i++) {
				Box box = section.removeBox();
				addToInventory(box);
        //currentPicked++;
        totalPickedBoxes++;
        
        /*System.out.println(this.pickId + " picked a box of category " + box.getCategory() + " from section " + section.getCategory());*/
        
			} 
      System.out.println(this.id + " finished picking from section " + section.getCategory() + ". Current inventory size: " + inventorySize + ". Section size: (before/after) " + currentSectionSize +"/" + (section.getSize()));
      //readyTick = Ticker.getCurrentTick() + inventorySize;

      Logging.logPickerEvent(this.id, "pick_done", pickId, section.getCategory());
    
      pickId++;
      this.status = PickerStatus.WAITING;
      section.releaseForPicker();
      readyTick = Ticker.getCurrentTick() + 500 + inventorySize / 5;
      return;
    }catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}


  public void startPicking(int daily_limit, Random randgen, ArrayList<Section> sections, Util util, String threadName) {
    while (true) {
      daily_limit--;
        int randomCategory = randgen.nextInt(5);


        String currentSection = pickBoxFromSection(sections.get(randomCategory));

        if (currentSection != null) {
          util.printPick(140, threadName, "pick_start", daily_limit, currentSection);
        }
        if (daily_limit <= 0) {
          break;
        }
    }
  }

  public void attemptPick(Section section) {
    boolean picked = false;
    while (!picked) {
      continue; // Simulate waiting for a box to be available
    }
  }

  public String pickBoxFromSection(Section section) {
      //long programStart = System.currentTimeMillis();
      Box box = section.removeBox();
      if (box != null) {
          //System.out.printf("Time since program start: %d ms\n", System.currentTimeMillis() - programStart);
          this.currentBox = box;
          //long programEnd = System.currentTimeMillis();
          //System.out.printf("Time taken to pick box: %d ms\n", programEnd - programStart);
          return box.getCategory();
        } else {
        //System.out.println("No boxes available in section.");
        return null;
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
		}
	}
}