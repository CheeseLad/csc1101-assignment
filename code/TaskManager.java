import java.util.Random;
import java.util.ArrayList;

class TaskManager implements Runnable {
  private int sleepTime;
  private String threadName;
  private static Random generator = new Random();

  public TaskManager(String name) {
    threadName = name;
    sleepTime = generator.nextInt(1000);
  }

  public void run() {
    try {
      System.out.printf("Thread %s running\n", threadName);
      // TODO: Implement tick behaviour

      String[] categories = {"electronics", "books", "medicines", "clothes", "tools"};

      Util util = new Util();
      Delivery delivery = new Delivery(categories);
      Warehouse warehouse = new Warehouse();

      for (String category : categories) {
        System.out.println("Creating section for category: " + category);
        Section section = new Section(category);
        System.out.println(section);
      }

      warehouse.addDeliveryToStagingArea(delivery.getDeliveryItems());
      util.printEventWithBoxes("120", "DEL", "delivery_arrived", warehouse.getStagingArea(), false);

      System.out.printf("%s sleeps for %d ms.\n", threadName, sleepTime);
      Thread.sleep(sleepTime);
    }

    catch (InterruptedException exception) {
      exception.printStackTrace();
    }
      System.out.printf( "%s done sleeping\n", threadName );
    }
}