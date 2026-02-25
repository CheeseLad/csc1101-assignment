import java.util.ArrayList;

public class Section {
  
  private ArrayList<Box> boxes;
  private String sectionCategory;

  public Section(String sectionCategory) {
    this.sectionCategory = sectionCategory;
    this.boxes = new ArrayList<>();
  }

  public void addBox(Box box) {
    synchronized (this) {
      if (box.getCategory().equals(sectionCategory)) {
        boxes.add(box);
      } else {
        System.out.println("[ERROR] Box category does not match section category.");
      }
    }
  }

  public  Box removeBox() {
    long startWait = System.nanoTime();

    synchronized (this) {

        long waitedMs = (System.nanoTime() - startWait) / 1_000_000;
        System.out.println("Waited for lock: " + waitedMs + " ms");

      if (!boxes.isEmpty()) {
        return boxes.remove(0);
      } 

      return null;
    }
  }

  public String getCategory() {
    return sectionCategory;
  }

  public Section getSectionByCategory(String category) {
    if (category.equals(sectionCategory)) {
      return this;
    }

    return null;
  }
}