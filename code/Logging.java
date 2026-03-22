import java.util.HashMap;

public class Logging {
    public static void logEventWithBoxes(String threadId, String event, java.util.ArrayList<Box> items, boolean printTotal) {
        int tick = Ticker.getCurrentTick();
        HashMap<String, Integer> _itemCounts = new HashMap<>();
        String tickStr = tick < 0 ? "?" : Integer.toString(tick);
        StringBuilder itemsString = new StringBuilder();
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

    public static void logEventWithBoxes(String threadId, String event, java.util.ArrayList<Box> items, boolean printTotal, int trolley_id) {
        int tick = Ticker.getCurrentTick();
        HashMap<String, Integer> _itemCounts = new HashMap<>();
        String tickStr = tick < 0 ? "?" : Integer.toString(tick);
        StringBuilder itemsString = new StringBuilder();
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
            System.out.println("tick=" + tickStr + " tid=" + threadId + " event=" + event + " " + itemsString.toString().trim() + " total_load=" + total + " trolley_id=" + trolley_id);
        } else {
            System.out.println("tick=" + tickStr + " tid=" + threadId + " event=" + event + " " + itemsString.toString().trim() + " trolley_id=" + trolley_id);
        }
    }

    public static void logPickerEvent(String threadId, String event, int pickId, String section, int currentSectionStock, int trolleyId) {
        int tick = Ticker.getCurrentTick();
        String tickStr = tick < 0 ? "?" : Integer.toString(tick);
        String pickStr = pickId < 0 ? "?" : Integer.toString(pickId);

        System.out.println("tick=" + tickStr + " tid=" + threadId + " event=" + event + " pick_id=" + pickId + " section=" + section + " current_stock=" + currentSectionStock + " trolley_id=" + trolleyId);
    }

    public static void logPickerEvent(String threadId, String event, int pickId, String section, int waitedTicks, int sectionStockLeft, int trolleyId) {
        int tick = Ticker.getCurrentTick();
        String tickStr = tick < 0 ? "?" : Integer.toString(tick);
        String pickStr = pickId < 0 ? "?" : Integer.toString(pickId);

        System.out.println("tick=" + tickStr + " tid=" + threadId + " event=" + event + " pick_id=" + pickId + " section=" + section + " stock_left=" + sectionStockLeft + " waited_ticks=" + waitedTicks + " trolley_id=" + trolleyId);
    }

    public static void logTrolleyEvent(String threadId, String event, int trolleyId) {
        int tick = Ticker.getCurrentTick();
        String tickStr = tick < 0 ? "?" : Integer.toString(tick);
        String trolleyStr = trolleyId < 0 ? "?" : Integer.toString(trolleyId);

        System.out.println("tick=" + tickStr + " tid=" + threadId + " event=" + event + " trolley_id=" + trolleyId);
    }

    public static void logTrolleyEvent(String threadId, String event, int trolleyId, int waitedTicks) {
        int tick = Ticker.getCurrentTick();
        String tickStr = tick < 0 ? "?" : Integer.toString(tick);
        String trolleyStr = trolleyId < 0 ? "?" : Integer.toString(trolleyId);

        System.out.println("tick=" + tickStr + " tid=" + threadId + " event=" + event + " trolley_id=" + trolleyId + " waited_ticks=" + waitedTicks);
    }

    public static void logMessage(String threadId, String message) {
        int tick = Ticker.getCurrentTick();
        String tickStr = tick < 0 ? "?" : Integer.toString(tick);
        if (message.startsWith("event=")) {
            // already formatted as an event
            System.out.println("tick=" + tickStr + " tid=" + threadId + " " + message);
        } else {
            System.out.println("tick=" + tickStr + " tid=" + threadId + " msg=" + message);
        }
    }
}
