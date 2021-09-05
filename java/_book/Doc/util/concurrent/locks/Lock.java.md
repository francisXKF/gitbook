# Lock
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

/**
 * {@code Lock} implementations provide more extensive locking
 * operations than can be obtained using {@code synchronized} methods
 * and statements.  They allow more flexible structuring, may have
 * quite different properties, and may support multiple associated
 * {@link Condition} objects.
 * 相比使用synchronized的方法和语句获得的锁操作，Lock的实现提供了更广泛的锁操作。
 * 它允许更灵活的结构，可能具有完全不同的属性，并且可能支持多个关联Condition对象
 * 简单说一下，synchronized methods 指的是 public synchronized getV()这个样子的
 * synchronized statements指的是 public getV() { synchronized(this) { ... }} 这个样子的
 *
 * <p>A lock is a tool for controlling access to a shared resource by
 * multiple threads. Commonly, a lock provides exclusive access to a
 * shared resource: only one thread at a time can acquire the lock and
 * all access to the shared resource requires that the lock be
 * acquired first. However, some locks may allow concurrent access to
 * a shared resource, such as the read lock of a {@link ReadWriteLock}.
 * Lock是一个工具，用来在多线程场景下对共享资源访问（访问包含读写）的限制。
 * 通常来说，lock提供对共享资源的独占访问：在同一时间只能有一个线程可以获得锁，并且所有对共享资源的访问都必须先获取到锁
 * 然而，一些lock（的实现）可能支持并发访问共享资源，例如读锁（ReadWriteLock）
 *
 * 下面开始说明synchronized的特性与不足：
 * <p>The use of {@code synchronized} methods or statements provides
 * access to the implicit monitor lock associated with every object, but
 * forces all lock acquisition and release to occur in a block-structured way:
 * when multiple locks are acquired they must be released in the opposite
 * order, and all locks must be released in the same lexical scope in which
 * they were acquired.
 * 使用synchronized方法或者语句提供对关联对象的隐式监视器锁访问，但是强制所有锁的获取与释放都按块结构（block-structured）方式进行：
 * 当获取多个锁时，必须按相反的顺序进行释放，并且必须在与获取锁同一个词法范围内释放锁。
 *
 * <p>While the scoping mechanism for {@code synchronized} methods
 * and statements makes it much easier to program with monitor locks,
 * and helps avoid many common programming errors involving locks,
 * there are occasions where you need to work with locks in a more
 * flexible way. For example, some algorithms for traversing
 * concurrently accessed data structures require the use of
 * &quot;hand-over-hand&quot; or &quot;chain locking&quot;: you
 * acquire the lock of node A, then node B, then release A and acquire
 * C, then release B and acquire D and so on.  Implementations of the
 * {@code Lock} interface enable the use of such techniques by
 * allowing a lock to be acquired and released in different scopes,
 * and allowing multiple locks to be acquired and released in any
 * order.
 * 尽管synchronized方法与语句的作用域机制使得使用监视器锁变成变得容易，
 * 并且可以帮助避免许多涉及到锁的常见编程错误，但在某些情况（occasion）下，需要更灵活的锁方式。
 * 例如一些遍历（traverse）并发访问数据结构的算法需要使用交换锁（hand-over-hand）或者链锁（chain locking），向下面这样：
 * 按这样的顺序操作锁：加锁节点A-->加锁节点B-->解锁节点A-->加锁节点C-->解锁节点B-->加锁节点D这样
 * Lock接口的实现，确保可以使用如下技术：允许在不同范围内获取和释放锁，并允许以任意顺序获取和释放多个锁
 *
 * <p>With this increased flexibility comes additional
 * responsibility. The absence of block-structured locking removes the
 * automatic release of locks that occurs with {@code synchronized}
 * methods and statements. In most cases, the following idiom
 * should be used:
 * 随着灵活性的提高，责任（responsibility）也随之（comes带来）增加。
 * 块结构锁的缺失消除了synchronized方法与语句中发生的锁自动释放（Lock接口的实现不会自动释放锁）
 * 在大多数情况下，应使用以下习惯用法
 *
 *  <pre> {@code
 * Lock l = ...;
 * l.lock();
 * try {
 *   // access the resource protected by this lock
 * } finally {
 *   l.unlock(); // 手工释放
 * }}</pre>
 *
 * When locking and unlocking occur in different scopes, care must be
 * taken to ensure that all code that is executed while the lock is
 * held is protected by try-finally or try-catch to ensure that the
 * lock is released when necessary.
 * 当在不同的作用域获得锁与释放锁时，必须注意确保所有在持有锁期间执行的方法必须被try-finally或者try-catch保护（包裹），确保在必要时刻可以释放锁
 *
 * <p>{@code Lock} implementations provide additional functionality
 * over the use of {@code synchronized} methods and statements by
 * providing a non-blocking attempt to acquire a lock ({@link
 * #tryLock()}), an attempt to acquire the lock that can be
 * interrupted ({@link #lockInterruptibly}, and an attempt to acquire
 * the lock that can timeout ({@link #tryLock(long, TimeUnit)}).
 * Lock的实现，相较于synchronized方法与语句提供了额外的方法，例如：
 * tryLock()，非阻塞前提下尝试获取锁
 * lockInterruptibly，尝试获取可以中断的锁
 * tryLock(long, TimeUnit)，有最大等待时间的尝试获取锁
 *
 * <p>A {@code Lock} class can also provide behavior and semantics
 * that is quite different from that of the implicit monitor lock,
 * such as guaranteed ordering, non-reentrant usage, or deadlock
 * detection. If an implementation provides such specialized semantics
 * then the implementation must document those semantics.
 * Lock类还可以提供与隐式监视器锁完全不同的行为与语义，例如保证排序，不可重入使用，或者死锁检测。
 * 如果实现类提供这些专门的语义，那么实现类必须在文档中记录这些语义。（看起来是对代码说明的限制）
 *
 * <p>Note that {@code Lock} instances are just normal objects and can
 * themselves be used as the target in a {@code synchronized} statement.
 * Acquiring the
 * monitor lock of a {@code Lock} instance has no specified relationship
 * with invoking any of the {@link #lock} methods of that instance.
 * It is recommended that to avoid confusion you never use {@code Lock}
 * instances in this way, except within their own implementation.
 * 请注意，Lock的实例只是个普通的对象，并且他们自身也可以用来做synchronized statement的目标对象（synchronized(lock1){...}这个样子）。
 * 获取Lock实例的监视器锁与调用lock实例的任何锁方法没有特定的关系。
 * 为避免混淆，建议不要以这种方式使用Lock实例，除非在他们自己的实现里。
 *
 * <p>Except where noted, passing a {@code null} value for any
 * parameter will result in a {@link NullPointerException} being
 * thrown.
 * 除非另有说明，传递null值给任何参数都会导致抛出NullPointException异常
 *
 * <h3>Memory Synchronization</h3>
 * 内存同步
 *
 * <p>All {@code Lock} implementations <em>must</em> enforce the same
 * memory synchronization semantics as provided by the built-in monitor
 * lock, as described in
 * <a href="https://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html#jls-17.4">
 * The Java Language Specification (17.4 Memory Model)</a>:
 * 强制所有Lock的实现类都必须与内置监视器锁提供的具有相同的内存锁语义，如这个链接里的所述
 * 
 * <ul>
 * <li>A successful {@code lock} operation has the same memory
 * synchronization effects as a successful <em>Lock</em> action.
 * <li>A successful {@code unlock} operation has the same
 * memory synchronization effects as a successful <em>Unlock</em> action.
 * </ul>
 * （不知道这句话实际含义）
 * 成功的Lock动作与成功的lock操作具有相同的内存同步效果
 * 成功的Unlock动作与成功的unlock操作具有相同的内存同步效果
 *
 * Unsuccessful locking and unlocking operations, and reentrant
 * locking/unlocking operations, do not require any memory
 * synchronization effects.
 * 不成功的加锁/解锁操作，和可重入的加锁/解锁操作，不要求任何内存同步效果
 *
 * <h3>Implementation Considerations</h3>
 * 实现的注意事项
 *
 * <p>The three forms of lock acquisition (interruptible,
 * non-interruptible, and timed) may differ in their performance
 * characteristics, ordering guarantees, or other implementation
 * qualities.  Further, the ability to interrupt the <em>ongoing</em>
 * acquisition of a lock may not be available in a given {@code Lock}
 * class.  Consequently, an implementation is not required to define
 * exactly the same guarantees or semantics for all three forms of
 * lock acquisition, nor is it required to support interruption of an
 * ongoing lock acquisition.  An implementation is required to clearly
 * document the semantics and guarantees provided by each of the
 * locking methods. It must also obey the interruption semantics as
 * defined in this interface, to the extent that interruption of lock
 * acquisition is supported: which is either totally, or only on
 * method entry.
 * 锁获取（acquisition）的三种形式（可中断，不可中断，和有时限的）可能有着不同的行为特征，顺序保证，与其他的实现质量。
 * 此外，中断正在获取锁的操作的能力，在给定的Lock实现中可能不支持。
 * 因此，并不要求实现类为三种形式的锁获取（lock acquisition）定义完全相同的保证或者语义，也不要求支持中断正在获取锁的操作。
 * 要求实现类清楚的记录提供的每个locking方法对应的语义和保证。
 * 同时，实现类也需要遵循在Lock这个接口类中定义的中断语义，以支持锁获取的中断：要么完全支持，要么只在方法入口
 *
 * <p>As interruption generally implies cancellation, and checks for
 * interruption are often infrequent, an implementation can favor responding
 * to an interrupt over normal method return. This is true even if it can be
 * shown that the interrupt occurred after another action may have unblocked
 * the thread. An implementation should document this behavior.
 * 中断通常意味着（implies）取消，并且中断检查通常很少发生，因此实现类可以偏向于响应中断而不是正常的方法返回。
 * 即使可以证明 中断发生在 另一个动作已经解除了本线程阻塞之后，也是如此。实现类中应该记录下该行为
 *
 * @see ReentrantLock
 * @see Condition
 * @see ReadWriteLock
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface Lock {

    /**
     * Acquires the lock.
     *
     * <p>If the lock is not available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until the
     * lock has been acquired.
     * 如果不能获取到锁，那么当前线程将被禁止用于线程调度目的并处于（lie）休眠（dormant蛰伏）状态，直到能够获取到锁
     *
     * <p><b>Implementation Considerations</b>
     * 实现注意事项
     *
     * <p>A {@code Lock} implementation may be able to detect erroneous use
     * of the lock, such as an invocation that would cause deadlock, and
     * may throw an (unchecked) exception in such circumstances.  The
     * circumstances and the exception type must be documented by that
     * {@code Lock} implementation.
     * Lock的实现可能需要能够检测到锁的错误使用，比如导致死锁的调用，和在这些情况（circumstances）下可能抛出（未检查）异常。
     * 在Lock的实现中必须记录下这些情况和异常类型。
     */
    void lock();

    /**
     * Acquires the lock unless the current thread is
     * {@linkplain Thread#interrupt interrupted}.
     * 除非当前线程被中断（interrupted），否则获取锁
     *
     * <p>Acquires the lock if it is available and returns immediately.
     * 如果可用，则获取锁并立即返回。
     *
     * <p>If the lock is not available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * one of two things happens:
     * 如果锁不可用，那么当前线程将被禁止用户线程调度目的并处于休眠状态，直到以下两种情况之一发生：（这两种情况就是1.要么获取到了锁，2.要么在获取锁的时候被中断了，而正好支持中断这个获取锁操作）
     *
     * <ul>
     * <li>The lock is acquired by the current thread; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread, and interruption of lock acquisition is supported.
     * </ul>
     * 1.锁被当前线程获取；
     * 2.或者其他的一些线程中断了当前线程，并且支持在获取锁的过程中被中断。
     *
     * 下面解释如果获取锁过程中被中断会发生什么
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while acquiring the
     * lock, and interruption of lock acquisition is supported,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     * 如果当前线程：
     * 1. 在进入这个方法时设置了中断状态
     * 2. 或者在获取锁时被中断，并且支持获取锁的过程被中断。
     * 那么将抛出InterruptedException，并且清除当前线程中断状态
     *
     * <p><b>Implementation Considerations</b>
     * 实现该方法的注意事项
     *
     * <p>The ability to interrupt a lock acquisition in some
     * implementations may not be possible, and if possible may be an
     * expensive operation.  The programmer should be aware that this
     * may be the case. An implementation should document when this is
     * the case.
     * 中断锁的获取的能力在一些实现中是不可能的，如果可能，也会是个昂贵的操作（比较费时等）。
     * 程序员应当意识到这是可能的情况。
     * 实现应当记录下这些情况。
     *
     * <p>An implementation can favor responding to an interrupt over
     * normal method return.
     * 实现可以倾向于响应中断而不是正常的方法返回。
     *
     * <p>A {@code Lock} implementation may be able to detect
     * erroneous use of the lock, such as an invocation that would
     * cause deadlock, and may throw an (unchecked) exception in such
     * circumstances.  The circumstances and the exception type must
     * be documented by that {@code Lock} implementation.
     * （跟上面一样，记得检测可能会发生的死锁等错误使用锁的情况，并进行记录）
     *
     * @throws InterruptedException if the current thread is
     *         interrupted while acquiring the lock (and interruption
     *         of lock acquisition is supported)
     * 抛出InterruptedException，如果当前线程在获取锁的时候被中断
     */
    void lockInterruptibly() throws InterruptedException;

    /**
     * Acquires the lock only if it is free at the time of invocation.
     * 只有在调用时，当前锁空闲才会获取到锁（跟上面两种方式不一样的是，如果在调用时锁被占用了，那么当前线程不会进入休眠，而是立即返回）
     *
     * <p>Acquires the lock if it is available and returns immediately
     * with the value {@code true}.
     * If the lock is not available then this method will return
     * immediately with the value {@code false}.
     * 如果可用那么会获取锁（加锁）并立即返回true
     * 如果锁不可用，该方法会立即返回false（没有休眠）
     *
     * <p>A typical usage idiom for this method would be: （typical usage idiom典型使用习惯）
     *  <pre> {@code
     * Lock lock = ...;
     * if (lock.tryLock()) {
     *   try {
     *     // manipulate protected state
     *   } finally {
     *     lock.unlock(); // tryLock()成功也会加锁，记得显式释放锁
     *   }
     * } else {
     *   // perform alternative actions // 执行替代操作
     * }}</pre>
     *
     * This usage ensures that the lock is unlocked if it was acquired, and
     * doesn't try to unlock if the lock was not acquired.
     * 这种用法确保在获取到锁后正确释放锁，并且不会在获取锁失败后尝试释放锁。
     *
     * @return {@code true} if the lock was acquired and
     *         {@code false} otherwise
     * 这个不会抛出异常
     */
    boolean tryLock();

    /**
     * Acquires the lock if it is free within the given waiting time and the
     * current thread has not been {@linkplain Thread#interrupt interrupted}.
     * 在给定的等待时间里，如果锁空闲并且当前线程没有被中断，则会获取锁（加锁）。
     *
     * <p>If the lock is available this method returns immediately
     * with the value {@code true}.
     * If the lock is not available then
     * the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until one of three things happens:
     * <ul>
     * <li>The lock is acquired by the current thread; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread, and interruption of lock acquisition is supported; or
     * <li>The specified waiting time elapses
     * </ul>
     * 如果能够获取锁，该方法立即返回true
     * 如果锁不可用，那么当前线程将禁止作为线程调用目标并进入休眠，直到以下三种情况之一发生：
     * 1. 当前线程获取到该锁
     * 2. 其他一些线程中断当前线程，并且支持中断获取锁操作
     * 3. 到达了等待时间（等待超时）
     *
     * 下面会针对上面说的三种情况进行说明
     * <p>If the lock is acquired then the value {@code true} is returned.
     * 如果成功获取到锁，会返回true
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while acquiring
     * the lock, and interruption of lock acquisition is supported,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     * 如果当前线程：
     * 1. 在进入这个方法时设置了中断状态
     * 2. 或者在获取锁时被中断，并且支持中断获取锁的操作
     * 那么就会抛出InterruptedException异常，并且当前线程清理中断状态
     *
     * <p>If the specified waiting time elapses then the value {@code false}
     * is returned.
     * If the time is
     * less than or equal to zero, the method will not wait at all.
     * 如果等待时间过去了，那么会返回false
     * 注意：如果等待时间参数<=0，那么该方法根本不会等待（可以传入<=0的等待时间，但是不会等待）
     *
     * <p><b>Implementation Considerations</b>
     * 注意事项
     * <p>The ability to interrupt a lock acquisition in some implementations
     * may not be possible, and if possible may
     * be an expensive operation.
     * The programmer should be aware that this may be the case. An
     * implementation should document when this is the case.
     * 与lockInterruptibly()的注意事项一样：
     * 中断锁的获取的能力在一些实现中是不可能的，如果可能，也会是个昂贵的操作（比较费时等）。
     * 程序员应当意识到这是可能的情况。
     * 实现应当记录下这些情况。
     * 
     * <p>An implementation can favor responding to an interrupt over normal
     * method return, or reporting a timeout.
     * 更倾向于抛出异常而不是正常的方法返回，或者报告超时
     * 
     * <p>A {@code Lock} implementation may be able to detect
     * erroneous use of the lock, such as an invocation that would cause
     * deadlock, and may throw an (unchecked) exception in such circumstances.
     * The circumstances and the exception type must be documented by that
     * {@code Lock} implementation.
     * 同样需要检测死锁等错误使用的情况，并且记录下来
     *
     * @param time the maximum time to wait for the lock // 最大等待时间
     * @param unit the time unit of the {@code time} argument // 等待时间的时间单位
     * @return {@code true} if the lock was acquired and {@code false}
     *         if the waiting time elapsed before the lock was acquired
     *
     * @throws InterruptedException if the current thread is interrupted
     *         while acquiring the lock (and interruption of lock
     *         acquisition is supported)
     * 这个方法如果响应中断也会抛出异常
     */
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;

    /**
     * Releases the lock.
     * 释放锁（解锁）
     *
     * <p><b>Implementation Considerations</b>
     * 注意事项
     * <p>A {@code Lock} implementation will usually impose
     * restrictions on which thread can release a lock (typically only the
     * holder of the lock can release it) and may throw
     * an (unchecked) exception if the restriction is violated.
     * Any restrictions and the exception
     * type must be documented by that {@code Lock} implementation.
     * 通常会对可以释放锁的线程加以限制（restrictions）（通常只有锁的持有者（持有该锁的线程）才可以释放它），如果违反限制可能会抛出（未检查）的异常
     * 必须记录下任何限制与异常情况
     */
    void unlock();

    /**
     * Returns a new {@link Condition} instance that is bound to this
     * {@code Lock} instance.
     * 返回绑定到此Lock实例的新Condition实例
     * 这个在AQS中有使用到，到那里在进行详细观察
     *
     * <p>Before waiting on the condition the lock must be held by the
     * current thread.
     * A call to {@link Condition#await()} will atomically release the lock
     * before waiting and re-acquire the lock before the wait returns.
     * 在等待条件之前，必须由当前线程来持有锁。
     * 调用wait()将在等待之前自动释放锁，并且在等待返回之前重新获取锁
     * （这个很关键，当前线程可以调用wait()来主动释放锁并休眠，直到等待结束（比如等待的条件满足了）当前线程会重新获取到锁来继续执行。
     *
     * <p><b>Implementation Considerations</b>
     * 注意事项
     * <p>The exact operation of the {@link Condition} instance depends on
     * the {@code Lock} implementation and must be documented by that
     * implementation.
     * Condition实例的具体操作取决于Lock实现，并且必须由实现记录下来。
     *
     * @return A new {@link Condition} instance for this {@code Lock} instance
     * @throws UnsupportedOperationException if this {@code Lock}
     *         implementation does not support conditions
     * 如果Lock实现不支持conditions，会返回UnsupporttedOperationException异常
     */
    Condition newCondition();
}
```