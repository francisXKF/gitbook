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
 * ֧��ȡ�����첽���㡣
 * �������Futureʵ����һЩ������֧��������ȡ�����㣬��ѯ�����Ƿ���ɣ����ؼ�������
 * ֻ���ڼ������ʱ�����ܷ��ؽ����
 * get����������ֱ��������ɡ�
 * һ��������ɣ����㲻��������������ȡ�������Ǹü���ʹ��runAndReset���ã�
 *
 * <p>A {@code FutureTask} can be used to wrap a {@link Callable} or
 * {@link Runnable} object.  Because {@code FutureTask} implements
 * {@code Runnable}, a {@code FutureTask} can be submitted to an
 * {@link Executor} for execution.
 * FutureTask����������װ��wrap��Callable����Runnable����
 * ��ΪFutureTaskʵ����Runnable�ӿڣ����������ύ��Executor��ִ�У�ͨ��execute����ִ�У���
 *
 * <p>In addition to serving as a standalone class, this class provides
 * {@code protected} functionality that may be useful when creating
 * customized task classes.
 * ������Ϊ����ʹ�õ����⣬�����ṩ���ܱ����ķ������ڴ����Զ���������ʱ���ܺ����á�
 *
 * @since 1.5
 * @author Doug Lea
 * @param <V> The result type returned by this FutureTask's {@code get} methods
 * V��ʾget�����ķ���ֵ����
 */
public class FutureTask<V> implements RunnableFuture<V> {
    /*
     * Revision notes: This differs from previous versions of this
     * class that relied on AbstractQueuedSynchronizer, mainly to
     * avoid surprising users about retaining interrupt status during
     * cancellation races. Sync control in the current design relies
     * on a "state" field updated via CAS to track completion, along
     * with a simple Treiber stack to hold waiting threads.
     * �޶���revision��˵������֮ǰ����AQS�İ汾��ͬ����ҪΪ�˱����û�������ȡ������ʱ�Ա����ж�״̬���е����ȡ�
     * �ڵ�ǰ�����ͬ��������ͨ��CAS���µ�state�ֶ����������״̬���Լ��򵥵�Treiberջ������ȴ��̡߳�
     * ��Treiber Stack Algorithm��һ������չ������ջ������ϸ���ȵĲ���ԭ��CAS��ʵ�ֵģ�
     *
     * Style note: As usual, we bypass overhead of using
     * AtomicXFieldUpdaters and instead directly use Unsafe intrinsics.
     * ��ʽ˵����������һ�����ƹ�ʹ��AtomicXFieldUpdaters�Ŀ�����ֱ��ʹ��Unsafe�ڲ�������
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
     * �����ִ��״̬��state������ʼ����NEW��
     * ����״̬����set��setException��cancel�����л�ת��Ϊ��ֹ��terminal��״̬��
     * ������ڼ䣬״̬���ܲ���COMPLIETING�������ý��ʱ������INTERRUPTING�������ж����г���������ȡ��Ϊtrueʱ������˲ʱ̬��
     * ����Щ�м�̬������̬��ת����ʹ��cheaper����/����д�룬��Ϊֵ��Ψһ�Ĳ����޷���һ����further���޸ġ�
     *
     * Possible state transitions:
     * ���ܵ�״̬ת��
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
    // �ײ��callable��ִ����ɺ���Ϊnull��
    private Callable<V> callable;   // ����Ҫִ�е�����
    /** The result to return or exception to throw from get() */
    // ͨ��get()���صĽ�������׳����쳣
    private Object outcome; // non-volatile, protected by state reads/writes // ��volatile����state�Ķ�/д����
    /** The thread running the callable; CASed during run() */
    // ִ��callable���̣߳���run()�ڼ�CAS����
    private volatile Thread runner;   // ִ��������߳�
    /** Treiber stack of waiting threads */
    // Treiberջ����ĵȴ��߳�
    private volatile WaitNode waiters; // ����ָ���һ���ȴ��߳�WaitNode��û�еĻ�Ϊnull������ĵȴ��̣߳���ָ�ĵȴ���ȡ������̣߳�������ִ��������߳�

    /**
     * Returns result or throws exception for completed task.
     * ����ִ����ɵ����񣬷��ؽ�������׳��쳣
     *
     * @param s completed state value
     */
    @SuppressWarnings("unchecked")
    private V report(int s) throws ExecutionException {
        Object x = outcome;
        if (s == NORMAL) // ��state����NORMAL״̬��ֱ�ӷ���ִ�н��
            return (V)x;
        if (s >= CANCELLED) // ��state����CANCELLED��INTERRUPTING��INTERRUPTED״̬���׳���ȡ���쳣��
            throw new CancellationException();
        throw new ExecutionException((Throwable)x); // �ܵ������ʣEXCEPTIONAL�ˣ���Ϊֻ��state>COMPLETING�Ĳ��ܽ���÷�������Ҫע�⣬�����ִ�н����װ����Exception���׳���
                                                     // ������쳣������Ϊ��public ExecutionException(Throwable cause)->public Exception(Throwable cause)->public Throwable(Throwable cause) 
    }

    /**
     * Creates a {@code FutureTask} that will, upon running, execute the
     * given {@code Callable}.
     * FutureTask���캯������������ʱִ�и�����Callable��
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
     * FutureTask���캯������������ʱִ�и�����Runnable�����Ұ��ţ�arrange���ڳɹ���ɺ�get�������ظ�����result��
     *
     * @param runnable the runnable task
     * @param result the result to return on successful completion. If
     * you don't need a particular result, consider using
     * constructions of the form:
     * {@code Future<?> f = new FutureTask<Void>(runnable, null)}
     * result������ʾ�ɹ�ִ�к�ķ���ֵ���������Ҫ�ض���result������ʹ��������ʽ�Ľṹ��
     * Future<?> f = new FutureTask<Void>(runnable, null)
     * Void��java.lang.Void����һ������ʵ������ռλ���࣬���ڶ�void�ؼ��ֵ����á��������ʾ�޷���ֵ��
     *
     * @throws NullPointerException if the runnable is null
     */
    public FutureTask(Runnable runnable, V result) {
        this.callable = Executors.callable(runnable, result); // ʹ��Executors�����ཫRunnable����ΪCallable��Executors.callable->Executors#RunnableAdapter�ࣩ
        this.state = NEW;       // ensure visibility of callable
    }

    public boolean isCancelled() {
        return state >= CANCELLED; // ����CANCELLED��INTERRUPTING��INTERRUPTED
    }

    public boolean isDone() {
        return state != NEW; // ����NEW�������������˲ʱ̬����������̬��
    }

    // ���˽�NEW״̬ת��ΪINTERRUPTING/CANCELLED
    // ���߶���֧��mayInterruptedIfRunning�ģ�����NEW״̬ת��ΪINTERRUPTED
    public boolean cancel(boolean mayInterruptIfRunning) {
    	  // ���state=NEW������ֱ������state=INTERRUPTING������������õĻ�������������state=CANCELLED����ʾ����ȡ����������ֻ��������״̬������ȡ���ں��棩
        if (!(state == NEW &&
              UNSAFE.compareAndSwapInt(this, stateOffset, NEW,
                  mayInterruptIfRunning ? INTERRUPTING : CANCELLED))) // ���mayInterruptIfRunningΪtrue����ʾͨ���ж�ִ�и�������߳�������ֹͣ����ִ�У�����ֱ������state=INTERRUPTING
            return false;
        try {    // in case call to interrupt throws exception // ��������жϻ��׳��쳣
            if (mayInterruptIfRunning) { // �������ͨ���ж�ִ�и�������߳���ֹͣ����ִ��
                try {
                    Thread t = runner; // runner��ִ�и�callable���߳�
                    if (t != null)
                        t.interrupt(); // �жϸ��߳�
                } finally { // final state
                    UNSAFE.putOrderedInt(this, stateOffset, INTERRUPTED); // ��󣬽�state��Ϊ����̬INTERRUPTED�������࿪ʼ��״̬ת����ϵ��ֻ���Ǵ�˲ʱ̬INTERRUPTINGת��ΪINTERRUPTED��
                }
            }
        } finally {
            finishCompletion(); // ���ø÷����������еĵȴ��̶߳��������Ƴ�������runner�߳���waitnode�߳�û�й�ϵ��runner��ִ������callable�����̣߳�waitnode����߳������ȡ������߳�
        }
        return true;
    }

    /**
     * @throws CancellationException {@inheritDoc}
     */
    public V get() throws InterruptedException, ExecutionException {
        int s = state;                   // 1����ȡ��ǰִ�е�״̬state
        if (s <= COMPLETING)             // 2�����state<=COMPLETING����ʾ��ǰ״̬ΪNEW����COMPLETINGʱ
            s = awaitDone(false, 0L);    // 3���õ�ǰ��stateֵ�������ǰ�̱߳��ж��ˣ�ֱ���׳��쳣������������̬��>COMPLETING������stateֵ��������μ�����waiterNode����Ҫɾ���������=COMPLETING����ô�ó�CPUʱ��ȴ���ɣ�����������̬����ôpark�ȴ�
        return report(s);                // 4��state��ʾ���������ͷ���ʵ�ʽ��outcome�������CANCELLED��������INTERRUPT���׳�ȡ���쳣�������EXCEPTIONAL���׳���Ӧ���쳣��
    }

    /**
     * @throws CancellationException {@inheritDoc}
     * ������ʱ�ȴ���get����
     */
    public V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
        if (unit == null)               // �����ʱΪnull��ֱ���׳��쳣
            throw new NullPointerException();
        int s = state;
        if (s <= COMPLETING &&
            (s = awaitDone(true, unit.toNanos(timeout))) <= COMPLETING) // ���õȴ���ɵķ����������ʱ���ص�state����δ���״̬����ô���׳��쳣
            throw new TimeoutException();
        return report(s);               // ���򷵻ض�Ӧ��ִ�н��
    }

    /**
     * Protected method invoked when this task transitions to state
     * {@code isDone} (whether normally or via cancellation). The
     * default implementation does nothing.  Subclasses may override
     * this method to invoke completion callbacks or perform
     * bookkeeping. Note that you can query status inside the
     * implementation of this method to determine whether this task
     * has been cancelled.
     * �ܱ����ķ�����������ת��ΪisDone״̬��state!=NEW��ʱ���ã�������������������ͨ��ȡ������
     * Ĭ��ʵ��ʲô��������
     * ������Ը��Ǵ˷������������ʱ�ص�������callback������ִ�в��ǣ������Ǽ�¼��־����˼������
     * ע�⣬�����ڸ÷�����ʵ���в�ѯ״̬����ȷ���������Ƿ��Ѿ�ȡ����
     * ������Բ���ExecutorCompletionService���࣬�����ж�done�����������أ���¼��ɵ�task�б�
     *
     */
    protected void done() { }

    /**
     * Sets the result of this future to the given value unless
     * this future has already been set or has been cancelled.
     * ���ø�future�Ľ��Ϊ������ֵ�����Ǹ�future�Ѿ������ù������Ѿ���ȡ����
     *
     * <p>This method is invoked internally by the {@link #run} method
     * upon successful completion of the computation.
     * ������ɹ�������ʱ�򣬸÷�����run�����ڲ����á�
     *
     * @param v the value
     */
    protected void set(V v) {
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) { // ��state��NEW����ΪCOMPLETING������Ѿ����ù��ˣ���ô�Ͳ����ظ����ã�
            outcome = v;
            UNSAFE.putOrderedInt(this, stateOffset, NORMAL); // final state // ��NORMAL����˲ʱ̬
            finishCompletion();                                              // �ͷ����������ȴ�ִ�н���ĵȴ��̣߳���������awaitDone�����������ˣ������������õ�������ء�
        }
    }

    /**
     * Causes this future to report an {@link ExecutionException}
     * with the given throwable as its cause, unless this future has
     * already been set or has been cancelled.
     * ��futureʹ�ø�����throwable��Ϊԭ���ϱ�ExecutionException�����Ǹ�future�Ѿ������ù����߱�ȡ����
     *
     * <p>This method is invoked internally by the {@link #run} method
     * upon failure of the computation.
     * ������ʧ�ܵ�ʱ�򣬸÷�����run�����ڲ����á�
     *
     * @param t the cause of failure // t��ʾʧ��ԭ��
     */
    protected void setException(Throwable t) {
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) { // ��state��NEW����ΪCOMPLETING������Ѿ����ù��ˣ���ô�Ͳ����ظ����ã�
            outcome = t;                                                     // ���ý��Ϊ�쳣ԭ��
            UNSAFE.putOrderedInt(this, stateOffset, EXCEPTIONAL); // final state // ��EXCEPTIONAL����˲ʱ̬
            finishCompletion();                                              // �ͷ����������ȴ�ִ�н���ĵȴ��߳�
        }
    }

    // run()�ǲ����ؽ���ģ������Ҫͨ��get()������ȡ
    public void run() {
        if (state != NEW ||
            !UNSAFE.compareAndSwapObject(this, runnerOffset,
                                         null, Thread.currentThread())) // ���state����NEW��������runnerΪ��ǰ�߳�ʧ�ܣ�ֱ�ӷ��أ��������߳�������runner��
            return;
        try {
            Callable<V> c = callable;
            if (c != null && state == NEW) { // �ٴ��ж�state
                V result;
                boolean ran;
                try {
                    result = c.call(); // �ȴ�ִ�����
                    ran = true;
                } catch (Throwable ex) {
                    result = null;
                    ran = false;
                    setException(ex); // ��outcome����Ϊ��Ӧ���쳣
                }
                if (ran)
                    set(result); // ��outcome����Ϊִ�н��
            }
        } finally {
            // runner must be non-null until state is settled to
            // prevent concurrent calls to run()
            // runner���벻Ϊnull��ֱ��state�ȶ���settled �̶��ģ����Է�ֹ��prevent����������run()����������null�Ļ�run�����Ϳɱ���������ִ�У�
            runner = null;
            // state must be re-read after nulling runner to prevent
            // leaked interrupts
            // ��runner����Ϊnull�󣬱������¶�ȡstate���Է�ֹй¶�жϡ�������ִ����������һ��֮��������¶�ȡstate��
            int s = state;
            if (s >= INTERRUPTING)
                handlePossibleCancellationInterrupt(s); // �����finally����Ϊcancel(true)������жϣ���ô�ȴ��ж���ɡ�
        }
    }

    /**
     * Executes the computation without setting its result, and then
     * resets this future to initial state, failing to do so if the
     * computation encounters an exception or is cancelled.  This is
     * designed for use with tasks that intrinsically execute more
     * than once.
     * ִ�м����ε������ý����Ȼ������futureΪ��ʶ״̬��
     * �������������encounter���쳣���߱�ȡ�������޷���ô�������޷�����Ϊ��ʼֵ��
     * ������ڱ����ϣ�intrinsically��ִ�ж�ε�����
     *
     * @return {@code true} if successfully run and reset
     */
    protected boolean runAndReset() {
        if (state != NEW ||
            !UNSAFE.compareAndSwapObject(this, runnerOffset,
                                         null, Thread.currentThread())) // ���state����NEW��������runnerΪ��ǰ�߳�ʧ�ܣ�ֱ�ӷ��أ��������߳�������runner��
            return false;
        boolean ran = false;
        int s = state;
        try {
            Callable<V> c = callable;
            if (c != null && s == NEW) {
                try {
                    c.call(); // don't set result // �����ý��
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
        return ran && s == NEW; // ���ִ�гɹ�������true�������û�����쳣���쳣���������棬һ����ִ��c.call��ʱ�����쳣��һ���ǵ�ǰrunner��cancel���쳣��
    }

    /**
     * Ensures that any interrupt from a possible cancel(true) is only
     * delivered to a task while in run or runAndReset.
     * ȷ����������cancel(true)���κ��жϣ�����������run����runAndResetʱ���ݸ�����
     * ֻ��һ���ط�������stateΪINTERRUPTING״̬�����ǵ���cancel(true)��ʱ��
     * ����cancel(true)���������state==NEW����ô������ΪINTERRUPTING��ֱ��runner.interrupt()ִ����ϲ�����INTERRUPTING״̬ΪINTERRUPTED״̬��
     *
     */
    private void handlePossibleCancellationInterrupt(int s) {
        // It is possible for our interrupter to stall before getting a
        // chance to interrupt us.  Let's spin-wait patiently.
        // �������ǵ�interrupter�ڻ�ȡ�����ж�����֮ǰ��ֹͣ��ֻ��Ҫ���ĵģ�patiently�������ȴ���
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
        // ���������ͨ��cancel(true)���ܻ�õ��κ��жϡ�
        // Ȼ��������ʹ���ж���Ϊ�������������֮���ͨ�ţ�communicate���Ķ������ƣ�java�Ļ������ƣ���
        // ����û�취��ȡ��cancel�жϡ�
        //
        // Thread.interrupted();
    }

    /**
     * Simple linked list nodes to record waiting threads in a Treiber
     * stack.  See other classes such as Phaser and SynchronousQueue
     * for more detailed explanation.
     * �򵥵�����ڵ㣬���ڼ�¼Treiber stack��ȴ��̡߳�
     * �йظ���ϸ��˵�������Բ��������࣬����Phaser��SynchronousQueue
     *
     */
    static final class WaitNode {
        volatile Thread thread; // ���浱ǰ�̣߳���ǰ�߳���һ���ȴ���ȡִ�н�����̣߳�����ִ��������̣߳�
        volatile WaitNode next; // ����ָ����һ���ȴ��߳�WaitNode
        WaitNode() { thread = Thread.currentThread(); } // ���̷߳�װ��WaitNode
    }

    /**
     * Removes and signals all waiting threads, invokes done(), and
     * nulls out callable.
     * �Ƴ��ͻ������еȴ�����Ҫ�õ�ִ�н���ģ��̣߳�����done()�������������ö�������Ϊnull��
     *
     */
    private void finishCompletion() {
        // assert state > COMPLETING; // ���assert������ע����
        for (WaitNode q; (q = waiters) != null;) { // ����Treiber stack�ṹ�ĵȴ��̶߳��У���ʵ�Ǹ�ջ���������ǲ��ϵĴӵ�һ��WaitNode��ͷ����ʼ������
            if (UNSAFE.compareAndSwapObject(this, waitersOffset, q, null)) { // ��q��Ϊnull
                for (;;) {
                    Thread t = q.thread;
                    if (t != null) {
                        q.thread = null;
                        LockSupport.unpark(t); // ���ѵ�ǰWaitNode���߳�
                    }
                    WaitNode next = q.next;
                    if (next == null) // ����ýڵ��nextΪnull���˳�����ѭ����˵����һ�ζԵȴ��̵߳ı�������ˣ��ѻ������еȴ��̣߳�
                        break;
                    q.next = null; // unlink to help gc // ��next��ϣ�����GC
                    q = next; // ��q��Ϊ��һ��waitNode����������
                }
                break;
            }
        }

        done(); // �����ǿգ�������Ը�����Ҫ�Զ���

        callable = null;        // to reduce footprint // �����㼣������
    }

    /**
     * Awaits completion or aborts on interrupt or timeout.
     * �ȴ���ɣ����������жϻ��߳�ʱ���µ���ֹ��
     *
     * @param timed true if use timed waits
     *        timed true ���ʹ����ʱ�ȴ�
     * @param nanos time to wait, if timed
     *        nanos ʱ��ֵ �����Ҫ��ʱ�ȴ�
     * @return state upon completion
     *         ��ɣ����߳�ʱ���󷵻�״̬���ж��׳��쳣��
     */
    private int awaitDone(boolean timed, long nanos)
        throws InterruptedException {
        final long deadline = timed ? System.nanoTime() + nanos : 0L;      // 1�������Ҫ��ʱ�ȴ��������ֹʱ��
        WaitNode q = null;                                                  // 2������һ���ȴ��߳̽ڵ㣬�ȴ�ִ�н��
        boolean queued = false;                                            // queued��ʾ��ǰ�ȴ��ڵ��Ƿ�����Treiber stack�ȴ����У���ʵ�Ǹ�ջ����
        for (;;) {
            if (Thread.interrupted()) {                                     // �����ǰ�̷߳����жϣ��Ƴ��ȴ�������߳̽ڵ㣬�׳��쳣
                removeWaiter(q);                                            // ΪʲôҪ������أ���Ϊq�����ڵȴ�ջ�Ҫ�ӵȴ�ջ����Ƴ�
                throw new InterruptedException();
            }

            int s = state;
            if (s > COMPLETING) {                                            // ���state״̬��Ϊ���̬��������ɡ��жϡ�ȡ����
                if (q != null)                                               //    ����ȴ��ڵ㲻Ϊnull�����ȴ��ڵ���߳���Ϊnull���൱�ڴ����ǣ�Ϊ�Ժ�removeWaiter()��ʱ�����ɾ������Ȼ�����ǲ���ȥ����removeWaiter()�ˣ�
                    q.thread = null;
                return s;                                                    // ����ִ��״̬
            }
            else if (s == COMPLETING) // cannot time out yet
                Thread.yield();                                               // ������ɵ�˲ʱ̬����ʾ��������ǰ�߳̿�����ʱ����CPU���ȣ����ٱ�����ʱ�����߽�park��֧��ֱ���ж�state�Ƿ�Ϊ���̬��
            else if (q == null)
                q = new WaitNode();                                           // ��һ��û���õ����̬������һ���ȴ��ڵ�
            else if (!queued)                                                 // ����ȴ��ڵ�û�ڵȴ����У���ʵ�Ǹ�ջ���У��������
                queued = UNSAFE.compareAndSwapObject(this, waitersOffset,
                                                     q.next = waiters, q);     // ����ջ����ջ�����õ�ǰ�ڵ�nextָ��ԭջ��Ԫ�أ�Ȼ�󽫵�ǰ�ڵ���ջ
            else if (timed) {                                                 // �����Ҫ��ʱ���ж��Ƿ�ȴ���ʱ����ʱ�ĵȴ����ӵȴ�ջ���Ƴ�
                nanos = deadline - System.nanoTime();
                if (nanos <= 0L) {
                    removeWaiter(q);
                    return state;
                }
                LockSupport.parkNanos(this, nanos);                          // ��Ҫ��ʱ�����û��ʱ������parkʱ�俪ʼ����
            }
            else
                LockSupport.park(this);                                      // ��һ���鷢������û����ɣ���ʼpark������ʲôʱ��ỽ���أ�������
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
     * ����ȡ����ʱ�����жϵĵȴ��ڵ�����ӣ������������ۡ�
     * �ڲ��ڵ���û��CAS�������ֻ�ǣ�simply ֻ�ǣ��򵥵ģ��𿪣���Ϊ���۷�������ô�������Ƕ����޺��ġ�
     * Ϊ�˱�����Ѿ��Ƴ��Ľڵ�𿪵�Ӱ�죬��������Ծ���������½����б���ݡ�
     * ����̫��ڵ��ʱ������������ϣ���б��㹻���Գ������߿����ķ�����
     * ������ͨ��ȡ�����õĽڵ㣬�������б��ȣ�ͬʱ�����������Ƴ��Ľڵ㵼�����Ӷ��ˣ���Ȼ���̵Ĺ�����Ҳ�����ʱ�����ģ�
     */
    private void removeWaiter(WaitNode node) {
        if (node != null) {
            node.thread = null;
            retry:
            for (;;) {          // restart on removeWaiter race // ���о���ʱ���¿�ʼ
                for (WaitNode pred = null, q = waiters, s; q != null; q = s) { // ��ʼʱq��Treiber�ȴ�ջ��ͷԪ��
                    s = q.next;
                    if (q.thread != null) // ��node�ĵȴ��̲߳�Ϊnull����ô�ƶ�predָ���node����ʾ��node����ɾ����
                        pred = q;
                    else if (pred != null) { // �����node�ĵȴ��߳�Ϊnull����ôɾ����node���������Ϊ�ø�node��ǰ��.nextָ���node�ĺ�̣�
                        pred.next = s;
                        if (pred.thread == null) // check for race // �������ǰ����threadҲ��null�ˣ�˵��ǰ��ҲӦ��ɾ���ˣ���ô�����±����ȴ�ջ��
                            continue retry;
                    }
                    // �������������q.thread == null && pred == null�����ʾpred��û�ҵ�������null�Ĳ���q��ͷ�ڵ㣨��ʵq���ǿ�ʼ��ͷ��㣬�ǲ���ͨ�������CAS���õ�ͷ��㣨ԭ����ͷ����˳��ɾ�ˣ���
                    else if (!UNSAFE.compareAndSwapObject(this, waitersOffset,
                                                          q, s)) // ��ͷ����next�滻ͷ��㣬����滻ʧ���ˣ�˵�������о�����ӵģ��ʹ�ͷ���±���
                        continue retry;
                }
                break;
            }
        }
    }

    // Unsafe mechanics
    // һ��ʹ��Unsafeʵ�ֵ�CAS
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
