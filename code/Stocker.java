import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.HashMap;
import java.util.ArrayList;

enum StockerStatus {MOVING, TAKING, STOCKING, WAITING}

public class Stocker extends Worker {
	private static final int INVENTORY_LIMIT = 10;

	// unique identifier for logging (S1, S2, ...)
	private static int nextId = 1;
	private final String id;

	private ConcurrentHashMap<String, ConcurrentLinkedQueue<Box>> inventory;
	private String position = "staging";
	private StockerStatus status = StockerStatus.WAITING;
	private int timer = 0;
	private StagingArea stagingArea;
	private HashMap<String, Section> sections;

	// bookkeeping for current stocking session
	private int currentStockTarget = 0;  // how many we intended to stock this trip
	private int currentStocked = 0;      // how many we've already moved

	private ArrayList<Box> inventoryItems(){
		ArrayList<Box> items = new ArrayList<>();
		for (ConcurrentLinkedQueue<Box> queue: this.inventory.values()){
			items.addAll(queue);
		}
		return items;
	}

	public Stocker(StagingArea stagingArea, HashMap<String, Section> sections) {
		this.inventory = new ConcurrentHashMap<>();
		this.stagingArea = stagingArea;
		this.sections = sections;
		this.id = "S" + nextId++;
	}

	public String getId(){
		return id;
	}

	private int inventorySize(){
		int size = 0;
		for (ConcurrentLinkedQueue<Box> queue: this.inventory.values()){
			size += queue.size();
		}
		return size;
	}

	private void addToInventory(Box box){
		this.inventory.putIfAbsent(box.getCategory(), new ConcurrentLinkedQueue<>());
		this.inventory.get(box.getCategory()).add(box);
	}

	private Box removeFromInventory(String category){
		return this.inventory.get(category).poll();
	}

	private boolean inventoryContainsCategory(String category){
		return this.inventory.containsKey(category) && !this.inventory.get(category).isEmpty();
	}

	private String peekInventoryCategory(){
		for (String category: this.inventory.keySet()){
			if (!this.inventory.get(category).isEmpty()){
				return category;
			}
		}
		return null;
	}

	public void take(){
		if (this.inventorySize() < INVENTORY_LIMIT && !this.stagingArea.isEmpty()){
			// Acquire staging area lock
			this.status = StockerStatus.TAKING;
			Box box = this.stagingArea.removeBox();
			this.addToInventory(box);
			this.timer++;
		}
		else if (this.inventorySize() == 0) {
			this.status = StockerStatus.WAITING; // Cannot do anything, wait (keep lock as no other stocker can get anything anyway)
		}
		else {
			// about to leave staging, log loaded inventory
			Ticker.logEventWithBoxes(id, "stocker_load", inventoryItems(), true);
			// Release staging area lock
			this.moveTo(peekInventoryCategory()); // Move to a section to start stocking
		}
	}

	public void stock(){
		if (this.inventorySize() == 0){
			// no inventory left; move back to staging (moveTo will log stock_end if needed)
			this.moveTo("staging"); // "restock" stocker at staging area
		}
		else if (this.inventoryContainsCategory(this.position)){
			// Acquire current section lock
			if (this.status != StockerStatus.STOCKING) {
				// starting a new stocking session
				Ticker.logMessage(id, "event=stock_begin section=" + this.position + " amount=" + currentStockTarget);
			}
			this.status = StockerStatus.STOCKING;
			Box box = removeFromInventory(this.position);
			this.sections.get(this.position).addBox(box);
			this.timer++;
			currentStocked++;
		}
		else {
			// Release current section lock if held
			this.moveTo(peekInventoryCategory()); // Move to a section that has boxes in inventory
		}
	}

	public void moveTo(String position){
		// if we were in the middle of stocking, emit end event before leaving
		if (this.status == StockerStatus.STOCKING) {
			Ticker.logMessage(id, "event=stock_end section=" + this.position +
				" stocked=" + currentStocked + " remaining_load=" + inventorySize());
		}
		String from = this.position;
		int load = this.inventorySize();
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

	public void advance(){
		if (this.timer > 0) this.timer--;
		if (this.timer == 0) { // Previous action expired
			if (this.status == StockerStatus.WAITING ||
				this.status == StockerStatus.MOVING
			){ // Waiting or arrived at destination
				if (this.position == "staging"){
					this.take();
				}
				else {
					this.stock();
				}
			}
			else if (this.status == StockerStatus.TAKING) {
				this.take(); // Continue taking
			}
			else if (this.status == StockerStatus.STOCKING) {
				this.stock(); // Continue stocking
			}
		}
	}
}