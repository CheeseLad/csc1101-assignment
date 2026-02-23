import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;

public class Picker {

  private ConcurrentHashMap<String, Integer> pickerInventory;

  public Picker() {
    this.pickerInventory = new ConcurrentHashMap<>();
  }

  public ConcurrentHashMap<String, Integer> getPickerInventory() {
    return pickerInventory;
  }

  public void takeBox(ConcurrentHashMap<String, Integer> warehouseInventory) {

    Random randgen = new Random(); //  set a seed to replicate behaviour

    int newItem = randgen.nextInt(5);
    if (newItem == 0) {
        warehouseInventory.put("electronics", warehouseInventory.get("electronics") - 1);
        pickerInventory.put("electronics", pickerInventory.getOrDefault("electronics", 0) + 1);
      } else if (newItem == 1) {
        pickerInventory.put("books", pickerInventory.getOrDefault("books", 0) + 1);
        warehouseInventory.put("books", warehouseInventory.get("books") - 1);
      } else if (newItem == 2) {
        pickerInventory.put("medicines", pickerInventory.getOrDefault("medicines", 0) + 1);
        warehouseInventory.put("medicines", warehouseInventory.get("medicines") - 1);
      } else if (newItem == 3) {
        pickerInventory.put("clothes", pickerInventory.getOrDefault("clothes", 0) + 1);
        warehouseInventory.put("clothes", warehouseInventory.get("clothes") - 1);
      } else if (newItem == 4) {
        pickerInventory.put("tools", pickerInventory.getOrDefault("tools", 0) + 1);
        warehouseInventory.put("tools", warehouseInventory.get("tools") - 1);
      }
    }
}