import java.util.ArrayList;

public class Section {
  
  private ArrayList<Box> contents;
  private String sectionCategory;

  public Section(String sectionCategory) {
    this.sectionCategory = sectionCategory;
    this.contents = new ArrayList<>();
  }

  public void addBox(Box box) {
    if (box.getCategory().equals(sectionCategory)) {
      contents.add(box);
    } else {
      System.out.println("[ERROR] Box category does not match section category.");
    }
  }

  // TO-DO: Revisit
  public Section getSectionByCategory(String category) {
    if (category.equals(sectionCategory)) {
      return this;
    }
    return null;
  }
}