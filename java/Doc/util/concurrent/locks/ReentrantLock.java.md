# ReentrantLock
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
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent.locks;
import java.util.concurrent.TimeUnit;
import java.util.Collection;

/**
 * A reentrant mutual exclusion {@link Lock} with the same basic
 * behavior and semantics as the implicit monitor lock accessed using
 * {@code synchronized} methods and statements, but with extended
 * capabilities.
 * 可重入的互斥锁与使用synchronized方法或语句访问的隐式监视器锁具有相同的行为和语义，不过提供了扩展功能
 *
 * <p>A {@code ReentrantLock} is <em>owned</em> by the thread last
 * successfully locking, but not yet unlocking it. A thread invoking
 * {@code lock} will return, successfully acquiring the lock, when
 * the lock is not owned by another thread. The method will return
 * immediately if the current thread already owns the lock. This can
 * be checked using methods {@link #isHeldByCurrentThread}, and {@link
 * #getHoldCount}.
 * ReentrantLock属于上次成功加锁并且还没解锁的线程。当lock不属于其他线程时，调用Lock方法的线程会返回并成功加锁。
 * 如果该线程已拥有该锁，那么Lock方法会立即返回。可以调用isHeldByCurrentThread与getHoldCount方法来检查锁的相关信息。
 *
 * <p>The constructor for this class accepts an optional
 * <em>fairness</em> parameter.  When set {@code true}, under
 * contention, locks favor granting access to the longest-waiting
 * thread.  Otherwise this lock does not guarantee any particular
 * access order.  Programs using fair locks accessed by many threads
 * may display lower overall throughput (i.e., are slower; often much
 * slower) than those using the default setting, but have smaller
 * variances in times to obtain locks and guarantee lack of
 * starvation. Note however, that fairness of locks does not guarantee
 * fairness of thread scheduling. Thus, one of many threads using a
 * fair lock may obtain it multiple times in succession while other
 * active threads are not progressing and not currently holding the
 * lock.
 * Also note that the untimed {@link #tryLock()} method does not
 * honor the fairness setting. It will succeed if the lock
 * is available even if other threads are waiting.
 * ReetrantLock的构造方法可以接收一个公平参数，如果设置该参数为true，在有竞争的情况下，lock偏向于授予等待时间最长的线程。否则这个锁不能保证任何的特定的访问顺序。
 * 通过多线程访问使用公平锁的程序，相较于使用默认（即非公平锁）设置，会显现出较低的吞吐量（即更慢，通常慢很多），但是在获取锁与保证不出现饥饿的时间上具有较小的差异。
 * 然而需要注意的是，锁的公平性并不保证线程调度的公平性。因此，使用公平锁的许多线程中的某一个会连续多次的获得锁（不断重入？），其他active线程并没有进行和持有该锁
 *
 * 另外需要注意的是，无时间的tryLock()方法没有公平性的设置，即使有其他线程在等待，当锁可用时也会返回true
 *
 * <p>It is recommended practice to <em>always</em> immediately
 * follow a call to {@code lock} with a {@code try} block, most
 * typically in a before/after construction such as:
 * 推荐的做法是“总是”在使用lock后马上调用try方法块，最常见的做法是在构造函数之前/之后
 *
 *  <pre> {@code
 * class X {
 *   private final ReentrantLock lock = new ReentrantLock();
 *   // ...
 *
 *   public void m() {
 *     lock.lock();  // block until condition holds
 *     try {
 *       // ... method body
 *     } finally {
 *       lock.unlock() //  因为不能自己释放锁，所以要确保try final手动释放锁
 *     }
 *   }
 * }}</pre>
 *
 * <p>In addition to implementing the {@link Lock} interface, this
 * class defines a number of {@code public} and {@code protected}
 * methods for inspecting the state of the lock.  Some of these
 * methods are only useful for instrumentation and monitoring.
 * 除了实现Lock接口，该class还定义了一些public和protected方法来检查锁的状态，其中的一些方法只对仪表盘与监控有用
 *
 * <p>Serialization of this class behaves in the same way as built-in
 * locks: a deserialized lock is in the unlocked state, regardless of
 * its state when serialized.
 * 此类的序列化与内置锁的行为方式一致；反序列化的锁处于解锁状态，无论其序列化时的状态如何。？？？
 *
 * <p>This lock supports a maximum of 2147483647 recursive locks by
 * the same thread. Attempts to exceed this limit result in
 * {@link Error} throws from locking methods.
 * 该锁最大支持同一个线程建立2147483647个递归锁，尝试超过此限制会导致抛出Error异常（通过递归的方式重入2^31 - 1次)
 *
 * @since 1.5
 * @author Doug Lea
 */
public class ReentrantLock implements Lock, java.io.Serializable {
    private static final long serialVersionUID = 7373984872572414699L;
    /** Synchronizer providing all implementation mechanics */
    // 提供所有实现机制的同步器
    private final Sync sync; // 用Sync来实现锁，它包含子类公平/非公平锁类的实现，与AQS的默认实现

    /**
     * Base of synchronization control for this lock. Subclassed
     * into fair and nonfair versions below. Uses AQS state to
     * represent the number of holds on the lock.
     * ReentrantLock的同步控制基础（根基）。可实现公平锁与非公平锁版本。使用AQS的state值来表示锁的持有次数。
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = -5179523762034025860L;

        /**
         * Performs {@link Lock#lock}. The main reason for subclassing
         * is to allow fast path for nonfair version.
         * lock方法。子类化（abstract）的主要原因是为了快速支持非公平版本。
         */
        abstract void lock();

        /**
         * Performs non-fair tryLock.  tryAcquire is implemented in
         * subclasses, but both need nonfair try for trylock method.
         * 执行非公平的tryLock。
         * tryAcquire是在子类中实现，但是nofaireTryAcquire与tryAcquire都需要对tryLock方法进行非公平尝试。
         */
        final boolean nonfairTryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) { // 当前AQS没有被占用
                if (compareAndSetState(0, acquires)) { // 设置state状态，这一步很关键，如果多线程同时调用该tryAcquire方法，只有一个线程能CAS成功，成为锁的拥有者，其他线程只能失败
                    setExclusiveOwnerThread(current); // 调用AQS继承的AbstarctOwnableSynchronizer类的方法，设置独占锁的当前拥有者
                    return true;
                }
            }
            else if (current == getExclusiveOwnerThread()) { // state!=0的情况下，如果独占锁当前的拥有者是本线程，表示可重入
                int nextc = c + acquires; // 计算AQS state的新值
                if (nextc < 0) // overflow // 越界了
                    throw new Error("Maximum lock count exceeded");
                setState(nextc); // 设置AQS的state新值，通过volatile保证的内存可见性
                return true;
            }
            return false; // 即不能占有锁，也不能重入，返回false
        }

        // 尝试释放（正常情况下，调用该方法的线程一定为独占锁的拥有者）
        protected final boolean tryRelease(int releases) {
            int c = getState() - releases; // 计算AQS state的新值
            if (Thread.currentThread() != getExclusiveOwnerThread()) // 如果当前线程不是独占锁的拥有者，说明调用的有问题，直接抛出异常
                throw new IllegalMonitorStateException();
            boolean free = false; // free表示该线程是否已完全释放锁（重入次数=0）
            if (c == 0) {
                free = true;
                setExclusiveOwnerThread(null); // 当前线程已完全释放锁，设置独占锁的拥有者为null
            }
            setState(c); // 设置AQS state的新值
            return free; // 如果该线程完全释放锁，返回true，否则返回false
        }

        protected final boolean isHeldExclusively() {
            // While we must in general read state before owner,
            // we don't need to do so to check if current thread is owner
            // 虽然通常来说，我们在成为（独占锁）拥有者之前一定要读state值，但如果要判断当前线程是否为拥有者的时候，不必那么做。直接调用该方法就行。
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        // 没实现Lock接口，但是声明了一个final方法，直接生成一个AQS里面的ConditionObject对象。
        final ConditionObject newCondition() {
            return new ConditionObject();
        }

        // Methods relayed from outer class
        // 从外部类继承的方法？还是说这些方法是给外部类用的

        final Thread getOwner() {
            return getState() == 0 ? null : getExclusiveOwnerThread(); // 先判断state再判断exclusiveOwner（exclusiveOwnerThread只保证记录最后一个加锁成功的线程，不保证没有owner时该字段为null）
        }

        final int getHoldCount() {
            return isHeldExclusively() ? getState() : 0;
        }

        final boolean isLocked() {
            return getState() != 0; // 通过state判断是否有加锁
        }

        /**
         * Reconstitutes the instance from a stream (that is, deserializes it).
         * 从流中重构实例（即进行反序列化）
         */
        private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
            s.defaultReadObject();
            setState(0); // reset to unlocked state
        }
    }

    /**
     * Sync object for non-fair locks
     * 非公平锁
     */
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = 7316153563782823691L;

        /**
         * Performs lock.  Try immediate barge, backing up to normal
         * acquire on failure.
         * 加锁。试图立即抢占（barge），在失败时退回到正常获取。
         * 
         */
        final void lock() {
            if (compareAndSetState(0, 1))                         // 1、如果state=0，表明现在没有线程抢占到锁，开始CAS抢占。这个方法是AQS里写的，直接设置state值
                setExclusiveOwnerThread(Thread.currentThread());  // 2、如果抢占成功，设置当前独占锁的拥有者为自己
            else
                acquire(1);                                       // 3、如果抢占失败，调用Sync继承的AQS里acquire方法（开始调用NonfaireSync（本类）实现的tryAcquire方法，如果加锁失败，将线程入AQS的sync队列等待）
        }

        protected final boolean tryAcquire(int acquires) {     // 子类实现的AQS里独占模式需要重写的3种方法之一：tryAcquire。（剩下的两种是继承的Sync类里实现了：tryRelease与isHeldExclusively）
            return nonfairTryAcquire(acquires);                   // 调用的Sync实现的方法
        }
    }

    /**
     * Sync object for fair locks
     * 公平锁
     */
    static final class FairSync extends Sync {
        private static final long serialVersionUID = -3000897897090466540L;

        final void lock() {
            acquire(1);                                           // 直接调用AQS里的方法（尝试tryAcquire，如果加锁失败，入AQS的sync队列）
        }

        /**
         * Fair version of tryAcquire.  Don't grant access unless
         * recursive call or no waiters or is first.
         * tryAcquire的公平版本。
         * 除非是递归调用（重入）或者是sync等待队列的第一个waiter，否则不授予访问权限
         */
        protected final boolean tryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {                                      // 1、分析当前state状态，如果为0，包含两种情况：1、sync队列没有等待线程；2、sync的head在锁释放后还没来得及竞争加锁。
                if (!hasQueuedPredecessors() &&                // 2、分析当前线程之前有没有在sync队列里等待的线程（调用的AQS里实现的方法，有的话返回true，没有返回false）
                    compareAndSetState(0, acquires)) {         // 3、如果该线程前面没有等待的线程，CAS来设置state值以抢占锁（这时候也可能有其他线程也在抢占锁）
                    setExclusiveOwnerThread(current);          // 4、抢占成功，设置当前独占锁的拥有者为当前线程
                    return true;                              // 5、返回true，抢占成功（不用排队了）
                }
            }
            else if (current == getExclusiveOwnerThread()) {   // 6、如果当前state状态不为0，再分析当前独占锁的拥有者是否为当前线程
                int nextc = c + acquires;                      // 7、如果当前线程是拥有者，将state值再次增加，表示重入
                if (nextc < 0)                                 // 8、如果递归重入次数过多(大于2^32 - 1移除)，抛出异常
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);                                // 9、设置state值
                return true;                                   // 10、返回true，重入成功
            }
            return false;                                      // 11、返回false，抢占/重入失败，需要排队了
        }
    }

    /**
     * Creates an instance of {@code ReentrantLock}.
     * This is equivalent to using {@code ReentrantLock(false)}.
     * 创建ReentrantLock实例（默认是非公平锁实现）
     * 与使用ReentrantLock(false)是等价的
     */
    public ReentrantLock() {
        sync = new NonfairSync();
    }

    /**
     * Creates an instance of {@code ReentrantLock} with the
     * given fairness policy.
     * 创建ReentrantLock实例，使用给定的公平策略。
     *
     * @param fair {@code true} if this lock should use a fair ordering policy
     *              true表示使用公平锁
     */
    public ReentrantLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync(); // 用sync来
    }


    // 下面这些方法是对Lock接口的实现

    /**
     * Acquires the lock.
     * 获取锁
     *
     * <p>Acquires the lock if it is not held by another thread and returns
     * immediately, setting the lock hold count to one.
     * 如果没有被其他线程持有，则获取锁并立即返回，设置锁的持有数量为1.
     *
     * <p>If the current thread already holds the lock then the hold
     * count is incremented by one and the method returns immediately.
     * 如果当前线程已经持有该锁，那么锁的持有数量加1，并且方法立即返回
     *
     * <p>If the lock is held by another thread then the
     * current thread becomes disabled for thread scheduling
     * purposes and lies dormant until the lock has been acquired,
     * at which time the lock hold count is set to one.
     * 如果该锁已被其他线程持有，那么当前线程被禁止用于线程调度并且挂起（休眠），直到锁能够获取，到那时设置锁的持有数为1
     *
     */
    public void lock() {
        sync.lock();
    }

    /**
     * Acquires the lock unless the current thread is
     * {@linkplain Thread#interrupt interrupted}.
     * 获取锁，除非当前线程被中断。
     *
     * <p>Acquires the lock if it is not held by another thread and returns
     * immediately, setting the lock hold count to one.
     * 如果锁没有被其他线程持有，加锁并且立即返回，设置锁持有数为1。
     *
     * <p>If the current thread already holds this lock then the hold count
     * is incremented by one and the method returns immediately.
     * 如果当前线程已经持有该锁，那么将持有数加1，并且立即返回。
     *
     * <p>If the lock is held by another thread then the
     * current thread becomes disabled for thread scheduling
     * purposes and lies dormant until one of two things happens:
     * 如果该锁已被其他线程获取，那么当前线程对于线程调度不可用并且挂起（休眠）直到以下两个事件发生任意一个：
     *
     * <ul>
     *
     * <li>The lock is acquired by the current thread; or
     * 线程成功获取到锁；
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread.
     * 其他线程中断了当前线程。
     *
     * </ul>
     *
     * <p>If the lock is acquired by the current thread then the lock hold
     * count is set to one.
     * 如果锁被当前线程成功获取，那么锁的持有数设置为1。
     *
     * <p>If the current thread:
     * 如果当前线程：
     * <ul>
     *
     * <li>has its interrupted status set on entry to this method; or
     * 在进入该方法前被设置了中断状态；
     *
     * <li>is {@linkplain Thread#interrupt interrupted} while acquiring
     * the lock,
     * 在获取锁的过程中被中断，
     *
     * </ul>
     *
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     * 那么抛出中断异常，并且清除当前线程的中断状态（线程的中断status由“中断”变为“无中断”）
     *
     * <p>In this implementation, as this method is an explicit
     * interruption point, preference is given to responding to the
     * interrupt over normal or reentrant acquisition of the lock.
     * 在此实现中，这个方法是个明显的中断点，所以优先（preference）响应中断而不是正常或者可重入的获取锁
     *
     * @throws InterruptedException if the current thread is interrupted
     */
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1); // 调用的AQS方法，如果进来时已经被中断，直接抛出异常，否则就调用AQS的doAcquireInterruptibly方法去排队竞争锁，如果排队过程中有中断，也抛出异常并退出等待
    }

    /**
     * Acquires the lock only if it is not held by another thread at the time
     * of invocation.
     * 获取锁，只有在调用时没有被其他线程持有该锁。
     *
     * <p>Acquires the lock if it is not held by another thread and
     * returns immediately with the value {@code true}, setting the
     * lock hold count to one. Even when this lock has been set to use a
     * fair ordering policy, a call to {@code tryLock()} <em>will</em>
     * immediately acquire the lock if it is available, whether or not
     * other threads are currently waiting for the lock.
     * This &quot;barging&quot; behavior can be useful in certain
     * circumstances, even though it breaks fairness. If you want to honor
     * the fairness setting for this lock, then use
     * {@link #tryLock(long, TimeUnit) tryLock(0, TimeUnit.SECONDS) }
     * which is almost equivalent (it also detects interruption).
     * 如果没有被其他线程持有则获取锁，并立即返回true，设置锁持有数为1。
     * 即使设置使用公平策略加锁，在锁可用的情况下，调用tryLock将立即获取锁，不管是否有其他线程正在sync队列上等待锁。
     * 抢占在某些（certain）情况（circumstances）下是有用的，即使破坏了公平性。
     * 如果想保持公平性，那么使用tryLock(long, TimeUnit)、tryLock(0, TimeUnit.SECONDS)，这俩几乎是等效的。
     *
     * <p>If the current thread already holds this lock then the hold
     * count is incremented by one and the method returns {@code true}.
     * 如果当前线程已经持有锁了，那么持有数加1，并且返回true。
     *
     * <p>If the lock is held by another thread then this method will return
     * immediately with the value {@code false}.
     * 如果锁已经被其他线程持有，那么该方法将立即返回false。
     *
     * @return {@code true} if the lock was free and was acquired by the
     *         current thread, or the lock was already held by the current
     *         thread; and {@code false} otherwise
     */
    public boolean tryLock() {
        return sync.nonfairTryAcquire(1); // 不管公平不公平，直接调用Sync的nonfairTryAcquire方法，试图抢占锁，加锁失败返回false。
    }

    /**
     * Acquires the lock if it is not held by another thread within the given
     * waiting time and the current thread has not been
     * {@linkplain Thread#interrupt interrupted}.
     * 获取锁，如果在等待时间内没有被其他线程持有（在等待时间内有时间点锁有空闲），并且当前线程没有被中断。
     *
     * <p>Acquires the lock if it is not held by another thread and returns
     * immediately with the value {@code true}, setting the lock hold count
     * to one. If this lock has been set to use a fair ordering policy then
     * an available lock <em>will not</em> be acquired if any other threads
     * are waiting for the lock. This is in contrast to the {@link #tryLock()}
     * method. If you want a timed {@code tryLock} that does permit barging on
     * a fair lock then combine the timed and un-timed forms together:
     * 获取锁，如果在等待时间内没有被其他线程持有，将立即返回true，设置锁的持有数为1。
     * 如果设置使用公平策略加锁，有线程在排队等锁的话，那么即使锁可用也不会成功获取锁。
     * 这与tryLock()方法相反（contrast 比对）。
     * 如果在公平锁下，既想用限时的tryLock()，又想允许抢占，那么联合使用限时与不限时的形式：
     *
     *  <pre> {@code
     * if (lock.tryLock() ||
     *     lock.tryLock(timeout, unit)) { // 先试试抢占，如果抢不到就在等待时间内尝试
     *   ...
     * }}</pre>
     *
     * <p>If the current thread
     * already holds this lock then the hold count is incremented by one and
     * the method returns {@code true}.
     * 如果当前线程已经持有锁，那么持有数加1，并且方法返回true。
     *
     * <p>If the lock is held by another thread then the
     * current thread becomes disabled for thread scheduling
     * purposes and lies dormant until one of three things happens:
     * 如果该锁已被其他线程持有，那么当前线程对线程调度不可用并且挂起（休眠），直到以下三种情况任意一种发生：
     *
     * <ul>
     *
     * <li>The lock is acquired by the current thread; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     *
     * <li>The specified waiting time elapses
     * 1、当前线程获取到锁；
     * 2、当前线程被其他线程中断；
     * 3、指定的等待时间超时；
     *
     * </ul>
     *
     * <p>If the lock is acquired then the value {@code true} is returned and
     * the lock hold count is set to one.
     *
     * <p>If the current thread:
     *
     * <ul>
     *
     * <li>has its interrupted status set on entry to this method; or
     *
     * <li>is {@linkplain Thread#interrupt interrupted} while
     * acquiring the lock,
     *
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     * 如果当前线程：
     * 1、在进入该方法之前已被中断；
     * 2、在等待获取锁时被中断；
     * 那么将抛出InterruptedException，并且当前线程的interrupted status清空。
     *
     * <p>If the specified waiting time elapses then the value {@code false}
     * is returned.  If the time is less than or equal to zero, the method
     * will not wait at all.
     * 如果指定等待时间超时，那么将返回false。
     * 如果指定的等待时间<=0，那么该方法将不会等待。
     *
     * <p>In this implementation, as this method is an explicit
     * interruption point, preference is given to responding to the
     * interrupt over normal or reentrant acquisition of the lock, and
     * over reporting the elapse of the waiting time.
     * 优先响应中断，而不是正常或者重入的获取锁，也不是响应已超时时间。
     *
     * @param timeout the time to wait for the lock
     * @param unit the time unit of the timeout argument
     * @return {@code true} if the lock was free and was acquired by the
     *         current thread, or the lock was already held by the current
     *         thread; and {@code false} if the waiting time elapsed before
     *         the lock could be acquired
     * @throws InterruptedException if the current thread is interrupted
     * @throws NullPointerException if the time unit is null
     */
    public boolean tryLock(long timeout, TimeUnit unit)
            throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(timeout)); // AQS的tryAcquireNanos，先尝试本类(sync)实现的tryAcquire，如果失败，则调用AQS的doAcquireNanos，将该线程入队，等待获取锁。
    }

    /**
     * Attempts to release this lock.
     * 释放锁
     *
     * <p>If the current thread is the holder of this lock then the hold
     * count is decremented.  If the hold count is now zero then the lock
     * is released.  If the current thread is not the holder of this
     * lock then {@link IllegalMonitorStateException} is thrown.
     * 如果当前线程持有该锁，那么持有数递减。
     * 如果锁的持有数成了0，那么当前锁被释放。
     * 如果当前线程不是该锁的持有者，那么抛出IllegalMonitorStateException（大概是非法修改监视器状态异常）
     *
     * @throws IllegalMonitorStateException if the current thread does not
     *         hold this lock
     */
    public void unlock() {
        sync.release(1); // 调用AQS实现的release方法，先调用本类（sync）实现的tryRelease，如果可以，从等待队列的head处unpark一个后继。
    }

    /**
     * Returns a {@link Condition} instance for use with this
     * {@link Lock} instance.
     * 返回一个与该Lock实例一起使用的Condition实例。
     *
     * <p>The returned {@link Condition} instance supports the same
     * usages as do the {@link Object} monitor methods ({@link
     * Object#wait() wait}, {@link Object#notify notify}, and {@link
     * Object#notifyAll notifyAll}) when used with the built-in
     * monitor lock.
     * 当与内置的监视器锁一起使用时，返回的Condition实例支持与Object监视器相同的使用方法，例如wait、notify和notifyAll。
     *
     * <ul>
     *
     * <li>If this lock is not held when any of the {@link Condition}
     * {@linkplain Condition#await() waiting} or {@linkplain
     * Condition#signal signalling} methods are called, then an {@link
     * IllegalMonitorStateException} is thrown.
     * 如果在调用Condition的waiting、signalling等方法时，关联的该锁没有被当前线程持有，那么将抛出IllegalMonitorStateException。
     *
     * <li>When the condition {@linkplain Condition#await() waiting}
     * methods are called the lock is released and, before they
     * return, the lock is reacquired and the lock hold count restored
     * to what it was when the method was called.
     * 当调用Condition的waiting方法时，持有的锁将释放。 
     * 在该方法返回时，将重新持有锁，并且将锁的持有数恢复到调用该方法时的状态。
     * （锁的释放与重新获取）
     *
     * <li>If a thread is {@linkplain Thread#interrupt interrupted}
     * while waiting then the wait will terminate, an {@link
     * InterruptedException} will be thrown, and the thread's
     * interrupted status will be cleared.
     * 如果线程在等待过程中被中断，那么wait将终止，抛出中断异常，并且线程的interrupted state被清理。
     *
     * <li> Waiting threads are signalled in FIFO order.
     * 等待线程按FIFO顺序被唤醒（signalled 接收信号）
     *
     * <li>The ordering of lock reacquisition for threads returning
     * from waiting methods is the same as for threads initially
     * acquiring the lock, which is in the default case not specified,
     * but for <em>fair</em> locks favors those threads that have been
     * waiting the longest.
     * 从等待方法返回的线程重新获取锁的顺序与最初获取锁的线程（方式）相同，在默认情况下没有指定，但是对于公平锁，那些队列中等待时间最长的线程要比该线程优先。
     *
     * </ul>
     *
     * @return the Condition object
     */
    public Condition newCondition() {
        return sync.newCondition(); // 本类重写的Lock接口方法，其实是调用的内部类Sync里写的方法。
    }

    /**
     * Queries the number of holds on this lock by the current thread.
     * 查询当前线程持有该锁的持有数。
     *
     * <p>A thread has a hold on a lock for each lock action that is not
     * matched by an unlock action.
     * 线程为每一个与解锁操作不匹配的加锁操作持有一个锁（含义可能是，如果加锁了但没解锁，那么锁的数量会加1）
     *
     * <p>The hold count information is typically only used for testing and
     * debugging purposes. For example, if a certain section of code should
     * not be entered with the lock already held then we can assert that
     * fact:
     * 持有数信息通常只用于测试和debug的目的。
     * 例如，如果在已经持有锁的情况下，某段代码不应该被输入，那么我们可以断言（assert）这个事实：
     *
     *  <pre> {@code
     * class X {
     *   ReentrantLock lock = new ReentrantLock();
     *   // ...
     *   public void m() {
     *     assert lock.getHoldCount() == 0; // assert 断言，在AQS里也有，表示如果满足就继续执行，不满足就抛出异常
     *     lock.lock();
     *     try {
     *       // ... method body
     *     } finally {
     *       lock.unlock();
     *     }
     *   }
     * }}</pre>
     * 上面那段代码的意思就是，assert 如果当前线程没有持有锁，那么就加锁，否则就直接跳过加锁。
     *
     * @return the number of holds on this lock by the current thread,
     *         or zero if this lock is not held by the current thread
     *         如果当前线程没有持有该锁，返回0
     */
    public int getHoldCount() {
        return sync.getHoldCount(); // 调用Sync的，先判断当前线程是否持有锁，如果持有，返回AQS的state值
    }

    /**
     * Queries if this lock is held by the current thread.
     * 查询当前线程是否持有该锁。
     *
     * <p>Analogous to the {@link Thread#holdsLock(Object)} method for
     * built-in monitor locks, this method is typically used for
     * debugging and testing. For example, a method that should only be
     * called while a lock is held can assert that this is the case:
     * 类似于内置监视器锁的holdsLock(Object)方法，通常只用于测试和debug的目的。
     * 例如，方法应该只在当前线程持有锁的情况下执行，可以这样使用assert：
     *
     *  <pre> {@code
     * class X {
     *   ReentrantLock lock = new ReentrantLock();
     *   // ...
     *
     *   public void m() {
     *       assert lock.isHeldByCurrentThread();
     *       // ... method body
     *   }
     * }}</pre>
     *
     * <p>It can also be used to ensure that a reentrant lock is used
     * in a non-reentrant manner, for example:
     * 也可以用于确保可重入锁以不可重入的方式使用。
     * （把可重入锁当做不可重入锁使用）
     *
     *  <pre> {@code
     * class X {
     *   ReentrantLock lock = new ReentrantLock();
     *   // ...
     *
     *   public void m() {
     *       assert !lock.isHeldByCurrentThread(); // 当前线程是否持有锁，如果持有，抛出异常
     *       lock.lock();
     *       try {
     *           // ... method body
     *       } finally {
     *           lock.unlock();
     *       }
     *   }
     * }}</pre>
     *
     * @return {@code true} if current thread holds this lock and
     *         {@code false} otherwise
     */
    public boolean isHeldByCurrentThread() {
        return sync.isHeldExclusively(); // Sync#isHeldExclusively -> AbstractOwnerSynchronizer#getExclusiveOwnerThread 与当前线程比较
    }

    /**
     * Queries if this lock is held by any thread. This method is
     * designed for use in monitoring of the system state,
     * not for synchronization control.
     * 查询该锁是否被线程持有（不限于当前线程）。
     * 该方法为了监控系统状态设计的，不是为了同步控制。
     *
     * @return {@code true} if any thread holds this lock and
     *         {@code false} otherwise
     */
    public boolean isLocked() {
        return sync.isLocked(); // Sync#isLocked，通过判断AQS的state值是否等于=0
    }

    /**
     * Returns {@code true} if this lock has fairness set true.
     * 使用公平策略的话，返回true。
     *
     * @return {@code true} if this lock has fairness set true
     */
    public final boolean isFair() {
        return sync instanceof FairSync;
    }

    /**
     * Returns the thread that currently owns this lock, or
     * {@code null} if not owned. When this method is called by a
     * thread that is not the owner, the return value reflects a
     * best-effort approximation of current lock status. For example,
     * the owner may be momentarily {@code null} even if there are
     * threads trying to acquire the lock but have not yet done so.
     * This method is designed to facilitate construction of
     * subclasses that provide more extensive lock monitoring
     * facilities.
     * 返回当前拥有该锁的线程，如果没有拥有者，返回null。
     * 当该方法被非拥有者调用时，返回的值尽力反映当前锁的状态。（是个近似值 approximation）
     * 例如，持有者可能某个时刻是null：有线程正在获取锁，但还没有完成。
     * 该方法是为了促进（facilitate）子类构建而设计，以提高更广泛的监控设备
     *
     * @return the owner, or {@code null} if not owned
     */
    protected Thread getOwner() {
        return sync.getOwner();
    }

    /**
     * Queries whether any threads are waiting to acquire this lock. Note that
     * because cancellations may occur at any time, a {@code true}
     * return does not guarantee that any other thread will ever
     * acquire this lock.  This method is designed primarily for use in
     * monitoring of the system state.
     * 查询是否有线程在等待获取锁。
     * 注意，因为取消（取消可能是中断或者超时）可能在任意时刻发生，返回true不保证任何其他线程将获得锁。（返回true不保证后续一定有线程会成功加锁，因为即使有排队，但排队线程可能会取消）
     * 该方法主要是为了监视系统状态设计的。
     *
     * @return {@code true} if there may be other threads waiting to
     *         acquire the lock
     */
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads(); // AQS的hasQueuedThreads方法，判断head != tail
    }

    /**
     * Queries whether the given thread is waiting to acquire this
     * lock. Note that because cancellations may occur at any time, a
     * {@code true} return does not guarantee that this thread
     * will ever acquire this lock.  This method is designed primarily for use
     * in monitoring of the system state.
     * 查询给定的线程是否在该锁的等待队列上。
     * 注意，因为取消可能在任意时刻发生，返回true不保证该线程将获得锁。（跟上面方法的原因一样）
     * 该方法主要是为了监视系统状态设计的。
     *
     * @param thread the thread
     * @return {@code true} if the given thread is queued waiting for this lock
     * @throws NullPointerException if the thread is null
     */
    public final boolean hasQueuedThread(Thread thread) {
        return sync.isQueued(thread); // AQS的isQueued方法，从tail向前遍历，如果找到该线程，返回true，否则false。（为什么从tail开始遍历，可以看一下AQS的addWaiter或者enq过程）
    }

    /**
     * Returns an estimate of the number of threads waiting to
     * acquire this lock.  The value is only an estimate because the number of
     * threads may change dynamically while this method traverses
     * internal data structures.  This method is designed for use in
     * monitoring of the system state, not for synchronization
     * control.
     * 返回一个预估的等待获取该锁的线程数。
     * 值只是个预估值，因为在遍历内部数据结构时，线程可能会动态改变。
     * 该方法是为了监视系统状态设计的，不是为了同步。
     *
     * @return the estimated number of threads waiting for this lock
     */
    public final int getQueueLength() {
        return sync.getQueueLength(); // 调用AQS#getQueueLength方法，从tail遍历，累加所有的节点数
    }

    /**
     * Returns a collection containing threads that may be waiting to
     * acquire this lock.  Because the actual set of threads may change
     * dynamically while constructing this result, the returned
     * collection is only a best-effort estimate.  The elements of the
     * returned collection are in no particular order.  This method is
     * designed to facilitate construction of subclasses that provide
     * more extensive monitoring facilities.
     * 返回包含可能在获取该锁等待的线程集合。
     * 由于实际上的线程集合可能在构建结果的过程中动态改变，这个集合的结果只是个尽力预估值。
     * 返回的集合中的元素没有特定顺序。
     * 该方法是为了促进子类构建而设计，以提高更广泛的监控设备。
     *
     * @return the collection of threads
     */
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads(); // 调用AQS#getQueuedThreads
    }

    /**
     * Queries whether any threads are waiting on the given condition
     * associated with this lock. Note that because timeouts and
     * interrupts may occur at any time, a {@code true} return does
     * not guarantee that a future {@code signal} will awaken any
     * threads.  This method is designed primarily for use in
     * monitoring of the system state.
     * 查询是否有线程在给定的与该锁关联的condition上等待。
     * 注意，由于超时和中断可能在任意时刻发生，返回true不保证未来signal将唤醒任意线程。（有可能现在在condition queue上有线程在等待，但是过了一段时间这些线程取消了，再过一段时间调用signal也不会有任何线程响应了）
     * 该方法主要是为了用于监视系统状态设计的。
     *
     * @param condition the condition
     * @return {@code true} if there are any waiting threads
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this lock
     * @throws NullPointerException if the condition is null
     */
    public boolean hasWaiters(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject)) // 判断给定的condition是否为AQS内部实现的ConditionObject
            throw new IllegalArgumentException("not owner");
        return sync.hasWaiters((AbstractQueuedSynchronizer.ConditionObject)condition); // 调用AQS#hasWaiters(ConditionObject condition)->AQS#hasWaiters，要求必须持有该锁。从firstWaiter开始遍历，如果有ws=CONDITION的，返回true
    }

    /**
     * Returns an estimate of the number of threads waiting on the
     * given condition associated with this lock. Note that because
     * timeouts and interrupts may occur at any time, the estimate
     * serves only as an upper bound on the actual number of waiters.
     * This method is designed for use in monitoring of the system
     * state, not for synchronization control.
     * 返回预估的在给定与该锁关联的condition上等待的线程数。
     * 注意，由于超时和中断可能在任意时刻发生，预估的waiter数量>=实际的waiter数量。
     * 该方法主要是为了用于监视系统状态设计的。
     *
     * @param condition the condition
     * @return the estimated number of waiting threads
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this lock
     * @throws NullPointerException if the condition is null
     */
    public int getWaitQueueLength(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitQueueLength((AbstractQueuedSynchronizer.ConditionObject)condition); // 调用AQS#getWaitQueueLength，要求必须持有该锁。
    }

    /**
     * Returns a collection containing those threads that may be
     * waiting on the given condition associated with this lock.
     * Because the actual set of threads may change dynamically while
     * constructing this result, the returned collection is only a
     * best-effort estimate. The elements of the returned collection
     * are in no particular order.  This method is designed to
     * facilitate construction of subclasses that provide more
     * extensive condition monitoring facilities.
     * 返回在给定的与该锁关联的condition上等待的线程集合。
     * 由于在构造结果的过程中实际的线程集会动态变化，所以返回的集合只是一个尽力预估值。
     * 返回的集合中元素没有特定的顺序。
     * 该方法是为了促进子类构建而设计，以提高更广泛的监控设备。
     *
     * @param condition the condition
     * @return the collection of threads
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this lock
     * @throws NullPointerException if the condition is null
     */
    protected Collection<Thread> getWaitingThreads(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitingThreads((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * Returns a string identifying this lock, as well as its lock state.
     * The state, in brackets, includes either the String {@code "Unlocked"}
     * or the String {@code "Locked by"} followed by the
     * {@linkplain Thread#getName name} of the owning thread.
     *
     * @return a string identifying this lock, as well as its lock state
     */
    public String toString() {
        Thread o = sync.getOwner();
        return super.toString() + ((o == null) ?
                                   "[Unlocked]" :
                                   "[Locked by thread " + o.getName() + "]");
    }
}
```