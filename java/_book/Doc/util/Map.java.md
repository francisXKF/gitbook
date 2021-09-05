# Map<K,V>

``` java
/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * （使用受条款约束（terms 条款））
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

package java.util;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.io.Serializable;

/**
 * An object that maps keys to values.  A map cannot contain duplicate keys;
 * each key can map to at most one value.
 * 将键映射到值的对象。
 * map不能包含重复的键；每个key至多对应一个value。
 *
 * <p>This interface takes the place of the <tt>Dictionary</tt> class, which
 * was a totally abstract class rather than an interface.
 * 该接口取代了Dictionary类，Dictionary类是一个抽象类而不是接口。
 *
 * <p>The <tt>Map</tt> interface provides three <i>collection views</i>, which
 * allow a map's contents to be viewed as a set of keys, collection of values,
 * or set of key-value mappings.  The <i>order</i> of a map is defined as
 * the order in which the iterators on the map's collection views return their
 * elements.  Some map implementations, like the <tt>TreeMap</tt> class, make
 * specific guarantees as to their order; others, like the <tt>HashMap</tt>
 * class, do not.
 * map接口提供了三种collection视图，允许将map的内容作为一组key，一组value，或者为一组key-value映射。
 * map的顺序定义为在map的集合视图上的迭代器返回的元素的顺序。
 * 一些map的实现，例如TreeMap类，对顺序有特定的保证；其他的，比如HashMap类就没有。
 *
 * <p>Note: great care must be exercised if mutable objects are used as map
 * keys.  The behavior of a map is not specified if the value of an object is
 * changed in a manner that affects <tt>equals</tt> comparisons while the
 * object is a key in the map.  A special case of this prohibition is that it
 * is not permissible for a map to contain itself as a key.  While it is
 * permissible for a map to contain itself as a value, extreme caution is
 * advised: the <tt>equals</tt> and <tt>hashCode</tt> methods are no longer
 * well defined on such a map.
 * 注意：如果用可变的对象作为map的key，必须非常小心。
 * 如果对象作为map中的key，它的值以影响equal比较的方式改变时，则不会指定map的行为。（map不会限制key是否是可变对象、可变对象是否发生变化）
 * 此禁忌（prohibition）的特殊场景为不允许使用该map对象作为该map的key。（只会限制不能把自身作为自身包含元素的key）
 * 虽然允许将map自身作为自己的value值，但建议格外（extreme 极端）小心（caution 慎重）：（可以把自身作为自身包含元素的value）
 * 在此map上不再定义equals和hashCode方法。（map没有特殊的equals与hashCode方法，要比较value相同时，需要考虑这一点）（从代码看是定义了的，这个是啥意思？？？）
 *
 * <p>All general-purpose map implementation classes should provide two
 * "standard" constructors: a void (no arguments) constructor which creates an
 * empty map, and a constructor with a single argument of type <tt>Map</tt>,
 * which creates a new map with the same key-value mappings as its argument.
 * In effect, the latter constructor allows the user to copy any map,
 * producing an equivalent map of the desired class.  There is no way to
 * enforce this recommendation (as interfaces cannot contain constructors) but
 * all of the general-purpose map implementations in the JDK comply.
 * 所有通用目的的map实现类都应该支持两种“标准”构造方法：
 * void（无参数）构造方法，创建空map
 * 带有map类型的单一参数构造方法，创建带有相同key-value映射作为其参数的新map。
 * 实际上，后一个构造方法允许用户拷贝任意map，生成与想要的map等效的map。（将目标map复制成一个新的map）
 * 没有强制要求执行此建议（作为接口类不能包含构造方法），但是所有在JDK中的通用map的实现都符合该建议。
 *
 * <p>The "destructive" methods contained in this interface, that is, the
 * methods that modify the map on which they operate, are specified to throw
 * <tt>UnsupportedOperationException</tt> if this map does not support the
 * operation.  If this is the case, these methods may, but are not required
 * to, throw an <tt>UnsupportedOperationException</tt> if the invocation would
 * have no effect on the map.  For example, invoking the {@link #putAll(Map)}
 * method on an unmodifiable map may, but is not required to, throw the
 * exception if the map whose mappings are to be "superimposed" is empty.
 * 接口中包含“破坏性”方法，即修改map的操作方法，如果该map不支持该操作，则指定抛出UnsupportedOperationException。
 * 如果是下面这种情况：如果调用可能不影响map，该方法可能（但不要求）抛出UnsupportedOperationException。
 * 例如，在一个不可修改的map上调用putAll(map)方法，如果该map要“叠加”的映射为空，可能（但不要求）抛出异常
 * （叠加就是通过putAll方法给map添加新的映射元素）
 *
 * <p>Some map implementations have restrictions on the keys and values they
 * may contain.  For example, some implementations prohibit null keys and
 * values, and some have restrictions on the types of their keys.  Attempting
 * to insert an ineligible key or value throws an unchecked exception,
 * typically <tt>NullPointerException</tt> or <tt>ClassCastException</tt>.
 * Attempting to query the presence of an ineligible key or value may throw an
 * exception, or it may simply return false; some implementations will exhibit
 * the former behavior and some will exhibit the latter.  More generally,
 * attempting an operation on an ineligible key or value whose completion
 * would not result in the insertion of an ineligible element into the map may
 * throw an exception or it may succeed, at the option of the implementation.
 * Such exceptions are marked as "optional" in the specification for this
 * interface.
 * 一些map实现类会对它可能包含的key、value进行限制。
 * 例如，有些实现禁止null key和value，有些实现限制key的类型。
 * 尝试插入不合格（ineligible）key或者value将抛出unchecked异常，通常为NullPointerException或者为ClassCastException。
 * 尝试查询不合格的key或者value可能抛出异常，或者可能简单的返回false；
 * 一些实现可能表现出（exhibit）前一种（former）行为，另一些实现可能表现出后一种行为。
 * 更一般的，尝试操作不合格的key或者value，其完成不会导致不合格元素插入到map中，可能会抛出异常，或者成功，这取决于具体实现。
 * （就是存在这种情况：元素为不合格元素时，插入操作可能会返回成功，但是元素并未插入）
 * 在此接口规范中，该异常被标记为“可选”。
 *
 * <p>Many methods in Collections Framework interfaces are defined
 * in terms of the {@link Object#equals(Object) equals} method.  For
 * example, the specification for the {@link #containsKey(Object)
 * containsKey(Object key)} method says: "returns <tt>true</tt> if and
 * only if this map contains a mapping for a key <tt>k</tt> such that
 * <tt>(key==null ? k==null : key.equals(k))</tt>." This specification should
 * <i>not</i> be construed to imply that invoking <tt>Map.containsKey</tt>
 * with a non-null argument <tt>key</tt> will cause <tt>key.equals(k)</tt> to
 * be invoked for any key <tt>k</tt>.  Implementations are free to
 * implement optimizations whereby the <tt>equals</tt> invocation is avoided,
 * for example, by first comparing the hash codes of the two keys.  (The
 * {@link Object#hashCode()} specification guarantees that two objects with
 * unequal hash codes cannot be equal.)  More generally, implementations of
 * the various Collections Framework interfaces are free to take advantage of
 * the specified behavior of underlying {@link Object} methods wherever the
 * implementor deems it appropriate.
 * 在Collections Framework（集合框架）的许多方法都是根据（in terms of）equals方法来定义的。
 * 例如，containsKey(Object key)方法规范（specification）说：当且仅当该map包含k的映射，使得(key==null ? k==null : key.equals(k))，才返回true。
 * 本规范不应被解释为暗示，如果使用非空key参数调用Map.containsKey方法，将导致为任意k调用key.equals(k)方法
 * 实现类可以自由的实现优化（optimization），由此（whereby）避免调用equals方法，
 * 例如，首先比较两个key的hash code。
 * （Object#hashCode()方法规范保证两个对象如果hash code不同则对象不相等。）
 * 更一般的，各种集合框架接口的实现可以自由的利用底层Object方法的指定行为，只要实现者认为合适在哪里用都可以。
 * （这段是说明有些集合框架的方法，是基于Object提供的方法来实现的，可以自由选择Object的方法来实现功能）
 *
 * <p>Some map operations which perform recursive traversal of the map may fail
 * with an exception for self-referential instances where the map directly or
 * indirectly contains itself. This includes the {@code clone()},
 * {@code equals()}, {@code hashCode()} and {@code toString()} methods.
 * Implementations may optionally handle the self-referential scenario, however
 * most current implementations do not do so.
 * 一些需要执行map的递归遍历的map操作可能会失败：当map直接或间接包含自身的自引用会有异常。（不能对包含将自身作为元素的map进行递归遍历，可能会死循环）
 * 这些操作包括clone()、equals()、hashCode()和toString()方法。
 * 实现可以选择性的处理自引用的场景（scenario 设想），然而当前大多数实现都没有这样做。
 *
 * <p>This interface is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 * 该接口属于Java Collections Framework（java集合框架）。
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 *
 * @author  Josh Bloch
 * @see HashMap
 * @see TreeMap
 * @see Hashtable
 * @see SortedMap
 * @see Collection
 * @see Set
 * @since 1.2
 */
public interface Map<K,V> {
    // Query Operations
    // 查询操作（我还以为所有的方法外注释都是/***/，原来并不是）

    /**
     * Returns the number of key-value mappings in this map.  If the
     * map contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
     * <tt>Integer.MAX_VALUE</tt>.
     * 返回该map中key-value映射的数量。
     * 如果map包含的元素数超过Integer.MAX_VALUE，返回Integer.MAX_VALUE
     *
     * @return the number of key-value mappings in this map
     */
    int size();

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     * 如果该map包含的key-value映射为空，返回true。
     *
     * @return <tt>true</tt> if this map contains no key-value mappings
     */
    boolean isEmpty();

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified
     * key.  More formally, returns <tt>true</tt> if and only if
     * this map contains a mapping for a key <tt>k</tt> such that
     * <tt>(key==null ? k==null : key.equals(k))</tt>.  (There can be
     * at most one such mapping.)
     * 如果该map包含指定key的映射，返回true。
     * 更正式的说法，当且仅当该map包含该k的映射，使得满足(key==null ? k==null : key.equals(k))，则返回true。
     * （最多可以有一个这样的映射）
     * （key唯一，最多只有一个）
     *
     * @param key key whose presence in this map is to be tested
     *        key参数，测试该key是否存在于该map
     * @return <tt>true</tt> if this map contains a mapping for the specified
     *         key
     * @throws ClassCastException if the key is of an inappropriate type for
     *         this map
     *         抛出ClassCastException，如果key的类型与该map不符。
     *
     * (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified key is null and this map
     *         does not permit null keys
     *         抛出NullPointerException，如果指定key是null并且该map不允许非空key
     *
     * (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     */
    boolean containsKey(Object key);

    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the
     * specified value.  More formally, returns <tt>true</tt> if and only if
     * this map contains at least one mapping to a value <tt>v</tt> such that
     * <tt>(value==null ? v==null : value.equals(v))</tt>.  This operation
     * will probably require time linear in the map size for most
     * implementations of the <tt>Map</tt> interface.
     * 返回true，如果该map有一个或多个key映射到指定的value。
     * 更正式的说法，返回true，当且仅当该map包含至少一个到该value v的映射，使得满足(value==null ? v==null : value.equals(v))。
     * 对于大多数该Map接口的实现来说，该操作花费的时间可能与map的大小成线性相关。
     *
     * @param value value whose presence in this map is to be tested
     * @return <tt>true</tt> if this map maps one or more keys to the
     *         specified value
     * @throws ClassCastException if the value is of an inappropriate type for
     *         this map
     * (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified value is null and this
     *         map does not permit null values
     *         抛出NullPointerException，如果指定value是null并且该map不允许非空value
     *
     * (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     */
    boolean containsValue(Object value);

    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     * 如果存在指定key的映射，返回对应的value，
     * 如果不存在指定key的映射，返回null。
     *
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code (key==null ? k==null :
     * key.equals(k))}, then this method returns {@code v}; otherwise
     * it returns {@code null}.  (There can be at most one such mapping.)
     * 更正式的说法，如果该map包含从键k到值v的映射，使得满足key==null ? k==null : key.equals(k))，则该方法返回v；否则返回null。
     * （最多可以有一个这样的映射）
     *
     * <p>If this map permits null values, then a return value of
     * {@code null} does not <i>necessarily</i> indicate that the map
     * contains no mapping for the key; it's also possible that the map
     * explicitly maps the key to {@code null}.  The {@link #containsKey
     * containsKey} operation may be used to distinguish these two cases.
     * 如果该map允许null值，那么返回值为null不再一定暗示该map不包含该key的映射；
     * 也有可能该map有明确的（explicitly）key到null的映射。
     * containsKey操作可以用于区分这两种场景。
     *
     * @param key the key whose associated value is to be returned
     *        key参数，返回该key关联的value
     * @return the value to which the specified key is mapped, or
     *         {@code null} if this map contains no mapping for the key
     * @throws ClassCastException if the key is of an inappropriate type for
     *         this map
     * (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified key is null and this map
     *         does not permit null keys
     * (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     */
    V get(Object key);

    // Modification Operations
    // 修改操作
    // 对于标注为可选操作的，都有可能由于不支持抛出UnsupportedOperationException

    /**
     * Associates the specified value with the specified key in this map
     * (optional operation).  If the map previously contained a mapping for
     * the key, the old value is replaced by the specified value.  (A map
     * <tt>m</tt> is said to contain a mapping for a key <tt>k</tt> if and only
     * if {@link #containsKey(Object) m.containsKey(k)} would return
     * <tt>true</tt>.)
     * 在该map中将指定的key与指定的value进行关联（可选操作）。
     * 如果map中之前已经包含该key的映射，那么使用指定的值替换旧值。
     * （map m包含键k映射，当且仅当m.containsKey(k)返回true）
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     *         (A <tt>null</tt> return can also indicate that the map
     *         previously associated <tt>null</tt> with <tt>key</tt>,
     *         if the implementation supports <tt>null</tt> values.)
     *         返回值为该key原关联的值，如果该key原来没关联值，返回null。
     *         （如果实现类支持value为null，那么返回的null也可以表示该key原来的关联值为null）
     *
     * @throws UnsupportedOperationException if the <tt>put</tt> operation
     *         is not supported by this map
     * @throws ClassCastException if the class of the specified key or value
     *         prevents it from being stored in this map
     * @throws NullPointerException if the specified key or value is null
     *         and this map does not permit null keys or values
     * @throws IllegalArgumentException if some property of the specified key
     *         or value prevents it from being stored in this map
     */
    V put(K key, V value);

    /**
     * Removes the mapping for a key from this map if it is present
     * (optional operation).   More formally, if this map contains a mapping
     * from key <tt>k</tt> to value <tt>v</tt> such that
     * <code>(key==null ?  k==null : key.equals(k))</code>, that mapping
     * is removed.  (The map can contain at most one such mapping.)
     * 如果key在该map中存在，从该map中移除该key映射（可选操作）
     * 更正式的说法，如果该map存在该key到value的映射，使得满足(key==null ?  k==null : key.equals(k))，那么移除该映射。
     * （该map至多存在一个这样的映射）
     *
     * <p>Returns the value to which this map previously associated the key,
     * or <tt>null</tt> if the map contained no mapping for the key.
     * 返回原关联到该key上的value值，如果该map没有包含该key的映射，返回null。
     *
     * <p>If this map permits null values, then a return value of
     * <tt>null</tt> does not <i>necessarily</i> indicate that the map
     * contained no mapping for the key; it's also possible that the map
     * explicitly mapped the key to <tt>null</tt>.
     * 如果该map允许值value为null，那么返回的值是null不再一定表示该map没有包含该key的映射；也有可能map有明确的（explicitly）key到null的映射。
     *
     * <p>The map will not contain a mapping for the specified key once the
     * call returns.
     * 一旦调用返回，映射将不包含（没有再，因为以前也可能没有）指定key的映射。
     *
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     * @throws UnsupportedOperationException if the <tt>remove</tt> operation
     *         is not supported by this map
     * @throws ClassCastException if the key is of an inappropriate type for
     *         this map
     * (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified key is null and this
     *         map does not permit null keys
     * (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     */
    V remove(Object key);


    // Bulk Operations
    // 批量操作

    /**
     * Copies all of the mappings from the specified map to this map
     * (optional operation).  The effect of this call is equivalent to that
     * of calling {@link #put(Object,Object) put(k, v)} on this map once
     * for each mapping from key <tt>k</tt> to value <tt>v</tt> in the
     * specified map.  The behavior of this operation is undefined if the
     * specified map is modified while the operation is in progress.
     * 将指定map的所有映射都拷贝到该map（可选操作）。
     * 该方法与对指定map的每个key到value映射做一次到该map的put(k, v)操作效果是相同的。
     * 如果在执行该操作的过程中指定map被修改了，该执行何种行为是没有定义的。（如果指定map变了，该方法要做啥操作没有特别定义）
     *
     * @param m mappings to be stored in this map
     * @throws UnsupportedOperationException if the <tt>putAll</tt> operation
     *         is not supported by this map
     * @throws ClassCastException if the class of a key or value in the
     *         specified map prevents it from being stored in this map
     * @throws NullPointerException if the specified map is null, or if
     *         this map does not permit null keys or values, and the
     *         specified map contains null keys or values
     * @throws IllegalArgumentException if some property of a key or value in
     *         the specified map prevents it from being stored in this map
     */
    void putAll(Map<? extends K, ? extends V> m);

    /**
     * Removes all of the mappings from this map (optional operation).
     * The map will be empty after this call returns.
     * 从该map中移除所有的映射（可选操作）。
     * 在执行该方法后，该map将为空。
     *
     * @throws UnsupportedOperationException if the <tt>clear</tt> operation
     *         is not supported by this map
     */
    void clear();


    // Views
    // 视图

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own <tt>remove</tt> operation), the results of
     * the iteration are undefined.  The set supports element removal,
     * which removes the corresponding mapping from the map, via the
     * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt>
     * operations.  It does not support the <tt>add</tt> or <tt>addAll</tt>
     * operations.
     * 返回该map包含的key的集合（set）视图。
     * 该集合由map支持，所以对map的修改会反映在集合上，反之亦然。
     * 如果在迭代器遍历集合的过程中该map被修改了（除了迭代器自己的remove操作修改map），迭代的结果是未明确定义的。
     * 该集合支持元素移除，即从map中移除相应的映射，元素移除可以通过下面几个方法：
     * Iterator.remove、Set.remove、removeAll、retainAll、clear
     * 不支持add与addAll操作。
     *
     * @return a set view of the keys contained in this map
     */
    Set<K> keySet();

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     * The collection is backed by the map, so changes to the map are
     * reflected in the collection, and vice-versa.  If the map is
     * modified while an iteration over the collection is in progress
     * (except through the iterator's own <tt>remove</tt> operation),
     * the results of the iteration are undefined.  The collection
     * supports element removal, which removes the corresponding
     * mapping from the map, via the <tt>Iterator.remove</tt>,
     * <tt>Collection.remove</tt>, <tt>removeAll</tt>,
     * <tt>retainAll</tt> and <tt>clear</tt> operations.  It does not
     * support the <tt>add</tt> or <tt>addAll</tt> operations.
     * 返回该map包含的value集合视图。
     * 该集合由map支持，所以对map的修改会反映在集合上，反之亦然。
     * 如果在迭代器遍历集合的过程中该map被修改了（除了迭代器自己的remove操作修改map），迭代的结果是未明确定义的。
     * 该集合支持元素移除，即从map中移除相应的（corresponding）映射，元素移除可以通过下面几个方法：
     * Iterator.remove、Set.remove、removeAll、retainAll、clear
     * 不支持add与addAll操作。
     *
     * @return a collection view of the values contained in this map
     */
    Collection<V> values();

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own <tt>remove</tt> operation, or through the
     * <tt>setValue</tt> operation on a map entry returned by the
     * iterator) the results of the iteration are undefined.  The set
     * supports element removal, which removes the corresponding
     * mapping from the map, via the <tt>Iterator.remove</tt>,
     * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt> and
     * <tt>clear</tt> operations.  It does not support the
     * <tt>add</tt> or <tt>addAll</tt> operations.
     * 返回该map包含的映射Set视图。
     * 该set集合由map支持，所以对map的修改会反映在集合上，反之亦然。
     * 如果在迭代器遍历set的过程中该map被修改了
     * （除了迭代器自己的remove操作，或者在迭代器返回的map entry上进行setValue操作修改map），
     * 迭代的结果是未明确定义的。
     * 该set支持元素移除，即从map中移除相应的（corresponding）映射，元素移除可以通过下面几个方法：
     * Iterator.remove、Set.remove、removeAll、retainAll和clear。
     * 不支持add与addAll操作。
     *
     * @return a set view of the mappings contained in this map
     */
    Set<Map.Entry<K, V>> entrySet();

    /**
     * A map entry (key-value pair).  The <tt>Map.entrySet</tt> method returns
     * a collection-view of the map, whose elements are of this class.  The
     * <i>only</i> way to obtain a reference to a map entry is from the
     * iterator of this collection-view.  These <tt>Map.Entry</tt> objects are
     * valid <i>only</i> for the duration of the iteration; more formally,
     * the behavior of a map entry is undefined if the backing map has been
     * modified after the entry was returned by the iterator, except through
     * the <tt>setValue</tt> operation on the map entry.
     * map的entry（实体？？？）（key-value对）。
     * Map.entrySet方法返回该map的集合视图，集合的元素类型为该类。
     * 获取map entry的唯一方法是通过刚才集合视图的迭代器。
     * Map.Entry对象仅在迭代期间有效；
     * 更正式的说法，如果在迭代器返回entry之后，来源的map被修改了，则map entry的行为是未定义的，除非通过setValue操作map entry。
     *
     * @see Map#entrySet()
     * @since 1.2
     */
    interface Entry<K,V> {
        /**
         * Returns the key corresponding to this entry.
         * 返回该entry对应的key
         *
         * @return the key corresponding to this entry
         * @throws IllegalStateException implementations may, but are not
         *         required to, throw this exception if the entry has been
         *         removed from the backing map.
         */
        K getKey();

        /**
         * Returns the value corresponding to this entry.  If the mapping
         * has been removed from the backing map (by the iterator's
         * <tt>remove</tt> operation), the results of this call are undefined.
         * 返回该entry对应的value。
         * 如果该映射关系从来源map移除（通过迭代器的remove操作），该调用结果是未定义。
         *
         * @return the value corresponding to this entry
         * @throws IllegalStateException implementations may, but are not
         *         required to, throw this exception if the entry has been
         *         removed from the backing map.
         */
        V getValue();

        /**
         * Replaces the value corresponding to this entry with the specified
         * value (optional operation).  (Writes through to the map.)  The
         * behavior of this call is undefined if the mapping has already been
         * removed from the map (by the iterator's <tt>remove</tt> operation).
         * 使用给定的value替换该entry对应的value（可选操作）。（也会写入到map）
         * 如果该映射已经从map中移除了（通过迭代器的remove操作），该调用行为未定义。
         *
         * @param value new value to be stored in this entry
         * @return old value corresponding to the entry
         *         返回值该entry对应的旧值
         * @throws UnsupportedOperationException if the <tt>put</tt> operation
         *         is not supported by the backing map
         * @throws ClassCastException if the class of the specified value
         *         prevents it from being stored in the backing map
         * @throws NullPointerException if the backing map does not permit
         *         null values, and the specified value is null
         * @throws IllegalArgumentException if some property of this value
         *         prevents it from being stored in the backing map
         * @throws IllegalStateException implementations may, but are not
         *         required to, throw this exception if the entry has been
         *         removed from the backing map.
         */
        V setValue(V value);

        /**
         * Compares the specified object with this entry for equality.
         * Returns <tt>true</tt> if the given object is also a map entry and
         * the two entries represent the same mapping.  More formally, two
         * entries <tt>e1</tt> and <tt>e2</tt> represent the same mapping
         * if<pre>
         *     (e1.getKey()==null ?
         *      e2.getKey()==null : e1.getKey().equals(e2.getKey()))  &amp;&amp;
         *     (e1.getValue()==null ?
         *      e2.getValue()==null : e1.getValue().equals(e2.getValue()))
         * </pre>
         * This ensures that the <tt>equals</tt> method works properly across
         * different implementations of the <tt>Map.Entry</tt> interface.
         * 比较给定的对象与该entry是否相等。
         * 返回true，如果给定的对象也是一个map的entry，并且这两个entry代表相同的映射。
         * 更正式的说法，两个entry：e1与e2代表相同映射的定义为：
         *     (e1.getKey()==null ?
         *      e2.getKey()==null : e1.getKey().equals(e2.getKey())) &&
         *     (e1.getValue()==null ?
         *      e2.getValue()==null : e1.getValue().equals(e2.getValue()))
         * （比较分为两部分，要保证两个entry的key与value都相等）
         * 这确保了equals方法能够在不同的Map.Entry接口实现中正常工作。
         *
         * @param o object to be compared for equality with this map entry
         * @return <tt>true</tt> if the specified object is equal to this map
         *         entry
         */
        boolean equals(Object o);

        /**
         * Returns the hash code value for this map entry.  The hash code
         * of a map entry <tt>e</tt> is defined to be: <pre>
         *     (e.getKey()==null   ? 0 : e.getKey().hashCode()) ^
         *     (e.getValue()==null ? 0 : e.getValue().hashCode())
         * </pre>
         * This ensures that <tt>e1.equals(e2)</tt> implies that
         * <tt>e1.hashCode()==e2.hashCode()</tt> for any two Entries
         * <tt>e1</tt> and <tt>e2</tt>, as required by the general
         * contract of <tt>Object.hashCode</tt>.
         * 返回该map entry的hash code值。
         * map entry--e的hash code定义如下：
         *     (e.getKey()==null   ? 0 : e.getKey().hashCode()) ^
         *     (e.getValue()==null ? 0 : e.getValue().hashCode())
         * （用key的hashCode与value的hashCode做异或操作，作为Entry的hashCode） 
         * 保证了对于任意两个Entry：e1与e2，e1.equals(e2)成立意味着e1.hashCode()==e2.hashCode()，
         * 这也符合Object.hashCode基本约定要求。（即equals相等hashCode必须相等）
         *
         * @return the hash code value for this map entry
         * @see Object#hashCode()
         * @see Object#equals(Object)
         * @see #equals(Object)
         */
        int hashCode();

        /**
         * Returns a comparator that compares {@link Map.Entry} in natural order on key.
         * 返回一个比较器，在key上按自然顺序比较Map.Entry
         *
         * <p>The returned comparator is serializable and throws {@link
         * NullPointerException} when comparing an entry with a null key.
         * 返回的comparator是可序列化的，当entry与为null的key进行比较时，抛出NullPointerException
         *
         * @param  <K> the {@link Comparable} type of then map keys
         * @param  <V> the type of the map values
         * @return a comparator that compares {@link Map.Entry} in natural order on key.
         * @see Comparable
         * @since 1.8
         */
        public static <K extends Comparable<? super K>, V> Comparator<Map.Entry<K,V>> comparingByKey() {
            // 对于泛型方法的定义：public/private等 static 方法域泛型 返回值 方法名(入参)
            // <K extends Comparable<? super K>, V> 这一块是方法域泛型，用于限定传入参数的类型，当然，传入参数没泛型这块有没有都没问题
            return (Comparator<Map.Entry<K, V>> & Serializable)
                (c1, c2) -> c1.getKey().compareTo(c2.getKey()); // 通过Comparable的compareTo方法比较
            // 返回值是个Comparator<T>的匿名类实例，涉及到函数式接口，(c1, c2) -> c1.getKey().compareTo(c2.getKey())是对接口类唯一的抽象方法的实现
        }

        /**
         * Returns a comparator that compares {@link Map.Entry} in natural order on value.
         * 返回一个比较器，在value上按自然顺序比较Map.Entry
         *
         * <p>The returned comparator is serializable and throws {@link
         * NullPointerException} when comparing an entry with null values.
         * 返回的comparator是可序列化的，当entry与为null的value进行比较时，抛出NullPointerException
         *
         * @param <K> the type of the map keys
         * @param <V> the {@link Comparable} type of the map values
         * @return a comparator that compares {@link Map.Entry} in natural order on value.
         * @see Comparable
         * @since 1.8
         */
        public static <K, V extends Comparable<? super V>> Comparator<Map.Entry<K,V>> comparingByValue() {
            return (Comparator<Map.Entry<K, V>> & Serializable)
                (c1, c2) -> c1.getValue().compareTo(c2.getValue());
        }

        /**
         * Returns a comparator that compares {@link Map.Entry} by key using the given
         * {@link Comparator}.
         * 返回一个比较器，在key上按给定的Comparator比较Map.Entry
         *
         * <p>The returned comparator is serializable if the specified comparator
         * is also serializable.
         * 如果给定的comparator是可序列化的，返回的comparator也是可序列化的
         *
         * @param  <K> the type of the map keys
         * @param  <V> the type of the map values
         * @param  cmp the key {@link Comparator}
         * @return a comparator that compares {@link Map.Entry} by the key.
         * @since 1.8
         */
        public static <K, V> Comparator<Map.Entry<K, V>> comparingByKey(Comparator<? super K> cmp) {
            Objects.requireNonNull(cmp);                            // JDK1.7 新增的Objects，判断cmp如果为null则抛出异常。
            return (Comparator<Map.Entry<K, V>> & Serializable)
                (c1, c2) -> cmp.compare(c1.getKey(), c2.getKey());  // 通过Comparator的compare比较
        }

        /**
         * Returns a comparator that compares {@link Map.Entry} by value using the given
         * {@link Comparator}.
         * 返回一个比较器，在value上按给定的Comparator比较Map.Entry
         *
         * <p>The returned comparator is serializable if the specified comparator
         * is also serializable.
         * 如果给定的comparator是可序列化的，返回的comparator也是可序列化的
         *
         * @param  <K> the type of the map keys
         * @param  <V> the type of the map values
         * @param  cmp the value {@link Comparator}
         * @return a comparator that compares {@link Map.Entry} by the value.
         * @since 1.8
         */
        public static <K, V> Comparator<Map.Entry<K, V>> comparingByValue(Comparator<? super V> cmp) {
            Objects.requireNonNull(cmp);
            return (Comparator<Map.Entry<K, V>> & Serializable)
                (c1, c2) -> cmp.compare(c1.getValue(), c2.getValue());
        }
    }

    // Comparison and hashing
    // 比较和hash

    /**
     * Compares the specified object with this map for equality.  Returns
     * <tt>true</tt> if the given object is also a map and the two maps
     * represent the same mappings.  More formally, two maps <tt>m1</tt> and
     * <tt>m2</tt> represent the same mappings if
     * <tt>m1.entrySet().equals(m2.entrySet())</tt>.  This ensures that the
     * <tt>equals</tt> method works properly across different implementations
     * of the <tt>Map</tt> interface.
     * 比较给定的对象与该map是否相等。
     * 如果给定的对象是map并且这两个map表示相同的映射，返回true。
     * 更正式的说法，两个map：m1与m2代表相同的映射，需要满足以下条件：
     * m1.entrySet().equals（m2.entrySet())。
     * 这确保了equals方法能够在不同的Map接口实现类中正常工作。
     *
     * @param o object to be compared for equality with this map
     * @return <tt>true</tt> if the specified object is equal to this map
     */
    boolean equals(Object o);

    /**
     * Returns the hash code value for this map.  The hash code of a map is
     * defined to be the sum of the hash codes of each entry in the map's
     * <tt>entrySet()</tt> view.  This ensures that <tt>m1.equals(m2)</tt>
     * implies that <tt>m1.hashCode()==m2.hashCode()</tt> for any two maps
     * <tt>m1</tt> and <tt>m2</tt>, as required by the general contract of
     * {@link Object#hashCode}.
     * 返回该map的hash code。
     * map的hash code是通过该map下的entrySet()视图中每个entry的hash code求和得到的。
     * 这确保了对于任意的m1.equals(m2)意味着m1.hashCode()==m2.hashCode()，正如hashCode的约定所要求的那样
     *
     * @return the hash code value for this map
     * @see Map.Entry#hashCode()
     * @see Object#equals(Object)
     * @see #equals(Object)
     */
    int hashCode();

    // Defaultable methods
    // 可默认方法

    /**
     * Returns the value to which the specified key is mapped, or
     * {@code defaultValue} if this map contains no mapping for the key.
     * 返回给定key映射的value值，如果map内没包含该key的映射，返回defaultValue
     *
     * @implSpec
     * 默认实现行为规范
     * The default implementation makes no guarantees about synchronization
     * or atomicity properties of this method. Any implementation providing
     * atomicity guarantees must override this method and document its
     * concurrency properties.
     * 该方法默认实现不保证同步或者原子性属性。
     * 任何提供原子性保证的实现必须重写（覆盖）此方法并记录下它的并发属性。
     *
     * @param key the key whose associated value is to be returned
     * @param defaultValue the default mapping of the key
     * @return the value to which the specified key is mapped, or
     * {@code defaultValue} if this map contains no mapping for the key
     * @throws ClassCastException if the key is of an inappropriate type for
     * this map
     * (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified key is null and this map
     * does not permit null keys
     * (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @since 1.8
     */
    default V getOrDefault(Object key, V defaultValue) {
        V v;
        return (((v = get(key)) != null) || containsKey(key)) // 先get，如果能拿到就返回，如果为null需要再判断containsKey
            ? v
            : defaultValue;
    }

    /**
     * Performs the given action for each entry in this map until all entries
     * have been processed or the action throws an exception.   Unless
     * otherwise specified by the implementing class, actions are performed in
     * the order of entry set iteration (if an iteration order is specified.)
     * Exceptions thrown by the action are relayed to the caller.
     * 对在该map内的每一个entry执行给定的操作，直到所有的entry被处理或者操作抛出异常。
     * 除非实现类另有规定（如果指定了迭代顺序），否则将按照entry集合迭代的顺序执行操作。
     * 操作抛出的异常将转发给调用者。
     *
     * @implSpec
     * The default implementation is equivalent to, for this {@code map}:
     * <pre> {@code
     * for (Map.Entry<K, V> entry : map.entrySet())
     *     action.accept(entry.getKey(), entry.getValue());
     * }</pre>
     * 该方法的默认实现与上面的map操作相等
     *
     * The default implementation makes no guarantees about synchronization
     * or atomicity properties of this method. Any implementation providing
     * atomicity guarantees must override this method and document its
     * concurrency properties.
     * 该方法默认实现不保证同步或者原子性属性。
     * 任何提供原子性保证的实现必须重写（覆盖）此方法并记录下它的并发属性。
     *
     * @param action The action to be performed for each entry
     * @throws NullPointerException if the specified action is null
     * @throws ConcurrentModificationException if an entry is found to be
     * removed during iteration
     * @since 1.8
     */
    default void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        for (Map.Entry<K, V> entry : entrySet()) {
            K k;
            V v;
            try {
                k = entry.getKey();
                v = entry.getValue();
            } catch(IllegalStateException ise) {                        // 通过entry来getKey()、getValue()，会因为依赖的map修改导致获取异常
                // this usually means the entry is no longer in the map.
                throw new ConcurrentModificationException(ise);
            }
            action.accept(k, v);
        }
    }

    /**
     * Replaces each entry's value with the result of invoking the given
     * function on that entry until all entries have been processed or the
     * function throws an exception.  Exceptions thrown by the function are
     * relayed to the caller.
     * 通过调用给定的方法替换每个entry的value值，直到所有entry被处理完或者方法抛出异常。
     * 方法抛出的异常被转发给调用者。
     *
     * @implSpec
     * <p>The default implementation is equivalent to, for this {@code map}:
     * <pre> {@code
     * for (Map.Entry<K, V> entry : map.entrySet())
     *     entry.setValue(function.apply(entry.getKey(), entry.getValue()));
     * }</pre>
     *
     * <p>The default implementation makes no guarantees about synchronization
     * or atomicity properties of this method. Any implementation providing
     * atomicity guarantees must override this method and document its
     * concurrency properties.
     * 该方法默认实现不保证同步或者原子性属性。
     * 任何提供原子性保证的实现必须重写（覆盖）此方法并记录下它的并发属性。
     *
     * @param function the function to apply to each entry
     * @throws UnsupportedOperationException if the {@code set} operation
     * is not supported by this map's entry set iterator.
     * @throws ClassCastException if the class of a replacement value
     * prevents it from being stored in this map
     * @throws NullPointerException if the specified function is null, or the
     * specified replacement value is null, and this map does not permit null
     * values
     * @throws ClassCastException if a replacement value is of an inappropriate
     *         type for this map
     *         (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if function or a replacement value is null,
     *         and this map does not permit null keys or values
     *         (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws IllegalArgumentException if some property of a replacement value
     *         prevents it from being stored in this map
     *         (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws ConcurrentModificationException if an entry is found to be
     * removed during iteration
     * @since 1.8
     */
    default void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        for (Map.Entry<K, V> entry : entrySet()) {
            K k;
            V v;
            try {
                k = entry.getKey();
                v = entry.getValue();
            } catch(IllegalStateException ise) {                       // 好像所有都是异常的缩写
                // this usually means the entry is no longer in the map.
                throw new ConcurrentModificationException(ise);        // 抛出entry指向的对象被删除导致的并发修改异常
            }

            // ise thrown from function is not a cme.
            v = function.apply(k, v);

            try {
                entry.setValue(v);
            } catch(IllegalStateException ise) {
                // this usually means the entry is no longer in the map.
                throw new ConcurrentModificationException(ise);
            }
        }
    }

    /**
     * If the specified key is not already associated with a value (or is mapped
     * to {@code null}) associates it with the given value and returns
     * {@code null}, else returns the current value.
     * 如果指定的key没有关联到value（或者映射为null），那么将该key与指定的value关联，并返回null。
     * 否则返回该key当前关联的value值
     *
     * @implSpec
     * The default implementation is equivalent to, for this {@code
     * map}:
     *
     * <pre> {@code
     * V v = map.get(key);
     * if (v == null)
     *     v = map.put(key, value);
     *
     * return v;
     * }</pre>
     * 该方法与上面这段代码等价
     *
     * <p>The default implementation makes no guarantees about synchronization
     * or atomicity properties of this method. Any implementation providing
     * atomicity guarantees must override this method and document its
     * concurrency properties.
     * 该方法默认实现不保证同步或者原子性属性。
     * 任何提供原子性保证的实现必须重写（覆盖）此方法并记录下它的并发属性。
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or
     *         {@code null} if there was no mapping for the key.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with the key,
     *         if the implementation supports null values.)
     *         （对于支持value为null的map，返回null有两种含义：不存在该key的映射，或者原映射value为null）
     * @throws UnsupportedOperationException if the {@code put} operation
     *         is not supported by this map
     *         (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws ClassCastException if the key or value is of an inappropriate
     *         type for this map
     *         (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified key or value is null,
     *         and this map does not permit null keys or values
     *         (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws IllegalArgumentException if some property of the specified key
     *         or value prevents it from being stored in this map
     *         (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @since 1.8
     */
    default V putIfAbsent(K key, V value) { // putIfAbsent 设置如果不存在
        V v = get(key);
        if (v == null) {
            v = put(key, value);
        }

        return v;
    }

    /**
     * Removes the entry for the specified key only if it is currently
     * mapped to the specified value.
     * 根据指定的key移除entry，当且仅当该key当前映射值为给定的value值。
     *
     * @implSpec
     * The default implementation is equivalent to, for this {@code map}:
     *
     * <pre> {@code
     * if (map.containsKey(key) && Objects.equals(map.get(key), value)) {
     *     map.remove(key);
     *     return true;
     * } else
     *     return false;
     * }</pre>
     *
     * <p>The default implementation makes no guarantees about synchronization
     * or atomicity properties of this method. Any implementation providing
     * atomicity guarantees must override this method and document its
     * concurrency properties.
     * 该方法默认实现不保证同步或者原子性属性。
     * 任何提供原子性保证的实现必须重写（覆盖）此方法并记录下它的并发属性。
     *
     * @param key key with which the specified value is associated
     * @param value value expected to be associated with the specified key
     * @return {@code true} if the value was removed
     * @throws UnsupportedOperationException if the {@code remove} operation
     *         is not supported by this map
     *         (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws ClassCastException if the key or value is of an inappropriate
     *         type for this map
     *         (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified key or value is null,
     *         and this map does not permit null keys or values
     *         (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @since 1.8
     */
    default boolean remove(Object key, Object value) {
        Object curValue = get(key);                     // 根据key找到当前value值（注意get方法对key为null的操作），如果get不到会返回null
        if (!Objects.equals(curValue, value) ||         // 使用Objects封装的equals方法比较两个对象是否相等（null安全）：(a == b) || (a != null && a.equals(b))
            (curValue == null && !containsKey(key))) {
            return false;                              // 如果指定key当前值与给定值不相等，或者当前值为null并且不包含指定key，则返回false。（这里判断curValue == null是为了效率？？？）
        }
        remove(key);                                    // 执行Map的抽象方法
        return true;
    }

    /**
     * Replaces the entry for the specified key only if currently
     * mapped to the specified value.
     * 根据指定的key替换entry，当且仅当该key当前映射值为给定的value值。
     *
     * @implSpec
     * The default implementation is equivalent to, for this {@code map}:
     *
     * <pre> {@code
     * if (map.containsKey(key) && Objects.equals(map.get(key), value)) {
     *     map.put(key, newValue);
     *     return true;
     * } else
     *     return false;
     * }</pre>
     *
     * The default implementation does not throw NullPointerException
     * for maps that do not support null values if oldValue is null unless
     * newValue is also null.
     * 如果对于不支持value为null的map的oldValue为null，则该方法默认实现不会抛出NullPointerException，除非newValue也是null。
     * （好奇oldValue是如何为null的，难道Map的实现允许动态调整是否支持value为null？？？）
     *
     * <p>The default implementation makes no guarantees about synchronization
     * or atomicity properties of this method. Any implementation providing
     * atomicity guarantees must override this method and document its
     * concurrency properties.
     * 该方法默认实现不保证同步或者原子性属性。
     * 任何提供原子性保证的实现必须重写（覆盖）此方法并记录下它的并发属性。
     *
     * @param key key with which the specified value is associated
     * @param oldValue value expected to be associated with the specified key
     * @param newValue value to be associated with the specified key
     * @return {@code true} if the value was replaced
     * @throws UnsupportedOperationException if the {@code put} operation
     *         is not supported by this map
     *         (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws ClassCastException if the class of a specified key or value
     *         prevents it from being stored in this map
     * @throws NullPointerException if a specified key or newValue is null,
     *         and this map does not permit null keys or values
     * @throws NullPointerException if oldValue is null and this map does not
     *         permit null values
     *         (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws IllegalArgumentException if some property of a specified key
     *         or value prevents it from being stored in this map
     * @since 1.8
     */
    default boolean replace(K key, V oldValue, V newValue) {
        Object curValue = get(key);
        if (!Objects.equals(curValue, oldValue) ||
            (curValue == null && !containsKey(key))) {
            return false;                                 // 如果指定key当前值与给定值不相等，或者当前值为null并且不包含指定key，则返回false。（这里判断curValue == null是为了效率？？？）
        }
        put(key, newValue);                                // 执行Map的抽象方法
        return true;
    }

    /**
     * Replaces the entry for the specified key only if it is
     * currently mapped to some value.
     * 根据指定的key替换entry，当且仅当该key当前映射值为某个值。
     * （当且仅当map中存在该key的映射，不管key映射的值是多少） 
     *
     * @implSpec
     * The default implementation is equivalent to, for this {@code map}:
     *
     * <pre> {@code
     * if (map.containsKey(key)) {
     *     return map.put(key, value);
     * } else
     *     return null;
     * }</pre>
     *
     * <p>The default implementation makes no guarantees about synchronization
     * or atomicity properties of this method. Any implementation providing
     * atomicity guarantees must override this method and document its
     * concurrency properties.
     * 该方法默认实现不保证同步或者原子性属性。
     * 任何提供原子性保证的实现必须重写（覆盖）此方法并记录下它的并发属性。
     *
     * @param key key with which the specified value is associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or
     *         {@code null} if there was no mapping for the key.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with the key,
     *         if the implementation supports null values.)
     * @throws UnsupportedOperationException if the {@code put} operation
     *         is not supported by this map
     *         (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws ClassCastException if the class of the specified key or value
     *         prevents it from being stored in this map
     *         (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified key or value is null,
     *         and this map does not permit null keys or values
     * @throws IllegalArgumentException if some property of the specified key
     *         or value prevents it from being stored in this map
     * @since 1.8
     */
    default V replace(K key, V value) {
        V curValue;
        if (((curValue = get(key)) != null) || containsKey(key)) {     // 这里先拿curValue来判断，是因为curValue是必拿的，因为返回值为curValue
            curValue = put(key, value);
        }
        return curValue;
    }

    /**
     * If the specified key is not already associated with a value (or is mapped
     * to {@code null}), attempts to compute its value using the given mapping
     * function and enters it into this map unless {@code null}.
     * 如果指定的key没有关联到value（或者映射为null），尝试使用给定的映射方法计算value值，若计算值不为null，则设置为该key映射的value值。
     *
     * <p>If the function returns {@code null} no mapping is recorded. If
     * the function itself throws an (unchecked) exception, the
     * exception is rethrown, and no mapping is recorded.  The most
     * common usage is to construct a new object serving as an initial
     * mapped value or memoized result, as in:
     * 如果方法返回null，那么不做任何映射记录（不会把新值映射给key）。
     * 如果方法自身抛出（未检查）异常，则在此方法中重新抛出该异常，并且不做任何映射记录。
     * 大多数用法是构建一个新对象作为初始化映射值或者记忆结果，像下面这样：
     *
     * <pre> {@code
     * map.computeIfAbsent(key, k -> new Value(f(k)));
     * }</pre>
     *
     * <p>Or to implement a multi-value map, {@code Map<K,Collection<V>>},
     * supporting multiple values per key:
     * 或者实现一个多value的map，Map<K,Collection<V>>支持单个key映射多value
     *
     * <pre> {@code
     * map.computeIfAbsent(key, k -> new HashSet<V>()).add(v);
     * }</pre>
     *
     *
     * @implSpec
     * The default implementation is equivalent to the following steps for this
     * {@code map}, then returning the current value or {@code null} if now
     * absent:
     *
     * <pre> {@code
     * if (map.get(key) == null) {
     *     V newValue = mappingFunction.apply(key);
     *     if (newValue != null)
     *         map.put(key, newValue);
     * }
     * }</pre>
     * 默认实现如上，返回结果为**当前value**（当前value可能是非null的旧值value，也可能是计算后非null的value），如果当前映射不存在返回null（function计算为null）。
     *
     * <p>The default implementation makes no guarantees about synchronization
     * or atomicity properties of this method. Any implementation providing
     * atomicity guarantees must override this method and document its
     * concurrency properties. In particular, all implementations of
     * subinterface {@link java.util.concurrent.ConcurrentMap} must document
     * whether the function is applied once atomically only if the value is not
     * present.
     * 该方法默认实现不保证同步或者原子性属性。
     * 任何提供原子性保证的实现必须重写（覆盖）此方法并记录下它的并发属性。
     * 特别是（in particular），子接口java.util.concurrent.ConcurrentMap的所有实现，必须记录该方法是否仅在value不存在时支持以原子性操作执行一次。
     *
     * @param key key with which the specified value is to be associated
     * @param mappingFunction the function to compute a value
     * @return the current (existing or computed) value associated with
     *         the specified key, or null if the computed value is null
     *         返回当前value值（当前value值有两种情况：1、key对应的原value不为null，2、function计算出的不为null的新value）
     *         如果计算结果为null，返回null。
     * @throws NullPointerException if the specified key is null and
     *         this map does not support null keys, or the mappingFunction
     *         is null
     * @throws UnsupportedOperationException if the {@code put} operation
     *         is not supported by this map
     *         (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws ClassCastException if the class of the specified key or value
     *         prevents it from being stored in this map
     *         (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @since 1.8
     */
    default V computeIfAbsent(K key,
            Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);                     // 判断给定的function是否为null
        V v;
        if ((v = get(key)) == null) {
            V newValue;
            if ((newValue = mappingFunction.apply(key)) != null) {
                put(key, newValue);                                  // 只有在计算结果非null的时候才设置新值value
                return newValue;
            }
        }

        return v;
    }

    /**
     * If the value for the specified key is present and non-null, attempts to
     * compute a new mapping given the key and its current mapped value.
     * 如果给定的key关联的value存在，并且非null，尝试计算新的value值，作为给定的key映射value。
     *
     * <p>If the function returns {@code null}, the mapping is removed.  If the
     * function itself throws an (unchecked) exception, the exception is
     * rethrown, and the current mapping is left unchanged.
     * 如果方法返回null，指定key的映射会被移除。
     * 如果方法自身抛出（未检查）异常，异常会被该方法重新抛出，并且指定key的映射保持不变。
     *
    * (源码中这个*就是凸出来的)
     * @implSpec
     * The default implementation is equivalent to performing the following
     * steps for this {@code map}, then returning the current value or
     * {@code null} if now absent:
     *
     * <pre> {@code
     * if (map.get(key) != null) {
     *     V oldValue = map.get(key);
     *     V newValue = remappingFunction.apply(key, oldValue);
     *     if (newValue != null)
     *         map.put(key, newValue);
     *     else
     *         map.remove(key);
     * }
     * }</pre>
     *
     * <p>The default implementation makes no guarantees about synchronization
     * or atomicity properties of this method. Any implementation providing
     * atomicity guarantees must override this method and document its
     * concurrency properties. In particular, all implementations of
     * subinterface {@link java.util.concurrent.ConcurrentMap} must document
     * whether the function is applied once atomically only if the value is not
     * present.
     * 该方法默认实现不保证同步或者原子性属性。
     * 任何提供原子性保证的实现必须重写（覆盖）此方法并记录下它的并发属性。
     * 特别是（in particular），子接口java.util.concurrent.ConcurrentMap的所有实现，必须记录该方法是否仅在value不存在时支持以原子性操作执行一次。
     *
     * @param key key with which the specified value is to be associated
     * @param remappingFunction the function to compute a value
     * @return the new value associated with the specified key, or null if none
     *         返回新的value，如果计算结果为null，返回null
     * @throws NullPointerException if the specified key is null and
     *         this map does not support null keys, or the
     *         remappingFunction is null
     * @throws UnsupportedOperationException if the {@code put} operation
     *         is not supported by this map
     *         (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws ClassCastException if the class of the specified key or value
     *         prevents it from being stored in this map
     *         (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @since 1.8
     */
    default V computeIfPresent(K key,
            BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        V oldValue;
        if ((oldValue = get(key)) != null) {
            V newValue = remappingFunction.apply(key, oldValue);  // 用旧值计算新值
            if (newValue != null) {
                put(key, newValue);                               // 如果新值不为null，则设置该key对应的新值，并返回新值
                return newValue;
            } else {
                remove(key);                                      // 如果新值为null，则删除该key的映射，并返回null
                return null;
            }
        } else {
            return null;                                        // 如果旧值为null，返回null
        }
    }

    /**
     * Attempts to compute a mapping for the specified key and its current
     * mapped value (or {@code null} if there is no current mapping). For
     * example, to either create or append a {@code String} msg to a value
     * mapping:
     * 尝试计算给定key与其当前映射的value的映射（如果当前没有映射，则为null）。
     * （通俗的说法，就是用key与它的当前value值进行计算，得到新值value）
     * 例如，创建或附加一个String类型的msg到一个value映射：
     *
     * <pre> {@code
     * map.compute(key, (k, v) -> (v == null) ? msg : v.concat(msg))}</pre>
     * (Method {@link #merge merge()} is often simpler to use for such purposes.)
     * （merge方法通常就用于将此类操作简化）
     *
     * <p>If the function returns {@code null}, the mapping is removed (or
     * remains absent if initially absent).  If the function itself throws an
     * (unchecked) exception, the exception is rethrown, and the current mapping
     * is left unchanged.
     * 如果方法返回null，该映射删除（或者如果最初不存在，则保留不存在）。（原来没有映射关系，就不做删除操作）
     * 如果计算方法自身抛出（未检查）异常，该方法重新抛出异常，并且指定key的映射保持不变。
     *
     * @implSpec
     * The default implementation is equivalent to performing the following
     * steps for this {@code map}, then returning the current value or
     * {@code null} if absent:
     *
     * <pre> {@code
     * V oldValue = map.get(key);
     * V newValue = remappingFunction.apply(key, oldValue);
     * if (oldValue != null ) {
     *    if (newValue != null)
     *       map.put(key, newValue);
     *    else
     *       map.remove(key);
     * } else {
     *    if (newValue != null)
     *       map.put(key, newValue);
     *    else
     *       return null;
     * }
     * }</pre>
     *
     * <p>The default implementation makes no guarantees about synchronization
     * or atomicity properties of this method. Any implementation providing
     * atomicity guarantees must override this method and document its
     * concurrency properties. In particular, all implementations of
     * subinterface {@link java.util.concurrent.ConcurrentMap} must document
     * whether the function is applied once atomically only if the value is not
     * present.
     * 该方法默认实现不保证同步或者原子性属性。
     * 任何提供原子性保证的实现必须重写（覆盖）此方法并记录下它的并发属性。
     * 特别是（in particular），子接口java.util.concurrent.ConcurrentMap的所有实现，必须记录该方法是否仅在value不存在时支持以原子性操作执行一次。
     *
     * @param key key with which the specified value is to be associated
     * @param remappingFunction the function to compute a value
     * @return the new value associated with the specified key, or null if none
     * @throws NullPointerException if the specified key is null and
     *         this map does not support null keys, or the
     *         remappingFunction is null
     * @throws UnsupportedOperationException if the {@code put} operation
     *         is not supported by this map
     *         (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws ClassCastException if the class of the specified key or value
     *         prevents it from being stored in this map
     *         (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @since 1.8
     */
    default V compute(K key,
            BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        V oldValue = get(key);                                  // 获取给定key的旧值value

        V newValue = remappingFunction.apply(key, oldValue);    // 用key与旧值value计算新值value
        if (newValue == null) {
            // delete mapping
            if (oldValue != null || containsKey(key)) {        // 如果新值为null，并且key映射存在（有非null旧值，或者旧值为null时key存在（key-null））
                // something to remove
                remove(key);                                    // 移除key的映射关系，返回null
                return null;
            } else {                                            // 如果新值为null，并且key映射不存在，啥也不做，返回null
                // nothing to do. Leave things as they were.
                return null;
            }
        } else {
            // add or replace old mapping
            put(key, newValue);                                 // 如果新值不为null，设置该key映射到新值value
            return newValue;
        }
    }

    /**
     * If the specified key is not already associated with a value or is
     * associated with null, associates it with the given non-null value.
     * Otherwise, replaces the associated value with the results of the given
     * remapping function, or removes if the result is {@code null}. This
     * method may be of use when combining multiple mapped values for a key.
     * For example, to either create or append a {@code String msg} to a
     * value mapping:
     * 如果给定的key没有关联value，或者关联的value为null，则将该key关联到给定的非null value。
     * 否则，用给定的remapping方法计算结果替换原关联值，如果计算结果为null，则删除该key的映射。（用旧值与给定值计算新值）
     * 当key组合映射到多个value上时，该方法可能有用。
     * 例如，创建或附件一个String类型的msg到value的映射：
     *
     * <pre> {@code
     * map.merge(key, msg, String::concat)
     * }</pre>
     *
     * <p>If the function returns {@code null} the mapping is removed.  If the
     * function itself throws an (unchecked) exception, the exception is
     * rethrown, and the current mapping is left unchanged.
     * 如果方法返回null，该映射删除。
     * 如果计算方法自身抛出（未检查）异常，该方法重新抛出异常，并且指定key的映射保持不变。
     *
     * @implSpec
     * The default implementation is equivalent to performing the following
     * steps for this {@code map}, then returning the current value or
     * {@code null} if absent:
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
     * <p>The default implementation makes no guarantees about synchronization
     * or atomicity properties of this method. Any implementation providing
     * atomicity guarantees must override this method and document its
     * concurrency properties. In particular, all implementations of
     * subinterface {@link java.util.concurrent.ConcurrentMap} must document
     * whether the function is applied once atomically only if the value is not
     * present.
     * 该方法默认实现不保证同步或者原子性属性。
     * 任何提供原子性保证的实现必须重写（覆盖）此方法并记录下它的并发属性。
     * 特别是（in particular），子接口java.util.concurrent.ConcurrentMap的所有实现，必须记录该方法是否仅在value不存在时支持以原子性操作执行一次。
     *
     * @param key key with which the resulting value is to be associated
     * @param value the non-null value to be merged with the existing value
     *        associated with the key or, if no existing value or a null value
     *        is associated with the key, to be associated with the key
     * @param remappingFunction the function to recompute a value if present
     * @return the new value associated with the specified key, or null if no
     *         value is associated with the key
     *         返回关联到给定key的新值value，如果没有关联到该key的value，返回null
     * @throws UnsupportedOperationException if the {@code put} operation
     *         is not supported by this map
     *         (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws ClassCastException if the class of the specified key or value
     *         prevents it from being stored in this map
     *         (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified key is null and this map
     *         does not support null keys or the value or remappingFunction is
     *         null
     * @since 1.8
     */
    default V merge(K key, V value,
            BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        Objects.requireNonNull(value);                        // 给定值不能为null
        V oldValue = get(key);
        V newValue = (oldValue == null) ? value :
                   remappingFunction.apply(oldValue, value);  // 用旧值与给定值计算新值
        if(newValue == null) {                               // 如果新值为null，删除映射关系
            remove(key);
        } else {
            put(key, newValue);                               // 否则设置key映射到新值
        }
        return newValue;                                      // 返回新值
    }
}
```