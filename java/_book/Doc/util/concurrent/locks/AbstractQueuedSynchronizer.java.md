# AbstractQueuedSynchronizer
``` java
/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

/*
 *
 *
 *
 *
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166 // assistance 协助
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent.locks;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import sun.misc.Unsafe;

// 这里引入了一个Unsafe，是以前没看过的，等看完这个再看

/**
 * Provides a framework for implementing blocking locks and related
 * synchronizers (semaphores, events, etc) that rely on
 * first-in-first-out (FIFO) wait queues.  This class is designed to
 * be a useful basis for most kinds of synchronizers that rely on a
 * single atomic {@code int} value to represent state. Subclasses
 * must define the protected methods that change this state, and which
 * define what that state means in terms of this object being acquired
 * or released.  Given these, the other methods in this class carry
 * out all queuing and blocking mechanics. Subclasses can maintain
 * other state fields, but only the atomically updated {@code int}
 * value manipulated using methods {@link #getState}, {@link
 * #setState} and {@link #compareAndSetState} is tracked with respect
 * to synchronization.
 * 提供一个框架，用于实现依赖FIFO等待队列的阻塞锁和相关（related）同步器（semaphores、events等）。
 * 这个类被设计成一个有用的基本类，对于大多数依赖单个原子int值代表状态的同步器。
 * 子类必须定义用来改变状态的受保护方法，并定义该状态在获取/释放这个对象时的含义。
 * 鉴于这些，此类中的其他方法执行（carry out）所有排队和阻塞机制。
 * 子类可以维护（maintain）其他状态字段，但是只有使用getState、setState和compareAndSetState方法来操纵（manipulated）原子性的更新int值，才会在同步方面进行跟踪
 *
 * <p>Subclasses should be defined as non-public internal helper
 * classes that are used to implement the synchronization properties
 * of their enclosing class.  Class
 * {@code AbstractQueuedSynchronizer} does not implement any
 * synchronization interface.  Instead it defines methods such as
 * {@link #acquireInterruptibly} that can be invoked as
 * appropriate by concrete locks and related synchronizers to
 * implement their public methods.
 * 子类应该被定义为非公共的内部帮助类，用于实现其封闭类的同步属性。（就是AQS的子类都应该作为[想要FIFO同步属性的]类的内部类使用，就像ReentrantLock里面的Sync类）
 * AbstractQueuedSynchronizer类没有实现任何同步接口。
 * 相反，它定义了例如acquireInterruptibly等方法，可以被有关（concrete）锁与相关同步器适当调用来实现他们的公共方法。
 *
 * <p>This class supports either or both a default <em>exclusive</em>
 * mode and a <em>shared</em> mode. When acquired in exclusive mode,
 * attempted acquires by other threads cannot succeed. Shared mode
 * acquires by multiple threads may (but need not) succeed. This class
 * does not &quot;understand&quot; these differences except in the
 * mechanical sense that when a shared mode acquire succeeds, the next
 * waiting thread (if one exists) must also determine whether it can
 * acquire as well. Threads waiting in the different modes share the
 * same FIFO queue. Usually, implementation subclasses support only
 * one of these modes, but both can come into play for example in a
 * {@link ReadWriteLock}. Subclasses that support only exclusive or
 * only shared modes need not define the methods supporting the unused mode.
 * 这个类支持独占（exclusive）模式与共享（shared）模式中的一种或者两种。
 * 在独占模式中获取（可以理解为加锁）时，其他线程获取不会成功。
 * 在共享模式中多个线程获取可能（但是不一定）成功。
 * 该类不理解这些不同点，除了在机械意义上说，当共享模式获取成功时，下一个等待线程（如果有一个的话）必须确定它是否也可以获取。
 * 在不同模式下等待的线程们共享同一个FIFO队列。（意味着一个AQS可以同时实现两种模式，就开头第一句话）。
 * 通常，子类实现时只支持其中一种模式，但是两种都可以起到作用，例如在ReadWriteLock。
 * 仅支持独占或者共享模式的子线程不需要实现支持未使用模式的方法（就是两种模式会有两套方法，如果只实现一种模式，只选择一套方法实现就行）（这里也可以解释为什么两套方法都不是abstract方法）。
 *
 * <p>This class defines a nested {@link ConditionObject} class that
 * can be used as a {@link Condition} implementation by subclasses
 * supporting exclusive mode for which method {@link
 * #isHeldExclusively} reports whether synchronization is exclusively
 * held with respect to the current thread, method {@link #release}
 * invoked with the current {@link #getState} value fully releases
 * this object, and {@link #acquire}, given this saved state value,
 * eventually restores this object to its previous acquired state.  No
 * {@code AbstractQueuedSynchronizer} method otherwise creates such a
 * condition, so if this constraint cannot be met, do not use it.  The
 * behavior of {@link ConditionObject} depends of course on the
 * semantics of its synchronizer implementation.
 * 该类定义了一个嵌套（nest）的ConditionObject对象，可以被支持独占模式的子类用来作为Condition的实现，其中：
 * isHeldExclusively方法报告是否针对当前线程独占同步，
 * 使用当前getState值调用release方法完全释放这个对象？？？
 * 和acquire，给定保存状态值，最终将这个对象恢复到它之前获取的状态。（看不懂，等下看源码再看看是啥意思）
 * 没有AbstractQueuedSynchronizer方法否则创建这样的condition，所以如果这些约束不能被满足，不要使用它。
 * ConditionObject的行为当然取决于其同步器实现的语义。
 *
 * <p>This class provides inspection, instrumentation, and monitoring
 * methods for the internal queue, as well as similar methods for
 * condition objects. These can be exported as desired into classes
 * using an {@code AbstractQueuedSynchronizer} for their
 * synchronization mechanics.
 * 该类提供对内部队列的检查、监测和监控的方法，对condition对象也有同样的方法。
 * 可以根据需要导出到类中，使用AQS作为他们的同步机制。
 *
 * <p>Serialization of this class stores only the underlying atomic
 * integer maintaining state, so deserialized objects have empty
 * thread queues. Typical subclasses requiring serializability will
 * define a {@code readObject} method that restores this to a known
 * initial state upon deserialization.
 * 该类的序列化只存储底层的用原子interger维护的状态，所以反序列化对象有空的线程队列（或者翻译为线程队列为空）。
 * 需要序列化的典型子类将定义readObject方法，在反序列化时将其恢复（restores）到已知（known）的初始化状态。
 *
 * <h3>Usage</h3>
 * 用法
 *
 * <p>To use this class as the basis of a synchronizer, redefine the
 * following methods, as applicable, by inspecting and/or modifying
 * the synchronization state using {@link #getState}, {@link
 * #setState} and/or {@link #compareAndSetState}:
 * 使用该类作为同步器基础，根据适应性（applicable）情况重新定义下面几个方法（5个方法），可以使用getState、setState与compareAndSetState来检查（inspect）和修改同步器状态。
 *
 * 下面这几个方法很关键，是实现类唯一能改的5个方法
 * <ul>
 * <li> {@link #tryAcquire}
 * <li> {@link #tryRelease}
 * <li> {@link #tryAcquireShared}
 * <li> {@link #tryReleaseShared}
 * <li> {@link #isHeldExclusively}
 * </ul>
 *
 * Each of these methods by default throws {@link
 * UnsupportedOperationException}.  Implementations of these methods
 * must be internally thread-safe, and should in general be short and
 * not block. Defining these methods is the <em>only</em> supported
 * means of using this class. All other methods are declared
 * {@code final} because they cannot be independently varied.
 * 上面的每个方法默认抛出UnsupportedOperationException异常。
 * 这些方法的实现必须是内部线程安全的，通常应该是简短并且不会阻塞的。
 * 定义这些方法，是使用该类唯一支持的方法（means）（mean是意味着）
 * 所有的其他方法都声明为final，因为他们不能独立变化（varied）
 * （就是除了上面这五个方法可以重新定义，其他方法都改不了）
 *
 * <p>You may also find the inherited methods from {@link
 * AbstractOwnableSynchronizer} useful to keep track of the thread
 * owning an exclusive synchronizer.  You are encouraged to use them
 * -- this enables monitoring and diagnostic tools to assist users in
 * determining which threads hold locks.
 * 从AbstractOwnableSynchronizer继承的方法对追踪独占同步器的线程很有用。
 * 鼓励使用这些方法 -- 监控和诊断工具能够去帮助使用者确定哪些线程持有锁
 *
 * <p>Even though this class is based on an internal FIFO queue, it
 * does not automatically enforce FIFO acquisition policies.  The core
 * of exclusive synchronization takes the form:
 * 即使该类基于内部的FIFO队列，它也不会自动执行FIFO获取策略。
 * 独占同步的核心形式为：
 *
 * <pre>
 * Acquire:
 *     while (!tryAcquire(arg)) { // 获取失败会循环
 *        <em>enqueue thread if it is not already queued</em>; // 如果没有排队，则线程排队
 *        <em>possibly block current thread</em>; // 可能阻塞当前线程
 *     }
 *
 * Release:
 *     if (tryRelease(arg)) // 释放成功
 *        <em>unblock the first queued thread</em>; // 第一个队列线程被唤醒
 * </pre>
 *
 * (Shared mode is similar but may involve cascading signals.)
 * 共享模式类似，不过可能会涉及级联信号
 *
 * 非公平锁与公平锁的大致实现
 * <p id="barging">Because checks in acquire are invoked before
 * enqueuing, a newly acquiring thread may <em>barge</em> ahead of
 * others that are blocked and queued.  However, you can, if desired,
 * define {@code tryAcquire} and/or {@code tryAcquireShared} to
 * disable barging by internally invoking one or more of the inspection
 * methods, thereby providing a <em>fair</em> FIFO acquisition order.
 * In particular, most fair synchronizers can define {@code tryAcquire}
 * to return {@code false} if {@link #hasQueuedPredecessors} (a method
 * specifically designed to be used by fair synchronizers) returns
 * {@code true}.  Other variations are possible.
 * 因为在入队之前会调用acquire进行检查，新的发起acquire的线程可能抢（barge ahead of抢先）在其它阻塞和在队列里的线程之前（拿到锁）。
 * 但是，如果需要，定义tryAcquire和/或tryAcquireShared以通过内部调用一个或多个检查方式来禁止抢占（插队），从而提供公平（fair）的FIFO获取顺序。
 * 特别的，如果hasQueuePredecessors（一个在公平同步器中使用的特定设计的方法）返回true，大多数公平同步器可以定义tryAcquire返回false。（就是等待队列不为空，就不允许插队）
 * 其他变化（variations）也是可能的。
 *
 * <p>Throughput and scalability are generally highest for the
 * default barging (also known as <em>greedy</em>,
 * <em>renouncement</em>, and <em>convoy-avoidance</em>) strategy.
 * While this is not guaranteed to be fair or starvation-free, earlier
 * queued threads are allowed to recontend before later queued
 * threads, and each recontention has an unbiased chance to succeed
 * against incoming threads.  Also, while acquires do not
 * &quot;spin&quot; in the usual sense, they may perform multiple
 * invocations of {@code tryAcquire} interspersed with other
 * computations before blocking.  This gives most of the benefits of
 * spins when exclusive synchronization is only briefly held, without
 * most of the liabilities when it isn't. If so desired, you can
 * augment this by preceding calls to acquire methods with
 * "fast-path" checks, possibly prechecking {@link #hasContended}
 * and/or {@link #hasQueuedThreads} to only do so if the synchronizer
 * is likely not to be contended.
 * 在默认抢占策略（又称greedy，renouncement和convoy-avoidance）下，吞吐量（throughput）和可扩展性（scalability）最高。
 * 虽然这样不能保证公平或者没有饥饿，但是允许较早的排队进程在较晚的排队线程之前重新竞争（recontend），并且每次重新竞争都有平等的机会成功对抗新来的线程。
 *（意思就是排在队首的线程一定在队列里的其他线程之前进行锁竞争，并且队首线程与新来的还未入队的线程竞争锁具有平等的概率）
 * 虽然acquires在通常意义上不会自旋（自旋就是重复操作直到某个状态退出，类似于while(cond){...}），但他们可能会在阻塞之前执行多次调用tryAcquire并穿插其他计算。
 * 在独占同步只短暂持有时，提供的这种自旋的方式具有很大的好处；如果不是，也没有多大的坏处。（如果锁占有时间短暂，可能在自旋过程中就能拿到锁，减少了阻塞再唤醒的消耗）
 * 如果有需求，你可以通过使用“快速路径”检查来预先调用acquire方法以增强这一点，可能预先检查hasContended 和/或hasQueuedThreads方法，以仅在如果同步器很可能没有竞争时才这样做。
 * （预先检查有没有竞争的情况，如果有不竞争的可能性，就通过自旋的方式来尝试获取锁）
 *
 * <p>This class provides an efficient and scalable basis for
 * synchronization in part by specializing its range of use to
 * synchronizers that can rely on {@code int} state, acquire, and
 * release parameters, and an internal FIFO wait queue. When this does
 * not suffice, you can build synchronizers from a lower level using
 * {@link java.util.concurrent.atomic atomic} classes, your own custom
 * {@link java.util.Queue} classes, and {@link LockSupport} blocking
 * support.
 * 该类对于部分是通过将其使用范围专门用于依赖int状态、获取和释放参数以及内部FIFO等待队列的同步器，从而为同步提供有效（efficient）和可扩展的基础。
 * 如果这不能满足（suffice），可以通过使用atomic类、自定义Queue类和LockSupprot阻塞支持从较低级别构建同步器
 *
 * <h3>Usage Examples</h3>
 * 使用样例
 *
 * <p>Here is a non-reentrant mutual exclusion lock class that uses
 * the value zero to represent the unlocked state, and one to
 * represent the locked state. While a non-reentrant lock
 * does not strictly require recording of the current owner
 * thread, this class does so anyway to make usage easier to monitor.
 * It also supports conditions and exposes
 * one of the instrumentation methods:
 * 这里是一个不可重入的互斥锁class，用0代表解锁状态，用1代表加锁状态。
 * 虽然不可重入锁不严格要求记录当前用有锁的线程，但这个类无论如何这样做是为了让使用更容易监控。
 * 它还支持条件并公开一种检测方法：
 *
 *  <pre> {@code
 * class Mutex implements Lock, java.io.Serializable { // mutex 是信号量
 *
 *   // Our internal helper class
 *   private static class Sync extends AbstractQueuedSynchronizer {
 *     // 继承AQS的，独占锁，得自己实现两个try方法（tryAcquire与tryRelease）与isHeldExclusively方法，因为AQS没有定义这仨实际操作。！！！！！！
 *     // Reports whether in locked state
 *     protected boolean isHeldExclusively() { // 当前同步器是否在独占模式下被线程占用，一般该方法表示是否被当前线程所独占
 *       return getState() == 1;
 *     }
 *
 *     // Acquires the lock if state is zero
 *     public boolean tryAcquire(int acquires) {
 *       assert acquires == 1; // Otherwise unused // assert是java的关键字--断言，如果表达式成立，则继续执行，否则抛出AssertionError，并终止执行。
 *       if (compareAndSetState(0, 1)) { // 使用CAS来设置加锁
 *         setExclusiveOwnerThread(Thread.currentThread());
 *         return true;
 *       }
 *       return false;
 *     }
 *
 *     // Releases the lock by setting state to zero
 *     protected boolean tryRelease(int releases) {
 *       assert releases == 1; // Otherwise unused
 *       if (getState() == 0) throw new IllegalMonitorStateException();
 *       setExclusiveOwnerThread(null);
 *       setState(0); // 不使用CAS，直接解锁
 *       return true;
 *     }
 *
 *     // Provides a Condition
 *     // 需要用到Conditionobject的，得自己写newCondition方法，AQS不提供这个方法，只提供ConditionObject这个内部类
 *     Condition newCondition() { return new ConditionObject(); }
 *
 *     // Deserializes properly // 正确反序列化
 *     private void readObject(ObjectInputStream s)
 *         throws IOException, ClassNotFoundException {
 *       s.defaultReadObject();
 *       setState(0); // reset to unlocked state // 重置为不加锁状态
 *     }
 *   }
 *
 *   // The sync object does all the hard work. We just forward to it.
 *   private final Sync sync = new Sync();
 *
 *   // 通过Sync实现Lock接口的一些功能
 *   public void lock()                { sync.acquire(1); }
 *   public boolean tryLock()          { return sync.tryAcquire(1); }
 *   public void unlock()              { sync.release(1); }
 *   public Condition newCondition()   { return sync.newCondition(); }
 *   public boolean isLocked()         { return sync.isHeldExclusively(); }
 *   public boolean hasQueuedThreads() { return sync.hasQueuedThreads(); }
 *   public void lockInterruptibly() throws InterruptedException {
 *     sync.acquireInterruptibly(1);
 *   }
 *   public boolean tryLock(long timeout, TimeUnit unit)
 *       throws InterruptedException {
 *     return sync.tryAcquireNanos(1, unit.toNanos(timeout));
 *   }
 * }}</pre>
 *
 * <p>Here is a latch class that is like a
 * {@link java.util.concurrent.CountDownLatch CountDownLatch}
 * except that it only requires a single {@code signal} to
 * fire. Because a latch is non-exclusive, it uses the {@code shared}
 * acquire and release methods.
 * 这里是一个类似于CountDownLatch的闩锁类，只需要单信号量就可以触发。
 * 因为latch是个非独占的，它使用共享的acquire与release方法
 *
 *  <pre> {@code
 * class BooleanLatch {
 *
 *   private static class Sync extends AbstractQueuedSynchronizer {
 *     // 用AQS实现共享锁，得自己实现tryAcquireShared和tryReleaseShared方法
 *     boolean isSignalled() { return getState() != 0; } // 是否有信号，state初始化为0
 *
 *     protected int tryAcquireShared(int ignore) {
 *       return isSignalled() ? 1 : -1;
 *     }
 *
 *     protected boolean tryReleaseShared(int ignore) {
 *       setState(1);
 *       return true;
 *     }
 *   }
 *
 *   private final Sync sync = new Sync();
 *   public boolean isSignalled() { return sync.isSignalled(); }
 *   public void signal()         { sync.releaseShared(1); } // 
 *   public void await() throws InterruptedException {  // 如果上来就调用await，那么因为state=0不满足条件，当前线程进入等待队列。如果现在state=1了，那么就不会阻塞直接执行
 *     sync.acquireSharedInterruptibly(1);
 *   }
 * }}</pre>
 *
 * @since 1.5
 * @author Doug Lea
 */
public abstract class AbstractQueuedSynchronizer
    extends AbstractOwnableSynchronizer // 用来记录当前独占锁的拥有者（拥有者是个Thread对象）
    implements java.io.Serializable {

    private static final long serialVersionUID = 7373984972572414691L;

    /**
     * Creates a new {@code AbstractQueuedSynchronizer} instance
     * with initial synchronization state of zero.
     * 初始化时，表示状态的state为0
     */
    protected AbstractQueuedSynchronizer() { }

    /**
     * Wait queue node class.
     * 等待队列节点类
     *
     * <p>The wait queue is a variant of a "CLH" (Craig, Landin, and
     * Hagersten) lock queue. CLH locks are normally used for
     * spinlocks.  We instead use them for blocking synchronizers, but
     * use the same basic tactic of holding some of the control
     * information about a thread in the predecessor of its node.  A
     * "status" field in each node keeps track of whether a thread
     * should block.  A node is signalled when its predecessor
     * releases.  Each node of the queue otherwise serves as a
     * specific-notification-style monitor holding a single waiting
     * thread. The status field does NOT control whether threads are
     * granted locks etc though.  A thread may try to acquire if it is
     * first in the queue. But being first does not guarantee success;
     * it only gives the right to contend.  So the currently released
     * contender thread may need to rewait.
     * 等待队列是CLH锁队列的变种（variant）。CLH锁通常用于自旋锁（spinlock）。
     * 我们改为将它用于阻塞同步器，但也用相同的基本策略（tactic），即在node的前驱（predecessor）中保存有关线程的一些控制信息
     * 每个节点中的“state”字段保持跟踪线程是否应该被阻塞。
     * 节点在其前驱解锁（releases）的时候收到信号（理解为唤醒）。
     * 队列中的每个node都充当一个特定通知式监视器，持有一个等待线程。（一个node里面包含了一个waiting线程对象）
     * 尽管status属性不会控制线程是否被授予锁。（status属性只是用来表名可以去竞争锁，不管会不会加锁成功）
     * 如果线程是队列里的第一个，它可能尝试去加锁（acquire）
     * 但是作为第一个不会保证一定能加锁成功；它只是被给予了去竞争的权利。（在unfair非公平锁里，队列的第一个线程要跟尚未入队的竞争线程一起竞争锁）
     * 所以当前释放的竞争者线程（也就是被唤醒的线程或者队列里的第一个线程）可能需要重新等待。
     *
     * <p>To enqueue into a CLH lock, you atomically splice it in as new
     * tail. To dequeue, you just set the head field.
     * CLH锁进队，需要原子性的将它拼接为新的尾部（tail）。
     * 出队，只需要设置它的头部（head）字段。
     * <pre>
     *      +------+  prev +-----+       +-----+
     * head |      | <---- |     | <---- |     |  tail
     *      +------+       +-----+       +-----+
     * </pre>
     *
     * <p>Insertion into a CLH queue requires only a single atomic
     * operation on "tail", so there is a simple atomic point of
     * demarcation from unqueued to queued. Similarly, dequeuing
     * involves only updating the "head". However, it takes a bit
     * more work for nodes to determine who their successors are,
     * in part to deal with possible cancellation due to timeouts
     * and interrupts.
     * 插入CLH队列只需要对“tail”进行一次原子性操作，所以从未入队到入队有一个简单的原子分界点。（入队仅经过一个原子性操作）
     * 类似的，出队涉及只更新“head”。
     * 然而，节点需要做更多的工作来确定他们的后继（successors）是谁，部分是为了处理由于超时和中断可能导致的取消。
     *
     * 下面会讲取消的问题
     * <p>The "prev" links (not used in original CLH locks), are mainly
     * needed to handle cancellation. If a node is cancelled, its
     * successor is (normally) relinked to a non-cancelled
     * predecessor. For explanation of similar mechanics in the case
     * of spin locks, see the papers by Scott and Scherer at
     * http://www.cs.rochester.edu/u/scott/synchronization/
     * “prev”连接（在原始CLH锁中未使用）主要用于处理取消。
     * 如果一个node被取消，它的后继（通常）会重新连接到一个未取消的前驱。（就是一个节点被取消了，那么这个节点应该从CLH链上删除，这时候就需要它的后继去重新找到未取消的前驱）
     * 有关自旋锁情况下的类似机制解释，请参阅链接的论文
     *
     * <p>We also use "next" links to implement blocking mechanics.
     * The thread id for each node is kept in its own node, so a
     * predecessor signals the next node to wake up by traversing
     * next link to determine which thread it is.  Determination of
     * successor must avoid races with newly queued nodes to set
     * the "next" fields of their predecessors.  This is solved
     * when necessary by checking backwards from the atomically
     * updated "tail" when a node's successor appears to be null.
     * (Or, said differently, the next-links are an optimization
     * so that we don't usually need a backward scan.)
     * 我们还使用“next”连接来实现阻塞机制。
     * 每个节点的线程ID保存在他们自己的node里，因此前驱信号是通过遍历next连接来确定它是哪个线程来通知唤醒下一个节点。？？？
     * 确定后驱节点必须避免与新排队节点竞争以设置其前驱节点的“next”字段。（就是设置其前驱节点的next值时不要与新入队的节点发生冲突）
     * 当一个节点的后驱出现空时，如果必要，从原子更新的“tail”向后检查来解决。（换句话说，next连接是一种优化（optimization），所以我们通常不需要向后扫描）？？？
     *
     * <p>Cancellation introduces some conservatism to the basic
     * algorithms.  Since we must poll for cancellation of other
     * nodes, we can miss noticing whether a cancelled node is
     * ahead or behind us. This is dealt with by always unparking
     * successors upon cancellation, allowing them to stabilize on
     * a new predecessor, unless we can identify an uncancelled
     * predecessor who will carry this responsibility.
     * 取消（Canellation）为基础算法引入了些保守性。由于我们必须轮询（poll）其他节点的取消，因此我们可能无法注意到取消的node是在我们之前还是之后。
     * 通过在取消时总是解除后继来处理的，允许他们稳定在一个新的前驱上，除非我们可以确定一个未取消的前驱将承担这个责任。
     *
     * <p>CLH queues need a dummy header node to get started. But
     * we don't create them on construction, because it would be wasted
     * effort if there is never contention. Instead, the node
     * is constructed and head and tail pointers are set upon first
     * contention.
     * CLH队列需要一个虚拟（dummy）头结点来启动。但是我们不会在构建时创建他们，因为如果没有从来没有竞争，这个虚拟头就是浪费。
     * 取而代之的是，在第一次竞争的时候，这个虚拟头节点将创建并设置head跟tail指针
     *
     * <p>Threads waiting on Conditions use the same nodes, but
     * use an additional link. Conditions only need to link nodes
     * in simple (non-concurrent) linked queues because they are
     * only accessed when exclusively held.  Upon await, a node is
     * inserted into a condition queue.  Upon signal, the node is
     * transferred to the main queue.  A special value of status
     * field is used to mark which queue a node is on.
     * 在Condition上等待的线程使用相同的node，不过使用额外的连接（就是说有一个是CLH主链（主链=主队列），还有一些是针对不同的Condition建立的不同的链）
     * Condition只需要连接在简单的（非并发）链接队列上的node，因为他们仅在独占时才会被访问。
     * 根据信号，node被转移到主队列上。（这个信号就是能够满足独占需求的信号，会将对应的Condition链转移到主队列上）
     * status字段的特殊值用来标记node所在的队列。
     *
     * <p>Thanks go to Dave Dice, Mark Moir, Victor Luchangco, Bill
     * Scherer and Michael Scott, along with members of JSR-166
     * expert group, for helpful ideas, discussions, and critiques
     * on the design of this class.
     * 大佬感谢大佬们的时间，不看了
     */
    static final class Node {
        /** Marker to indicate a node is waiting in shared mode */
        // 表明在共享模式下等待的node
        static final Node SHARED = new Node();
        /** Marker to indicate a node is waiting in exclusive mode */
        // 表明在独占模式下等待的node
        static final Node EXCLUSIVE = null;

        /** waitStatus value to indicate thread has cancelled */
        // 等待status值=1 表示线程取消
        static final int CANCELLED =  1;
        /** waitStatus value to indicate successor's thread needs unparking */
        // 等待status值=-1 表示后继的线程需要唤醒
        static final int SIGNAL    = -1;
        /** waitStatus value to indicate thread is waiting on condition */
        // 等待status值=-2 表示线程在等待条件（或者说线程在condition队列上）
        static final int CONDITION = -2;
        /**
         * waitStatus value to indicate the next acquireShared should
         * unconditionally propagate
         */
         // waitStatus值=-3 表示下一个acquireShared应该无条件传播
        static final int PROPAGATE = -3;

        /**
         * Status field, taking on only the values:
         *   SIGNAL:     The successor of this node is (or will soon be)
         *               blocked (via park), so the current node must
         *               unpark its successor when it releases or
         *               cancels. To avoid races, acquire methods must
         *               first indicate they need a signal,
         *               then retry the atomic acquire, and then,
         *               on failure, block.
         *               当前节点的后继节点是被阻塞的，所以当前节点在释放或者取消的时候，必须unpark他的后继节点。
         *               为了避免竞争，加锁方法必须首先声明他们需要一个信号，然后重试原子操作的加锁，然后在失败时阻塞。
         *   CANCELLED:  This node is cancelled due to timeout or interrupt.
         *               Nodes never leave this state. In particular,
         *               a thread with cancelled node never again blocks.
         *               当前节点由于超时或者中断被取消。
         *               节点从来不会离开这个状态。特别是，取消节点的线程永远不会再阻塞。
         *   CONDITION:  This node is currently on a condition queue.
         *               It will not be used as a sync queue node
         *               until transferred, at which time the status
         *               will be set to 0. (Use of this value here has
         *               nothing to do with the other uses of the
         *               field, but simplifies mechanics.)
         *               当前节点在条件队列中。
         *               它在传输之前不会用作同步节点，到那个时候status将被设置为0。（在这里使用这个值和这个字段的其他用法无关，但是简化了机制）
         *   PROPAGATE:  A releaseShared should be propagated to other
         *               nodes. This is set (for head node only) in
         *               doReleaseShared to ensure propagation
         *               continues, even if other operations have
         *               since intervened.
         *               共享锁的释放（releaseShared）应当传播到其他节点。
         *               这在doReleaseShared中设置（仅适用于头节点）以确保传播继续，即使其他的操作已经介入。
         *   0:          None of the above
         *               不处于以上情况的status值就是0
         *
         * The values are arranged numerically to simplify use.
         * Non-negative values mean that a node doesn't need to
         * signal. So, most code doesn't need to check for particular
         * values, just for sign.
         * 值按数字化排列以简化使用。非负值意味着节点不需要发出信号。
         * 所以，大多数代码不需要检查特定值，只需要检查符号。
         *
         * The field is initialized to 0 for normal sync nodes, and
         * CONDITION for condition nodes.  It is modified using CAS
         * (or when possible, unconditional volatile writes).
         * 该字段对于普通的同步节点初始化为0，对于condition节点初识化为CONDIITON。使用CAS进行修改（或者可以的话，使用无条件的volatile写入）。
         */
        volatile int waitStatus;

        /**
         * Link to predecessor node that current node/thread relies on
         * for checking waitStatus. Assigned during enqueuing, and nulled
         * out (for sake of GC) only upon dequeuing.  Also, upon
         * cancellation of a predecessor, we short-circuit while
         * finding a non-cancelled one, which will always exist
         * because the head node is never cancelled: A node becomes
         * head only as a result of successful acquire. A
         * cancelled thread never succeeds in acquiring, and a thread only
         * cancels itself, not any other node.
         * 连接到当前线程/节点依赖检查waitStatus的前驱节点。
         * 在入队期间分配，并仅在出队时取消（为了GC）。
         * 同样，在前驱cancel时，当找到一个未取消的node之前进行短路，未取消的node始终存在，因为头节点从来不会cancel：一个节点要成为头结点，只有成功获取到结果。
         * 取消的线程在加锁时永远不会成功，并且线程只能取消自己，不能取消其他node。
         */
        volatile Node prev;

        /**
         * Link to the successor node that the current node/thread
         * unparks upon release. Assigned during enqueuing, adjusted
         * when bypassing cancelled predecessors, and nulled out (for
         * sake of GC) when dequeued.  The enq operation does not
         * assign next field of a predecessor until after attachment,
         * so seeing a null next field does not necessarily mean that
         * node is at end of queue. However, if a next field appears
         * to be null, we can scan prev's from the tail to
         * double-check.  The next field of cancelled nodes is set to
         * point to the node itself instead of null, to make life
         * easier for isOnSyncQueue.
         * 连接到当前节点/线程在解锁时需要unpark的后继节点。
         * 在入队时分配，在绕过取消的前驱时调整，在出队时取消（置为null）（为了GC）。
         * enq操作直到连接后才分配前驱的next字段，因此看到next字段为null时不一定意味着节点是尾结点。
         * 然而，如果next字段为null，可以从tail开始扫描上一个字段以进行二次检查。
         * 取消节点的next字段指向该节点自己而不是null，以使isOnSyncQueue的工作更轻松。
         */
        volatile Node next;

        /**
         * The thread that enqueued this node.  Initialized on
         * construction and nulled out after use.
         * 入队node的线程。
         * 在构造时初始化，在使用后置为null。
         */
        volatile Thread thread;

        /**
         * Link to next node waiting on condition, or the special
         * value SHARED.  Because condition queues are accessed only
         * when holding in exclusive mode, we just need a simple
         * linked queue to hold nodes while they are waiting on
         * conditions. They are then transferred to the queue to
         * re-acquire. And because conditions can only be exclusive,
         * we save a field by using special value to indicate shared
         * mode.
         * 连接到下一个等待条件（在条件上等待）的节点，或者特殊值SHARED。
         * 因为条件队列（condition queues）只有在独占模式下才会被访问，所以当他们正在等待条件时，我们只需要一个简单的链接队列来保存节点。
         * 然后将他们转移到主队列上来重新加锁。因为条件只能是独占的，所以我们通过使用特殊值来表名共享模式来保存字段。
         */
        Node nextWaiter;

        /**
         * Returns true if node is waiting in shared mode.
         */
        final boolean isShared() {
            return nextWaiter == SHARED;
        }

        /**
         * Returns previous node, or throws NullPointerException if null.
         * Use when predecessor cannot be null.  The null check could
         * be elided, but is present to help the VM.
         * 返回前驱节点，如果前驱为空，抛出NullPointerException
         * 当前驱不能为空时使用。
         * 空检查可以省略，但是对VM有帮助。
         *
         * @return the predecessor of this node
         */
        final Node predecessor() throws NullPointerException {
            Node p = prev;
            if (p == null)
                throw new NullPointerException();
            else
                return p;
        }

        Node() {    // Used to establish initial head or SHARED marker // 独占的可以用来初始化头结点，共享的可以建立SHARED标记
        }

        Node(Thread thread, Node mode) {     // Used by addWaiter
            this.nextWaiter = mode;
            this.thread = thread;
        }

        Node(Thread thread, int waitStatus) { // Used by Condition
            this.waitStatus = waitStatus;
            this.thread = thread;
        }
    }

    /**
     * Head of the wait queue, lazily initialized.  Except for
     * initialization, it is modified only via method setHead.  Note:
     * If head exists, its waitStatus is guaranteed not to be
     * CANCELLED.
     * 等待队列的头，延迟初始化。
     * 除了初始化，只能使用setHead方法来修改
     * 提示：如果head存在，保证waitStatus不是CANCELLED
     */
    private transient volatile Node head;

    /**
     * Tail of the wait queue, lazily initialized.  Modified only via
     * method enq to add new wait node.
     * 等待队列的尾部，懒初始化。
     * 只能通过enq方法，在增加新的等待node时修改。
     */
    private transient volatile Node tail;

    /**
     * The synchronization state.
     * 同步状态（获取锁的状态）（用volatile修饰，内存可见）
     */
    private volatile int state;

    /**
     * Returns the current value of synchronization state.
     * This operation has memory semantics of a {@code volatile} read.
     * state由volatile修饰，这个操作有内存读的语义
     * @return current state value
     */
    protected final int getState() {
        return state;
    }

    /**
     * Sets the value of synchronization state.
     * This operation has memory semantics of a {@code volatile} write.
     * 通过volatile实现的内存语义，如果不安全，可以用下面的compareAndSetState方法。
     * （这个方法通常被实现AQS的子类来调用，由子类决定如何更新state值）
     *
     * @param newState the new state value
     */
    protected final void setState(int newState) {
        state = newState;
    }

    /**
     * Atomically sets synchronization state to the given updated
     * value if the current state value equals the expected value.
     * This operation has memory semantics of a {@code volatile} read
     * and write.
     * 用原子操作来设定state值
     * （这个方法通常被实现AQS的子类来调用，由子类决定如何更新state值）
     *
     * @param expect the expected value
     * @param update the new value
     * @return {@code true} if successful. False return indicates that the actual
     *         value was not equal to the expected value.
     */
    protected final boolean compareAndSetState(int expect, int update) {
        // See below for intrinsics setup to support this
        return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
    }

    // Queuing utilities
    // 队列相关有益操作

    /**
     * The number of nanoseconds for which it is faster to spin
     * rather than to use timed park. A rough estimate suffices
     * to improve responsiveness with very short timeouts.
     * 自旋比使用定时park具有更快的纳秒数。粗略估计满足在非常短的超时时间内提高响应。
     * （自旋的时间，单位：纳秒）
     */
    static final long spinForTimeoutThreshold = 1000L;

    /**
     * Inserts node into queue, initializing if necessary. See picture above.
     * node入队，必要的话初始化。上面有图。
     * @param node the node to insert
     * @return node's predecessor
     */
    private Node enq(final Node node) {
        for (;;) {  // 这个循环是为了，如果CAS加锁失败，通过循环来重新加锁或者执行其他操作
            Node t = tail;
            if (t == null) { // Must initialize // 队尾为null，说明现在队列里啥也没有，需要初始化队列（主要就是初始化队列Head与Tail，用来指向队列头尾）
                if (compareAndSetHead(new Node())) // 如果Head为null，则设置为New Node()
                    tail = head; // 新增一个空节点，头和尾都指向同一个节点
            } else {
                node.prev = t; // 当前节点的prev指向尾指针指向的节点
                if (compareAndSetTail(t, node)) { // CAS修改tail指向的节点为当前节点
                    t.next = node; // 原来的最后一个节点next指向这个node
                    return t;
                }
            }
        }
    }

    /**
     * Creates and enqueues node for current thread and given mode.
     * 为当前线程和给定模式创建和入队node
     * （用当前线程创建Node，根据指定的模式入队）
     *
     * @param mode Node.EXCLUSIVE for exclusive, Node.SHARED for shared
     * @return the new node
     */
    private Node addWaiter(Node mode) {
        Node node = new Node(Thread.currentThread(), mode); // addWaiter的构造方法
        // Try the fast path of enq; backup to full enq on failure
        // 尝试简化enq过程；
        Node pred = tail; // 前驱=tail（注意不是head）
        if (pred != null) {  // 有tail，就简化enq(...)方法
            node.prev = pred; // node.prev->tail
            if (compareAndSetTail(pred, node)) { // tail -> node
                pred.next = node; // 原来最后一个节点的next指向这个node，这个node的next是null
                return node;
            }
        }
        enq(node); // 上面的不行再走enq方法
        return node;
    }

    /**
     * Sets head of queue to be node, thus dequeuing. Called only by
     * acquire methods.  Also nulls out unused fields for sake of GC
     * and to suppress unnecessary signals and traversals.
     * 设置队列的head为该节点，从而出队。只能通过加锁方法调用。
     * 将不使用的字段设置为null，为了GC与抑制不必要的信号与遍历。
     * （相当于在该节点加锁成功时（就是成功获取到了锁，不需要再排队了），把当前节点设置为了原来的虚拟节点作为head）
     *
     * @param node the node
     */
    private void setHead(Node node) {
        head = node;
        node.thread = null;
        node.prev = null;
    }

    /**
     * Wakes up node's successor, if one exists.
     * 如果存在，唤醒node的后继
     *
     * @param node the node
     */
    private void unparkSuccessor(Node node) {
        /*
         * If status is negative (i.e., possibly needing signal) try
         * to clear in anticipation of signalling.  It is OK if this
         * fails or if status is changed by waiting thread.
         * 如果状态为负值（可能需要信号量）尝试清楚以期待信号。
         * 如果清除状态失败或者状态已经被等待线程修改了，也没问题。
         *
         */
        int ws = node.waitStatus;
        if (ws < 0) // 只有cancelled是1，其他的signal、condition、propagate都是负值
            compareAndSetWaitStatus(node, ws, 0); // 0就是正常等待的node的waitStatus值，表示该节点正在被操作？？？其他关于该节点的操作可以等等

        /*
         * Thread to unpark is held in successor, which is normally
         * just the next node.  But if cancelled or apparently null,
         * traverse backwards from tail to find the actual
         * non-cancelled successor.
         * unpark线程被保存在后继节点中，通常是next指向的下一个节点。
         * 如果next节点被取消或者明显为null，从tail回溯找到实际上没有取消的后继者（为什么要从tail回溯？因为next可能是null，没法从前遍历）
         */
        Node s = node.next;
        if (s == null || s.waitStatus > 0) { // 后继为null或者被取消
            s = null;
            for (Node t = tail; t != null && t != node; t = t.prev) // 找到离当前node最近的未取消的非空（后继）node （当前node的prev是null，所以找到当前node之后就会终止）
                if (t.waitStatus <= 0)
                    s = t;
        }
        if (s != null)
            LockSupport.unpark(s.thread); // 唤醒后继，竞争锁
    }

    /**
     * Release action for shared mode -- signals successor and ensures
     * propagation. (Note: For exclusive mode, release just amounts
     * to calling unparkSuccessor of head if it needs signal.)
     * 共享模式的释放（解锁）动作 -- 信号通知后继者并且确保广播。
     * （提示：对于独占模式，如果需要信号量，解锁只是相当于唤醒head的后继者）
     * 这个解锁动作，实际上是将节点从park状态唤醒（调用unpark），而不是释放竞争锁
     */
    private void doReleaseShared() {
        /*
         * Ensure that a release propagates, even if there are other
         * in-progress acquires/releases.  This proceeds in the usual
         * way of trying to unparkSuccessor of head if it needs
         * signal. But if it does not, status is set to PROPAGATE to
         * ensure that upon release, propagation continues.
         * Additionally, we must loop in case a new node is added
         * while we are doing this. Also, unlike other uses of
         * unparkSuccessor, we need to know if CAS to reset status
         * fails, if so rechecking.
         * 即使有其他线程正在加锁/解锁，也要确保release广播。
         * 通常尝试去unpark head的后继，如果它需要信号的话。如果不是这样，设置head的后继的status为PROPAGATE，以确保release时传播继续。
         * 此外，必须loop循环以防止在我们这样做时有新的节点加入。
         * 此外，不同于其他的unparkSuccessor，必须知道使用CAS重置status是否失败，如果失败则重新检查。
         */
        for (;;) {
            Node h = head;
            if (h != null && h != tail) {
                int ws = h.waitStatus;
                if (ws == Node.SIGNAL) { // 当前node的ws为SIGNAL，则表示后继节点需要信号（也就是需要唤醒的信号）
                    if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0)) // 将head节点的waitStatus由SIGNAL更新为0
                        continue;            // loop to recheck cases // 循环，再次从头开始设置，直到head由SIGNAL状态转为0（因为当前队列可能会有新的节点出队/入队）
                    unparkSuccessor(h); // 唤醒后继节点
                }
                else if (ws == 0 &&
                         !compareAndSetWaitStatus(h, 0, Node.PROPAGATE)) // 后继节点不需要信号量，那么直接设置ws为PROPAGATE，确保release的时候传播继续
                    continue;                // loop on failed CAS
            }
            if (h == head)                   // loop if head changed // 如果head没变，就退出
                break;
        }
    }

    /**
     * Sets head of queue, and checks if successor may be waiting
     * in shared mode, if so propagating if either propagate > 0 or
     * PROPAGATE status was set.
     * 设置队列头，并且检查它的后继者是否在共享模式下等待。
     * 如果propagate > 0，或者设置了PROPAGATE状态，则进行传播。
     *
     * @param node the node
     * @param propagate the return value from a tryAcquireShared
     * propagate入参值是tryAcquireShared方法的返回值。
     */
    private void setHeadAndPropagate(Node node, int propagate) {
        Node h = head; // Record old head for check below
        setHead(node); // 设置head指向node，并清除thread、prev的值为null（因为当前线程一定是成功获取到锁了，所以直接置为head，表示线程已执行，变成了虚拟head）
        /*
         * Try to signal next queued node if:
         *   Propagation was indicated by caller,
         *     or was recorded (as h.waitStatus either before
         *     or after setHead) by a previous operation
         *     (note: this uses sign-check of waitStatus because
         *      PROPAGATE status may transition to SIGNAL.)
         * and
         *   The next node is waiting in shared mode,
         *     or we don't know, because it appears null
         *
         * The conservatism in both of these checks may cause
         * unnecessary wake-ups, but only when there are multiple
         * racing acquires/releases, so most need signals now or soon
         * anyway.
         * 尝试向下一个队列里的node发出信号，如果：
         * 1. 调用者明确指示广播（Propagation），或者被前一个操作记录（作为h.waitStataus，在setHead之前或者之后）。（注意：waitStatus上的信号检查，因为PROPAGATE状态可能会转化为SIGNAL状态）
         * 2. 下一个节点在共享模式下等待，或者我们不知道，因为它看起来是null。
         * 这个两项检查的保守性可能会导致不必要的唤醒，但是仅当有多个竞争加锁/解锁时，大多数很快就会需要信号。
         */
         
        // h == null 的判断是防止空指针异常。
        // h.waitStatus < 0，表示 h 处于 SIGNAL 或 PROPAGATE 状态，一般情况下是 PROPAGATE 状态，因为在 doReleaseShared 方法中 h 状态变化是 SIGNAL -> 0 -> PROPAGATE。
        // 那么为什么 SIGNAL 状态也要唤醒呢？这是因为在 doAcquireShared 中，第一次没有获得足够的资源时，shouldParkAfterFailedAcquire 将 PROPAGATE 状态转换成 SIGNAL，准备阻塞线程，
        // 但是第二次进入本方法时发现资源刚好够，而此时 h 的状态是 SIGNAL 状态
        // (h = head) == null 是再次检查
        if (propagate > 0 || h == null || h.waitStatus < 0 || 
            (h = head) == null || h.waitStatus < 0) { // 重新指向head，再判断
            Node s = node.next;
            if (s == null || s.isShared()) // 后继为null或者后继为共享模式
                doReleaseShared(); // 共享模式的解锁
        }
    }

    // Utilities for various versions of acquire
    // 各种版本的加锁

    /**
     * Cancels an ongoing attempt to acquire.
     * 取消正在进行的加锁尝试
     *
     * @param node the node
     */
    private void cancelAcquire(Node node) {
        // Ignore if node doesn't exist
        if (node == null)
            return;

        node.thread = null;

        // Skip cancelled predecessors
        // 跳过已取消的前驱（一个要取消加锁的node为啥还有前驱，可以从中间取消么？？？）
        Node pred = node.prev;
        while (pred.waitStatus > 0)
            node.prev = pred = pred.prev; // 不断修改当前节点的前驱

        // predNext is the apparent node to unsplice. CASes below will
        // fail if not, in which case, we lost race vs another cancel
        // or signal, so no further action is necessary.
        // predNext是要取消拼接的明显节点（就是这个节点要退出队列，不在队列链上）。
        // 如果没有，下面的CAS将失败，在这种场景下，在与另一个cancel或者signal竞争中输了，所以不需要采取后续操作。
        Node predNext = pred.next;

        // Can use unconditional write instead of CAS here.
        // After this atomic step, other Nodes can skip past us.
        // Before, we are free of interference from other threads.
        // 在这里可使用无条件的写而不是用CAS
        // 在这个原子步骤之后，其他节点可以跳过我们
        // 之前，不受其他线程的干扰（waitStatus由volatile提供内存可见性）
        node.waitStatus = Node.CANCELLED;

        // If we are the tail, remove ourselves.
        // 如果待取消的节点是在队尾，直接移除
        if (node == tail && compareAndSetTail(node, pred)) { // 设置tail为该节点的前驱节点
            compareAndSetNext(pred, predNext, null); // 将该节点前驱节点的next设置为null（断开与该节点的关联，为了GC与其他操作）
        } else {
            // If successor needs signal, try to set pred's next-link
            // so it will get one. Otherwise wake it up to propagate.
            // 如果待取消的节点不在队尾（在队列中间（不能为队首，因为队首是个虚拟节点））
            // 如果后继需要signal，尝试设置前驱节点的下一个连接，这样就会得到一个。
            // 否则唤醒它进行广播（propagate）
            int ws;
            if (pred != head && // 如果该节点前面是队首，说明它前面没有等待的节点了
                ((ws = pred.waitStatus) == Node.SIGNAL || // 前驱节点的ws本来就是SIGNAL
                 (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) && // 前驱节点状态非默认与取消，并且更新前驱的ws为SIGNAL成功
                pred.thread != null) { // 前驱节点里线程不为null
                Node next = node.next;
                if (next != null && next.waitStatus <= 0) // 当前节点的后继节点不是空，并且没有取消
                    compareAndSetNext(pred, predNext, next); // 设置前驱节点的next指向该节点的后继节点
            } else {
                unparkSuccessor(node); // 这个否则很灵性，如果该节点是head之后的第一个节点，（或者是上面的条件不满足）那么它取消之后就得直接唤醒后继节点了。
            }

            node.next = node; // help GC // 打断当前节点与其他节点的关联，方便GC
        }
    }

    /**
     * Checks and updates status for a node that failed to acquire.
     * Returns true if thread should block. This is the main signal
     * control in all acquire loops.  Requires that pred == node.prev.
     * 检查和更新加锁失败的节点状态。
     * 如果线程需要阻塞，返回true。这是在所有加锁循环中主要的信号控制。
     * 要求pred == node.prev
     *
     * @param pred node's predecessor holding status // 节点的前驱持有状态
     * @param node the node // 当前节点
     * @return {@code true} if thread should block
     */
    private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        int ws = pred.waitStatus;
        if (ws == Node.SIGNAL)
            /*
             * This node has already set status asking a release
             * to signal it, so it can safely park.
             * 该node已经设置了status，要求解锁时通知它（这个翻译不一定准，等看具体调用），所以它可以安全park。
             */
            return true;
        if (ws > 0) {
            /*
             * Predecessor was cancelled. Skip over predecessors and
             * indicate retry.
             * 前驱节点已经取消了。跳过取消节点重试找到未取消的。
             */
            do {
                node.prev = pred = pred.prev;
            } while (pred.waitStatus > 0);
            pred.next = node;
        } else {
            /*
             * waitStatus must be 0 or PROPAGATE.  Indicate that we
             * need a signal, but don't park yet.  Caller will need to
             * retry to make sure it cannot acquire before parking.
             * 到了这里waitStatus一定是0或者PROPAGATE（-3）。声明需要一个signal，但park还没有执行。？？？
             * 调用者应该重试以确保在park前无法加锁？？？
             */
            compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
        }
        return false;
    }

    /**
     * Convenience method to interrupt current thread.
     * 中断当前线程的便捷（convenience）方法
     */
    static void selfInterrupt() {
        Thread.currentThread().interrupt();
    }

    /**
     * Convenience method to park and then check if interrupted
     * park和之后检查是否中断的便捷方法
     *
     * @return {@code true} if interrupted
     */
    private final boolean parkAndCheckInterrupt() {
        LockSupport.park(this);  // park会响应中断，中断发生时会设置interrupted值，不会抛出异常
        return Thread.interrupted(); // 返回检查到的线程中断状态，并清除中断状态
    }

    /*
     * Various flavors of acquire, varying in exclusive/shared and
     * control modes.  Each is mostly the same, but annoyingly
     * different.  Only a little bit of factoring is possible due to
     * interactions of exception mechanics (including ensuring that we
     * cancel if tryAcquire throws exception) and other control, at
     * least not without hurting performance too much.
     * 不同形式的加锁，在独占/共享和控制模式下各有不同。
     * 每一个都大致相同，但总有不同（annoyingly 恼人的）。
     * 由于异常机制（包括确保我们在tryAcquire抛出的异常时cancel）和其他控制的相互作用，只能进行一点点分解，至少在不会过多损耗性能的前提下进行。
     *
     */

    /**
     * Acquires in exclusive uninterruptible mode for thread already in
     * queue. Used by condition wait methods as well as acquire.
     * 对于已经在队列里的线程，在独占非中断模式下加锁。
     * 由条件等待方法使用也能加锁？？？ 
     *
     * @param node the node
     * @param arg the acquire argument
     * @return {@code true} if interrupted while waiting // 如果在等待时中断，返回true
     */
    final boolean acquireQueued(final Node node, int arg) {
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) { // 如果加锁失败，这个for啥时候会退出呢？？？
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) { // 如果前驱是head，并且尝试获取独占锁成功
                    setHead(node);  // 将当前node转移为head
                    p.next = null; // help GC // 原来head的next指向null，断开head的连接，准备回收原head
                    failed = false; // 加锁失败状态为false（表示获取锁成功）
                    return interrupted; // 获取锁成功了，返回当前线程中断状态
                }
                if (shouldParkAfterFailedAcquire(p, node) && // 判断在获取锁失败后是否需要park
                    parkAndCheckInterrupt())                 // 如果需要park，进行park并且检查中断状态（如果线程为中断状态，返回true）（因为park能响应中断，中断时会退出park）
                    interrupted = true;                     // 能进到这里说明已经设置了park（阻塞），并且在park等待时发生了中断，当前线程中断状态为true。（这里true也不会直接抛出异常，而是继续去尝试获取锁）
            }
        } finally {
            if (failed)
                cancelAcquire(node); // 啥时候会走到这里呢？？？
        }
    }

    /**
     * Acquires in exclusive interruptible mode.
     * 在独占可中断模式下加锁
     * @param arg the acquire argument
     */
    private void doAcquireInterruptibly(int arg)
        throws InterruptedException {
        final Node node = addWaiter(Node.EXCLUSIVE); // 返回用当前线程封装的node
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) { // tryAcquire()方法需要自己实现，来决定如何实现尝试获取锁的语义，在AQS里没有阻塞/非阻塞的概念
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return;
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    throw new InterruptedException(); // 如果检查到线程中断，抛出异常
            }
        } finally {
            if (failed)
                cancelAcquire(node); // 啥时候会走到这里呢？？？
        }
    }

    /**
     * Acquires in exclusive timed mode.
     * 在独占限时模式下加锁
     *
     * @param arg the acquire argument
     * @param nanosTimeout max wait time // 最大等待时间
     * @return {@code true} if acquired // 成功加锁返回true
     */
    private boolean doAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (nanosTimeout <= 0L)
            return false; // 对于等待时间<=0的，直接返回false
        final long deadline = System.nanoTime() + nanosTimeout; // 生成截止时间
        final Node node = addWaiter(Node.EXCLUSIVE);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return true;
                }
                nanosTimeout = deadline - System.nanoTime(); // 检查剩余的等待时间
                if (nanosTimeout <= 0L)
                    return false; // 等待时间不够，返回false
                if (shouldParkAfterFailedAcquire(p, node) &&
                    nanosTimeout > spinForTimeoutThreshold) // 如果等待时间（相当于预估的还需要等待时间）> 自旋的时间阈值，就进入park（如果预估时间小于自旋的阈值，可以通过自旋继续等待）。
                    // 这里表明，如果给一个较小的等待时间，就可以不断的通过自旋来加锁（当然也会因为自旋加锁失败，需要不断调用加锁的消耗）
                    LockSupport.parkNanos(this, nanosTimeout);
                if (Thread.interrupted())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * Acquires in shared uninterruptible mode.
     * 在共享非中断模式下加锁
     * @param arg the acquire argument
     */
    private void doAcquireShared(int arg) {
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) {
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg); // 需要自己实现的5大方法之一，在共享模式下尝试加锁。负值表示失败，0表示当前成功，但后继的线程们加锁可能会失败，正值表示当前成功，后继的线程们加锁也可能成功
                    if (r >= 0) {
                        setHeadAndPropagate(node, r); // 将当前节点设置为头结点（表示当前节点已成功获取到锁），如果其他后继节点也能获取到锁（毕竟是个共享锁），也会被从park唤醒
                        p.next = null; // help GC
                        if (interrupted)
                            selfInterrupt(); // 如果检测到线程中断，调用中断方法（只是写了个中断标记，没有抛出异常）
                        failed = false;
                        return;
                    }
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt()) // 检测线程是否被中断
                    interrupted = true; // 如果判断在加锁失败时需要阻塞（park），并且阻塞后检测到线程被中断，更新当前中断标记为true
            }
        } finally { // 不知道什么时候会走到这里，可能是中断的时候？？？
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * Acquires in shared interruptible mode.
     * 在共享可中断模式下加锁
     * @param arg the acquire argument
     */
    private void doAcquireSharedInterruptibly(int arg)
        throws InterruptedException {
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        failed = false;
                        return;
                    }
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    throw new InterruptedException(); // 与上面不同的就在这里，如果检测到了中断，直接抛出中断异常
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * Acquires in shared timed mode.
     * 在共享限时模式下加锁
     *
     * @param arg the acquire argument
     * @param nanosTimeout max wait time
     * @return {@code true} if acquired
     */
    private boolean doAcquireSharedNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (nanosTimeout <= 0L)
            return false;
        final long deadline = System.nanoTime() + nanosTimeout;
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        failed = false;
                        return true;
                    }
                }
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L)
                    return false;
                if (shouldParkAfterFailedAcquire(p, node) &&
                    nanosTimeout > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if (Thread.interrupted())
                    throw new InterruptedException(); // 同样的，也会抛出中断异常
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    // Main exported methods
    // 主要对外方法（上面那些都是private的方法）
    
    // 下面这5个protected方法需要子类来实现
    /**
     * Attempts to acquire in exclusive mode. This method should query
     * if the state of the object permits it to be acquired in the
     * exclusive mode, and if so to acquire it.
     * 尝试以独占模式加锁。这个方法应当检查对象的状态是否允许在独占模式下加锁，如果允许则加锁
     *
     * <p>This method is always invoked by the thread performing
     * acquire.  If this method reports failure, the acquire method
     * may queue the thread, if it is not already queued, until it is
     * signalled by a release from some other thread. This can be used
     * to implement method {@link Lock#tryLock()}.
     * 这个方法总是被执行加锁（acquire）的线程调用。
     * 如果此方法报告失败，这个acquire方法可能会将线程入队（如果该线程还没有入队），直到它被其他线程发出release（释放）信号。
     * 可以通过tryLock()方法实现。
     *
     * <p>The default
     * implementation throws {@link UnsupportedOperationException}.
     *
     * @param arg the acquire argument. This value is always the one
     *        passed to an acquire method, or is the value saved on entry
     *        to a condition wait.  The value is otherwise uninterpreted
     *        and can represent anything you like.
     *        加锁参数。该值始终是传递给acquire方法的值，或者是进入条件等待时保存的值。
     *        该值是未经解释的，可以表示你喜欢的任何内容
     * @return {@code true} if successful. Upon success, this object has
     *         been acquired.
     * @throws IllegalMonitorStateException if acquiring would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     *         如果加锁会导致同步器进入非法状态则会抛出IllegalMonitorStateException。
     *         必须以一致的方式抛出此异常，同步器才能正常工作（也是个runtimeException）
     * @throws UnsupportedOperationException if exclusive mode is not supported // 如果不支持独占模式，抛出UnsupportedOperationException（这是个runtimeException）
     */
    protected boolean tryAcquire(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to set the state to reflect a release in exclusive
     * mode.
     * 尝试设置状态来反映独占模式下的解锁/释放（release）
     *
     * <p>This method is always invoked by the thread performing release.
     * 该方法总是被执行解锁（release）的线程调用
     *
     * <p>The default implementation throws
     * {@link UnsupportedOperationException}.
     *
     * @param arg the release argument. This value is always the one
     *        passed to a release method, or the current state value upon
     *        entry to a condition wait.  The value is otherwise
     *        uninterpreted and can represent anything you like.
     * @return {@code true} if this object is now in a fully released
     *         state, so that any waiting threads may attempt to acquire;
     *         and {@code false} otherwise.
     *         true：表示此对象处于完全释放状态，任何等待线程都可以尝试获取
     * @throws IllegalMonitorStateException if releasing would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     * @throws UnsupportedOperationException if exclusive mode is not supported
     */
    protected boolean tryRelease(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to acquire in shared mode. This method should query if
     * the state of the object permits it to be acquired in the shared
     * mode, and if so to acquire it.
     * 尝试在共享模式下加锁。这个方法应当检查对象的状态是否允许在共享模式下加锁，如果允许则加锁
     *
     * <p>This method is always invoked by the thread performing
     * acquire.  If this method reports failure, the acquire method
     * may queue the thread, if it is not already queued, until it is
     * signalled by a release from some other thread.
     * 这个方法总是被想加锁的线程调用。
     * 如果这个方法报告失败，这个方法可能会将还没在等待队列里的线程入队，直到其他线程释放锁时来唤醒它。
     *
     * <p>The default implementation throws {@link
     * UnsupportedOperationException}.
     *
     * @param arg the acquire argument. This value is always the one
     *        passed to an acquire method, or is the value saved on entry
     *        to a condition wait.  The value is otherwise uninterpreted
     *        and can represent anything you like.
     * @return a negative value on failure; zero if acquisition in shared
     *         mode succeeded but no subsequent shared-mode acquire can
     *         succeed; and a positive value if acquisition in shared
     *         mode succeeded and subsequent shared-mode acquires might
     *         also succeed, in which case a subsequent waiting thread
     *         must check availability. (Support for three different
     *         return values enables this method to be used in contexts
     *         where acquires only sometimes act exclusively.)  Upon
     *         success, this object has been acquired.
     *         负值表示失败；
     *         0表示在共享模式下加锁成功但是在随后的共享模式加锁中没有成功；（就是当前线程能够获取到共享锁，没有剩余的共享锁可以被获取）
     *         正值表示在共享模式下加锁成功但是在随后的共享模式加锁中也可能成功（就是当前线程能够获取到共享锁，还有剩余的共享锁可以被获取），在这种情况下，后续等待线程必须检查可用性。
     *         （支持三种不同的返回值，能够保证这个方法能够在仅执行独占行为的上下文中使用）
     *         成功值表示该对象加锁成功。
     *         
     * @throws IllegalMonitorStateException if acquiring would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     * @throws UnsupportedOperationException if shared mode is not supported
     */
    protected int tryAcquireShared(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to set the state to reflect a release in shared mode.
     * 尝试设置状态来反映在共享模式下解锁/释放（release）
     *
     * <p>This method is always invoked by the thread performing release.
     *
     * <p>The default implementation throws
     * {@link UnsupportedOperationException}.
     *
     * @param arg the release argument. This value is always the one
     *        passed to a release method, or the current state value upon
     *        entry to a condition wait.  The value is otherwise
     *        uninterpreted and can represent anything you like.
     * @return {@code true} if this release of shared mode may permit a
     *         waiting acquire (shared or exclusive) to succeed; and
     *         {@code false} otherwise
     *         true：表示共享模式下的release可以允许等待获取（共享/独占）操作成功
     * @throws IllegalMonitorStateException if releasing would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     * @throws UnsupportedOperationException if shared mode is not supported
     */
    protected boolean tryReleaseShared(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns {@code true} if synchronization is held exclusively with
     * respect to the current (calling) thread.  This method is invoked
     * upon each call to a non-waiting {@link ConditionObject} method.
     * (Waiting methods instead invoke {@link #release}.)
     * 如果当前（调用）线程持有独占的同步锁，将返回true。
     * 这个方法被每个非等待的ConditionObject方法调用。
     * （等待方法替换成调用release）
     *
     * <p>The default implementation throws {@link
     * UnsupportedOperationException}. This method is invoked
     * internally only within {@link ConditionObject} methods, so need
     * not be defined if conditions are not used.
     * 这个方法仅在ConditionObject的方法内部调用，如果不使用Condition就不用定义这个方法。
     *
     * @return {@code true} if synchronization is held exclusively;
     *         {@code false} otherwise
     * @throws UnsupportedOperationException if conditions are not supported
     */
    protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }

		// 下面是完全对外的public方法
    /**
     * Acquires in exclusive mode, ignoring interrupts.  Implemented
     * by invoking at least once {@link #tryAcquire},
     * returning on success.  Otherwise the thread is queued, possibly
     * repeatedly blocking and unblocking, invoking {@link
     * #tryAcquire} until success.  This method can be used
     * to implement method {@link Lock#lock}.
     * 在独占模式下获取，忽略中断。通过至少调用一次tryAcquire来实现，在成功时返回。
     * 否则线程会排队，可能会反复阻塞与解除阻塞，调用tryAcquire直到成功。
     * 这个方法可以用来实现lock方法。
     * （拿不到锁就入队排队，等不到就阻塞等待唤醒）
     * 
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquire} but is otherwise uninterpreted and
     *        can represent anything you like.
     */
    public final void acquire(int arg) {
        if (!tryAcquire(arg) && // 尝试获取失败
            acquireQueued(addWaiter(Node.EXCLUSIVE), arg)) 
            // addWaiter(Node.EXCLUSIVE), arg)将当前线程按独占模式创建node，加入到队列中
            // acquireQueued如果当前node的前驱是head，那么尝试获取，如果不是，分析是否阻塞等待与阻塞唤醒后检测中断信号
            selfInterrupt();
    }

    /**
     * Acquires in exclusive mode, aborting if interrupted.
     * Implemented by first checking interrupt status, then invoking
     * at least once {@link #tryAcquire}, returning on
     * success.  Otherwise the thread is queued, possibly repeatedly
     * blocking and unblocking, invoking {@link #tryAcquire}
     * until success or the thread is interrupted.  This method can be
     * used to implement method {@link Lock#lockInterruptibly}.
     * 在独占模式下获取，如果中断了就终止。
     * 通过首先检查中断状态，然后至少调用一次tryAcquire实现，成功时返回。
     * 否则线程入队，可能反复阻塞与取消阻塞，调用tryAcquire直到成功或者线程被中断。
     * 这个方法可以用来实现lockInterruptibly方法。
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquire} but is otherwise uninterpreted and
     *        can represent anything you like.
     * @throws InterruptedException if the current thread is interrupted
     */
    public final void acquireInterruptibly(int arg)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        if (!tryAcquire(arg))
            doAcquireInterruptibly(arg); // 如果获取失败，调用doAcquireInterruptibly方法，跟acquireQueued方法类似，只是会在检测到线程中断后，直接抛出中断异常，而不是继续尝试获取锁
    }

    /**
     * Attempts to acquire in exclusive mode, aborting if interrupted,
     * and failing if the given timeout elapses.  Implemented by first
     * checking interrupt status, then invoking at least once {@link
     * #tryAcquire}, returning on success.  Otherwise, the thread is
     * queued, possibly repeatedly blocking and unblocking, invoking
     * {@link #tryAcquire} until success or the thread is interrupted
     * or the timeout elapses.  This method can be used to implement
     * method {@link Lock#tryLock(long, TimeUnit)}.
     * 在独占模式下获取，如果中断或者超时就终止或者失败。
     * 通过首先检查中断状态，然后至少一次调用tryAcquire来实现，成功时返回。
     * 否则当前线程入队，可能反复阻塞与取消阻塞，调用tryAcquire直到获取成功，或者被中断，或者超时。
     * 这个方法可以用来实现tryLock(long, TimeUnit)方法
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquire} but is otherwise uninterpreted and
     *        can represent anything you like.
     * @param nanosTimeout the maximum number of nanoseconds to wait
     * @return {@code true} if acquired; {@code false} if timed out
     * @throws InterruptedException if the current thread is interrupted
     */
    public final boolean tryAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        return tryAcquire(arg) ||
            doAcquireNanos(arg, nanosTimeout);
    }

    /**
     * Releases in exclusive mode.  Implemented by unblocking one or
     * more threads if {@link #tryRelease} returns true.
     * This method can be used to implement method {@link Lock#unlock}.
     * 在独占模式下释放。如果tryRelease返回true，通过解除阻塞一个或多个线程来实现。
     * 这个方法可以用来实现unlock方法
     *
     * @param arg the release argument.  This value is conveyed to
     *        {@link #tryRelease} but is otherwise uninterpreted and
     *        can represent anything you like.
     * @return the value returned from {@link #tryRelease}
     */
    public final boolean release(int arg) {
        if (tryRelease(arg)) {
            Node h = head;
            if (h != null && h.waitStatus != 0) // 如果有头结点，并且ws不是默认值0
                unparkSuccessor(h); // 唤醒一个后继节点，这个方法会把ws置为0，表示正在操作？？？
            return true;
        }
        return false;
    }

    /**
     * Acquires in shared mode, ignoring interrupts.  Implemented by
     * first invoking at least once {@link #tryAcquireShared},
     * returning on success.  Otherwise the thread is queued, possibly
     * repeatedly blocking and unblocking, invoking {@link
     * #tryAcquireShared} until success.
     * 共享模式下获取，忽略中断。
     * 通过首先调用至少一次tryAcquireShared方法来实现，成功时返回。
     * 否则该线程入队，可能反复阻塞或者解除阻塞，调用tryAcquireShared直到成功。
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquireShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     */
    public final void acquireShared(int arg) {
        if (tryAcquireShared(arg) < 0) // 尝试获取失败（这是个需要自己实现的方法）
            doAcquireShared(arg); // 将该线程封装成共享模式的node然后入队，循环调用tryAcquireShared来获取，如果tryAcquireShared返回值>=0，会调用setHeadAndPropagate方法，先把当前node设置为head，然后尝试释放后继的node；如果tryAcquireShared返回<0，会判断是否需要park
    }

    /**
     * Acquires in shared mode, aborting if interrupted.  Implemented
     * by first checking interrupt status, then invoking at least once
     * {@link #tryAcquireShared}, returning on success.  Otherwise the
     * thread is queued, possibly repeatedly blocking and unblocking,
     * invoking {@link #tryAcquireShared} until success or the thread
     * is interrupted.
     * 在共享模式下获取，在中断时终止。
     * 通过首先检查中断状态，然后至少调用一次tryAcquireShared方法实现，成功时返回。
     * 否则当前线程入队，可能反复阻塞与解除阻塞，调用tryAcquireShared方法，直到成功或者线程中断。
     * 
     * @param arg the acquire argument.
     * This value is conveyed to {@link #tryAcquireShared} but is
     * otherwise uninterpreted and can represent anything
     * you like.
     * @throws InterruptedException if the current thread is interrupted
     */
    public final void acquireSharedInterruptibly(int arg)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        if (tryAcquireShared(arg) < 0)
            doAcquireSharedInterruptibly(arg); // 在中断时抛出中断异常
    }

    /**
     * Attempts to acquire in shared mode, aborting if interrupted, and
     * failing if the given timeout elapses.  Implemented by first
     * checking interrupt status, then invoking at least once {@link
     * #tryAcquireShared}, returning on success.  Otherwise, the
     * thread is queued, possibly repeatedly blocking and unblocking,
     * invoking {@link #tryAcquireShared} until success or the thread
     * is interrupted or the timeout elapses.
     * 尝试在共享模式下获取，如果中断则终止，如果超时则返回失败。
     * 通过首先检查中断状态，然后至少调用一次tryAcquireShared方法实现，成功时返回。
     * 否则，当前线程入队，可能反复多次阻塞与解除阻塞，调用tryAcquireShared方法直到成功，或者线程中断，或者超时
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquireShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     * @param nanosTimeout the maximum number of nanoseconds to wait
     * @return {@code true} if acquired; {@code false} if timed out
     * @throws InterruptedException if the current thread is interrupted
     */
    public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        return tryAcquireShared(arg) >= 0 ||
            doAcquireSharedNanos(arg, nanosTimeout); // 正常逻辑操作，没啥说的
    }

    /**
     * Releases in shared mode.  Implemented by unblocking one or more
     * threads if {@link #tryReleaseShared} returns true.
     * 共享模式下释放。
     * 如果tryReleaseShared返回true，通过解除阻塞一个或多个线程来实现。
     *
     * @param arg the release argument.  This value is conveyed to
     *        {@link #tryReleaseShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     * @return the value returned from {@link #tryReleaseShared}
     */
    public final boolean releaseShared(int arg) {
        if (tryReleaseShared(arg)) {
            doReleaseShared();
            return true;
        }
        return false;
    }

    // Queue inspection methods
    // 队列检查方法

    /**
     * Queries whether any threads are waiting to acquire. Note that
     * because cancellations due to interrupts and timeouts may occur
     * at any time, a {@code true} return does not guarantee that any
     * other thread will ever acquire.
     * 查询是否有线程正在等待获取。
     * 注意：由于可能在任意时间发生中断或者超时，会导致线程取消，所以返回true也不能保证有线程永远在等待获取。
     *
     * <p>In this implementation, this operation returns in
     * constant time.
     *
     * @return {@code true} if there may be other threads waiting to acquire
     */
    public final boolean hasQueuedThreads() {
        return head != tail;
    }

    /**
     * Queries whether any threads have ever contended to acquire this
     * synchronizer; that is if an acquire method has ever blocked.
     * 查询是否有线程竞争过获取这个同步器；
     * 也就是说，曾经有线程调用acquire方法阻塞过。
     *
     * <p>In this implementation, this operation returns in
     * constant time.
     *
     * @return {@code true} if there has ever been contention
     */
    public final boolean hasContended() {
        return head != null;
    }

    /**
     * Returns the first (longest-waiting) thread in the queue, or
     * {@code null} if no threads are currently queued.
     * 返回队列里的第一个线程（等待时间最长的），如果没有线程在当前队列，返回null
     *
     * <p>In this implementation, this operation normally returns in
     * constant time, but may iterate upon contention if other threads are
     * concurrently modifying the queue.
     *
     * @return the first (longest-waiting) thread in the queue, or
     *         {@code null} if no threads are currently queued
     */
    public final Thread getFirstQueuedThread() {
        // handle only fast path, else relay
        return (head == tail) ? null : fullGetFirstQueuedThread();
    }

    /**
     * Version of getFirstQueuedThread called when fastpath fails
     * 快速路径失败时调用的版本
     */
    private Thread fullGetFirstQueuedThread() {
        /*
         * The first node is normally head.next. Try to get its
         * thread field, ensuring consistent reads: If thread
         * field is nulled out or s.prev is no longer head, then
         * some other thread(s) concurrently performed setHead in
         * between some of our reads. We try this twice before
         * resorting to traversal.
         * 确保读一致性，如果线程字段被置null，或者s.prev不再是head，然后其他线程并发的在我们读中间setHead。
         * 尝试两次
         */
        Node h, s;
        Thread st;
        if (((h = head) != null && (s = h.next) != null &&
             s.prev == head && (st = s.thread) != null) ||
            ((h = head) != null && (s = h.next) != null &&
             s.prev == head && (st = s.thread) != null))
            return st;

        /*
         * Head's next field might not have been set yet, or may have
         * been unset after setHead. So we must check to see if tail
         * is actually first node. If not, we continue on, safely
         * traversing from tail back to head to find first,
         * guaranteeing termination.
         * head的next可能还没有设置，或者可能在setHead之后未设置。所以应该检查tail是否实际上是第一个节点。
         * 如果不是，继续安全的从tail向head查找第一个，保证终止。
         */

        Node t = tail;
        Thread firstThread = null;
        while (t != null && t != head) {
            Thread tt = t.thread;
            if (tt != null)
                firstThread = tt;
            t = t.prev;
        }
        return firstThread;
    }

    /**
     * Returns true if the given thread is currently queued.
     * 如果当前线程在排队，返回true
     *
     * <p>This implementation traverses the queue to determine
     * presence of the given thread.
     *
     * @param thread the thread
     * @return {@code true} if the given thread is on the queue
     * @throws NullPointerException if the thread is null
     */
    public final boolean isQueued(Thread thread) {
        if (thread == null)
            throw new NullPointerException();
        for (Node p = tail; p != null; p = p.prev)
            if (p.thread == thread)
                return true;
        return false;
    }

    /**
     * Returns {@code true} if the apparent first queued thread, if one
     * exists, is waiting in exclusive mode.  If this method returns
     * {@code true}, and the current thread is attempting to acquire in
     * shared mode (that is, this method is invoked from {@link
     * #tryAcquireShared}) then it is guaranteed that the current thread
     * is not the first queued thread.  Used only as a heuristic in
     * ReentrantReadWriteLock.
     * 如果明显的第一个线程（如果有的话）在独占模式下等待，返回true。
     * 如果该方法返回true，并且当前线程试图在共享模式下获取（这意味着，这个方式是通过tryAcquireShared方法调用的），则可以保证当前线程不是第一个排队的线程。
     * 仅用于ReentrantReadWriteLcok的启发式方法
     * 
     */
    final boolean apparentlyFirstQueuedIsExclusive() {
        Node h, s;
        return (h = head) != null &&
            (s = h.next)  != null &&
            !s.isShared()         &&
            s.thread != null;
    }

    /**
     * Queries whether any threads have been waiting to acquire longer
     * than the current thread.
     * 查询是否有比当前线程等待时间更久的线程。
     *
     * <p>An invocation of this method is equivalent to (but may be
     * more efficient than):
     * 调用此方法等效于（但是可能更高效）：
     *  <pre> {@code
     * getFirstQueuedThread() != Thread.currentThread() &&
     * hasQueuedThreads()}</pre>
     * 当前线程不是队列里的第一个，并且队列里有线程
     *
     * <p>Note that because cancellations due to interrupts and
     * timeouts may occur at any time, a {@code true} return does not
     * guarantee that some other thread will acquire before the current
     * thread.  Likewise, it is possible for another thread to win a
     * race to enqueue after this method has returned {@code false},
     * due to the queue being empty.
     * 这个方法不能保证这个线程前面一定有老线程，或者这个线程一定是等待时间最长的线程
     * 原因1、可能在这个方法返回true后，前面的线程由于中断或者超时导致退出了等待
     * 原因2、可能在这个方法返回false后，在队列为空时，有新的线程在与该线程入队竞争时获胜，比该线程更早入队
     *
     * <p>This method is designed to be used by a fair synchronizer to
     * avoid <a href="AbstractQueuedSynchronizer#barging">barging</a>.
     * Such a synchronizer's {@link #tryAcquire} method should return
     * {@code false}, and its {@link #tryAcquireShared} method should
     * return a negative value, if this method returns {@code true}
     * (unless this is a reentrant acquire).  For example, the {@code
     * tryAcquire} method for a fair, reentrant, exclusive mode
     * synchronizer might look like this:
     * 该方法是为了公平同步器设计的，避免插队情况。
     * 如果这个方法返回true，这种同步器的tryAcquire方法应该返回false，它的tryAcquireShared方法应该返回负值，除非这是一个可重入的获取过程。
     * 例如：公平、可重入、独占模式下的同步器tryAcquire方法可能如下所示：
     *
     *  <pre> {@code
     * protected boolean tryAcquire(int arg) {
     *   if (isHeldExclusively()) { // 先看是不是当前线程独占
     *     // A reentrant acquire; increment hold count
     *     return true;
     *   } else if (hasQueuedPredecessors()) { // 再看前面有没有排队的（现在线程可能不在队列，也可能在队列里，不影响判断）
     *     return false;
     *   } else { // 都没有就准备竞争
     *     // try to acquire normally
     *   }
     * }}</pre>
     *
     * @return {@code true} if there is a queued thread preceding the
     *         current thread, and {@code false} if the current thread
     *         is at the head of the queue or the queue is empty
     * @since 1.7
     */
    public final boolean hasQueuedPredecessors() {
        // The correctness of this depends on head being initialized
        // before tail and on head.next being accurate if the current
        // thread is first in queue.
        // 正确性取决于head在tail之前被初始化，并且如果当前线程是在队列的第一个，那么head.next是准确的
        Node t = tail; // Read fields in reverse initialization order
        Node h = head;
        Node s;
        // 方法返回true表示在当前线程之前还有等待的线程，false表示没有
        // head != tail排除了两种情况：
        // 1、head = tail = null（此时队列还未有阶段入过队）
        // 2、head = tail = node（此时队列里曾经有入过队的，但都已出队）
        // (s = h.next) == null（可能存在一种情况，设置了head，但是next还没来得及设置为（除了head之外）第一个等待的线程节点，这种情况下，next不准确，因为不知道next是当前线程还是其他线程，为了保险起见，返回true）
        // s.thread != Thread.currentThread() （这个简单了，判断第一个等待节点是不是该线程的节点，不是的话返回true）
        return h != t &&
            ((s = h.next) == null || s.thread != Thread.currentThread());
    }


    // Instrumentation and monitoring methods
    // 仪表盘和监控方法

    /**
     * Returns an estimate of the number of threads waiting to
     * acquire.  The value is only an estimate because the number of
     * threads may change dynamically while this method traverses
     * internal data structures.  This method is designed for use in
     * monitoring system state, not for synchronization
     * control.
     * 返回预估的等待获取的线程数。
     * 这个返回值只是个预估值，因为在方法遍历内部数据结构时，线程是可能动态变化的。
     * 该方法是为了监控系统状态设计的，不是为了同步。
     *
     * @return the estimated number of threads waiting to acquire
     */
    public final int getQueueLength() {
        int n = 0;
        for (Node p = tail; p != null; p = p.prev) { // 再次理解一下，说是queue，其实没有维护一个队列长度的属性
            if (p.thread != null)
                ++n;
        }
        return n;
    }

    /**
     * Returns a collection containing threads that may be waiting to
     * acquire.  Because the actual set of threads may change
     * dynamically while constructing this result, the returned
     * collection is only a best-effort estimate.  The elements of the
     * returned collection are in no particular order.  This method is
     * designed to facilitate construction of subclasses that provide
     * more extensive monitoring facilities.
     * 返回一个包含正在等待获取的线程集合。
     * 因为在构造该集合结果时，实际线程可能在动态改变，所以这个返回的集合只是一个尽力的预估。
     * 返回集合里的线程没有特定顺序。
     * 该方法是为了促进子类构建而设计，以提高更广泛的监控设备
     *
     * @return the collection of threads
     */
    public final Collection<Thread> getQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            Thread t = p.thread;
            if (t != null)
                list.add(t);
        }
        return list;
    }

    /**
     * Returns a collection containing threads that may be waiting to
     * acquire in exclusive mode. This has the same properties
     * as {@link #getQueuedThreads} except that it only returns
     * those threads waiting due to an exclusive acquire.
     * 返回一个包含可能正在独占模式下等待的线程集合。
     * 跟上面的getQueueThreads方法类似，除了只返回独占获取的等待线程
     *
     * @return the collection of threads
     */
    public final Collection<Thread> getExclusiveQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            if (!p.isShared()) { // 去掉共享模式下的等待线程（就是说一个queue里面可能既有独占，又有共享的等待线程）
                Thread t = p.thread;
                if (t != null)
                    list.add(t);
            }
        }
        return list;
    }

    /**
     * Returns a collection containing threads that may be waiting to
     * acquire in shared mode. This has the same properties
     * as {@link #getQueuedThreads} except that it only returns
     * those threads waiting due to a shared acquire.
     * 返回一个包含在共享模式下等待的线程集合。
     *
     * @return the collection of threads
     */
    public final Collection<Thread> getSharedQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            if (p.isShared()) { // 只要共享模式下的
                Thread t = p.thread;
                if (t != null)
                    list.add(t);
            }
        }
        return list;
    }

    /**
     * Returns a string identifying this synchronizer, as well as its state.
     * The state, in brackets, includes the String {@code "State ="}
     * followed by the current value of {@link #getState}, and either
     * {@code "nonempty"} or {@code "empty"} depending on whether the
     * queue is empty.
     * 返回标识此同步器和state的字符串（state由子类实现，在AQS中没使用）
     * （bracket 括号）
     * @return a string identifying this synchronizer, as well as its state
     */
    public String toString() {
        int s = getState();
        String q  = hasQueuedThreads() ? "non" : "";
        return super.toString() +
            "[State = " + s + ", " + q + "empty queue]";
    }


    // Internal support methods for Conditions
    // 内部支持Condition的方法

    /**
     * Returns true if a node, always one that was initially placed on
     * a condition queue, is now waiting to reacquire on sync queue.
     * 如果一个节点始终是最初放在条件队列中的节点，现在正在等待重新获取sync队列，则返回true
     * 
     * @param node the node
     * @return true if is reacquiring
     */
    final boolean isOnSyncQueue(Node node) {
    	  // 如果节点的等待状态是CONDITION，说明在condition队列中（不在AQS主队列）；
    	  // 如果prev是null，并且是AQS，也是已获取到锁的head节点，也不在AQS主队列中等待
        if (node.waitStatus == Node.CONDITION || node.prev == null)
            return false;
        if (node.next != null) // If has successor, it must be on queue // 意思是只有AQS主队列才有next跟prev关系，如果next不为空，一定在AQS sync主队列里
            return true;
        /*
         * node.prev can be non-null, but not yet on queue because
         * the CAS to place it on queue can fail. So we have to
         * traverse from tail to make sure it actually made it.  It
         * will always be near the tail in calls to this method, and
         * unless the CAS failed (which is unlikely), it will be
         * there, so we hardly ever traverse much.
         * node.prev如果不为null，不能保证一定在队列里，因为通过CAS操作入队时会失败。（可以看addWaiter方法，先设置node.prev=tail，然后去做的CAS，只有CAS成功了才算成功入队）
         * 所以从tail向前遍历，确保它确实入队了。
         * 除非CAS失败（基本不太可能），否则在调用这个方法时它总是靠近尾部，所以我们不会遍历太多。
         */
        return findNodeFromTail(node);
    }

    /**
     * Returns true if node is on sync queue by searching backwards from tail.
     * Called only when needed by isOnSyncQueue.
     * 从sync队列的tail向前遍历，如果找到该节点，就返回true
     *
     * @return true if present
     */
    private boolean findNodeFromTail(Node node) {
        Node t = tail;
        for (;;) {
            if (t == node)
                return true;
            if (t == null) // 要么tail为null，要么找到了head的prev，也是null，可以看setHead
                return false;
            t = t.prev;
        }
    }

    /**
     * Transfers a node from a condition queue onto sync queue.
     * Returns true if successful.
     * 将node从condition队列转移到sync队列。
     * 成功转移返回true
     *
     * @param node the node
     * @return true if successfully transferred (else the node was
     * cancelled before signal)
     */
    final boolean transferForSignal(Node node) {
        /*
         * If cannot change waitStatus, the node has been cancelled.
         * 如果不能更改ws，说明这个node已经被取消了。（在condition队列上的node一定是CONDITION状态，如果node存活，一定可以CAS改变ws）
         */
        if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
            return false;

        /*
         * Splice onto queue and try to set waitStatus of predecessor to
         * indicate that thread is (probably) waiting. If cancelled or
         * attempt to set waitStatus fails, wake up to resync (in which
         * case the waitStatus can be transiently and harmlessly wrong).
         * 拼接到队列，尝试设置前驱的ws值，表名当前线程在（可能）等待。
         * 如果前驱被取消或者试图设置前驱的ws失败，则唤醒以重新同步（在这种情况下，waitStatus不匹配可能是暂时且无害的错误）
         * 
         */
        Node p = enq(node); // 加入到sync队列中，并返回前驱节点
        int ws = p.waitStatus;
        if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL)) // 如果前驱的ws>0（说明前驱节点被cancel了），或者设置前驱的ws为SIGNAL失败，那么直接唤醒当前这个node去竞争资源（竞争不到咋办？？？由具体的实现去做，比如重新入队）
            LockSupport.unpark(node.thread);
        return true;
    }

    /**
     * Transfers node, if necessary, to sync queue after a cancelled wait.
     * Returns true if thread was cancelled before being signalled.
     * 如果必要的话，在取消等待后，转移node到sync队列。（这个取消等待，不是在condition队列wait时被CANCEL了，而是wait的条件满足，或者等待时间超时，被唤醒了，才调用的这个方法）
     * 如果线程在收到信号前取消等待了，返回true
     *
     * @param node the node
     * @return true if cancelled before the node was signalled
     */
    final boolean transferAfterCancelledWait(Node node) {
        if (compareAndSetWaitStatus(node, Node.CONDITION, 0)) { // 如果当前状态为CONDITION，则进行入sync队列操作
            enq(node);
            return true;
        }
        // 不为CONDITION状态的话，有两种情况
        // 1、CANCEL 说明当前节点被取消了，但不知道是在队列里被取消还是没在队列里被取消
        // 2、其他状态 说明当前节点在队列里
        // 上面那句话不对，其实不为CONDITION只有一种情况，那就是该node已经进了sync队列，并且ws发生了变化。在condition队列里ws是不会变的。这句话也不对，condition里面状态可以变。
        /*
         * If we lost out to a signal(), then we can't proceed
         * until it finishes its enq().  Cancelling during an
         * incomplete transfer is both rare and transient, so just
         * spin.
         * 如果我们输给了signal方法，那么在它完成enq()之前不能继续做别的。
         * 在未完成转移时取消，是罕见又短暂的，因此只需要自旋。
         */
        while (!isOnSyncQueue(node))
            Thread.yield();
        return false;
    }

    /**
     * Invokes release with current state value; returns saved state.
     * Cancels node and throws exception on failure.
     * 在当前state值下调用release；
     * 返回保存的state值
     * 在失败时取消节点并抛出异常
     * 
     * @param node the condition node for this wait
     * @return previous sync state
     */
    final int fullyRelease(Node node) {
        boolean failed = true;
        try {
            int savedState = getState();
            if (release(savedState)) { // 用当前state值作为release(arg)的入参，如果自定义的tryRelease返回true，从head开始唤醒后继节点
                failed = false;
                return savedState;
            } else {
                throw new IllegalMonitorStateException();
            }
        } finally {
            if (failed)
                node.waitStatus = Node.CANCELLED; // 如果唤醒失败，就取消当前节点（这是个什么操作？）
        }
    }

    // Instrumentation methods for conditions
    // condition的检测方法

    /**
     * Queries whether the given ConditionObject
     * uses this synchronizer as its lock.
     * 查询给定的ConditionObject是否用该同步器作为它的锁
     *
     * @param condition the condition
     * @return {@code true} if owned
     * @throws NullPointerException if the condition is null
     */
    public final boolean owns(ConditionObject condition) {
        return condition.isOwnedBy(this);
    }

    /**
     * Queries whether any threads are waiting on the given condition
     * associated with this synchronizer. Note that because timeouts
     * and interrupts may occur at any time, a {@code true} return
     * does not guarantee that a future {@code signal} will awaken
     * any threads.  This method is designed primarily for use in
     * monitoring of the system state.
     * 查询是否有线程在给定的condition关联的同步器上等待。
     * 注意，由于超时和中断可能在任意时刻发生，返回结果true不保证未来signal可以唤醒线程。
     * 该方法设计的意图是为了在监控系统中使用。
     *
     * @param condition the condition
     * @return {@code true} if there are any waiting threads
     * @throws IllegalMonitorStateException if exclusive synchronization
     *         is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this synchronizer
     * @throws NullPointerException if the condition is null
     */
    public final boolean hasWaiters(ConditionObject condition) {
        if (!owns(condition)) // 如果condition没有关联到本AbstractQueuedSynchronizer（sync)
            throw new IllegalArgumentException("Not owner");
        return condition.hasWaiters(); // 如果有在CONDITION上等待条件的，返回true
    }

    /**
     * Returns an estimate of the number of threads waiting on the
     * given condition associated with this synchronizer. Note that
     * because timeouts and interrupts may occur at any time, the
     * estimate serves only as an upper bound on the actual number of
     * waiters.  This method is designed for use in monitoring of the
     * system state, not for synchronization control.
     * 返回预估的在给定的condition关联的同步器上等待的线程数。
     * 注意，由于超时与中断可能在任意时刻发生，预估的服务数是实际服务数的上限。
     * 该方法只是设计用作监控系统的，不是为了同步控制。
     *
     * @param condition the condition
     * @return the estimated number of waiting threads
     * @throws IllegalMonitorStateException if exclusive synchronization
     *         is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this synchronizer
     * @throws NullPointerException if the condition is null
     */
    public final int getWaitQueueLength(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.getWaitQueueLength();
    }

    /**
     * Returns a collection containing those threads that may be
     * waiting on the given condition associated with this
     * synchronizer.  Because the actual set of threads may change
     * dynamically while constructing this result, the returned
     * collection is only a best-effort estimate. The elements of the
     * returned collection are in no particular order.
     * 返回包含可能在给定condition关联的同步器上等待的线程集合。
     * 由于在结构化生成结果过程中，实际上线程是动态变化的，这个返回结果稽核只是一个尽力预估值。
     * 返回的稽核元素没有特定的顺序。
     *
     * @param condition the condition
     * @return the collection of threads
     * @throws IllegalMonitorStateException if exclusive synchronization
     *         is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this synchronizer
     * @throws NullPointerException if the condition is null
     */
    public final Collection<Thread> getWaitingThreads(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.getWaitingThreads();
    }

    /**
     * Condition implementation for a {@link
     * AbstractQueuedSynchronizer} serving as the basis of a {@link
     * Lock} implementation.
     * 作为AQS服务的条件实现，作为Lock实现的基础。
     *
     * <p>Method documentation for this class describes mechanics,
     * not behavioral specifications from the point of view of Lock
     * and Condition users. Exported versions of this class will in
     * general need to be accompanied by documentation describing
     * condition semantics that rely on those of the associated
     * {@code AbstractQueuedSynchronizer}.
     * 该类的方法说明从使用者的角度描述Lock与Condition的机制，而不是行为规范。
     * 该类的导出版本通常需要跟依赖关联的AQS的条件语义文档一起看。
     *
     * <p>This class is Serializable, but all fields are transient,
     * so deserialized conditions have no waiters.
     * class是可序列化的，不过所有的字段都是transient（暂时的），所以反序列化的condition没有任何waiter。
     *
     */
    public class ConditionObject implements Condition, java.io.Serializable {
        private static final long serialVersionUID = 1173984872572414699L; // 用来验证版本一致性。根据包名，类名，继承关系，非私有的方法和属性，以及参数，返回值等诸多因子计算得出的，极度复杂生成的一个64位的哈希字段。基本上计算出来的这个值是唯一的。默认是1L。
        /** First node of condition queue. */
        private transient Node firstWaiter; // firstWaiter是个Node类型，具有prev、next、thread等属性，还有nextWaiter这种属性
        /** Last node of condition queue. */
        private transient Node lastWaiter; // lastWaiter是个Node类型

        /**
         * Creates a new {@code ConditionObject} instance.
         */
        public ConditionObject() { }

        // Internal methods
        // 内部方法

        /**
         * Adds a new waiter to wait queue.
         * @return its new wait node
         */
        private Node addConditionWaiter() {
            Node t = lastWaiter;
            // If lastWaiter is cancelled, clean out. // 如果最后一个节点取消了，清理掉。
            if (t != null && t.waitStatus != Node.CONDITION) {
                unlinkCancelledWaiters(); // 从头开始进行完整遍历，留下ws=CONDITION的节点
                t = lastWaiter; // unlink会更新lastWaiter，重新指向lastWaiter
            }
            Node node = new Node(Thread.currentThread(), Node.CONDITION); // 封装当前线程为Node，默认ws为CONDITION
            if (t == null) // 没有lastWaiter，说明当前condition队列为空，让firstWaiter指向该节点
                firstWaiter = node;
            else
                t.nextWaiter = node; // 否则让当前lastWaiter的next指向该节点
            lastWaiter = node; // 设置当前节点为lastWaiter
            return node;
        }

        /**
         * Removes and transfers nodes until hit non-cancelled one or
         * null. Split out from signal in part to encourage compilers
         * to inline the case of no waiters.
         * 转移和移除节点，直到命中未取消的节点或者遍历完没有非null节点。
         * 从signal中分离出来，一部分是为了鼓励编译器内联没有waiters的情况。
         *
         * @param first (non-null) the first node on condition queue
         */
        private void doSignal(Node first) {
        	  // 目标就是将firstWaiter节点转移到sync队列里，然后移除该节点。first为非空的firstWaiter。（如果是null，会导致transferForSignal(first)报错）
            do {
                if ( (firstWaiter = first.nextWaiter) == null) // 1、让firstWaiter指向firstWaiter的下一个节点
                    lastWaiter = null;                         // 2、如果下一个节点为空，说明condition队列里没有等待节点了，lastWaiter也置为空
                first.nextWaiter = null;                       // 3、断开即将移入sync队列的节点next引用
            } while (!transferForSignal(first) &&              // 4、如果该节点状态非CONDITION，表示该节点已CANCEL，返回false，表示无法入队，否则将该节点enq入队，如果入队失败，会unpark该节点直接竞争锁
                     (first = firstWaiter) != null);           // 5、让first指向firstWaiter，如果现在第一个节点不为null，并且上一个节点已经被取消了，那么尝试释放下一个节点。
        }

        /**
         * Removes and transfers all nodes.
         * 转移和移除所有节点
         *
         * @param first (non-null) the first node on condition queue
         */
        private void doSignalAll(Node first) {
            lastWaiter = firstWaiter = null; // 反正都要移除了，啥也不管直接都置为null
            do {
                Node next = first.nextWaiter; // 遍历非null的节点，有一个算一个，都给扔到transferForSignal(first)方法中去转移到sync队列里，然后移除。
                first.nextWaiter = null;
                transferForSignal(first);
                first = next;
            } while (first != null);
        }

        /**
         * Unlinks cancelled waiter nodes from condition queue.
         * Called only while holding lock. This is called when
         * cancellation occurred during condition wait, and upon
         * insertion of a new waiter when lastWaiter is seen to have
         * been cancelled. This method is needed to avoid garbage
         * retention in the absence of signals. So even though it may
         * require a full traversal, it comes into play only when
         * timeouts or cancellations occur in the absence of
         * signals. It traverses all nodes rather than stopping at a
         * particular target to unlink all pointers to garbage nodes
         * without requiring many re-traversals during cancellation
         * storms.
         * 从条件队列中取消连接的cancel waiter节点。
         * 只有在持有锁的时候才会被调用。
         * 当在条件等待时发生取消，在插入新的waiter时发现最后的waiter已经被取消时，会调用到该方法。（发现最后一个被取消时会调用，遍历则是从头开始向后遍历）
         * 需要该方法在没有信号（absence 缺席）的情况下避免垃圾保留。
         * 因此，即使它可能需要全部遍历，它也仅在没有信号的情况下发生超时或者取消时才起作用。
         * 它遍历所有节点而不是在特定节点处停止，以取消所有指向垃圾节点的指针，而不需要在取消风暴中多次重新遍历。
         *
         */
        private void unlinkCancelledWaiters() {
            Node t = firstWaiter; // 从第一个节点开始向后遍历
            Node trail = null; // trail保留最后一个未取消的节点引用（每当发现一个后续未取消节点，这个trail就变为指向该节点）
            while (t != null) {
                Node next = t.nextWaiter;
                if (t.waitStatus != Node.CONDITION) { // 如果当前节点的ws不是CONDITION，说明当前节点不再等待了（就是取消了），需要取消连接
                    t.nextWaiter = null; // 断开该节点与下一个节点的关联，
                    if (trail == null) // 如果现在剩余节点link为空，说明现在还没有节点留下来
                        firstWaiter = next; // 把第一个留下来的节点作为firstWaiter
                    else
                        trail.nextWaiter = next; // 否则就把当前节点（准备清理的节点）的下一个节点加入到剩余节点link中（通过trail保留截止到目前最后一个节点的引用，使用next来构建link）
                    if (next == null) // 如果没有后续waiter了
                        lastWaiter = trail; // lastWaiter指向剩余节点link的最后一个节点
                }
                else
                    trail = t; // 当前节点不用取消，link没变化，直接让trail指向当前未取消的节点，继续向后遍历
                t = next; // 准备下一个节点的遍历
            }
        }

        // public methods
        // 公共方法

        /**
         * Moves the longest-waiting thread, if one exists, from the
         * wait queue for this condition to the wait queue for the
         * owning lock.
         * 如果有等待线程的话，将等待时间最长的线程从等待condition的队列转移到等待lock的队列
         *
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        public final void signal() {
            if (!isHeldExclusively()) // 首先要当前线程得持有锁（自己实现的方法返回true）
                throw new IllegalMonitorStateException();
            Node first = firstWaiter; // 排在第一个的线程节点，就是等待时间最长的（因为先来先入队先等待）
            if (first != null) // 如果上来就是null，就不转移了
                doSignal(first);
        }

        /**
         * Moves all threads from the wait queue for this condition to
         * the wait queue for the owning lock.
         * 把所有的线程Node从等待condition的队列转移到等待lock的队列。
         *
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        public final void signalAll() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            Node first = firstWaiter;
            if (first != null)
                doSignalAll(first);
        }

        /**
         * Implements uninterruptible condition wait.
         * 实现非中断的condition等待
         *
         * <ol>
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * </ol>
         */
        public final void awaitUninterruptibly() {
            Node node = addConditionWaiter(); // 将当前线程封装成Node添加到等待condition队列中
            int savedState = fullyRelease(node); // 这里为什么要去释放呢？释放不成功还会直接cancel  这里结合场景说一下，如果当前线程想要在condition上做await操作，那么它一定是已经获取到锁了，这是第一。第二，已获取到锁的线程需要await，那么它一定要释放锁，把资源交出去，直到它被唤醒进行竞争。
            boolean interrupted = false;
            while (!isOnSyncQueue(node)) { // 判断当前node是否已在sync队列里（就是等待lock的队列），如果没有的话，就自己阻塞了
                LockSupport.park(this);
                if (Thread.interrupted()) // 即使有中断，也只是记录状态，不响应
                    interrupted = true;
            }
            if (acquireQueued(node, savedState) || interrupted) // 能到这里，说明已经在sync队列里了，尝试获取锁，获取失败也阻塞。如果在加入sync队列时发生了中断，或者在sync获取锁的时候发生中断，都会重新中断。
                selfInterrupt();
        }

        /*
         * For interruptible waits, we need to track whether to throw
         * InterruptedException, if interrupted while blocked on
         * condition, versus reinterrupt current thread, if
         * interrupted while blocked waiting to re-acquire.
         * 对于可中断的waits，需要跟踪是否抛出InterruptedException，
         * 如果在condition阻塞过程中发生中断，
         * 相对的重新中断当前线程
         * 如果在等待重新获取的阻塞时发生中断
         */

        /** Mode meaning to reinterrupt on exit from wait */
        // 在退出时重新中断
        private static final int REINTERRUPT =  1;
        /** Mode meaning to throw InterruptedException on exit from wait */
        // 在退出时抛出中断异常
        private static final int THROW_IE    = -1;

        /**
         * Checks for interrupt, returning THROW_IE if interrupted
         * before signalled, REINTERRUPT if after signalled, or
         * 0 if not interrupted.
         * 检查中断
         * 如果在获取到signal之前发生中断，返回THROW_IE
         * 如果在获取到signal之后发生中断，返回REINTERRUPT
         * 没有中断发生，返回0
         */
        private int checkInterruptWhileWaiting(Node node) {
            return Thread.interrupted() ?
                (transferAfterCancelledWait(node) ? THROW_IE : REINTERRUPT) : // transferAfterCancelledWait如果能成功将ws从CONDITION更新成0，会尝试将node加入到sync队列，返回true表示被signal之前被cancel
                0;
        }

        /**
         * Throws InterruptedException, reinterrupts current thread, or
         * does nothing, depending on mode.
         * 根据模式，抛出中断异常，或者重新中断当前线程，或者啥也不做
         *
         */
        private void reportInterruptAfterWait(int interruptMode)
            throws InterruptedException {
            if (interruptMode == THROW_IE)
                throw new InterruptedException();
            else if (interruptMode == REINTERRUPT)
                selfInterrupt();
        }

        /**
         * Implements interruptible condition wait.
         * 实现可中断condition等待
         *
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled or interrupted.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * 1、如果当前线程被中断，抛出异常。
         * 2、保存由getState返回的锁的state值。
         * 3、调用release方法，将保存的state值作为参数，如果release失败，抛出IllegalMonitorStateException异常。（说明该线程的操作非法，类似于Ojbect上的wait与notify）
         * 4、阻塞，直到被唤醒或者被中断
         * 5、通过使用保存的state值，调用特殊版本的acquire方法，来重新获取。
         * 6、如果在第4步阻塞时被中断，抛出中断异常。
         *
         * </ol>
         */
        public final void await() throws InterruptedException {
            if (Thread.interrupted())                                           // 0、上来先看中断状态，如果已经中断了，直接抛出异常。
                throw new InterruptedException();
            Node node = addConditionWaiter();                                   // 1、将当前线程加入到Condition队列中。与不可中断的await一样
            int savedState = fullyRelease(node);                                // 2、当前线程放弃对锁的竞争，释放资源唤醒后继进行锁获取
            int interruptMode = 0;                                              // 
            while (!isOnSyncQueue(node)) {                                      // 3、判断当前线程node是否在sync队列里
                LockSupport.park(this);                                         // 4、如果不在sync队列，说明没有满足的Condition，进行park
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)    // 5、检查在wait时发生的中断，如果没有中断，返回0，当返回值!=0时，跳出循环等待
                    break;
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)    // 6、调用不可中断的acquireQueued方法竞争，竞争成功返回中断状态。如果竞争时发生中断，并且中断模式为THROW_IE（在获取到信号之前就被中断了）
                interruptMode = REINTERRUPT;                                     // 7、中断模式改为REINTERRUPT（在获取信号之后被中断）
            if (node.nextWaiter != null) // clean up if cancelled               // 8、如果当前节点的nextWaiter不为null，从CONDITION队列的firstWaiter开始清理一遍非CONDITION状态的节点
                unlinkCancelledWaiters();
            if (interruptMode != 0)                                              // 9、如果中断模式不是0（意味着发生过中断），按照中断模式，调用reportInterruptAfterWait方法抛出异常或者恢复中断状态（就是重新将中断标识位置为中断）
                reportInterruptAfterWait(interruptMode);
        }

        /**
         * Implements timed condition wait.
         * 实现超时condition等待
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * 1、如果当前线程被中断，抛出InterruptedException。
         * 2、保存getState方法返回的state值。
         * 3、调用release方法，使用保存的state值作为参数。如果调用失败，抛出IllegalMonitorStateException。
         * 4、阻塞，直到被唤醒，或者被中断，或者超时。
         * 5、通过带着保存的state值调用特殊版本的acquire方法，来重新获取。
         * 6、如果在步骤4阻塞时被中断，抛出中断异常。
         *
         * </ol>
         */
        public final long awaitNanos(long nanosTimeout)
                throws InterruptedException {
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            final long deadline = System.nanoTime() + nanosTimeout;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (nanosTimeout <= 0L) {
                    transferAfterCancelledWait(node); // 如果超时时间<=0，调用transferAfterCancelledWait方法尝试进行该node入sync队列。
                    break;
                }
                if (nanosTimeout >= spinForTimeoutThreshold) // 如果剩余的超时时间比自旋的等待时间阈值高，那么直接park
                    LockSupport.parkNanos(this, nanosTimeout);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0) // 检查是否发生过中断，如果有，跳出循环
                    break;
                nanosTimeout = deadline - System.nanoTime();
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return deadline - System.nanoTime(); // 返回剩余的等待纳秒数
        }

        /**
         * Implements absolute timed condition wait.
         * 实现绝对定时condition等待。
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * <li> If timed out while blocked in step 4, return false, else true.
         * 1、如果当前线程被中断，抛出InterruptedException。
         * 2、保存getState方法返回的state值。
         * 3、调用release方法，使用保存的state值作为参数。如果调用失败，抛出IllegalMonitorStateException。
         * 4、阻塞，直到被唤醒，或者被中断，或者超时。
         * 5、通过带着保存的state值调用特殊版本的acquire方法，来重新获取。
         * 6、如果在步骤4阻塞时被中断，抛出中断异常。
         * 7、如果在步骤4超时了，返回false，否则返回true。         
         * </ol>
         */
        public final boolean awaitUntil(Date deadline)
                throws InterruptedException {
            long abstime = deadline.getTime();
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (System.currentTimeMillis() > abstime) {
                    timedout = transferAfterCancelledWait(node); // transferAfterCancelledWait如果在信号之前取消了wait，返回true（超时了返回true）
                    break;
                }
                LockSupport.parkUntil(this, abstime);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return !timedout; // 超时了返回false，否则返回true
        }

        /**
         * Implements timed condition wait.
         * 实现超时condition等待
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * <li> If timed out while blocked in step 4, return false, else true.
         * 1、如果当前线程被中断，抛出InterruptedException。
         * 2、保存getState方法返回的state值。
         * 3、调用release方法，使用保存的state值作为参数。如果调用失败，抛出IllegalMonitorStateException。
         * 4、阻塞，直到被唤醒，或者被中断，或者超时。
         * 5、通过带着保存的state值调用特殊版本的acquire方法，来重新获取。
         * 6、如果在步骤4阻塞时被中断，抛出中断异常。
         * 7、如果在步骤4超时了，返回false，否则返回true。
         * </ol>
         */
        public final boolean await(long time, TimeUnit unit)
                throws InterruptedException {
            long nanosTimeout = unit.toNanos(time);
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            final long deadline = System.nanoTime() + nanosTimeout;
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (nanosTimeout <= 0L) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout >= spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
                nanosTimeout = deadline - System.nanoTime();
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return !timedout;
        }

        //  support for instrumentation

        /**
         * Returns true if this condition was created by the given
         * synchronization object.
         * 如果该condition是通过给定的同步器创建的，返回true
         *
         * @return {@code true} if owned
         */
        final boolean isOwnedBy(AbstractQueuedSynchronizer sync) {
            return sync == AbstractQueuedSynchronizer.this;
        }

        /**
         * Queries whether any threads are waiting on this condition.
         * Implements {@link AbstractQueuedSynchronizer#hasWaiters(ConditionObject)}.
         * 查询是否在该condition上有线程在等待。
         *
         * @return {@code true} if there are any waiting threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        protected final boolean hasWaiters() {
            if (!isHeldExclusively()) // 如果当前线程没有独占锁，抛出异常，这个方法是需要独占锁自己实现的三个方法之一。
                throw new IllegalMonitorStateException();
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) { // 从第一个等待节点开始找，如果有waitStatus是CONDITION的，表示在等待条件，返回true
                if (w.waitStatus == Node.CONDITION) // （在CONDITION队列上的，ws只有CONDITION跟CANCEL状态么？）
                    return true;
            }
            return false;
        }

        /**
         * Returns an estimate of the number of threads waiting on
         * this condition.
         * Implements {@link AbstractQueuedSynchronizer#getWaitQueueLength(ConditionObject)}.
         * 返回预估的在该condition上等待的线程数
         * AbstractQueuedSynchronizer的getWaitQueueLength(ConditionObject)方法会调用
         *
         * @return the estimated number of waiting threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        protected final int getWaitQueueLength() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            int n = 0;
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION)
                    ++n;
            }
            return n;
        }

        /**
         * Returns a collection containing those threads that may be
         * waiting on this Condition.
         * Implements {@link AbstractQueuedSynchronizer#getWaitingThreads(ConditionObject)}.
         * 返回包含可能在该Condition上等待的线程集合。
         *
         * @return the collection of threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        protected final Collection<Thread> getWaitingThreads() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            ArrayList<Thread> list = new ArrayList<Thread>();
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION) {
                    Thread t = w.thread;
                    if (t != null)
                        list.add(t);
                }
            }
            return list;
        }
    }

    /**
     * Setup to support compareAndSet. We need to natively implement
     * this here: For the sake of permitting future enhancements, we
     * cannot explicitly subclass AtomicInteger, which would be
     * efficient and useful otherwise. So, as the lesser of evils, we
     * natively implement using hotspot intrinsics API. And while we
     * are at it, we do the same for other CASable fields (which could
     * otherwise be done with atomic field updaters).
     * 设置以支持CAS。
     * 需要在这里进行本地实现：
     * 为了允许未来增强，我们不能显式集成AtomicInteger类，否则就是有效和有用的。（如果不是为了增强，就可以用AtomicXXX操作了）
     * 所以，作为较小的弊端，本地实现使用hotspot内在函数API。
     * 当我们这样做时，对其他可以使用CAS字段也这样做（否则可以使用原子字段更新程序来完成）
     */
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final long stateOffset;
    private static final long headOffset;
    private static final long tailOffset;
    private static final long waitStatusOffset;
    private static final long nextOffset;

    static {
        try {
            stateOffset = unsafe.objectFieldOffset
                (AbstractQueuedSynchronizer.class.getDeclaredField("state"));
            headOffset = unsafe.objectFieldOffset
                (AbstractQueuedSynchronizer.class.getDeclaredField("head"));
            tailOffset = unsafe.objectFieldOffset
                (AbstractQueuedSynchronizer.class.getDeclaredField("tail"));
            waitStatusOffset = unsafe.objectFieldOffset
                (Node.class.getDeclaredField("waitStatus"));
            nextOffset = unsafe.objectFieldOffset
                (Node.class.getDeclaredField("next"));

        } catch (Exception ex) { throw new Error(ex); }
    }

    /**
     * CAS head field. Used only by enq.
     */
    private final boolean compareAndSetHead(Node update) {
        return unsafe.compareAndSwapObject(this, headOffset, null, update);
    }

    /**
     * CAS tail field. Used only by enq.
     */
    private final boolean compareAndSetTail(Node expect, Node update) {
        return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
    }

    /**
     * CAS waitStatus field of a node.
     */
    private static final boolean compareAndSetWaitStatus(Node node,
                                                         int expect,
                                                         int update) {
        return unsafe.compareAndSwapInt(node, waitStatusOffset,
                                        expect, update);
    }

    /**
     * CAS next field of a node.
     */
    private static final boolean compareAndSetNext(Node node,
                                                   Node expect,
                                                   Node update) {
        return unsafe.compareAndSwapObject(node, nextOffset, expect, update);
    }
}
```