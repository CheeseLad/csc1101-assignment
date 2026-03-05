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

		try {
			if (status != StockerStatus.TAKING) {
				stagingArea.acquireForStocker();
				// Ticker.logMessage(id, "event=acquire_staging_lock");
				status = StockerStatus.TAKING;
			}

			stagingArea.waitForBoxes();

			if (!stagingArea.isEmpty() && inventorySize() < INVENTORY_LIMIT) {
				int space = INVENTORY_LIMIT - inventorySize();
				for (int i = 0; i < space && !stagingArea.isEmpty(); i++) {
					Box box = stagingArea.removeBox();
					addToInventory(box);
				}
				timer = 1;
				return;
			}

			if (inventorySize() > 0) {
				Ticker.logEventWithBoxes(id,"stocker_load",inventoryItems(),true);
				stagingArea.releaseForStocker();
				// Ticker.logMessage(id,"event=release_staging_lock");
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
			// Ticker.logMessage(id, "event=acquire_" + position + "_lock");

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
			// Ticker.logMessage(id, "event=release_" + this.position + "_lock");
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
				if (this.position.equals("staging")){
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