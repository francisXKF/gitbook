# Future
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
 * A {@code Future} represents the result of an asynchronous
 * computation.  Methods are provided to check if the computation is
 * complete, to wait for its completion, and to retrieve the result of
 * the computation.  The result can only be retrieved using method
 * {@code get} when the computation has completed, blocking if
 * necessary until it is ready.  Cancellation is performed by the
 * {@code cancel} method.  Additional methods are provided to
 * determine if the task completed normally or was cancelled. Once a
 * computation has completed, the computation cannot be cancelled.
 * If you would like to use a {@code Future} for the sake
 * of cancellability but not provide a usable result, you can
 * declare types of the form {@code Future<?>} and
 * return {@code null} as a result of the underlying task.
 * Future表示一个异步计算的结果。
 * 提供的方法可以检查计算是否完成，等待计算完成，取回（retrieve）计算结果。
 * 只能使用get方法在计算完成时取回结果，必要时阻塞，直到计算完成（结果准备好）。
 * 通过cancel方法执行（performed）取消。
 * 提供了额外的（additional）方法支持判断任务是正常完成还是被取消了。
 * 一旦计算完成，那么计算就不能被取消了。
 * 如果只是为了可取消性而想使用Future，不想提供可用的结果，可以声明Future<?>这种类型的形式，返回null作为底层任务执行结果
 * 
 * <p>
 * <b>Sample Usage</b> (Note that the following classes are all
 * made-up.)
 * 简单用例（下面的类都是虚构的（made-up））
 * <pre> {@code
 * interface ArchiveSearcher { String search(String target); }
 * class App {
 *   ExecutorService executor = ...
 *   ArchiveSearcher searcher = ...
 *   void showSearch(final String target)
 *       throws InterruptedException {
 *     Future<String> future                                            // -- 带有submit的方法start
 *       = executor.submit(new Callable<String>() {
 *         public String call() {
 *             return searcher.search(target);
 *         }});                                                         // -- 带有submit的方法end
 *     displayOtherThings(); // do other things while searching
 *     try {
 *       displayText(future.get()); // use future
 *     } catch (ExecutionException ex) { cleanup(); return; }
 *   }
 * }}</pre>
 *
 * The {@link FutureTask} class is an implementation of {@code Future} that
 * implements {@code Runnable}, and so may be executed by an {@code Executor}.
 * FutureTask类是Future的实现类，实现了Runnable接口，可以被Executor执行。
 * （因为Executor接口只有一个execute方法，执行传入的Runnable类型的对象）
 * 
 * For example, the above construction with {@code submit} could be replaced by:
 * 例如，上面带有submit结构的代码，可以改成这个样子（这个样例看不出来改FutureTask的必要）
 *  <pre> {@code
 * FutureTask<String> future =
 *   new FutureTask<String>(new Callable<String>() {
 *     public String call() {
 *       return searcher.search(target);
 *   }});
 * executor.execute(future);}</pre>
 *
 * <p>Memory consistency effects: Actions taken by the asynchronous computation
 * <a href="package-summary.html#MemoryVisibility"> <i>happen-before</i></a>
 * actions following the corresponding {@code Future.get()} in another thread.
 * 内存一致性的影响
 * （跟以前的一样，不太明白这块的含义，这次不直译了）
 *
 * @see FutureTask
 * @see Executor
 * @since 1.5
 * @author Doug Lea
 * @param <V> The result type returned by this Future's {@code get} method
 */
public interface Future<V> {

    /**
     * Attempts to cancel execution of this task.  This attempt will
     * fail if the task has already completed, has already been cancelled,
     * or could not be cancelled for some other reason. If successful,
     * and this task has not started when {@code cancel} is called,
     * this task should never run.  If the task has already started,
     * then the {@code mayInterruptIfRunning} parameter determines
     * whether the thread executing this task should be interrupted in
     * an attempt to stop the task.
     * 尝试取消该任务执行。
     * 如果有以下情况任意一种发生，该尝试会失败：
     * 1、任务已经执行完毕
     * 2、任务已经被取消
     * 3、由于其他原因任务不能被取消
     * 如果取消成功，若任务在被调用cancel取消之前尚未启动，那么该任务将永远不会启动了（任务还没开始执行就被取消了，那任务就不会启动了）
     * 如果任务已经开始执行了，那么通过mayInterruptIfRunning参数决定，是否通过中断执行该任务的线程来尝试停止任务执行。
     *
     * <p>After this method returns, subsequent calls to {@link #isDone} will
     * always return {@code true}.  Subsequent calls to {@link #isCancelled}
     * will always return {@code true} if this method returned {@code true}.
     * 在该方法返回后，后续调用isDone方法将总是返回true。
     * 如果该方法返回true，后续调用isCancelled方法也总是返回true
     *
     * @param mayInterruptIfRunning {@code true} if the thread executing this
     * task should be interrupted; otherwise, in-progress tasks are allowed
     * to complete
     *        mayInterruptIfRunning参数为true时，执行该任务的线程应该被中断；
     *        除此之外，正在执行的任务被运行执行完毕。
     * @return {@code false} if the task could not be cancelled,
     * typically because it has already completed normally;
     *        返回false，如果任务不能被取消，通常是由于该任务已经正常执行完毕了。
     * {@code true} otherwise
     */
    boolean cancel(boolean mayInterruptIfRunning);

    /**
     * Returns {@code true} if this task was cancelled before it completed
     * normally.
     * 如果该任务在正常完成前被取消，返回true
     *
     * @return {@code true} if this task was cancelled before it completed
     */
    boolean isCancelled();

    /**
     * Returns {@code true} if this task completed.
     *
     * Completion may be due to normal termination, an exception, or
     * cancellation -- in all of these cases, this method will return
     * {@code true}.
     * 完成有以下几种情况：
     * 1、正常结束
     * 2、发生异常
     * 3、被取消
     * 所有这些场景，该方法都会返回true
     *
     * @return {@code true} if this task completed
     */
    boolean isDone();

    /**
     * Waits if necessary for the computation to complete, and then
     * retrieves its result.
     * 必要时等待计算完成，然后返回执行结果
     *
     * @return the computed result
     * @throws CancellationException if the computation was cancelled
     *                               如果在等待时发生取消，抛出CancellationException
     * @throws ExecutionException if the computation threw an
     * exception
     * @throws InterruptedException if the current thread was interrupted
     *                              如果在等待时执行该任务的线程发生中断，抛出InterruptedException
     * while waiting
     */
    V get() throws InterruptedException, ExecutionException;

    /**
     * Waits if necessary for at most the given time for the computation
     * to complete, and then retrieves its result, if available.
     * 必要时等待，如果在给定的时间内能够计算完成，那么返回结果。
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return the computed result
     * @throws CancellationException if the computation was cancelled
     * @throws ExecutionException if the computation threw an
     * exception
     * @throws InterruptedException if the current thread was interrupted
     * while waiting
     * @throws TimeoutException if the wait timed out
     */
    V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException;
}
```