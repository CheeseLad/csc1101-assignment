import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Ticker {
    private static final int TICKS_IN_DAY = 1000;
    
    private static Ticker instance = null; // singleton instance

    private int currentTick = 0; // Measures elapsed ticks from programme start
    private int currentDay = 0; // 1 day = 1000 ticks. This value increments by 1 every 1000 ticks.
    private final int tickDuration; // Amount of miliseconds in a tick, represented as an int (default recommended = 50ms).
    private final int totalRuntimeLength; // Total amount of ticks a programme runs for.
    private final ArrayList<Worker> workers;
    // Note: A day does not mean a real-life 24-hour day, e.g. 50ms ticks * 1000 = 50-second days

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService threadExecutor;
    private final ReentrantLock tickLock = new ReentrantLock();
    private final Condition tickAdvanced = tickLock.newCondition();


    private Ticker(int tickDuration, int totalRuntimeLength, ArrayList<Worker> workers, int numThreads){
        this.tickDuration = tickDuration;
        this.totalRuntimeLength = totalRuntimeLength;
        this.workers = workers;
        this.threadExecutor = Executors.newFixedThreadPool(numThreads);
    }


    // GETTERS

    public static Ticker getInstance(){
        if (instance == null){
            throw new IllegalStateException("Ticker not initialised!");
        }
        return instance;
    }

    public static int getCurrentTick(){
        return getInstance().currentTick;
    }

    public static int getCurrentDay(){
        return getInstance().currentDay;
    }


    // TICKER STATE HANDLING

    public static void initialise(int tickDuration, int totalRuntimeLength, ArrayList<Worker> workers, int numThreads){
        /* Begin the Ticker singleton instance */

        // Ensures single instance of Ticker only (singleton) to avoid accidental strange behaviour.
        if (instance != null) {
            throw new IllegalStateException("Ticker already initialised!");
        }

        instance = new Ticker(tickDuration, totalRuntimeLength, workers, numThreads);
    }

    public void start(){
        /**
         * Starts simulation. Add all generated workers into a thread executor and stard a scheduler at selected period execution.
         * 
         * Every tickDuration ms, the advance() function will be called (which also calls each respective worker advance() function).
        */
        for (Worker worker : workers){
            threadExecutor.submit(worker);
        }
        scheduler.scheduleAtFixedRate(this::advance, 0, tickDuration, TimeUnit.MILLISECONDS);
    }

    public void stop(){
        /**
         * Stop scheduler. Force exits the threadExecutor's threads after all working threads perform their final action.
         */
        scheduler.shutdown();

        tickLock.lock();
        try {
            tickAdvanced.signalAll(); // Wake all waiting working threads for correct exit
        } finally {
            tickLock.unlock();
        }
        threadExecutor.shutdownNow();
    }


    // TICKING LOGIC

    public int waitForNextTick(int lastTick) {
        /* Each worker enters here per loop, and waits for next tick (to ensure that the system is fundamentally "tick-centric") */

        tickLock.lock();
        try {
            while (currentTick == lastTick && !scheduler.isShutdown()) {
                tickAdvanced.await(); // Waits until next tick
            }
            
            return scheduler.isShutdown() ? -1 : currentTick; // If we are shut down, break running worker loop, hence ending thread

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
        finally {
            tickLock.unlock();
        }
    }

    public void advance(){
        /* Main action scheduling interface for workers. This runs every tickDuration ms, tracking ticks and advancing all workers by a tick. */

        tickLock.lock(); // Ensure 2 advance() events cannot occur simultaneously
        try {
            currentTick++;

            if (currentTick % TICKS_IN_DAY == 0){
                currentDay++;
            }

            tickAdvanced.signalAll(); // Wake our waiting workers

        } finally {
            tickLock.unlock();
        }

        if (currentTick >= totalRuntimeLength){
            stop();
        }
    }


    // STATUS CHECK

    public boolean isRunning(){
        return !scheduler.isShutdown();
    }
}
