import java.util.concurrent.ConcurrentHashMap;

public class Warehouse {

  private ConcurrentHashMap<String, Integer> inventory;
  private ConcurrentHashMap<String, Integer> stagingArea;

  public Warehouse() {
    inventory = new ConcurrentHashMap<>();
    stagingArea = new ConcurrentHashMap<>();
  }

  public ConcurrentHashMap<String, Integer> getInventory() {
    return inventory;
  }

  public ConcurrentHashMap<String, Integer> getStagingArea() {
    return stagingArea;
  }

  public void addItem(String item, int quantity) {
    inventory.put(item, quantity);
  }

  public void addDeliveryToStagingArea(ConcurrentHashMap<String, Integer> deliveryItems) {
    for (String item : deliveryItems.keySet()) {
      stagingArea.put(item, stagingArea.getOrDefault(item, 0) + deliveryItems.get(item));
    }
  }

  public void printInventory() {
    for (String item : inventory.keySet()) {
      System.out.println(item + ": " + inventory.get(item));
    }
  }

  public void printStagingArea() {
    for (String item : stagingArea.keySet()) {
      System.out.println(item + ": " + stagingArea.get(item));
    }
  }

}