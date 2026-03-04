public abstract class Worker {
    // private boolean ready = false;

    // public void setReady(boolean flag) {
    //     this.ready = flag;
    // }
    
    // public boolean isReady() {
    //     /**
    //      * Is Worker ready for next tick advance?
    //      */
    //     return this.ready;
    // }
    
    public abstract void advance(); // Ticker class calls this upon a 1-tick advance
}
