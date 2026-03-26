import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.HashMap;
import java.util.Random;
import java.util.ArrayList;

enum StockerStatus {MOVING, TAKING, STOCKING, WAITING}

public class Stocker extends Worker {

	private final String id;
	private static int nextId = 1;
	private static Random RNG = null;
	
	// Internal State
	private ConcurrentHashMap<String, ConcurrentLinkedQueue<Box>> inventory = new ConcurrentHashMap<>();
	private int inventorySize = 0;
	private final int inventoryLimit;
	private StockerStatus status = StockerStatus.WAITING;
	private int readyTick = 0; // Tracks when Stocker is ready for next action
	
	// Environment
	private StagingArea stagingArea;
	private HashMap<String, Section> sections;
	private String nextMoveTarget = null;
	private String position = "staging"; // All Stockers start at Staging Area

	// Break trackers
	private int startBreakTick = -1;
	private int endBreakTick = -1;
	private boolean onBreak = false;
	private Section lockedSection = null; // For recovering state after break
	private boolean holdingStagingLock = false; // For recovering state after break

	// Trolley trackers
	private TrolleyPool trolleyPool;
	private Trolley trolley = null;
	private int trolleyWaitStartTick = -1;

	// track current stocking session values for logs
	private int currentStockTarget = 0;  // how many we intended to stock this trip
	private int currentStocked = 0;      // how many we've already moved

	public Stocker(StagingArea stagingArea, HashMap<String, Section> sections, TrolleyPool trolleyPool, int inventoryLimit, long seed) {
		this.id = "S" + nextId++;
		this.stagingArea = stagingArea;
		this.sections = sections;
		this.trolleyPool = trolleyPool;
		this.inventoryLimit = inventoryLimit;
		if (seed != 0)
			RNG = new Random(seed);
		else
			RNG = new Random();
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

	private ArrayList<String> getInventoryCategoryPriorityList(){
		// Get categories in ascending section stock order, skipping sections stocker has no boxes for
		
		ArrayList<String> categories = new ArrayList<>();

		// Collect valid categories (valid box in inventory)
		for (String category : inventory.keySet()) {
			if (!inventory.get(category).isEmpty()) {
				categories.add(category);
			}
		}

		// Sort by section stock (ascending)
		categories.sort((a, b) -> {
			int sizeA = sections.get(a).getSize();
			int sizeB = sections.get(b).getSize();
			return Integer.compare(sizeA, sizeB);
		});

		return categories;
	}


	// BEHAVIOUR HANDLING
	public void take() {
		try {
			// Not yet taking, try acquiring staging area lock if no deliverers are waiting for lock & begin taking, otherwise wait for next tick 
			if (status != StockerStatus.TAKING){
				if (stagingArea.isDelivererWaiting()) return; // Deliverer priority to prevent delivery starvation (in case of many stockers)
				if (!stagingArea.tryAcquireStagingLock()) return;
				holdingStagingLock = true;
				status = StockerStatus.TAKING;
			}

			// Check if there are boxes to take
			if (stagingArea.isEmpty()) {
				// Nothing to take, so provide lock priority for Deliverer and release lock.
				stagingArea.setDelivererWaiting(true);
				stagingArea.releaseStagingLock();
				holdingStagingLock = false;
				status = StockerStatus.WAITING; // Reset status (for per-tick lock acquire retries)
				return;
			}

			// Before taking, acquire a trolley
			if (trolley == null){
				if (trolleyWaitStartTick == -1) {
					trolleyWaitStartTick = Ticker.getCurrentTick();
				}

				trolley = trolleyPool.tryAcquireTrolley(this);
				if (trolley == null) return;

				// Trolley acquired
				int waited = Ticker.getCurrentTick() - trolleyWaitStartTick;
				Logging.logTrolleyAcquireEvent(this.id, trolley.getId(), waited);
				trolleyWaitStartTick = -1;
			}

			// Taking from Staging Area

			// 1. Try to have at least one of each box category for each currently empty section (ensure progress)
			for (String category : sections.keySet()) {
				if (inventorySize >= inventoryLimit) break;

				Section section = sections.get(category);

				if (section.isEmpty() && !inventoryContainsCategory(category)) {
					Box box = stagingArea.removeBox(category);
					if (box != null) addToInventory(box);
				}
			}

			// 2. Now take a box for each non-full section.
			for (String category : sections.keySet()) {
				if (inventorySize >= inventoryLimit) break;

				Section section = sections.get(category);

				if (!section.isFull() && !inventoryContainsCategory(category)) {
					Box box = stagingArea.removeBox(category);
					if (box != null) addToInventory(box);
				}
			}

			// 3. If we still have space, fill up inventory with boxes for non-full sections
			while (inventorySize < inventoryLimit && !stagingArea.isEmpty()) {

				boolean added = false; 

				for (String category : sections.keySet()) {
					if (inventorySize >= inventoryLimit) break;

					Section section = sections.get(category);
					if (section.isFull()) continue;

					Box box = stagingArea.removeBox(category);
					if (box != null) {
						addToInventory(box);
						added = true;
					}
				}

				if (!added) break; // Don't loop if there's no relevant boxes to add
			}

			// Note: The stocker_load shows current inventory, not what was actually added
			Logging.logEventWithBoxes(id,"stocker_load", getInventoryItems(),true, trolley.getId());

			stagingArea.releaseStagingLock();
			holdingStagingLock = false;

			ArrayList<String> priorityCategories = getInventoryCategoryPriorityList(); // Save where to move (so we can move there on next tick)
			nextMoveTarget = priorityCategories.isEmpty() ? null : priorityCategories.get(0);

			readyTick = Ticker.getCurrentTick() + 1;
			status = StockerStatus.MOVING;

		} catch (Exception e) {
			Logging.logMessage(id, "Stocker failed in take(). Force releasing staging area lock for deadlock prevention. Did the simulation terminate?");
			stagingArea.releaseStagingLock();
			holdingStagingLock = false;
		}
	}

	public void stock(){
		Section currentSection = sections.get(position);
		try {
			// Try acquiring section lock & begin stocking, otherwise wait for next tick
			if (status != StockerStatus.STOCKING) {
				if (!currentSection.tryStockerLock()) return;
				lockedSection = currentSection;
				Logging.logMessage(id,
					"event=stock_begin section=" + position + " amount=" + currentStockTarget + " trolley_id=" + trolley.getId());

				status = StockerStatus.STOCKING;
			}

			// We are in stocking mode

			// Do we have valid boxes for this section?
			if (inventoryContainsCategory(position)){
				
				// Try adding a box to the section. This box adding happens each tick until section is full or no more valid boxes in inventory.
				Box box = removeFromInventory(position);
				boolean success = currentSection.stockerTryAddBox(box);

				if (success){
					readyTick = Ticker.getCurrentTick() + 1;
					currentStocked++;
					return;
				}

				// Failed to add new box but we should still have valid boxes left! We assume section is full or otherwise inaccessible at the moment.

				// Give back my box!
				addToInventory(box);
			}
			
			// There is no boxes (left) in inventory for this section

			Logging.logMessage(id, "event=stock_end section=" + position + " stocked=" + currentStocked + " remaining_load=" + inventorySize + " trolley_id=" + trolley.getId());
			
			currentSection.releaseStockerLock();
			lockedSection = null;
			
			// Can I stock elsewhere that is not full and is prioritised by ascending current section stock?
			for (String category : getInventoryCategoryPriorityList()) {
				Section section = sections.get(category);
				if (!section.isFull()) {
					moveTo(category);
					return;
				}
			}

			// Can't stock anything with current inventory, return to staging to restock to ensure progress.

			if (inventorySize == 0 && trolley != null) {
				trolleyPool.releaseTrolley(this, trolley);
				Logging.logTrolleyReleaseEvent(this.id, trolley.getId());
				trolley = null;
			}

			moveTo("staging");
		}
		catch (Exception e) {
			Logging.logMessage(id, "Stocker failed in stock(). Force releasing section lock for deadlock prevention. Did the simulation terminate?");
			currentSection.releaseStockerLock();
			lockedSection = null;
		}
	}

	public void moveTo(String newPosition){
		if (trolley != null)
			Logging.logMessage(id, "event=move from=" + position + " to=" + newPosition + " load=" + inventorySize + " trolley_id=" + trolley.getId());
		else
			Logging.logMessage(id, "event=move from=" + position + " to=" + newPosition + " load=" + inventorySize);

		// when heading to a new section (not staging) set up stock counters
		if (!newPosition.equals("staging")){
			currentStockTarget = inventory.containsKey(newPosition) ? inventory.get(newPosition).size() : 0;
			currentStocked = 0;
		}
		
		this.status = StockerStatus.MOVING;
		this.position = newPosition;
		readyTick = Ticker.getCurrentTick() + 10 + inventorySize;
	}

	private void releaseAllLocks() {
		// Open the flood gates!
		if (lockedSection != null) {
			lockedSection.releaseStockerLock();
			lockedSection = null;
		}
		if (holdingStagingLock) {
			stagingArea.releaseStagingLock();
			holdingStagingLock = false;
		}
	}


	public void advance(){
		if (!Ticker.getInstance().isRunning()) return;

		int currentTick = Ticker.getCurrentTick();

		// Did previous break period end?
		if (endBreakTick < currentTick){
			onBreak = false;
			startBreakTick = currentTick + 200 + RNG.nextInt(100); // Next break in 200-300 ticks
			endBreakTick = startBreakTick + 150; // Break always lasts 150 ticks
			Logging.logMessage(id, "event=end_break (next break between ticks " + startBreakTick + " and " + endBreakTick + ")");
		}

		// We are entering a break period
		if (!onBreak && startBreakTick <= currentTick && currentTick <= endBreakTick){
			Logging.logMessage(id, "event=start_break");
			releaseAllLocks();
			status = StockerStatus.WAITING;
			nextMoveTarget = null;
			readyTick = readyTick + 150; // Pause action we are doing for 150 ticks
			onBreak = true;
			return;
		}

		if (onBreak || currentTick < readyTick){
			// If we are on break or not ready for next action (doing something already), skip tick
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