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
 * һ��ExecutorService��ʹ�ÿ��ܵļ����̳߳�֮һִ���ύ������
 * ͨ��ʹ��Executors�������������á�
 * ��ͨ����Executors�������������ɸ���ʵ������������̳߳ء��߳����ȣ��������ڲ��Ƽ�ʹ�ã�
 *
 * <p>Thread pools address two different problems: they usually
 * provide improved performance when executing large numbers of
 * asynchronous tasks, due to reduced per-task invocation overhead,
 * and they provide a means of bounding and managing the resources,
 * including threads, consumed when executing a collection of tasks.
 * Each {@code ThreadPoolExecutor} also maintains some basic
 * statistics, such as the number of completed tasks.
 * �̳߳ؽ����address��������ͬ�����⣺
 * 1��ͨ������ÿ������ĵ��ÿ�����ͨ����ִ�д����첽����ʱ�ṩ��Ч��improved�����ܣ�performance����
 * 2���ṩ���ƣ�bounding���͹�����Դ�ķ���������ִ�����񼯺�ʱ���ĵ��̡߳�
 * ÿ��ThreadPoolExecutorҲά����maintain��һЩ������ͳ����Ϣ����������������������
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
 * Ϊ���ڹ㷺�������������ã������ṩ�����ɵ����Ĳ����Ϳ���չ�Ĺ��ӷ�����hooks����
 * ���ǣ�ǿ�ҽ��飨urge �ش٣�����Աʹ�ø�����ģ�convenient��Executors�ṩ�Ĺ���������
 * Executors#newCachedThreadPool���޽��̳߳أ������Զ��̻߳��գ�reclamation��
 * Executors#newFixedThreadPool���̶���С�̳߳�
 * Executors#newSingleThreadExecutor��������̨�߳�
 * ����Ԥ�������������ʹ�ó�����scenarios����
 * �������ֹ���manually�������������tuning������ʱ��ʹ������ָ�ϣ�
 *
 * <dl>
 *
 * <dt>Core and maximum pool sizes</dt>
 * ���ĺ���󣨳��У��߳���
 *
 * <dd>A {@code ThreadPoolExecutor} will automatically adjust the
 * pool size (see {@link #getPoolSize})
 * according to the bounds set by
 * corePoolSize (see {@link #getCorePoolSize}) and
 * maximumPoolSize (see {@link #getMaximumPoolSize}).
 * ThreadPoolExecutor������corePoolSize��getCorePoolSize����maximumPoolSize��getMaximumPoolSize�����õı߽磬
 * �Զ������ش�С����getPoolSize��ѯ��
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
 * ��ʹ��execute(Runnable)�ύ����
 * 1�������ǰ���е��߳���С��corePoolSizeʱ���µ��̱߳�������������������񣩣���ʹ�����߳̿��С�
 * 2�������ǰ���е��߳�������corePoolSize��С��maximumPoolSizeʱ��ֻ���ڶ��У��ȴ�����Ķ��У�����ʱ��Żᴴ���µ��̡߳�
 * ͨ������corePoolSize����maximumPoolSize���ʹ�����һ���̶���С���̳߳ء�
 * ͨ������maximumPoolSize����Ϊһ�������ϣ�essentially���޽�ֵ������Integer.MAX_VALUE�������������̳߳���Ӧ��accommodate ���ɣ����⣨arbitrary�������Ĳ����̡߳�
 * ����͵��ǣ����ĺ�����̳߳ش�Сֻ�ڹ���ʱ���ã���Ҳ������ͬsetCorePoolSize��setMaximumPoolSize����̬���ġ�
 *
 * <dt>On-demand construction</dt>
 * ���蹹��
 *
 * <dd>By default, even core threads are initially created and
 * started only when new tasks arrive, but this can be overridden
 * dynamically using method {@link #prestartCoreThread} or {@link
 * #prestartAllCoreThreads}.  You probably want to prestart threads if
 * you construct the pool with a non-empty queue. </dd>
 * Ĭ������£���ʹ�����߳�Ҳ�����µ����񵽴�ʱ�ų�ʼ��������������������ʹ��prestartCoreThread����prestartAllCoreThreads������̬���ǣ���ģʽ��
 * ���ʹ�÷ǿն��й����̳߳�ʱ��������Ԥ�����̡߳�
 *
 * <dt>Creating new threads</dt>
 * �������߳�
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
 * ʹ��ThreadFactory�����µ��̡߳�
 * ���û������ָ����ʹ��Executors#defaultThreadFactory�����д������߳�ʹ����ͬ��ThreadGroup����ͬ��NORM_PRIORITY���ȼ��ͷ��ػ��Ľ���״̬��
 * ͨ���ṩ��ͬ��ThreadFactory�����Ը����߳������߳��顢���ȼ����ػ�����״̬�ȡ�
 * ����ڵ���ThreadFactory��newThread��������nullʱ���򴴽��߳�ʧ�ܣ�executor���������У��������޷�ִ���κ�����
 * �߳�Ӧ��ӵ��modifyThread��RuntimePermission������ʱȨ�ޣ���
 * ���ʹ���̳߳ص�worker�̻߳��������߳�û��ӵ�и�Ȩ�ޣ��������ܻᱻ������degrade����
 * 1�������޸Ŀ����޷���ʱ��Ч
 * 2���ر��̳߳ؿ���ͣ���ڿ�����ֹ��δ���״̬
 *
 * <dt>Keep-alive times</dt>
 * ���ֻ�Ծ��ʱ��
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
 * �����ǰ�̳߳����߳�������corePoolSize��������߳̽��ڿ���ʱ�䳬��KeepAliveTime����getKeepAliveTime(TimeUnit)��ʱ����ֹ��
 * ���ṩ��һ�ַ��������̳߳�δ������õ�����¼�����Դ�����ģ�consumption����
 * ����̳߳��Ժ��û�Ծ��ʹ��Ƶ�ʱ�ߣ����򽫹����µ��̡߳�
 * �ò�������ʹ��setKeepAliveTime(long, TimeUnit)������̬�޸ġ�
 * ʹ��Long.MAX_VALUE TimeUnit#NANOSECONDSֵ������Ч��ֹ�����߳����̳߳عر�֮ǰ����ֹ��
 * Ĭ������£�keep-alive���Խ������̳߳���corePoolSize��ʱ��Ż����á�
 * ����allowCoreThreadTimeOut(boolean)�������������ں����̵߳�time-out���ԣ�ǰ����keepAliveTimeֵ��Ϊ0��
 *������Ĭ������£�keepAliveTime���ܿ��ƷǺ����̵߳Ĵ��ʱ�䣬allowCoreThreadTimeOut�������Կ��ƽ�keepAliveTime���ں����̣߳�
 *
 * <dt>Queuing</dt>
 * ����
 *
 * <dd>Any {@link BlockingQueue} may be used to transfer and hold
 * submitted tasks.  The use of this queue interacts with pool sizing:
 * ����BlockingQueue���������ڴ��ݺͱ����ύ������
 * ʹ�øö������̳߳ش�С�������£�
 *
 * <ul>
 *
 * <li> If fewer than corePoolSize threads are running, the Executor
 * always prefers adding a new thread
 * rather than queuing.</li>
 * ��������߳��� < corePoolSize����ôExecutor����ϲ����prefer��������̶߳�������ӡ�
 *
 * <li> If corePoolSize or more threads are running, the Executor
 * always prefers queuing a request rather than adding a new
 * thread.</li>
 *  ��������߳��� >= coolPoolSize��Executor����ϲ����������Ӷ�����������̡߳�
 *
 * <li> If a request cannot be queued, a new thread is created unless
 * this would exceed maximumPoolSize, in which case, the task will be
 * rejected.</li>
 * ��������޷���ӣ��������̣߳������߳�����������exceed��maximumPoolSize������������£������񽫱��ܾ���reject��
 *
 * </ul>
 *
 * There are three general strategies for queuing:
 * ���ڶ��е����ֳ������ԣ�
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
 * Direct handoffs��ֱ�ӽ���/ֱ�����֣���
 * ��������һ���õ�Ĭ��ѡ����SynchronousQueue���������ύ��hand off�����̶߳�����������ʽ��������
 * ��������û���߳̿�����ִ�����񣬳��Խ�������ӻ�ʧ�ܣ����Խ������µ��̡߳�
 * �ò��Ա�������������������󼯿��ܺ����ڲ�������ʱ��
 * Direct handoffsͨ����ҪmaximumPoolSize���޽�ģ��Ա������ύ�����񱻾ܾ���
 * �ⷴ������turn���ֳ��ϣ�admit���˴������ֿ����ԣ���commands���������ƽ���ٶȱȴ����ٶȿ�ʱ���߳̽�����������
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
 * Unbounded queues���޽���У�
 * ʹ���޽���У�����û��Ԥ������������LinkedBlockingQueue��������������corePoolSize���̶߳�æ��ʱ���������ڶ����еȴ���
 * ��ˣ��������߳������ᳬ��corePoolSize�������ң�maximumPoolSizeֵ��˲������κ�Ӱ�죨ûɶ�ã���
 * ����������ڣ�ÿ��������ȫ������������ÿ�����񲻻�Ӱ��˴˵�ִ�У�
 * ���磬����ҳ�����У��������ַ��Ķ��ж���ƽ������ͻ����������ã�����commands��������ƽ���ٶȳ����������ٶ�ʱ���ᵼ�¹�����������������
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
 * Bounded queues���н���У�
 * �н���У�����ArrayBlockingQueue���������޵ģ�finite��maximumPoolSizeһ��ʹ��ʱ�������ڱ�����Դ�ľ���exhaustion�������������Ե����Ϳ��ơ�
 * ���д�С������̳߳ش�С���ܻ���Ӱ�죨traded off ���У���
 * ʹ�ô���к�С�̳߳���������޶ȵļ���CPUʹ���ʡ�OS��Դ���������л����������ǻ���Ϊ�ģ�artificially��������������
 * �������Ƶ����frequently������������������I/O��������������߳����ȣ�ϵͳ�����ܹ�����ʱ���������̡߳�
 * �����Ƕ���I/O�ܼ��͵�������ôϵͳ�������ø�����߳���ʹ��CPU����Դ��������õ��̳߳�����С���ͻᵼ�����������ͣ�
 * ʹ��С����ͨ����Ҫ���̳߳��������ڱ���CPU��æ�����ǿ��ܻ��������ɽ��ܵĵ��ȿ�������Ҳ�ή����������
 * �����Ƕ���CPU�ܼ�����������̹߳��࣬��Ƶ�������̵߳����������л�������ĵ��ȿ�����
 *
 * </ol>
 *
 * </dd>
 *
 * <dt>Rejected tasks</dt>
 * �ܾ�����
 *
 * <dd>New tasks submitted in method {@link #execute(Runnable)} will be
 * <em>rejected</em> when the Executor has been shut down, and also when
 * the Executor uses finite bounds for both maximum threads and work queue
 * capacity, and is saturated.  In either case, the {@code execute} method
 * invokes the {@link
 * RejectedExecutionHandler#rejectedExecution(Runnable, ThreadPoolExecutor)}
 * method of its {@link RejectedExecutionHandler}.  Four predefined handler
 * policies are provided:
 * ͨ��execute(Runnable)�����ύ��������ܱ��ܾ���1����Executor�Ѿ����رգ�2�����ߵ�Executor������߳����빤����������ʹ���н����ƴﵽ���ͣ�saturated����
 * �������ֳ�����execute�����������RejectedExecutionHandler��RejectedExecutionHandle#rejectedException(Runnable, ThreadPoolExecutor)������
 * ֧������Ԥ�����handler���ԣ�
 *
 * <ol>
 *
 * <li> In the default {@link ThreadPoolExecutor.AbortPolicy}, the
 * handler throws a runtime {@link RejectedExecutionException} upon
 * rejection. </li>
 * �������������ԣ��׳��쳣��
 * Ĭ�ϲ���ΪThreadPoolExecutor.AbortPolicy���ô������ھܾ�ʱ�׳�����ʱRejectedExecutionException
 *
 * <li> In {@link ThreadPoolExecutor.CallerRunsPolicy}, the thread
 * that invokes {@code execute} itself runs the task. This provides a
 * simple feedback control mechanism that will slow down the rate that
 * new tasks are submitted. </li>
 * ��������ִ�в��ԣ�
 * ��ThreadPoolExecutor.CallerRunsPolicy������execute���߳��Լ�ִ�и����񣨱��̳߳ؾܾ������񣩡�
 * ���ṩ��һ�ּ򵥵ķ�����feedback�����ƻ��ƣ����Լ����ύ��������ٶȣ�rate����
 *
 * <li> In {@link ThreadPoolExecutor.DiscardPolicy}, a task that
 * cannot be executed is simply dropped.  </li>
 * ��������ֱ�Ӷ������ԣ�
 * ��ThreadPoolExecutor.DiscardPolicy���޷�ִ�е�����ֻ�Ǽ򵥵Ķ���������ִ��Ҳ���׳��쳣��
 * ��discard ������
 *
 * <li>In {@link ThreadPoolExecutor.DiscardOldestPolicy}, if the
 * executor is not shut down, the task at the head of the work queue
 * is dropped, and then execution is retried (which can fail again,
 * causing this to be repeated.) </li>
 * ���������������ԣ�
 * ��ThreadPoolExecutor.DiscardOldestPolicy�����ִ����û�б��رգ��ڹ������ж��׵�����������Ȼ�����ԣ�retried��ִ�У��п����ٴ�ʧ�ܣ����¸ò��������ظ�ִ�У�
 *
 * </ol>
 *
 * It is possible to define and use other kinds of {@link
 * RejectedExecutionHandler} classes. Doing so requires some care
 * especially when policies are designed to work only under particular
 * capacity or queuing policies. </dd>
 * ���Զ����ʹ���������͵�RejectedExceptionHandler�ࡣ
 * ��������Ҫ�����������ǵ�������ƽ����ض���particular�����������ŶӲ����¹���ʱ��
 *
 * <dt>Hook methods</dt>
 * ���Ӻ���
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
 * �����ṩ�ܱ����Ŀɸ��ǵ�beforeExecute(Thread, Runnable)��afterExecute(Runnable, Throwable)��������ÿ������ִ��֮ǰ��֮�󱻵��á�
 * ��Щ�������ڲ�����manipulate��ִ�л��������磬���³�ʼ��Threadlocals���ռ���gather��ͳ����Ϣ�����������־��Ŀ��entries����
 * ���⣬���Ը���terminated��������Executor��ȫ��fully����ֹ��ִ�У�perform����Ҫ�������⴦��
 * ���ἰ�������ӷ�����beforeExecute��afterExecute��terminated��
 *
 * <p>If hook or callback methods throw exceptions, internal worker
 * threads may in turn fail and abruptly terminate.</dd>
 * ���hook���߻ص���callback�������׳��쳣���ڲ�worker�߳̿��ܷ�����ʧ�ܲ���ͻȻ��abruptly����ֹ��
 * �����ǹ��ӷ������߻ص������׳��쳣�Ļ�����Ӱ���worker�߳�ʧ������ֹ��
 *
 * <dt>Queue maintenance</dt>
 * ����ά��
 *
 * <dd>Method {@link #getQueue()} allows access to the work queue
 * for purposes of monitoring and debugging.  Use of this method for
 * any other purpose is strongly discouraged.  Two supplied methods,
 * {@link #remove(Runnable)} and {@link #purge} are available to
 * assist in storage reclamation when large numbers of queued tasks
 * become cancelled.</dd>
 * getQueue()����������ʸ��̶߳��У����ڼ�غ�debugĿ�ġ�
 * ǿ�ҽ��鲻Ҫ��discouraged �����飩���÷�����������Ŀ�ġ�
 * �ṩ����������remove(Runnable)��purge���ڴ�����ӵ�����ȡ��ʱ��������Э���洢���գ�reclamation����
 *
 * <dt>Finalization</dt>
 * ���壿����
 *
 * <dd>A pool that is no longer referenced in a program <em>AND</em>
 * has no remaining threads will be {@code shutdown} automatically. If
 * you would like to ensure that unreferenced pools are reclaimed even
 * if users forget to call {@link #shutdown}, then you must arrange
 * that unused threads eventually die, by setting appropriate
 * keep-alive times, using a lower bound of zero core threads and/or
 * setting {@link #allowCoreThreadTimeOut(boolean)}.  </dd>
 * �����в��ٱ����ò���û��ʣ�ࣨremain���̵߳��̳߳ؽ����Զ��رա�
 * �������ȷ��δ���õ��̳߳ر����գ���ʹʹ�������ǵ���shutdown��
 * ��ô����밲��δʹ�õ��߳����գ�eventually��������ͨ�������ʵ��ģ�appropriate��keep-aliveʱ�䡢ʹ��0�����߳��������޲���/��������allowCoreThreadTimeOut(boolean)��
 * ��corePoolSizeΪ0ʱ��allowCoreThreadTimeOut�����������ɲ��裬corePoolSize��Ϊ0ʱ�������裩
 * ������̳߳���û�д���̣߳������̳߳�û�б����ã���ô�̳߳ض���ͻᱻ�Զ����գ�
 *
 * </dl>
 *
 * <p><b>Extension example</b>. Most extensions of this class
 * override one or more of the protected hook methods. For example,
 * here is a subclass that adds a simple pause/resume feature:
 * ��չ������������������չ�඼�Ḳ��һ�������ܱ�����hook������
 * ���磬�����������������һ���򵥵���ͣ/�ָ����ܣ�feature ���ԣ���
 *
 *  <pre> {@code
 * class PausableThreadPoolExecutor extends ThreadPoolExecutor {
 *   private boolean isPaused;
 *   private ReentrantLock pauseLock = new ReentrantLock();  // ����İ���Ļ������ȿ���Lock��Condition��AQS��ReentrantLock����Դ��
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
     * ctl���̳߳���Ҫ�Ŀ���״̬����һ��ԭ��integer�������������conceptual���ֶ�
     *   workerCount��������Ч���߳���
     *   runState��   �����̳߳�״̬�Ƿ�Ϊ���С��رյ�
     *
     * In order to pack them into one int, we limit workerCount to
     * (2^29)-1 (about 500 million) threads rather than (2^31)-1 (2
     * billion) otherwise representable. If this is ever an issue in
     * the future, the variable can be changed to be an AtomicLong,
     * and the shift/mask constants below adjusted. But until the need
     * arises, this code is a bit faster and simpler using an int.
     * Ϊ�˽��������ֶδ����һ��int����workerCount����Ϊ(2^29)-1���̣߳�������(2^31)-1�������ɱ�ʾ���̡߳�
     * ������ڽ�����Ϊ���⣬�ñ�����variable��ctl���Ը�ΪAtomicLong���ͣ��������������λ/���볣����������ָ��COUNT_BITS��
     * ��������Ҫ֮ǰ��arises ���֣�����δ���ʹ��int�������򵥡�
     *
     * The workerCount is the number of workers that have been
     * permitted to start and not permitted to stop.  The value may be
     * transiently different from the actual number of live threads,
     * for example when a ThreadFactory fails to create a thread when
     * asked, and when exiting threads are still performing
     * bookkeeping before terminating. The user-visible pool size is
     * reported as the current size of the workers set.
     * workerCount��ʾ�Ѿ�������������δ����ֹͣ��worker������
     * ���ֵ���ܺ�ʵ�ʻ�߳�����ʱ��transiently����һ�£�������֤һ������ΪworkerCount-1������workers.remove(w)����һ��Lock���µ�ͬ��������
     * ���磬��ThreadFactory�����߳�ʧ�ܣ����˳��߳�����ֹǰ�Ծ�ִ��bookkeeping��
     * �û��ɼ����̳߳ش�С����worker����ǰ��С��
     *
     * The runState provides the main lifecycle control, taking on values:
     * runState�ṩ��Ҫ���������ڿ��ƣ�ȡֵ���£�
     *
     *   RUNNING:  Accept new tasks and process queued tasks
     *             ���������񲢴�������е�����
     *   SHUTDOWN: Don't accept new tasks, but process queued tasks
     *             �����������񣬵���������е�����
     *   STOP:     Don't accept new tasks, don't process queued tasks,
     *             and interrupt in-progress tasks
     *             �����������񣬲���������е�����
     *             �����ж����ڴ��������
     *   TIDYING:  All tasks have terminated, workerCount is zero,
     *             the thread transitioning to state TIDYING
     *             will run the terminated() hook method
     *             ���������Ѿ���ֹ��workerCountΪ0��
     *             �߳�ת��ΪTIDYING״̬��TIDYING ����
     *             ��ִ��terminated()���ӷ���
     *   TERMINATED: terminated() has completed
     *             terminated()����ִ�����
     *             
     *
     * The numerical order among these values matters, to allow
     * ordered comparisons. The runState monotonically increases over
     * time, but need not hit each state. The transitions are:
     * ��ֵ��ָ��runState��֮������֣�numerical��˳�����Ҫ��matter�����������������Ƚϡ�
     * runState����ʱ�䵥����monotonically��������������Ҫ����ÿ��״̬�����ǲ���Ҫ��˳����������粻��Ҫ-1��0��1��2��3��������������Ϊ-1��1������ֻҪ�����������У�
     * �����ǣ�
     *
     * RUNNING -> SHUTDOWN
     *    On invocation of shutdown(), perhaps implicitly in finalize()
     *    ��RUNNINGת��ΪSHUTDOWN���ڵ���shutdown()ʱ�����ܣ�perhaps�����أ�implicitly����finalize()�����У�finalize()�л�ִ��shutdown()��
     * (RUNNING or SHUTDOWN) -> STOP
     *    On invocation of shutdownNow()
     *    ��RUNNING����SHUTDOWN��ת��ΪSTOP���ڵ���shutdownNow()��ʱ��ת��ΪSTOP״̬��remove����������������񣿣�����
     * SHUTDOWN -> TIDYING
     *    When both queue and pool are empty
     *    SHUTDOWNת��ΪTIDYING�����������̳߳ض�Ϊ��ʱ
     * STOP -> TIDYING
     *    When pool is empty
     *    STOPת��ΪTIDYING�����̳߳�Ϊ��ʱ
     * TIDYING -> TERMINATED
     *    When the terminated() hook method has completed
     *    TIDYINGת��ΪTERMINATED����terminated()���ӷ���ִ�����
     *
     * Threads waiting in awaitTermination() will return when the
     * state reaches TERMINATED.
     * ��awaitTermination()�����ϵȴ����̣߳�����״̬����TERMINATEDʱ���ء�
     *
     * Detecting the transition from SHUTDOWN to TIDYING is less
     * straightforward than you'd like because the queue may become
     * empty after non-empty and vice versa during SHUTDOWN state, but
     * we can only terminate if, after seeing that it is empty, we see
     * that workerCount is 0 (which sometimes entails a recheck -- see
     * below).
     * ��⣨detecting����SHUTDOWN��TIDYING��ת�䲢��������Ҫ����ôֱ�ӣ�straightforward����
     * ��Ϊ��SHUTDOWN״̬�ڼ䣬���п��ܴӷǿձ�Ϊ�գ���֮��Ȼ��vice versa���������ǲ��ܱ�֤�����Ƿ�һ��Ϊ�գ���Ϊ��仯��
     * ��������ֻ���ڿ�������Ϊ��֮���ټ��workerCountҲ��0ʱ������ֹ����Щʱ����Ҫ��entail ���������� -- �����ģ�
     *
     */
    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0)); // �̳߳���Ҫ����״̬����ʼ��ΪrunState=RUNNING��workerCount=0
    private static final int COUNT_BITS = Integer.SIZE - 3;                // ��λ��
    private static final int CAPACITY   = (1 << COUNT_BITS) - 1;           // worker�߳��������

    // runState is stored in the high-order bits // runState�洢�ڸ�λ����RUNNING��TERMINATED��ֵ�𽥵���
    private static final int RUNNING    = -1 << COUNT_BITS; // -536870912
    private static final int SHUTDOWN   =  0 << COUNT_BITS; // 0
    private static final int STOP       =  1 << COUNT_BITS; // 536870912
    private static final int TIDYING    =  2 << COUNT_BITS; // 1073741824
    private static final int TERMINATED =  3 << COUNT_BITS; // 1610612736

    // Packing and unpacking ctl // ����ͽ��ctl
    private static int runStateOf(int c)     { return c & ~CAPACITY; } // �����̳߳�����״̬
    private static int workerCountOf(int c)  { return c & CAPACITY; }  // worker����
    private static int ctlOf(int rs, int wc) { return rs | wc; }       // ��runState��workerCount�����ctl

    /*
     * Bit field accessors that don't require unpacking ctl.
     * These depend on the bit layout and on workerCount being never negative.
     * ����Ҫ���ctl��λ�������
     * ��ȡ����λ���ֺ�workerCount��Զ����Ϊ��ֵ
     */

    private static boolean runStateLessThan(int c, int s) {
        return c < s;
    }

    private static boolean runStateAtLeast(int c, int s) {
        return c >= s;
    }

    // �ж��̳߳�״̬�Ƿ�Ϊrunning��c < SHUTDOWN��
    private static boolean isRunning(int c) {
        return c < SHUTDOWN;
    }

    /**
     * Attempts to CAS-increment the workerCount field of ctl.
     * ����ͨ��CAS����ctl���workerCountֵ
     *
     */
    private boolean compareAndIncrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect + 1);
    }

    /**
     * Attempts to CAS-decrement the workerCount field of ctl.
     * ����ͨ��CAS�ݼ�ctl���workerCountֵ
     *
     */
    private boolean compareAndDecrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect - 1);
    }

    /**
     * Decrements the workerCount field of ctl. This is called only on
     * abrupt termination of a thread (see processWorkerExit). Other
     * decrements are performed within getTask.
     * �ݼ�ctl���workerCountֵ���÷������Ա������������ã�
     * 1��processWorkerExit��������������Ϊ�������߳�ͻȻ��abrupt����ֹʱ����
     * 2��getTask�����������ݼ���getTaskʱִ�С��������̳߳��г���corePoolSize���߳̿��еȣ�
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
     * �ö������ڱ�������ͽ������ƽ���worker�̡߳�
     * ��Ҫ��workQueue.poll()����nullһ����ζ��workQueue.isEmpty()��
     * ����ֻ�ܣ�solely��������rely on��isEmpty���������Ƿ�Ϊ�գ����磬��������Ҫ�����Ƿ��̳߳�״̬��SHUTDOWNת��ΪTIDYING����
     * ��������Ӧ����Ŀ�ĵĶ��У�����DelayQueue������poll()����null������ûԪ�أ�����Ԫ�ػ�û���ӳ�ʱ�䣩�����Ժ��ӳٵ���ʱ���ط�nullֵ��
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
     * �����ã�������workers�����bookkeeping���м������ʡ�
     * ���ܿ���ʹ��ĳ�֣�some sort���������������turn out��֤��ͨ�����ʹ������
     * ����һ��ԭ���������л���interruptIdleWorkers�������˲���Ҫ���жϷ籩�����������̳߳�shutdown��ʱ��
     * �����˳��߳̽�ͬʱ�ж���Щ��δ�жϵ��̡߳�������n���ж��̲߳���ȥ�ж�1����δ�жϵ��̣߳�
     * ������������̳߳����ȹ���ͳ��bookkeeping��
     * Ҳ��shutdown��shutdownNow��ʱ�����mainLock��Ϊ��ȷ��worker�����ڷֱ����ж�Ȩ�޺�ʵ���ж�ʱ�����ȶ�
     * �����������漰����workers������ϵĲ�������Ҫ��mainLock����
     *
     */
    private final ReentrantLock mainLock = new ReentrantLock();

    /**
     * Set containing all worker threads in pool. Accessed only when
     * holding mainLock.
     * ���ϰ����̳߳������е�worker�̡߳�
     * ֻ���ڳ���mainLock��ʱ���ܷ��ʡ�
     *
     */
    private final HashSet<Worker> workers = new HashSet<Worker>();

    /**
     * Wait condition to support awaitTermination
     * �������ϵȴ���֧��awaitTermination
     * �����÷��룬�����ȿ���Condition�ӿ�Դ�룩
     *
     */
    private final Condition termination = mainLock.newCondition();

    /**
     * Tracks largest attained pool size. Accessed only under
     * mainLock.
     * �����̳߳ص��������߳�����
     * ֻ�г���mainLockʱ���ܷ��ʡ�
     *
     */
    private int largestPoolSize;

    /**
     * Counter for completed tasks. Updated only on termination of
     * worker threads. Accessed only under mainLock.
     * ͳ���������������
     * ����worker�߳̽�������¸�ֵ�����ɼ�processWorkerExit������
     * ֻ�г���mainLock���ܷ��ʡ�
     *
     */
    private long completedTaskCount;

    /*
     * All user control parameters are declared as volatiles so that
     * ongoing actions are based on freshest values, but without need
     * for locking, since no internal invariants depend on them
     * changing synchronously with respect to other actions.
     * �����û����ƵĲ�����������Ϊvolatile���Ա��ڽ��еĶ������������µ�ֵ��
     * ������Ҫ��������Ϊû���ڲ���������invariants������������������ͬ���޸ġ�
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
     * ���̵߳Ĺ����������̶߳�ͨ����factory��������ͨ��addWorker��������
     * ���е����߶�����׼���ö�addWorkerʧ�ܵĴ�������ܷ�ӳ��ϵͳ����ʹ���������߳����Ĳ��ԡ�
     * ��ʹ��û�б���Ϊ��treat �Դ���error�������߳�ʧ�ܿ��ܵ����µ����񱻾ܾ��������������ڶ����С�
     *
     * We go further and preserve pool invariants even in the face of
     * errors such as OutOfMemoryError, that might be thrown while
     * trying to create threads.  Such errors are rather common due to
     * the need to allocate a native stack in Thread.start, and users
     * will want to perform clean pool shutdown to clean up.  There
     * will likely be enough memory available for the cleanup code to
     * complete without encountering yet another OutOfMemoryError.
     * ���Ǹ���һ������ʹ�������쳣�����ڴ����߳�ʱ�����׳�OOM�쳣��Ҳ�����̳߳صĲ�������
     * ������Thread.startʱ��Ҫ�ӱ���ջ�з��䣬�������error�൱�����������û�ϣ���ɾ���shutdown�̳߳������������������ջ�Ϸ�����߳̿ռ䣿������
     * �������㹻���ڴ���������������ɣ���������������OOM�쳣��
     * 
     */
    private volatile ThreadFactory threadFactory;

    /**
     * Handler called when saturated or shutdown in execute.
     * �����ͣ�saturated ���͵ģ�����shutdownʱִ�е�handler�������ڴ�����Щ�̳߳ش����˵�task��
     *
     */
    private volatile RejectedExecutionHandler handler;

    /**
     * Timeout in nanoseconds for idle threads waiting for work.
     * Threads use this timeout when there are more than corePoolSize
     * present or if allowCoreThreadTimeOut. Otherwise they wait
     * forever for new work.
     * �����߳����ȴ���ҵʱ�䡣����λ�����룩
     * �߳�ʹ�ô˳�ʱ��
     * 1����ǰ�߳�������corePoolSize�������߳�����
     * 2����������̳߳�ʱ�˳���allowCoreThreadTimeOut��
     * ��������õȴ��µ���ҵ��
     * 
     */
    private volatile long keepAliveTime;

    /**
     * If false (default), core threads stay alive even when idle.
     * If true, core threads use keepAliveTime to time out waiting
     * for work.
     * ���Ϊfalse��Ҳ��Ĭ��ֵ���������̼߳�ʹ����Ҳ�ᱣ�ֻ�Ծ��alive����
     * ���Ϊtrue�������߳��ڵȴ���ҵʱʹ��keepAliveTime���õ���ʱʱ�䡣����ʱ�˳���
     *
     */
    private volatile boolean allowCoreThreadTimeOut;

    /**
     * Core pool size is the minimum number of workers to keep alive
     * (and not allow to time out etc) unless allowCoreThreadTimeOut
     * is set, in which case the minimum is zero.
     * �����̳߳ش�С�Ǳ��ֻ�Ծ�����Ҳ�����ʱ�ȣ���worker��Сֵ������������allowCoreThreadTimeOut������allowCoreThreadTimeOut�ĳ����£��߳�����СֵΪ0��
     * ��ע�⣺0<=corePoolSize<=CAPACITY��
     */
    private volatile int corePoolSize;

    /**
     * Maximum pool size. Note that the actual maximum is internally
     * bounded by CAPACITY.
     * ����̳߳ش�С��ע�⣬ʵ���ϵ����ֵ��CAPACITY�ڲ����ơ���maximumCoreSize���ܳ���CAPACITY��
     * ��ע�⣺corePoolSize<=maximumPoolSize<=CAPACITY��
     */
    private volatile int maximumPoolSize;

    /**
     * The default rejected execution handler
     * Ĭ�ϵľܾ�ִ�еĴ������handler��
     * �������񱻾ܾ�ʱ��Ĭ�ϲ��õľܾ�������AbortPolicy()���ܾ�ִ�в��׳�RejectedExecutionException��extends RuntimeException����
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
     * ������ִ��shutdown��shutdownNowʱ��Ҫ��Ȩ�ޡ�
     * ���ǻ�Ҫ�󣨼�checkShutdonwAccess�������߾���ʵ���ܹ� �ж���worker������߳� ��Ȩ�ޡ�
     * ����Thread.interrupt����governed����������ThreadGroup.checkAccess��������������SecurityManager.checkAccess����
     * ֻ�иü��ͨ����shutdownϵ�з����Ż᳢��ִ�С������Թرգ�
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
     * ����ʵ�ʵ���Thread.interrupte����interruptIdleWorkers��interruptWorkers������������SecurityExceptions��
     * ����ζ�ų��Ե��ж�ʧ��ʱ���Ǿ�Ĭ�ġ���silently fail����ʿ��Կ���throw Exception��
     * ��shutdown�����£�����SecurityManage�з�һ���Բ��ԣ���ʱ��������߳���ʱ������������Ӧ��ʧ�ܡ�
     * ����������£�δ��ʵ���ж��߳̿��ܵ��²����û����ӳ���ȫ��ֹ����ɶ�����ã�ɶ�ӳ���ȫ��ֹ��������
     * interruptIdleWorkers�������÷��ǽ����Եģ�advisory����δ��ʵ���ж�ֻ�ᣨmerely���ӳٶԲ����޸ĵ���Ӧ�����Բ��ᱻ�쳣����
     * 
     */
    private static final RuntimePermission shutdownPerm =
        new RuntimePermission("modifyThread"); // �޸��̣߳�����ͨ�������̵߳� interrupt��stop��suspend��resume��setDaemon��setPriority��setName �� setUncaughtExceptionHandler ����

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
     * Worker����Ҫ���ڱ����߳����е������жϿ���״̬����������Ҫbookkeeping��
     * ����ȡ�ɼ̳�AQS����ʵ��Χ��ÿ������ִ�л�ȡ���ͷ�����
     * ���Է�ֹ�ж����ڻ��ѵȴ������worker�̣߳��������ж�����ִ�е����񡣣��������ж������ѵȴ����̶߳������ж�����ִ�е��̣߳�������
     * ����ʵ����һ���򵥵Ĳ�������Ļ�������������ʹ��ReentrantLock��
     * ��Ϊ���ǲ���worker�����ܹ��ڵ����̳߳ؿ��Ʒ�������setCorePoolSize��ʱ�ܹ����»�ȡ�����ٴμ�������
     * ���⣬Ϊ�����߳�ʵ�ʿ�ʼִ������֮ǰ���ƣ�suppress���жϣ����ǳ�ʼ������״̬Ϊ��ֵ���ڿ�ʼ����runWorker��֮����������������ȿ�runWorker������
     * 
     */
    private final class Worker
        extends AbstractQueuedSynchronizer  // �̳�AQS
        implements Runnable                 // ʵ��Runnable
    {
        /**
         * This class will never be serialized, but we provide a
         * serialVersionUID to suppress a javac warning.
         * ������Զ���ᱻ���л��������ṩ��serialVersionUID������javac���档
         *
         */
        private static final long serialVersionUID = 6138294804551838833L;

        /** Thread this worker is running in.  Null if factory fails. */
        // ���и�worker���̣߳��������ʧ����Ϊnull��������
        final Thread thread;
        /** Initial task to run.  Possibly null. */
        // ��ʼ���Ĵ��������񣬿���Ϊnull������۲쵽��������������̳߳���ûworker�ˣ�
        Runnable firstTask;
        /** Per-thread task counter */
        // ÿ���߳����������
        volatile long completedTasks;

        /**
         * Creates with given first task and thread from ThreadFactory.
         * ���췽�����ø���������ʹ�ThreadFactory���ɵ��߳���������
         * @param firstTask the first task (null if none)
         */
        Worker(Runnable firstTask) {
            setState(-1); // inhibit interrupts until runWorker // ��ֹ��inhibit���жϣ�ֱ��runWorker�����õ�state��AQS��state
            this.firstTask = firstTask;
            this.thread = getThreadFactory().newThread(this); // ����worker��Ϊ�̵߳�ִ�ж���
        }

        /** Delegates main run loop to outer runWorker  */
        // ������ѭ��ί�и��ⲿ��runWorker��run��ʱ��զִ�����ⲿ��runWorker������
        public void run() {
            runWorker(this);
        }

        // Lock methods
        // ����ط���
        //
        // The value 0 represents the unlocked state.
        // The value 1 represents the locked state.
        // ֵΪ0���������δ������״̬
        // ֵΪ1�������״̬

        // isHeldExclusively��tryAcquire��tryRelease��ʹ��AQSʵ�ֶ�ռ����Ҫ�Լ�ʵ�ֵ���������

        // ��ǰ�߳��Ƿ���ж�ռ��
        protected boolean isHeldExclusively() {
            return getState() != 0; // getState()������AQS����ģ�������ȡ��ǰstate
        }

        // ���Լ���
        protected boolean tryAcquire(int unused) {  // ���û�õ�
            if (compareAndSetState(0, 1)) { // ����AQS#compareAndSetState����������stateֵ�����ReentrantLock��Worker��stateֻ��0��1����ֵ��
                setExclusiveOwnerThread(Thread.currentThread()); // ��������ɹ������¶�ռ���ĳ�����Ϊ��ǰ�߳�
                return true;
            }
            return false;
        }

        // ���Խ����������ض��ɹ���
        protected boolean tryRelease(int unused) {
            setExclusiveOwnerThread(null);
            setState(0); // ����û��ͨ��CAS������ֵ��ֱ����Ϊ0����Ҫ��Ϊʲô������ţ�
            return true;
        }

        public void lock()        { acquire(1); } // AQS#acquire���������Ի�ȡ�������ʧ����ȴ���������
        public boolean tryLock()  { return tryAcquire(1); } // Worker#tryAcquire
        public void unlock()      { release(1); } // AQS#release�������ͷ����������Դ�AQS�ȴ����е�head��ʼ����һ�����ú�̡�������������tryRelease�����Ե�����state��Ϊ0�ˡ�
        public boolean isLocked() { return isHeldExclusively(); } // Worker#isHeldExclusively

        void interruptIfStarted() {
            Thread t;
            if (getState() >= 0 && (t = thread) != null && !t.isInterrupted()) { // �ж�ִ�е�ǰworker���߳��Ƿ���������state��ֵ��thread��Ϊnull��threadû���ж�
                try {
                    t.interrupt();
                } catch (SecurityException ignore) { // �����쳣������
                }
            }
        }
    }

    /*
     * Methods for setting control state
     * ���ÿ���״̬�ķ���
     */

    /**
     * Transitions runState to given target, or leaves it alone if
     * already at least the given target.
     * ��runStateת��Ϊ������Ŀ��ֵ�����runState��ֵ�Ѿ�>=������Ŀ��ֵ�������κβ�����
     *
     * @param targetState the desired state, either SHUTDOWN or STOP
     *        (but not TIDYING or TERMINATED -- use tryTerminate for that)
     *        targetState�������״̬��ֻ��ΪSHUTDOWN����STOP
     *        ������޸�runStateΪTIDYING����TERMINATED����ôʹ��tryTerminate����
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
     * runStateֵת��ΪTERMINATED�������������������һ�ַ�����
     * 1��runState=SHUTDOWN�������̳߳���������ж�Ϊ��
     * 2��runState=STOP�������̳߳�Ϊ��
     * ������ʸ�eligible����ֹ����workerCount��Ϊ0�����жϿ��е�worker��֤�ر��źŴ�����
     * �����ڿ��ܻᵼ���̳߳���ֹ�Ĳ�����������ø÷��������ܵĲ���������
     * 1����shutdown�ڼ����worker����
     * 2����shutdown�ڼ����������Ƴ�����
     * �������漰�������ֲ����ķ���A���ܻᵼ���̳߳عرգ��Ǿ���A����֮����ø÷�������runStateֵΪTERMINATED��
     * �÷����Ƿ�˽�еģ������ScheduledThreadPoolExecutor�����ʡ�
     * 
     */
    final void tryTerminate() { // Ҫע��÷���Ŀ����Ҫ���̳߳ص�runState����ΪTERMINATED
        for (;;) {
            int c = ctl.get();
            if (isRunning(c) ||                                       // ����̳߳ػ������У�
                runStateAtLeast(c, TIDYING) ||                        // ����״̬�Ѿ�>=TIDYING������TIDYING��TERMINATED����TIDYING״̬�ǹ��ӷ�����ûִ���ִ꣬����ͳ�TERMINATED��
                (runStateOf(c) == SHUTDOWN && ! workQueue.isEmpty())) // ����״̬=SHUTDOWN���ǻ�������û���
                return;                                              // ��������runStateΪTERMINATED
            if (workerCountOf(c) != 0) { // Eligible to terminate     // �ߵ���һ����runState=SHUTDOWN����STOP����ʱ�������Ϊ�գ����worker����Ϊ0�����д���worker�̣߳�
                interruptIdleWorkers(ONLY_ONE);                       // �����жϿ��е�worker��ֻ�ж�һ����������Ϊ����û�����˲����̳߳ش��ڼ����ر�״̬�������߳�����Ҳû�ã�
                return;
            }

            final ReentrantLock mainLock = this.mainLock;            // ����runState=SHUTDOWN����STOP�����������worker��Ϊempty����ʼ�޸�runStateֵ
            mainLock.lock();                                          // ��ȡmainLock
            try {
                if (ctl.compareAndSet(c, ctlOf(TIDYING, 0))) {        // ���Խ�ctlת����runState=TIDYING��workerCount=0
                    try {
                        terminated();                                 // ���ù��ӷ���ThreadPoolExecutor#terminated()������÷���Ϊ�գ�������԰����Լ�������ʵ��
                    } finally {
                        ctl.set(ctlOf(TERMINATED, 0));                // terminated()���ӷ���ִ����ϣ�ctlת��ΪrunState=TERMINATED��workerCount=0
                        termination.signalAll();                      // ����������Condition�ϵȴ����̣߳���û�м���Condition���ô���
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
     * ����worker�߳��жϵķ���
     *
     */

    /**
     * If there is a security manager, makes sure caller has
     * permission to shut down threads in general (see shutdownPerm).
     * If this passes, additionally makes sure the caller is allowed
     * to interrupt each worker thread. This might not be true even if
     * first check passed, if the SecurityManager treats some threads
     * specially.
     * ������̹߳�������ͨ����Ҫȷ��������ӵ�йر��̵߳�Ȩ�ޣ���shutdownPrem����
     * ��������ͨ���ˣ�������Ҫȷ������������ж�����worker�̡߳�
     * ��ʹ��һ�����ͨ���ˣ����SecurityManager����Դ�ĳЩ�̣߳���Ҳ��һ��������
     *
     */
    private void checkShutdownAccess() {
        SecurityManager security = System.getSecurityManager(); // �����ǰӦ�ô����˰�ȫ����������ô���ظù����������򷵻�null
        if (security != null) {
            security.checkPermission(shutdownPerm);             // ����Ƿ���shutdownȨ�ޣ����û���׳�����SecurityException���쳣
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();                                    // ������Ϊ�˷�ֹ�ڱ���workers��ʱ��worker�����б仯
            try {
                for (Worker w : workers)
                    security.checkAccess(w.thread);             // �������Ƿ��ж�ÿ��worker�̵߳ķ���Ȩ�ޣ�������MODIFY_THREAD_PERMISSIONȨ�ޣ������û���׳�����SecurityException���쳣
            } finally {
                mainLock.unlock();
            }
        }
    }

    /**
     * Interrupts all threads, even if active. Ignores SecurityExceptions
     * (in which case some threads may remain uninterrupted).
     * �ж������̣߳���ʹ�߳��ǻ�Ծ�ġ����Ը���SecurityException
     * ����ĳЩ����£�ĳЩ�߳̿��ܻᱣ�ֲ��жϣ�����Ӧ�жϣ���
     *
     */
    private void interruptWorkers() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (Worker w : workers)
                w.interruptIfStarted();                      // �жϴ�������״̬��worker�߳�
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
     * �жϿ������ڵȴ�������̣߳�ͨ��û�б�lock�������������workerû���ڲ���������ô���ж�Ϊ���ڵȴ����񣩣���
     * �Ա����ǣ������ڵȴ������worker�����Լ����ֹ�������ñ仯��
     * ����SecurityException����ĳЩ����£�ĳЩ�߳̿��ܻᱣ�ֲ��жϣ�����Ӧ�жϣ���
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
     * ����onlyOne���Ϊtrue��������ж�һ��worker��
     * ֻ��ͨ��tryTerminate�������ֵ��ã���������������ֹ��������worker��ʱ�򣩣����ǳ�����worker���������������������ֹ��ʱ�򣬲Ż��������ø÷�����
     * ����������£�����worker�̵߳�ǰ�����ڵȴ�״̬�����һ���ȴ�worker���ж����㲥shutdown�źš�
     * �ж������߳̿��Ա�֤��shutdown��ʼ�������µ����workerҲ�������˳���
     * Ϊ��֤����ȫ���˳���ÿ��ֻ�ж�һ������worker���㹻�ˣ���ͨ����ε������ж�����worker��
     * ����shutdown()�ж����п����̣߳��Ա����ࣨredundant����worker������promptly���˳��������ǵȴ�һ�����ģ�straggler��������ɡ�
     * 
     */
    private void interruptIdleWorkers(boolean onlyOne) {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();                                    // ����workers������Ҫ����mainLock
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
     * interruptIdleWorkers��ͨ����ʽ��������Ҫ��ס��ε�ʵ�ʺ���
     * ��Ĭ������εĻ��ж����п���worker��
     *
     */
    private void interruptIdleWorkers() {
        interruptIdleWorkers(false);
    }

    // ����interruptIdleWorkers�ĳ���
    private static final boolean ONLY_ONE = true;

    /*
     * Misc utilities, most of which are also exported to
     * ScheduledThreadPoolExecutor
     * �����������������¶��ScheduledThreadPoolExecutor
     *
     */

    /**
     * Invokes the rejected execution handler for the given command.
     * Package-protected for use by ScheduledThreadPoolExecutor.
     * �Ը�����������þܾ�ִ�д�����
     * protectedȨ�ޣ���ScheduledThreadPoolExecutorʹ�á�
     *
     */
    final void reject(Runnable command) {
        handler.rejectedExecution(command, this);
    }

    /**
     * Performs any further cleanup following run state transition on
     * invocation of shutdown.  A no-op here, but used by
     * ScheduledThreadPoolExecutor to cancel delayed tasks.
     * �ڵ���shutdown����״̬ת����ִ�н�һ��������
     * �˴��޲�����������ScheduledThreadPoolExecutorȥȡ���ӳ�����
     * 
     */
    void onShutdown() {
    }

    /**
     * State check needed by ScheduledThreadPoolExecutor to
     * enable running tasks during shutdown.
     * ����ScheduledThreadPoolExecutor����״̬��飬ȷ��shutdown�ڼ��ܹ���������
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
     * ���������ת�Ƶ��µ��б��У�ͨ��ʹ��drainTo��
     * �����������DelayQueue���κ��������͵�queue���������ǵ�poll����drainTo�����޷��Ƴ�ĳЩԪ�أ�����һ��һ����ɾ������
     * 
     */
    private List<Runnable> drainQueue() {
        BlockingQueue<Runnable> q = workQueue;
        ArrayList<Runnable> taskList = new ArrayList<Runnable>();
        q.drainTo(taskList);  // ��q���Ƴ�����Ԫ�أ���ת�Ƶ�������taskList��
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
     * ���ڴ�����ִ�С�����worker�ķ���
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
     * �����ݵ�ǰ�̳߳�״̬�͸����ı߽磨�̳߳صĺ����߳���������߳��������ж��Ƿ���������µ�worker��
     * ������ԣ���Ӧ��accordingly������worker���������ң���ο��ܵĻ���������worker��������ִ��firstTask��Ϊ���ĵ�һ������
     * ����̳߳��Ѿ�ֹͣ�������ʸ�eligible���رգ��÷�������false��
     * ��������̵߳Ĺ�������ʱ�����߳�ʧ�ܣ��÷�������false��
     * ������̴߳���ʧ�ܣ����������������
     * 1�������̹߳�������null
     * 2�������쳣�����͵�����Thread.start()ʱ����OOM��
     * �����ǻ�ɾ�����Ľ��лع���
     *
     * @param firstTask the task the new thread should run first (or
     * null if none). Workers are created with an initial first task
     * (in method execute()) to bypass queuing when there are fewer
     * than corePoolSize threads (in which case we always start one),
     * or when the queue is full (in which case we must bypass queue).
     * Initially idle threads are usually created via
     * prestartCoreThread or to replace other dying workers.
     * firstTask�����������߳�Ӧ������ִ�е��������û����Ϊnull����
     * �������������֮һʱ��ʹ�ó�ʼ�ĵ�һ������ͨ��execute()�������룩�����µ�Worker��
     * 1�����߳�������corePoolSize�������������������һ�����̣߳�
     * 2�����ߵ����������������������±���Ҫ�ƹ���bypass��queue��
     * ����Ŀ��н���ͨ��ͨ��preStartCoreThread�����������滻������Ҫ�ҵ���worker��
     * ������˵������滻����ָ��ԭ����Ҫ�˳���worker��ûremove�����ͽ�����wc+1��Ȼ��add�µ�worker�ˣ���
     *
     * @param core if true use corePoolSize as bound, else
     * maximumPoolSize. (A boolean indicator is used here rather than a
     * value to ensure reads of fresh values after checking other pool
     * state).
     * core���������Ϊtrue��ʹ��corePoolSize��Ϊ�߽磬����ʹ��maximumPoolSize��
     * ������ʹ��booleanָʾ�������Ǿ�����߳���value����ȷ���ڼ���������̳߳�״̬�����¶�ȡ���µ��߳���value�� 
     *
     * @return true if successful
     */
    private boolean addWorker(Runnable firstTask, boolean core) {           // addWorker������workerCount�봴������worker����ͬ����
        retry:
        for (;;) {
            int c = ctl.get();
            int rs = runStateOf(c);

            // Check if queue empty only if necessary.                         // �����̳߳�״̬�빤�����У��е�������ǲ��������µ�worker��
            if (rs >= SHUTDOWN &&                                              // 1��runStateΪSTOP��TIDYING��TERMINATED����ʱ�̳߳ش��ڹر�״̬��������������û������
                ! (rs == SHUTDOWN &&                                           // 2��runStateΪSHUTDOWNʱ��firstTask��Ϊnull����ʾ��SHUTDOWN״̬�ύ�����������ֵ�ֱ�Ӿܾ���
                   firstTask == null &&                                        // 3��runStateΪSHUTDOWNʱ����������Ϊ�գ����ֵ�Ҳ���ܴ�����worker��
                   ! workQueue.isEmpty()))                                     // ��2��3�����ж���Ϊ����������̳߳ش���SHUTDOWN״̬���ҹ������в�Ϊ�գ���ô������firstTaskΪnull��worker�����ѹ�Ĺ�����������
                return false;

            for (;;) {
                int wc = workerCountOf(c);
                if (wc >= CAPACITY ||
                    wc >= (core ? corePoolSize : maximumPoolSize))             // �жϵ�ǰworkerCount�Ƿ���ڱ߽磨�������core�ж�corePoolSize����maximumPoolSize��
                    return false;
                if (compareAndIncrementWorkerCount(c))                         // �������workerCount�ɹ���������������forѭ��������÷�������worker��������������
                    break retry;
                c = ctl.get();  // Re-read ctl
                if (runStateOf(c) != rs)                                       // �������workerCountʧ�ܣ�����runState�����˱仯����ô�����ж��Ƿ��������µ�worker���ӵ�һ��forѭ����ʼ������
                    continue retry;
                // else CAS failed due to workerCount change; retry inner loop // �������workerCountʧ�ܣ�����runStateû�б仯����ô�����ǲ�������workerCount�����˱仯������CAS������workerCount
            }
        }

        boolean workerStarted = false;
        boolean workerAdded = false;
        Worker w = null;
        try {
            w = new Worker(firstTask);                                        // �����µ�worker
            final Thread t = w.thread;                                        // ��ȡ��ǰworker���߳�t������߳�t������ִ�и�worker���������ģ�����߳�t��ͨ���̹߳��������ģ�
            if (t != null) {                                                  // t != null����ʾ�̹߳����ɹ������̡߳�
                final ReentrantLock mainLock = this.mainLock;                 // ����workers�Ĳ�������Ҫ��mainLock��
                mainLock.lock();
                try {
                    // Recheck while holding lock.
                    // Back out on ThreadFactory failure or if
                    // shut down before lock acquired.                        // ��������״̬�����¼�飬��ThreadFactoryʧ�ܻ����ڻ�ȡ��֮ǰ�̳߳�shutdown�����˳�������
                    int rs = runStateOf(ctl.get());

                    if (rs < SHUTDOWN ||
                        (rs == SHUTDOWN && firstTask == null)) {              // �����ж��̳߳�״̬��runState����ֻ����������ʱ�Ž���worker����workers���ϣ��������µ�worker
                        if (t.isAlive()) // precheck that t is startable      // ��֪�����alive()������ɶ
                            throw new IllegalThreadStateException();
                        workers.add(w);                                       // ����worker�����workers����
                        int s = workers.size();
                        if (s > largestPoolSize)
                            largestPoolSize = s;                              // �����ǰworkers������������largestPoolSize����ô����largestPoolSizeΪ��ǰֵ�����ֵ�����ڸ����̳߳شﵽ�����ֵ
                        workerAdded = true;                                   // ��worker����workers���ϳɹ��������µ�worker����
                    }
                } finally {
                    mainLock.unlock();
                }
                if (workerAdded) {
                    t.start();                                               // ִ��t.start()�������worker.run()�����������Thread.start()��
                    workerStarted = true;                                    // ����һ����ʾworker����û���쳣�������OOM���쳣�����߲�����һ���ˣ�
                }
            }
        } finally {
            if (! workerStarted)                                             // �����worker����ʧ�ܣ���ô��Ҫ��workers������ɾ����worker����ȻҲ�п���worker��û���뵽workers���ϣ�����ûӰ�죩
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
     * �ع�worker�̵߳Ĵ���
     * 1��������ڵĻ�����workers�������Ƴ���worker
     * 2��worker������һ
     * 3�����¼����ֹ���Է�ֹ��worker�Ĵ��ڵ������̳߳���ֹ��
     * 
     */
    private void addWorkerFailed(Worker w) {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();                 // ��workers�Ĳ�����Ҫ��mainLock��
        try {
            if (w != null)
                workers.remove(w);       // ��workers�������Ƴ���worker�����������û�и�worker���᷵��false��
            decrementWorkerCount();      // ����ߵ�������worker��һ��������worker��û�д�������ɹ�������workerCount�����һ
            tryTerminate();              // ������ֹ�̳߳أ�Ϊ���Ǳ������ڸ�worker�Ĵ����谭��ԭ����ֹ�̳߳ز���
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
     * ������worker���������bookkeeping��������workerΪ�����˳�worker���е��̣߳�����ִ���ύ���̳߳ص�����
     * ֻ����worker�̵߳��á�
     * ����������completedAbruptly��ͻȻ��ɣ�������ٶ�workerCount�Ѿ�����Ϊ�϶��˳��������Ƿ�completedAbruptly����£��Ѿ���workerCount�м�ȥ�˸�worker��wc-1������
     * �÷�����worker�������Ƴ���Ӧ���̣߳���������ֹ�̳߳ػ����滻��worker���滻worker�ĳ���Ϊ��
     * 1���û��������쳣���¸�worker�߳��쳣�˳�����ʱcompletedAbruptlyΪtrue��
     * 2�����е�worker�߳���С��corePoolSize
     * 3���������зǿյ���û�д���worker
     *
     * @param w the worker
     * @param completedAbruptly if the worker died due to user exception
     *        completedAbruptly���������worker�����û������쳣���¹ҵ��������Ϊtrue
     */
    private void processWorkerExit(Worker w, boolean completedAbruptly) {
        if (completedAbruptly) // If abrupt, then workerCount wasn't adjusted  // ���ͻȻ�жϣ�workerCountû�����ü�����
            decrementWorkerCount();                                            // wc - 1

        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();                                                       // �κζ�worker���ϵĲ�����Ҫ��mainLock��
        try {
            completedTaskCount += w.completedTasks;                             // �����������������ע�������õ�w.completedTasks��
            workers.remove(w);                                                  // ��worker�������Ƴ���worker
        } finally {
            mainLock.unlock();
        }

        tryTerminate();                                                         // ��Ϊ��worker�˳�����ֹ��worker�Ĵ���Ӱ���̳߳�������ֹ�����й��³�����ֹ�̳߳�

        int c = ctl.get();
        if (runStateLessThan(c, STOP)) {                                        // �жϵ�ǰ�̳߳�����״̬�Ƿ�<STOP
            if (!completedAbruptly) {                                           // ��������쳣�˳�����ô��Ҫ������ǰ�߳��Ƿ�С�ں����̳߳������߹���������������ûalive worker
                int min = allowCoreThreadTimeOut ? 0 : corePoolSize;
                if (min == 0 && ! workQueue.isEmpty())
                    min = 1;
                if (workerCountOf(c) >= min)
                    return; // replacement not needed                           // ���������������������Ͳ����滻worker
            }
            addWorker(null, false);                                             // ������쳣�˳����������������������ô��Ҫ����һ����ʼ����Ϊnull��worker�����ѹ��������е�����
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
     * ����������ʱ�ȴ���ȡ����ѡȡ���ַ�ʽȡ���ڵ�ǰ�������ã�
     * �����worker��������ԭ������˳�����᷵��null��
     * 1����ǰworker��������maximumPoolSize�����ڵ�����setMaximumPoolSize��������������̳߳�����
     * 2���̳߳ر�ֹͣ��runState>=STOP��
     * 3���̳߳ر��رգ�runState=SHUTDOWN�������ҹ�������Ϊ��
     * 4����worker�ȴ�����ʱ�����ҳ�ʱworker����ʱ�ȴ�֮ǰ��֮�󶼻���ֹ����allowCoreThreadTimeOut || workerCount > corePoolSize��������������зǿգ���ô��worker�������̳߳������һ���̡߳�
     * ����4�������������ע��˵��ɶ�����忴����ɣ�
     *
     * @return task, or null if the worker must exit, in which case
     *         workerCount is decremented
     */
    private Runnable getTask() {
        boolean timedOut = false; // Did the last poll() time out? // ���poll�Ƿ�ʱ��poll������ʱ������ȡ��

        for (;;) {
            int c = ctl.get();
            int rs = runStateOf(c);

            // Check if queue empty only if necessary.
            if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {  // �벻��������ж���ôд�Ż�����
                decrementWorkerCount();                                   // ����decrementWorkerCount������Ϊ���̳߳�״̬Ϊ�رղ����������û����ʱ��������������worker�����ùܲ����������worker̫�ࣩ
                return null;                                             // wc-1֮��ֱ�ӷ���null����worker�Լ�ȥִ���˳�
            }

            int wc = workerCountOf(c);

            // Are workers subject to culling?
            boolean timed = allowCoreThreadTimeOut || wc > corePoolSize; // �ж��Ƿ�����ʱ�˳�

            if ((wc > maximumPoolSize || (timed && timedOut))            // ��ǰwc��������̳߳�����������ʱ�˳�������ϴλ�ȡ�ѳ�ʱ
                && (wc > 1 || workQueue.isEmpty())) {                    // ����ע�⹤�����зǿ�ʱ�����һ��worker�̲߳����˳�
                if (compareAndDecrementWorkerCount(c))                  // ����compareAndDecrementWorkerCount(c)��Ҫ�õ�ǰ��worker��-1���Ƿ�ֹ�����������worker���ࣨ��������һ������worker�����ȴ���ʱ�ˣ�������л���Ϊ�գ�����������ߵ�����һ�������ֱ��-1.�����Ͷ��������ˣ�
                    return null;
                continue;
            }

            try {
                Runnable r = timed ?
                    workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) :
                    workQueue.take();                                     // ���worker���Գ�ʱ�˳�����ôʹ����ʱ������poll����������ʹ��take����
                if (r != null)
                    return r;
                timedOut = true;                                          // �����ʱ�ò����������¼�ϴλ�ȡ�ѳ�ʱ
            } catch (InterruptedException retry) {
                timedOut = false;                                         // ����ڴӹ���������������ʱ���ж��˳�����ô�����ϴλ�ȡ��ʱ
            }
        }
    }

    /**
     * Main worker run loop.  Repeatedly gets tasks from queue and
     * executes them, while coping with a number of issues:
     * ��Ҫ��workerִ��ѭ����
     * �ظ��ӹ��������л�ȡ����Ȼ��ִ�����ǣ�ͬʱ����cope��������Щ���⣺
     *
     * 1. We may start out with an initial task, in which case we
     * don't need to get the first one. Otherwise, as long as pool is
     * running, we get tasks from getTask. If it returns null then the
     * worker exits due to changed pool state or configuration
     * parameters.  Other exits result from exception throws in
     * external code, in which case completedAbruptly holds, which
     * usually leads processWorkerExit to replace this thread.
     * 1�����ǿ��Դ�һ����ʼ����ʼִ�У���������������ǲ���Ҫ��ȡ��һ������
     * ����ֻҪ�̳߳������У����Ǿ�ͨ��getTask������ȡ����
     * ��������̳߳�״̬�ı���߲������÷����仯��getTask����null����ô��worker�˳���
     * �����˳��������ⲿ�����׳��쳣����ģ������������completedAbruptly����������completedAbruptly��������
     * ��ͨ���ᵼ��processWorkerExit���滻��worker�̡߳�
     *
     * 2. Before running any task, the lock is acquired to prevent
     * other pool interrupts while the task is executing, and then we
     * ensure that unless pool is stopping, this thread does not have
     * its interrupt set.
     * 2���������κ�����֮ǰ��Ҫ��ȡlock����ֹ��������ʱ���������̳߳��жϣ�
     * ����ȷ�������̳߳ر�stopping��������̲߳��������жϡ�
     *
     * 3. Each task run is preceded by a call to beforeExecute, which
     * might throw an exception, in which case we cause thread to die
     * (breaking loop with completedAbruptly true) without processing
     * the task.
     * 3��ÿ������ִ��֮ǰ�������beforeExecute����������ܻ��׳��쳣��
     * ����������»ᵼ���̹߳ҵ�������completedAbruptlyΪtrueȻ������ѭ����������������
     *
     * 4. Assuming beforeExecute completes normally, we run the task,
     * gathering any of its thrown exceptions to send to afterExecute.
     * We separately handle RuntimeException, Error (both of which the
     * specs guarantee that we trap) and arbitrary Throwables.
     * Because we cannot rethrow Throwables within Runnable.run, we
     * wrap them within Errors on the way out (to the thread's
     * UncaughtExceptionHandler).  Any thrown exception also
     * conservatively causes thread to die.
     * 4������beforeExecute������ɣ�Ȼ��ִ��task���ռ�ִ�������ڼ��׳����κ��쳣���͸�afterExecute������
     * �ֱ���RuntimeException��Error���淶��spec����֤���ǿ��Բ�������������������Throwables��
     * ������Runnable.run�����ڲ����������׳�Throwables�����Խ�Throwables��װ��Errors����������̵߳�UncaughtExceptionHandler����
     * �κ��׳����쳣Ҳ�ᱣ�صģ�conservatively�������̹߳ҵ���
     *
     * 5. After task.run completes, we call afterExecute, which may
     * also throw an exception, which will also cause thread to
     * die. According to JLS Sec 14.20, this exception is the one that
     * will be in effect even if task.run throws.
     * 5����task.run����ִ����ɣ�����afterExecute����������Ҳ���׳��쳣��Ҳ�ᵼ�¸�worker�̹߳ҵ���
     * ����JLS Sec 14.20�����쳣��ʹ��task.run�׳���Ҳ����Ч������������ 
     *
     * The net effect of the exception mechanics is that afterExecute
     * and the thread's UncaughtExceptionHandler have as accurate
     * information as we can provide about any problems encountered by
     * user code.
     * �����쳣���Ƶ�����Ч���ǣ�afterExecute���̵߳�UncaughtExceptionHandler���������ǿ����ṩ�Ĺ����û������������κ������׼ȷ��accurate����Ϣ��
     *
     * @param w the worker
     */
    final void runWorker(Worker w) {
        Thread wt = Thread.currentThread();                          // ��Ϊ��thread���õ�worker#run����ǰ�߳�Ϊworker�̣߳����߳�Ҳ����worker��thread��ͨ���̹߳������������ġ�
        Runnable task = w.firstTask;                                  // �õ�����worker��firstTask��Ȼ��worker��firstTask��Ϊnull
        w.firstTask = null;
        w.unlock(); // allow interrupts                              // unlock->AQS#release->tryRelease��state��Ϊ0����ʾ��worker�������н׶Σ�����interruptIfStarted��
        boolean completedAbruptly = true;                           // ͻȻ��ɵı�ʶ�������ж��Ƿ�Ҫ�滻��worker
        try {
            while (task != null || (task = getTask()) != null) {    // �������Ҫִ�е����񣬾ͻ�ȡ����
                w.lock();                                             // ������������ʾ��worker��ִ�����񡣷�ֹ��interruptIdleWorkers������Ϊ�ǿ����̸߳��ж���
                // If pool is stopping, ensure thread is interrupted; // ����̳߳ر�ֹͣ��ȷ���̱߳��ж�
                // if not, ensure thread is not interrupted.  This    // ����̳߳�û��ֹͣ��ȷ���߳�û�б��жϡ�
                // requires a recheck in second case to deal with     // ����Ҫ�ڵڶ�����������¼���߳��ж�״̬����������жϵ�ͬʱ����shutdownNow�ľ�����
                // shutdownNow race while clearing interrupt
                if ((runStateAtLeast(ctl.get(), STOP) ||              // ���if���������ܣ�1���жϳ����߳��Ƿ�Ӧ���жϣ�runState>=STOP�ľ�ֱ���жϣ�2��������̲߳�Ӧ���жϣ���ô�����ж�״̬��
                     (Thread.interrupted() &&                         // ������runState<STOP��ǰ���£�Thread.interrupted()������ǰ�̵߳��ж�״̬��������ж��ˣ��÷����᷵��true
                      runStateAtLeast(ctl.get(), STOP))) &&           // ���worker�̷߳������жϣ���ô�����¼��runState�Ƿ���<STOP������ǣ����if������������ǵĻ���������if�ڼ䷢����shutdownNow��������Ҫ��������worker�߳��ж�
                    !wt.isInterrupted())                              // ���worker�߳��Ѿ��ж��ˣ���ô�Ͳ������������ж�״̬�ˣ�����������ж�״̬��wt.isInterrupted���ڼ���ж�״̬����������ж�״̬��
                    wt.interrupt();                                   // �����ִ�У���ʾrunState>=STOP�����Ҵ�ʱ�߳�δ�����ж�״̬����Ҫworker�߳��жϲ��˳�
                try {
                    beforeExecute(wt, task);                          // ����ִ��֮ǰ�ȵ���beforeExecute
                    Throwable thrown = null;
                    try {
                        task.run();                                   // ִ������ע�����ﲻ���ٿ����߳���ִ���ˣ�ֻ�ǵ�����Runnable��run������������RunnableFutureʵ�ֵ�Runnable��
                    } catch (RuntimeException x) {
                        thrown = x; throw x;
                    } catch (Error x) {
                        thrown = x; throw x;
                    } catch (Throwable x) {
                        thrown = x; throw new Error(x);
                    } finally {                                      // ע�⣺��һ���쳣try-catch-finally���߼����ڲ�������쳣���������֣���һ�����Ǽ���throw�׳����ڶ������Ǵ���afterExecute����һ�����׳����쳣����ߵ�try�������ˡ�
                        afterExecute(task, thrown);                   // ����ִ��֮�����afterExecute������ִ�й����в��񵽵��쳣
                    }
                } finally {
                    task = null;
                    w.completedTasks++;                               // ���������+1
                    w.unlock();                                       // ��������Ϊ�����̵߳�����
                }
            }
            completedAbruptly = false;                               // ���getTask������null���ͱ�ʾ��worker��׼�������˳���       
        } finally {                                                  // ע�⣺�ڲ�ѭ��������׳��쳣��ֱ�Ӻ���
            processWorkerExit(w, completedAbruptly);                  // ����completedAbruptlyȥ���Ա�worker�����˳�����ֹ�̳߳�
        }
    }

    // Public constructors and methods
    // �������캯���ͷ���

    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters and default thread factory and rejected execution handler.
     * It may be more convenient to use one of the {@link Executors} factory
     * methods instead of this general purpose constructor.
     * ������
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
     * ͬ��
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
     * ͬ��
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
     * �����µ�ThreadPoolExecutor��ͨ�������ĳ�ʼ��������Ĭ�ϵ��̹߳�����ThreadFactory����Ĭ�ϵľܾ�ִ�д�������RejectedExecutionHandler����
     * ���ܸ�����ģ�convenient����ͨ������Executors�Ĺ��������������Ǵ˹������췽������Ȼ��ʵ���ϴ�������ͨ��������ֱ�ӵ���ThreadPoolExecutor�Ĺ��췽����
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     *        corePoolSize��ʾ�̳߳ر��ֵ��߳�������ʹ��Щ�߳��ǿ��еģ�����������allowCoreThreadTimeOutΪtrue
     * @param maximumPoolSize the maximum number of threads to allow in the
     *        pool
     *        maximumPoolSize��ʾ�̳߳����������߳���
     * @param keepAliveTime when the number of threads is greater than
     *        the core, this is the maximum time that excess idle threads
     *        will wait for new tasks before terminating.
     *        keepAliveTime��ʾ���߳������������߳���ʱ������ģ�excess�������߳�����ֹǰ���ڵȴ�����������ȴ�ʱ��
     * @param unit the time unit for the {@code keepAliveTime} argument
     *        unit��ʾkeepAliveTime��ʱ�䵥λ
     * @param workQueue the queue to use for holding tasks before they are
     *        executed.  This queue will hold only the {@code Runnable}
     *        tasks submitted by the {@code execute} method.
     *        workQueue���ڱ��滹δִ������Ķ��С��ö��н�ֻ����ͨ��execute�����ύ��ʵ��Runnable�ӿڵ�����
     * @param threadFactory the factory to use when the executor
     *        creates a new thread
     *        threadFactor���ڵ�executor�������̵߳�ʱ��
     * @param handler the handler to use when execution is blocked
     *        because the thread bounds and queue capacities are reached
     *        handler���ڵ��̳߳��̱߽߳��빤��������������ʱ����ִ����������Ҫִ�е���ؾܾ�����
     * @throws IllegalArgumentException if one of the following holds:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>   // maximum����<=0
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
        this.keepAliveTime = unit.toNanos(keepAliveTime); // ת��Ϊ����
        this.threadFactory = threadFactory;
        this.handler = handler;
    }

    /**
     * Executes the given task sometime in the future.  The task
     * may execute in a new thread or in an existing pooled thread.
     * ��δ����ĳ��ʱ��ִ�и���������
     * �����������һ���µ��̻߳���һ���Ѿ����ڵ��̳߳��߳���ִ�С�
     *
     * If the task cannot be submitted for execution, either because this
     * executor has been shutdown or because its capacity has been reached,
     * the task is handled by the current {@code RejectedExecutionHandler}.
     * ����������ύִ�У�����������ִ�����Ѿ�shutdown�������������Ѿ����ͣ�
     * ��������¸������ɵ�ǰRejectedExecutionHandler������
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
         * ����������
         *
         * 1. If fewer than corePoolSize threads are running, try to
         * start a new thread with the given command as its first
         * task.  The call to addWorker atomically checks runState and
         * workerCount, and so prevents false alarms that would add
         * threads when it shouldn't, by returning false.
         * 1����������߳���<corePoolSize�����Դ����µ��̣߳�����������command��Ϊ���ĵ�һ������
         * ����addWorker��ԭ���Եļ��runState��workerCount�������Ӧ�ô����µ�worker�̣߳�addWorkerͨ������false�Է�ֹ����߳�ʱ���󱨡�
         *
         * 2. If a task can be successfully queued, then we still need
         * to double-check whether we should have added a thread
         * (because existing ones died since last checking) or that
         * the pool shut down since entry into this method. So we
         * recheck state and if necessary roll back the enqueuing if
         * stopped, or start a new thread if there are none.
         * 2����������ܹ��ɹ���ӣ���������Ҫ�ٴμ�������Ƿ�Ӧ�����һ���̣߳���Ϊ�ϴμ��֮�����̹߳ҵ��ˣ�������̳߳��߳���������Ҫ����ô��Ҫ�����µ��̣߳�
         * �����ڽ���÷������̳߳���shutdown��
         * �����������¼��״̬���б�Ҫ�Ļ���ֹͣʱ�ع���Ӳ�����������û���̵߳����������һ�����̡߳�
         *
         * 3. If we cannot queue task, then we try to add a new
         * thread.  If it fails, we know we are shut down or saturated
         * and so reject the task.
         * 3������޷���task��ӣ���ô�������һ���µ��̡߳�
         * ������ʧ�ܣ���֪���̳߳��Ѿ�shutdown���߱��ͣ������Ҫ�ܾ�������
         *
         */
        int c = ctl.get();
        if (workerCountOf(c) < corePoolSize) {
            if (addWorker(command, true))                    // 1�������ǰ�߳���<corePoolSize����ô������worker�̣߳�����������ΪfirstTask
                return;
            c = ctl.get();                                    // 2����������workerʧ�ܣ����²�ѯ��ǰ�̳߳���Ϣ
        }
        if (isRunning(c) && workQueue.offer(command)) {       // ���̳߳ش���RUNNING��ǰ���£�����command��ӣ�offerΪ��������ӣ�ʧ�ܷ���false��
            int recheck = ctl.get();                          // ���¼���̳߳�״̬�����recheck�о��Ǹ����ϣ�ȷ���ύ������һ���ᱻ����������worker��ִ�У��������̳߳ز�����������е�������Ƴ�������
            if (! isRunning(recheck) && remove(command))      // �����ʱ�̳߳ش��ڹر�״̬��>=SHUTDOWN�������Ƴ���command
                reject(command);                              // �ܾ��������൱����Ϊ���ύ���������ܾ��ģ���SHUTDOWN״̬���ǻᴦ���������е�����
            else if (workerCountOf(recheck) == 0)            // ����̳߳ػ�����RUNNINGʱ��worker�߳�Ϊ0������Ҫ���worker�߳�����������
                addWorker(null, false);
        }
        else if (!addWorker(command, false))                 // 3������߳���>corePoolSize���������ʧ�ܣ������������ˣ�����ô���½�worker�̴߳�������񣨴���corePoolSize��maximumPoolSize�м��worker�̣߳�
            reject(command);                                  // ����½�workerʧ�ܣ����統ǰrs>=SHUTDOWN������ܾ�������
    }

    /**
     * Initiates an orderly shutdown in which previously submitted
     * tasks are executed, but no new tasks will be accepted.
     * Invocation has no additional effect if already shut down.
     * ��������رգ�����֮ǰ���ύ������ִ�У������ٽ���������
     * ����Ѿ�����SHUTDOWN���ظ����ò����ж����Ч����
     *
     * <p>This method does not wait for previously submitted tasks to
     * complete execution.  Use {@link #awaitTermination awaitTermination}
     * to do that.
     * �÷�������ȴ�֮ǰ���ύ���������ִ�С�����˼���Ǹ÷�����֪ͨ�̳߳�ȥSHUTDOWN��������ȴ�����worker�̶߳�����˳����̳߳���ֹ��
     * ʹ��awaitTermination������Щ�¶�����ʱ�ȴ��������������̳߳���ֹ����
     * �����÷���
     * threadPoolExecutor.shutdown();
     * while(!threadPoolExecutor.awaitTermination(...)) { // ѭ�� ��ʱ�ȴ�worker�߳�����˳����̳߳�״̬��ΪTERMINATED};
     *
     * @throws SecurityException {@inheritDoc}
     *
     * ע�⣬�÷��������˳�ʱ����ʾ����ȷ�������̳߳�״̬ΪSHUTDOWN��������֤����worker�߳�����ɣ�Ҳ����֤�̳߳�״̬����TERMINATED״̬
     *
     */
    public void shutdown() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();                                               // �漰worker���ϵĲ�����mainLock����ΪҪ�����worker�̣߳�
        try {
            checkShutdownAccess();                                     // ���shutdownȨ��
            advanceRunState(SHUTDOWN);                                 // �����̳߳�״̬ΪSHUTDOWN�����rs<SHUTDOWN��
            interruptIdleWorkers();                                    // �ж�����*����*worker�߳�
            onShutdown(); // hook for ScheduledThreadPoolExecutor      // ����onShutDown���ӷ�����ͨ�������ScheduledThreadPoolExecutor
        } finally {
            mainLock.unlock();                                         // �ǵý���
        }
        tryTerminate();                                                // ������ֹ�̳߳أ����������TERMINATED��������ͨ���жϿ����߳����ȴ��̳߳��Լ���ֹ
    }

    /**
     * Attempts to stop all actively executing tasks, halts the
     * processing of waiting tasks, and returns a list of the tasks
     * that were awaiting execution. These tasks are drained (removed)
     * from the task queue upon return from this method.
     * ����ֹͣ��������ִ�е�����ֹͣ��halt������ȴ����񣬷��صȴ�ִ�е������б�
     * �Ӹ÷�������ʱ������Щ�ȴ�ִ�е��������������ų����Ƴ�����
     *
     * <p>This method does not wait for actively executing tasks to
     * terminate.  Use {@link #awaitTermination awaitTermination} to
     * do that.
     * �÷�������ȴ�����ִ�е�����ִ����ɡ�����˼���Ǹ÷�����֪ͨ�̳߳�ȥSTOP��������ȴ�����worker�߳�����˳����̳߳���ֹ��
     * ʹ��awaitTermination��������һ�㡣��������ʱ�ȴ�����̳߳���������Ƿ���ִ����ϣ��̳߳��ѹرգ�
     *
     * <p>There are no guarantees beyond best-effort attempts to stop
     * processing actively executing tasks.  This implementation
     * cancels tasks via {@link Thread#interrupt}, so any task that
     * fails to respond to interrupts may never terminate.
     * ���˾�������ֹͣ��������ִ�е������⣬û���κα�֤��
     * ͨ��Thread#interruptʵ��ȡ�����������κ�δ����Ӧ�жϵĵ����������Զ������ֹ��
     * �����÷���
     * threadPoolExecutor.shutdown();
     * while(!threadPoolExecutor.awaitTermination(...)) { // ѭ�� ��ʱ�ȴ�worker�߳�����˳����̳߳�״̬��ΪTERMINATED};
     *
     * @throws SecurityException {@inheritDoc}
     *
     * ע�⣬�÷��������˳�ʱ����ʾ����ȷ�������̳߳�״̬ΪSHUTDOWN��������֤����worker�߳�����ɣ�Ҳ����֤�̳߳�״̬����TERMINATED״̬
     *
     */
    public List<Runnable> shutdownNow() {
        List<Runnable> tasks;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();                                               // �漰worker���ϵĲ�����mainLock����ΪҪ�����worker�̣߳�
        try {
            checkShutdownAccess();                                     // ���shutdownȨ��
            advanceRunState(STOP);                                     // �����̳߳�״̬ΪSTOP�����rs<STOP��
            interruptWorkers();                                        // �ж������̣߳���shutdownֻ�жϿ����̲߳�ͬ��
            tasks = drainQueue();                                      // �����������еȴ������ų��������ɵ�һ���µ�List��
        } finally {
            mainLock.unlock();
        }
        tryTerminate();                                                // ������ֹ�̳߳�
        return tasks;                                                 // ���ع�����������δִ�е������б�
    }

    // �ж��̳߳�״̬�Ƿ�ֹͣ��rs>RUNNING��
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
     * ���executor��shutdown����shutdownNow֮��������ֹ����δ��ȫ��ֹ���÷�������true��
     * �÷������ܶ�debug���á������shutdown֮���㹻��sufficient������ʱ����÷����Է���true��
     * ���ܱ����ύ�������Ѿ����Ի����������жϣ�����Ӧ�жϣ�������executor�޷�������ֹ��
     *
     * @return {@code true} if terminating but not yet terminated
     *          ������ֹ��δ�����ֹ������true
     */
    public boolean isTerminating() {
        int c = ctl.get();
        return ! isRunning(c) && runStateLessThan(c, TERMINATED);
    }

    // ����̳߳��Ƿ�����ֹ
    public boolean isTerminated() {
        return runStateAtLeast(ctl.get(), TERMINATED);
    }

    // ������shutdown()��shutdonwNow()����֮�󣬵��ø÷������ȴ�����̳߳��Ƿ��ѹر�
    public boolean awaitTermination(long timeout, TimeUnit unit)
        throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();                                          // ͨ����������ֹ������worker�Ĳ����������������/����worker��
        try {
            for (;;) {
                if (runStateAtLeast(ctl.get(), TERMINATED))       // ����̳߳�״̬�ѵ���TERMINATED��˵�������̳߳��ѹرգ�û�������е�worker�߳��ˣ�������true
                    return true;
                if (nanos <= 0)
                    return false;
                nanos = termination.awaitNanos(nanos);            // ͨ��Condition����ʱ�ȴ�������̳߳���ֹ�ˣ�tryTerminate��ͨ������termination.signalAll�����ѣ���
            }
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Invokes {@code shutdown} when this executor is no longer
     * referenced and it has no threads.
     * ���������ô�executor����û���߳�ʱִ�йر�
     *
     */
    protected void finalize() {
        shutdown();
    }

    /**
     * Sets the thread factory used to create new threads.
     * �����̹߳������ڴ������߳�
     *
     * @param threadFactory the new thread factory
     * @throws NullPointerException if threadFactory is null
     * @see #getThreadFactory
     */
    public void setThreadFactory(ThreadFactory threadFactory) {
        if (threadFactory == null)
            throw new NullPointerException();
        this.threadFactory = threadFactory;     // ͨ��volatile���ֿɼ���
    }

    /**
     * Returns the thread factory used to create new threads.
     * �������ڴ����̵߳��̹߳���
     *
     * @return the current thread factory
     * @see #setThreadFactory(ThreadFactory)
     */
    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    /**
     * Sets a new handler for unexecutable tasks.
     * �����µ�handler�����޷�ִ�е�����
     *
     * @param handler the new handler
     * @throws NullPointerException if handler is null
     * @see #getRejectedExecutionHandler
     */
    public void setRejectedExecutionHandler(RejectedExecutionHandler handler) {
        if (handler == null)
            throw new NullPointerException();
        this.handler = handler;              // ͨ��volatile���ֿɼ���
    }

    /**
     * Returns the current handler for unexecutable tasks.
     * ���ص�ǰ���ڴ����޷�ִ�������handler
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
     * ���ú����߳�����
     * ��Ḳ�ǹ��캯�������õ�����ֵ�������ǲ��ܹ��캯����ԭֵ�Ƿ����/С��/����Ŀ��ֵ����������ΪĿ��ֵ��
     * �����ֵ<��ǰֵ��ԭֵ��������������̻߳�����һ�ο���ʱ��ֹ��
     * �����ֵ>��ǰֵ��ԭֵ���������Ҫ�Ļ������������߳���ִ�������Ŷ�����
     *
     * @param corePoolSize the new core size
     * @throws IllegalArgumentException if {@code corePoolSize < 0} // ע�ⲻ��<0
     * @see #getCorePoolSize
     */
    public void setCorePoolSize(int corePoolSize) {
        if (corePoolSize < 0)                                         // �����߳�������<0
            throw new IllegalArgumentException();
        int delta = corePoolSize - this.corePoolSize;
        this.corePoolSize = corePoolSize;
        if (workerCountOf(ctl.get()) > corePoolSize)
            interruptIdleWorkers();                                   // �����ǰ�߳���>���º�ĺ����߳����������жϿ���worker�߳�
        else if (delta > 0) {                                        // ������º�����߳���>ԭֵ����Ҫ�����Ƿ�����Ҫ��Ϊ������worker�̶߳����������˽ϴ�ĺ����߳�����
            // We don't really know how many new threads are "needed".
            // As a heuristic, prestart enough new workers (up to new
            // core size) to handle the current number of tasks in
            // queue, but stop if queue becomes empty while doing so.
            // ���ǲ�֪����Ҫ�����������̡߳�
            // ��Ϊһ������ʽ��heuristic��������Ԥ�������㹻�����worker�̣߳����ﵽ�µĺ����߳���������������еĵ�ǰ����
            // �������ִ�д˲��������ж��б�գ���ֹͣ�����µ�worker�̣߳�ԭ��������worker�̻߳���������û������allowCoreThreadTimeOut����
            //
            int k = Math.min(delta, workQueue.size());              // ���������߳���ȡ�����߳����뵱ǰ�����������е���Сֵ
            while (k-- > 0 && addWorker(null, true)) {             // �����������߳�̫�࣬�в���Ҫ���˷�
                if (workQueue.isEmpty())                             // ����������Ϊ���ˣ���ô���ܵ�ǰworker�߳����Ƿ�ﵽ�˺����߳�������ֹͣ�����µ��߳�
                    break;
            }
        }
    }

    /**
     * Returns the core number of threads.
     * ���غ����߳���
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
     * ���������̣߳����������ŵȴ�����
     * �÷���������Ĭ��ֻ����ִ���������ʱ������������̵߳Ĳ��ԣ�Ĭ�����û����Ͳ������̣߳�
     * �÷����᷵��false��������еĺ����߳��ڴ�֮ǰ���Ѿ������ˣ��ڽ���÷���ǰ��worker�߳����Ѵﵽ�����߳�����
     * ��Ԥ����һ�������̣߳��������ύ֮ǰ׼�����̣߳�
     *
     * @return {@code true} if a thread was started
     */
    public boolean prestartCoreThread() {
        return workerCountOf(ctl.get()) < corePoolSize &&
            addWorker(null, true);                         // ֻ����һ�������߳�
    }

    /**
     * Same as prestartCoreThread except arranges that at least one
     * thread is started even if corePoolSize is 0.
     * ��prestartCoreThread��ͬ����������һ���߳���������ʹ���õ�corePoolSizeΪ0
     * ����֤�̳߳���������һ��worker�̣߳���ʹ�������̳߳ص�corePoolSizeΪ0��
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
     * �������к����̣߳������ǿ��еȴ���������
     * �÷���������Ĭ��ֻ����ִ���������ʱ������������̵߳Ĳ��ԣ�Ĭ�����û����Ͳ������̣߳�
     *
     * @return the number of threads started
     *         �����������߳���
     */
    public int prestartAllCoreThreads() {
        int n = 0;
        while (addWorker(null, true))     // ����addWorker()�����У�true������ʾ��Ҫ���������̣߳������ǰworker�߳���>corePoolSize������false
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
     * ������̳߳���������߳���keepAliveʱ����û�����񵽴�ʱ��ʱ����ֹ���򷵻�true��
     * ���������񵽴�ʱ������Ҫ�����滻������֪������滻��addWorker()���滻����ʲô��ϵ��������
     * ������trueʱ��Ӧ���ڷǺ����̵߳�keep-alive����Ҳ��Ӧ���ں����̡߳�
     * ������falseʱ��Ҳ����allowCoreThreadTimeOut��Ĭ��ֵ���������߳���Զ��������ȱʧ�����������ֹ��
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
     * ���ò��ԣ���Ҫ���ڿ��Ƹ��̳߳غ����߳���keepAliveʱ����û�����񵽴�ʱ��ʱ����ֹ��
     * ���������񵽴�ʱ������Ҫ�����滻��
     * �������Ϊfalse�������߳���Զ��������ȱʧ�����������ֹ��
     * �������Ϊtrue��Ӧ���ڷǺ����̵߳�keep-alive����Ҳ��Ӧ���ں����̡߳�
     * Ϊ���ⲻ�ϸ����̣߳�������Ϊtrueʱ��keep-alive��ֵ�������0��
     * ͨ��������ʹ���̳߳�֮ǰ���ô˷�����
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
        if (value != allowCoreThreadTimeOut) {     // �����ֵ������ԭ����allowCoreThreadTimeOut����ô��������ֵ
            allowCoreThreadTimeOut = value;
            if (value)
                interruptIdleWorkers();            // ���ԭ������true������Ϊtrue�ˣ����жϿ��е��߳�
        }
    }

    /**
     * Sets the maximum allowed number of threads. This overrides any
     * value set in the constructor. If the new value is smaller than
     * the current value, excess existing threads will be
     * terminated when they next become idle.
     * �������������߳�����
     * �÷������ǹ��췽�������õ�����ֵ��
     * �����ֵ<��ǰֵ��ԭֵ�������ڵĶ��ࣨexcess���߳̽���������һ�ο���ʱ����ֹ
     *
     * @param maximumPoolSize the new maximum
     * @throws IllegalArgumentException if the new maximum is
     *         less than or equal to zero, or
     *         less than the {@linkplain #getCorePoolSize core pool size}
     *         Ҫ��֤maximumPoolSize>=corePoolSize>0
     * @see #getMaximumPoolSize
     */
    public void setMaximumPoolSize(int maximumPoolSize) {
        if (maximumPoolSize <= 0 || maximumPoolSize < corePoolSize)
            throw new IllegalArgumentException();
        this.maximumPoolSize = maximumPoolSize;
        if (workerCountOf(ctl.get()) > maximumPoolSize) // �����ǰworker�߳���>��ֵmaximumPoolSize����Ҫ�������߳���ֹ
            interruptIdleWorkers();
    }

    /**
     * Returns the maximum allowed number of threads.
     * �������������߳���
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
     * �����߳�����ֹǰ���Ա��ֿ��е�ʱ�����ơ������ж���ʱ������ֹ��
     * ������̳߳��еĵ�ǰ�߳������������߳�����������εȴ�ʱ����û��ִ�����񣬶�����߳̽�����ֹ��
     * ��Ḳ�ǹ��캯�������õ�����ֵ��
     *
     * @param time the time to wait.  A time value of zero will cause
     *        excess threads to terminate immediately after executing tasks.
     *        time�������ȴ�ʱ�䡣���timeֵΪ0��������ִ�ж�����̣߳����������̵߳��ǲ����̣߳���ִ����������������ֹ
     *
     * @param unit the time unit of the {@code time} argument
     * @throws IllegalArgumentException if {@code time} less than zero or
     *         if {@code time} is zero and {@code allowsCoreThreadTimeOut}
     *         ע�⣬���뱣֤time>=0��
     *               ���������allowCoreThreadTimeOut�����뱣֤time>0��Ҫ��Ȼ����ס�����̣߳�
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
        if (delta < 0)               // ���keepAliveTimeʱ���С������Ҫ������ֹ�ѳ�ʱ�Ŀ����߳�
            interruptIdleWorkers();
    }

    /**
     * Returns the thread keep-alive time, which is the amount of time
     * that threads in excess of the core pool size may remain
     * idle before being terminated.
     * �����߳�keep-aliveʱ�������ǳ����̳߳غ����߳������߳�����ֹ֮ǰ���ܱ��ֿ��е�ʱ����
     *
     * @param unit the desired time unit of the result
     * @return the time limit
     * @see #setKeepAliveTime(long, TimeUnit)
     */
    public long getKeepAliveTime(TimeUnit unit) {
        return unit.convert(keepAliveTime, TimeUnit.NANOSECONDS); // ת��Ϊ��ε�ʱ�䵥λ����
    }

    /* User-level queue utilities */
    // �û����Ķ��в���

    /**
     * Returns the task queue used by this executor. Access to the
     * task queue is intended primarily for debugging and monitoring.
     * This queue may be in active use.  Retrieving the task queue
     * does not prevent queued tasks from executing.
     * �����̳߳�ʹ�õ�������С�
     * �������������Ҫ����debug���ء�
     * �ö��п�������ʹ�ã���̬�仯��
     * ����������в�����ֹ��ӵ�����ִ�С�
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
     * �����������ڣ�present������executor���ڲ��������Ƴ���������ᵼ����δ���������񲻻�ȥ���С�
     *
     * <p>This method may be useful as one part of a cancellation
     * scheme.  It may fail to remove tasks that have been converted
     * into other forms before being placed on the internal queue. For
     * example, a task entered using {@code submit} might be
     * converted into a form that maintains {@code Future} status.
     * However, in such cases, method {@link #purge} may be used to
     * remove those Futures that have been cancelled.
     * �÷�������Ϊȡ��������һ���֡�
     * �������޷�ɾ����Щ�ڷ����ڲ�����֮ǰ��ת��Ϊ������ʽ������
     * ���磬ʹ��submit������ӵ��������ת��Ϊ�˺���Future״̬����ʽ��
     * ������Runnableͨ��AbstractExecutorService#submit()ת����FutureTask����������̽���ʽת�������������Runnable������remove����ɾ�����˵ģ�
     * ���ǣ�����������£�purge����������ɾ���ⲿ���Ѿ�ȡ����Future��
     *
     * @param task the task to remove
     * @return {@code true} if the task was removed
     */
    public boolean remove(Runnable task) {
        boolean removed = workQueue.remove(task);
        tryTerminate(); // In case SHUTDOWN and now empty // ����rs=SHUTDOWN����������зǿյ�����£�ɾ����һ���������������Ҫ������ֹ�̳߳ء�
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
     * ���Դӹ���������ɾ��������ȡ����Future����
     * �÷����������洢��storage�����գ�reclamation���������Թ���û������Ӱ�졣
     * ȡ����������Զ����ִ�У������ܻ��ڹ����������ۻ���ֱ��worker�߳�����ɾ�����ǡ���worker����������run��������FutureTask�У����run��Callable��state��ΪNEW����ֱ�ӽ�����
     * ���ڵ��ø÷����᳢��ɾ�����ǡ�
     * Ȼ�������ַ������ܻ��ڣ�presence ���ڣ������̸߳��ţ�interference�����޷�ɾ������
     *
     */
    public void purge() {
        final BlockingQueue<Runnable> q = workQueue;
        try {
            Iterator<Runnable> it = q.iterator();
            while (it.hasNext()) {
                Runnable r = it.next();
                if (r instanceof Future<?> && ((Future<?>)r).isCancelled()) // �ж������Ƿ�ʵ����Future�ӿڣ����жϸ�Future�����Ƿ���ȡ��
                    it.remove();
            }
        } catch (ConcurrentModificationException fallThrough) {
            // Take slow path if we encounter interference during traversal.
            // Make copy for traversal and call remove for cancelled entries.
            // The slow path is more likely to be O(N*N).
            // ���������޸��쳣
            // ����ڱ����������������ţ���ȡslow path��
            // Ϊ��������������������remove��ȡ��entries�����Ǳ�������
            // slow path��ʱ�临�ӶȺܴ�Ŀ���ΪO(N*N)
            // 
            for (Object r : q.toArray())
                if (r instanceof Future<?> && ((Future<?>)r).isCancelled())
                    q.remove(r);
        }

        tryTerminate(); // In case SHUTDOWN and now empty // ����rs=SHUTDOWN����������зǿյ�����£�ɾ����һ���������������Ҫ������ֹ�̳߳ء�
    }

    /* Statistics */
    // ͳ������

    /**
     * Returns the current number of threads in the pool.
     * ���ص�ǰ�̳߳��߳���
     *
     * @return the number of threads
     */
    public int getPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();                                       // ����worker���ϵĲ�����Ҫ��mainLock
        try {
            // Remove rare and surprising possibility of
            // isTerminated() && getPoolSize() > 0
            // һ����˵����ִ�е�isTerminated()����ʱ���̳߳���Ӧ����û��worker�߳���
            // ����Ϊ�˷�ֹ�����뾪�ȵĿ����Է�����������������е����ж�
            return runStateAtLeast(ctl.get(), TIDYING) ? 0 // ���rs=TIDYING��ֱ�ӷ���0������ȥ���worker���ϵ�����
                : workers.size();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns the approximate number of threads that are actively
     * executing tasks.
     * ��������ִ��������̴߳��£�approximate�������������������е��̣߳�
     *
     * @return the number of threads
     */
    public int getActiveCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();                // ��worker���ϵĲ�������Ҫ��mainLock
        try {
            int n = 0;
            for (Worker w : workers)
                if (w.isLocked())       // �ж��߳��Ƿ�����ִ������������ǿ���worker�Ƿ��м���
                    ++n;
            return n;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns the largest number of threads that have ever
     * simultaneously been in the pool.
     * ����ͬʱ��simultaneously�������̳߳ص���ʷ����߳�����
     *
     * @return the number of threads
     */
    public int getLargestPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();            // ��worker���ϵĲ�������Ҫ��mainLock����ΪlargestPoolSize������workers.size()������Ҳ������
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
     * �����Ѱ���ִ�е����������Ԥ��ֵ����������ִ����ɡ�����ִ�С���ӵȴ�ִ�е�������������
     * �����ڼ������������״̬���߳�״̬���ܶ�̬�仯����˷���ֵֻ��һ������ֵ��
     *
     * @return the number of tasks
     */
    public long getTaskCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();                    // ��worker���ϵĲ�������Ҫ��mainLock
        try {
            long n = completedTaskCount;
            for (Worker w : workers) {
                n += w.completedTasks;      // �������ۼ���ִ�����������
                if (w.isLocked())
                    ++n;                    // �������ۼӵ�ǰworker����ִ������
            }
            return n + workQueue.size();    // �������ۼӹ�����������δִ�е�������
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
     * ������ִ����ɵ���������Ԥ��ֵ��
     * �����ڼ������������״̬���߳�״̬���ܶ�̬�仯����˷���ֵֻ��һ������ֵ��
     * �������������ñ�����ʱ��ֵ��Զ������١�
     *
     * @return the number of tasks
     */
    public long getCompletedTaskCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();                    // ��worker���ϵĲ�������Ҫ��mainLock
        try {
            long n = completedTaskCount;
            for (Worker w : workers)
                n += w.completedTasks;     // �������ۼ���ִ�����������
            return n;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns a string identifying this pool, as well as its state,
     * including indications of run state and estimated worker and
     * task counts.
     * ���ر�ʶ���̳߳ؼ���״̬���ַ�������������״̬��Ԥ��worker��������������
     *
     * @return a string identifying this pool, as well as its state
     */
    public String toString() {
        long ncompleted;        // ���������
        int nworkers, nactive;  // worker�߳�������Ծworker�߳�������Ծ=�ǿ��У�
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
            "[" + rs +                                  // ����״̬
            ", pool size = " + nworkers +               // �̳߳ش�С=worker�߳���
            ", active threads = " + nactive +           // ��Ծ�߳���=�ǿ����߳���
            ", queued tasks = " + workQueue.size() +    // �Ŷӵȴ�������=�������д�С
            ", completed tasks = " + ncompleted +       // ���������
            "]";
    }

    /* Extension hooks */
    // ��չ�Ĺ��ӷ���

    /**
     * Method invoked prior to executing the given Runnable in the
     * given thread.  This method is invoked by thread {@code t} that
     * will execute task {@code r}, and may be used to re-initialize
     * ThreadLocals, or to perform logging.
     * �ڸ������߳���ִ�и�����Runnable����ʱ������ִ�и÷�����
     * ���߳�t��Ҫִ������r��ʱ����ø÷��������������³�ʼ��ThreadLocals�����߼�¼��־��
     *
     * <p>This implementation does nothing, but may be customized in
     * subclasses. Note: To properly nest multiple overridings, subclasses
     * should generally invoke {@code super.beforeExecute} at the end of
     * this method.
     * ����÷���û�о���ʵ�֣����������п��ܻ��Զ���ʵ��
     * ע�⣺Ϊ����ȷ��Ƕ�׸��ǣ�����ͨ��Ӧ���ڸ÷�����ĩβ����super.beforeExecute
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
     * ��ִ���������Runnable�������ø÷�����
     * ��ִ��������̵߳��ø÷�����
     * ���Throwable�ǿգ���ʾ����ִ��ͻȻ�жϲ���δ�������RunntimeException��Error
     * �����δ�����񣬱�ʾδ��Runnable�ڲ�catch������runWorker��catch���ˣ���ͨ������afterExecute���д���
     *
     * <p>This implementation does nothing, but may be customized in
     * subclasses. Note: To properly nest multiple overridings, subclasses
     * should generally invoke {@code super.afterExecute} at the
     * beginning of this method.
     * ����÷���û�о���ʵ�֣����������п��ܻ��Զ���ʵ��
     * ע�⣺Ϊ����ȷ��Ƕ�׸��ǣ�����ͨ��Ӧ���ڸ÷���ǰ�����super.afterExecute����beforeExecute˳���෴��
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
     * ע�⣺����������ȷ�Ļ���ͨ��submit���������������У�����FutureTask������������󲶻��ά�������쳣��
     * ���������ᵼ��ͻȻ��ֹ�������ڲ����쳣���ᴫ�ݸ��˷�����
     * ���ع�һ��FutureTask��run�������÷���ִ��ʱ�����׳��쳣�����ǽ��쳣��set����outcome�ֻ��ͨ��get������ִ�н��outcomeʱ������֪�����н���Ƿ����쳣��
     * ��������ᴫ����˼�ǣ��ⲿ���쳣����ͨ��Throwable t��δ��ݣ�����task�ڲ����쳣��task�ڲ�ά�����쳣�����׳�����runWorker��Ҳ���񲻵���
     * �����Ҫ�ڸ÷����в���������ʧ�ܣ����Խ�һ��̽����������
     * �����������������������������ֹ��abort��������ӡֱ��ԭ����ߵײ��쳣��
     *
     *  <pre> {@code
     * class ExtendedExecutor extends ThreadPoolExecutor {
     *   // ...
     *   protected void afterExecute(Runnable r, Throwable t) {
     *     super.afterExecute(r, t);                                 // �ȵ��ø���afterExecute
     *     if (t == null && r instanceof Future<?>) {                // ���runWorker���񵽵��쳣Ϊnull������Runnable rΪFuture���ͣ������쳣��Futureά������û��throw��runWorker��
     *       try {
     *         Object result = ((Future<?>) r).get();                // ����Future��getʱ�����ִ�й����з����쳣����ô�÷�����ֱ���׳�����ά��������쳣
     *       } catch (CancellationException ce) {                    // ����Futureά�����쳣
     *           t = ce;
     *       } catch (ExecutionException ee) {
     *           t = ee.getCause();
     *       } catch (InterruptedException ie) {
     *           Thread.currentThread().interrupt(); // ignore/reset  // �����ж��쳣�����������ж�
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
     * ��executor��ֹʱ���ø÷�����
     * Ĭ�ϱ���÷���û�о���ʵ�֡�
     * ע�⣺Ϊ����ȷ��Ƕ��(nest �� mutiple �����Ƕ�׶��)���ǣ�����ͨ��Ӧ���ڸ÷����е���super.terminated��
     * 
     */
    protected void terminated() { }

    /* Predefined RejectedExecutionHandlers */
    /* Ԥ�����RejectedExecutionHandlers */

    /**
     * A handler for rejected tasks that runs the rejected task
     * directly in the calling thread of the {@code execute} method,
     * unless the executor has been shut down, in which case the task
     * is discarded.
     * ����ܾ������handler����ֱ����execute�����ĵ����߳���ִ�б��ܾ�������
     * ����executor�Ѿ��رգ�����������¸����񽫱�������
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
         * ��caller�߳���ִ������r������executor�Ѿ��رգ���������¸����񽫱�������
         *
         * @param r the runnable task requested to be executed    r����Ҫִ�е�����
         * @param e the executor attempting to execute this task  e�ǳ���ִ�и������executor�������жϸ�executor�̳߳��Ƿ񱻹رգ�
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
     * ����ܾ������handler���׳�RejectedExecutionException
     *
     */
    public static class AbortPolicy implements RejectedExecutionHandler {
        /**
         * Creates an {@code AbortPolicy}.
         */
        public AbortPolicy() { }

        /**
         * Always throws RejectedExecutionException.
         * �����׳�RejectedExecutionException
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
     * ����ܾ������handler����Ĭ�����ܾ�������
     *
     */
    public static class DiscardPolicy implements RejectedExecutionHandler {
        /**
         * Creates a {@code DiscardPolicy}.
         */
        public DiscardPolicy() { }

        /**
         * Does nothing, which has the effect of discarding task r.
         * ɶҲ�������ͻᾲĬ��������r
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
     * ����ܾ������handler����������δ������������񣩣�Ȼ��Ե�ǰ�ܾ�����������execute��
     * ����executor�رգ���������¸����񽫱�������
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
         * ��ȡ������executor��Ҫִ�е���һ������
         * ���һ�������������ã���ô��������r��ִ�У�
         * ����executor�رգ���������¸�����r����������
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            if (!e.isShutdown()) {
                e.getQueue().poll(); // poll ��������û�з���null
                e.execute(r);
            }
        }
    }
}
