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
 * �ṩExecutorServiceִ�з�����Ĭ��ʵ�֡�
 * ����ͨ��ʹ��RunnableFuture���ص�newTaskFor��newTaskForĬ���ṩ�ķ��ض���Ϊ�ð��µ�FutureTask��ʵ����ʵ����submit��invokeAny��invokeAll������
 * ���磬ʵ�ֵ�submit(Runnable)�������ʹ���һ��������RunnableFuture��������ִ�������뷵�ء�
 * ������Ը���newTaskFor�������Է���RunnableFuture��ʵ�֣�������FutureTask��ʵ�֡�
 *
 * <p><b>Extension example</b>. Here is a sketch of a class
 * that customizes {@link ThreadPoolExecutor} to use
 * a {@code CustomTask} class instead of the default {@code FutureTask}:
 * ��չ������
 * ����һ����ͼ��sketch����һ���Զ����ThreadPoolExecutorʹ��CustomTask�����Ĭ�ϵ�FutureTask�ࣺ
 * 
 *  <pre> {@code
 * public class CustomThreadPoolExecutor extends ThreadPoolExecutor {
 *
 *   static class CustomTask<V> implements RunnableFuture<V> {...}
 *
 *   protected <V> RunnableFuture<V> newTaskFor(Callable<V> c) {
 *       return new CustomTask<V>(c); // AbstractExecutorService�����Ƿ���һ��new FutureTask�����Զ����෵����һ��new CustomTask��ֻҪ�Զ������ʵ����RunnableFuture�ӿھ��С�
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
     * ����RunnableFture��ͨ��������runnable��Ĭ�Ϸ���ֵ������
     *
     * @param runnable the runnable task being wrapped
     *        runnable ����װ��runnable����
     * @param value the default value for the returned future
     *        value Ĭ�ϵ�future����ֵ
     * @param <T> the type of the given value
     *        <T> ��������ֵ������
     * @return a {@code RunnableFuture} which, when run, will run the
     * underlying runnable and which, as a {@code Future}, will yield
     * the given value as its result and provide for cancellation of
     * the underlying task
     *         ����һ��RunnableFuture��������ʱ��ִ�еײ��runnable��
     *         ������ΪFuture����ʹ�ø�����ֵ��Ϊ���ؽ�������ṩ�ײ������ȡ����������
     * @since 1.6
     */
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new FutureTask<T>(runnable, value); // FutureTask�����Executors#callable��������runnable+valueת��Ϊcallable�����runnableΪnull���׳���ָ���쳣
    }

    /**
     * Returns a {@code RunnableFuture} for the given callable task.
     * ����Runnable��ͨ��������callable���񹹽���
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
        return new FutureTask<T>(callable); // ���callableΪnull���׳���ָ���쳣�����򴴽�FutureTask��callable=callable��state=NEW
    }

    /**
     * �ύ����ֵΪnull��Runnable����
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException       {@inheritDoc}
     */
    public Future<?> submit(Runnable task) {
        if (task == null) throw new NullPointerException(); // ����ύ������Ϊnull���׳��쳣
        RunnableFuture<Void> ftask = newTaskFor(task, null); // ������ת��ΪFutureTask
        execute(ftask);                                       // �����execute��Executor�ӿ��ඨ��ķ���������ĵ�����ʵ�������ģ�����ThreadPoolExecutor#execute(Runnable command)
        return ftask;                                        // ���ｫ�ύ��RunnableFuture���󷵻��ˣ����������Future����ȡ�����
    }

    /**
     * �ύ����ָ������ֵ��Runnable����
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException       {@inheritDoc}
     */
    public <T> Future<T> submit(Runnable task, T result) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<T> ftask = newTaskFor(task, result);
        execute(ftask);                                       // �����ǽ���ʵ�������ģ�����ThreadPoolExecutor#execute
        return ftask;
    }

    /**
     * �ύcallable����
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException       {@inheritDoc}
     */
    public <T> Future<T> submit(Callable<T> task) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<T> ftask = newTaskFor(task);
        execute(ftask);                                       // �����ǽ���ʵ�������ģ�����ThreadPoolExecutor#execute
        return ftask;
    }

    /**
     * the main mechanics of invokeAny.
     * invokeAny����Ҫ����
     * ��ִ�и��������񼯺ϣ���������һ���ɹ���ɵ�����������û���׳��쳣��������еĻ�����
     */
    private <T> T doInvokeAny(Collection<? extends Callable<T>> tasks,
                              boolean timed, long nanos) // tasksΪҪ�ύ�����񼯺ϣ���ʱִ�У�����ʱΪ�õ�����ִ�н��������ʱ��
        throws InterruptedException, ExecutionException, TimeoutException {
        if (tasks == null)
            throw new NullPointerException();
        int ntasks = tasks.size();
        if (ntasks == 0)
            throw new IllegalArgumentException(); // ������񼯺���û�������׳�IllegalArgumentException
        ArrayList<Future<T>> futures = new ArrayList<Future<T>>(ntasks); // ������ʶ����Ϊ���������ȵĿ��б�����ʹ��future��Ϊ���ܹ�����������񣬱���cancel�Ȳ�����
        ExecutorCompletionService<T> ecs =
            new ExecutorCompletionService<T>(this); // ��AbstractExecutorService����ExecutorCompletionService��ecs��������ִ�����񼯣�������ɵ����񱣴�����ɶ�����

        // For efficiency, especially in executors with limited
        // parallelism, check to see if previously submitted tasks are
        // done before submitting more of them. This interleaving
        // plus the exception mechanics account for messiness of main
        // loop.
        // Ϊ���Ч�ʣ�efficiency�����������ڲ����ԣ�parallelism�����޵�ִ�����У�executors����
        // �ڸ���������ύ֮ǰ���֮ǰ�ύ�������Ƿ�����ɡ�
        // ���ֽ���interleaving ���棩�����쳣���ƽ����ˣ�account for����ѭ���Ļ��ҡ�

        try {
            // Record exceptions so that if we fail to obtain any
            // result, we can throw the last exception we got.
            // ��¼�쳣������޷���ȡ�κν���������׳���ȡ�������һ���쳣��
            ExecutionException ee = null;
            final long deadline = timed ? System.nanoTime() + nanos : 0L;  // ������ʱ��ֹʱ��
            Iterator<? extends Callable<T>> it = tasks.iterator();

            // Start one task for sure; the rest incrementally
            // ȷ����ʼ����������������
            futures.add(ecs.submit(it.next()));                    // ecs.submit�������ύ��Executor��ִ�У����ص�RunnableFuture����ͨ��AES��Ҳ���Ǳ��ࣩ��newTaskFor���������ġ�
            --ntasks;                                              // ִ����һ�����񣬵ȴ�������-1
            int active = 1;                                        // ��Ծ����������Ϊ1����Ծ����=����ִ�е�����

            for (;;) {
                Future<T> f = ecs.poll();                          // ��ȡ��ɶ��ж���Ԫ�أ����û���򷵻�null������������take����poll(ʱ��)�Ż�������
                if (f == null) {                                  // f == null��ʾû���õ�����Ԫ�أ�˵����ǰû��������ɽ��
                    if (ntasks > 0) {
                        --ntasks;
                        futures.add(ecs.submit(it.next()));        // �������ûִ���꣬��ô����һ���������ecs��ִ�У�ע��ʹ�õ�����ʵ�ֻ�ȡ��һ�����񣩡�������-1����Ծ������+1
                        ++active;
                    }
                    else if (active == 0)
                        break;                                     // �����Ծ������Ϊ0����ʾ��������ִ����ϣ��˳�
                    else if (timed) {
                        f = ecs.poll(nanos, TimeUnit.NANOSECONDS);  // ���������δִ�е�����=0����Ծ������>0��ֻ��Ҫ����������ˡ������ʱ�ȴ�������ecs.poll(ʱ��)�ķ����ȣ��Ȳ������쳣
                        if (f == null)
                            throw new TimeoutException();
                        nanos = deadline - System.nanoTime();       // ע���������ȵ���һ����ʣ��ȴ�ʱ����Ҫ��ȥ��ǰ����ʱ��
                    }
                    else
                        f = ecs.take();                             // �������ʱ���Ǿ�һֱ����
                }                                                   // ����������Կ�������������������ڲ���ִ��
                if (f != null) {                                   // �����������ɣ��õ���ɽ������ע�����f��������f==null��֧����ִ�еĽ����
                    --active;                                       // ��Ծ������-1
                    try {
                        return f.get();                            // ����ִ�н��
                    } catch (ExecutionException eex) {
                        ee = eex;                                   // ee������¼�ϴε��쳣���
                    } catch (RuntimeException rex) {
                        ee = new ExecutionException(rex);
                    }
                }
            }

            if (ee == null)
                ee = new ExecutionException();                      // ִ�е����˵���˳���forѭ��������û��ִ�н����ֵ��Ҳû���쳣��Ϣ����ô���׳�ִ���쳣��
            throw ee;

        } finally {
            for (int i = 0, size = futures.size(); i < size; i++)
                futures.get(i).cancel(true);                        // ��Ϊ��invokeAny����һ��ִ�����ˣ���������δ��ɵ�����ȡ������ֻ��FutureTask NEW->CANCELLED������NEW->INTERRUPTING->INTERRUPTED��
        }
    }

    // ִ�и��������񼯺ϣ���������һ���ɹ���ɵ�����������û���׳��쳣��������еĻ���������ExecutorService������
    // ����ʱ��invokeAny
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
        throws InterruptedException, ExecutionException {
        try {
            return doInvokeAny(tasks, false, 0); // ����ʱ
        } catch (TimeoutException cannotHappen) {
            assert false; // �������˼��һ���������ᷢ�����쳣��������ˣ�ֱ�Ӷ���Ϊʧ�ܣ���return�������������֪����ɶ�ã�
            return null;
        }
    }

    // ִ�и��������񼯺ϣ���������һ���ɹ���ɵ�����������û���׳��쳣��������еĻ���������ExecutorService������
    // ��ʱ��invokeAny������ʱΪ�õ�����ִ�н��������ʱ��
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
                           long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
        return doInvokeAny(tasks, true, unit.toNanos(timeout));
    }

    // ִ�и��������񼯺ϣ�����Future�б���е���������ִ����ɺ��״̬�ͽ�������÷�����ȴ�wait��ֱ������������ɣ�������ExecutorService������
    // ����ʱ��invokeAll
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
        throws InterruptedException {
        if (tasks == null)
            throw new NullPointerException();
        ArrayList<Future<T>> futures = new ArrayList<Future<T>>(tasks.size()); // ������ʶ����Ϊ���������ȵĿ��б�����ʹ��future��Ϊ���ܹ�����������񣬱���cancel�Ȳ�����
        boolean done = false;                                       // ����������ɱ�ʶ
        try {
            for (Callable<T> t : tasks) {
                RunnableFuture<T> f = newTaskFor(t);
                futures.add(f);                                      // ����װ���������뵽futures
                execute(f);                                          // ���������execute����ִ��������ThreadPoolExecutor����ֻ���ύ������������Ҫ�Ŷ�ִ�У�
            }
            for (int i = 0, size = futures.size(); i < size; i++) {
                Future<T> f = futures.get(i);                        // ����futures����ÿ�������ִ��future
                if (!f.isDone()) {                                   // �жϸ������Ƿ�ִ����ɣ�state!=NEW��
                    try {
                        f.get();                                     // ���û��ɣ�ͨ��FutureTask#get()��FutureTask#awaitDone(false, 0L)���ȴ����
                    } catch (CancellationException ignore) {
                    } catch (ExecutionException ignore) {
                    }
                }
            }
            done = true;                                             // ִ�е�����˵���������������
            return futures;                                          // ���ؽ��������future����
        } finally {
            if (!done)                                                // �������û��ɶ�������һ����˵�����ܸ÷������жϣ���Ҫȡ������δ��ɵ�����
                for (int i = 0, size = futures.size(); i < size; i++)
                    futures.get(i).cancel(true);                      // ȡ�����񣨿�ȡ��������state == NEW������ͨ���ж�runner������
        }
    }

    // ִ�и��������񼯺ϣ�����Future�б���е���������ִ����ɺ��״̬�ͽ�������÷�����ȴ�wait��ֱ������������ɣ�������ExecutorService������
    // ��ʱ��invokeAll������ʱΪ�õ�����ִ�н��������ʱ��
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
                futures.add(newTaskFor(t));                           // ���������ʱ��invokeAll�����𣬲�û������������ŵ�Executor�������ʵ���ࣩ#executeִ��

            final long deadline = System.nanoTime() + nanos;
            final int size = futures.size();

            // Interleave time checks and calls to execute in case
            // executor doesn't have any/much parallelism.
            // ����ʱ����͵���ִ�У��Է�ֹexecutorû���κ�/�ܶಢ����
            // ����������ύִ�У������д���ʵ����Executorʵ������������������¼ִ��ʱ�䣬ȷ����ʱ���ܣ�
            for (int i = 0; i < size; i++) {
                execute((Runnable)futures.get(i));                    // ����ύִ�У�����ʣ��ʱ�䣬���ʣ��ʱ��<=0L�����أ���finally��ֹͣ��δִ�е�����
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
                        f.get(nanos, TimeUnit.NANOSECONDS);           // ��ʣ��ʱ��ȴ�������ɣ��������ִ�г���ʣ��ȴ�ʱ�䣬���߲�������ִ��ʱ���Ѿ������ȴ�ʱ�䣬���أ���finally��ֹͣ��δִ�е�����
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
                    futures.get(i).cancel(true);                      // ȡ�����񣨿�ȡ��������state == NEW������ͨ���ж�runner������
        }
    }

}
