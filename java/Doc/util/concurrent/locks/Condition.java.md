# Condition
``` java
/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent.locks;
import java.util.concurrent.TimeUnit;
import java.util.Date;

/**
 * {@code Condition} factors out the {@code Object} monitor
 * methods ({@link Object#wait() wait}, {@link Object#notify notify}
 * and {@link Object#notifyAll notifyAll}) into distinct objects to
 * give the effect of having multiple wait-sets per object, by
 * combining them with the use of arbitrary {@link Lock} implementations.
 * Where a {@code Lock} replaces the use of {@code synchronized} methods
 * and statements, a {@code Condition} replaces the use of the Object
 * monitor methods.
 * Condition将Object监视器方法（wait、notify、notifyAll）分解到不同的对象，通过将他们与使用任意的Lock实现进行组合，以产生每个对象具有多个等待集的效果。
 *在使用Lock替换synchronized使用的地方，用Condition替换Object监视器使用（Lock与Condition配对，synchronized与ObjectMonitor配对（就是Object.wait/notify那一套））
 *
 * <p>Conditions (also known as <em>condition queues</em> or
 * <em>condition variables</em>) provide a means for one thread to
 * suspend execution (to &quot;wait&quot;) until notified by another
 * thread that some state condition may now be true.  Because access
 * to this shared state information occurs in different threads, it
 * must be protected, so a lock of some form is associated with the
 * condition. The key property that waiting for a condition provides
 * is that it <em>atomically</em> releases the associated lock and
 * suspends the current thread, just like {@code Object.wait}.
 * Conditions（也叫条件队列或者条件变量）提供了一种一个线程暂停执行（等待）直到一些状态条件可能为true时被另一个线程唤醒的能力。
 * 因为访问共享变量信息发生在不同的线程，它必须受保护，所以需要某种形式的锁与条件相关联。
 * 等待条件提供的关键属性是原子地释放关联的锁并且暂停当前线程，就像Object.wait一样。
 *
 * <p>A {@code Condition} instance is intrinsically bound to a lock.
 * To obtain a {@code Condition} instance for a particular {@link Lock}
 * instance use its {@link Lock#newCondition newCondition()} method.
 * Condition实例本质上（intrinsically）绑定到了一个锁。
 * 获取（obtain）特定Lock实例的Condition实例使用Lock的newCondition()方法。
 *
 * <p>As an example, suppose we have a bounded buffer which supports
 * {@code put} and {@code take} methods.  If a
 * {@code take} is attempted on an empty buffer, then the thread will block
 * until an item becomes available; if a {@code put} is attempted on a
 * full buffer, then the thread will block until a space becomes available.
 * We would like to keep waiting {@code put} threads and {@code take}
 * threads in separate wait-sets so that we can use the optimization of
 * only notifying a single thread at a time when items or spaces become
 * available in the buffer. This can be achieved using two
 * {@link Condition} instances.
 * 举例来说，假设我们有一个有界的buffer，支持put和take方法。
 * 如果有线程对空buffer尝试take，当前线程会阻塞直到有东西变得可用（就是buffer里有东西）
 * 如果有线程对满buffer尝试put，当前线程会阻塞直到有空地方可用
 * 我们希望将等待的多个put线程与多个take线程保持在独立的等待集，这样我们就可以优化（optimization）成当buffer里有东西或者有空间可用时，每次只唤醒一个线程。
 * 可以通过使用两个Condition实例来实现
 * 
 * <pre>
 * class BoundedBuffer {
 *   <b>final Lock lock = new ReentrantLock();</b> // 等下再返回看ReentrantLock实现
 *   final Condition notFull  = <b>lock.newCondition(); </b> // 同一个lock创建两个Condition实例
 *   final Condition notEmpty = <b>lock.newCondition(); </b>
 *
 *   final Object[] items = new Object[100];
 *   int putptr, takeptr, count;
 *
 *   public void put(Object x) throws InterruptedException {
 *     <b>lock.lock();
 *     try {</b>
 *       // 注意这里的while循环，特别有用，因为线程被唤醒后，可能仍然没有满足的条件（比如条件被别的线程抢了），这时候就得重新进入挂起-唤醒的循环
 *       //（这其实就是虚假唤醒的语义，下面会解释）
 *       while (count == items.length)
 *         <b>notFull.await();</b> // 挂起，并释放锁
 *       items[putptr] = x;
 *       if (++putptr == items.length) putptr = 0;
 *       ++count;
 *       <b>notEmpty.signal();</b> // buffer里有东西了，唤醒一个挂起的take
 *     <b>} finally {
 *       lock.unlock(); // 记得释放锁
 *     }</b>
 *   }
 *
 *   public Object take() throws InterruptedException {
 *     <b>lock.lock();
 *     try {</b>
 *       while (count == 0)
 *         <b>notEmpty.await();</b>
 *       Object x = items[takeptr];
 *       if (++takeptr == items.length) takeptr = 0;
 *       --count;
 *       <b>notFull.signal();</b>
 *       return x;
 *     <b>} finally {
 *       lock.unlock();
 *     }</b>
 *   }
 * }
 * </pre>
 *
 * (The {@link java.util.concurrent.ArrayBlockingQueue} class provides
 * this functionality, so there is no reason to implement this
 * sample usage class.)
 * ArrayBlockingQueue类提供了这些方法，所有没有必要去实现这个相同用处的类
 *
 * <p>A {@code Condition} implementation can provide behavior and semantics
 * that is
 * different from that of the {@code Object} monitor methods, such as
 * guaranteed ordering for notifications, or not requiring a lock to be held
 * when performing notifications.
 * If an implementation provides such specialized semantics then the
 * implementation must document those semantics.
 * Condition的实现可以提供与Object监视器不同的行为与语义，例如保证通知顺序，或者执行通知时不要求持有锁。
 * 如果实现提供这些专门的语义，那么实现必须记录下来。
 *
 * <p>Note that {@code Condition} instances are just normal objects and can
 * themselves be used as the target in a {@code synchronized} statement,
 * and can have their own monitor {@link Object#wait wait} and
 * {@link Object#notify notification} methods invoked.
 * Acquiring the monitor lock of a {@code Condition} instance, or using its
 * monitor methods, has no specified relationship with acquiring the
 * {@link Lock} associated with that {@code Condition} or the use of its
 * {@linkplain #await waiting} and {@linkplain #signal signalling} methods.
 * It is recommended that to avoid confusion you never use {@code Condition}
 * instances in this way, except perhaps within their own implementation.
 * 注意，Condition的实例就是普通的object，并且他自身可以用于synchronized statement，并且也拥有自己的监视器wait和notify方法调用。
 * 获取Condition实例的监视器锁，或者使用它的监视器方法，跟获取该实例关联的Lock，或者使用await、signal方法没有任何特定的关系。
 * 为了避免混淆，建议不要以这种方式使用Condition的实例，除非在他们自己的实现中
 * （跟Lock的建议一样）
 *
 * <p>Except where noted, passing a {@code null} value for any parameter
 * will result in a {@link NullPointerException} being thrown.
 * 除非另有说明，传参时传个null会抛出NullPointerException异常。
 *
 * <h3>Implementation Considerations</h3>
 * 注意事项
 * <p>When waiting upon a {@code Condition}, a &quot;<em>spurious
 * wakeup</em>&quot; is permitted to occur, in
 * general, as a concession to the underlying platform semantics.
 * This has little practical impact on most application programs as a
 * {@code Condition} should always be waited upon in a loop, testing
 * the state predicate that is being waited for.  An implementation is
 * free to remove the possibility of spurious wakeups but it is
 * recommended that applications programmers always assume that they can
 * occur and so always wait in a loop.
 * 当等待Condition时（就是调用await后等待唤醒时），允许虚假唤醒（spurious wakeup）发生，通常，作为对底层平台语义的让步（concession）。
 * 这对大多数应用几乎没有实际影响，因为应用始终在循环中等待Condition，在循环中检测等待的状态谓词。
 * 实现可以自由的消除虚假唤醒的可能性，不过建议应用程序员始终假设他们可能发生，并且始终在循环中等待（与检测）。
 *
 * <p>The three forms of condition waiting
 * (interruptible, non-interruptible, and timed) may differ in their ease of
 * implementation on some platforms and in their performance characteristics.
 * In particular, it may be difficult to provide these features and maintain
 * specific semantics such as ordering guarantees.
 * Further, the ability to interrupt the actual suspension of the thread may
 * not always be feasible to implement on all platforms.
 * 条件等待的三种形式（可中断、不可中断、定时）在某些平台上实现的难易程度和性能特征方面可能有所不同。
 * 特别是，可能很难提供这些功能并维护特定的语义，例如排序保证。
 * 此外，在所有平台上实现中断线程实际挂起的能力并不总是可行。
 *
 * <p>Consequently, an implementation is not required to define exactly the
 * same guarantees or semantics for all three forms of waiting, nor is it
 * required to support interruption of the actual suspension of the thread.
 * 因此，不需要一个实现针对这三种形式定义完全（exactly 确切）相同的保证或者语义，也不需要支持中断线程实际挂起的。
 *
 * <p>An implementation is required to
 * clearly document the semantics and guarantees provided by each of the
 * waiting methods, and when an implementation does support interruption of
 * thread suspension then it must obey the interruption semantics as defined
 * in this interface.
 * 要求实现明确记录下每个等待方法提供的语义和保证，并且当实现支持中断线程挂起，那么它必须遵循这个接口中定义的中断语义
 *
 * <p>As interruption generally implies cancellation, and checks for
 * interruption are often infrequent, an implementation can favor responding
 * to an interrupt over normal method return. This is true even if it can be
 * shown that the interrupt occurred after another action that may have
 * unblocked the thread. An implementation should document this behavior.
 * 中断通常意味着（implies）取消，并且中断检查通常很少发生，因此实现类可以偏向于响应中断而不是正常的方法返回。
 * 即使可以证明 中断发生在 另一个动作已经解除了本线程阻塞之后，也是如此。实现类中应该记录下该行为
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface Condition {

    /**
     * Causes the current thread to wait until it is signalled or
     * {@linkplain Thread#interrupt interrupted}.
     * 使当前线程等待，直到收到信号或者被中断
     *
     * <p>The lock associated with this {@code Condition} is atomically
     * released and the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until <em>one</em> of four things happens:
     * 原子操作释放Condition关联的锁，当前线程转变为不可用的线程调度目标，并且挂起，直到以下四种情况之一发生：
     *
     * <ul>
     * <li>Some other thread invokes the {@link #signal} method for this
     * {@code Condition} and the current thread happens to be chosen as the
     * thread to be awakened; or
     * <li>Some other thread invokes the {@link #signalAll} method for this
     * {@code Condition}; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread, and interruption of thread suspension is supported; or
     * <li>A &quot;<em>spurious wakeup</em>&quot; occurs.
     * </ul>
     * 1.其他一些线程调用了这个Condition的signal方法，并且当前线程被选中为唤醒线程
     * 2.其他一些线程调用了这个Condition的signalAll方法
     * 3.其他一些线程中断当前线程，并且当前线程支持中断线程挂起
     * 4.发生虚假唤醒
     *
     * <p>In all cases, before this method can return the current thread must
     * re-acquire the lock associated with this condition. When the
     * thread returns it is <em>guaranteed</em> to hold this lock.
     * 在所有情况下，在此方法可以返回到当前线程（继续执行）之前，当前线程必须重新获取到这个condition关联的锁。
     * 当线程返回时，它保证持有这个锁
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * and interruption of thread suspension is supported,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared. It is not specified, in the first
     * case, whether or not the test for interruption occurs before the lock
     * is released.
     * 如果当前线程：
     * 1. 在进入该方法时设置了中断状态
     * 2. 或者在等待时被中断，并且当前线程支持中断挂起
     * 那么会抛出InterruptedException异常并且清理当前线程的中断状态。
     * 对于第一种情况，没有规定要检测在中断发生前锁是否被释放。
     *
     * <p><b>Implementation Considerations</b>
     * 注意事项
     * <p>The current thread is assumed to hold the lock associated with this
     * {@code Condition} when this method is called.
     * It is up to the implementation to determine if this is
     * the case and if not, how to respond. Typically, an exception will be
     * thrown (such as {@link IllegalMonitorStateException}) and the
     * implementation must document that fact.
     * 在调用此方法时，假定当前线程拥有该Condition关联的锁。
     * 由实现去决定如果不满足上述假设时该如何响应。
     * 通常会抛出IllegalMonitorStateException异常，并且实现必须记录下该事实（写明处理规则）
     *
     * <p>An implementation can favor responding to an interrupt over normal
     * method return in response to a signal. In that case the implementation
     * must ensure that the signal is redirected to another waiting thread, if
     * there is one.
     * 实现响应信号时可以倾向于响应中断而不是正常方法返回。
     * 实现必须确保信号量被重定向到另一个等待线程，如果还有等待线程的话。（就是如果当前线程唤醒失败，需要把唤醒信号量传递给其他等待线程）
     *
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     */
    void await() throws InterruptedException;

    /**
     * Causes the current thread to wait until it is signalled.
     * (这些都与await()方法一样）
     * <p>The lock associated with this condition is atomically
     * released and the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until <em>one</em> of three things happens:
     * <ul>
     * <li>Some other thread invokes the {@link #signal} method for this
     * {@code Condition} and the current thread happens to be chosen as the
     * thread to be awakened; or
     * <li>Some other thread invokes the {@link #signalAll} method for this
     * {@code Condition}; or
     * <li>A &quot;<em>spurious wakeup</em>&quot; occurs.
     * </ul>
     * 这里少了一种打断wait的方法，那就是少了中断
     *
     * <p>In all cases, before this method can return the current thread must
     * re-acquire the lock associated with this condition. When the
     * thread returns it is <em>guaranteed</em> to hold this lock.
     *
     * <p>If the current thread's interrupted status is set when it enters
     * this method, or it is {@linkplain Thread#interrupt interrupted}
     * while waiting, it will continue to wait until signalled. When it finally
     * returns from this method its interrupted status will still
     * be set.
     * 注意这里是不一样的：
     * 如果当前线程在进入方法之前被设置了中断，或者在等待时被中断，它将仍然处于等待状态直到收到唤醒信号。
     * 当它最终返回的时候，它的中断状态仍旧被设置。（不是清除中断状态）
     * 所以，它不会抛出异常
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>The current thread is assumed to hold the lock associated with this
     * {@code Condition} when this method is called.
     * It is up to the implementation to determine if this is
     * the case and if not, how to respond. Typically, an exception will be
     * thrown (such as {@link IllegalMonitorStateException}) and the
     * implementation must document that fact.
     */
    void awaitUninterruptibly();

    /**
     * Causes the current thread to wait until it is signalled or interrupted,
     * or the specified waiting time elapses.
     * 当前线程等待直到收到唤醒信号或者被中断，或者是指定的等待时间超时。
     *
     * <p>The lock associated with this condition is atomically
     * released and the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until <em>one</em> of five things happens:
     * <ul>
     * <li>Some other thread invokes the {@link #signal} method for this
     * {@code Condition} and the current thread happens to be chosen as the
     * thread to be awakened; or
     * <li>Some other thread invokes the {@link #signalAll} method for this
     * {@code Condition}; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread, and interruption of thread suspension is supported; or
     * <li>The specified waiting time elapses; or
     * <li>A &quot;<em>spurious wakeup</em>&quot; occurs.
     * </ul>
     * 这里有五种方法可以打断线程的挂起： 
     * 1. signal并且当前线程被选中
     * 2. signalAll
     * 3. interrupted
     * 4. 等待超时（相较await()，多了个这个）
     * 5. 虚假唤醒
     * 
     * <p>In all cases, before this method can return the current thread must
     * re-acquire the lock associated with this condition. When the
     * thread returns it is <em>guaranteed</em> to hold this lock.
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * and interruption of thread suspension is supported,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared. It is not specified, in the first
     * case, whether or not the test for interruption occurs before the lock
     * is released.
     *
     * <p>The method returns an estimate of the number of nanoseconds
     * remaining to wait given the supplied {@code nanosTimeout}
     * value upon return, or a value less than or equal to zero if it
     * timed out. This value can be used to determine whether and how
     * long to re-wait in cases where the wait returns but an awaited
     * condition still does not hold. Typical uses of this method take
     * the following form:
     * 这个方法返回给定超时纳秒数的剩余纳秒数预估值（就是如果在给定的超时纳秒数之前收到了唤醒信号，就返回剩余的等待时间），如果超时了，返回一个小于等于0的值。
     * 传入的超时纳秒数M，线程等待了N秒，返回值为M-N
     * 这个返回值（剩余等待时间）可以用来确定当等待返回了但是等待条件仍未满足时，是否重新等待与重新等待的时间。
     * 此方法的典型用法如下：
     * 
     *  <pre> {@code
     * boolean aMethod(long timeout, TimeUnit unit) {
     *   long nanos = unit.toNanos(timeout);
     *   lock.lock();
     *   try {
     *     while (!conditionBeingWaitedFor()) { // 判断是否重新等待
     *       if (nanos <= 0L)
     *         return false;
     *       nanos = theCondition.awaitNanos(nanos); // 等待指定纳秒数
     *     }
     *     // ...
     *   } finally {
     *     lock.unlock();
     *   }
     * }}</pre>
     *
     * <p>Design note: This method requires a nanosecond argument so
     * as to avoid truncation errors in reporting remaining times.
     * Such precision loss would make it difficult for programmers to
     * ensure that total waiting times are not systematically shorter
     * than specified when re-waits occur.
     * 这个方法要求传入的参数为纳秒值，以避免报告剩余时间时出现截断错误。
     * 这种精度损失会导致程序员难以保证在重新等待发生时，总等待时间（系统性的）不短于指定时间。
     * 理解上总等待时间要>=指定时间（对应超时时，返回值为<=0），如果发生精度缺失，会导致总等待时间<指定时间的可能。
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>The current thread is assumed to hold the lock associated with this
     * {@code Condition} when this method is called.
     * It is up to the implementation to determine if this is
     * the case and if not, how to respond. Typically, an exception will be
     * thrown (such as {@link IllegalMonitorStateException}) and the
     * implementation must document that fact.
     *
     * <p>An implementation can favor responding to an interrupt over normal
     * method return in response to a signal, or over indicating the elapse
     * of the specified waiting time. In either case the implementation
     * must ensure that the signal is redirected to another waiting thread, if
     * there is one.
     * （记得发生中断后要把唤醒信号重定向到另一个等待线程）
     *
     * @param nanosTimeout the maximum time to wait, in nanoseconds
     * @return an estimate of the {@code nanosTimeout} value minus
     *         the time spent waiting upon return from this method.
     *         A positive value may be used as the argument to a
     *         subsequent call to this method to finish waiting out
     *         the desired time.  A value less than or equal to zero
     *         indicates that no time remains.
     * indicates 表示，remains 剩余
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     */
    long awaitNanos(long nanosTimeout) throws InterruptedException;

    /**
     * Causes the current thread to wait until it is signalled or interrupted,
     * or the specified waiting time elapses. This method is behaviorally
     * equivalent to:
     *  <pre> {@code awaitNanos(unit.toNanos(time)) > 0}</pre>
     * 这个方法等效于awaitNanos(unit.toNanos(time)) > 0，就是如果在等待时间内被唤醒，返回true，等待超时返回false
     * 这里有个问题，0算等待超时么？可以看具体实现，比如AQS里面的ConditionObject
     *
     * @param time the maximum time to wait
     * @param unit the time unit of the {@code time} argument
     * @return {@code false} if the waiting time detectably elapsed
     *         before return from the method, else {@code true} // 如果在返回之前可检测到等待时间超时，返回true，否则返回false
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     */
    boolean await(long time, TimeUnit unit) throws InterruptedException;

    /**
     * Causes the current thread to wait until it is signalled or interrupted,
     * or the specified deadline elapses.
     * 当前线程等待直到收到信号、被中断，或者超过指定截止日期。
     *
     * <p>The lock associated with this condition is atomically
     * released and the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until <em>one</em> of five things happens:
     * <ul>
     * <li>Some other thread invokes the {@link #signal} method for this
     * {@code Condition} and the current thread happens to be chosen as the
     * thread to be awakened; or
     * <li>Some other thread invokes the {@link #signalAll} method for this
     * {@code Condition}; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread, and interruption of thread suspension is supported; or
     * <li>The specified deadline elapses; or
     * <li>A &quot;<em>spurious wakeup</em>&quot; occurs.
     * </ul>
     * 有五种方式可以打断线程的挂起：
     * 1. signal并且当先线程被选中
     * 2. signalAll
     * 3. interrupted
     * 4. 超过指定的截止日期
     * 5. 虚假唤醒
     *
     * <p>In all cases, before this method can return the current thread must
     * re-acquire the lock associated with this condition. When the
     * thread returns it is <em>guaranteed</em> to hold this lock.
     * （源码里多了个空行，不严谨了-.-）
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * and interruption of thread suspension is supported,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared. It is not specified, in the first
     * case, whether or not the test for interruption occurs before the lock
     * is released.
     *
     *
     * <p>The return value indicates whether the deadline has elapsed,
     * which can be used as follows:
     * 返回值表示当前方法是否已经超过了截止日期
     *
     *  <pre> {@code
     * boolean aMethod(Date deadline) {
     *   boolean stillWaiting = true;
     *   lock.lock();
     *   try {
     *     while (!conditionBeingWaitedFor()) {
     *       if (!stillWaiting)
     *         return false;
     *       stillWaiting = theCondition.awaitUntil(deadline); // 继续等待到原来的截止日期（awaitNanos是等待到剩余时间）
     *     }
     *     // ...
     *   } finally {
     *     lock.unlock();
     *   }
     * }}</pre>
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>The current thread is assumed to hold the lock associated with this
     * {@code Condition} when this method is called.
     * It is up to the implementation to determine if this is
     * the case and if not, how to respond. Typically, an exception will be
     * thrown (such as {@link IllegalMonitorStateException}) and the
     * implementation must document that fact.
     *
     * <p>An implementation can favor responding to an interrupt over normal
     * method return in response to a signal, or over indicating the passing
     * of the specified deadline. In either case the implementation
     * must ensure that the signal is redirected to another waiting thread, if
     * there is one.
     *
     * @param deadline the absolute time to wait until
     * @return {@code false} if the deadline has elapsed upon return, else // （upon 在...之上）
     *         {@code true}
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     */
    boolean awaitUntil(Date deadline) throws InterruptedException;

    /**
     * Wakes up one waiting thread.
     * 唤醒一个等待线程
     *
     * <p>If any threads are waiting on this condition then one
     * is selected for waking up. That thread must then re-acquire the
     * lock before returning from {@code await}.
     * 如果在这个condition下有许多线程在等待，那么会选择一个唤醒。
     * 选中的线程必须在await返回之前重新获取锁。
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>An implementation may (and typically does) require that the
     * current thread hold the lock associated with this {@code
     * Condition} when this method is called. Implementations must
     * document this precondition and any actions taken if the lock is
     * not held. Typically, an exception such as {@link
     * IllegalMonitorStateException} will be thrown.
     * 实现可能（基本是必须）要求调用该方法的线程必须持有该Condition的关联锁，实现必须记录下先决条件和如果没有持有锁下的任何操作。
     * 如果没有持有锁就调用，通常会抛出IllegalMonitorStateException（这是个RuntimeException）。
     */
    void signal();

    /**
     * Wakes up all waiting threads.
     * 唤醒所有等待线程
     *
     * <p>If any threads are waiting on this condition then they are
     * all woken up. Each thread must re-acquire the lock before it can
     * return from {@code await}.
     * 唤醒所有在这个condition上等待的线程，并且每一个线程在从wait返回前必须重新获取到锁
     *（如果是共享锁，所有线程都可以从await转为正式运行，如果是排它锁，就会出现唤醒了一堆，但只有一个会运行，造成部分开销）
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>An implementation may (and typically does) require that the
     * current thread hold the lock associated with this {@code
     * Condition} when this method is called. Implementations must
     * document this precondition and any actions taken if the lock is
     * not held. Typically, an exception such as {@link
     * IllegalMonitorStateException} will be thrown.
     */
    void signalAll();
}
```