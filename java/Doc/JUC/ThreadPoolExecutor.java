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
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;

/**
 * An {@link ExecutorService} that executes each submitted task using
 * one of possibly several pooled threads, normally configured
 * using {@link Executors} factory methods.
 * 一个ExecutorService，使用可能的几个线程池之一执行提交的任务，
 * 通常使用Executors工厂方法来配置。
 * （通常用Executors工厂方法来生成该类实例，比如给定线程池、线程数等，不过现在不推荐使用）
 *
 * <p>Thread pools address two different problems: they usually
 * provide improved performance when executing large numbers of
 * asynchronous tasks, due to reduced per-task invocation overhead,
 * and they provide a means of bounding and managing the resources,
 * including threads, consumed when executing a collection of tasks.
 * Each {@code ThreadPoolExecutor} also maintains some basic
 * statistics, such as the number of completed tasks.
 * 线程池解决（address）两个不同的问题：
 * 1、通过减少每个任务的调用开销，通常在执行大量异步任务时提供高效（improved）性能（performance）。
 * 2、提供限制（bounding）和管理资源的方法，包括执行任务集合时消耗的线程。
 * 每个ThreadPoolExecutor也维护（maintain）一些基本的统计信息，例如已完成任务的数量。
 *
 * <p>To be useful across a wide range of contexts, this class
 * provides many adjustable parameters and extensibility
 * hooks. However, programmers are urged to use the more convenient
 * {@link Executors} factory methods {@link
 * Executors#newCachedThreadPool} (unbounded thread pool, with
 * automatic thread reclamation), {@link Executors#newFixedThreadPool}
 * (fixed size thread pool) and {@link
 * Executors#newSingleThreadExecutor} (single background thread), that
 * preconfigure settings for the most common usage
 * scenarios. Otherwise, use the following guide when manually
 * configuring and tuning this class:
 * 为了在广泛的上下文中有用，该类提供了许多可调整的参数和可扩展的钩子方法（hooks）。
 * 但是，强烈建议（urge 敦促）程序员使用更方便的（convenient）Executors提供的工厂方法：
 * Executors#newCachedThreadPool：无界线程池，具有自动线程回收（reclamation）
 * Executors#newFixedThreadPool：固定大小线程池
 * Executors#newSingleThreadExecutor：单个后台线程
 * 他们预先配置了最常见的使用场景（scenarios）。
 * 否则，在手工（manually）配置与调整（tuning）此类时，使用如下指南：
 *
 * <dl>
 *
 * <dt>Core and maximum pool sizes</dt>
 * 核心和最大（池中）线程数
 *
 * <dd>A {@code ThreadPoolExecutor} will automatically adjust the
 * pool size (see {@link #getPoolSize})
 * according to the bounds set by
 * corePoolSize (see {@link #getCorePoolSize}) and
 * maximumPoolSize (see {@link #getMaximumPoolSize}).
 * ThreadPoolExecutor会依据corePoolSize（getCorePoolSize）与maximumPoolSize（getMaximumPoolSize）设置的边界，
 * 自动调整池大小（用getPoolSize查询）
 *
 * When a new task is submitted in method {@link #execute(Runnable)},
 * and fewer than corePoolSize threads are running, a new thread is
 * created to handle the request, even if other worker threads are
 * idle.  If there are more than corePoolSize but less than
 * maximumPoolSize threads running, a new thread will be created only
 * if the queue is full.  By setting corePoolSize and maximumPoolSize
 * the same, you create a fixed-size thread pool. By setting
 * maximumPoolSize to an essentially unbounded value such as {@code
 * Integer.MAX_VALUE}, you allow the pool to accommodate an arbitrary
 * number of concurrent tasks. Most typically, core and maximum pool
 * sizes are set only upon construction, but they may also be changed
 * dynamically using {@link #setCorePoolSize} and {@link
 * #setMaximumPoolSize}. </dd>
 * 当使用execute(Runnable)提交任务，
 * 1、如果当前运行的线程数小于corePoolSize时，新的线程被创建来处理该请求（任务），即使其他线程空闲。
 * 2、如果当前运行的线程数大于corePoolSize但小于maximumPoolSize时，只有在队列（等待处理的队列）满的时候才会创建新的线程。
 * 通过设置corePoolSize等于maximumPoolSize，就创建了一个固定大小的线程池。
 * 通过设置maximumPoolSize设置为一个本质上（essentially）无界值（例如Integer.MAX_VALUE），可以允许线程池适应（accommodate 容纳）任意（arbitrary）数量的并发线程。
 * 最典型的是，核心和最大线程池大小只在构造时设置，但也可以视同setCorePoolSize与setMaximumPoolSize来动态更改。
 *
 * <dt>On-demand construction</dt>
 * 按需构建
 *
 * <dd>By default, even core threads are initially created and
 * started only when new tasks arrive, but this can be overridden
 * dynamically using method {@link #prestartCoreThread} or {@link
 * #prestartAllCoreThreads}.  You probably want to prestart threads if
 * you construct the pool with a non-empty queue. </dd>
 * 默认情况下，即使核心线程也仅在新的任务到达时才初始化创建与启动，但可以使用prestartCoreThread或者prestartAllCoreThreads方法动态覆盖（该模式）
 * 如果使用非空队列构建线程池时，可能想预启动线程。
 *
 * <dt>Creating new threads</dt>
 * 创建新线程
 *
 * <dd>New threads are created using a {@link ThreadFactory}.  If not
 * otherwise specified, a {@link Executors#defaultThreadFactory} is
 * used, that creates threads to all be in the same {@link
 * ThreadGroup} and with the same {@code NORM_PRIORITY} priority and
 * non-daemon status. By supplying a different ThreadFactory, you can
 * alter the thread's name, thread group, priority, daemon status,
 * etc. If a {@code ThreadFactory} fails to create a thread when asked
 * by returning null from {@code newThread}, the executor will
 * continue, but might not be able to execute any tasks. Threads
 * should possess the "modifyThread" {@code RuntimePermission}. If
 * worker threads or other threads using the pool do not possess this
 * permission, service may be degraded: configuration changes may not
 * take effect in a timely manner, and a shutdown pool may remain in a
 * state in which termination is possible but not completed.</dd>
 * 使用ThreadFactory创建新的线程。
 * 如果没有另外指定，使用Executors#defaultThreadFactory，所有创建的线程使用相同的ThreadGroup、相同的NORM_PRIORITY优先级和非守护的进程状态。
 * 通过提供不同的ThreadFactory，可以更改线程名、线程组、优先级、守护进程状态等。
 * 如果在调用ThreadFactory的newThread方法返回null时，则创建线程失败，executor将继续运行，但可能无法执行任何任务。
 * 线程应该拥有modifyThread的RuntimePermission（运行时权限）。
 * 如果使用线程池的worker线程或者其他线程没有拥有该权限，则服务可能会被降级（degrade）：
 * 1、参数修改可能无法及时生效
 * 2、关闭线程池可能停留在可以终止但未完成状态
 *
 * <dt>Keep-alive times</dt>
 * 保持活跃的时间
 *
 * <dd>If the pool currently has more than corePoolSize threads,
 * excess threads will be terminated if they have been idle for more
 * than the keepAliveTime (see {@link #getKeepAliveTime(TimeUnit)}).
 * This provides a means of reducing resource consumption when the
 * pool is not being actively used. If the pool becomes more active
 * later, new threads will be constructed. This parameter can also be
 * changed dynamically using method {@link #setKeepAliveTime(long,
 * TimeUnit)}.  Using a value of {@code Long.MAX_VALUE} {@link
 * TimeUnit#NANOSECONDS} effectively disables idle threads from ever
 * terminating prior to shut down. By default, the keep-alive policy
 * applies only when there are more than corePoolSize threads. But
 * method {@link #allowCoreThreadTimeOut(boolean)} can be used to
 * apply this time-out policy to core threads as well, so long as the
 * keepAliveTime value is non-zero. </dd>
 * 如果当前线程池中线程数超过corePoolSize，多余的线程将在空闲时间超过KeepAliveTime（见getKeepAliveTime(TimeUnit)）时被终止。
 * 这提供了一种方法，在线程池未充分利用的情况下减少资源的消耗（consumption）。
 * 如果线程池稍后变得活跃（使用频率变高），则将构建新的线程。
 * 该参数可以使用setKeepAliveTime(long, TimeUnit)方法动态修改。
 * 使用Long.MAX_VALUE TimeUnit#NANOSECONDS值可以有效禁止空闲线程在线程池关闭之前被终止。
 * 默认情况下，keep-alive策略仅在有线程超过corePoolSize的时候才会适用。
 * 但是allowCoreThreadTimeOut(boolean)方法，可以用于核心线程的time-out策略，前提是keepAliveTime值不为0。
 *（就是默认情况下，keepAliveTime仅能控制非核心线程的存活时间，allowCoreThreadTimeOut方法可以控制将keepAliveTime用于核心线程）
 *
 * <dt>Queuing</dt>
 * 队列
 *
 * <dd>Any {@link BlockingQueue} may be used to transfer and hold
 * submitted tasks.  The use of this queue interacts with pool sizing:
 * 任意BlockingQueue都可以用于传递和保存提交的任务。
 * 使用该队列与线程池大小交互如下：
 *
 * <ul>
 *
 * <li> If fewer than corePoolSize threads are running, the Executor
 * always prefers adding a new thread
 * rather than queuing.</li>
 * 如果运行线程数 < corePoolSize，那么Executor总是喜欢（prefer）添加新线程而不是入队。
 *
 * <li> If corePoolSize or more threads are running, the Executor
 * always prefers queuing a request rather than adding a new
 * thread.</li>
 *  如果运行线程数 >= coolPoolSize，Executor总是喜欢将请求入队而不是添加新线程。
 *
 * <li> If a request cannot be queued, a new thread is created unless
 * this would exceed maximumPoolSize, in which case, the task will be
 * rejected.</li>
 * 如果请求无法入队，创建新线程，除非线程数将超过（exceed）maximumPoolSize，在这种情况下，该任务将被拒绝（reject）
 *
 * </ul>
 *
 * There are three general strategies for queuing:
 * 对于队列的三种常见策略：
 *
 * <ol>
 *
 * <li> <em> Direct handoffs.</em> A good default choice for a work
 * queue is a {@link SynchronousQueue} that hands off tasks to threads
 * without otherwise holding them. Here, an attempt to queue a task
 * will fail if no threads are immediately available to run it, so a
 * new thread will be constructed. This policy avoids lockups when
 * handling sets of requests that might have internal dependencies.
 * Direct handoffs generally require unbounded maximumPoolSizes to
 * avoid rejection of new submitted tasks. This in turn admits the
 * possibility of unbounded thread growth when commands continue to
 * arrive on average faster than they can be processed.  </li>
 * Direct handoffs（直接交接/直接握手）。
 * 工作队列一个好的默认选择是SynchronousQueue，将任务提交（hand off）给线程而不用其他方式保留任务。
 * 在这里，如果没有线程可立即执行任务，尝试将任务入队会失败，所以将创建新的线程。
 * 该策略避免锁定，当处理的请求集可能含有内部依赖的时候。
 * Direct handoffs通常需要maximumPoolSize是无界的，以避免新提交的任务被拒绝。
 * 这反过来（turn）又承认（admit）了存在这种可能性：当commands持续到达的平均速度比处理速度快时，线程将无限增长。
 *
 * <li><em> Unbounded queues.</em> Using an unbounded queue (for
 * example a {@link LinkedBlockingQueue} without a predefined
 * capacity) will cause new tasks to wait in the queue when all
 * corePoolSize threads are busy. Thus, no more than corePoolSize
 * threads will ever be created. (And the value of the maximumPoolSize
 * therefore doesn't have any effect.)  This may be appropriate when
 * each task is completely independent of others, so tasks cannot
 * affect each others execution; for example, in a web page server.
 * While this style of queuing can be useful in smoothing out
 * transient bursts of requests, it admits the possibility of
 * unbounded work queue growth when commands continue to arrive on
 * average faster than they can be processed.  </li>
 * Unbounded queues（无界队列）
 * 使用无界队列（例如没有预先设置容量的LinkedBlockingQueue）将导致在所有corePoolSize的线程都忙的时候，新任务在队列中等待。
 * 因此，创建的线程数不会超过corePoolSize。（并且，maximumPoolSize值因此不会有任何影响（没啥用））
 * 这可能适用于：每个任务完全互相独立，因此每个任务不会影响彼此的执行；
 * 例如，在网页服务中，尽管这种风格的队列对于平滑处理突发请求很有用，但在commands持续到达平均速度超过服务处理速度时，会导致工作队列无限增长。
 *
 * <li><em>Bounded queues.</em> A bounded queue (for example, an
 * {@link ArrayBlockingQueue}) helps prevent resource exhaustion when
 * used with finite maximumPoolSizes, but can be more difficult to
 * tune and control.  Queue sizes and maximum pool sizes may be traded
 * off for each other: Using large queues and small pools minimizes
 * CPU usage, OS resources, and context-switching overhead, but can
 * lead to artificially low throughput.  If tasks frequently block (for
 * example if they are I/O bound), a system may be able to schedule
 * time for more threads than you otherwise allow. Use of small queues
 * generally requires larger pool sizes, which keeps CPUs busier but
 * may encounter unacceptable scheduling overhead, which also
 * decreases throughput.  </li>
 * Bounded queues（有界队列）
 * 有界队列（例如ArrayBlockingQueue）当与有限的（finite）maximumPoolSize一起使用时，有助于避免资源耗尽（exhaustion），但可能难以调整和控制。
 * 队列大小和最大线程池大小可能互相影响（traded off 折中）：
 * 使用大队列和小线程池数将最大限度的减少CPU使用率、OS资源和上下文切换开销，但是会认为的（artificially）降低吞吐量。
 * 如果任务频繁（frequently）阻塞（例如受限于I/O），跟你允许的线程数比，系统可能能够安排时间给更多的线程。
 * （就是对于I/O密集型的任务，那么系统可以设置更多的线程来使用CPU等资源，如果设置的线程池数过小，就会导致吞吐量降低）
 * 使用小队列通常需要大线程池数，用于保持CPU繁忙，但是可能会遇到不可接受的调度开销，这也会降低吞吐量。
 * （就是对于CPU密集型任务，如果线程过多，会频繁发生线程调用上下文切换，额外的调度开销）
 *
 * </ol>
 *
 * </dd>
 *
 * <dt>Rejected tasks</dt>
 * 拒绝任务
 *
 * <dd>New tasks submitted in method {@link #execute(Runnable)} will be
 * <em>rejected</em> when the Executor has been shut down, and also when
 * the Executor uses finite bounds for both maximum threads and work queue
 * capacity, and is saturated.  In either case, the {@code execute} method
 * invokes the {@link
 * RejectedExecutionHandler#rejectedExecution(Runnable, ThreadPoolExecutor)}
 * method of its {@link RejectedExecutionHandler}.  Four predefined handler
 * policies are provided:
 * 通过execute(Runnable)方法提交新任务可能被拒绝，1、当Executor已经被关闭，2、或者当Executor在最大线程数与工作队列容量使用有界限制达到饱和（saturated）。
 * 无论哪种场景，execute方法都会调用RejectedExecutionHandler的RejectedExecutionHandle#rejectedException(Runnable, ThreadPoolExecutor)方法。
 * 支持四种预定义的handler策略：
 *
 * <ol>
 *
 * <li> In the default {@link ThreadPoolExecutor.AbortPolicy}, the
 * handler throws a runtime {@link RejectedExecutionException} upon
 * rejection. </li>
 * （本任务丢弃策略，抛出异常）
 * 默认策略为ThreadPoolExecutor.AbortPolicy，该处理器在拒绝时抛出运行时RejectedExecutionException
 *
 * <li> In {@link ThreadPoolExecutor.CallerRunsPolicy}, the thread
 * that invokes {@code execute} itself runs the task. This provides a
 * simple feedback control mechanism that will slow down the rate that
 * new tasks are submitted. </li>
 * （调用者执行策略）
 * 在ThreadPoolExecutor.CallerRunsPolicy，调用execute的线程自己执行该任务（被线程池拒绝的任务）。
 * 这提供了一种简单的反馈（feedback）控制机制，可以减慢提交新任务的速度（rate）。
 *
 * <li> In {@link ThreadPoolExecutor.DiscardPolicy}, a task that
 * cannot be executed is simply dropped.  </li>
 * （本任务直接丢弃策略）
 * 在ThreadPoolExecutor.DiscardPolicy，无法执行的任务只是简单的丢弃。（不执行也不抛出异常）
 * （discard 丢弃）
 *
 * <li>In {@link ThreadPoolExecutor.DiscardOldestPolicy}, if the
 * executor is not shut down, the task at the head of the work queue
 * is dropped, and then execution is retried (which can fail again,
 * causing this to be repeated.) </li>
 * （最老任务丢弃策略）
 * 在ThreadPoolExecutor.DiscardOldestPolicy，如果执行器没有被关闭，在工作队列队首的任务被抛弃，然后重试（retried）执行（有可能再次失败，导致该操作不断重复执行）
 *
 * </ol>
 *
 * It is possible to define and use other kinds of {@link
 * RejectedExecutionHandler} classes. Doing so requires some care
 * especially when policies are designed to work only under particular
 * capacity or queuing policies. </dd>
 * 可以定义和使用其他类型的RejectedExceptionHandler类。
 * 这样做需要谨慎，尤其是当策略设计仅在特定（particular）容量或者排队策略下工作时。
 *
 * <dt>Hook methods</dt>
 * 钩子函数
 *
 * <dd>This class provides {@code protected} overridable
 * {@link #beforeExecute(Thread, Runnable)} and
 * {@link #afterExecute(Runnable, Throwable)} methods that are called
 * before and after execution of each task.  These can be used to
 * manipulate the execution environment; for example, reinitializing
 * ThreadLocals, gathering statistics, or adding log entries.
 * Additionally, method {@link #terminated} can be overridden to perform
 * any special processing that needs to be done once the Executor has
 * fully terminated.
 * 该类提供受保护的可覆盖的beforeExecute(Thread, Runnable)和afterExecute(Runnable, Throwable)方法，在每个任务执行之前与之后被调用。
 * 这些可以用于操作（manipulate）执行环境；例如，重新初始化Threadlocals、收集（gather）统计信息、或者添加日志条目（entries）。
 * 此外，可以覆盖terminated方法，在Executor完全（fully）终止后，执行（perform）需要做的特殊处理。
 * （提及三个钩子方法：beforeExecute、afterExecute、terminated）
 *
 * <p>If hook or callback methods throw exceptions, internal worker
 * threads may in turn fail and abruptly terminate.</dd>
 * 如果hook或者回调（callback）方法抛出异常，内部worker线程可能反过来失败并且突然（abruptly）终止。
 * （就是钩子方法或者回调方法抛出异常的话，会影响该worker线程失败与终止）
 *
 * <dt>Queue maintenance</dt>
 * 队列维护
 *
 * <dd>Method {@link #getQueue()} allows access to the work queue
 * for purposes of monitoring and debugging.  Use of this method for
 * any other purpose is strongly discouraged.  Two supplied methods,
 * {@link #remove(Runnable)} and {@link #purge} are available to
 * assist in storage reclamation when large numbers of queued tasks
 * become cancelled.</dd>
 * getQueue()方法允许访问该线程队列，用于监控和debug目的。
 * 强烈建议不要（discouraged 不建议）将该方法用于其他目的。
 * 提供的两个方法remove(Runnable)和purge，在大量入队的任务被取消时，可用于协助存储回收（reclamation）。
 *
 * <dt>Finalization</dt>
 * 定稿？？？
 *
 * <dd>A pool that is no longer referenced in a program <em>AND</em>
 * has no remaining threads will be {@code shutdown} automatically. If
 * you would like to ensure that unreferenced pools are reclaimed even
 * if users forget to call {@link #shutdown}, then you must arrange
 * that unused threads eventually die, by setting appropriate
 * keep-alive times, using a lower bound of zero core threads and/or
 * setting {@link #allowCoreThreadTimeOut(boolean)}.  </dd>
 * 程序中不再被引用并且没有剩余（remain）线程的线程池将被自动关闭。
 * 如果你想确保未引用的线程池被回收，即使使用者忘记调用shutdown，
 * 那么你必须安排未使用的线程最终（eventually）死亡，通过设置适当的（appropriate）keep-alive时间、使用0核心线程数的下限并且/或者设置allowCoreThreadTimeOut(boolean)。
 * （corePoolSize为0时，allowCoreThreadTimeOut这个方法可设可不设，corePoolSize不为0时，必须设）
 * （如果线程池中没有存活线程，并且线程池没有被引用，那么线程池对象就会被自动回收）
 *
 * </dl>
 *
 * <p><b>Extension example</b>. Most extensions of this class
 * override one or more of the protected hook methods. For example,
 * here is a subclass that adds a simple pause/resume feature:
 * 扩展样例。大多数该类的扩展类都会覆盖一个或多个受保护的hook方法。
 * 例如，下面这个子类增加了一个简单的暂停/恢复功能（feature 特性）。
 *
 *  <pre> {@code
 * class PausableThreadPoolExecutor extends ThreadPoolExecutor {
 *   private boolean isPaused;
 *   private ReentrantLock pauseLock = new ReentrantLock();  // 看着陌生的话可以先看看Lock、Condition、AQS、ReentrantLock部分源码
 *   private Condition unpaused = pauseLock.newCondition();
 *
 *   public PausableThreadPoolExecutor(...) { super(...); }
 *
 *   protected void beforeExecute(Thread t, Runnable r) {
 *     super.beforeExecute(t, r);
 *     pauseLock.lock();
 *     try {
 *       while (isPaused) unpaused.await();
 *     } catch (InterruptedException ie) {
 *       t.interrupt();
 *     } finally {
 *       pauseLock.unlock();
 *     }
 *   }
 *
 *   public void pause() {
 *     pauseLock.lock();
 *     try {
 *       isPaused = true;
 *     } finally {
 *       pauseLock.unlock();
 *     }
 *   }
 *
 *   public void resume() {
 *     pauseLock.lock();
 *     try {
 *       isPaused = false;
 *       unpaused.signalAll();
 *     } finally {
 *       pauseLock.unlock();
 *     }
 *   }
 * }}</pre>
 *
 * @since 1.5
 * @author Doug Lea
 */
public class ThreadPoolExecutor extends AbstractExecutorService {
    /**
     * The main pool control state, ctl, is an atomic integer packing
     * two conceptual fields
     *   workerCount, indicating the effective number of threads
     *   runState,    indicating whether running, shutting down etc
     * ctl，线程池主要的控制状态，是一个原子integer，包含两个概念（conceptual）字段
     *   workerCount，代表有效的线程数
     *   runState，   代表线程池状态是否为运行、关闭等
     *
     * In order to pack them into one int, we limit workerCount to
     * (2^29)-1 (about 500 million) threads rather than (2^31)-1 (2
     * billion) otherwise representable. If this is ever an issue in
     * the future, the variable can be changed to be an AtomicLong,
     * and the shift/mask constants below adjusted. But until the need
     * arises, this code is a bit faster and simpler using an int.
     * 为了将这两个字段打包成一个int，将workerCount限制为(2^29)-1个线程，而不是(2^31)-1个其他可表示的线程。
     * 如果这在将来成为问题，该变量（variable）ctl可以改为AtomicLong类型，并调整下面的移位/掩码常量。（常量指的COUNT_BITS）
     * 但是在需要之前（arises 出现），这段代码使用int会更快更简单。
     *
     * The workerCount is the number of workers that have been
     * permitted to start and not permitted to stop.  The value may be
     * transiently different from the actual number of live threads,
     * for example when a ThreadFactory fails to create a thread when
     * asked, and when exiting threads are still performing
     * bookkeeping before terminating. The user-visible pool size is
     * reported as the current size of the workers set.
     * workerCount表示已经被允许启动但未允许停止的worker数量。
     * 这个值可能和实际活动线程数暂时（transiently）不一致，（不保证一致是因为workerCount-1操作与workers.remove(w)不是一个Lock锁下的同步操作）
     * 例如，当ThreadFactory创建线程失败，当退出线程在终止前仍旧执行bookkeeping。
     * 用户可见的线程池大小代表worker集当前大小。
     *
     * The runState provides the main lifecycle control, taking on values:
     * runState提供主要的生命周期控制，取值如下：
     *
     *   RUNNING:  Accept new tasks and process queued tasks
     *             接受新任务并处理队列中的任务
     *   SHUTDOWN: Don't accept new tasks, but process queued tasks
     *             不接受新任务，但处理队列中的任务
     *   STOP:     Don't accept new tasks, don't process queued tasks,
     *             and interrupt in-progress tasks
     *             不接受新任务，不处理队列中的任务
     *             并且中断正在处理的任务
     *   TIDYING:  All tasks have terminated, workerCount is zero,
     *             the thread transitioning to state TIDYING
     *             will run the terminated() hook method
     *             所有任务已经终止，workerCount为0，
     *             线程转化为TIDYING状态（TIDYING 整理）
     *             将执行terminated()钩子方法
     *   TERMINATED: terminated() has completed
     *             terminated()方法执行完毕
     *             
     *
     * The numerical order among these values matters, to allow
     * ordered comparisons. The runState monotonically increases over
     * time, but need not hit each state. The transitions are:
     * 该值（指的runState）之间的数字（numerical）顺序很重要（matter），以允许进行有序比较。
     * runState跟随时间单调（monotonically）递增，但不需要命中每个状态（就是不需要按顺序递增，比如不需要-1、0、1、2、3这样增，可以跳为-1、1这样，只要单调递增就行）
     * 过渡是：
     *
     * RUNNING -> SHUTDOWN
     *    On invocation of shutdown(), perhaps implicitly in finalize()
     *    从RUNNING转化为SHUTDOWN：在调用shutdown()时，可能（perhaps）隐藏（implicitly）在finalize()方法中（finalize()中会执行shutdown()）
     * (RUNNING or SHUTDOWN) -> STOP
     *    On invocation of shutdownNow()
     *    （RUNNING或者SHUTDOWN）转化为STOP：在调用shutdownNow()的时候（转化为STOP状态会remove掉队列里的所有任务？？？）
     * SHUTDOWN -> TIDYING
     *    When both queue and pool are empty
     *    SHUTDOWN转化为TIDYING：当队列与线程池都为空时
     * STOP -> TIDYING
     *    When pool is empty
     *    STOP转化为TIDYING：当线程池为空时
     * TIDYING -> TERMINATED
     *    When the terminated() hook method has completed
     *    TIDYING转化为TERMINATED：当terminated()钩子方法执行完毕
     *
     * Threads waiting in awaitTermination() will return when the
     * state reaches TERMINATED.
     * 在awaitTermination()方法上等待的线程，将在状态到达TERMINATED时返回。
     *
     * Detecting the transition from SHUTDOWN to TIDYING is less
     * straightforward than you'd like because the queue may become
     * empty after non-empty and vice versa during SHUTDOWN state, but
     * we can only terminate if, after seeing that it is empty, we see
     * that workerCount is 0 (which sometimes entails a recheck -- see
     * below).
     * 检测（detecting）从SHUTDOWN到TIDYING的转变并不像你想要的那么直接（straightforward），
     * 因为在SHUTDOWN状态期间，队列可能从非空变为空，反之亦然（vice versa），（就是不能保证队列是否一定为空，因为会变化）
     * 但是我们只能在看到队列为空之后，再检查workerCount也是0时，才终止（有些时候需要（entail 包含）复查 -- 见下文）
     *
     */
    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0)); // 线程池主要控制状态，初始化为runState=RUNNING，workerCount=0
    private static final int COUNT_BITS = Integer.SIZE - 3;                // 总位数
    private static final int CAPACITY   = (1 << COUNT_BITS) - 1;           // worker线程最大容量

    // runState is stored in the high-order bits // runState存储在高位，从RUNNING到TERMINATED，值逐渐递增
    private static final int RUNNING    = -1 << COUNT_BITS; // -536870912
    private static final int SHUTDOWN   =  0 << COUNT_BITS; // 0
    private static final int STOP       =  1 << COUNT_BITS; // 536870912
    private static final int TIDYING    =  2 << COUNT_BITS; // 1073741824
    private static final int TERMINATED =  3 << COUNT_BITS; // 1610612736

    // Packing and unpacking ctl // 打包和解包ctl
    private static int runStateOf(int c)     { return c & ~CAPACITY; } // 解析线程池运行状态
    private static int workerCountOf(int c)  { return c & CAPACITY; }  // worker数量
    private static int ctlOf(int rs, int wc) { return rs | wc; }       // 将runState与workerCount打包成ctl

    /*
     * Bit field accessors that don't require unpacking ctl.
     * These depend on the bit layout and on workerCount being never negative.
     * 不需要解包ctl的位域访问器
     * 这取决于位布局和workerCount永远不会为负值
     */

    private static boolean runStateLessThan(int c, int s) {
        return c < s;
    }

    private static boolean runStateAtLeast(int c, int s) {
        return c >= s;
    }

    // 判断线程池状态是否为running（c < SHUTDOWN）
    private static boolean isRunning(int c) {
        return c < SHUTDOWN;
    }

    /**
     * Attempts to CAS-increment the workerCount field of ctl.
     * 尝试通过CAS递增ctl里的workerCount值
     *
     */
    private boolean compareAndIncrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect + 1);
    }

    /**
     * Attempts to CAS-decrement the workerCount field of ctl.
     * 尝试通过CAS递减ctl里的workerCount值
     *
     */
    private boolean compareAndDecrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect - 1);
    }

    /**
     * Decrements the workerCount field of ctl. This is called only on
     * abrupt termination of a thread (see processWorkerExit). Other
     * decrements are performed within getTask.
     * 递减ctl里的workerCount值。该方法可以被两个方法调用：
     * 1、processWorkerExit方法，调用条件为：仅在线程突然（abrupt）终止时调用
     * 2、getTask方法：其他递减在getTask时执行。（比如线程池有超过corePoolSize的线程空闲等）
     * 
     */
    private void decrementWorkerCount() {
        do {} while (! compareAndDecrementWorkerCount(ctl.get()));
    }

    /**
     * The queue used for holding tasks and handing off to worker
     * threads.  We do not require that workQueue.poll() returning
     * null necessarily means that workQueue.isEmpty(), so rely
     * solely on isEmpty to see if the queue is empty (which we must
     * do for example when deciding whether to transition from
     * SHUTDOWN to TIDYING).  This accommodates special-purpose
     * queues such as DelayQueues for which poll() is allowed to
     * return null even if it may later return non-null when delays
     * expire.
     * 该队列用于保存任务和将任务移交给worker线程。
     * 不要求workQueue.poll()返回null一定意味着workQueue.isEmpty()，
     * 所以只能（solely）依赖（rely on）isEmpty来检查队列是否为空（例如，当我们需要决定是否将线程池状态从SHUTDOWN转化为TIDYING）。
     * 这样能适应特殊目的的队列，例如DelayQueue，允许poll()返回null（可能没元素，或者元素还没到延迟时间），在稍后延迟到期时返回非null值。
     * 
     */
    private final BlockingQueue<Runnable> workQueue;

    /**
     * Lock held on access to workers set and related bookkeeping.
     * While we could use a concurrent set of some sort, it turns out
     * to be generally preferable to use a lock. Among the reasons is
     * that this serializes interruptIdleWorkers, which avoids
     * unnecessary interrupt storms, especially during shutdown.
     * Otherwise exiting threads would concurrently interrupt those
     * that have not yet interrupted. It also simplifies some of the
     * associated statistics bookkeeping of largestPoolSize etc. We
     * also hold mainLock on shutdown and shutdownNow, for the sake of
     * ensuring workers set is stable while separately checking
     * permission to interrupt and actually interrupting.
     * 对设置（操作）workers与关联bookkeeping进行加锁访问。
     * 尽管可以使用某种（some sort）并发集，结果（turn out）证明通常最好使用锁。
     * 其中一个原因是它序列化了interruptIdleWorkers，避免了不必要的中断风暴，尤其是在线程池shutdown的时候。
     * 否则退出线程将同时中断那些尚未中断的线程。（可能n个中断线程并发去中断1个尚未中断的线程）
     * 它还简化了最大线程池数等关联统计bookkeeping。
     * 也在shutdown与shutdownNow的时候持有mainLock，为了确保worker集合在分别检查中断权限和实际中断时保持稳定
     * （几乎所有涉及到对workers这个集合的操作，都要加mainLock锁）
     *
     */
    private final ReentrantLock mainLock = new ReentrantLock();

    /**
     * Set containing all worker threads in pool. Accessed only when
     * holding mainLock.
     * 集合包含线程池里所有的worker线程。
     * 只有在持有mainLock锁时才能访问。
     *
     */
    private final HashSet<Worker> workers = new HashSet<Worker>();

    /**
     * Wait condition to support awaitTermination
     * 在条件上等待，支持awaitTermination
     * （不好翻译，可以先看看Condition接口源码）
     *
     */
    private final Condition termination = mainLock.newCondition();

    /**
     * Tracks largest attained pool size. Accessed only under
     * mainLock.
     * 跟踪线程池到达的最大线程数。
     * 只有持有mainLock时才能访问。
     *
     */
    private int largestPoolSize;

    /**
     * Counter for completed tasks. Updated only on termination of
     * worker threads. Accessed only under mainLock.
     * 统计已完成任务数。
     * 仅在worker线程结束后更新该值。（可见processWorkerExit方法）
     * 只有持有mainLock才能访问。
     *
     */
    private long completedTaskCount;

    /*
     * All user control parameters are declared as volatiles so that
     * ongoing actions are based on freshest values, but without need
     * for locking, since no internal invariants depend on them
     * changing synchronously with respect to other actions.
     * 所有用户控制的参数都被声明为volatile，以便在进行的动作都基于最新的值，
     * 但不需要加锁，因为没有内部不变量（invariants）依赖其他动作进行同步修改。
     * 
     */

    /**
     * Factory for new threads. All threads are created using this
     * factory (via method addWorker).  All callers must be prepared
     * for addWorker to fail, which may reflect a system or user's
     * policy limiting the number of threads.  Even though it is not
     * treated as an error, failure to create threads may result in
     * new tasks being rejected or existing ones remaining stuck in
     * the queue.
     * 新线程的工厂。所有线程都通过该factory来创建（通过addWorker方法）。
     * 所有调用者都必须准备好对addWorker失败的处理，这可能反映了系统或者使用者限制线程数的策略。
     * 即使它没有被作为（treat 对待）error，创建线程失败可能导致新的任务被拒绝或者现有任务卡在队列中。
     *
     * We go further and preserve pool invariants even in the face of
     * errors such as OutOfMemoryError, that might be thrown while
     * trying to create threads.  Such errors are rather common due to
     * the need to allocate a native stack in Thread.start, and users
     * will want to perform clean pool shutdown to clean up.  There
     * will likely be enough memory available for the cleanup code to
     * complete without encountering yet another OutOfMemoryError.
     * 我们更进一步，即使在面临异常例如在创建线程时可能抛出OOM异常，也保留线程池的不变量。
     * 由于在Thread.start时需要从本地栈中分配，因此这类error相当常见，并且用户希望干净的shutdown线程池来进行清理。（清理从栈上分配的线程空间？？？）
     * 可能有足够的内存可用于清理代码完成，而不会遇到其他OOM异常。
     * 
     */
    private volatile ThreadFactory threadFactory;

    /**
     * Handler called when saturated or shutdown in execute.
     * 当饱和（saturated 饱和的）或者shutdown时执行的handler。（用于处理那些线程池处理不了的task）
     *
     */
    private volatile RejectedExecutionHandler handler;

    /**
     * Timeout in nanoseconds for idle threads waiting for work.
     * Threads use this timeout when there are more than corePoolSize
     * present or if allowCoreThreadTimeOut. Otherwise they wait
     * forever for new work.
     * 空闲线程最大等待作业时间。（单位：纳秒）
     * 线程使用此超时：
     * 1、当前线程数超过corePoolSize（核心线程数）
     * 2、允许核心线程超时退出（allowCoreThreadTimeOut）
     * 否则会永久等待新的作业。
     * 
     */
    private volatile long keepAliveTime;

    /**
     * If false (default), core threads stay alive even when idle.
     * If true, core threads use keepAliveTime to time out waiting
     * for work.
     * 如果为false（也是默认值），核心线程即使空闲也会保持活跃（alive）。
     * 如果为true，核心线程在等待作业时使用keepAliveTime设置的限时时间。（超时退出）
     *
     */
    private volatile boolean allowCoreThreadTimeOut;

    /**
     * Core pool size is the minimum number of workers to keep alive
     * (and not allow to time out etc) unless allowCoreThreadTimeOut
     * is set, in which case the minimum is zero.
     * 核心线程池大小是保持活跃（并且不允许超时等）的worker最小值，除非设置了allowCoreThreadTimeOut（设置allowCoreThreadTimeOut的场景下，线程数最小值为0）
     * （注意：0<=corePoolSize<=CAPACITY）
     */
    private volatile int corePoolSize;

    /**
     * Maximum pool size. Note that the actual maximum is internally
     * bounded by CAPACITY.
     * 最大线程池大小。注意，实际上的最大值受CAPACITY内部限制。（maximumCoreSize不能超过CAPACITY）
     * （注意：corePoolSize<=maximumPoolSize<=CAPACITY）
     */
    private volatile int maximumPoolSize;

    /**
     * The default rejected execution handler
     * 默认的拒绝执行的处理程序（handler）
     * （当任务被拒绝时，默认采用的拒绝策略是AbortPolicy()，拒绝执行并抛出RejectedExecutionException（extends RuntimeException））
     *
     */
    private static final RejectedExecutionHandler defaultHandler =
        new AbortPolicy();

    /**
     * Permission required for callers of shutdown and shutdownNow.
     * We additionally require (see checkShutdownAccess) that callers
     * have permission to actually interrupt threads in the worker set
     * (as governed by Thread.interrupt, which relies on
     * ThreadGroup.checkAccess, which in turn relies on
     * SecurityManager.checkAccess). Shutdowns are attempted only if
     * these checks pass.
     * 调用者执行shutdown与shutdownNow时需要的权限。
     * 我们还要求（见checkShutdonwAccess）调用者具有实际能够 中断在worker集里的线程 的权限。
     * （由Thread.interrupt管理（governed），它依赖ThreadGroup.checkAccess，后者又依赖于SecurityManager.checkAccess）。
     * 只有该检查通过，shutdown系列方法才会尝试执行。（尝试关闭）
     *
     * All actual invocations of Thread.interrupt (see
     * interruptIdleWorkers and interruptWorkers) ignore
     * SecurityExceptions, meaning that the attempted interrupts
     * silently fail. In the case of shutdown, they should not fail
     * unless the SecurityManager has inconsistent policies, sometimes
     * allowing access to a thread and sometimes not. In such cases,
     * failure to actually interrupt threads may disable or delay full
     * termination. Other uses of interruptIdleWorkers are advisory,
     * and failure to actually interrupt will merely delay response to
     * configuration changes so is not handled exceptionally.
     * 所有实际调用Thread.interrupte（见interruptIdleWorkers和interruptWorkers方法）都忽略SecurityExceptions，
     * 这意味着尝试的中断失败时都是静默的。（silently fail反义词可以看做throw Exception）
     * 在shutdown场景下，除非SecurityManage有非一致性策略（有时允许访问线程有时不允许），否则不应该失败。
     * 在这种情况下，未能实际中断线程可能导致不可用或者延迟完全终止。（啥不可用，啥延迟完全终止？？？）
     * interruptIdleWorkers的其他用法是建议性的（advisory），未能实际中断只会（merely）延迟对参数修改的响应，所以不会被异常处理。
     * 
     */
    private static final RuntimePermission shutdownPerm =
        new RuntimePermission("modifyThread"); // 修改线程，例如通过调用线程的 interrupt、stop、suspend、resume、setDaemon、setPriority、setName 和 setUncaughtExceptionHandler 方法

    /**
     * Class Worker mainly maintains interrupt control state for
     * threads running tasks, along with other minor bookkeeping.
     * This class opportunistically extends AbstractQueuedSynchronizer
     * to simplify acquiring and releasing a lock surrounding each
     * task execution.  This protects against interrupts that are
     * intended to wake up a worker thread waiting for a task from
     * instead interrupting a task being run.  We implement a simple
     * non-reentrant mutual exclusion lock rather than use
     * ReentrantLock because we do not want worker tasks to be able to
     * reacquire the lock when they invoke pool control methods like
     * setCorePoolSize.  Additionally, to suppress interrupts until
     * the thread actually starts running tasks, we initialize lock
     * state to a negative value, and clear it upon start (in
     * runWorker).
     * Worker类主要用于保存线程运行的任务中断控制状态，和其他次要bookkeeping。
     * 该类取巧继承AQS，来实现围绕每个任务执行获取和释放锁。
     * 可以防止中断用于唤醒等待任务的worker线程，而不是中断正在执行的任务。（是想用中断来唤醒等待的线程而不是中断正在执行的线程？？？）
     * 我们实现了一个简单的不可重入的互斥锁，而不是使用ReentrantLock，
     * 因为我们不想worker任务能够在调用线程池控制方法（像setCorePoolSize）时能够重新获取锁（再次加锁）。
     * 此外，为了在线程实际开始执行任务之前抑制（suppress）中断，我们初始化锁的状态为负值，在开始（在runWorker）之后清除它。（可以先看runWorker方法）
     * 
     */
    private final class Worker
        extends AbstractQueuedSynchronizer  // 继承AQS
        implements Runnable                 // 实现Runnable
    {
        /**
         * This class will never be serialized, but we provide a
         * serialVersionUID to suppress a javac warning.
         * 该类永远不会被序列化，但是提供了serialVersionUID来抑制javac警告。
         *
         */
        private static final long serialVersionUID = 6138294804551838833L;

        /** Thread this worker is running in.  Null if factory fails. */
        // 运行该worker的线程，如果工厂失败则为null。？？？
        final Thread thread;
        /** Initial task to run.  Possibly null. */
        // 初始化的待运行任务，可能为null（比如观察到任务队列有任务但线程池里没worker了）
        Runnable firstTask;
        /** Per-thread task counter */
        // 每个线程任务计数器
        volatile long completedTasks;

        /**
         * Creates with given first task and thread from ThreadFactory.
         * 构造方法，用给定的任务和从ThreadFactory生成的线程来创建。
         * @param firstTask the first task (null if none)
         */
        Worker(Runnable firstTask) {
            setState(-1); // inhibit interrupts until runWorker // 禁止（inhibit）中断，直到runWorker。设置的state是AQS的state
            this.firstTask = firstTask;
            this.thread = getThreadFactory().newThread(this); // 将该worker作为线程的执行对象
        }

        /** Delegates main run loop to outer runWorker  */
        // 主运行循环委托给外部的runWorker（run的时候咋执行由外部的runWorker决定）
        public void run() {
            runWorker(this);
        }

        // Lock methods
        // 锁相关方法
        //
        // The value 0 represents the unlocked state.
        // The value 1 represents the locked state.
        // 值为0代表解锁（未加锁）状态
        // 值为1代表加所状态

        // isHeldExclusively、tryAcquire、tryRelease是使用AQS实现独占锁需要自己实现的三个方法

        // 当前线程是否持有独占锁
        protected boolean isHeldExclusively() {
            return getState() != 0; // getState()方法是AQS定义的，用来获取当前state
        }

        // 尝试加锁
        protected boolean tryAcquire(int unused) {  // 入参没用到
            if (compareAndSetState(0, 1)) { // 调用AQS#compareAndSetState方法，设置state值（类比ReentrantLock，Worker的state只有0、1两个值）
                setExclusiveOwnerThread(Thread.currentThread()); // 如果加锁成功，更新独占锁的持有者为当前线程
                return true;
            }
            return false;
        }

        // 尝试解锁（解锁必定成功）
        protected boolean tryRelease(int unused) {
            setExclusiveOwnerThread(null);
            setState(0); // 这里没有通过CAS来设置值，直接置为0（需要看为什么如此自信）
            return true;
        }

        public void lock()        { acquire(1); } // AQS#acquire方法，尝试获取锁，如果失败入等待队列阻塞
        public boolean tryLock()  { return tryAcquire(1); } // Worker#tryAcquire
        public void unlock()      { release(1); } // AQS#release方法，释放锁，并尝试从AQS等待队列的head开始唤醒一个可用后继。这个方法会调用tryRelease，所以调用完state变为0了。
        public boolean isLocked() { return isHeldExclusively(); } // Worker#isHeldExclusively

        void interruptIfStarted() {
            Thread t;
            if (getState() >= 0 && (t = thread) != null && !t.isInterrupted()) { // 判断执行当前worker的线程是否已启动。state有值，thread不为null，thread没有中断
                try {
                    t.interrupt();
                } catch (SecurityException ignore) { // 捕获异常并忽略
                }
            }
        }
    }

    /*
     * Methods for setting control state
     * 设置控制状态的方法
     */

    /**
     * Transitions runState to given target, or leaves it alone if
     * already at least the given target.
     * 将runState转化为给定的目标值，如果runState的值已经>=给定的目标值，则不做任何操作。
     *
     * @param targetState the desired state, either SHUTDOWN or STOP
     *        (but not TIDYING or TERMINATED -- use tryTerminate for that)
     *        targetState，所需的状态，只能为SHUTDOWN或者STOP
     *        如果想修改runState为TIDYING或者TERMINATED，那么使用tryTerminate方法
     */
    private void advanceRunState(int targetState) {
        for (;;) {
            int c = ctl.get();
            if (runStateAtLeast(c, targetState) ||
                ctl.compareAndSet(c, ctlOf(targetState, workerCountOf(c))))
                break;
        }
    }

    /**
     * Transitions to TERMINATED state if either (SHUTDOWN and pool
     * and queue empty) or (STOP and pool empty).  If otherwise
     * eligible to terminate but workerCount is nonzero, interrupts an
     * idle worker to ensure that shutdown signals propagate. This
     * method must be called following any action that might make
     * termination possible -- reducing worker count or removing tasks
     * from the queue during shutdown. The method is non-private to
     * allow access from ScheduledThreadPoolExecutor.
     * runState值转化为TERMINATED，当以下两种情况任意一种发生：
     * 1、runState=SHUTDOWN，并且线程池与任务队列都为空
     * 2、runState=STOP，并且线程池为空
     * 如果有资格（eligible）终止但是workerCount不为0，则中断空闲的worker保证关闭信号传播。
     * 必须在可能会导致线程池终止的操作发生后调用该方法，可能的操作包括：
     * 1、在shutdown期间减少worker数量
     * 2、在shutdown期间从任务队列移除任务
     * （就是涉及上面两种操作的方法A可能会导致线程池关闭，那就在A方法之后调用该方法更新runState值为TERMINATED）
     * 该方法是非私有的，允许从ScheduledThreadPoolExecutor来访问。
     * 
     */
    final void tryTerminate() { // 要注意该方法目标是要把线程池的runState更新为TERMINATED
        for (;;) {
            int c = ctl.get();
            if (isRunning(c) ||                                       // 如果线程池还在运行，
                runStateAtLeast(c, TIDYING) ||                        // 或者状态已经>=TIDYING（包含TIDYING、TERMINATED）（TIDYING状态是钩子方法还没执行完，执行完就成TERMINATED）
                (runStateOf(c) == SHUTDOWN && ! workQueue.isEmpty())) // 或者状态=SHUTDOWN但是还有任务没完成
                return;                                              // 则不能设置runState为TERMINATED
            if (workerCountOf(c) != 0) { // Eligible to terminate     // 走到这一步，runState=SHUTDOWN或者STOP，此时任务队列为空，如果worker数不为0（还有存活的worker线程）
                interruptIdleWorkers(ONLY_ONE);                       // 尝试中断空闲的worker（只中断一个）。（因为现在没任务了并且线程池处于即将关闭状态，空闲线程留着也没用）
                return;
            }

            final ReentrantLock mainLock = this.mainLock;            // 到此runState=SHUTDOWN或者STOP，任务队列与worker都为empty，开始修改runState值
            mainLock.lock();                                          // 获取mainLock
            try {
                if (ctl.compareAndSet(c, ctlOf(TIDYING, 0))) {        // 尝试将ctl转化成runState=TIDYING，workerCount=0
                    try {
                        terminated();                                 // 调用钩子方法ThreadPoolExecutor#terminated()，本类该方法为空，子类可以按照自己的需求实现
                    } finally {
                        ctl.set(ctlOf(TERMINATED, 0));                // terminated()钩子方法执行完毕，ctl转化为runState=TERMINATED，workerCount=0
                        termination.signalAll();                      // 唤醒所有在Condition上等待的线程（还没有见到Condition的用处）
                    }
                    return;
                }
            } finally {
                mainLock.unlock();
            }
            // else retry on failed CAS
        }
    }

    /*
     * Methods for controlling interrupts to worker threads.
     * 控制worker线程中断的方法
     *
     */

    /**
     * If there is a security manager, makes sure caller has
     * permission to shut down threads in general (see shutdownPerm).
     * If this passes, additionally makes sure the caller is allowed
     * to interrupt each worker thread. This might not be true even if
     * first check passed, if the SecurityManager treats some threads
     * specially.
     * 如果有线程管理器，通常需要确保调用者拥有关闭线程的权限（见shutdownPrem）。
     * 如果上面的通过了，另外需要确保允许调用者中断所有worker线程。
     * 即使第一个检查通过了，如果SecurityManager特殊对待某些线程，这也不一定成立。
     *
     */
    private void checkShutdownAccess() {
        SecurityManager security = System.getSecurityManager(); // 如果当前应用创建了安全管理器，那么返回该管理器，否则返回null
        if (security != null) {
            security.checkPermission(shutdownPerm);             // 检查是否有shutdown权限，如果没有抛出基于SecurityException的异常
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();                                    // 加锁是为了防止在遍历workers的时候，worker集合有变化
            try {
                for (Worker w : workers)
                    security.checkAccess(w.thread);             // 逐个检查是否有对每个worker线程的访问权限（具体是MODIFY_THREAD_PERMISSION权限），如果没有抛出基于SecurityException的异常
            } finally {
                mainLock.unlock();
            }
        }
    }

    /**
     * Interrupts all threads, even if active. Ignores SecurityExceptions
     * (in which case some threads may remain uninterrupted).
     * 中断所有线程，即使线程是活跃的。忽略各种SecurityException
     * （在某些情况下，某些线程可能会保持不中断（不响应中断））
     *
     */
    private void interruptWorkers() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (Worker w : workers)
                w.interruptIfStarted();                      // 中断处于运行状态的worker线程
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Interrupts threads that might be waiting for tasks (as
     * indicated by not being locked) so they can check for
     * termination or configuration changes. Ignores
     * SecurityExceptions (in which case some threads may remain
     * uninterrupted).
     * 中断可能正在等待任务的线程（通过没有被lock来表明（如果该worker没有内部加锁，那么就判断为正在等待任务）），
     * 以便他们（至正在等待任务的worker）可以检查终止或者配置变化。
     * 忽略SecurityException（在某些情况下，某些线程可能会保持不中断（不响应中断））
     *
     * @param onlyOne If true, interrupt at most one worker. This is
     * called only from tryTerminate when termination is otherwise
     * enabled but there are still other workers.  In this case, at
     * most one waiting worker is interrupted to propagate shutdown
     * signals in case all threads are currently waiting.
     * Interrupting any arbitrary thread ensures that newly arriving
     * workers since shutdown began will also eventually exit.
     * To guarantee eventual termination, it suffices to always
     * interrupt only one idle worker, but shutdown() interrupts all
     * idle workers so that redundant workers exit promptly, not
     * waiting for a straggler task to finish.
     * 参数onlyOne如果为true，则最多中断一个worker。
     * 只会通过tryTerminate进行这种调用（当其他都可以终止但是仍有worker的时候）（就是除了有worker，其他条件都满足可以终止的时候，才会这样调用该方法）
     * 在这种情况下，所有worker线程当前都处于等待状态，最多一个等待worker被中断来广播shutdown信号。
     * 中断任意线程可以保证自shutdown开始以来，新到达的worker也将最终退出。
     * 为保证最终全部退出，每次只中断一个空闲worker就足够了，（通过多次调用来中断所有worker）
     * 但是shutdown()中断所有空闲线程，以便冗余（redundant）的worker立即（promptly）退出，而不是等待一个落后的（straggler）任务完成。
     * 
     */
    private void interruptIdleWorkers(boolean onlyOne) {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();                                    // 所有workers操作需要加锁mainLock
        try {
            for (Worker w : workers) {
                Thread t = w.thread;
                if (!t.isInterrupted() && w.tryLock()) {
                    try {
                        t.interrupt();
                    } catch (SecurityException ignore) {
                    } finally {
                        w.unlock();
                    }
                }
                if (onlyOne)
                    break;
            }
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Common form of interruptIdleWorkers, to avoid having to
     * remember what the boolean argument means.
     * interruptIdleWorkers的通用形式，避免需要记住入参的实际含义
     * （默认无入参的会中断所有空闲worker）
     *
     */
    private void interruptIdleWorkers() {
        interruptIdleWorkers(false);
    }

    // 用于interruptIdleWorkers的常量
    private static final boolean ONLY_ONE = true;

    /*
     * Misc utilities, most of which are also exported to
     * ScheduledThreadPoolExecutor
     * 其他方法，大多数暴露给ScheduledThreadPoolExecutor
     *
     */

    /**
     * Invokes the rejected execution handler for the given command.
     * Package-protected for use by ScheduledThreadPoolExecutor.
     * 对给定的命令调用拒绝执行处理器
     * protected权限，供ScheduledThreadPoolExecutor使用。
     *
     */
    final void reject(Runnable command) {
        handler.rejectedExecution(command, this);
    }

    /**
     * Performs any further cleanup following run state transition on
     * invocation of shutdown.  A no-op here, but used by
     * ScheduledThreadPoolExecutor to cancel delayed tasks.
     * 在调用shutdown运行状态转化后，执行进一步的清理。
     * 此处无操作，被用于ScheduledThreadPoolExecutor去取消延迟任务。
     * 
     */
    void onShutdown() {
    }

    /**
     * State check needed by ScheduledThreadPoolExecutor to
     * enable running tasks during shutdown.
     * 用于ScheduledThreadPoolExecutor进行状态检查，确保shutdown期间能够运行任务。
     *
     * @param shutdownOK true if should return true if SHUTDOWN
     */
    final boolean isRunningOrShutdown(boolean shutdownOK) {
        int rs = runStateOf(ctl.get());
        return rs == RUNNING || (rs == SHUTDOWN && shutdownOK);
    }

    /**
     * Drains the task queue into a new list, normally using
     * drainTo. But if the queue is a DelayQueue or any other kind of
     * queue for which poll or drainTo may fail to remove some
     * elements, it deletes them one by one.
     * 将任务队列转移到新的列表中，通常使用drainTo。
     * 但如果队列是DelayQueue或任何其他类型的queue，对于他们的poll或者drainTo可能无法移除某些元素，它会一个一个的删除他们
     * 
     */
    private List<Runnable> drainQueue() {
        BlockingQueue<Runnable> q = workQueue;
        ArrayList<Runnable> taskList = new ArrayList<Runnable>();
        q.drainTo(taskList);  // 从q中移除所有元素，并转移到给定的taskList里
        if (!q.isEmpty()) {
            for (Runnable r : q.toArray(new Runnable[0])) {
                if (q.remove(r))
                    taskList.add(r);
            }
        }
        return taskList;
    }

    /*
     * Methods for creating, running and cleaning up after workers
     * 用于创建、执行、清理worker的方法
     */

    /**
     * Checks if a new worker can be added with respect to current
     * pool state and the given bound (either core or maximum). If so,
     * the worker count is adjusted accordingly, and, if possible, a
     * new worker is created and started, running firstTask as its
     * first task. This method returns false if the pool is stopped or
     * eligible to shut down. It also returns false if the thread
     * factory fails to create a thread when asked.  If the thread
     * creation fails, either due to the thread factory returning
     * null, or due to an exception (typically OutOfMemoryError in
     * Thread.start()), we roll back cleanly.
     * 检查根据当前线程池状态和给定的边界（线程池的核心线程数与最大线程数），判断是否可以增加新的worker。
     * 如果可以，相应（accordingly）调整worker数量，并且，如何可能的话，创建新worker并启动，执行firstTask作为它的第一个任务。
     * 如果线程池已经停止或者有资格（eligible）关闭，该方法返回false。
     * 如果调用线程的工厂方法时创建线程失败，该方法返回false。
     * 如果该线程创建失败，可能有两种情况：
     * 1、由于线程工厂返回null
     * 2、由于异常（典型的如在Thread.start()时发生OOM）
     * 那我们会干净利落的进行回滚。
     *
     * @param firstTask the task the new thread should run first (or
     * null if none). Workers are created with an initial first task
     * (in method execute()) to bypass queuing when there are fewer
     * than corePoolSize threads (in which case we always start one),
     * or when the queue is full (in which case we must bypass queue).
     * Initially idle threads are usually created via
     * prestartCoreThread or to replace other dying workers.
     * firstTask参数，是新线程应该首先执行的任务（如果没有则为null）。
     * 有以下两种情况之一时，使用初始的第一个任务（通过execute()方法传入）创建新的Worker：
     * 1、当线程数少于corePoolSize（这种情况下总是启动一个新线程）
     * 2、或者当任务队列已满（这种情况下必须要绕过（bypass）queue）
     * 最初的空闲进程通常通过preStartCoreThread创建，或者替换其他将要挂掉的worker。
     * （上面说的这个替换，是指的原来将要退出的worker还没remove掉，就进行了wc+1，然后add新的worker了？）
     *
     * @param core if true use corePoolSize as bound, else
     * maximumPoolSize. (A boolean indicator is used here rather than a
     * value to ensure reads of fresh values after checking other pool
     * state).
     * core参数，如果为true则使用corePoolSize作为边界，否则使用maximumPoolSize。
     * （这里使用boolean指示符而不是具体的线程数value，是确保在检查完其他线程池状态后重新读取最新的线程数value） 
     *
     * @return true if successful
     */
    private boolean addWorker(Runnable firstTask, boolean core) {           // addWorker里增加workerCount与创建启动worker不是同步的
        retry:
        for (;;) {
            int c = ctl.get();
            int rs = runStateOf(c);

            // Check if queue empty only if necessary.                         // 根据线程池状态与工作队列，有的情况下是不允许创建新的worker的
            if (rs >= SHUTDOWN &&                                              // 1、runState为STOP、TIDYING、TERMINATED，此时线程池处于关闭状态，工作队列中已没有任务
                ! (rs == SHUTDOWN &&                                           // 2、runState为SHUTDOWN时，firstTask不为null（表示在SHUTDOWN状态提交的新任务，这种的直接拒绝）
                   firstTask == null &&                                        // 3、runState为SHUTDOWN时，工作队列为空（这种的也不能创建新worker）
                   ! workQueue.isEmpty()))                                     // （2、3）的判断是为了允许如果线程池处于SHUTDOWN状态，且工作队列不为空，那么允许创建firstTask为null的worker处理积压的工作队列任务
                return false;

            for (;;) {
                int wc = workerCountOf(c);
                if (wc >= CAPACITY ||
                    wc >= (core ? corePoolSize : maximumPoolSize))             // 判断当前workerCount是否大于边界（根据入参core判断corePoolSize或者maximumPoolSize）
                    return false;
                if (compareAndIncrementWorkerCount(c))                         // 如果增加workerCount成功，则跳出这两个for循环，进入该方法的新worker创建与启动部分
                    break retry;
                c = ctl.get();  // Re-read ctl
                if (runStateOf(c) != rs)                                       // 如果增加workerCount失败，并且runState发生了变化，那么重新判断是否允许创建新的worker（从第一个for循环开始分析）
                    continue retry;
                // else CAS failed due to workerCount change; retry inner loop // 如果增加workerCount失败，并且runState没有变化，那么可能是并发导致workerCount发生了变化，重新CAS来增加workerCount
            }
        }

        boolean workerStarted = false;
        boolean workerAdded = false;
        Worker w = null;
        try {
            w = new Worker(firstTask);                                        // 创建新的worker
            final Thread t = w.thread;                                        // 获取当前worker的线程t（这个线程t是用于执行该worker里面的任务的，这个线程t是通过线程工厂创建的）
            if (t != null) {                                                  // t != null，表示线程工厂成功创建线程。
                final ReentrantLock mainLock = this.mainLock;                 // 对于workers的操作都需要加mainLock锁
                mainLock.lock();
                try {
                    // Recheck while holding lock.
                    // Back out on ThreadFactory failure or if
                    // shut down before lock acquired.                        // 保持锁的状态下重新检查，在ThreadFactory失败或者在获取锁之前线程池shutdown，则退出？？？
                    int rs = runStateOf(ctl.get());

                    if (rs < SHUTDOWN ||
                        (rs == SHUTDOWN && firstTask == null)) {              // 重新判断线程池状态（runState），只有满足条件时才将新worker加入workers集合，并运行新的worker
                        if (t.isAlive()) // precheck that t is startable      // 不知道这个alive()检查的是啥
                            throw new IllegalThreadStateException();
                        workers.add(w);                                       // 将新worker加入的workers集合
                        int s = workers.size();
                        if (s > largestPoolSize)
                            largestPoolSize = s;                              // 如果当前workers集合数量大于largestPoolSize，那么更新largestPoolSize为当前值。这个值仅用于跟踪线程池达到的最大值
                        workerAdded = true;                                   // 新worker加入workers集合成功，允许新的worker启动
                    }
                } finally {
                    mainLock.unlock();
                }
                if (workerAdded) {
                    t.start();                                               // 执行t.start()，会调用worker.run()方法（具体见Thread.start()）
                    workerStarted = true;                                    // 到这一步表示worker启动没有异常（如果有OOM等异常，就走不到这一步了）
                }
            }
        } finally {
            if (! workerStarted)                                             // 如果新worker启动失败，那么需要从workers集合中删除该worker（当然也有可能worker就没加入到workers集合，不过没影响）
                addWorkerFailed(w);
        }
        return workerStarted;
    }

    /**
     * Rolls back the worker thread creation.
     * - removes worker from workers, if present
     * - decrements worker count
     * - rechecks for termination, in case the existence of this
     *   worker was holding up termination
     * 回滚worker线程的创建
     * 1、如果存在的话，从workers集合中移除该worker
     * 2、worker数量减一
     * 3、重新检查终止，以防止该worker的存在耽误了线程池终止。
     * 
     */
    private void addWorkerFailed(Worker w) {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();                 // 对workers的操作都要加mainLock锁
        try {
            if (w != null)
                workers.remove(w);       // 从workers集合中移除该worker（如果集合中没有该worker，会返回false）
            decrementWorkerCount();      // 如果走到了增加worker这一步（不管worker有没有创建加入成功），那workerCount必须减一
            tryTerminate();              // 尝试终止线程池，为的是避免由于该worker的存在阻碍了原本终止线程池操作
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Performs cleanup and bookkeeping for a dying worker. Called
     * only from worker threads. Unless completedAbruptly is set,
     * assumes that workerCount has already been adjusted to account
     * for exit.  This method removes thread from worker set, and
     * possibly terminates the pool or replaces the worker if either
     * it exited due to user task exception or if fewer than
     * corePoolSize workers are running or queue is non-empty but
     * there are no workers.
     * 清理将死worker并进行相关bookkeeping。（将死worker为即将退出worker序列的线程，不再执行提交给线程池的任务）
     * 只能由worker线程调用。
     * 除非设置了completedAbruptly（突然完成），否则假定workerCount已经调整为认定退出。（就是非completedAbruptly情况下，已经从workerCount中减去了该worker（wc-1））。
     * 该方法从worker集合中移除对应的线程，并尝试终止线程池或者替换该worker，替换worker的场景为：
     * 1、用户的任务异常导致该worker线程异常退出（此时completedAbruptly为true）
     * 2、运行的worker线程数小于corePoolSize
     * 3、工作队列非空但是没有存活的worker
     *
     * @param w the worker
     * @param completedAbruptly if the worker died due to user exception
     *        completedAbruptly参数，如果worker由于用户任务异常导致挂掉的情况下为true
     */
    private void processWorkerExit(Worker w, boolean completedAbruptly) {
        if (completedAbruptly) // If abrupt, then workerCount wasn't adjusted  // 如果突然中断，workerCount没有来得及调整
            decrementWorkerCount();                                            // wc - 1

        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();                                                       // 任何对worker集合的操作都要加mainLock锁
        try {
            completedTaskCount += w.completedTasks;                             // 更新已完成任务数，注意这里用的w.completedTasks。
            workers.remove(w);                                                  // 从worker集合中移除该worker
        } finally {
            mainLock.unlock();
        }

        tryTerminate();                                                         // 因为有worker退出，防止该worker的存在影响线程池正常终止，例行公事尝试终止线程池

        int c = ctl.get();
        if (runStateLessThan(c, STOP)) {                                        // 判断当前线程池运行状态是否<STOP
            if (!completedAbruptly) {                                           // 如果不是异常退出，那么需要分析当前线程是否小于核心线程出，或者工作队列有任务但是没alive worker
                int min = allowCoreThreadTimeOut ? 0 : corePoolSize;
                if (min == 0 && ! workQueue.isEmpty())
                    min = 1;
                if (workerCountOf(c) >= min)
                    return; // replacement not needed                           // 如果不满足上面的条件，就不用替换worker
            }
            addWorker(null, false);                                             // 如果是异常退出或者满足上面的条件，那么需要新增一个初始任务为null的worker来消费工作队列中的任务
        }
    }

    /**
     * Performs blocking or timed wait for a task, depending on
     * current configuration settings, or returns null if this worker
     * must exit because of any of:
     * 1. There are more than maximumPoolSize workers (due to
     *    a call to setMaximumPoolSize).
     * 2. The pool is stopped.
     * 3. The pool is shutdown and the queue is empty.
     * 4. This worker timed out waiting for a task, and timed-out
     *    workers are subject to termination (that is,
     *    {@code allowCoreThreadTimeOut || workerCount > corePoolSize})
     *    both before and after the timed wait, and if the queue is
     *    non-empty, this worker is not the last thread in the pool.
     * 阻塞或者限时等待获取任务，选取哪种方式取决于当前参数设置，
     * 如果该worker由于以下原因必须退出，则会返回null：
     * 1、当前worker数量超过maximumPoolSize（由于调用了setMaximumPoolSize重新设置了最大线程池数）
     * 2、线程池被停止（runState>=STOP）
     * 3、线程池被关闭（runState=SHUTDOWN），并且工作队列为空
     * 4、该worker等待任务超时，并且超时worker在限时等待之前与之后都会终止（即allowCoreThreadTimeOut || workerCount > corePoolSize），如果工作队列非空，那么该worker不会是线程池里最后一个线程。
     * （第4条这个，不明白注释说的啥，具体看代码吧）
     *
     * @return task, or null if the worker must exit, in which case
     *         workerCount is decremented
     */
    private Runnable getTask() {
        boolean timedOut = false; // Did the last poll() time out? // 最近poll是否超时（poll进行限时阻塞获取）

        for (;;) {
            int c = ctl.get();
            int rs = runStateOf(c);

            // Check if queue empty only if necessary.
            if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {  // 想不出来这个判断怎么写才会更简洁
                decrementWorkerCount();                                   // 这里decrementWorkerCount，是因为在线程池状态为关闭并且任务队列没任务时，可以清理所有worker（不用管并发下清理的worker太多）
                return null;                                             // wc-1之后直接返回null，让worker自己去执行退出
            }

            int wc = workerCountOf(c);

            // Are workers subject to culling?
            boolean timed = allowCoreThreadTimeOut || wc > corePoolSize; // 判断是否允许超时退出

            if ((wc > maximumPoolSize || (timed && timedOut))            // 当前wc超过最大线程池数或者允许超时退出情况下上次获取已超时
                && (wc > 1 || workQueue.isEmpty())) {                    // 还需注意工作队列非空时，最后一个worker线程不能退出
                if (compareAndDecrementWorkerCount(c))                  // 这里compareAndDecrementWorkerCount(c)，要拿当前的worker数-1，是防止并发下清理的worker过多（比如现在一共有俩worker，都等待超时了，任务队列还不为空，结果并发都走到了这一步，如果直接-1.这俩就都被清理了）
                    return null;
                continue;
            }

            try {
                Runnable r = timed ?
                    workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) :
                    workQueue.take();                                     // 如果worker可以超时退出，那么使用限时阻塞的poll方法，否则使用take方法
                if (r != null)
                    return r;
                timedOut = true;                                          // 如果限时拿不到任务，则记录上次获取已超时
            } catch (InterruptedException retry) {
                timedOut = false;                                         // 如果在从工作队列中拿任务时被中断退出，那么不算上次获取超时
            }
        }
    }

    /**
     * Main worker run loop.  Repeatedly gets tasks from queue and
     * executes them, while coping with a number of issues:
     * 主要的worker执行循环。
     * 重复从工作队列中获取任务然后执行他们，同时处理（cope）以下这些问题：
     *
     * 1. We may start out with an initial task, in which case we
     * don't need to get the first one. Otherwise, as long as pool is
     * running, we get tasks from getTask. If it returns null then the
     * worker exits due to changed pool state or configuration
     * parameters.  Other exits result from exception throws in
     * external code, in which case completedAbruptly holds, which
     * usually leads processWorkerExit to replace this thread.
     * 1、我们可以从一个初始任务开始执行，在这种情况下我们不需要获取第一个任务。
     * 否则，只要线程池在运行，我们就通过getTask方法获取任务。
     * 如果由于线程池状态改变或者参数配置发生变化，getTask返回null，那么该worker退出。
     * 其他退出是由于外部代码抛出异常引起的，在这种情况下completedAbruptly成立（满足completedAbruptly条件），
     * 这通常会导致processWorkerExit来替换该worker线程。
     *
     * 2. Before running any task, the lock is acquired to prevent
     * other pool interrupts while the task is executing, and then we
     * ensure that unless pool is stopping, this thread does not have
     * its interrupt set.
     * 2、在运行任何任务之前，要获取lock来防止运行任务时发生其他线程池中断，
     * 并且确保除非线程池被stopping，否则该线程不会设置中断。
     *
     * 3. Each task run is preceded by a call to beforeExecute, which
     * might throw an exception, in which case we cause thread to die
     * (breaking loop with completedAbruptly true) without processing
     * the task.
     * 3、每个任务执行之前都会调用beforeExecute方法，这可能会抛出异常，
     * 在这种情况下会导致线程挂掉（设置completedAbruptly为true然后跳出循环）而不处理任务。
     *
     * 4. Assuming beforeExecute completes normally, we run the task,
     * gathering any of its thrown exceptions to send to afterExecute.
     * We separately handle RuntimeException, Error (both of which the
     * specs guarantee that we trap) and arbitrary Throwables.
     * Because we cannot rethrow Throwables within Runnable.run, we
     * wrap them within Errors on the way out (to the thread's
     * UncaughtExceptionHandler).  Any thrown exception also
     * conservatively causes thread to die.
     * 4、假设beforeExecute正常完成，然后执行task，收集执行任务期间抛出的任何异常发送给afterExecute方法。
     * 分别处理RuntimeException、Error（规范（spec）保证我们可以捕获到这两个），和任意Throwables。
     * 由于在Runnable.run方法内部不能重新抛出Throwables。所以将Throwables包装到Errors中输出（到线程的UncaughtExceptionHandler）。
     * 任何抛出的异常也会保守的（conservatively）导致线程挂掉。
     *
     * 5. After task.run completes, we call afterExecute, which may
     * also throw an exception, which will also cause thread to
     * die. According to JLS Sec 14.20, this exception is the one that
     * will be in effect even if task.run throws.
     * 5、当task.run方法执行完成，调用afterExecute方法，可能也会抛出异常，也会导致该worker线程挂掉。
     * 根据JLS Sec 14.20，该异常即使是task.run抛出，也会生效。（？？？） 
     *
     * The net effect of the exception mechanics is that afterExecute
     * and the thread's UncaughtExceptionHandler have as accurate
     * information as we can provide about any problems encountered by
     * user code.
     * 这种异常机制的最终效果是：afterExecute与线程的UncaughtExceptionHandler，具有我们可以提供的关于用户代码遇到的任何问题的准确（accurate）信息。
     *
     * @param w the worker
     */
    final void runWorker(Worker w) {
        Thread wt = Thread.currentThread();                          // 因为是thread调用的worker#run，当前线程为worker线程，该线程也就是worker的thread，通过线程工厂创建出来的。
        Runnable task = w.firstTask;                                  // 拿到给定worker的firstTask，然后将worker的firstTask置为null
        w.firstTask = null;
        w.unlock(); // allow interrupts                              // unlock->AQS#release->tryRelease，state变为0，表示该worker进入运行阶段，允许interruptIfStarted。
        boolean completedAbruptly = true;                           // 突然完成的标识，用于判断是否要替换该worker
        try {
            while (task != null || (task = getTask()) != null) {    // 如果有需要执行的任务，就获取任务
                w.lock();                                             // 上来加锁，表示该worker在执行任务。防止被interruptIdleWorkers方法认为是空闲线程给中断了
                // If pool is stopping, ensure thread is interrupted; // 如果线程池被停止，确保线程被中断
                // if not, ensure thread is not interrupted.  This    // 如果线程池没有停止，确保线程没有被中断。
                // requires a recheck in second case to deal with     // 这需要在第二种情况下重新检查线程中断状态，以在清除中断的同时处理shutdownNow的竞争。
                // shutdownNow race while clearing interrupt
                if ((runStateAtLeast(ctl.get(), STOP) ||              // 这个if有两个功能：1、判断出该线程是否应该中断，runState>=STOP的就直接中断；2、如果该线程不应该中断，那么清理中断状态；
                     (Thread.interrupted() &&                         // 在满足runState<STOP的前提下，Thread.interrupted()会清理当前线程的中断状态，如果被中断了，该方法会返回true
                      runStateAtLeast(ctl.get(), STOP))) &&           // 如果worker线程发生过中断，那么再重新检查runState是否仍<STOP，如果是，则该if结束；如果不是的话，可能在if期间发生了shutdownNow，可能需要重新设置worker线程中断
                    !wt.isInterrupted())                              // 如果worker线程已经中断了，那么就不用重新设置中断状态了，否则就设置中断状态。wt.isInterrupted用于检查中断状态，不会清除中断状态。
                    wt.interrupt();                                   // 这里会执行，表示runState>=STOP，并且此时线程未设置中断状态，需要worker线程中断并退出
                try {
                    beforeExecute(wt, task);                          // 任务执行之前先调用beforeExecute
                    Throwable thrown = null;
                    try {
                        task.run();                                   // 执行任务。注意这里不会再开新线程来执行了，只是调用了Runnable的run方法（可能是RunnableFuture实现的Runnable）
                    } catch (RuntimeException x) {
                        thrown = x; throw x;
                    } catch (Error x) {
                        thrown = x; throw x;
                    } catch (Throwable x) {
                        thrown = x; throw new Error(x);
                    } finally {                                      // 注意：这一块异常try-catch-finally的逻辑，内部捕获的异常会走两部分，第一部分是继续throw抛出，第二部分是传入afterExecute。第一部分抛出的异常在外边的try给忽略了。
                        afterExecute(task, thrown);                   // 任务执行之后调用afterExecute，传入执行过程中捕获到的异常
                    }
                } finally {
                    task = null;
                    w.completedTasks++;                               // 完成任务数+1
                    w.unlock();                                       // 解锁，作为空闲线程等着了
                }
            }
            completedAbruptly = false;                               // 如果getTask返回了null，就表示该worker是准备正常退出了       
        } finally {                                                  // 注意：内部循环如果有抛出异常，直接忽略
            processWorkerExit(w, completedAbruptly);                  // 带着completedAbruptly去尝试本worker进行退出与终止线程池
        }
    }

    // Public constructors and methods
    // 公共构造函数和方法

    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters and default thread factory and rejected execution handler.
     * It may be more convenient to use one of the {@link Executors} factory
     * methods instead of this general purpose constructor.
     * 见调用
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     *
     * @param maximumPoolSize the maximum number of threads to allow in the
     *        pool
     *
     * @param keepAliveTime when the number of threads is greater than
     *        the core, this is the maximum time that excess idle threads
     *        will wait for new tasks before terminating.
     *
     * @param unit the time unit for the {@code keepAliveTime} argument
     *
     * @param workQueue the queue to use for holding tasks before they are
     *        executed.  This queue will hold only the {@code Runnable}
     *        tasks submitted by the {@code execute} method.
     *
     * @throws IllegalArgumentException if one of the following holds:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException if {@code workQueue} is null
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
             Executors.defaultThreadFactory(), defaultHandler);
    }

    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters and default rejected execution handler.
     * 同上
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the
     *        pool
     * @param keepAliveTime when the number of threads is greater than
     *        the core, this is the maximum time that excess idle threads
     *        will wait for new tasks before terminating.
     * @param unit the time unit for the {@code keepAliveTime} argument
     * @param workQueue the queue to use for holding tasks before they are
     *        executed.  This queue will hold only the {@code Runnable}
     *        tasks submitted by the {@code execute} method.
     * @param threadFactory the factory to use when the executor
     *        creates a new thread
     * @throws IllegalArgumentException if one of the following holds:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException if {@code workQueue}
     *         or {@code threadFactory} is null
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
             threadFactory, defaultHandler);
    }

    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters and default thread factory.
     * 同上
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the
     *        pool
     * @param keepAliveTime when the number of threads is greater than
     *        the core, this is the maximum time that excess idle threads
     *        will wait for new tasks before terminating.
     * @param unit the time unit for the {@code keepAliveTime} argument
     * @param workQueue the queue to use for holding tasks before they are
     *        executed.  This queue will hold only the {@code Runnable}
     *        tasks submitted by the {@code execute} method.
     * @param handler the handler to use when execution is blocked
     *        because the thread bounds and queue capacities are reached
     * @throws IllegalArgumentException if one of the following holds:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException if {@code workQueue}
     *         or {@code handler} is null
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              RejectedExecutionHandler handler) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
             Executors.defaultThreadFactory(), handler);
    }

    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters.
     * 创建新的ThreadPoolExecutor，通过给定的初始化参数和默认的线程工厂（ThreadFactory）和默认的拒绝执行处理器（RejectedExecutionHandler）。
     * 可能更方便的（convenient）是通过调用Executors的工厂方法，而不是此公共构造方法。（然而实际上代码风格检测通常更建议直接调用ThreadPoolExecutor的构造方法）
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     *        corePoolSize表示线程池保持的线程数，即使这些线程是空闲的，除非设置了allowCoreThreadTimeOut为true
     * @param maximumPoolSize the maximum number of threads to allow in the
     *        pool
     *        maximumPoolSize表示线程池允许的最大线程数
     * @param keepAliveTime when the number of threads is greater than
     *        the core, this is the maximum time that excess idle threads
     *        will wait for new tasks before terminating.
     *        keepAliveTime表示当线程数超过核心线程数时，多余的（excess）空闲线程在终止前用于等待新任务的最大等待时间
     * @param unit the time unit for the {@code keepAliveTime} argument
     *        unit表示keepAliveTime的时间单位
     * @param workQueue the queue to use for holding tasks before they are
     *        executed.  This queue will hold only the {@code Runnable}
     *        tasks submitted by the {@code execute} method.
     *        workQueue用于保存还未执行任务的队列。该队列将只保存通过execute方法提交的实现Runnable接口的任务。
     * @param threadFactory the factory to use when the executor
     *        creates a new thread
     *        threadFactor用于当executor创建新线程的时候
     * @param handler the handler to use when execution is blocked
     *        because the thread bounds and queue capacities are reached
     *        handler用于当线程池线程边界与工作队列容量饱和时导致执行阻塞，需要执行的相关拒绝策略
     * @throws IllegalArgumentException if one of the following holds:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>   // maximum不能<=0
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException if {@code workQueue}
     *         or {@code threadFactory} or {@code handler} is null
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory,
                              RejectedExecutionHandler handler) {
        if (corePoolSize < 0 ||
            maximumPoolSize <= 0 ||
            maximumPoolSize < corePoolSize ||
            keepAliveTime < 0)
            throw new IllegalArgumentException();
        if (workQueue == null || threadFactory == null || handler == null)
            throw new NullPointerException();
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.workQueue = workQueue;
        this.keepAliveTime = unit.toNanos(keepAliveTime); // 转化为纳秒
        this.threadFactory = threadFactory;
        this.handler = handler;
    }

    /**
     * Executes the given task sometime in the future.  The task
     * may execute in a new thread or in an existing pooled thread.
     * 在未来的某个时间执行给定的任务。
     * 该任务可能在一个新的线程或者一个已经存在的线程池线程中执行。
     *
     * If the task cannot be submitted for execution, either because this
     * executor has been shutdown or because its capacity has been reached,
     * the task is handled by the current {@code RejectedExecutionHandler}.
     * 如果任务不能提交执行，可能是由于执行器已经shutdown或者它的容量已经饱和，
     * 这种情况下该任务由当前RejectedExecutionHandler来处理。
     *
     * @param command the task to execute
     * @throws RejectedExecutionException at discretion of
     *         {@code RejectedExecutionHandler}, if the task
     *         cannot be accepted for execution
     * @throws NullPointerException if {@code command} is null
     */
    public void execute(Runnable command) {
        if (command == null)
            throw new NullPointerException();
        /*
         * Proceed in 3 steps:
         * 分三步进行
         *
         * 1. If fewer than corePoolSize threads are running, try to
         * start a new thread with the given command as its first
         * task.  The call to addWorker atomically checks runState and
         * workerCount, and so prevents false alarms that would add
         * threads when it shouldn't, by returning false.
         * 1、如果运行线程数<corePoolSize，尝试创建新的线程，并将给定的command作为它的第一个任务。
         * 调用addWorker将原子性的检查runState和workerCount，如果不应该创建新的worker线程，addWorker通过返回false以防止添加线程时的误报。
         *
         * 2. If a task can be successfully queued, then we still need
         * to double-check whether we should have added a thread
         * (because existing ones died since last checking) or that
         * the pool shut down since entry into this method. So we
         * recheck state and if necessary roll back the enqueuing if
         * stopped, or start a new thread if there are none.
         * 2、如果任务能够成功入队，我们仍需要再次检查我们是否应该添加一个线程（因为上次检查之后，有线程挂掉了）（如果线程池线程数不满足要求，那么需要创建新的线程）
         * 或者在进入该方法后线程池已shutdown。
         * 所以我们重新检查状态，有必要的话在停止时回滚入队操作，或者在没有线程的情况下启动一个新线程。
         *
         * 3. If we cannot queue task, then we try to add a new
         * thread.  If it fails, we know we are shut down or saturated
         * and so reject the task.
         * 3、如果无法将task入队，那么尝试添加一个新的线程。
         * 如果添加失败，则知道线程池已经shutdown或者饱和，因此需要拒绝该任务。
         *
         */
        int c = ctl.get();
        if (workerCountOf(c) < corePoolSize) {
            if (addWorker(command, true))                    // 1、如果当前线程数<corePoolSize，那么启动新worker线程，将该任务作为firstTask
                return;
            c = ctl.get();                                    // 2、如果添加新worker失败，重新查询当前线程池信息
        }
        if (isRunning(c) && workQueue.offer(command)) {       // 在线程池处于RUNNING的前提下，将该command入队（offer为非阻塞入队，失败返回false）
            int recheck = ctl.get();                          // 重新检查线程池状态（这个recheck感觉是个保障，确保提交的任务一定会被处理：包括有worker来执行，或者在线程池不处理任务队列的情况下移除该任务）
            if (! isRunning(recheck) && remove(command))      // 如果此时线程池处于关闭状态（>=SHUTDOWN），则移除该command
                reject(command);                              // 拒绝该任务（相当于作为新提交的任务来拒绝的，而SHUTDOWN状态还是会处理工作队列中的任务）
            else if (workerCountOf(recheck) == 0)            // 如果线程池还处于RUNNING时，worker线程为0，则需要添加worker线程来处理任务
                addWorker(null, false);
        }
        else if (!addWorker(command, false))                 // 3、如果线程数>corePoolSize，并且入队失败（工作队列满了），那么就新建worker线程处理该任务（创建corePoolSize到maximumPoolSize中间的worker线程）
            reject(command);                                  // 如果新建worker失败（比如当前rs>=SHUTDOWN），则拒绝该任务
    }

    /**
     * Initiates an orderly shutdown in which previously submitted
     * tasks are executed, but no new tasks will be accepted.
     * Invocation has no additional effect if already shut down.
     * 启动有序关闭，其中之前已提交的任务将执行，不会再接受新任务。
     * 如果已经处于SHUTDOWN，重复调用不会有额外的效果。
     *
     * <p>This method does not wait for previously submitted tasks to
     * complete execution.  Use {@link #awaitTermination awaitTermination}
     * to do that.
     * 该方法不会等待之前已提交的任务完成执行。（意思就是该方法会通知线程池去SHUTDOWN，但不会等待所有worker线程都完成退出与线程池终止）
     * 使用awaitTermination来做这些事儿（限时等待检测任务完成与线程池终止）。
     * 常见用法：
     * threadPoolExecutor.shutdown();
     * while(!threadPoolExecutor.awaitTermination(...)) { // 循环 限时等待worker线程完成退出与线程池状态成为TERMINATED};
     *
     * @throws SecurityException {@inheritDoc}
     *
     * 注意，该方法正常退出时仅表示已正确设置了线程池状态为SHUTDOWN，但不保证所有worker线程已完成，也不保证线程池状态到达TERMINATED状态
     *
     */
    public void shutdown() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();                                               // 涉及worker集合的操作加mainLock（因为要逐步清除worker线程）
        try {
            checkShutdownAccess();                                     // 检查shutdown权限
            advanceRunState(SHUTDOWN);                                 // 更新线程池状态为SHUTDOWN（如果rs<SHUTDOWN）
            interruptIdleWorkers();                                    // 中断所有*空闲*worker线程
            onShutdown(); // hook for ScheduledThreadPoolExecutor      // 调用onShutDown钩子方法，通常是针对ScheduledThreadPoolExecutor
        } finally {
            mainLock.unlock();                                         // 记得解锁
        }
        tryTerminate();                                                // 尝试终止线程池，如果不满足TERMINATED条件，会通过中断空闲线程来等待线程池自己终止
    }

    /**
     * Attempts to stop all actively executing tasks, halts the
     * processing of waiting tasks, and returns a list of the tasks
     * that were awaiting execution. These tasks are drained (removed)
     * from the task queue upon return from this method.
     * 尝试停止所有正在执行的任务，停止（halt）处理等待任务，返回等待执行的任务列表。
     * 从该方法返回时，将这些等待执行的任务从任务队列排出（移除）。
     *
     * <p>This method does not wait for actively executing tasks to
     * terminate.  Use {@link #awaitTermination awaitTermination} to
     * do that.
     * 该方法不会等待正在执行的任务执行完成。（意思就是该方法会通知线程池去STOP，但不会等待所有worker线程完成退出与线程池终止）
     * 使用awaitTermination来做到这一点。（用于限时等待检测线程池里的任务是否已执行完毕，线程池已关闭）
     *
     * <p>There are no guarantees beyond best-effort attempts to stop
     * processing actively executing tasks.  This implementation
     * cancels tasks via {@link Thread#interrupt}, so any task that
     * fails to respond to interrupts may never terminate.
     * 除了尽力尝试停止运行正在执行的任务外，没有任何保证。
     * 通过Thread#interrupt实现取消任务，所以任何未能响应中断的的任务可能永远不会终止。
     * 常见用法：
     * threadPoolExecutor.shutdown();
     * while(!threadPoolExecutor.awaitTermination(...)) { // 循环 限时等待worker线程完成退出与线程池状态成为TERMINATED};
     *
     * @throws SecurityException {@inheritDoc}
     *
     * 注意，该方法正常退出时仅表示已正确设置了线程池状态为SHUTDOWN，但不保证所有worker线程已完成，也不保证线程池状态到达TERMINATED状态
     *
     */
    public List<Runnable> shutdownNow() {
        List<Runnable> tasks;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();                                               // 涉及worker集合的操作加mainLock（因为要逐步清除worker线程）
        try {
            checkShutdownAccess();                                     // 检查shutdown权限
            advanceRunState(STOP);                                     // 更新线程池状态为STOP（如果rs<STOP）
            interruptWorkers();                                        // 中断所有线程（与shutdown只中断空闲线程不同）
            tasks = drainQueue();                                      // 将工作队列中等待任务排出，并生成到一个新的List中
        } finally {
            mainLock.unlock();
        }
        tryTerminate();                                                // 尝试终止线程池
        return tasks;                                                 // 返回工作队列中尚未执行的任务列表
    }

    // 判断线程池状态是否停止（rs>RUNNING）
    public boolean isShutdown() {
        return ! isRunning(ctl.get());
    }

    /**
     * Returns true if this executor is in the process of terminating
     * after {@link #shutdown} or {@link #shutdownNow} but has not
     * completely terminated.  This method may be useful for
     * debugging. A return of {@code true} reported a sufficient
     * period after shutdown may indicate that submitted tasks have
     * ignored or suppressed interruption, causing this executor not
     * to properly terminate.
     * 如果executor在shutdown或者shutdownNow之后正在终止但尚未完全终止，该方法返回true。
     * 该方法可能对debug有用。如果在shutdown之后足够（sufficient）长的时间里该方法仍返回true，
     * 可能表明提交的任务已经忽略或者抑制了中断（不响应中断），导致executor无法正常终止。
     *
     * @return {@code true} if terminating but not yet terminated
     *          正在终止但未完成终止，返回true
     */
    public boolean isTerminating() {
        int c = ctl.get();
        return ! isRunning(c) && runStateLessThan(c, TERMINATED);
    }

    // 检测线程池是否已终止
    public boolean isTerminated() {
        return runStateAtLeast(ctl.get(), TERMINATED);
    }

    // 可以在shutdown()与shutdonwNow()调用之后，调用该方法，等待检测线程池是否已关闭
    public boolean awaitTermination(long timeout, TimeUnit unit)
        throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();                                          // 通过加锁来防止其他对worker的操作（包括清理空闲/所有worker）
        try {
            for (;;) {
                if (runStateAtLeast(ctl.get(), TERMINATED))       // 如果线程池状态已到达TERMINATED，说明现在线程池已关闭（没有在运行的worker线程了），返回true
                    return true;
                if (nanos <= 0)
                    return false;
                nanos = termination.awaitNanos(nanos);            // 通过Condition来限时等待（如果线程池终止了，tryTerminate会通过调用termination.signalAll来唤醒）。
            }
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Invokes {@code shutdown} when this executor is no longer
     * referenced and it has no threads.
     * 当不再引用此executor并且没有线程时执行关闭
     *
     */
    protected void finalize() {
        shutdown();
    }

    /**
     * Sets the thread factory used to create new threads.
     * 设置线程工厂用于创建新线程
     *
     * @param threadFactory the new thread factory
     * @throws NullPointerException if threadFactory is null
     * @see #getThreadFactory
     */
    public void setThreadFactory(ThreadFactory threadFactory) {
        if (threadFactory == null)
            throw new NullPointerException();
        this.threadFactory = threadFactory;     // 通过volatile保持可见性
    }

    /**
     * Returns the thread factory used to create new threads.
     * 返回用于创建线程的线程工厂
     *
     * @return the current thread factory
     * @see #setThreadFactory(ThreadFactory)
     */
    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    /**
     * Sets a new handler for unexecutable tasks.
     * 设置新的handler用于无法执行的任务
     *
     * @param handler the new handler
     * @throws NullPointerException if handler is null
     * @see #getRejectedExecutionHandler
     */
    public void setRejectedExecutionHandler(RejectedExecutionHandler handler) {
        if (handler == null)
            throw new NullPointerException();
        this.handler = handler;              // 通过volatile保持可见性
    }

    /**
     * Returns the current handler for unexecutable tasks.
     * 返回当前用于处理无法执行任务的handler
     *
     * @return the current handler
     * @see #setRejectedExecutionHandler(RejectedExecutionHandler)
     */
    public RejectedExecutionHandler getRejectedExecutionHandler() {
        return handler;
    }

    /**
     * Sets the core number of threads.  This overrides any value set
     * in the constructor.  If the new value is smaller than the
     * current value, excess existing threads will be terminated when
     * they next become idle.  If larger, new threads will, if needed,
     * be started to execute any queued tasks.
     * 设置核心线程数。
     * 这会覆盖构造函数中设置的任意值。（就是不管构造函数中原值是否大于/小于/等于目标值，都会设置为目标值）
     * 如果新值<当前值（原值），多余的现有线程会在下一次空闲时终止。
     * 如果新值>当前值（原值），如果需要的话，将启动新线程来执行任意排队任务。
     *
     * @param corePoolSize the new core size
     * @throws IllegalArgumentException if {@code corePoolSize < 0} // 注意不能<0
     * @see #getCorePoolSize
     */
    public void setCorePoolSize(int corePoolSize) {
        if (corePoolSize < 0)                                         // 核心线程数不能<0
            throw new IllegalArgumentException();
        int delta = corePoolSize - this.corePoolSize;
        this.corePoolSize = corePoolSize;
        if (workerCountOf(ctl.get()) > corePoolSize)
            interruptIdleWorkers();                                   // 如果当前线程数>更新后的核心线程数，尝试中断空闲worker线程
        else if (delta > 0) {                                        // 如果更新后核心线程数>原值，需要考虑是否是需要是为了增加worker线程而特意设置了较大的核心线程数。
            // We don't really know how many new threads are "needed".
            // As a heuristic, prestart enough new workers (up to new
            // core size) to handle the current number of tasks in
            // queue, but stop if queue becomes empty while doing so.
            // 我们不知道需要启动多少新线程。
            // 作为一种启发式（heuristic）方法，预先启动足够多的新worker线程（最多达到新的核心线程数）来处理队列中的当前任务
            // 但如果在执行此操作过程中队列变空，则停止启动新的worker线程（原已启动的worker线程会继续存活（如果没有设置allowCoreThreadTimeOut））
            //
            int k = Math.min(delta, workQueue.size());              // 新启动的线程数取核心线程数与当前队列任务数中的最小值
            while (k-- > 0 && addWorker(null, true)) {             // 避免启的新线程太多，有不必要的浪费
                if (workQueue.isEmpty())                             // 如果任务队列为空了，那么不管当前worker线程数是否达到了核心线程数，都停止创建新的线程
                    break;
            }
        }
    }

    /**
     * Returns the core number of threads.
     * 返回核心线程数
     *
     * @return the core number of threads
     * @see #setCorePoolSize
     */
    public int getCorePoolSize() {
        return corePoolSize;
    }

    /**
     * Starts a core thread, causing it to idly wait for work. This
     * overrides the default policy of starting core threads only when
     * new tasks are executed. This method will return {@code false}
     * if all core threads have already been started.
     * 启动核心线程，让它空闲着等待任务。
     * 该方法覆盖了默认只有在执行新任务的时候才启动核心线程的策略（默认如果没任务就不启动线程）
     * 该方法会返回false，如果所有的核心线程在此之前都已经启动了（在进入该方法前，worker线程数已达到核心线程数）
     * （预启动一个核心线程，在任务提交之前准备好线程）
     *
     * @return {@code true} if a thread was started
     */
    public boolean prestartCoreThread() {
        return workerCountOf(ctl.get()) < corePoolSize &&
            addWorker(null, true);                         // 只启动一个核心线程
    }

    /**
     * Same as prestartCoreThread except arranges that at least one
     * thread is started even if corePoolSize is 0.
     * 与prestartCoreThread相同，安排至少一个线程启动，即使设置的corePoolSize为0
     * （保证线程池中至少有一个worker线程，即使设置了线程池的corePoolSize为0）
     *
     */
    void ensurePrestart() {
        int wc = workerCountOf(ctl.get());
        if (wc < corePoolSize)
            addWorker(null, true);
        else if (wc == 0)
            addWorker(null, false);
    }

    /**
     * Starts all core threads, causing them to idly wait for work. This
     * overrides the default policy of starting core threads only when
     * new tasks are executed.
     * 启动所有核心线程，让他们空闲等待任务到来。
     * 该方法覆盖了默认只有在执行新任务的时候才启动核心线程的策略（默认如果没任务就不启动线程）
     *
     * @return the number of threads started
     *         返回启动的线程数
     */
    public int prestartAllCoreThreads() {
        int n = 0;
        while (addWorker(null, true))     // 传给addWorker()方法中，true参数表示需要启动核心线程，如果当前worker线程数>corePoolSize，返回false
            ++n;
        return n;
    }

    /**
     * Returns true if this pool allows core threads to time out and
     * terminate if no tasks arrive within the keepAlive time, being
     * replaced if needed when new tasks arrive. When true, the same
     * keep-alive policy applying to non-core threads applies also to
     * core threads. When false (the default), core threads are never
     * terminated due to lack of incoming tasks.
     * 如果该线程池允许核心线程在keepAlive时间内没有任务到达时超时与终止，则返回true，
     * 并在新任务到达时根据需要进行替换。（不知道这个替换跟addWorker()的替换又有什么关系？？？）
     * 当返回true时，应用于非核心线程的keep-alive策略也会应用于核心线程。
     * 当返回false时（也就是allowCoreThreadTimeOut的默认值），核心线程永远不会由于缺失传入任务而终止。
     *
     * @return {@code true} if core threads are allowed to time out,
     *         else {@code false}
     *
     * @since 1.6
     */
    public boolean allowsCoreThreadTimeOut() {
        return allowCoreThreadTimeOut;
    }

    /**
     * Sets the policy governing whether core threads may time out and
     * terminate if no tasks arrive within the keep-alive time, being
     * replaced if needed when new tasks arrive. When false, core
     * threads are never terminated due to lack of incoming
     * tasks. When true, the same keep-alive policy applying to
     * non-core threads applies also to core threads. To avoid
     * continual thread replacement, the keep-alive time must be
     * greater than zero when setting {@code true}. This method
     * should in general be called before the pool is actively used.
     * 设置策略，主要用于控制该线程池核心线程在keepAlive时间内没有任务到达时超时与终止，
     * 并在新任务到达时根据需要进行替换。
     * 如果设置为false，核心线程永远不会由于缺失传入任务而终止。
     * 如果设置为true，应用于非核心线程的keep-alive策略也会应用于核心线程。
     * 为避免不断更换线程，当设置为true时，keep-alive的值必须大于0。
     * 通常在主动使用线程池之前调用此方法。
     *
     * @param value {@code true} if should time out, else {@code false}
     * @throws IllegalArgumentException if value is {@code true}
     *         and the current keep-alive time is not greater than zero
     *
     * @since 1.6
     */
    public void allowCoreThreadTimeOut(boolean value) {
        if (value && keepAliveTime <= 0)
            throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
        if (value != allowCoreThreadTimeOut) {     // 如果新值不等于原来的allowCoreThreadTimeOut，那么就设置新值
            allowCoreThreadTimeOut = value;
            if (value)
                interruptIdleWorkers();            // 如果原来不是true，现在为true了，则中断空闲的线程
        }
    }

    /**
     * Sets the maximum allowed number of threads. This overrides any
     * value set in the constructor. If the new value is smaller than
     * the current value, excess existing threads will be
     * terminated when they next become idle.
     * 设置允许的最大线程数。
     * 该方法覆盖构造方法中设置的任意值。
     * 如果新值<当前值（原值），存在的多余（excess）线程将在他们下一次空闲时被终止
     *
     * @param maximumPoolSize the new maximum
     * @throws IllegalArgumentException if the new maximum is
     *         less than or equal to zero, or
     *         less than the {@linkplain #getCorePoolSize core pool size}
     *         要保证maximumPoolSize>=corePoolSize>0
     * @see #getMaximumPoolSize
     */
    public void setMaximumPoolSize(int maximumPoolSize) {
        if (maximumPoolSize <= 0 || maximumPoolSize < corePoolSize)
            throw new IllegalArgumentException();
        this.maximumPoolSize = maximumPoolSize;
        if (workerCountOf(ctl.get()) > maximumPoolSize) // 如果当前worker线程数>新值maximumPoolSize，需要将空闲线程终止
            interruptIdleWorkers();
    }

    /**
     * Returns the maximum allowed number of threads.
     * 返回允许的最大线程数
     *
     * @return the maximum allowed number of threads
     * @see #setMaximumPoolSize
     */
    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    /**
     * Sets the time limit for which threads may remain idle before
     * being terminated.  If there are more than the core number of
     * threads currently in the pool, after waiting this amount of
     * time without processing a task, excess threads will be
     * terminated.  This overrides any value set in the constructor.
     * 设置线程在终止前可以保持空闲的时间限制。（空闲多少时长后终止）
     * 如果在线程池中的当前线程数超过核心线程数，并在这段等待时间内没有执行任务，多余的线程将被终止。
     * 这会覆盖构造函数中设置的任意值。
     *
     * @param time the time to wait.  A time value of zero will cause
     *        excess threads to terminate immediately after executing tasks.
     *        time参数，等待时间。如果time值为0，将导致执行额外的线程（超过核心线程的那部分线程）在执行完任务后会立即终止
     *
     * @param unit the time unit of the {@code time} argument
     * @throws IllegalArgumentException if {@code time} less than zero or
     *         if {@code time} is zero and {@code allowsCoreThreadTimeOut}
     *         注意，必须保证time>=0，
     *               如果设置了allowCoreThreadTimeOut，必须保证time>0（要不然留不住核心线程）
     * @see #getKeepAliveTime(TimeUnit)
     */
    public void setKeepAliveTime(long time, TimeUnit unit) {
        if (time < 0)
            throw new IllegalArgumentException();
        if (time == 0 && allowsCoreThreadTimeOut())
            throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
        long keepAliveTime = unit.toNanos(time);
        long delta = keepAliveTime - this.keepAliveTime;
        this.keepAliveTime = keepAliveTime;
        if (delta < 0)               // 如果keepAliveTime时间调小，则需要尝试终止已超时的空闲线程
            interruptIdleWorkers();
    }

    /**
     * Returns the thread keep-alive time, which is the amount of time
     * that threads in excess of the core pool size may remain
     * idle before being terminated.
     * 返回线程keep-alive时长，这是超过线程池核心线程数的线程在终止之前可能保持空闲的时长。
     *
     * @param unit the desired time unit of the result
     * @return the time limit
     * @see #setKeepAliveTime(long, TimeUnit)
     */
    public long getKeepAliveTime(TimeUnit unit) {
        return unit.convert(keepAliveTime, TimeUnit.NANOSECONDS); // 转化为入参的时间单位返回
    }

    /* User-level queue utilities */
    // 用户级的队列操作

    /**
     * Returns the task queue used by this executor. Access to the
     * task queue is intended primarily for debugging and monitoring.
     * This queue may be in active use.  Retrieving the task queue
     * does not prevent queued tasks from executing.
     * 返回线程池使用的任务队列。
     * 访问任务队列主要用于debug与监控。
     * 该队列可能正在使用（动态变化）
     * 遍历任务队列不会阻止入队的任务执行。
     *
     * @return the task queue
     */
    public BlockingQueue<Runnable> getQueue() {
        return workQueue;
    }

    /**
     * Removes this task from the executor's internal queue if it is
     * present, thus causing it not to be run if it has not already
     * started.
     * 如果该任务存在（present），从executor的内部队列中移除该任务，这会导致尚未启动的任务不会去运行。
     *
     * <p>This method may be useful as one part of a cancellation
     * scheme.  It may fail to remove tasks that have been converted
     * into other forms before being placed on the internal queue. For
     * example, a task entered using {@code submit} might be
     * converted into a form that maintains {@code Future} status.
     * However, in such cases, method {@link #purge} may be used to
     * remove those Futures that have been cancelled.
     * 该方法可作为取消方案的一部分。
     * 它可能无法删除那些在放入内部队列之前已转化为其他形式的任务。
     * 例如，使用submit方法入队的任务可能转化为了含有Future状态的形式。
     * （例如Runnable通过AbstractExecutorService#submit()转成了FutureTask对象（这个过程叫形式转化），如果还用Runnable对象来remove，是删除不了的）
     * 但是，在这种情况下，purge方法可用于删除这部分已经取消的Future。
     *
     * @param task the task to remove
     * @return {@code true} if the task was removed
     */
    public boolean remove(Runnable task) {
        boolean removed = workQueue.remove(task);
        tryTerminate(); // In case SHUTDOWN and now empty // 对于rs=SHUTDOWN并且任务队列非空的情况下，删除了一个任务队列任务，需要尝试终止线程池。
        return removed;
    }

    /**
     * Tries to remove from the work queue all {@link Future}
     * tasks that have been cancelled. This method can be useful as a
     * storage reclamation operation, that has no other impact on
     * functionality. Cancelled tasks are never executed, but may
     * accumulate in work queues until worker threads can actively
     * remove them. Invoking this method instead tries to remove them now.
     * However, this method may fail to remove tasks in
     * the presence of interference by other threads.
     * 尝试从工作队列中删除所有已取消的Future任务。
     * 该方法可用作存储（storage）回收（reclamation）操作，对功能没有其他影响。
     * 取消的任务永远不会执行，但可能会在工作队列中累积，直到worker线程主动删除他们。（worker会调用任务的run方法，在FutureTask中，如果run的Callable的state不为NEW，则直接结束）
     * 现在调用该方法会尝试删除他们。
     * 然而，这种方法可能会在（presence 存在）其他线程干扰（interference）下无法删除任务。
     *
     */
    public void purge() {
        final BlockingQueue<Runnable> q = workQueue;
        try {
            Iterator<Runnable> it = q.iterator();
            while (it.hasNext()) {
                Runnable r = it.next();
                if (r instanceof Future<?> && ((Future<?>)r).isCancelled()) // 判断任务是否实现了Future接口，并判断该Future任务是否已取消
                    it.remove();
            }
        } catch (ConcurrentModificationException fallThrough) {
            // Take slow path if we encounter interference during traversal.
            // Make copy for traversal and call remove for cancelled entries.
            // The slow path is more likely to be O(N*N).
            // 发生并发修改异常
            // 如果在遍历过程中遇到干扰，采取slow path。
            // 为遍历创建副本，并调用remove来取消entries（就是遍历对象）
            // slow path的时间复杂度很大的可能为O(N*N)
            // 
            for (Object r : q.toArray())
                if (r instanceof Future<?> && ((Future<?>)r).isCancelled())
                    q.remove(r);
        }

        tryTerminate(); // In case SHUTDOWN and now empty // 对于rs=SHUTDOWN并且任务队列非空的情况下，删除了一个任务队列任务，需要尝试终止线程池。
    }

    /* Statistics */
    // 统计数据

    /**
     * Returns the current number of threads in the pool.
     * 返回当前线程池线程数
     *
     * @return the number of threads
     */
    public int getPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();                                       // 对于worker集合的操作都要加mainLock
        try {
            // Remove rare and surprising possibility of
            // isTerminated() && getPoolSize() > 0
            // 一般来说，当执行到isTerminated()方法时，线程池里应该是没有worker线程了
            // 但是为了防止罕见与惊讶的可能性发生，对这种情况进行单独判断
            return runStateAtLeast(ctl.get(), TIDYING) ? 0 // 如果rs=TIDYING，直接返回0，不再去检查worker集合的数量
                : workers.size();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns the approximate number of threads that are actively
     * executing tasks.
     * 返回正在执行任务的线程大致（approximate）数量。（不包含空闲的线程）
     *
     * @return the number of threads
     */
    public int getActiveCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();                // 对worker集合的操作都需要加mainLock
        try {
            int n = 0;
            for (Worker w : workers)
                if (w.isLocked())       // 判断线程是否正在执行任务的依据是看该worker是否有加锁
                    ++n;
            return n;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns the largest number of threads that have ever
     * simultaneously been in the pool.
     * 返回同时（simultaneously）进入线程池的历史最大线程数。
     *
     * @return the number of threads
     */
    public int getLargestPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();            // 对worker集合的操作都需要加mainLock，因为largestPoolSize依赖与workers.size()，所以也加了锁
        try {
            return largestPoolSize;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns the approximate total number of tasks that have ever been
     * scheduled for execution. Because the states of tasks and
     * threads may change dynamically during computation, the returned
     * value is only an approximation.
     * 返回已安排执行的所有任务的预估值。（包含已执行完成、正在执行、入队等待执行的所有任务数）
     * 由于在计算过程中任务状态与线程状态可能动态变化，因此返回值只是一个近似值。
     *
     * @return the number of tasks
     */
    public long getTaskCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();                    // 对worker集合的操作都需要加mainLock
        try {
            long n = completedTaskCount;
            for (Worker w : workers) {
                n += w.completedTasks;      // 任务数累加已执行完成任务数
                if (w.isLocked())
                    ++n;                    // 任务数累加当前worker正在执行任务
            }
            return n + workQueue.size();    // 任务数累加工作队列中尚未执行的任务数
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns the approximate total number of tasks that have
     * completed execution. Because the states of tasks and threads
     * may change dynamically during computation, the returned value
     * is only an approximation, but one that does not ever decrease
     * across successive calls.
     * 返回已执行完成的所有数的预估值。
     * 由于在计算过程中任务状态与线程状态可能动态变化，因此返回值只是一个近似值，
     * 但是在连续调用本方法时该值永远不会减少。
     *
     * @return the number of tasks
     */
    public long getCompletedTaskCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();                    // 对worker集合的操作都需要加mainLock
        try {
            long n = completedTaskCount;
            for (Worker w : workers)
                n += w.completedTasks;     // 任务数累加已执行完成任务数
            return n;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns a string identifying this pool, as well as its state,
     * including indications of run state and estimated worker and
     * task counts.
     * 返回标识此线程池及其状态的字符串，包括运行状态、预估worker数量、任务数量
     *
     * @return a string identifying this pool, as well as its state
     */
    public String toString() {
        long ncompleted;        // 完成任务数
        int nworkers, nactive;  // worker线程数、活跃worker线程数（活跃=非空闲）
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            ncompleted = completedTaskCount;
            nactive = 0;
            nworkers = workers.size();
            for (Worker w : workers) {
                ncompleted += w.completedTasks;
                if (w.isLocked())
                    ++nactive;
            }
        } finally {
            mainLock.unlock();
        }
        int c = ctl.get();
        String rs = (runStateLessThan(c, SHUTDOWN) ? "Running" :
                     (runStateAtLeast(c, TERMINATED) ? "Terminated" :
                      "Shutting down"));
        return super.toString() +
            "[" + rs +                                  // 运行状态
            ", pool size = " + nworkers +               // 线程池大小=worker线程数
            ", active threads = " + nactive +           // 活跃线程数=非空闲线程数
            ", queued tasks = " + workQueue.size() +    // 排队等待任务数=工作队列大小
            ", completed tasks = " + ncompleted +       // 完成任务数
            "]";
    }

    /* Extension hooks */
    // 扩展的钩子方法

    /**
     * Method invoked prior to executing the given Runnable in the
     * given thread.  This method is invoked by thread {@code t} that
     * will execute task {@code r}, and may be used to re-initialize
     * ThreadLocals, or to perform logging.
     * 在给定的线程中执行给定的Runnable任务时，首先执行该方法。
     * 在线程t将要执行任务r的时候调用该方法，可用于重新初始化ThreadLocals，或者记录日志。
     *
     * <p>This implementation does nothing, but may be customized in
     * subclasses. Note: To properly nest multiple overridings, subclasses
     * should generally invoke {@code super.beforeExecute} at the end of
     * this method.
     * 本类该方法没有具体实现，但在子类中可能会自定义实现
     * 注意：为了正确的嵌套覆盖，子类通常应该在该方法的末尾调用super.beforeExecute
     *
     * @param t the thread that will run task {@code r}
     * @param r the task that will be executed
     */
    protected void beforeExecute(Thread t, Runnable r) { }

    /**
     * Method invoked upon completion of execution of the given Runnable.
     * This method is invoked by the thread that executed the task. If
     * non-null, the Throwable is the uncaught {@code RuntimeException}
     * or {@code Error} that caused execution to terminate abruptly.
     * 在执行完给定的Runnable任务后调用该方法。
     * 由执行任务的线程调用该方法。
     * 如果Throwable非空，表示导致执行突然中断并且未被捕获的RunntimeException或Error
     * （这个未被捕获，表示未被Runnable内部catch，但被runWorker给catch到了，再通过调用afterExecute进行处理）
     *
     * <p>This implementation does nothing, but may be customized in
     * subclasses. Note: To properly nest multiple overridings, subclasses
     * should generally invoke {@code super.afterExecute} at the
     * beginning of this method.
     * 本类该方法没有具体实现，但在子类中可能会自定义实现
     * 注意：为了正确的嵌套覆盖，子类通常应该在该方法前面调用super.afterExecute（跟beforeExecute顺序相反）
     *
     * <p><b>Note:</b> When actions are enclosed in tasks (such as
     * {@link FutureTask}) either explicitly or via methods such as
     * {@code submit}, these task objects catch and maintain
     * computational exceptions, and so they do not cause abrupt
     * termination, and the internal exceptions are <em>not</em>
     * passed to this method. If you would like to trap both kinds of
     * failures in this method, you can further probe for such cases,
     * as in this sample subclass that prints either the direct cause
     * or the underlying exception if a task has been aborted:
     * 注意：当动作被明确的或者通过submit方法包含到任务中（例如FutureTask），该任务对象捕获和维护计算异常，
     * 所以它不会导致突然终止，并且内部的异常不会传递给此方法。
     * （回顾一下FutureTask的run方法，该方法执行时不会抛出异常，而是将异常给set进了outcome里，只有通过get方法拿执行结果outcome时，才能知道运行结果是否有异常）
     * （这个不会传递意思是，这部分异常不会通过Throwable t入参传递，而是task内部的异常。task内部维护的异常不会抛出，在runWorker中也捕获不到）
     * 如果想要在该方法中捕获这两种失败，可以进一步探测此种情况，
     * 例如下面的样例子类里，如果任务已中止（abort），将打印直接原因或者底层异常。
     *
     *  <pre> {@code
     * class ExtendedExecutor extends ThreadPoolExecutor {
     *   // ...
     *   protected void afterExecute(Runnable r, Throwable t) {
     *     super.afterExecute(r, t);                                 // 先调用父类afterExecute
     *     if (t == null && r instanceof Future<?>) {                // 如果runWorker捕获到的异常为null，并且Runnable r为Future类型（存在异常由Future维护，而没有throw给runWorker）
     *       try {
     *         Object result = ((Future<?>) r).get();                // 调用Future的get时，如果执行过程中发生异常，那么该方法会直接抛出由它维护的相关异常
     *       } catch (CancellationException ce) {                    // 捕获Future维护的异常
     *           t = ce;
     *       } catch (ExecutionException ee) {
     *           t = ee.getCause();
     *       } catch (InterruptedException ie) {
     *           Thread.currentThread().interrupt(); // ignore/reset  // 捕获到中断异常，重新设置中断
     *       }
     *     }
     *     if (t != null)
     *       System.out.println(t);
     *   }
     * }}</pre>
     *
     * @param r the runnable that has completed
     * @param t the exception that caused termination, or null if
     * execution completed normally
     */
    protected void afterExecute(Runnable r, Throwable t) { }

    /**
     * Method invoked when the Executor has terminated.  Default
     * implementation does nothing. Note: To properly nest multiple
     * overridings, subclasses should generally invoke
     * {@code super.terminated} within this method.
     * 当executor终止时调用该方法。
     * 默认本类该方法没有具体实现。
     * 注意：为了正确的嵌套(nest 巢 mutiple 多个，嵌套多个)覆盖，子类通常应该在该方法中调用super.terminated。
     * 
     */
    protected void terminated() { }

    /* Predefined RejectedExecutionHandlers */
    /* 预定义的RejectedExecutionHandlers */

    /**
     * A handler for rejected tasks that runs the rejected task
     * directly in the calling thread of the {@code execute} method,
     * unless the executor has been shut down, in which case the task
     * is discarded.
     * 处理拒绝任务的handler，将直接在execute方法的调用线程中执行被拒绝的任务。
     * 除非executor已经关闭，在这种情况下该任务将被丢弃。
     *
     */
    public static class CallerRunsPolicy implements RejectedExecutionHandler {
        /**
         * Creates a {@code CallerRunsPolicy}.
         */
        public CallerRunsPolicy() { }

        /**
         * Executes task r in the caller's thread, unless the executor
         * has been shut down, in which case the task is discarded.
         * 在caller线程中执行任务r，除非executor已经关闭，这种情况下该任务将被抛弃。
         *
         * @param r the runnable task requested to be executed    r是需要执行的任务
         * @param e the executor attempting to execute this task  e是尝试执行该任务的executor（用于判断该executor线程池是否被关闭）
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            if (!e.isShutdown()) {
                r.run();
            }
        }
    }

    /**
     * A handler for rejected tasks that throws a
     * {@code RejectedExecutionException}.
     * 处理拒绝任务的handler，抛出RejectedExecutionException
     *
     */
    public static class AbortPolicy implements RejectedExecutionHandler {
        /**
         * Creates an {@code AbortPolicy}.
         */
        public AbortPolicy() { }

        /**
         * Always throws RejectedExecutionException.
         * 总是抛出RejectedExecutionException
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         * @throws RejectedExecutionException always
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            throw new RejectedExecutionException("Task " + r.toString() +
                                                 " rejected from " +
                                                 e.toString());
        }
    }

    /**
     * A handler for rejected tasks that silently discards the
     * rejected task.
     * 处理拒绝任务的handler，静默抛弃拒绝的任务
     *
     */
    public static class DiscardPolicy implements RejectedExecutionHandler {
        /**
         * Creates a {@code DiscardPolicy}.
         */
        public DiscardPolicy() { }

        /**
         * Does nothing, which has the effect of discarding task r.
         * 啥也不做，就会静默丢弃任务r
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        }
    }

    /**
     * A handler for rejected tasks that discards the oldest unhandled
     * request and then retries {@code execute}, unless the executor
     * is shut down, in which case the task is discarded.
     * 处理拒绝任务的handler，丢弃最早未处理的请求（任务），然后对当前拒绝的任务重试execute，
     * 除非executor关闭，这种情况下该任务将被抛弃。
     * 
     */
    public static class DiscardOldestPolicy implements RejectedExecutionHandler {
        /**
         * Creates a {@code DiscardOldestPolicy} for the given executor.
         */
        public DiscardOldestPolicy() { }

        /**
         * Obtains and ignores the next task that the executor
         * would otherwise execute, if one is immediately available,
         * and then retries execution of task r, unless the executor
         * is shut down, in which case task r is instead discarded.
         * 获取并忽略executor将要执行的下一个任务，
         * 如果一个任务立即可用，那么重试任务r的执行，
         * 除非executor关闭，这种情况下该任务r将被抛弃。
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            if (!e.isShutdown()) {
                e.getQueue().poll(); // poll 非阻塞，没有返回null
                e.execute(r);
            }
        }
    }
}
