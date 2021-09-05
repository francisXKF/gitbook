# AbstractOwnableSynchronizer
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

package java.util.concurrent.locks;

/**
 * A synchronizer that may be exclusively owned by a thread.  This
 * class provides a basis for creating locks and related synchronizers
 * that may entail a notion of ownership.  The
 * {@code AbstractOwnableSynchronizer} class itself does not manage or
 * use this information. However, subclasses and tools may use
 * appropriately maintained values to help control and monitor access
 * and provide diagnostics.
 * 同步器可能被一个线程独占
 * 该类为创建可能需要所有权概念（notion）的锁和相关同步器提供了基础。
 * AbstractOwnableSynchronizer类本身不管理或使用这些信息。
 * 但是子类和工具可以使用适当（appropriately）维护（maintain）的值来帮助控制、访问控制器、提供诊断。
 *
 * @since 1.6
 * @author Doug Lea
 */
public abstract class AbstractOwnableSynchronizer
    implements java.io.Serializable {

    /** Use serial ID even though all fields transient. */
    private static final long serialVersionUID = 3737899427754241961L;

    /**
     * Empty constructor for use by subclasses.
     */
    protected AbstractOwnableSynchronizer() { }

    /**
     * The current owner of exclusive mode synchronization.
     * 独占锁当前的拥有者
     * transient 在序列化过程中，用transient修饰的属性不会被序列化，也就是在序列化之后该属性无法被访问
     * 一旦变量被transient修饰，变量将不再是对象持久化的一部分
     */
    private transient Thread exclusiveOwnerThread;

    /**
     * Sets the thread that currently owns exclusive access.
     * A {@code null} argument indicates that no thread owns access.
     * This method does not otherwise impose any synchronization or
     * {@code volatile} field accesses.
     * @param thread the owner thread
     * 设置线程为当前独占访问的拥有者。
     * 参数为null表示没有线程拥有访问。也就是没有被占用。
     * 此方法不会以其他方式强加任何同步或者volatile字段访问？？？
     */
    protected final void setExclusiveOwnerThread(Thread thread) {
        exclusiveOwnerThread = thread;
    }

    /**
     * Returns the thread last set by {@code setExclusiveOwnerThread},
     * or {@code null} if never set.  This method does not otherwise
     * impose any synchronization or {@code volatile} field accesses.
     * @return the owner thread
     * 返回最后一次通过setExclusiveOwnerThread设置的线程，如果从来没有设置，返回null。
     * 此方法不会以其他方式强加任何同步或者volatile字段访问？？？
     */
    protected final Thread getExclusiveOwnerThread() {
        return exclusiveOwnerThread;
    }
}
```