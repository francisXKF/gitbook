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
 * A {@link CompletionService} that uses a supplied {@link Executor}
 * to execute tasks.  This class arranges that submitted tasks are,
 * upon completion, placed on a queue accessible using {@code take}.
 * The class is lightweight enough to be suitable for transient use
 * when processing groups of tasks.
 * CompletionService，使用提供的Executor来执行任务。
 * 该类安排提交的任务完成后，通过take放到可访问的队列中。
 * 该类足够轻量级，适合在处理任务组时临时使用。
 *
 * <p>
 *
 * <b>Usage Examples.</b>
 * 用例
 *
 * Suppose you have a set of solvers for a certain problem, each
 * returning a value of some type {@code Result}, and would like to
 * run them concurrently, processing the results of each of them that
 * return a non-null value, in some method {@code use(Result r)}. You
 * could write this as:
 * 假设你有一组对某个核心问题的求解器，每个求解器都返回某种Result类型的值，并且希望并发运行他们，
 * 在某些方法中使用use(Result r)处理他们返回的每一个非null结果。
 * 你可以这样写：
 *
 * <pre> {@code
 * void solve(Executor e,
 *            Collection<Callable<Result>> solvers)
 *     throws InterruptedException, ExecutionException {
 *     CompletionService<Result> ecs
 *         = new ExecutorCompletionService<Result>(e); // 用给定的executor创建CompletionService
 *     for (Callable<Result> s : solvers)
 *         ecs.submit(s);                // 提交任务，将完成结果写入完成队列汇总
 *     int n = solvers.size(); // 这里遍历的是solvers的数量
 *     for (int i = 0; i < n; ++i) {
 *         Result r = ecs.take().get(); // 遍历完成队列，获取执行完成的结果
 *         if (r != null)
 *             use(r);                  // 自定义处理执行结果
 *     }
 * }}</pre>
 *
 * Suppose instead that you would like to use the first non-null result
 * of the set of tasks, ignoring any that encounter exceptions,
 * and cancelling all other tasks when the first one is ready:
 * 假设你想使用任务集里的第一个非空结果，忽略任何遇到（encounter）的异常结果，并且在第一个已完成后取消所有其他线程
 *
 * <pre> {@code
 * void solve(Executor e,
 *            Collection<Callable<Result>> solvers)
 *     throws InterruptedException {
 *     CompletionService<Result> ecs
 *         = new ExecutorCompletionService<Result>(e);
 *     int n = solvers.size();
 *     List<Future<Result>> futures
 *         = new ArrayList<Future<Result>>(n); // 这里用Future，是为了可以cancel任务
 *     Result result = null;
 *     try {
 *         for (Callable<Result> s : solvers)
 *             futures.add(ecs.submit(s));
 *         for (int i = 0; i < n; ++i) {
 *             try {
 *                 Result r = ecs.take().get(); // 从队首开始，找第一个非空结果
 *                 if (r != null) {
 *                     result = r;
 *                     break;
 *                 }
 *             } catch (ExecutionException ignore) {}
 *         }
 *     }
 *     finally {                               // 使用future将未完成的任务取消
 *         for (Future<Result> f : futures)
 *             f.cancel(true);
 *     }
 *
 *     if (result != null)
 *         use(result);                       // 如果有非空结果，进行自定义处理
 * }}</pre>
 */
public class ExecutorCompletionService<V> implements CompletionService<V> {
    private final Executor executor;                        // 保存给定的executor
    private final AbstractExecutorService aes;              // 如果给定的executor是aes，保存该对象
    private final BlockingQueue<Future<V>> completionQueue; // 保存完成的任务

    /**
     * FutureTask extension to enqueue upon completion
     * 对FutureTask的扩展，在任务完成时入队（入完成队列）
     */
    private class QueueingFuture extends FutureTask<Void> {
        QueueingFuture(RunnableFuture<V> task) {
            super(task, null);  // 调用FutureTask的FutureTask(Runnable runnable, V result)构造函数，这里设置返回结果为null
            this.task = task;    // 本类中新增的属性，用于记录提交的任务。（FutureTask里需要执行的任务用callable来引用的）
        }
        protected void done() { completionQueue.add(task); } // 这里是重点，对FutureTask的done方法进行重载，将完成的任务加入ExecutorCompletionService的完成队列中
        private final Future<V> task;
    }

    // 将callable任务转化为RunnableFuture
    // 如果aes为null，表示当前executor不是AbstractExecutorService实现的，创建FutureTask对象返回
    // 如果aes不为null，那么直接使用AbstractExecutorService#newTaskFor方法创建RunnableFuture结果
    // 看AbstractExecutorService#newTaskFor方法，也是用的new FutureTask，
    // 推测是为了方便如果aes的实现类重载了newTaskFor方法，那么该方法就可以调用aes的实现类方法
    private RunnableFuture<V> newTaskFor(Callable<V> task) {
        if (aes == null)
            return new FutureTask<V>(task);
        else
            return aes.newTaskFor(task);
    }

    // 与上面的方法类似
    private RunnableFuture<V> newTaskFor(Runnable task, V result) {
        if (aes == null)
            return new FutureTask<V>(task, result);
        else
            return aes.newTaskFor(task, result);
    }

    /**
     * Creates an ExecutorCompletionService using the supplied
     * executor for base task execution and a
     * {@link LinkedBlockingQueue} as a completion queue.
     * 创建一个ExecutorCompletionService，用提供的执行器（executor）执行基础任务，
     * 和创建一个LinkedBlockingQueue作为完成队列。
     *
     * @param executor the executor to use
     * @throws NullPointerException if executor is {@code null}
     */
    public ExecutorCompletionService(Executor executor) {
        if (executor == null)
            throw new NullPointerException();                         // 如果给定的执行器为null，抛出空指针异常
        this.executor = executor;                                     // 设置执行器
        this.aes = (executor instanceof AbstractExecutorService) ?   // 判断如果给定的执行器是AbstractExecutorService实例，那么设置aes为给定执行器，否则为null。这里设置aes是为了后续使用aes现有的方法
            (AbstractExecutorService) executor : null;
        this.completionQueue = new LinkedBlockingQueue<Future<V>>();  // 设置completionQueue为新的LinkedBlockingQueue，元素为Future<V>
    }

    /**
     * Creates an ExecutorCompletionService using the supplied
     * executor for base task execution and the supplied queue as its
     * completion queue.
     * 创建一个ExecutorCompletionService，用提供的执行器（executor）执行基础任务，
     * 和使用提供的队列作为完成队列
     *
     * @param executor the executor to use
     * @param completionQueue the queue to use as the completion queue
     *        normally one dedicated for use by this service. This
     *        queue is treated as unbounded -- failed attempted
     *        {@code Queue.add} operations for completed tasks cause
     *        them not to be retrievable.
     *        给定的completionQueue通常专用于（delicated）该服务作为完成队列。
     *        该队列被视为（treated 对待的）无界的 -- 已完成任务尝试Queue.add操作失败的话，会导致他们不可取回。
     *        
     * @throws NullPointerException if executor or completionQueue are {@code null}
     */
    public ExecutorCompletionService(Executor executor,
                                     BlockingQueue<Future<V>> completionQueue) {
        if (executor == null || completionQueue == null)
            throw new NullPointerException();
        this.executor = executor;
        this.aes = (executor instanceof AbstractExecutorService) ?
            (AbstractExecutorService) executor : null;
        this.completionQueue = completionQueue;                       // 使用给定的等待队列
    }

    // 提交任务
    public Future<V> submit(Callable<V> task) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<V> f = newTaskFor(task);        // 将callable任务转化为RunnableFuture
        executor.execute(new QueueingFuture(f));       // 
        return f;
    }

    public Future<V> submit(Runnable task, V result) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<V> f = newTaskFor(task, result);
        executor.execute(new QueueingFuture(f));
        return f;
    }

    // 获取队首元素，必要时需等待（如果队列为空，就需阻塞等待）
    public Future<V> take() throws InterruptedException {
        return completionQueue.take(); // 调用的是BlockingQueue#take方法
    }

    // 获取队首元素，如果队列为空返回null（这个不会阻塞）
    public Future<V> poll() {
        return completionQueue.poll(); // 调用的是Queue#poll方法
    }

    // 获取队首元素，必要时在有限时间内等待队首，如果仍等不到返回null
    // 与take的不同就是这个是有限时间等待
    public Future<V> poll(long timeout, TimeUnit unit)
            throws InterruptedException {
        return completionQueue.poll(timeout, unit); // 调用的是BlockingQueue#poll
    }

}
