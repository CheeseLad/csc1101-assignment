public abstract class Worker implements Runnable {

    private int lastTick = -1;

    @Override
    public void run() {
        while (Ticker.getInstance().isRunning()) {
            int tick = Ticker.getInstance().waitForNextTick(lastTick);
            if (tick < 0) {
                break;
            }
            lastTick = tick;
            advance();
        }
    }

    public abstract void advance();
}