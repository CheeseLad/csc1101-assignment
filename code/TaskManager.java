// import java.util.Random;
// import java.util.ArrayList;

// class TaskManager implements Runnable {
//   private int sleepTime;
//   private String threadName;
//   private static Random generator = new Random();
//   private int daily_limit = 10;
//   private ArrayList<oldSection> sections;
//   private Random randgen;

//   public  TaskManager(String name, int daily_limit, ArrayList<oldSection> sections, Random randgen) {
//     threadName = name;
//     sleepTime = generator.nextInt(50) + 50; // Random sleep time between 50 and 100 ms
//     this.daily_limit = daily_limit;
//     this.sections = sections;
//     this.randgen = randgen;
    
//   }

//   public void run() {
//     try {
//       System.out.printf("Thread %s running\n", threadName);
//       Util util = new Util();
//       // TODO: Implement tick behaviour
//       Picker picker = new Picker();

//       picker.startPicking(daily_limit, randgen, sections, util, threadName);



//       /*for (int i = 0; i < 11; i++) {
//         String currentSection = picker.pickBox(warehouse);
//         if (currentSection != null) {
//           util.printPick(123, "PIC", "pick_start", i, currentSection);
//         }
//       }*/

//       System.out.printf("%s sleeps for %d ms.\n", threadName, sleepTime);
//       Thread.sleep(sleepTime);
//     }

//     catch (InterruptedException exception) {
//       exception.printStackTrace();
//     }
//       System.out.printf( "%s done sleeping\n", threadName );
//     }
// }