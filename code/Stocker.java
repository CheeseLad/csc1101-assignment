import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.HashMap;
import java.util.ArrayList;

enum StockerStatus {MOVING, TAKING, STOCKING, WAITING}

public class Stocker extends Worker {
	private static final int INVENTORY_LIMIT = 10;

	private final String id;
	private static int nextId = 1;
	private ConcurrentHashMap<String, ConcurrentLinkedQueue<Box>> inventory = new ConcurrentHashMap<>();
	private int inventorySize = 0;
	private String position = "staging";
	private StockerStatus status = StockerStatus.WAITING;
	private int readyTick = 0;
	private StagingArea stagingArea;
	private HashMap<String, Section> sections;
	private String nextMoveTarget = null;

	// bookkeeping for current stocking session
	private int currentStockTarget = 0;  // how many we intended to stock this trip
	private int currentStocked = 0;      // how many we've already moved


	public Stocker(StagingArea stagingArea, HashMap<String, Section> sections) {
		this.stagingArea = stagingArea;
		this.sections = sections;
		this.id = "S" + nextId++;
	}

	// GETTERS
	public String getId(){
		return id;
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

	private String peekInventoryCategory(){
		for (String category: this.inventory.keySet()){
			if (!this.inventory.get(category).isEmpty()){
				return category;
			}
		}
		return null;
	}


	// BEHAVIOUR HANDLING
	public void take() {
		try {
			if (status != StockerStatus.TAKING) { // Not yet taking, try to acquire lock
				stagingArea.acquireForStocker();
				status = StockerStatus.TAKING;
			}

			if (stagingArea.isEmpty()) { // Cannot take anything, wait instead
				stagingArea.releaseForStocker();
				status = StockerStatus.WAITING;
				return;
			}

			// Fill up inventory as much as possible
			int space = INVENTORY_LIMIT - inventorySize;

			for (int i = 0; i < space && !stagingArea.isEmpty(); i++) {
				Box box = stagingArea.removeBox();
				addToInventory(box);
			}

			Ticker.logEventWithBoxes(id,"stocker_load",getInventoryItems(),true);

			stagingArea.releaseForStocker(); // Taking complete, release lock

			nextMoveTarget = peekInventoryCategory(); // Save where to move (so we can move there on next tick)

			readyTick = Ticker.getCurrentTick() + 1;
			status = StockerStatus.MOVING;

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public void stock(){
		Section currentSection = sections.get(position);

		if (status != StockerStatus.STOCKING) {

			currentSection.getStockerLock().lock();

			Ticker.logMessage(id,
				"event=stock_begin section=" + position + " amount=" + currentStockTarget);

			status = StockerStatus.STOCKING;
		}

		if (inventoryContainsCategory(position)){
			Box box = removeFromInventory(position);
			currentSection.addBox(box);
			readyTick = Ticker.getCurrentTick() + 1;
			currentStocked++;
		}
		else {
			currentSection.getStockerLock().unlock();
			String nextPosition = peekInventoryCategory();
			if (nextPosition == null) {
				moveTo("staging");
			}
			else {
				moveTo(nextPosition);
			}
		}
	}

	public void moveTo(String position){
		// if we were in the middle of stocking, emit end event before leaving
		if (this.status == StockerStatus.STOCKING) {
			Ticker.logMessage(id, "event=stock_end section=" + this.position +
				" stocked=" + currentStocked + " remaining_load=" + inventorySize);
		}
		String from = this.position;
		int load = this.inventorySize;
		Ticker.logMessage(id, "event=move from=" + from + " to=" + position + " load=" + load);
		// when heading to a new section (not staging) set up stock counters
		if (!position.equals("staging")){
			currentStockTarget = inventory.containsKey(position) ? this.inventory.get(position).size() : 0;
			currentStocked = 0;
		}
		this.status = StockerStatus.MOVING;
		this.position = position;
		readyTick = Ticker.getCurrentTick() + 10 + inventorySize;
	}

	// MAIN PER-TICK RUNNER
	public void advance(){

		int tick = Ticker.getCurrentTick();

		if (tick < readyTick){
			return;
		}

		switch(status){

			case WAITING:
				if(position.equals("staging"))
					take();
				else
					stock();
				break;

			case MOVING:
				if (nextMoveTarget != null) { // We have a target destination, move to it
					moveTo(nextMoveTarget);
					nextMoveTarget = null;
				}
				else if(position.equals("staging")) // Movement complete, check position
					take();
				else
					stock();
				break;

			case TAKING:
				take();
				break;

			case STOCKING:
				stock();
				break;
		}
	}
}