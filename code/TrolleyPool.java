import java.util.ArrayList;

public class TrolleyPool {

    final int poolSize;
    private final ArrayList<Trolley> trolleyPool = new ArrayList<>();
    private int stockersWithTrolleys = 0;
    private int pickersWithTrolleys = 0;

    public TrolleyPool(int TROLLEY_POOL_SIZE) {
        this.poolSize = TROLLEY_POOL_SIZE;
        for (int i = 1; i <= poolSize; i++) {
            this.trolleyPool.add(new Trolley(i));
        }
	}

    public synchronized Trolley tryAcquireTrolley(Worker worker) throws InterruptedException {
        
        int availableTrolleyCount = trolleyPool.size();

        boolean isStocker = worker instanceof Stocker;
        boolean isPicker = worker instanceof Picker;

        // If last trolley available and other side has no trolleys, let them have it - prevents deadlocks
        if (isStocker && pickersWithTrolleys == 0 && availableTrolleyCount == 1){
            return null;
        }

        if (isPicker && stockersWithTrolleys == 0 && availableTrolleyCount == 1){
            return null;
        }

        if (trolleyPool.isEmpty()) {
            return null;
        }

        // Try to acquire, fail otherwise to not stall ticks
        Trolley trolley = trolleyPool.remove(trolleyPool.size() - 1);

        
        if (isStocker) 
            stockersWithTrolleys++;
        else if (isPicker) 
            pickersWithTrolleys++;

        return trolley;
    }

    public synchronized void releaseTrolley(Worker worker, Trolley trolley) {
        trolleyPool.add(trolley);

        if (worker instanceof Stocker) 
            stockersWithTrolleys--;
        else if (worker instanceof Picker) 
            pickersWithTrolleys--;
    }
}
