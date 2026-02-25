import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.Random;
import java.util.ArrayList;

public class Main {
  public static void main(String[] args) {

    String[] categories = {"electronics", "books", "medicines", "clothes", "tools"};

    Util util = new Util();
    Warehouse warehouse = new Warehouse();

    ArrayList<Section> sections = new ArrayList<>();

    for (String category : categories) {
      Section section = new Section(category);
      sections.add(section);
    }

    Delivery delivery = new Delivery(categories);
    warehouse.addDeliveryToStagingArea(delivery.getDeliveryItems());
    util.printEventWithBoxes("120", "DEL", "delivery_arrived", warehouse.getStagingArea(), false);

    // Simulate stockers moving boxes from staging area to sections
    for (Box box : warehouse.getStagingArea()) {
      for (Section section : sections) {
        if (section.getSectionByCategory(box.getCategory()) != null) {
          section.addBox(box);
          util.printPick(130, "STK", "stocked_section", 1, section.getCategory());
          break;
        }
      }
    }

    Random randgen = new Random(); //  set a seed to replicate behaviour

    int daily_limit = randgen.nextInt(100) + 50; // Random daily limit between 50 and 150

    TaskManager P1 = new TaskManager("P1", daily_limit, sections, randgen);
    TaskManager P2 = new TaskManager("P2", daily_limit, sections, randgen);
    TaskManager P3 = new TaskManager("P3", daily_limit, sections, randgen);
    TaskManager P4 = new TaskManager("P4", daily_limit, sections, randgen);
    TaskManager P5 = new TaskManager("P5", daily_limit, sections, randgen);

    System.out.println("Starting threads");

    ExecutorService threadExecutor = Executors.newFixedThreadPool(5);

    threadExecutor.execute(P1);
    threadExecutor.execute(P2);
    threadExecutor.execute(P3);
    threadExecutor.execute(P4);
    threadExecutor.execute(P5);
    threadExecutor.shutdown();

    System.out.println( "Threads started, main ends\n" );

  }
}