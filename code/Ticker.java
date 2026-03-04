import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.List;

public class Ticker {
    private static final int TICKS_IN_DAY = 1000;
    
    // singleton instance so that other classes can query the current tick/day
    private static Ticker activeTicker = null;

    private int currentTick = 0; // Measures current tick in the day (resetting to 0 on new day).
    private int currentDay = 0; // 1 day = 1000 ticks. This value increments by 1 once 1000 ticks pass.
    private int totalTicksElapsed = 0; // Track all ticks elapsed since programme start.
    private final int tickDuration; // Amount of miliseconds in a tick, represented as an int (default recommended = 50ms).
    private final int totalRuntimeLength; // Total amount of ticks a programme runs for.
    private final ArrayList<Worker> workers;
    // Note: A day does not mean a real-life 24-hour day, e.g. 50ms ticks * 1000 = 50-second days

    private final ScheduledExecutorService tickerScheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService workerThreadPool; // fixed thread pool for worker threads
    private List<Thread> workerThreads = new ArrayList<>();
    private volatile boolean isShuttingDown = false;

    public Ticker(int tickDuration, int totalRuntimeLength, ArrayList<Worker> workers, int threadPoolSize){
        this.tickDuration = tickDuration;
        this.totalRuntimeLength = totalRuntimeLength;
        this.workers = workers;
        this.workerThreadPool = Executors.newFixedThreadPool(threadPoolSize);
        // register singleton instance for global access
        activeTicker = this;
    }

    public int getCurrentTick(){
        return this.currentTick;
    }

    /**
     * Static accessor for the currently running ticker (may return -1 if none exists).
     */
    public static int getCurrentTickStatic() {
        return activeTicker != null ? activeTicker.currentTick : -1;
    }

    /**
     * Static accessor for the current day of the running ticker.
     */
    public static int getCurrentDayStatic() {
        return activeTicker != null ? activeTicker.currentDay : -1;
    }

    /**
     * Static accessor for tick duration (used by Worker threads).
     */
    public static int getTickDuration() {
        return activeTicker != null ? activeTicker.tickDuration : -1;
    }

    public int getCurrentDay(){
        return this.currentDay;
    }

    /**
     * Start the ticker and submit all worker threads to the pool.
     */
    public void start(){
        // Submit all workers as tasks to the thread pool
        for (Worker worker : workers) {
            workerThreadPool.submit(worker);
        }
        
        // Start the ticker scheduler which will count ticks and call advance()
        tickerScheduler.scheduleAtFixedRate(this::advanceTick, 0, tickDuration, TimeUnit.MILLISECONDS);
    }

    /**
     * Stop the simulation: signal workers to stop and shutdown pools.
     */
    public void stop(){
        isShuttingDown = true;
        
        // Signal all workers to stop
        for (Worker worker : workers) {
            worker.signalStop();
        }
        
        // Shutdown thread pools
        tickerScheduler.shutdown();
        workerThreadPool.shutdown();
        
        try {
            // Wait for pools to terminate
            if (!tickerScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                tickerScheduler.shutdownNow();
            }
            if (!workerThreadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                workerThreadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            tickerScheduler.shutdownNow();
            workerThreadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Called by ticker scheduler to advance the simulation tick.
     * This runs in the scheduler thread, not in worker threads.
     */
    private void advanceTick(){
        this.currentTick++;
        this.totalTicksElapsed++;
        
        if (this.currentTick >= TICKS_IN_DAY){
            this.currentDay++;
            this.currentTick %= TICKS_IN_DAY;
        }
        if (totalTicksElapsed >= totalRuntimeLength){
            System.out.println("Programme complete.");
            stop();
        }
    }


    private static final java.util.HashMap<String, Integer> _tempCounts = new java.util.HashMap<>();

    /**
     * Log an event with a list of boxes.  The tick value is looked up
     * from the active ticker if available.
     */
    public static void logEventWithBoxes(String threadId, String event, java.util.ArrayList<Box> items, boolean printTotal) {
        int tick = getCurrentTickStatic();
        String tickStr = tick < 0 ? "?" : Integer.toString(tick);
        StringBuilder itemsString = new StringBuilder();
        _tempCounts.clear();
        int total = 0;
        for (Box box : items) {
            String item = box.getCategory();
            _tempCounts.put(item, _tempCounts.getOrDefault(item, 0) + 1);
        }
        for (String item : _tempCounts.keySet()) {
            itemsString.append(item).append("=").append(_tempCounts.get(item)).append(" ");
            total += _tempCounts.get(item);
        }

        if (printTotal) {
            System.out.println("tick=" + tickStr + " tid=" + threadId + " event=" + event + " " + itemsString.toString().trim() + " total_load=" + total);
        } else {
            System.out.println("tick=" + tickStr + " tid=" + threadId + " event=" + event + " " + itemsString.toString().trim());
        }
    }


    public static void logMessage(String threadId, String message) {
        int tick = getCurrentTickStatic();
        String tickStr = tick < 0 ? "?" : Integer.toString(tick);
        if (message.startsWith("event=")) {
            // already formatted as an event
            System.out.println("tick=" + tickStr + " tid=" + threadId + " " + message);
        } else {
            System.out.println("tick=" + tickStr + " tid=" + threadId + " msg=" + message);
        }
    }


    public boolean isRunning(){
        return !tickerScheduler.isShutdown();
    }
}
