# ConcurrentMap

```java
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
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A {@link java.util.Map} providing thread safety and atomicity
 * guarantees.
 * 提供线程安全与原子操作保证的Map。
 *
 * <p>Memory consistency effects: As with other concurrent
 * collections, actions in a thread prior to placing an object into a
 * {@code ConcurrentMap} as a key or value
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * actions subsequent to the access or removal of that object from
 * the {@code ConcurrentMap} in another thread.
 * 内存一致性影响：与其他并发集合一样，在线程A将对象作为key或者value放入ConcurrentMap之前，
 * 线程A的操作发生在另一个线程从ConcurrentMap访问或删除该对象之前。
 * （保证插入的操作对插入后的访问操作是可见的）
 *
 * <p>This interface is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 * 该接口是Java Collections Framework的一员。
 *
 * @since 1.5
 * @author Doug Lea
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public interface ConcurrentMap<K, V> extends Map<K, V> {

    /**
     * {@inheritDoc}
     * 在Map接口也有default实现
     *
     * @implNote This implementation assumes that the ConcurrentMap cannot
     * contain null values and {@code get()} returning null unambiguously means
     * the key is absent. Implementations which support null values
     * <strong>must</strong> override this default implementation.
     * 实现说明，该实现假定ConcurrentMap不能包含value为null值，并且get()返回null明确（unambiguously）表示key不存在（absent）。
     * 支持value为null的实现必须重写（override）该默认实现。
     *
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @since 1.8
     */
    @Override
    default V getOrDefault(Object key, V defaultValue) {
        V v;
        return ((v = get(key)) != null) ? v : defaultValue; // 与Map接口的default不同在于，value不能为null（或者说value为null会返回给定的默认值）
    }

   /**
     * {@inheritDoc}
     *
     * @implSpec The default implementation is equivalent to, for this
     * {@code map}:
     * <pre> {@code
     * for ((Map.Entry<K, V> entry : map.entrySet())
     *     action.accept(entry.getKey(), entry.getValue());
     * }</pre>
     * 对于该map，默认实现等效于：
     * for ((Map.Entry<K, V> entry : map.entrySet())
     *     action.accept(entry.getKey(), entry.getValue());
     *
     * @implNote The default implementation assumes that
     * {@code IllegalStateException} thrown by {@code getKey()} or
     * {@code getValue()} indicates that the entry has been removed and cannot
     * be processed. Operation continues for subsequent entries.
     * 默认实现假定，当getKey()或者getValue()抛出IllegalStateException时，表示该entry已经被移除并且无法处理。
     * 会继续操作后续的entry
     *
     * @throws NullPointerException {@inheritDoc}
     * @since 1.8
     */
    @Override
    default void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        for (Map.Entry<K, V> entry : entrySet()) {
            K k;
            V v;
            try {
                k = entry.getKey();
                v = entry.getValue();
            } catch(IllegalStateException ise) {
                // this usually means the entry is no longer in the map.
                continue;
            }
            action.accept(k, v);  // 不太清楚这个action.accept做了啥
        }
    }

    /**
     * If the specified key is not already associated
     * with a value, associate it with the given value.
     * This is equivalent to
     *  <pre> {@code
     * if (!map.containsKey(key))
     *   return map.put(key, value);
     * else
     *   return map.get(key);
     * }</pre>
     * 如果给定的key尚未关联value，则关联到给定的value。
     * 等价于：
     * if (!map.containsKey(key))
     *   return map.put(key, value);
     * else
     *   return map.get(key);
     *
     * except that the action is performed atomically.
     * 除了动作是原子执行的。
     * （这句话意思是与上面的等价操作非原子性的，而这个方法提供的操作是原子的）
     *
     * @implNote This implementation intentionally re-abstracts the
     * inappropriate default provided in {@code Map}.
     * 该实现试图（有意）重新抽象由Map提供的不适当的默认实现。
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or
     *         {@code null} if there was no mapping for the key.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with the key,
     *         if the implementation supports null values.)
     *         返回原关联的value值（支持value为null的话也可能返回null），没关联的话返回null
     * @throws UnsupportedOperationException if the {@code put} operation
     *         is not supported by this map
     * @throws ClassCastException if the class of the specified key or value
     *         prevents it from being stored in this map
     * @throws NullPointerException if the specified key or value is null,
     *         and this map does not permit null keys or values
     * @throws IllegalArgumentException if some property of the specified key
     *         or value prevents it from being stored in this map
     */
     V putIfAbsent(K key, V value);

    /**
     * Removes the entry for a key only if currently mapped to a given value.
     * This is equivalent to
     *  <pre> {@code
     * if (map.containsKey(key) && Objects.equals(map.get(key), value)) {
     *   map.remove(key);
     *   return true;
     * } else
     *   return false;
     * }</pre>
     * 根据key移除entry，当且仅当当前key映射的value为给定value。
     * 等价于：
     * if (map.containsKey(key) && Objects.equals(map.get(key), value)) { // 用了1.7之后的Objects类
     *   map.remove(key);
     *   return true;
     * } else
     *   return false;
     *
     * except that the action is performed atomically.
     * 除了动作是原子执行的。
     * （这句话意思是与上面的等价操作非原子性的，而这个方法提供的操作是原子的）
     *
     * @implNote This implementation intentionally re-abstracts the
     * inappropriate default provided in {@code Map}.
     * 该实现试图（有意）重新抽象由Map提供的不适当的默认实现。
     *
     * @param key key with which the specified value is associated
     * @param value value expected to be associated with the specified key
     * @return {@code true} if the value was removed
     *          移除成功返回true
     * @throws UnsupportedOperationException if the {@code remove} operation
     *         is not supported by this map
     * @throws ClassCastException if the key or value is of an inappropriate
     *         type for this map
     *         (<a href="../Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified key or value is null,
     *         and this map does not permit null keys or values
     *         (<a href="../Collection.html#optional-restrictions">optional</a>)
     */
    boolean remove(Object key, Object value);

    /**
     * Replaces the entry for a key only if currently mapped to a given value.
     * This is equivalent to
     *  <pre> {@code
     * if (map.containsKey(key) && Objects.equals(map.get(key), oldValue)) {
     *   map.put(key, newValue);
     *   return true;
     * } else
     *   return false;
     * }</pre>
     * 根据key替换entry，当且仅当当前key映射的value值为给定的value值。
     * 等价于：
     * if (map.containsKey(key) && Objects.equals(map.get(key), oldValue)) {
     *   map.put(key, newValue);
     *   return true;
     * } else
     *   return false;
     *
     * except that the action is performed atomically.
     * 除了动作是原子执行的。
     * （这句话意思是与上面的等价操作非原子性的，而这个方法提供的操作是原子的）
     *
     * @implNote This implementation intentionally re-abstracts the
     * inappropriate default provided in {@code Map}.
     * 该实现试图（有意）重新抽象由Map提供的不适当的默认实现。
     *
     * @param key key with which the specified value is associated
     * @param oldValue value expected to be associated with the specified key
     * @param newValue value to be associated with the specified key
     * @return {@code true} if the value was replaced
     *          替换成功返回true
     * @throws UnsupportedOperationException if the {@code put} operation
     *         is not supported by this map
     * @throws ClassCastException if the class of a specified key or value
     *         prevents it from being stored in this map
     * @throws NullPointerException if a specified key or value is null,
     *         and this map does not permit null keys or values
     * @throws IllegalArgumentException if some property of a specified key
     *         or value prevents it from being stored in this map
     */
    boolean replace(K key, V oldValue, V newValue);

    /**
     * Replaces the entry for a key only if currently mapped to some value.
     * This is equivalent to
     *  <pre> {@code
     * if (map.containsKey(key)) {
     *   return map.put(key, value);
     * } else
     *   return null;
     * }</pre>
     * 根据key替换entry，当且仅当当前key映射到某些value（某些是指保证key存在，但不关心key具体映射的value值）。
     * 等价于
     * if (map.containsKey(key)) {
     *   return map.put(key, value);
     * } else
     *   return null;
     *
     * except that the action is performed atomically.
     * 除了动作是原子执行的。
     * （这句话意思是与上面的等价操作非原子性的，而这个方法提供的操作是原子的）
     *
     * @implNote This implementation intentionally re-abstracts the
     * inappropriate default provided in {@code Map}.
     * 该实现试图（有意）重新抽象由Map提供的不适当的默认实现。
     *
     * @param key key with which the specified value is associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or
     *         {@code null} if there was no mapping for the key.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with the key,
     *         if the implementation supports null values.)
     *         返回该key关联的原value值（如果实现支持null，则返回的值可能为null），没有该key原没有映射value，则返回null。
     * @throws UnsupportedOperationException if the {@code put} operation
     *         is not supported by this map
     * @throws ClassCastException if the class of the specified key or value
     *         prevents it from being stored in this map
     * @throws NullPointerException if the specified key or value is null,
     *         and this map does not permit null keys or values
     * @throws IllegalArgumentException if some property of the specified key
     *         or value prevents it from being stored in this map
     */
    V replace(K key, V value);

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * <p>The default implementation is equivalent to, for this {@code map}:
     * <pre> {@code
     * for ((Map.Entry<K, V> entry : map.entrySet())
     *     do {
     *        K k = entry.getKey();
     *        V v = entry.getValue();
     *     } while(!replace(k, v, function.apply(k, v)));
     * }</pre>
     * 对于该map，默认实现等价于：
     * for ((Map.Entry<K, V> entry : map.entrySet())
     *     do {
     *        K k = entry.getKey();
     *        V v = entry.getValue();
     *     } while(!replace(k, v, function.apply(k, v)));
     *
     * The default implementation may retry these steps when multiple
     * threads attempt updates including potentially calling the function
     * repeatedly for a given key.
     * 当多个线程尝试更新时，默认实现可能会多次尝试这些步骤，包括可能（potentially 潜在地）为给定的key重复调用该function。
     *
     * <p>This implementation assumes that the ConcurrentMap cannot contain null
     * values and {@code get()} returning null unambiguously means the key is
     * absent. Implementations which support null values <strong>must</strong>
     * override this default implementation.
     * 该实现假定ConcurrentMap不能包含为null值的value，并且get()返回null明确（unambiguously）表示key不存在（absent）。
     * 支持value为null的实现必须重写（override）该默认实现。
     * 
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @since 1.8
     */
    @Override
    default void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        forEach((k,v) -> {
            while(!replace(k, v, function.apply(k, v))) {
                // v changed or k is gone
                if ( (v = get(k)) == null) {
                    // k is no longer in the map.
                    break;
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     * 通过给定的方法与原value值计算出新value值，并更新
     *
     * @implSpec
     * The default implementation is equivalent to the following steps for this
     * {@code map}, then returning the current value or {@code null} if now
     * absent:
     * 对于该map，默认实现等价于下面的步骤，然后返回当前（原）value，如果当前key不存在，返回null：
     *
     * <pre> {@code
     * if (map.get(key) == null) {
     *     V newValue = mappingFunction.apply(key);
     *     if (newValue != null)
     *         return map.putIfAbsent(key, newValue);
     * }
     * }</pre>
     *
     * The default implementation may retry these steps when multiple
     * threads attempt updates including potentially calling the mapping
     * function multiple times.
     * 当多个线程尝试更新时，默认实现可能会多次尝试这些步骤，包括可能为给定的key重复调用该映射function。
     *
     * <p>This implementation assumes that the ConcurrentMap cannot contain null
     * values and {@code get()} returning null unambiguously means the key is
     * absent. Implementations which support null values <strong>must</strong>
     * override this default implementation.
     * 该实现假定ConcurrentMap不能包含为null值的value，并且get()返回null明确（unambiguously）表示key不存在（absent）。
     * 支持value为null的实现必须重写（override）该默认实现。
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @since 1.8
     */
    @Override
    default V computeIfAbsent(K key,
            Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);
        V v, newValue;
        return ((v = get(key)) == null &&
                (newValue = mappingFunction.apply(key)) != null &&        // 用旧值计算新值
                (v = putIfAbsent(key, newValue)) == null) ? newValue : v; // 如果key不存在，返回计算后value，否则返回原value
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * The default implementation is equivalent to performing the following
     * steps for this {@code map}, then returning the current value or
     * {@code null} if now absent. :
     * 对于该map，默认实现等价于下面的步骤，然后返回当前（原）value，如果当前key不存在，返回null：
     *
     * <pre> {@code
     * if (map.get(key) != null) {
     *     V oldValue = map.get(key);
     *     V newValue = remappingFunction.apply(key, oldValue);
     *     if (newValue != null)
     *         map.replace(key, oldValue, newValue);
     *     else
     *         map.remove(key, oldValue);
     * }
     * }</pre>
     *
     * The default implementation may retry these steps when multiple threads
     * attempt updates including potentially calling the remapping function
     * multiple times.
     * 当多个线程尝试更新时，默认实现可能会多次尝试这些步骤，包括可能为给定的key重复调用该重新映射的function。
     *
     * <p>This implementation assumes that the ConcurrentMap cannot contain null
     * values and {@code get()} returning null unambiguously means the key is
     * absent. Implementations which support null values <strong>must</strong>
     * override this default implementation.
     * 该实现假定ConcurrentMap不能包含为null值的value，并且get()返回null明确（unambiguously）表示key不存在（absent）。
     * 支持value为null的实现必须重写（override）该默认实现。
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @since 1.8
     */
    @Override
    default V computeIfPresent(K key,
            BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        V oldValue;
        while((oldValue = get(key)) != null) {                   // 这里循环是为了当并发时，replace由于竞争导致失败，需要重新进行replace。
            V newValue = remappingFunction.apply(key, oldValue);
            if (newValue != null) {
                if (replace(key, oldValue, newValue))
                    return newValue;
            } else if (remove(key, oldValue))                    // 如果计算的新值为null，则直接移除该key的映射关系
               return null;
        }
        return oldValue;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * The default implementation is equivalent to performing the following
     * steps for this {@code map}, then returning the current value or
     * {@code null} if absent:
     * 对于该map，默认实现等价于下面的步骤，然后返回当前（原）value，如果当前key不存在，返回null：
     *
     * <pre> {@code
     * V oldValue = map.get(key);
     * V newValue = remappingFunction.apply(key, oldValue);
     * if (oldValue != null ) {
     *    if (newValue != null)
     *       map.replace(key, oldValue, newValue);
     *    else
     *       map.remove(key, oldValue);
     * } else {
     *    if (newValue != null)
     *       map.putIfAbsent(key, newValue);
     *    else
     *       return null;
     * }
     * }</pre>
     *
     * The default implementation may retry these steps when multiple
     * threads attempt updates including potentially calling the remapping
     * function multiple times.
     * 当多个线程尝试更新时，默认实现可能会多次尝试这些步骤，包括可能为给定的key重复调用该重新映射的function。
     *
     * <p>This implementation assumes that the ConcurrentMap cannot contain null
     * values and {@code get()} returning null unambiguously means the key is
     * absent. Implementations which support null values <strong>must</strong>
     * override this default implementation.
     * 该实现假定ConcurrentMap不能包含为null值的value，并且get()返回null明确（unambiguously）表示key不存在（absent）。
     * 支持value为null的实现必须重写（override）该默认实现。
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @since 1.8
     */
    @Override
    default V compute(K key,
            BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        V oldValue = get(key);
        for(;;) {
            V newValue = remappingFunction.apply(key, oldValue);
            if (newValue == null) {                                 // 计算新值为null，尝试删除给定key的映射关系
                // delete mapping
                if (oldValue != null || containsKey(key)) {          // 考虑了key映射的value为null的情况
                    // something to remove
                    if (remove(key, oldValue)) {
                        // removed the old value as expected
                        return null;
                    }

                    // some other value replaced old value. try again. // 尝试失败，获取给定key当前value值继续尝试
                    oldValue = get(key);
                } else {
                    // nothing to do. Leave things as they were.
                    return null;
                }
            } else {                                                   // 新值不为null，尝试更新给定key的映射关系
                // add or replace old mapping
                if (oldValue != null) {
                    // replace
                    if (replace(key, oldValue, newValue)) {
                        // replaced as expected.
                        return newValue;
                    }

                    // some other value replaced old value. try again. // 尝试失败，获取给定key当前value值继续尝试
                    oldValue = get(key);
                } else {
                    // add (replace if oldValue was null)
                    if ((oldValue = putIfAbsent(key, newValue)) == null) { // 原值为null的情况下，新增key与计算后value的映射关系
                        // replaced
                        return newValue;
                    }

                    // some other value replaced old value. try again.
                }
            }
        }
    }


    /**
     * {@inheritDoc}
     * merge与compute的区别是，merge依赖给定的value与原value进行计算，compute只基于原value计算
     *
     * @implSpec
     * The default implementation is equivalent to performing the following
     * steps for this {@code map}, then returning the current value or
     * {@code null} if absent:
     * 对于该map，默认实现等价于下面的步骤，然后返回当前（原）value，如果当前key不存在，返回null：
     *
     * <pre> {@code
     * V oldValue = map.get(key);
     * V newValue = (oldValue == null) ? value :
     *              remappingFunction.apply(oldValue, value);
     * if (newValue == null)
     *     map.remove(key);
     * else
     *     map.put(key, newValue);
     * }</pre>
     *
     * <p>The default implementation may retry these steps when multiple
     * threads attempt updates including potentially calling the remapping
     * function multiple times.
     * 当多个线程尝试更新时，默认实现可能会多次尝试这些步骤，包括可能为给定的key重复调用该重新映射的function。
     *
     * <p>This implementation assumes that the ConcurrentMap cannot contain null
     * values and {@code get()} returning null unambiguously means the key is
     * absent. Implementations which support null values <strong>must</strong>
     * override this default implementation.
     * 该实现假定ConcurrentMap不能包含为null值的value，并且get()返回null明确（unambiguously）表示key不存在（absent）。
     * 支持value为null的实现必须重写（override）该默认实现。
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @since 1.8
     */
    @Override
    default V merge(K key, V value,
            BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        Objects.requireNonNull(value);
        V oldValue = get(key);
        for (;;) {
            if (oldValue != null) {
                V newValue = remappingFunction.apply(oldValue, value);
                if (newValue != null) {
                    if (replace(key, oldValue, newValue))
                        return newValue;
                } else if (remove(key, oldValue)) {
                    return null;
                }
                oldValue = get(key);
            } else {
                if ((oldValue = putIfAbsent(key, value)) == null) {
                    return value;
                }
            }
        }
    }
}
```
