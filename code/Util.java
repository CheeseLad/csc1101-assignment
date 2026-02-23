import java.util.concurrent.ConcurrentHashMap;

public class Util {

  public Util() {

  }

  public void printEventWithBoxes(String tick, String thread_id, String event, ConcurrentHashMap<String, Integer> items, Boolean printTotal) {
    StringBuilder itemsString = new StringBuilder();
    int total = 0;
    for (String item : items.keySet()) {
      itemsString.append(item).append("=").append(items.get(item)).append(" ");
      total += items.get(item);
    }
    if (printTotal) {
      System.out.println("tick=" + tick + " tid=" + thread_id + " event=" + event + " " + itemsString.toString().trim() + " total_load=" + total);
    } else {
      System.out.println("tick=" + tick + " tid=" + thread_id + " event=" + event + " " + itemsString.toString().trim());
    }
  }

  public void printPickEvent(String tick, String thread_id, String event, String pick_id, ConcurrentHashMap<String, Integer> items) {
    String section = items.keySet().stream().filter(item -> items.get(item) > 0).findFirst().orElse("none");
    System.out.println("tick=" + tick + " tid=" + thread_id + " event=" + event + " pick_id=" + pick_id + " section=" + section);
  }
}