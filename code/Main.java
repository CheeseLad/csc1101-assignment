public class Main {
  public static void main(String[] args) {

    // TODO: Implement tick behaviour
    Delivery delivery = new Delivery();

    Warehouse warehouse = new Warehouse();

    warehouse.addDeliveryToStagingArea(delivery.getDeliveryItems());
    warehouse.printStagingArea();

    /*warehouse.addItem("electronics", 7);
    warehouse.addItem("books", 0);

    warehouse.printInventory();

    warehouse.printEvent("120", "DEL", "delivery_arrived", warehouse.getInventory());*/

  }
}