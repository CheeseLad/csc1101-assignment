import java.util.ArrayList;
import java.util.Random;

public class Delivery {

  private ArrayList<Box> deliveryItems;
  //private long seed = 12345L;

  public ArrayList<Box> getDeliveryItems() {
    return deliveryItems;
  }

  public Delivery(String[] categories) {

    Random randgen = new Random(); //  set a seed to replicate behaviour
    //Random randgen = new Random(seed); //  set a seed to replicate behaviour

    deliveryItems = new ArrayList<>();

    for (int i = 0; i < 500; i++) {
      int randomCategory = randgen.nextInt(5);
      Box box = new Box(categories[randomCategory]);
      deliveryItems.add(box);

    }
  }
}