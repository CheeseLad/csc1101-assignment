public abstract class Worker implements Runnable {

    private int lastTick = -1;

    @Override
    public void run() {
        /* Main Worker loop that waits for next tick, then calls advance() */
        while (Ticker.getInstance().isRunning()) {
            int tick = Ticker.getInstance().waitForNextTick(lastTick);
            if (tick < 0) { // -1 returned breaks the loop
                break;
            }
            lastTick = tick;
            advance();
        }
    }

    public abstract void advance();
}