import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class Main {
  public static void main(String[] args) {

    TaskManager single_threaded = new TaskManager("single_threaded");

    System.out.println("Starting threads");

    ExecutorService threadExecutor = Executors.newFixedThreadPool(1);

    threadExecutor.execute(single_threaded);
    threadExecutor.shutdown();

    System.out.println( "Threads started, main ends\n" );

  }
}