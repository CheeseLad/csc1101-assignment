public abstract class Worker implements Runnable {
    // volatile flag to signal worker thread to stop
    protected volatile boolean shouldStop = false;
    
    // called by Ticker to signal shutdown
    public void signalStop() {
        this.shouldStop = true;
    }
    
    /**
     * Main run loop called when worker is executed in a thread.
     * Each iteration sleeps briefly to throttle execution to sim ticks.
     */
    @Override
    public void run() {
        while (!shouldStop) {
            // Call the per-tick advance method
            advance();
            
            // Sleep for the duration of one tick to sync with simulation
            int tickDuration = Ticker.getTickDuration();
            if (tickDuration > 0) {
                try {
                    Thread.sleep(tickDuration);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
    
    public abstract void advance(); // Called once per tick by run loop
}
