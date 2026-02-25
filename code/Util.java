import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.HashMap;

public class Util {

  public Util() {

  }

  public void printEventWithBoxes(String tick, String thread_id, String event, ArrayList<Box> items, Boolean printTotal) {
    StringBuilder itemsString = new StringBuilder();
    HashMap<String, Integer> itemCounts = new HashMap<>();
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
      System.out.println("tick=" + tick + " tid=" + thread_id + " event=" + event + " " + itemsString.toString().trim() + " total_load=" + total);
    } else {
      System.out.println("tick=" + tick + " tid=" + thread_id + " event=" + event + " " + itemsString.toString().trim());
    }
  }

  // TODO: Update with new picker logic
  public void printPick(int tick, String thread_id, String event, int pick_id, String section) {
    System.out.println("tick=" + tick + " tid=" + thread_id + " event=" + event + " pick_id=" + pick_id + " section=" + section);
  }
}