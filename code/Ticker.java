import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Ticker {
    private static final int TICKS_IN_DAY = 1000;
    
    private static Ticker instance = null;

    private int currentTick = 0; // Measures current tick in the day (resetting to 0 on new day).
    private int currentDay = 0; // 1 day = 1000 ticks. This value increments by 1 once 1000 ticks pass.
    private int totalTicksElapsed = 0; // Track all ticks elapsed since programme start.
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


    public static void initialise(int tickDuration, int totalRuntimeLength, ArrayList<Worker> workers, int numThreads){
        // Ensures single instance of Ticker only (singleton)

        if (instance != null) {
            throw new IllegalStateException("Ticker already initialised!");
        }

        instance = new Ticker(tickDuration, totalRuntimeLength, workers, numThreads);
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

    public static int getTickDuration(){
        return getInstance().tickDuration;
    }

    // RUNNERS
    public void start(){
        for (Worker worker : workers){
            threadExecutor.submit(worker);
        }
        scheduler.scheduleAtFixedRate(this::advance, 0, tickDuration, TimeUnit.MILLISECONDS);
    }

    public void stop(){
        scheduler.shutdown();
    }

    // WORKER WAITING HANDLER
    public int waitForNextTick(int lastTick) {

        tickLock.lock();
        try {
            while (currentTick == lastTick && !scheduler.isShutdown()) {
                tickAdvanced.await();
            }
            
            return scheduler.isShutdown() ? -1 : currentTick;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
        finally {
            tickLock.unlock();
        }
    }

    // LOGGERS
    public static void logEventWithBoxes(String threadId, String event, java.util.ArrayList<Box> items, boolean printTotal) {
        int tick = getCurrentTick();
        HashMap<String, Integer> _itemCounts = new HashMap<>();
        String tickStr = tick < 0 ? "?" : Integer.toString(tick);
        StringBuilder itemsString = new StringBuilder();
        _itemCounts.clear();
        int total = 0;
        for (Box box : items) {
            String item = box.getCategory();
            _itemCounts.put(item, _itemCounts.getOrDefault(item, 0) + 1);
        }
        for (String item : _itemCounts.keySet()) {
            itemsString.append(item).append("=").append(_itemCounts.get(item)).append(" ");
            total += _itemCounts.get(item);
        }

        if (printTotal) {
            System.out.println("tick=" + tickStr + " tid=" + threadId + " event=" + event + " " + itemsString.toString().trim() + " total_load=" + total);
        } else {
            System.out.println("tick=" + tickStr + " tid=" + threadId + " event=" + event + " " + itemsString.toString().trim());
        }
    }

    public static void logMessage(String threadId, String message) {
        int tick = getCurrentTick();
        String tickStr = tick < 0 ? "?" : Integer.toString(tick);
        if (message.startsWith("event=")) {
            // already formatted as an event
            System.out.println("tick=" + tickStr + " tid=" + threadId + " " + message);
        } else {
            System.out.println("tick=" + tickStr + " tid=" + threadId + " msg=" + message);
        }
    }

    // MAIN FUNCTION EXECUTED EVERY <tickDuration> ms
    public void advance(){
        tickLock.lock();
        try {

            currentTick++;
            totalTicksElapsed++;

            if (currentTick >= TICKS_IN_DAY){
                currentDay++;
                currentTick %= TICKS_IN_DAY;
            }

            tickAdvanced.signalAll();

        } finally {
            tickLock.unlock();
        }

        if (totalTicksElapsed >= totalRuntimeLength){
            System.out.println("Programme complete.");
            stop();
        }
    }

    // STATUS CHECKERS
    public boolean isRunning(){
        return !scheduler.isShutdown();
    }
}
