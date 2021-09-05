# FutureTask
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

package java.util.concurrent;
import java.util.concurrent.locks.LockSupport;

/**
 * A cancellable asynchronous computation.  This class provides a base
 * implementation of {@link Future}, with methods to start and cancel
 * a computation, query to see if the computation is complete, and
 * retrieve the result of the computation.  The result can only be
 * retrieved when the computation has completed; the {@code get}
 * methods will block if the computation has not yet completed.  Once
 * the computation has completed, the computation cannot be restarted
 * or cancelled (unless the computation is invoked using
 * {@link #runAndReset}).
 * 支持取消的异步计算。
 * 该类基于Future实现了一些方法，支持启动和取消计算，查询计算是否完成，返回计算结果。
 * 只有在计算完成时，才能返回结果；
 * get方法将阻塞直到计算完成。
 * 一旦计算完成，计算不能重新启动或者取消（除非该计算使用runAndReset调用）
 *
 * <p>A {@code FutureTask} can be used to wrap a {@link Callable} or
 * {@link Runnable} object.  Because {@code FutureTask} implements
 * {@code Runnable}, a {@code FutureTask} can be submitted to an
 * {@link Executor} for execution.
 * FutureTask可以用来包装（wrap）Callable或者Runnable对象。
 * 因为FutureTask实现了Runnable接口，可以用于提交到Executor中执行（通过execute方法执行）。
 *
 * <p>In addition to serving as a standalone class, this class provides
 * {@code protected} functionality that may be useful when creating
 * customized task classes.
 * 除了作为独立使用的类外，该类提供的受保护的方法，在创建自定义任务类时可能很有用。
 *
 * @since 1.5
 * @author Doug Lea
 * @param <V> The result type returned by this FutureTask's {@code get} methods
 * V表示get方法的返回值类型
 */
public class FutureTask<V> implements RunnableFuture<V> {
    /*
     * Revision notes: This differs from previous versions of this
     * class that relied on AbstractQueuedSynchronizer, mainly to
     * avoid surprising users about retaining interrupt status during
     * cancellation races. Sync control in the current design relies
     * on a "state" field updated via CAS to track completion, along
     * with a simple Treiber stack to hold waiting threads.
     * 修订（revision）说明：与之前依赖AQS的版本不同，主要为了避免用户对于在取消竞争时仍保留中断状态而感到惊讶。
     * 在当前设计中同步器依赖通过CAS更新的state字段来跟踪完成状态，以及简单的Treiber栈来保存等待线程。
     * （Treiber Stack Algorithm是一个可扩展的无锁栈，利用细粒度的并发原语CAS来实现的）
     *
     * Style note: As usual, we bypass overhead of using
     * AtomicXFieldUpdaters and instead directly use Unsafe intrinsics.
     * 样式说明：像往常一样，绕过使用AtomicXFieldUpdaters的开销，直接使用Unsafe内部函数。
     *
     */

    /**
     * The run state of this task, initially NEW.  The run state
     * transitions to a terminal state only in methods set,
     * setException, and cancel.  During completion, state may take on
     * transient values of COMPLETING (while outcome is being set) or
     * INTERRUPTING (only while interrupting the runner to satisfy a
     * cancel(true)). Transitions from these intermediate to final
     * states use cheaper ordered/lazy writes because values are unique
     * and cannot be further modified.
     * 任务的执行状态（state），初始化是NEW。
     * 运行状态仅在set、setException、cancel方法中会转变为终止（terminal）状态。
     * 在完成期间，状态可能采用COMPLIETING（在设置结果时）或者INTERRUPTING（仅在中断运行程序以满足取消为true时）这种瞬时态。
     * 从这些中间态到最终态的转化，使用cheaper有序/懒惰写入，因为值是唯一的并且无法进一步（further）修改。
     *
     * Possible state transitions:
     * 可能的状态转化
     *
     * NEW -> COMPLETING -> NORMAL
     * NEW -> COMPLETING -> EXCEPTIONAL
     * NEW -> CANCELLED
     * NEW -> INTERRUPTING -> INTERRUPTED
     */
    private volatile int state;
    private static final int NEW          = 0;
    private static final int COMPLETING   = 1;
    private static final int NORMAL       = 2;
    private static final int EXCEPTIONAL  = 3;
    private static final int CANCELLED    = 4;
    private static final int INTERRUPTING = 5;
    private static final int INTERRUPTED  = 6;

    /** The underlying callable; nulled out after running */
    // 底层的callable；执行完成后置为null。
    private Callable<V> callable;   // 是需要执行的任务。
    /** The result to return or exception to throw from get() */
    // 通过get()返回的结果或者抛出的异常
    private Object outcome; // non-volatile, protected by state reads/writes // 非volatile，受state的读/写保护
    /** The thread running the callable; CASed during run() */
    // 执行callable的线程；在run()期间CAS控制
    private volatile Thread runner;   // 执行任务的线程
    /** Treiber stack of waiting threads */
    // Treiber栈保存的等待线程
    private volatile WaitNode waiters; // 用来指向第一个等待线程WaitNode，没有的话为null。这里的等待线程，是指的等待获取结果的线程，而不是执行任务的线程

    /**
     * Returns result or throws exception for completed task.
     * 对于执行完成的任务，返回结果或者抛出异常
     *
     * @param s completed state value
     */
    @SuppressWarnings("unchecked")
    private V report(int s) throws ExecutionException {
        Object x = outcome;
        if (s == NORMAL) // 当state处于NORMAL状态，直接返回执行结果
            return (V)x;
        if (s >= CANCELLED) // 当state处于CANCELLED、INTERRUPTING、INTERRUPTED状态，抛出已取消异常。
            throw new CancellationException();
        throw new ExecutionException((Throwable)x); // 能到这里就剩EXCEPTIONAL了，因为只有state>COMPLETING的才能进入该方法，需要注意，这里把执行结果封装成了Exception给抛出了
                                                     // 构造该异常的流程为：public ExecutionException(Throwable cause)->public Exception(Throwable cause)->public Throwable(Throwable cause) 
    }

    /**
     * Creates a {@code FutureTask} that will, upon running, execute the
     * given {@code Callable}.
     * FutureTask构造函数，将在运行时执行给定的Callable。
     *
     * @param  callable the callable task
     * @throws NullPointerException if the callable is null
     */
    public FutureTask(Callable<V> callable) {
        if (callable == null)
            throw new NullPointerException();
        this.callable = callable;
        this.state = NEW;       // ensure visibility of callable
    }

    /**
     * Creates a {@code FutureTask} that will, upon running, execute the
     * given {@code Runnable}, and arrange that {@code get} will return the
     * given result on successful completion.
     * FutureTask构造函数，将在运行时执行给定的Runnable，并且安排（arrange）在成功完成后，get方法返回给定的result。
     *
     * @param runnable the runnable task
     * @param result the result to return on successful completion. If
     * you don't need a particular result, consider using
     * constructions of the form:
     * {@code Future<?> f = new FutureTask<Void>(runnable, null)}
     * result参数表示成功执行后的返回值，如果不需要特定的result，考虑使用这样形式的结构：
     * Future<?> f = new FutureTask<Void>(runnable, null)
     * Void是java.lang.Void，是一个不可实例化的占位符类，用于对void关键字的引用。在这里表示无返回值。
     *
     * @throws NullPointerException if the runnable is null
     */
    public FutureTask(Runnable runnable, V result) {
        this.callable = Executors.callable(runnable, result); // 使用Executors工具类将Runnable适配为Callable（Executors.callable->Executors#RunnableAdapter类）
        this.state = NEW;       // ensure visibility of callable
    }

    public boolean isCancelled() {
        return state >= CANCELLED; // 包括CANCELLED、INTERRUPTING、INTERRUPTED
    }

    public boolean isDone() {
        return state != NEW; // 不是NEW就算结束（包含瞬时态与其他结束态）
    }

    // 除了将NEW状态转化为INTERRUPTING/CANCELLED
    // 或者对于支持mayInterruptedIfRunning的，将非NEW状态转化为INTERRUPTED
    public boolean cancel(boolean mayInterruptIfRunning) {
    	  // 如果state=NEW，尝试直接设置state=INTERRUPTING（如果这样设置的话），否则设置state=CANCELLED来表示任务取消。（这里只是设置了状态，具体取消在后面）
        if (!(state == NEW &&
              UNSAFE.compareAndSwapInt(this, stateOffset, NEW,
                  mayInterruptIfRunning ? INTERRUPTING : CANCELLED))) // 如果mayInterruptIfRunning为true，表示通过中断执行该任务的线程来尝试停止任务执行，这里直接设置state=INTERRUPTING
            return false;
        try {    // in case call to interrupt throws exception // 如果调用中断会抛出异常
            if (mayInterruptIfRunning) { // 如果允许通过中断执行该任务的线程来停止任务执行
                try {
                    Thread t = runner; // runner是执行该callable的线程
                    if (t != null)
                        t.interrupt(); // 中断该线程
                } finally { // final state
                    UNSAFE.putOrderedInt(this, stateOffset, INTERRUPTED); // 最后，将state置为最终态INTERRUPTED（根据类开始的状态转化关系，只能是从瞬时态INTERRUPTING转化为INTERRUPTED）
                }
            }
        } finally {
            finishCompletion(); // 调用该方法，将所有的等待线程都唤醒与移除。这里runner线程与waitnode线程没有关系，runner是执行任务（callable）的线程，waitnode里的线程是想获取结果的线程
        }
        return true;
    }

    /**
     * @throws CancellationException {@inheritDoc}
     */
    public V get() throws InterruptedException, ExecutionException {
        int s = state;                   // 1、获取当前执行的状态state
        if (s <= COMPLETING)             // 2、如果state<=COMPLETING，表示当前状态为NEW或者COMPLETING时
            s = awaitDone(false, 0L);    // 3、拿当前的state值，如果当前线程被中断了，直接抛出异常，如果处于完成态（>COMPLETING）返回state值（如果本次加入了waiterNode，需要删除），如果=COMPLETING，那么让出CPU时间等待完成，如果不是完成态，那么park等待
        return report(s);                // 4、state表示正常结束就返回实际结果outcome，如果是CANCELLED或者两个INTERRUPT，抛出取消异常，如果是EXCEPTIONAL，抛出对应的异常。
    }

    /**
     * @throws CancellationException {@inheritDoc}
     * 带有限时等待的get方法
     */
    public V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
        if (unit == null)               // 如果限时为null，直接抛出异常
            throw new NullPointerException();
        int s = state;
        if (s <= COMPLETING &&
            (s = awaitDone(true, unit.toNanos(timeout))) <= COMPLETING) // 调用等待完成的方法，如果超时返回的state仍是未完成状态，那么就抛出异常
            throw new TimeoutException();
        return report(s);               // 否则返回对应的执行结果
    }

    /**
     * Protected method invoked when this task transitions to state
     * {@code isDone} (whether normally or via cancellation). The
     * default implementation does nothing.  Subclasses may override
     * this method to invoke completion callbacks or perform
     * bookkeeping. Note that you can query status inside the
     * implementation of this method to determine whether this task
     * has been cancelled.
     * 受保护的方法，当任务转化为isDone状态（state!=NEW）时调用（无论是正常结束还是通过取消）。
     * 默认实现什么都不做。
     * 子类可以覆盖此方法来调用完成时回调方法（callback）或者执行簿记（可能是记录日志的意思？）。
     * 注意，可以在该方法的实现中查询状态，以确定该任务是否已经取消。
     * 这个可以参照ExecutorCompletionService子类，里面有对done方法进行重载，记录完成的task列表
     *
     */
    protected void done() { }

    /**
     * Sets the result of this future to the given value unless
     * this future has already been set or has been cancelled.
     * 设置该future的结果为给定的值，除非该future已经被设置过或者已经被取消。
     *
     * <p>This method is invoked internally by the {@link #run} method
     * upon successful completion of the computation.
     * 当计算成功结束的时候，该方法由run方法内部调用。
     *
     * @param v the value
     */
    protected void set(V v) {
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) { // 将state从NEW更新为COMPLETING（如果已经设置过了，那么就不会重复设置）
            outcome = v;
            UNSAFE.putOrderedInt(this, stateOffset, NORMAL); // final state // 用NORMAL覆盖瞬时态
            finishCompletion();                                              // 释放所有阻塞等待执行结果的等待线程（基本都在awaitDone方法上阻塞了），好让他们拿到结果返回。
        }
    }

    /**
     * Causes this future to report an {@link ExecutionException}
     * with the given throwable as its cause, unless this future has
     * already been set or has been cancelled.
     * 该future使用给定的throwable作为原因上报ExecutionException，除非该future已经被设置过或者被取消。
     *
     * <p>This method is invoked internally by the {@link #run} method
     * upon failure of the computation.
     * 当计算失败的时候，该方法由run方法内部调用。
     *
     * @param t the cause of failure // t表示失败原因
     */
    protected void setException(Throwable t) {
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) { // 将state从NEW更新为COMPLETING（如果已经设置过了，那么就不会重复设置）
            outcome = t;                                                     // 设置结果为异常原因
            UNSAFE.putOrderedInt(this, stateOffset, EXCEPTIONAL); // final state // 用EXCEPTIONAL覆盖瞬时态
            finishCompletion();                                              // 释放所有阻塞等待执行结果的等待线程
        }
    }

    // run()是不返回结果的，结果需要通过get()方法获取
    public void run() {
        if (state != NEW ||
            !UNSAFE.compareAndSwapObject(this, runnerOffset,
                                         null, Thread.currentThread())) // 如果state不是NEW或者设置runner为当前线程失败，直接返回（有其他线程抢着做runner）
            return;
        try {
            Callable<V> c = callable;
            if (c != null && state == NEW) { // 再次判断state
                V result;
                boolean ran;
                try {
                    result = c.call(); // 等待执行完成
                    ran = true;
                } catch (Throwable ex) {
                    result = null;
                    ran = false;
                    setException(ex); // 将outcome设置为对应的异常
                }
                if (ran)
                    set(result); // 将outcome设置为执行结果
            }
        } finally {
            // runner must be non-null until state is settled to
            // prevent concurrent calls to run()
            // runner必须不为null，直到state稳定（settled 固定的），以防止（prevent）并发调用run()方法。（是null的话run方法就可被并发调用执行）
            runner = null;
            // state must be re-read after nulling runner to prevent
            // leaked interrupts
            // 将runner重置为null后，必须重新读取state，以防止泄露中断。（就是执行了上面那一步之后必须重新读取state）
            int s = state;
            if (s >= INTERRUPTING)
                handlePossibleCancellationInterrupt(s); // 如果进finally是因为cancel(true)引起的中断，那么等待中断完成。
        }
    }

    /**
     * Executes the computation without setting its result, and then
     * resets this future to initial state, failing to do so if the
     * computation encounters an exception or is cancelled.  This is
     * designed for use with tasks that intrinsically execute more
     * than once.
     * 执行计算多次但不设置结果，然后重置future为初识状态，
     * 如果计算遇到（encounter）异常或者被取消，则无法这么做。（无法重置为初始值）
     * 设计用于本质上（intrinsically）执行多次的任务。
     *
     * @return {@code true} if successfully run and reset
     */
    protected boolean runAndReset() {
        if (state != NEW ||
            !UNSAFE.compareAndSwapObject(this, runnerOffset,
                                         null, Thread.currentThread())) // 如果state不是NEW或者设置runner为当前线程失败，直接返回（有其他线程抢着做runner）
            return false;
        boolean ran = false;
        int s = state;
        try {
            Callable<V> c = callable;
            if (c != null && s == NEW) {
                try {
                    c.call(); // don't set result // 不设置结果
                    ran = true;
                } catch (Throwable ex) {
                    setException(ex);
                }
            }
        } finally {
            // runner must be non-null until state is settled to
            // prevent concurrent calls to run()
            runner = null;
            // state must be re-read after nulling runner to prevent
            // leaked interrupts
            s = state;
            if (s >= INTERRUPTING)
                handlePossibleCancellationInterrupt(s);
        }
        return ran && s == NEW; // 如果执行成功，返回true（完成且没发生异常，异常有两个方面，一个是执行c.call的时候发生异常，一个是当前runner被cancel的异常）
    }

    /**
     * Ensures that any interrupt from a possible cancel(true) is only
     * delivered to a task while in run or runAndReset.
     * 确保可能来自cancel(true)的任何中断，仅在任务处于run或者runAndReset时传递给任务。
     * 只有一个地方会设置state为INTERRUPTING状态，就是调用cancel(true)的时候。
     * 调用cancel(true)，如果满足state==NEW，那么就设置为INTERRUPTING，直到runner.interrupt()执行完毕才设置INTERRUPTING状态为INTERRUPTED状态。
     *
     */
    private void handlePossibleCancellationInterrupt(int s) {
        // It is possible for our interrupter to stall before getting a
        // chance to interrupt us.  Let's spin-wait patiently.
        // 可能我们的interrupter在获取机会中断我们之前会停止，只需要耐心的（patiently）自旋等待。
        //
        if (s == INTERRUPTING)
            while (state == INTERRUPTING)
                Thread.yield(); // wait out pending interrupt

        // assert state == INTERRUPTED;

        // We want to clear any interrupt we may have received from
        // cancel(true).  However, it is permissible to use interrupts
        // as an independent mechanism for a task to communicate with
        // its caller, and there is no way to clear only the
        // cancellation interrupt.
        // 我们想清除通过cancel(true)可能获得的任何中断。
        // 然而，允许使用中断作为任务与其调用者之间的通信（communicate）的独立机制（java的基础机制），
        // 所以没办法仅取消cancel中断。
        //
        // Thread.interrupted();
    }

    /**
     * Simple linked list nodes to record waiting threads in a Treiber
     * stack.  See other classes such as Phaser and SynchronousQueue
     * for more detailed explanation.
     * 简单的链表节点，用于记录Treiber stack里等待线程。
     * 有关更详细的说明，可以参阅其他类，例如Phaser与SynchronousQueue
     *
     */
    static final class WaitNode {
        volatile Thread thread; // 保存当前线程（当前线程是一个等待获取执行结果的线程，不是执行任务的线程）
        volatile WaitNode next; // 用于指向下一个等待线程WaitNode
        WaitNode() { thread = Thread.currentThread(); } // 将线程封装成WaitNode
    }

    /**
     * Removes and signals all waiting threads, invokes done(), and
     * nulls out callable.
     * 移除和唤醒所有等待（想要拿到执行结果的）线程，调用done()方法，并将调用对象设置为null。
     *
     */
    private void finishCompletion() {
        // assert state > COMPLETING; // 这个assert本来就注释了
        for (WaitNode q; (q = waiters) != null;) { // 遍历Treiber stack结构的等待线程队列（其实是个栈）（这里是不断的从第一个WaitNode（头）开始遍历）
            if (UNSAFE.compareAndSwapObject(this, waitersOffset, q, null)) { // 将q置为null
                for (;;) {
                    Thread t = q.thread;
                    if (t != null) {
                        q.thread = null;
                        LockSupport.unpark(t); // 唤醒当前WaitNode的线程
                    }
                    WaitNode next = q.next;
                    if (next == null) // 如果该节点的next为null，退出本层循环（说明这一次对等待线程的遍历完成了，已唤醒所有等待线程）
                        break;
                    q.next = null; // unlink to help gc // 将next打断，方便GC
                    q = next; // 让q成为下一个waitNode，继续遍历
                }
                break;
            }
        }

        done(); // 本类是空，子类可以根据需要自定义

        callable = null;        // to reduce footprint // 减少足迹？？？
    }

    /**
     * Awaits completion or aborts on interrupt or timeout.
     * 等待完成，或者由于中断或者超时导致的终止。
     *
     * @param timed true if use timed waits
     *        timed true 如果使用限时等待
     * @param nanos time to wait, if timed
     *        nanos 时间值 如果需要限时等待
     * @return state upon completion
     *         完成（或者超时）后返回状态（中断抛出异常）
     */
    private int awaitDone(boolean timed, long nanos)
        throws InterruptedException {
        final long deadline = timed ? System.nanoTime() + nanos : 0L;      // 1、如果需要限时等待，计算截止时间
        WaitNode q = null;                                                  // 2、声明一个等待线程节点，等待执行结果
        boolean queued = false;                                            // queued表示当前等待节点是否已在Treiber stack等待队列（其实是个栈）里
        for (;;) {
            if (Thread.interrupted()) {                                     // 如果当前线程发生中断，移除等待结果的线程节点，抛出异常
                removeWaiter(q);                                            // 为什么要做这个呢？因为q可能在等待栈里，要从等待栈里给移除
                throw new InterruptedException();
            }

            int s = state;
            if (s > COMPLETING) {                                            // 如果state状态成为完成态（包含完成、中断、取消）
                if (q != null)                                               //    如果等待节点不为null，将等待节点的线程置为null（相当于打个标记，为以后removeWaiter()的时候可以删除，当然本次是不会去调用removeWaiter()了）
                    q.thread = null;
                return s;                                                    // 返回执行状态
            }
            else if (s == COMPLETING) // cannot time out yet
                Thread.yield();                                               // 处在完成的瞬时态，提示调度器当前线程可以暂时放弃CPU调度，等再被调度时不会走进park分支，直接判断state是否为完成态。
            else if (q == null)
                q = new WaitNode();                                           // 第一轮没有拿到完成态，创建一个等待节点
            else if (!queued)                                                 // 如果等待节点没在等待队列（其实是个栈）中，尝试入队
                queued = UNSAFE.compareAndSwapObject(this, waitersOffset,
                                                     q.next = waiters, q);     // 新入栈的在栈顶，让当前节点next指向原栈顶元素，然后将当前节点入栈
            else if (timed) {                                                 // 如果需要限时，判断是否等待超时，超时的等待将从等待栈中移除
                nanos = deadline - System.nanoTime();
                if (nanos <= 0L) {
                    removeWaiter(q);
                    return state;
                }
                LockSupport.parkNanos(this, nanos);                          // 需要限时的如果没超时，设置park时间开始阻塞
            }
            else
                LockSupport.park(this);                                      // 这一遍检查发现任务没有完成，开始park阻塞（什么时候会唤醒呢？？？）
        }
    }

    /**
     * Tries to unlink a timed-out or interrupted wait node to avoid
     * accumulating garbage.  Internal nodes are simply unspliced
     * without CAS since it is harmless if they are traversed anyway
     * by releasers.  To avoid effects of unsplicing from already
     * removed nodes, the list is retraversed in case of an apparent
     * race.  This is slow when there are a lot of nodes, but we don't
     * expect lists to be long enough to outweigh higher-overhead
     * schemes.
     * 尝试取消超时或者中断的等待节点的链接，避免垃圾积累。
     * 内部节点在没有CAS的情况下只是（simply 只是，简单的）拆开，因为无论发布者怎么遍历他们都是无害的。
     * 为了避免从已经移除的节点拆开的影响，如果有明显竞争的情况下进行列表回溯。
     * 当有太多节点的时候会很慢，但不希望列表足够长以超过更高开销的方案。
     * （就是通过取消无用的节点，来缩短列表长度，同时避免由于已移除的节点导致连接断了，虽然缩短的过程中也会带来时间消耗）
     */
    private void removeWaiter(WaitNode node) {
        if (node != null) {
            node.thread = null;
            retry:
            for (;;) {          // restart on removeWaiter race // 在有竞争时重新开始
                for (WaitNode pred = null, q = waiters, s; q != null; q = s) { // 开始时q是Treiber等待栈的头元素
                    s = q.next;
                    if (q.thread != null) // 该node的等待线程不为null，那么移动pred指向该node（表示该node不用删除）
                        pred = q;
                    else if (pred != null) { // 如果该node的等待线程为null，那么删除该node（具体操作为让该node的前驱.next指向该node的后继）
                        pred.next = s;
                        if (pred.thread == null) // check for race // 如果发现前驱的thread也是null了，说明前驱也应该删除了，那么就重新遍历等待栈。
                            continue retry;
                    }
                    // 到这里的条件是q.thread == null && pred == null，这表示pred就没找到个不是null的并且q是头节点（其实q不是开始的头结点，是不断通过下面的CAS设置的头结点（原来的头结点就顺带删了））
                    else if (!UNSAFE.compareAndSwapObject(this, waitersOffset,
                                                          q, s)) // 用头结点的next替换头结点，如果替换失败了，说明可能有竞争入队的，就从头重新遍历
                        continue retry;
                }
                break;
            }
        }
    }

    // Unsafe mechanics
    // 一堆使用Unsafe实现的CAS
    private static final sun.misc.Unsafe UNSAFE;
    private static final long stateOffset;
    private static final long runnerOffset;
    private static final long waitersOffset;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = FutureTask.class;
            stateOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("state"));
            runnerOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("runner"));
            waitersOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("waiters"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

}
```