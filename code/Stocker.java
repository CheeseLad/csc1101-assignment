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

	// Trolley trackers
	private TrolleyPool trolleyPool;
	private Trolley trolley = null;
	private int trolleyWaitStartTick = -1;

	// track current stocking session values for logs
	private int currentStockTarget = 0;  // how many we intended to stock this trip
	private int currentStocked = 0;      // how many we've already moved

	public Stocker(StagingArea stagingArea, HashMap<String, Section> sections, TrolleyPool trolleyPool) {
		this.stagingArea = stagingArea;
		this.sections = sections;
		this.trolleyPool = trolleyPool;
		this.id = "S" + nextId++;
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

	private String peekInventoryCategory(){
		// Gets the next category with the highest quantity of boxes (semi-randomisation + optimal to reduce travel costs)
		
		String chosenCategory = null;
		int maxCount = 0;
		
		for (String category: inventory.keySet()){
			ConcurrentLinkedQueue<Box> queue = inventory.get(category);
			int size = queue.size();
			if (size > maxCount){
				maxCount = size;
				chosenCategory = category;
			}
		}
		return chosenCategory;
	}


	// BEHAVIOUR HANDLING
	public void take() {
		try {
			if (status != StockerStatus.TAKING){ // Not yet taking, try to acquire lock
				stagingArea.acquireForStocker();
				status = StockerStatus.TAKING;
			}

			// Check if there are boxes to take
			if (stagingArea.isEmpty()) { // Cannot take anything, wait instead
				stagingArea.releaseForStocker();
				status = StockerStatus.WAITING;
				return;
			}

			// Before taking, acquire a trolley
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

			// Fill up inventory as much as possible
			int space = INVENTORY_LIMIT - inventorySize;

			for (int i = 0; i < space && !stagingArea.isEmpty(); i++) {
				Box box = stagingArea.removeBox();
				addToInventory(box);
			}

			Logging.logEventWithBoxes(id,"stocker_load",getInventoryItems(),true, trolley.getId());

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
			// currentSection.getStockerLock().lock();
			Logging.logMessage(id,
				"event=stock_begin section=" + position + " amount=" + currentStockTarget + " trolley_id=" + trolley.getId());

			status = StockerStatus.STOCKING;
		}

		if (inventoryContainsCategory(position)){
			try {
				Box box = removeFromInventory(position);
				currentSection.addBox(box);
				readyTick = Ticker.getCurrentTick() + 1;
				currentStocked++;
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		else {
			// currentSection.getStockerLock().unlock();
			Logging.logMessage(id, "event=stock_end section=" + position +
				" stocked=" + currentStocked + " remaining_load=" + inventorySize + " trolley_id=" + trolley.getId());
			
			sections.get(position).stockerDoneStocking();
			String nextPosition = peekInventoryCategory();

			if (nextPosition == null) {
				// Inventory is empty and returning to staging area, release trolley
				if (trolley != null){
					trolleyPool.releaseTrolley(this, trolley);
					Logging.logTrolleyEvent(this.id, "release_trolley", trolley.getId());
					trolley = null;
				}

				moveTo("staging");
			}
			else {
				moveTo(nextPosition);
			}
		}
	}

	public void moveTo(String newPosition){
		Logging.logMessage(id, "event=move from=" + position + " to=" + newPosition + " load=" + inventorySize + " trolley_id=" + trolley.getId());

		// when heading to a new section (not staging) set up stock counters
		if (!newPosition.equals("staging")){
			currentStockTarget = inventory.containsKey(newPosition) ? inventory.get(newPosition).size() : 0;
			currentStocked = 0;
		}
		this.status = StockerStatus.MOVING;
		this.position = newPosition;
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