public abstract class Worker implements Runnable {

    private int lastTick = -1;

    @Override
    public void run() {
        Ticker ticker = Ticker.getActiveTicker();
        while (true) {
            int tick = ticker.waitForNextTick(lastTick);
            if (tick < 0) {
                break;
            }
            lastTick = tick;
            advance();
        }
    }

    public abstract void advance();
}