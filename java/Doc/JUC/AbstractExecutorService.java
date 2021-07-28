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
import java.util.*;

/**
 * Provides default implementations of {@link ExecutorService}
 * execution methods. This class implements the {@code submit},
 * {@code invokeAny} and {@code invokeAll} methods using a
 * {@link RunnableFuture} returned by {@code newTaskFor}, which defaults
 * to the {@link FutureTask} class provided in this package.  For example,
 * the implementation of {@code submit(Runnable)} creates an
 * associated {@code RunnableFuture} that is executed and
 * returned. Subclasses may override the {@code newTaskFor} methods
 * to return {@code RunnableFuture} implementations other than
 * {@code FutureTask}.
 * 提供ExecutorService执行方法的默认实现。
 * 该类通过使用RunnableFuture返回的newTaskFor（newTaskFor默认提供的返回对象为该包下的FutureTask类实例）实现了submit、invokeAny和invokeAll方法，
 * 例如，实现的submit(Runnable)方法，就创建一个关联的RunnableFuture对象，用于执行任务与返回。
 * 子类可以覆盖newTaskFor方法，以返回RunnableFuture的实现，而不是FutureTask的实现。
 *
 * <p><b>Extension example</b>. Here is a sketch of a class
 * that customizes {@link ThreadPoolExecutor} to use
 * a {@code CustomTask} class instead of the default {@code FutureTask}:
 * 扩展样例。
 * 这有一个草图（sketch），一个自定义的ThreadPoolExecutor使用CustomTask类替代默认的FutureTask类：
 * 
 *  <pre> {@code
 * public class CustomThreadPoolExecutor extends ThreadPoolExecutor {
 *
 *   static class CustomTask<V> implements RunnableFuture<V> {...}
 *
 *   protected <V> RunnableFuture<V> newTaskFor(Callable<V> c) {
 *       return new CustomTask<V>(c); // AbstractExecutorService这里是返回一个new FutureTask，该自定义类返回了一个new CustomTask，只要自定义的类实现了RunnableFuture接口就行。
 *   }
 *   protected <V> RunnableFuture<V> newTaskFor(Runnable r, V v) {
 *       return new CustomTask<V>(r, v);
 *   }
 *   // ... add constructors, etc.
 * }}</pre>
 *
 * @since 1.5
 * @author Doug Lea
 */
public abstract class AbstractExecutorService implements ExecutorService {

    /**
     * Returns a {@code RunnableFuture} for the given runnable and default
     * value.
     * 返回RunnableFture，通过给定的runnable与默认返回值构建。
     *
     * @param runnable the runnable task being wrapped
     *        runnable 被包装的runnable任务
     * @param value the default value for the returned future
     *        value 默认的future返回值
     * @param <T> the type of the given value
     *        <T> 给定返回值的类型
     * @return a {@code RunnableFuture} which, when run, will run the
     * underlying runnable and which, as a {@code Future}, will yield
     * the given value as its result and provide for cancellation of
     * the underlying task
     *         返回一个RunnableFuture，在运行时会执行底层的runnable，
     *         并且作为Future，将使用给定的值作为返回结果，并提供底层任务的取消（方法）
     * @since 1.6
     */
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new FutureTask<T>(runnable, value); // FutureTask会调用Executors#callable方法，将runnable+value转化为callable。如果runnable为null，抛出空指针异常
    }

    /**
     * Returns a {@code RunnableFuture} for the given callable task.
     * 返回Runnable，通过给定的callable任务构建。
     *
     * @param callable the callable task being wrapped
     * @param <T> the type of the callable's result
     * @return a {@code RunnableFuture} which, when run, will call the
     * underlying callable and which, as a {@code Future}, will yield
     * the callable's result as its result and provide for
     * cancellation of the underlying task
     * @since 1.6
     */
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new FutureTask<T>(callable); // 如果callable为null，抛出空指针异常。否则创建FutureTask，callable=callable，state=NEW
    }

    /**
     * 提交返回值为null的Runnable任务
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException       {@inheritDoc}
     */
    public Future<?> submit(Runnable task) {
        if (task == null) throw new NullPointerException(); // 如果提交的任务为null，抛出异常
        RunnableFuture<Void> ftask = newTaskFor(task, null); // 将任务转化为FutureTask
        execute(ftask);                                       // 这里的execute是Executor接口类定义的方法，具体的调用是实现类做的，比如ThreadPoolExecutor#execute(Runnable command)
        return ftask;                                        // 这里将提交的RunnableFuture对象返回了（是想拿这个Future来获取结果）
    }

    /**
     * 提交带有指定返回值的Runnable任务
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException       {@inheritDoc}
     */
    public <T> Future<T> submit(Runnable task, T result) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<T> ftask = newTaskFor(task, result);
        execute(ftask);                                       // 这里是交给实现类做的，比如ThreadPoolExecutor#execute
        return ftask;
    }

    /**
     * 提交callable任务
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException       {@inheritDoc}
     */
    public <T> Future<T> submit(Callable<T> task) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<T> ftask = newTaskFor(task);
        execute(ftask);                                       // 这里是交给实现类做的，比如ThreadPoolExecutor#execute
        return ftask;
    }

    /**
     * the main mechanics of invokeAny.
     * invokeAny的主要机制
     * （执行给定的任务集合，返回其中一个成功完成的任务结果（即没有抛出异常），如果有的话。）
     */
    private <T> T doInvokeAny(Collection<? extends Callable<T>> tasks,
                              boolean timed, long nanos) // tasks为要提交的任务集合，限时执行（该限时为拿到所有执行结果的总限时）
        throws InterruptedException, ExecutionException, TimeoutException {
        if (tasks == null)
            throw new NullPointerException();
        int ntasks = tasks.size();
        if (ntasks == 0)
            throw new IllegalArgumentException(); // 如果任务集合里没有任务，抛出IllegalArgumentException
        ArrayList<Future<T>> futures = new ArrayList<Future<T>>(ntasks); // 创建初识容量为任务数长度的空列表。这里使用future是为了能够方便操作任务，比如cancel等操作。
        ExecutorCompletionService<T> ecs =
            new ExecutorCompletionService<T>(this); // 用AbstractExecutorService创建ExecutorCompletionService（ecs），用于执行任务集，并将完成的任务保存在完成队列中

        // For efficiency, especially in executors with limited
        // parallelism, check to see if previously submitted tasks are
        // done before submitting more of them. This interleaving
        // plus the exception mechanics account for messiness of main
        // loop.
        // 为提高效率（efficiency），尤其是在并行性（parallelism）有限的执行器中（executors），
        // 在更多的任务提交之前检查之前提交的任务是否已完成。
        // 这种交错（interleaving 交叉）加上异常机制解释了（account for）主循环的混乱。

        try {
            // Record exceptions so that if we fail to obtain any
            // result, we can throw the last exception we got.
            // 记录异常，如果无法获取任何结果，可以抛出获取到的最后一个异常。
            ExecutionException ee = null;
            final long deadline = timed ? System.nanoTime() + nanos : 0L;  // 计算限时截止时间
            Iterator<? extends Callable<T>> it = tasks.iterator();

            // Start one task for sure; the rest incrementally
            // 确定开始的任务，其余逐渐增加
            futures.add(ecs.submit(it.next()));                    // ecs.submit将任务提交到Executor中执行，返回的RunnableFuture，是通过AES（也就是本类）的newTaskFor方法创建的。
            --ntasks;                                              // 执行了一个任务，等待任务数-1
            int active = 1;                                        // 活跃的任务数置为1。活跃任务=正在执行的任务

            for (;;) {
                Future<T> f = ecs.poll();                          // 获取完成队列队首元素，如果没有则返回null（不会阻塞，take或者poll(时限)才会阻塞）
                if (f == null) {                                  // f == null表示没有拿到队首元素，说明当前没有任务完成结果
                    if (ntasks > 0) {
                        --ntasks;
                        futures.add(ecs.submit(it.next()));        // 如果任务没执行完，那么将下一个任务加入ecs中执行（注意使用迭代器实现获取下一个任务）。任务数-1，活跃任务数+1
                        ++active;
                    }
                    else if (active == 0)
                        break;                                     // 如果活跃任务数为0，表示所有任务执行完毕，退出
                    else if (timed) {
                        f = ecs.poll(nanos, TimeUnit.NANOSECONDS);  // 到了这里，尚未执行的任务=0，活跃的任务>0，只需要等任务完成了。如果限时等待，就用ecs.poll(时限)的方法等，等不到抛异常
                        if (f == null)
                            throw new TimeoutException();
                        nanos = deadline - System.nanoTime();       // 注意这里，如果等到了一个，剩余等待时间需要减去当前已用时间
                    }
                    else
                        f = ecs.take();                             // 如果不限时，那就一直等了
                }                                                   // 到了这里可以看到，最多有两个任务在并行执行
                if (f != null) {                                   // 如果有任务完成，拿到完成结果。（注意这个f可以是在f==null分支里面执行的结果）
                    --active;                                       // 活跃任务数-1
                    try {
                        return f.get();                            // 返回执行结果
                    } catch (ExecutionException eex) {
                        ee = eex;                                   // ee用来记录上次的异常结果
                    } catch (RuntimeException rex) {
                        ee = new ExecutionException(rex);
                    }
                }
            }

            if (ee == null)
                ee = new ExecutionException();                      // 执行到这里，说明退出了for循环，但并没有执行结果的值，也没有异常信息，那么就抛出执行异常。
            throw ee;

        } finally {
            for (int i = 0, size = futures.size(); i < size; i++)
                futures.get(i).cancel(true);                        // 因为是invokeAny，有一个执行完了，其他所有未完成的任务都取消。（只有FutureTask NEW->CANCELLED或者是NEW->INTERRUPTING->INTERRUPTED）
        }
    }

    // 执行给定的任务集合，返回其中一个成功完成的任务结果（即没有抛出异常），如果有的话。（重载ExecutorService方法）
    // 不限时的invokeAny
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
        throws InterruptedException, ExecutionException {
        try {
            return doInvokeAny(tasks, false, 0); // 不限时
        } catch (TimeoutException cannotHappen) {
            assert false; // 这个有意思，一个从来不会发生的异常如果进来了，直接断言为失败，不return结果？？？（不知道有啥用）
            return null;
        }
    }

    // 执行给定的任务集合，返回其中一个成功完成的任务结果（即没有抛出异常），如果有的话。（重载ExecutorService方法）
    // 限时的invokeAny（该限时为拿到所有执行结果的总限时）
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
                           long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
        return doInvokeAny(tasks, true, unit.toNanos(timeout));
    }

    // 执行给定的任务集合，返回Future列表持有当所有任务都执行完成后的状态和结果。（该方法会等待wait，直到所有任务都完成）（重载ExecutorService方法）
    // 不限时的invokeAll
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
        throws InterruptedException {
        if (tasks == null)
            throw new NullPointerException();
        ArrayList<Future<T>> futures = new ArrayList<Future<T>>(tasks.size()); // 创建初识容量为任务数长度的空列表。这里使用future是为了能够方便操作任务，比如cancel等操作。
        boolean done = false;                                       // 所有任务完成标识
        try {
            for (Callable<T> t : tasks) {
                RunnableFuture<T> f = newTaskFor(t);
                futures.add(f);                                      // 将包装后的任务加入到futures
                execute(f);                                          // 调用子类的execute方法执行任务（像ThreadPoolExecutor可能只是提交了任务，任务需要排队执行）
            }
            for (int i = 0, size = futures.size(); i < size; i++) {
                Future<T> f = futures.get(i);                        // 遍历futures，拿每个任务的执行future
                if (!f.isDone()) {                                   // 判断该任务是否执行完成（state!=NEW）
                    try {
                        f.get();                                     // 如果没完成，通过FutureTask#get()来FutureTask#awaitDone(false, 0L)，等待完成
                    } catch (CancellationException ignore) {
                    } catch (ExecutionException ignore) {
                    }
                }
            }
            done = true;                                             // 执行到这里说明所有任务都完成了
            return futures;                                          // 返回结果是所有future集合
        } finally {
            if (!done)                                                // 如果任务没完成而到了这一步，说明可能该方法被中断，需要取消所有未完成的任务
                for (int i = 0, size = futures.size(); i < size; i++)
                    futures.get(i).cancel(true);                      // 取消任务（可取消的任务state == NEW），可通过中断runner来结束
        }
    }

    // 执行给定的任务集合，返回Future列表持有当所有任务都执行完成后的状态和结果。（该方法会等待wait，直到所有任务都完成）（重载ExecutorService方法）
    // 限时的invokeAll（该限时为拿到所有执行结果的总限时）
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
                                         long timeout, TimeUnit unit)
        throws InterruptedException {
        if (tasks == null)
            throw new NullPointerException();
        long nanos = unit.toNanos(timeout);
        ArrayList<Future<T>> futures = new ArrayList<Future<T>>(tasks.size());
        boolean done = false;
        try {
            for (Callable<T> t : tasks)
                futures.add(newTaskFor(t));                           // 这里跟不限时的invokeAll有区别，并没有立即将任务放到Executor（具体的实现类）#execute执行

            final long deadline = System.nanoTime() + nanos;
            final int size = futures.size();

            // Interleave time checks and calls to execute in case
            // executor doesn't have any/much parallelism.
            // 交错时间检查和调用执行，以防止executor没有任何/很多并行性
            // （逐个任务提交执行（不并行处理，实际由Executor实现子类来决定），记录执行时间，确保限时功能）
            for (int i = 0; i < size; i++) {
                execute((Runnable)futures.get(i));                    // 逐个提交执行，计算剩余时间，如果剩余时间<=0L，返回（在finally中停止尚未执行的任务）
                nanos = deadline - System.nanoTime();
                if (nanos <= 0L)
                    return futures;
            }

            for (int i = 0; i < size; i++) {
                Future<T> f = futures.get(i);
                if (!f.isDone()) {
                    if (nanos <= 0L)
                        return futures;
                    try {
                        f.get(nanos, TimeUnit.NANOSECONDS);           // 用剩余时间等待任务完成，如果任务执行超过剩余等待时间，或者部分任务执行时间已经超过等待时间，返回（在finally中停止尚未执行的任务）
                    } catch (CancellationException ignore) {
                    } catch (ExecutionException ignore) {
                    } catch (TimeoutException toe) {
                        return futures;
                    }
                    nanos = deadline - System.nanoTime();
                }
            }
            done = true;
            return futures;
        } finally {
            if (!done)
                for (int i = 0, size = futures.size(); i < size; i++)
                    futures.get(i).cancel(true);                      // 取消任务（可取消的任务state == NEW），可通过中断runner来结束
        }
    }

}
