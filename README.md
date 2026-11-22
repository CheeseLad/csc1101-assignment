# **CSC1101 Assignment Design Document**

*Group AJ: Adrian Szydlo & Jake Farrell*

## 1\. List of Working Functionality

| Description | Status | Note |
| :---- | :---- | :---- |
| Delivery of 10 boxes with 0.1 probability every tick | Working | Modifiable in config.properties |
| More than 1 stocker thread | Working | Modifiable in config.properties |
| More than 1 picker thread | Working | Modifiable in config.properties |
| 5 sections by default (5 boxes starting off) | Working | Modifiable in config.properties |
| Real-time reporting/logging in the correct format | Working | N/A |
| Variable to adjust how long in ms a tick is | Working | Modifiable in config.properties |
| Prioritisation of stocking boxes | Working | Sections prioritised in current size ascending order (empty \-\> full) |
| Limited section capacity | Working | Modifiable in config.properties |
| Breaks by stocker | Working | Modifiable in config.properties |
| Adjustable amount of trolleys (K) | Working | Modifiable in config.properties |
| Stockers use trolleys | Working | Stockers can never take last trolley if no Pickers have a trolley to ensure system progress |
| Pickers use trolleys | Working | Pickers can never take last trolley if no Stockers have a trolley to ensure system progress |

## 2\. Division of Work

| Adrian | Jake |
| :---: | :---: |
| Deliverer, StagingArea, Stocker, Ticker, TrolleyPool, Testing | Initial Layout, Picker, Sections, Design Document, Logging, Configuration File |

## 3\. Running the Code

To compile and run the code (on Linux/Ubuntu), change directory into “code” and run “javac \*.java” to compile all of the .java files into .class files. Update any configuration parameters you would like to change in “config.properties” and run the program with “java Main”

**Configuration File Details**

Inside the “code” directory, you will find a file called “config.properties”, containing multiple key-value pair entries allowing you to change various aspects of the simulation, without the need to recompile for each execution. The configuration file has extra information about what the default values are. Here are the main configurations:

* **DELIVERERS\_COUNT**, **STOCKERS\_COUNT**, **PICKERS\_COUNT** \- How many threads there are for each object.  
* **CATEGORIES** \- Comma-separated list of the different sections  
* **DELIVERY\_SIZE** \- The amount of items that will be delivered with each delivery (which sections the items are from are randomised)  
* **TROLLEY\_POOL\_SIZE** \- An adjustable parameter that lets you control the amount of trolleys available  
* **SECTION\_CAPACITY** \- How many boxes a section can hold until it is full  
* **INITIAL\_SECTION\_SIZES** \- How many boxes there will be in each section when the simulation starts  
* **STOCKER\_INVENTORY\_SIZE** \- How many boxes can a Stocker hold at once  
* **STOCKER\_BREAK\_LENGTH** \- How long a Stocker takes a break for  
* **TICK\_TIME\_SIZE** \- How long each tick will last (in ms)  
* **TOTAL\_RUNTIME\_TICKS** \- How long the program will run for (in ticks)  
* **SEED** \- Allows you to replicate random behaviour  
* **DELIVERY\_PROBABILITY** \- The probability that a delivery will happen every tick   
* **PICKER\_PROBABILITY** \- The probability that a picker will pick every tick

## 4\. Considered Tasks/Dependencies

**General Idea.** Deliverers, Stockers and Pickers must be individual threads operating on the Staging Area and Section shared resources. The operations must be tick-centric based on a central “clock” that times the running of the otherwise asynchronous simulation.

**Delivering.** Deliverers supply the Staging Area with a set number of random boxes (chance to occur per tick). Stockers must not starve the Deliverer from accessing the Staging Area, justifying a lock priority for the producer in this Producer-Consumer relationship.

**Stocking.** Stockers must acquire a trolley before taking boxes from the Staging Area and not release the trolley until their inventory is empty. They must prioritise Deliver lock requests for the Staging Area and ensure mutually exclusive access to it. They must intelligently determine what to pick from the Staging Area (if there are boxes available) to fill the sections starved of boxes while also not ending up in a situation where their inventory contains no space for more “valid” Boxes (otherwise they hold their trolley indefinitely, making no progress). Stocker must then move to a section, prioritising emptier ones, stocking it if they acquire the section lock, granted the section is not full. Stockers must also take occasional breaks, stopping what they are doing, releasing held locks, and resuming the task after the break ends.

**Picking.** Pickers must acquire a trolley, start a pick attempt at a random section, only picking when the section is not being stocked and has boxes, then releasing the trolley when the pick is successful.

**Sections.** Pickers must share a Reentrant Lock between each other when reading (and picking from) Sections. This access is mutually exclusive from a Stocker’s Section locking (and between Stockers), hence implying a sort of Read-Write Lock.

**Locking.** Worker actions must not be blocked as they must all complete within the tick period, hence lock acquisitions must be single attempts, not blocking waits, with subsequent retries on following ticks. The only blocking must occur with the core Ticker logic to ensure ticks are of the correct interval.

**Trolleys.** The trolley shared resource must never be fully acquired by the Stocker or Picker completely, i.e. no trolley monopolies or there will be no system progress.

## 

## 5\. Patterns/Strategies Used to Manage Concurrency 

**Fixed Thread Pool.** All Warehouse Workers were submitted into a Fixed Thread Pool within the Ticker class that manages their execution. This was done for easier thread management, where the number of threads equals the number of workers.

**Scheduled Executor Service.** A single-threaded scheduled executor was implemented within the Ticker class to execute its advance() function every tick duration that tracks ticks and advances all Worker threads, ensuring consistent timing across threads rather than relying on decentralised tick checking that could risk invalid asynchronous behaviour.

**Locks.** The simulation mainly uses non-blocking locks. The only blocking lock exists inside the Ticker class to ensure atomic access to tick checks. Here, a tick lock condition is also implemented for Worker threads to wait until the next tick is ready so we maintain consistent tick intervals (found in Worker’s run() function). Essentially, if a Worker is finished, they wait until the next tick, then call its respective advance() function to do the next action. The Staging Area and Sections utilise a ReentrantLock and a ReentrantReadWriteLock respectively, both of which are fair. The reentrant property ensures that if a Worker somehow returns before releasing their lock in the current tick, this means they are still using that resource for the next tick, ensuring they continue performing their action on that resource until completed (see Status-Dirven Actions below). In the case of the Staging Area, this ensures that operations are mutually exclusive, such as a Deliverer adding all delivered boxes during a delivery before a Stocker can perform any taking. A Read-Write lock for each  section ensures that if a Stocker (writer) stocks a section, no other Stocker or Picker can access the section. Alternatively, if a Picker (reader) is picking from a section, no other Stocker should be stocking to that section but other Pickers can pick from the section.

**Deliverer Lock Priority.** A volatile delivererWaiting boolean is stored in the Staging Area. This is checked by Stockers before they attempt to acquire the Staging Area lock, making them quit for this tick and yield the Staging Area for the Deliverer. The volatile property helps to provide cross-thread visibility of this check. *Arguably, this can be a case where a blocking wait can occur instead of a Stocker dropping their action for the tick.*

**Status-Driven Actions.** Each Worker has its own statuses that help it understand what should be done in the current tick. Statuses are checked inside of each Worker’s advance() method and updated inside of the action logic. For example, when a Stocker acquires the Staging Area lock, their status becomes TAKING and they know not to try and re-acquire the lock again on subsequent ticks for as long as they are TAKING. These statuses in particular are very useful for debugging and potential logging.

**Concurrent Data Structures.** Despite the locking logic, we still needed to ensure that the data structures for the Staging Area, Sections and Stocker inventory are thread-safe. Uses of, say, a LinkedList inside of the Staging Area, while not causing apparent missing or duplicated boxes, has caused strange behaviour such as a Stocker attempting to stock from an empty Staging Area for several ticks despite a check being implemented to prevent this, suggesting a stale Staging Area size being passed during that check. A ConcurrentLinkedQueue helped to prevent any unexpected behaviours and invalid logging in such cases. Similarly, as a preventative measure, we opted for a ConcurrentHashMap rather than the standard HashMap for a Stocker inventory. Then, why not just rely on these data structures and not bother with locking? This is to ensure that, say, two Stockers attempting to access the Staging Area do not get 5 items each of a 10 item delivery (which is what we observed for the ConcurrentLinkedQueue-only setup). Rather, we want one Worker to have mutually exclusive access to the resource, complete their action, and only then can another one access the resource.

**Java “Synchronized” Keyword**

Synchronised is used when incrementing the pick\_id or getting the total number of picks to ensure the pick\_id increments properly across threads. When we first tried to implement the pick\_ids without synchronisation we noticed that for 3 picker threads running, the number of pick attempts would be triple the supposed value\! After implementing synchronisation this issue went away. It’s also used in the trolley system when acquiring and releasing trolleys to ensure that only one trolley can be taken or returned at once, keeping the count of available trolleys consistent.

## 

## 6\. Description of How Our Solution Addresses Issues

**Fairness**

When configuring our ReentrantLocks, we made sure to enable the fair boolean which keeps the ordering policy fair.

**Preventing Deadlocks**

In all cases, we aimed to avoid Deadlocks by attempting to invalidate or avoid one of the conditions necessary for a deadlock. For mutually exclusive resources, a non-blocking acquisition of relevant locks is performed to avoid waiting for a resource that is not available in the current tick. (If the resource is not available now, try on the next tick.) Below are some examples of relevant issues we ran into:

**Case:** All Stockers own inventory for only max capacity sections, while all Pickers wait at empty sections.

**Solution:** This Circular Wait is avoided by making Stockers intelligently take boxes from the staging area for Sections that are empty or have only partial stock first. Stockers also prioritise going to sections based on ascending order of current stock (empty sections first).

**Case:** All trolleys are owned by Pickers who wait for Stockers to stock the sections. No stocker can acquire new trolleys to take from the staging area and stock the sections.

**Solution:** This Circular Wait (and Hold and Wait) is avoided by capping the amount of trolleys any side may have to invalidate a monopoly of trolleys. During our testing, we limited this to ¾ of the trolley pool but later tested it for other values (see Appendix).

**Case:** Worker acquires a lock but an internal check or event disallows it from performing its intended action (e.g. Stocker takes a break).

**Solution:** No Preemption is avoided by ensuring that all possible states lead to an acquired lock being released when the action is no longer possible. For example, Stockers taking a break release all held locks (but of course keep the trolleys).

**Case:** Stocker has inventory available for two sections that are full and hops between them indefinitely.

**Solution:** This livelock is solved by a Stocker intelligently detecting that all sections it can stock are full, making it return to the staging area to resupply.

**Prevention of Starvation**

For Stocker-Deliverer interactions, we ensured that the Deliverer always gets prioritised regardless of how many Stockers are waiting at the Staging Area by using a volatile variable tracking whether the Deliverer is waiting to deliver. Without this, deliveries failed as Stockers constantly took the lock before a Deliverer could.

Stocker’s prioritisation of empty sections when stocking ensures Pickers waiting at empty sections are not starved indefinitely.

**Ensuring System Continues to Make Progress**

Beyond avoiding typical deadlocks and starvation, the system treats the ticks as the main timing despite its asynchronous nature by making all lock acquisitions non-blocking so a thread does not wait indefinitely for a resource. The only blocking wait (with a Condition) in the system exists for the main Worker loop that communicates with the Ticker. This combination means that threads are ready for the next tick as soon as possible without blocking, and only waiting for the next Ticker signal to advance to the next tick (when the Scheduler Service in the Ticker calls the core advance() method).

In the event of a problem happening in the system, any functions that utilise locks implement the *finally* clause where applicable, which ensures that the lock is released at the end of the function call no matter what happens in the program, ensuring progress continues. In all cases, where a shared resource is no longer needed by a Worker, we ensure the lock is released.

A status approach ensured that all workers know what to do on each tick iteration.

**Race Conditions**

To prevent race conditions from occurring when creating a new pick\_id (two pick attempts cannot share the same pick\_id), we used the synchronised keyword to ensure it increments properly across threads. Additionally, when a Picker successfully picked a box, we ensured it stores the current size of the section locally. Otherwise, multiple Pickers would read the same final size when reporting the stock\_left count, resulting in only one final stock being reported rather than a decremental count (e.g. \<5, 4, 3, 2, 1\> and not \<1, 1, 1, 1, 1\>)

Similarly, when attempting to acquire trolleys from the trolley pool, the acquire and release methods use Java’s synchronised keyword to ensure the trolley pool was updated prior to another thread accessing it. *This is arguably an exception in the otherwise non-blocking approach to thread resource acquisition attempts but it proved functional and simplest to implement.*

## 7\. Appendix 

### 7.1 Example Simulation Results

The below test some sample scenarios as proof of work. *Note that Trolleys=0 actually represents the default trolley value, not 0 trolleys (see config.properties).*

| ![][image1] | ![][image2] |
| :---- | :---- |
| ![][image3] | ![][image4] |

Note the weaker Picker Average for the last test case where Section Capacity is only 10 and available trolleys are capped at 4, with delivery size of 50\. Unfortunately, the Stockers are unable to keep up with these restraints, but progresses regardless. 

All above test cases assume either Stockers or Pickers can own at most ¾ of the total available trolleys. Further tests were carried out into this as seen in the Appendix below.

### 7.2 Experimenting with Trolley Pool Limits

Below are some additional tests performed for different Trolley Limit strategies where we test different ways to limit the amount of trolleys either Stockers or Pickers can have at a given time. Firstly, we tested always guaranteeing at least one trolley is available, then we tested neither side being able to acquire over half the trolley pool, and finally we tested neither side being able to acquire over three quarters of the trolley pool.

| ![][image5] Never own over ¾ trolleys | ![][image6] Never own over ½ trolleys | ![][image7] Ensure at least 1 free trolley |
| :---: | :---: | :---: |

As can be seen, for a situation with 3 Stockers and 10 Pickers, it seems that the last strategy of leaving 1 spare trolley seems best due to the highest average pick rate. However, running another test to compare to a larger Worker count, we found the following:

| ![][image8] Never own over ¾ trolleys | ![][image9] Never own over ½ trolleys | ![][image10] Ensure at least 1 free trolley |
| :---: | :---: | :---: |

Interestingly, this demonstrates that the strategy does not offer much difference in larger Worker sample sizes despite our initial assumption that the ¾ trolley strategy would work best. To note, given the concurrent nature of the system, it is impossible to provide accurate and replicable results for this experiment.

[image1]: images/image1.png

[image2]: images/image2.png

[image3]: images/image3.png

[image4]: images/image4.png

[image5]: images/image5.png

[image6]: images/image6.png

[image7]: images/image7.png

[image8]: images/image8.png

[image9]: images/image9.png

[image10]: images/image10.png
