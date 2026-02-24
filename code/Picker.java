import java.util.ArrayList;

public class Picker {

  private Box currentBox;

  public Picker() {
    this.currentBox = null;
  }

  public String pickBox(Warehouse warehouse) {
      Box box = warehouse.removeBoxFromStagingArea();
      this.currentBox = box;
      if (box != null) {
        return box.getCategory();
      } else {
        System.out.println("No boxes available in staging area.");
        return null;
      }
  }
}