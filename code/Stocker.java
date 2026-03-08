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
	private int timer = 0;
	private StagingArea stagingArea;
	private HashMap<String, Section> sections;

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
	public void take(){
		try {
			if (status != StockerStatus.TAKING) {
				stagingArea.acquireForStocker();
				status = StockerStatus.TAKING; 
			}

			stagingArea.waitForBoxes();

			if (!stagingArea.isEmpty() && inventorySize < INVENTORY_LIMIT) {
				int space = INVENTORY_LIMIT - inventorySize;
				for (int i = 0; i < space && !stagingArea.isEmpty(); i++) {
					Box box = stagingArea.removeBox();
					addToInventory(box);
				}
				Ticker.logEventWithBoxes(id,"stocker_load",getInventoryItems(),true);
				timer = 1;
				return;
			}

			if (inventorySize > 0) {
				stagingArea.releaseForStocker();
				moveTo(peekInventoryCategory());
				return;
			}

		}
		catch (InterruptedException e){
			Thread.currentThread().interrupt();
		}
	}

	public void stock(){
		Section currentSection = this.sections.get(this.position);

		if (status != StockerStatus.STOCKING) {

			currentSection.getStockerLock().lock();

			Ticker.logMessage(id,
				"event=stock_begin section=" + position + " amount=" + currentStockTarget);

			status = StockerStatus.STOCKING;
		}

		if (this.inventoryContainsCategory(this.position)){
			Box box = removeFromInventory(this.position);
			currentSection.addBox(box);
			this.timer++;
			currentStocked++;
		}
		else {
			currentSection.getStockerLock().unlock();
			String nextPosition = peekInventoryCategory();
			if (nextPosition == null) {
				this.moveTo("staging");
			}
			else {
				this.moveTo(nextPosition);
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
		this.timer = 10 + inventory.size();
	}

	// MAIN PER-TICK RUNNER
	public void advance(){
		if (timer > 0) timer--;
		if (timer == 0) { // Previous action expired
			if (status == StockerStatus.WAITING ||
				status == StockerStatus.MOVING
			){ // Waiting or arrived at destination
				if (position.equals("staging")){
					take();
				}
				else { // At a section
					stock();
				}
			}
			else if (status == StockerStatus.TAKING) {
				take(); // Continue taking
			}
			else if (status == StockerStatus.STOCKING) {
				stock(); // Continue stocking
			}
		}
	}
}