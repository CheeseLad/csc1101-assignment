import java.util.HashMap;

public class Logging {
    
    public static void logEventWithBoxes(String threadId, String event, java.util.ArrayList<Box> items, boolean printTotal) {
        /* Log this worker's event alongside passed items split into "category=quantity" pairs. */
        int tick = Ticker.getCurrentTick();
        HashMap<String, Integer> itemCounts = new HashMap<>();
        StringBuilder itemsString = new StringBuilder();
        
        int total = 0;
        for (Box box : items) {
            String item = box.getCategory();
            itemCounts.put(item, itemCounts.getOrDefault(item, 0) + 1);
        }
        for (String item : itemCounts.keySet()) {
            itemsString.append(item).append("=").append(itemCounts.get(item)).append(" ");
            total += itemCounts.get(item);
        }

        if (printTotal) {
            System.out.println("tick=" + tick + " tid=" + threadId + " event=" + event + " " + itemsString.toString().trim() + " total_load=" + total);
        } else {
            System.out.println("tick=" + tick + " tid=" + threadId + " event=" + event + " " + itemsString.toString().trim());
        }
    }


    public static void logEventWithBoxes(String threadId, String event, java.util.ArrayList<Box> items, boolean printTotal, int trolley_id) {
        /* Log this worker's event alongside passed items split into "category=quantity" pairs and held trolley id. */
        int tick = Ticker.getCurrentTick();
        HashMap<String, Integer> itemCounts = new HashMap<>();
        StringBuilder itemsString = new StringBuilder();
        
        int total = 0;
        for (Box box : items) {
            String item = box.getCategory();
            itemCounts.put(item, itemCounts.getOrDefault(item, 0) + 1);
        }
        for (String item : itemCounts.keySet()) {
            itemsString.append(item).append("=").append(itemCounts.get(item)).append(" ");
            total += itemCounts.get(item);
        }

        if (printTotal) {
            System.out.println("tick=" + tick + " tid=" + threadId + " event=" + event + " " + itemsString.toString().trim() + " total_load=" + total + " trolley_id=" + trolley_id);
        } else {
            System.out.println("tick=" + tick + " tid=" + threadId + " event=" + event + " " + itemsString.toString().trim() + " trolley_id=" + trolley_id);
        }
    }


    public static void logPickerStartEvent(String threadId, String event, int pickId, String section, int currentSectionStock, int trolleyId) {
        /* Log picker's start picking event, selected section, current stock in section (if 0, picker is expected to wait) and held trolley id */
        int tick = Ticker.getCurrentTick();
        
        System.out.println("tick=" + tick + " tid=" + threadId + " event=" + event + " pick_id=" + pickId + " section=" + section + " current_stock=" + currentSectionStock + " trolley_id=" + trolleyId);
    }


    public static void logPickerDoneEvent(String threadId, String event, int pickId, String section, int waitedTicks, int sectionStockLeft, int trolleyId) {
        /* Log picker's done picking event, selected section and its stock after picking, and held trolley id */
        int tick = Ticker.getCurrentTick();

        System.out.println("tick=" + tick + " tid=" + threadId + " event=" + event + " pick_id=" + pickId + " section=" + section + " stock_left=" + sectionStockLeft + " waited_ticks=" + waitedTicks + " trolley_id=" + trolleyId);
    }


    public static void logTrolleyAcquireEvent(String threadId, int trolleyId, int waitedTicks) {
        /* Log trolley acquire alongside waited ticks  */
        int tick = Ticker.getCurrentTick();

        System.out.println("tick=" + tick + " tid=" + threadId + " event=acquire_trolley" + " trolley_id=" + trolleyId + " waited_ticks=" + waitedTicks);
    }


    public static void logTrolleyReleaseEvent(String threadId, int trolleyId) {
        /* Log trolley release  */
        int tick = Ticker.getCurrentTick();

        System.out.println("tick=" + tick + " tid=" + threadId + " event=release_trolley" + " trolley_id=" + trolleyId);
    }


    public static void logMessage(String threadId, String message) {
        /* Log generic message or misc event for a thread */
        int tick = Ticker.getCurrentTick();

        if (message.startsWith("event=")) {
            System.out.println("tick=" + tick + " tid=" + threadId + " " + message);
        } else {
            System.out.println("tick=" + tick + " tid=" + threadId + " msg=" + message);
        }
    }
}
