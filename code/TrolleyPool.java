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

    public synchronized Trolley tryAcquireTrolley(Worker worker) {
        /**
         * Attempt to acquire the trolley for the worker (non-blocking). 
         * 
         * Neither Stocker nor Picker can have all trolleys. If one trolley is left, ensure both sides have trolleys, otherwise revoke the request.
         * 
         * */

        boolean isStocker = worker instanceof Stocker;
        boolean isPicker = worker instanceof Picker;

        if (!isStocker && !isPicker) {
            return null; // Not allowed to acquire trolley
        }

        int availableTrolleyCount = trolleyPool.size();

        int trolleyLimit = Math.max(1, (availableTrolleyCount * 3) / 4);

        // Ensure that neither stockers nor pickers ever have more that 3/4 of available trolleys
        if (isStocker && stockersWithTrolleys >= trolleyLimit){
            return null;
        }

        if (isPicker && pickersWithTrolleys >= trolleyLimit){
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
        /* Release the trolley and add it back into the available trolleys. */

        trolleyPool.add(trolley);

        if (worker instanceof Stocker) 
            stockersWithTrolleys--;
        else if (worker instanceof Picker) 
            pickersWithTrolleys--;
    }
}
