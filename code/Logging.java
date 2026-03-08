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
