import java.util.concurrent.ConcurrentHashMap;

public class Stocker {

  private ConcurrentHashMap<String, Integer> stockerInventory;

  public Stocker() {
    this.stockerInventory = new ConcurrentHashMap<>();
  }

  public void pickupItemsFromStagingArea(ConcurrentHashMap<String, Integer> warehouseStagingArea) {
    for (String item : warehouseStagingArea.keySet()) {
      stockerInventory.put(item, stockerInventory.getOrDefault(item, 0) + warehouseStagingArea.get(item));
    }
    warehouseStagingArea.clear();
  }

  public void stockItemsToWarehouse(ConcurrentHashMap<String, Integer> warehouseInventory) {
    for (String item : stockerInventory.keySet()) {
      warehouseInventory.put(item, warehouseInventory.getOrDefault(item, 0) + stockerInventory.get(item));
    }
    stockerInventory.clear();
  }

  public ConcurrentHashMap<String, Integer> getStockerInventory() {
    return stockerInventory;
  }

}