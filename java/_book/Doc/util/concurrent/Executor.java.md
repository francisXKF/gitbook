# Executor
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

/**
 * An object that executes submitted {@link Runnable} tasks. This
 * interface provides a way of decoupling task submission from the
 * mechanics of how each task will be run, including details of thread
 * use, scheduling, etc.  An {@code Executor} is normally used
 * instead of explicitly creating threads. For example, rather than
 * invoking {@code new Thread(new(RunnableTask())).start()} for each
 * of a set of tasks, you might use:
 * 一个执行提交的Runnable任务的对象。
 * 该接口提供了一种将任务提交与每个任务将如何运行的机制分离的方法，包含线程使用、调度等细节。
 * Executor通常用于替代显式（explicitly）创建线程。
 * 例如：不想为了task集合中的的每一个都调用new Thread(new(RunnableTask())).start()，可以使用下面的方法：
 *
 * <pre>
 * Executor executor = <em>anExecutor</em>;
 * executor.execute(new RunnableTask1());
 * executor.execute(new RunnableTask2());
 * ...
 * </pre>
 *
 * However, the {@code Executor} interface does not strictly
 * require that execution be asynchronous. In the simplest case, an
 * executor can run the submitted task immediately in the caller's
 * thread:
 * 但是，Executor接口并不严格（strictly）要求执行是异步的。
 * 举一个简单的例子：executor可以在调用者的线程里立即执行task：
 *
 *  <pre> {@code
 * class DirectExecutor implements Executor {
 *   public void execute(Runnable r) {
 *     r.run(); // 没有通过传入线程对象执行，那么run就作为普通方法执行了
 *   }
 * }}</pre>
 *
 * More typically, tasks are executed in some thread other
 * than the caller's thread.  The executor below spawns a new thread
 * for each task.
 * 通常情况，task在调用者线程之外，启动线程执行。
 * 下面的执行程序为每个任务生成一个新线程：
 *
 *  <pre> {@code
 * class ThreadPerTaskExecutor implements Executor {
 *   public void execute(Runnable r) {
 *     new Thread(r).start(); // 每个run方法都会在不同的Thread中执行
 *   }
 * }}</pre>
 *
 * Many {@code Executor} implementations impose some sort of
 * limitation on how and when tasks are scheduled.  The executor below
 * serializes the submission of tasks to a second executor,
 * illustrating a composite executor.
 * 许多Executor的实现，对task调度的方式和时间施加（impose 强加）了一些限制。
 * 下面的executor将任务的提交序列化到了第二个executor，说明（illustrating）了一个复合（composite）executor
 *
 *  <pre> {@code
 * class SerialExecutor implements Executor {
 *   final Queue<Runnable> tasks = new ArrayDeque<Runnable>();
 *   final Executor executor; // 内部有一个Executor
 *   Runnable active;
 *
 *   SerialExecutor(Executor executor) {
 *     this.executor = executor;
 *   }
 *
 *   public synchronized void execute(final Runnable r) {
 *     tasks.offer(new Runnable() {
 *       public void run() {
 *         try {
 *           r.run();
 *         } finally {
 *           scheduleNext();
 *         }
 *       }
 *     });
 *     if (active == null) {
 *       scheduleNext();
 *     }
 *   }
 *
 *   protected synchronized void scheduleNext() {
 *     if ((active = tasks.poll()) != null) {
 *       executor.execute(active); // 本类的execute方法执行完后，继续执行下一个executor的execute
 *     }
 *   }
 * }}</pre>
 *
 * The {@code Executor} implementations provided in this package
 * implement {@link ExecutorService}, which is a more extensive
 * interface.  The {@link ThreadPoolExecutor} class provides an
 * extensible thread pool implementation. The {@link Executors} class
 * provides convenient factory methods for these Executors.
 * Executor在包里提供了一个ExecutorService实现类，该类是一个更广泛的接口。
 * ThreadPoolExecutor类提供了一个可扩展的线程池实现。
 * Executors类为这些Executor提供了方便的（convenient）的工厂方法。
 *
 * <p>Memory consistency effects: Actions in a thread prior to
 * submitting a {@code Runnable} object to an {@code Executor}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * its execution begins, perhaps in another thread.
 * 内存一致性影响：在线程提交Runnable对象给Executor之前，内存可见性happen-before执行之前，可能在其它线程（不知道啥意思）
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface Executor {

    /**
     * Executes the given command at some time in the future.  The command
     * may execute in a new thread, in a pooled thread, or in the calling
     * thread, at the discretion of the {@code Executor} implementation.
     * 执行给定的命令（实现Runnable接口的）在未来的某个时间。
     * 该命令可能在一个新的线程中执行，或者在一个线程池执行，或者在调用者的线程里执行，执行方式由Executor的实现来决定。
     *
     * @param command the runnable task
     * @throws RejectedExecutionException if this task cannot be
     * accepted for execution
     * @throws NullPointerException if command is null
     */
    void execute(Runnable command);
}
```