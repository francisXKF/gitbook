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
 * CompletionService��ʹ���ṩ��Executor��ִ������
 * ���ల���ύ��������ɺ�ͨ��take�ŵ��ɷ��ʵĶ����С�
 * �����㹻���������ʺ��ڴ���������ʱ��ʱʹ�á�
 *
 * <p>
 *
 * <b>Usage Examples.</b>
 * ����
 *
 * Suppose you have a set of solvers for a certain problem, each
 * returning a value of some type {@code Result}, and would like to
 * run them concurrently, processing the results of each of them that
 * return a non-null value, in some method {@code use(Result r)}. You
 * could write this as:
 * ��������һ���ĳ������������������ÿ�������������ĳ��Result���͵�ֵ������ϣ�������������ǣ�
 * ��ĳЩ������ʹ��use(Result r)�������Ƿ��ص�ÿһ����null�����
 * ���������д��
 *
 * <pre> {@code
 * void solve(Executor e,
 *            Collection<Callable<Result>> solvers)
 *     throws InterruptedException, ExecutionException {
 *     CompletionService<Result> ecs
 *         = new ExecutorCompletionService<Result>(e); // �ø�����executor����CompletionService
 *     for (Callable<Result> s : solvers)
 *         ecs.submit(s);                // �ύ���񣬽���ɽ��д����ɶ��л���
 *     int n = solvers.size(); // �����������solvers������
 *     for (int i = 0; i < n; ++i) {
 *         Result r = ecs.take().get(); // ������ɶ��У���ȡִ����ɵĽ��
 *         if (r != null)
 *             use(r);                  // �Զ��崦��ִ�н��
 *     }
 * }}</pre>
 *
 * Suppose instead that you would like to use the first non-null result
 * of the set of tasks, ignoring any that encounter exceptions,
 * and cancelling all other tasks when the first one is ready:
 * ��������ʹ��������ĵ�һ���ǿս���������κ�������encounter�����쳣����������ڵ�һ������ɺ�ȡ�����������߳�
 *
 * <pre> {@code
 * void solve(Executor e,
 *            Collection<Callable<Result>> solvers)
 *     throws InterruptedException {
 *     CompletionService<Result> ecs
 *         = new ExecutorCompletionService<Result>(e);
 *     int n = solvers.size();
 *     List<Future<Result>> futures
 *         = new ArrayList<Future<Result>>(n); // ������Future����Ϊ�˿���cancel����
 *     Result result = null;
 *     try {
 *         for (Callable<Result> s : solvers)
 *             futures.add(ecs.submit(s));
 *         for (int i = 0; i < n; ++i) {
 *             try {
 *                 Result r = ecs.take().get(); // �Ӷ��׿�ʼ���ҵ�һ���ǿս��
 *                 if (r != null) {
 *                     result = r;
 *                     break;
 *                 }
 *             } catch (ExecutionException ignore) {}
 *         }
 *     }
 *     finally {                               // ʹ��future��δ��ɵ�����ȡ��
 *         for (Future<Result> f : futures)
 *             f.cancel(true);
 *     }
 *
 *     if (result != null)
 *         use(result);                       // ����зǿս���������Զ��崦��
 * }}</pre>
 */
public class ExecutorCompletionService<V> implements CompletionService<V> {
    private final Executor executor;                        // ���������executor
    private final AbstractExecutorService aes;              // ���������executor��aes������ö���
    private final BlockingQueue<Future<V>> completionQueue; // ������ɵ�����

    /**
     * FutureTask extension to enqueue upon completion
     * ��FutureTask����չ�����������ʱ��ӣ�����ɶ��У�
     */
    private class QueueingFuture extends FutureTask<Void> {
        QueueingFuture(RunnableFuture<V> task) {
            super(task, null);  // ����FutureTask��FutureTask(Runnable runnable, V result)���캯�����������÷��ؽ��Ϊnull
            this.task = task;    // ���������������ԣ����ڼ�¼�ύ�����񡣣�FutureTask����Ҫִ�е�������callable�����õģ�
        }
        protected void done() { completionQueue.add(task); } // �������ص㣬��FutureTask��done�����������أ�����ɵ��������ExecutorCompletionService����ɶ�����
        private final Future<V> task;
    }

    // ��callable����ת��ΪRunnableFuture
    // ���aesΪnull����ʾ��ǰexecutor����AbstractExecutorServiceʵ�ֵģ�����FutureTask���󷵻�
    // ���aes��Ϊnull����ôֱ��ʹ��AbstractExecutorService#newTaskFor��������RunnableFuture���
    // ��AbstractExecutorService#newTaskFor������Ҳ���õ�new FutureTask��
    // �Ʋ���Ϊ�˷������aes��ʵ����������newTaskFor��������ô�÷����Ϳ��Ե���aes��ʵ���෽��
    private RunnableFuture<V> newTaskFor(Callable<V> task) {
        if (aes == null)
            return new FutureTask<V>(task);
        else
            return aes.newTaskFor(task);
    }

    // ������ķ�������
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
     * ����һ��ExecutorCompletionService�����ṩ��ִ������executor��ִ�л�������
     * �ʹ���һ��LinkedBlockingQueue��Ϊ��ɶ��С�
     *
     * @param executor the executor to use
     * @throws NullPointerException if executor is {@code null}
     */
    public ExecutorCompletionService(Executor executor) {
        if (executor == null)
            throw new NullPointerException();                         // ���������ִ����Ϊnull���׳���ָ���쳣
        this.executor = executor;                                     // ����ִ����
        this.aes = (executor instanceof AbstractExecutorService) ?   // �ж����������ִ������AbstractExecutorServiceʵ������ô����aesΪ����ִ����������Ϊnull����������aes��Ϊ�˺���ʹ��aes���еķ���
            (AbstractExecutorService) executor : null;
        this.completionQueue = new LinkedBlockingQueue<Future<V>>();  // ����completionQueueΪ�µ�LinkedBlockingQueue��Ԫ��ΪFuture<V>
    }

    /**
     * Creates an ExecutorCompletionService using the supplied
     * executor for base task execution and the supplied queue as its
     * completion queue.
     * ����һ��ExecutorCompletionService�����ṩ��ִ������executor��ִ�л�������
     * ��ʹ���ṩ�Ķ�����Ϊ��ɶ���
     *
     * @param executor the executor to use
     * @param completionQueue the queue to use as the completion queue
     *        normally one dedicated for use by this service. This
     *        queue is treated as unbounded -- failed attempted
     *        {@code Queue.add} operations for completed tasks cause
     *        them not to be retrievable.
     *        ������completionQueueͨ��ר���ڣ�delicated���÷�����Ϊ��ɶ��С�
     *        �ö��б���Ϊ��treated �Դ��ģ��޽�� -- �����������Queue.add����ʧ�ܵĻ����ᵼ�����ǲ���ȡ�ء�
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
        this.completionQueue = completionQueue;                       // ʹ�ø����ĵȴ�����
    }

    // �ύ����
    public Future<V> submit(Callable<V> task) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<V> f = newTaskFor(task);        // ��callable����ת��ΪRunnableFuture
        executor.execute(new QueueingFuture(f));       // 
        return f;
    }

    public Future<V> submit(Runnable task, V result) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<V> f = newTaskFor(task, result);
        executor.execute(new QueueingFuture(f));
        return f;
    }

    // ��ȡ����Ԫ�أ���Ҫʱ��ȴ����������Ϊ�գ����������ȴ���
    public Future<V> take() throws InterruptedException {
        return completionQueue.take(); // ���õ���BlockingQueue#take����
    }

    // ��ȡ����Ԫ�أ��������Ϊ�շ���null���������������
    public Future<V> poll() {
        return completionQueue.poll(); // ���õ���Queue#poll����
    }

    // ��ȡ����Ԫ�أ���Ҫʱ������ʱ���ڵȴ����ף�����ԵȲ�������null
    // ��take�Ĳ�ͬ�������������ʱ��ȴ�
    public Future<V> poll(long timeout, TimeUnit unit)
            throws InterruptedException {
        return completionQueue.poll(timeout, unit); // ���õ���BlockingQueue#poll
    }

}
