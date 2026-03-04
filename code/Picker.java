// import java.util.ArrayList;
// import java.util.Random;

// public class Picker {

//   private Box currentBox;
  
//   public Picker() {
//     this.currentBox = null;
//   }

//   public void startPicking(int daily_limit, Random randgen, ArrayList<oldSection> sections, Util util, String threadName) {
//     while (true) {
//       daily_limit--;
//         int randomCategory = randgen.nextInt(5);


//         String currentSection = pickBoxFromSection(sections.get(randomCategory));

//         if (currentSection != null) {
//           util.printPick(140, threadName, "pick_start", daily_limit, currentSection);
//         }
//         if (daily_limit <= 0) {
//           break;
//         }
//     }
//   }

//   public void attemptPick(oldSection section) {
//     boolean picked = false;
//     while (!picked) {
//       continue; // Simulate waiting for a box to be available
//     }
//   }

//   public String pickBoxFromSection(oldSection section) {
//       //long programStart = System.currentTimeMillis();
//       Box box = section.removeBox();
//       if (box != null) {
//           //System.out.printf("Time since program start: %d ms\n", System.currentTimeMillis() - programStart);
//           this.currentBox = box;
//           //long programEnd = System.currentTimeMillis();
//           //System.out.printf("Time taken to pick box: %d ms\n", programEnd - programStart);
//           return box.getCategory();
//         } else {
//         //System.out.println("No boxes available in section.");
//         return null;
//       }
//   }
// }