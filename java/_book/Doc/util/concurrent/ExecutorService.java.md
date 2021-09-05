# ExecutorService
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
import java.util.List;
import java.util.Collection;

/**
 * An {@link Executor} that provides methods to manage termination and
 * methods that can produce a {@link Future} for tracking progress of
 * one or more asynchronous tasks.
 * 一个Executor提供管理终止的方法，并且可以生成Future来跟踪一个或多个异步任务的执行进度的方法。
 *
 * <p>An {@code ExecutorService} can be shut down, which will cause
 * it to reject new tasks.  Two different methods are provided for
 * shutting down an {@code ExecutorService}. The {@link #shutdown}
 * method will allow previously submitted tasks to execute before
 * terminating, while the {@link #shutdownNow} method prevents waiting
 * tasks from starting and attempts to stop currently executing tasks.
 * Upon termination, an executor has no tasks actively executing, no
 * tasks awaiting execution, and no new tasks can be submitted.  An
 * unused {@code ExecutorService} should be shut down to allow
 * reclamation of its resources.
 * 可以关闭（shut down）ExecutorService，将它导致拒绝新任务。
 * 提供了两种不同的方法来shutdownExecutorService。
 * 1、shutdown方法，将允许之前提交的任务在终止之前继续执行。
 * 2、shutdownNow方法，防止（prevent）等待任务开始，并且尝试结束当前正在执行的任务。
 * 终止时，executor没有正在执行的任务，没有等待执行的任务，没有新任务可以提交。
 * 应关闭未使用的ExecutorService，以回收资源。
 *
 * <p>Method {@code submit} extends base method {@link
 * Executor#execute(Runnable)} by creating and returning a {@link Future}
 * that can be used to cancel execution and/or wait for completion.
 * Methods {@code invokeAny} and {@code invokeAll} perform the most
 * commonly useful forms of bulk execution, executing a collection of
 * tasks and then waiting for at least one, or all, to
 * complete. (Class {@link ExecutorCompletionService} can be used to
 * write customized variants of these methods.)
 * submit方法基于Executor#execute(Runnable)方法扩展，通过创建和返回Future对象，Future可以用来取消执行的任务，并且/或等待任务完成。
 * invokeAny方法和invokeAll方法最常用于批量（bulk）任务执行，执行一组任务，然后等待至少一个或者所有执行完成。
 *（ExecutorCompletionService类可用于编写这些方法的自定义变体（variants 变种））
 *
 * <p>The {@link Executors} class provides factory methods for the
 * executor services provided in this package.
 * 在本包里的Executors类为executor服务提供了工厂方法
 *
 * <h3>Usage Examples</h3>
 * 用例
 *
 * Here is a sketch of a network service in which threads in a thread
 * pool service incoming requests. It uses the preconfigured {@link
 * Executors#newFixedThreadPool} factory method:
 * 这是一个网络服务的草图，在线程池里的线程服务进来的请求。
 * 使用预配置的Executors#newFixedThreadPool工厂方法：
 *
 *  <pre> {@code
 * class NetworkService implements Runnable { // 为什么要实现Runnable接口，为了表明这是个要执行的类？
 *   private final ServerSocket serverSocket;
 *   private final ExecutorService pool;
 *
 *   public NetworkService(int port, int poolSize)
 *       throws IOException {
 *     serverSocket = new ServerSocket(port);
 *     pool = Executors.newFixedThreadPool(poolSize);
 *   }
 *
 *   public void run() { // run the service
 *     try {
 *       for (;;) {
 *         pool.execute(new Handler(serverSocket.accept()));
 *       }
 *     } catch (IOException ex) {
 *       pool.shutdown();        // 这里给手工shutdown了
 *     }
 *   }
 * }
 *
 * class Handler implements Runnable {
 *   private final Socket socket;
 *   Handler(Socket socket) { this.socket = socket; }
 *   public void run() {
 *     // read and service request on socket
 *   }
 * }}</pre>
 *
 * The following method shuts down an {@code ExecutorService} in two phases,
 * first by calling {@code shutdown} to reject incoming tasks, and then
 * calling {@code shutdownNow}, if necessary, to cancel any lingering tasks:
 * 接下来的方法从两个阶段停止ExecutorService，
 * 1、首先调用shutdown方法拒绝传入任务（拒绝新增任务）
 * 2、然后在有必要的时候，调用shutdownNow方法，取消任何延迟（linger）任务
 *
 *  <pre> {@code
 * void shutdownAndAwaitTermination(ExecutorService pool) {
 *   pool.shutdown(); // Disable new tasks from being submitted
 *   try {
 *     // Wait a while for existing tasks to terminate
 *     if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
 *       pool.shutdownNow(); // Cancel currently executing tasks
 *       // Wait a while for tasks to respond to being cancelled
 *       if (!pool.awaitTermination(60, TimeUnit.SECONDS))
 *           System.err.println("Pool did not terminate");
 *     }
 *   } catch (InterruptedException ie) {
 *     // (Re-)Cancel if current thread also interrupted
 *     pool.shutdownNow();
 *     // Preserve interrupt status
 *     Thread.currentThread().interrupt();
 *   }
 * }}</pre>
 *
 * <p>Memory consistency effects: Actions in a thread prior to the
 * submission of a {@code Runnable} or {@code Callable} task to an
 * {@code ExecutorService}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * any actions taken by that task, which in turn <i>happen-before</i> the
 * result is retrieved via {@code Future.get()}.
 * 内存一致性影响：在将Runnable或者Callable任务提交ExecutorService之前，MemoryVisibility发生在该任务采取的任何操作之前（happen-before）
 * 而后者又发生在通过Future.get()检索结果之前。
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface ExecutorService extends Executor {

    /**
     * Initiates an orderly shutdown in which previously submitted
     * tasks are executed, but no new tasks will be accepted.
     * Invocation has no additional effect if already shut down.
     * 启动有序的关闭，之前提交的任务将继续执行，但不会接受新的任务。
     * 如果已经shutdown关闭了，多次调用该方法不会有任何额外效果（或者说影响）。
     *
     * <p>This method does not wait for previously submitted tasks to
     * complete execution.  Use {@link #awaitTermination awaitTermination}
     * to do that.
     * 该方法不会等之前提交的任务执行完毕。
     * 想要做到这一点的话，使用awaitTermination方法
     *
     * @throws SecurityException if a security manager exists and
     *         shutting down this ExecutorService may manipulate
     *         threads that the caller is not permitted to modify
     *         because it does not hold {@link
     *         java.lang.RuntimePermission}{@code ("modifyThread")},
     *         or the security manager's {@code checkAccess} method
     *         denies access.
     *         如果安全管理器存在，并且关闭这个ExecutorService可能会操纵不允许调用者修改的线程，因为它没有持有修改线程的执行权限（RuntimePermission），
     *         或者安全管理器的checkAccess方法拒绝访问。
     */
    void shutdown();

    /**
     * Attempts to stop all actively executing tasks, halts the
     * processing of waiting tasks, and returns a list of the tasks
     * that were awaiting execution.
     * 尝试停止全部正在执行的活跃任务，停止（halt）等待任务的处理，并返回等待执行的任务列表。
     *
     * <p>This method does not wait for actively executing tasks to
     * terminate.  Use {@link #awaitTermination awaitTermination} to
     * do that.
     * 该方法不会等待正在执行的任务停止。（停止而不是完成）
     * 如果想做到这一点，使用awaitTermination方法
     *
     * <p>There are no guarantees beyond best-effort attempts to stop
     * processing actively executing tasks.  For example, typical
     * implementations will cancel via {@link Thread#interrupt}, so any
     * task that fails to respond to interrupts may never terminate.
     * 不保证，除了尽力尝试停止正在执行的任务处理。例如，典型的实现方式为通过interrupt来取消（正在执行的线程），因此任何未能响应中断的任务可能永远不会终止。
     *（意思就是调用了shutdownNow方法，只是会尝试去取消正在执行的任务，通常是用interrupt来实现，如果任务不响应中断，那任务还是会正常执行）
     *
     * @return list of tasks that never commenced execution
     *         从未开始（commenced）执行的任务列表（等待任务列表）
     * @throws SecurityException if a security manager exists and
     *         shutting down this ExecutorService may manipulate
     *         threads that the caller is not permitted to modify
     *         because it does not hold {@link
     *         java.lang.RuntimePermission}{@code ("modifyThread")},
     *         or the security manager's {@code checkAccess} method
     *         denies access.
     */
    List<Runnable> shutdownNow();

    /**
     * Returns {@code true} if this executor has been shut down.
     * 如果该executor被关闭了，返回true
     *
     * @return {@code true} if this executor has been shut down
     */
    boolean isShutdown();

    /**
     * Returns {@code true} if all tasks have completed following shut down.
     * Note that {@code isTerminated} is never {@code true} unless
     * either {@code shutdown} or {@code shutdownNow} was called first.
     * 如果所有任务伴随着shutdown都结束了，返回true
     * 注意，除非shutdown()或者shutdownNow()方法首先被调用，否则直接调用该方法并不会返回true。
     *
     * @return {@code true} if all tasks have completed following shut down
     */
    boolean isTerminated();

    /**
     * Blocks until all tasks have completed execution after a shutdown
     * request, or the timeout occurs, or the current thread is
     * interrupted, whichever happens first.
     * 在shutdown请求之后进行阻塞，直到所有任务完成处理，或者处理超时，或者当前线程被中断，以先发生者为准。
     *
     * @param timeout the maximum time to wait
     *        timeout参数，最大等待时间
     * @param unit the time unit of the timeout argument
     * @return {@code true} if this executor terminated and
     *         {@code false} if the timeout elapsed before termination
     *         true  如果executor终止
     *         false 如果在终止前超时
     * @throws InterruptedException if interrupted while waiting
     *         抛出InterruptedException，如果在等待时被中断。
     */
    boolean awaitTermination(long timeout, TimeUnit unit)
        throws InterruptedException;

    /**
     * Submits a value-returning task for execution and returns a
     * Future representing the pending results of the task. The
     * Future's {@code get} method will return the task's result upon
     * successful completion.
     * 提交一个有返回值的任务执行，并且返回一个代表等待task执行结果的Future。
     * Future的get方法将在task执行成功后返回执行结果。
     *
     * <p>
     * If you would like to immediately block waiting
     * for a task, you can use constructions of the form
     * {@code result = exec.submit(aCallable).get();}
     * 如果想立即阻塞等待任务完成，可以使用如下结构形式
     * result=exec.submit(aCallable).get(); // 提交任务，直接get等待
     *
     * <p>Note: The {@link Executors} class includes a set of methods
     * that can convert some other common closure-like objects,
     * for example, {@link java.security.PrivilegedAction} to
     * {@link Callable} form so they can be submitted.
     * 注意：Executors类包含了一系列的方法，可以将一些其他常见的类似闭包（closure-like）的对象（例如PrivilegedAction）转化为Callable形式，
     * 以便他们可以被提交
     *
     * @param task the task to submit
     *        task 提交的任务
     * @param <T> the type of the task's result
     *        <T> 泛型T代表任务的执行结果类型
     * @return a Future representing pending completion of the task
     *         Future代表等待task的执行
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     *         RejectedExecutionException 如果该任务无法被execution安排执行（调度）
     * @throws NullPointerException if the task is null
     */
    <T> Future<T> submit(Callable<T> task);

    /**
     * Submits a Runnable task for execution and returns a Future
     * representing that task. The Future's {@code get} method will
     * return the given result upon successful completion.
     * 提交一个Runnable任务执行并返回一个表示该任务的Future。
     * Future的get方法将在任务执行完成后返回给定的result（通过参数传入的result）
     *
     * @param task the task to submit
     * @param result the result to return
     *        result 任务执行结束后的返回值
     * @param <T> the type of the result
     * @return a Future representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if the task is null
     */
    <T> Future<T> submit(Runnable task, T result);

    /**
     * Submits a Runnable task for execution and returns a Future
     * representing that task. The Future's {@code get} method will
     * return {@code null} upon <em>successful</em> completion.
     * 提交一个Runnable任务执行并返回一个表示该任务的Future。
     * Future的get方法在任务成功执行完成将返回null。
     *
     * @param task the task to submit
     * @return a Future representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if the task is null
     */
    Future<?> submit(Runnable task);

    /**
     * Executes the given tasks, returning a list of Futures holding
     * their status and results when all complete.
     * {@link Future#isDone} is {@code true} for each
     * element of the returned list.
     * Note that a <em>completed</em> task could have
     * terminated either normally or by throwing an exception.
     * The results of this method are undefined if the given
     * collection is modified while this operation is in progress.
     * 执行给定的任务集合，返回Future列表持有当所有任务都执行完成后的状态和结果。（该方法会等待wait，直到所有任务都完成）
     * Future的isDone方法对每一个返回列表元素都是true。
     * 注意，已完成的任务可能是正常终止或者是抛出了异常。
     * 如果给定的集合在此操作期间被修改，该方法的结果是未定义。
     *
     * @param tasks the collection of tasks
     * @param <T> the type of the values returned from the tasks
     * @return a list of Futures representing the tasks, in the same
     *         sequential order as produced by the iterator for the
     *         given task list, each of which has completed
     * 返回值  代表任务的Future列表，通过给定的task列表迭代器生成相同顺序的序列，每个任务都已完成。（因为这个方法会等待所有集合里的任务都执行完成）
     * @throws InterruptedException if interrupted while waiting, in
     *         which case unfinished tasks are cancelled
     *         InterruptedException，如果在等待时中断，在这种情况下未完成的任务将取消。
     * @throws NullPointerException if tasks or any of its elements are {@code null}
     * @throws RejectedExecutionException if any task cannot be
     *         scheduled for execution
     */
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
        throws InterruptedException;

    /**
     * Executes the given tasks, returning a list of Futures holding
     * their status and results
     * when all complete or the timeout expires, whichever happens first.
     * {@link Future#isDone} is {@code true} for each
     * element of the returned list.
     * Upon return, tasks that have not completed are cancelled.
     * Note that a <em>completed</em> task could have
     * terminated either normally or by throwing an exception.
     * The results of this method are undefined if the given
     * collection is modified while this operation is in progress.
     * 执行给定的任务集合，返回一个Future列表，持有当所有task完成，或者发生超时（以先发生的为准）后的状态与结果。
     * Future的Done方法对每一个返回列表中的元素都是true。
     * 返回后，未完成的任务将被取消。（在运行超时时，对于还没完成的任务就直接取消）
     * 注意，已完成的任务可能是正常终止或者是抛出了异常。
     * 如果给定的集合在此操作期间被修改，该方法的结果是未定义
     *
     * @param tasks the collection of tasks
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @param <T> the type of the values returned from the tasks
     * @return a list of Futures representing the tasks, in the same
     *         sequential order as produced by the iterator for the
     *         given task list. If the operation did not time out,
     *         each task will have completed. If it did time out, some
     *         of these tasks will not have completed.
     *         如果没有超时操作，每个任务都将完成。
     *         如果有超时，一些任务将无法完成。
     * @throws InterruptedException if interrupted while waiting, in
     *         which case unfinished tasks are cancelled
     * @throws NullPointerException if tasks, any of its elements, or
     *         unit are {@code null}
     * @throws RejectedExecutionException if any task cannot be scheduled
     *         for execution
     */
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
                                  long timeout, TimeUnit unit)
        throws InterruptedException;

    /**
     * Executes the given tasks, returning the result
     * of one that has completed successfully (i.e., without throwing
     * an exception), if any do. Upon normal or exceptional return,
     * tasks that have not completed are cancelled.
     * The results of this method are undefined if the given
     * collection is modified while this operation is in progress.
     * 执行给定的任务集合，返回其中一个成功完成的任务结果（即没有抛出异常），如果有的话。
     * 在正常或者异常返回时，未完成的task将被取消。（意思是所有任务有一个正常执行完，其他任务就都取消？？？这大概就是invokeAny（执行任意一个）的含义）
     * 如果给定的集合在此操作期间被修改，该方法的结果是未定义
     *
     * @param tasks the collection of tasks
     * @param <T> the type of the values returned from the tasks
     * @return the result returned by one of the tasks
     * 返回值  返回其中一个任务的执行结果
     * @throws InterruptedException if interrupted while waiting
     * @throws NullPointerException if tasks or any element task
     *         subject to execution is {@code null}
     * @throws IllegalArgumentException if tasks is empty
     * @throws ExecutionException if no task successfully completes
     * @throws RejectedExecutionException if tasks cannot be scheduled
     *         for execution
     */
    <T> T invokeAny(Collection<? extends Callable<T>> tasks)
        throws InterruptedException, ExecutionException;

    /**
     * Executes the given tasks, returning the result
     * of one that has completed successfully (i.e., without throwing
     * an exception), if any do before the given timeout elapses.
     * Upon normal or exceptional return, tasks that have not
     * completed are cancelled.
     * The results of this method are undefined if the given
     * collection is modified while this operation is in progress.
     * 执行给定的任务集合，返回其中一个成功完成的任务结果（即没有抛出异常），如果在超时之前有的话。
     * 在正常或者异常返回时，未完成的task将被取消。（意思是所有任务有一个正常执行完，其他任务就都取消？？？）
     * 如果给定的集合在此操作期间被修改，该方法的结果是未定义
     *
     * @param tasks the collection of tasks
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @param <T> the type of the values returned from the tasks
     * @return the result returned by one of the tasks
     * @throws InterruptedException if interrupted while waiting
     * @throws NullPointerException if tasks, or unit, or any element
     *         task subject to execution is {@code null}
     * @throws TimeoutException if the given timeout elapses before
     *         any task successfully completes
     * @throws ExecutionException if no task successfully completes
     * @throws RejectedExecutionException if tasks cannot be scheduled
     *         for execution
     */
    <T> T invokeAny(Collection<? extends Callable<T>> tasks,
                    long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException;
}
```