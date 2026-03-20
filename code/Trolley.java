import java.util.concurrent.Semaphore;

public class Trolley {

  int K;
  final Semaphore availableTrollies;

	public Trolley(int K) {
    this.K = K;
    this.availableTrollies = new Semaphore(K);
	}

  public int acquireTrolley() throws InterruptedException {
      availableTrollies.acquire();
      return availableTrollies.availablePermits();
  }

  public void releaseTrolley() throws InterruptedException {
      availableTrollies.release();
  }
}