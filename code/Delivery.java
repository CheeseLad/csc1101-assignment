import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;

public class Delivery {

  private ConcurrentHashMap<String, Integer> deliveryItems;
  //private long seed = 12345L;

  public ConcurrentHashMap<String, Integer> getDeliveryItems() {
    return deliveryItems;
  }

  public Delivery() {

    Random randgen = new Random(); //  set a seed to replicate behaviour
    //Random randgen = new Random(seed); //  set a seed to replicate behaviour

    deliveryItems = new ConcurrentHashMap<>();

    deliveryItems.put("electronics", 0);
    deliveryItems.put("books", 0);
    deliveryItems.put("medicines", 0);
    deliveryItems.put("clothes", 0);
    deliveryItems.put("tools", 0);

    for (int i = 0; i < 10; i++) {
      int newItem = randgen.nextInt(5);
      if (newItem == 0) {
        deliveryItems.put("electronics", deliveryItems.get("electronics") + 1);
      } else if (newItem == 1) {
        deliveryItems.put("books", deliveryItems.get("books") + 1);
      } else if (newItem == 2) {
        deliveryItems.put("medicines", deliveryItems.get("medicines") + 1);
      } else if (newItem == 3) {
        deliveryItems.put("clothes", deliveryItems.get("clothes") + 1);
      } else if (newItem == 4) {
        deliveryItems.put("tools", deliveryItems.get("tools") + 1);
      }
    }

    /*for (String item : deliveryItems.keySet()) {
      System.out.println(item + ": " + deliveryItems.get(item));
    }*/
  }

}