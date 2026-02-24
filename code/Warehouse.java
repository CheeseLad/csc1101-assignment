import java.util.ArrayList;

public class Warehouse {

  private ArrayList<Box> stagingArea;
  private ArrayList<Box> inventory;

  public Warehouse() {
    inventory = new ArrayList<>();
    stagingArea = new ArrayList<>();
  }

  public ArrayList<Box> getStagingArea() {
    return stagingArea;
  }

  public void addDeliveryToStagingArea(ArrayList<Box> delivery) {
    synchronized (this) {
      stagingArea.addAll(delivery);
    }
  }
}