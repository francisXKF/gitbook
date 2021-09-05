# AbstractMap

```java
/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.util;
import java.util.Map.Entry;

/**
 * This class provides a skeletal implementation of the <tt>Map</tt>
 * interface, to minimize the effort required to implement this interface.
 * 该类对Map接口提供了骨架实现，以尽量减少实现Map接口所需要的工作量。
 *
 * <p>To implement an unmodifiable map, the programmer needs only to extend this
 * class and provide an implementation for the <tt>entrySet</tt> method, which
 * returns a set-view of the map's mappings.  Typically, the returned set
 * will, in turn, be implemented atop <tt>AbstractSet</tt>.  This set should
 * not support the <tt>add</tt> or <tt>remove</tt> methods, and its iterator
 * should not support the <tt>remove</tt> method.
 * 实现不可修改的map，程序员仅需要扩展本类并且提供对entrySet方法的实现，entrySet方法返回map的映射set集合视图。
 * 通常，返回的set集合将依次在AbstractSet之上实现。
 * 该set集合将不支持add、remove方法，并且它的迭代器不支持remove方法。
 *
 * <p>To implement a modifiable map, the programmer must additionally override
 * this class's <tt>put</tt> method (which otherwise throws an
 * <tt>UnsupportedOperationException</tt>), and the iterator returned by
 * <tt>entrySet().iterator()</tt> must additionally implement its
 * <tt>remove</tt> method.
 * 实现可修改的map，程序员必须额外覆盖（override 重写）本类的put方法（否则将抛出UnsupportedOperationException），
 * 并且通过entrySet()/iterator()返回的迭代器必须额外实现它的remove方法。
 *
 * <p>The programmer should generally provide a void (no argument) and map
 * constructor, as per the recommendation in the <tt>Map</tt> interface
 * specification.
 * 根据Map接口的建议，程序员通常提供了一个void（无参数）和一个map的构造函数。
 * (Map接口的建议：所有通用目的的map实现类都应该支持两种“标准”构造方法，一种是无参，一种是map（会拷贝成等效map）)
 *
 * <p>The documentation for each non-abstract method in this class describes its
 * implementation in detail.  Each of these methods may be overridden if the
 * map being implemented admits a more efficient implementation.
 * 对于在本类中的每个非抽象方法，该文档描述了他们的实现细节。
 * 每个方法都可能被覆盖，如果正在实现的map允许更有效的实现。
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 * 本类属于Java Collection Framework
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 *
 * @author  Josh Bloch
 * @author  Neal Gafter
 * @see Map
 * @see Collection
 * @since 1.2
 */

public abstract class AbstractMap<K,V> implements Map<K,V> {
    /**
     * Sole constructor.  (For invocation by subclass constructors, typically
     * implicit.)
     * 唯一构造器。（对于子类构造函数的调用，通常是隐式的）
     */
    protected AbstractMap() {
    }

    // Query Operations
    // 查询操作

    /**
     * {@inheritDoc}
     * 继承文档
     * 注意，{@inheritDoc}表明
     *
     * @implSpec
     * 实现规范
     * This implementation returns <tt>entrySet().size()</tt>.
     * 该实现返回entrySet().size()
     *
     */
    public int size() {
        return entrySet().size();
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * This implementation returns <tt>size() == 0</tt>.
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * This implementation iterates over <tt>entrySet()</tt> searching
     * for an entry with the specified value.  If such an entry is found,
     * <tt>true</tt> is returned.  If the iteration terminates without
     * finding such an entry, <tt>false</tt> is returned.  Note that this
     * implementation requires linear time in the size of the map.
     * 此实现通过遍历entrySet()来搜索具有指定value的entry。
     * 如果搜到对应的entry，返回true。
     * 如果迭代结束没有找到对应的entry，返回false。
     * 注意，该实现所需要的时间与map的大小成线性相关。
     *
     * @throws ClassCastException   {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public boolean containsValue(Object value) {
        Iterator<Entry<K,V>> i = entrySet().iterator(); // （这个迭代器对象缩写成i看呆了）
        if (value==null) {                             // 区分null与非null的情况，分别进行遍历
            while (i.hasNext()) {
                Entry<K,V> e = i.next();
                if (e.getValue()==null)
                    return true;
            }
        } else {
            while (i.hasNext()) {
                Entry<K,V> e = i.next();
                if (value.equals(e.getValue()))
                    return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * This implementation iterates over <tt>entrySet()</tt> searching
     * for an entry with the specified key.  If such an entry is found,
     * <tt>true</tt> is returned.  If the iteration terminates without
     * finding such an entry, <tt>false</tt> is returned.  Note that this
     * implementation requires linear time in the size of the map; many
     * implementations will override this method.
     * 此实现通过遍历entrySet()来搜索具有指定key的entry。
     * 如果搜到对应的entry，返回true。
     * 如果迭代结束没有找到对应的entry，返回false。
     * 注意，该实现所需要的时间与map的大小成线性相关。
     * 许多实现会重写（override）该方法。（因为有很多对提高key查询效率做的优化）
     *
     * @throws ClassCastException   {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public boolean containsKey(Object key) {
        Iterator<Map.Entry<K,V>> i = entrySet().iterator();
        if (key==null) {
            while (i.hasNext()) {
                Entry<K,V> e = i.next();
                if (e.getKey()==null)
                    return true;
            }
        } else {
            while (i.hasNext()) {
                Entry<K,V> e = i.next();
                if (key.equals(e.getKey()))
                    return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * This implementation iterates over <tt>entrySet()</tt> searching
     * for an entry with the specified key.  If such an entry is found,
     * the entry's value is returned.  If the iteration terminates without
     * finding such an entry, <tt>null</tt> is returned.  Note that this
     * implementation requires linear time in the size of the map; many
     * implementations will override this method.
     * 此实现通过遍历entrySet()来搜索具有指定key的entry。
     * 如果搜到对应的entry，返回该entry的value值。
     * 如果迭代结束没有找到对应的entry，返回null。
     * 注意，该实现所需要的时间与map的大小成线性相关。
     * 许多实现会重写（override）该方法。（对key的访问优化）
     *
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     */
    public V get(Object key) {
        Iterator<Entry<K,V>> i = entrySet().iterator();
        if (key==null) {
            while (i.hasNext()) {
                Entry<K,V> e = i.next();
                if (e.getKey()==null)
                    return e.getValue();
            }
        } else {
            while (i.hasNext()) {
                Entry<K,V> e = i.next();
                if (key.equals(e.getKey()))
                    return e.getValue();
            }
        }
        return null;
    }


    // Modification Operations
    // 修改操作

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * This implementation always throws an
     * <tt>UnsupportedOperationException</tt>.
     * 该实现总是抛出UnsupportedOperationException。（啥原因，因为不能通过entry来put么？？？）
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     */
    public V put(K key, V value) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * This implementation iterates over <tt>entrySet()</tt> searching for an
     * entry with the specified key.  If such an entry is found, its value is
     * obtained with its <tt>getValue</tt> operation, the entry is removed
     * from the collection (and the backing map) with the iterator's
     * <tt>remove</tt> operation, and the saved value is returned.  If the
     * iteration terminates without finding such an entry, <tt>null</tt> is
     * returned.  Note that this implementation requires linear time in the
     * size of the map; many implementations will override this method.
     * 此实现通过遍历entrySet()来搜索具有指定key的entry。
     * 如果搜到对应的entry，则通过getValue操作获取该key对应的value值，并通过迭代器的remove操作将该entry从集合中（也会反映到map上）移除，并返回刚保存的value值。
     * 如果遍历结束没有找到对应的entry，返回null。
     * 注意，该实现所需要的时间与map的大小成线性相关。
     * 许多实现会重写（override）该方法。（对key的访问优化）
     *
     * <p>Note that this implementation throws an
     * <tt>UnsupportedOperationException</tt> if the <tt>entrySet</tt>
     * iterator does not support the <tt>remove</tt> method and this map
     * contains a mapping for the specified key.
     * 注意，如果entrySet迭代器不支持remove方法，并且该map包含给定key的映射，则抛出UnsupportedOperationException。
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     */
    public V remove(Object key) {
        Iterator<Entry<K,V>> i = entrySet().iterator();
        Entry<K,V> correctEntry = null;
        if (key==null) {
            while (correctEntry==null && i.hasNext()) {  // 找到对应的entry结束
                Entry<K,V> e = i.next();
                if (e.getKey()==null)
                    correctEntry = e;                     // 保存对应的entry
            }
        } else {
            while (correctEntry==null && i.hasNext()) {
                Entry<K,V> e = i.next();
                if (key.equals(e.getKey()))
                    correctEntry = e;
            }
        }

        // 不在遍历时直接保存oldValue的值，而是通过保存entry再获取value值，是因为oldValue为null时不好判断map中是否存在给定key？？？那为啥不用findFlag+oldValue这种东西？？？
        V oldValue = null;
        if (correctEntry !=null) {
            oldValue = correctEntry.getValue();           // 通过entry获取给定key对应的value
            i.remove();                                   // 通过迭代器删除该entry
        }
        return oldValue;
    }


    // Bulk Operations
    // 批量操作

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * This implementation iterates over the specified map's
     * <tt>entrySet()</tt> collection, and calls this map's <tt>put</tt>
     * operation once for each entry returned by the iteration.
     * 本实现通过迭代给定map（mapA）的entrySet()集合，并对通过迭代返回的每一个entry调用该map（mapB）的put操作。
     *
     * <p>Note that this implementation throws an
     * <tt>UnsupportedOperationException</tt> if this map does not support
     * the <tt>put</tt> operation and the specified map is nonempty.
     * 注意，如果该map不支持put操作并且给定map不为空，则抛出UnsupportedOperationException。
     *
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     */
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
            put(e.getKey(), e.getValue());                           // 遍历给定map，将每个entry都加入到当前map中
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * This implementation calls <tt>entrySet().clear()</tt>.
     * 本实现调用entrySet().clear()
     *
     * <p>Note that this implementation throws an
     * <tt>UnsupportedOperationException</tt> if the <tt>entrySet</tt>
     * does not support the <tt>clear</tt> operation.
     * 注意，如果entrySet不支持clear操作，则抛出UnsupportedOperationException。
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     */
    public void clear() {
        entrySet().clear();
    }


    // Views
    // 视图

    /**
     * Each of these fields are initialized to contain an instance of the
     * appropriate view the first time this view is requested.  The views are
     * stateless, so there's no reason to create more than one of each.
     * 在第一次请求该视图的时候，每一个字段都被初始化为包含相应（appropriate）视图的实例。
     * 视图是无状态的，所以没有必要创建多个视图。
     *
     * <p>Since there is no synchronization performed while accessing these fields,
     * it is expected that java.util.Map view classes using these fields have
     * no non-final fields (or any fields at all except for outer-this). Adhering
     * to this rule would make the races on these fields benign.
     * 由于在访问这些字段的时候没有（限制）同步执行，因此使用这些字段的java.util.Map视图应该没有使用非final字段（或除了outer-this之外的任何字段）。
     * 遵守这一规则将使得在这些字段上的竞争变得良性。
     * （必须使用final来修饰引用下面这些视图的field？？？）
     *
     * <p>It is also imperative that implementations read the field only once,
     * as in:
     * 实现只读取该字段一次也是有必要的。（仅有一次读取赋值）
     *
     * <pre> {@code
     * public Set<K> keySet() {
     *   Set<K> ks = keySet;  // single racy read
     *   if (ks == null) {
     *     ks = new KeySet();
     *     keySet = ks;
     *   }
     *   return ks;
     * }
     *}</pre>
     */
    transient Set<K>        keySet; // transient修饰，不会被序列化
    transient Collection<V> values;

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * This implementation returns a set that subclasses {@link AbstractSet}.
     * The subclass's iterator method returns a "wrapper object" over this
     * map's <tt>entrySet()</tt> iterator.  The <tt>size</tt> method
     * delegates to this map's <tt>size</tt> method and the
     * <tt>contains</tt> method delegates to this map's
     * <tt>containsKey</tt> method.
     * 返回AbstractSet的子类实现。
     * 该子类的迭代器方法，在该map的entrySet()迭代器上返回一个“包装器对象”。（借用了该map的entrySet()迭代器）
     * size方法委托给该map的size方法，contains方法委托给该map的containsKey方法。（都是借用）
     *
     * <p>The set is created the first time this method is called,
     * and returned in response to all subsequent calls.  No synchronization
     * is performed, so there is a slight chance that multiple calls to this
     * method will not all return the same set.
     * 该set在首次调用该方法时创建（并返回），并且在后续的调用中直接返回。
     * 没有同步执行（的限制），因此（slight chance有微弱的机会）多次调用该方法可能不会总返回相同set。
     *
     */
    public Set<K> keySet() {
        Set<K> ks = keySet;
        if (ks == null) {                                                       // 如果ks非空则直接返回值（可能是第一次，也有可能是并发的问题）
            ks = new AbstractSet<K>() {
                public Iterator<K> iterator() {                                 // AbstractSet继承的AbstractConllection（它又实现的Collection），实现类必须实现iterator方法。
                    return new Iterator<K>() {
                        private Iterator<Entry<K,V>> i = entrySet().iterator(); // 调用该map的entrySet()迭代器方法进行了包装。

                        public boolean hasNext() {
                            return i.hasNext();
                        }

                        public K next() {
                            return i.next().getKey();                           // 因为该方法是keySet，所以要从entrySet中取出key来返回
                        }

                        public void remove() {
                            i.remove();
                        }
                    };
                }

                public int size() {
                    return AbstractMap.this.size();                             // 调用该map的size方法（表明key的个数与该map的个数是相等的）
                }

                public boolean isEmpty() {
                    return AbstractMap.this.isEmpty();
                }

                public void clear() {
                    AbstractMap.this.clear();                                   // keySet的clear会将整个map进行clear
                }

                public boolean contains(Object k) {
                    return AbstractMap.this.containsKey(k);                     // key的contains是通过调用map的containsKey来实现的
                }
            };
            keySet = ks;
        }
        return ks;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * This implementation returns a collection that subclasses {@link
     * AbstractCollection}.  The subclass's iterator method returns a
     * "wrapper object" over this map's <tt>entrySet()</tt> iterator.
     * The <tt>size</tt> method delegates to this map's <tt>size</tt>
     * method and the <tt>contains</tt> method delegates to this map's
     * <tt>containsValue</tt> method.
     * 返回AbstractCollection的子类实现。
     * 该子类的迭代器方法，在该map的entrySet()迭代器上返回一个“包装器对象”。（借用了该map的entrySet()迭代器）
     * size方法委托给该map的size方法，contains方法委托给该map的containsKey方法。（都是借用）
     *
     * <p>The collection is created the first time this method is called, and
     * returned in response to all subsequent calls.  No synchronization is
     * performed, so there is a slight chance that multiple calls to this
     * method will not all return the same collection.
     * 该collection在首次调用该方法时创建（并返回），并且在后续的调用中直接返回。
     * 没有同步执行（的限制），因此（slight chance有微弱的机会）多次调用该方法可能不会总返回相同set。
     *
     */
    public Collection<V> values() {
        Collection<V> vals = values;
        if (vals == null) {                                                      // 如果vals非空则直接返回值（可能是第一次，也有可能是并发的问题）
            vals = new AbstractCollection<V>() {
                public Iterator<V> iterator() {                                  // AbstractConllection（它又实现的Collection），实现类必须实现iterator方法。
                    return new Iterator<V>() {
                        private Iterator<Entry<K,V>> i = entrySet().iterator();  // 调用该map的entrySet()迭代器方法进行了包装。

                        public boolean hasNext() {
                            return i.hasNext();
                        }

                        public V next() {
                            return i.next().getValue();                           // 因为该方法是values，所以要从entrySet中取出value来返回
                        }

                        public void remove() {
                            i.remove();
                        }
                    };
                }

                public int size() {
                    return AbstractMap.this.size();                               // 用map的size返回，与keySet()一样
                }

                public boolean isEmpty() {
                    return AbstractMap.this.isEmpty();
                }

                public void clear() {
                    AbstractMap.this.clear();                                    // 调用values的clear也会将整个map给clear
                }

                public boolean contains(Object v) {
                    return AbstractMap.this.containsValue(v);                    // contains是通过map的containsValue(v)方法实现的
                }
            };
            values = vals;
        }
        return vals;
    }

    public abstract Set<Entry<K,V>> entrySet(); // 需要实现类自己定义


    // Comparison and hashing
    // 比较与hash

    /**
     * Compares the specified object with this map for equality.  Returns
     * <tt>true</tt> if the given object is also a map and the two maps
     * represent the same mappings.  More formally, two maps <tt>m1</tt> and
     * <tt>m2</tt> represent the same mappings if
     * <tt>m1.entrySet().equals(m2.entrySet())</tt>.  This ensures that the
     * <tt>equals</tt> method works properly across different implementations
     * of the <tt>Map</tt> interface.
     * 比较给定的object与该map是否相等。
     * 如果给定的object也是个map，并且与该map代表相同的映射，则返回true。
     * 更正式的说法，两个map：m1与m2代表相同的映射，当且仅当m1.entrySet().equals(m2.entrySet())。
     * 这确保了equals方法可以在Map接口的不同实现中正常工作。
     *
     * @implSpec
     * This implementation first checks if the specified object is this map;
     * if so it returns <tt>true</tt>.  Then, it checks if the specified
     * object is a map whose size is identical to the size of this map; if
     * not, it returns <tt>false</tt>.  If so, it iterates over this map's
     * <tt>entrySet</tt> collection, and checks that the specified map
     * contains each mapping that this map contains.  If the specified map
     * fails to contain such a mapping, <tt>false</tt> is returned.  If the
     * iteration completes, <tt>true</tt> is returned.
     * 该实现首先检查给定的object是否为该（本）map；如果是，则返回true。
     * 然后，检查给定的map是否是大小与该map一致的map，如果不是，返回false。
     * 如果相同，迭代遍历该map的entrySet集合，检查给定的map是否包含该map含有的所有映射。
     * 如果给定的map未包含这样的映射，返回false。
     * 如果迭代完成，返回true。
     *
     * @param o object to be compared for equality with this map
     * @return <tt>true</tt> if the specified object is equal to this map
     */
    public boolean equals(Object o) {
        if (o == this)                                             // 首先判断是否为同一个对象
            return true;

        if (!(o instanceof Map))                                   // 2、判断给定对象是否为map类型
            return false;
        Map<?,?> m = (Map<?,?>) o;
        if (m.size() != size())                                    // 3、将给定对象强转成map，判断给定对象的size是否与本map的size相同
            return false;

        try {
            Iterator<Entry<K,V>> i = entrySet().iterator();         // 4、迭代遍历本map的entry，进行两个对象间的key、value比较
            while (i.hasNext()) {
                Entry<K,V> e = i.next();
                K key = e.getKey();
                V value = e.getValue();
                if (value == null) {
                    if (!(m.get(key)==null && m.containsKey(key)))  // 5、如果本entry的value为null，判断给定的对象该key对应的value是否为null（containsKey是为了在get为null时，确认key是否存在）
                        return false;
                } else {
                    if (!value.equals(m.get(key)))                  // 6、如果本entry的value不为null，通过给定对象的get(key)来比较value是否相等。（能get到非null的说明key是存在的）
                        return false;
                }
            }
        } catch (ClassCastException unused) {
            return false;
        } catch (NullPointerException unused) {
            return false;
        }

        return true;
    }

    /**
     * Returns the hash code value for this map.  The hash code of a map is
     * defined to be the sum of the hash codes of each entry in the map's
     * <tt>entrySet()</tt> view.  This ensures that <tt>m1.equals(m2)</tt>
     * implies that <tt>m1.hashCode()==m2.hashCode()</tt> for any two maps
     * <tt>m1</tt> and <tt>m2</tt>, as required by the general contract of
     * {@link Object#hashCode}.
     * 返回该map的hash值。
     * map的hash值，是由map中entrySet()视图里每一个entry的hash值相加得到的。
     * 确保了对任意两个map：m1与m2，当m1.equals(m2)成立时m1.hashCode()==m2.hashCode()也成立，符合Object#hashCode的普遍要求。
     *
     * @implSpec
     * This implementation iterates over <tt>entrySet()</tt>, calling
     * {@link Map.Entry#hashCode hashCode()} on each element (entry) in the
     * set, and adding up the results.
     * 此实现迭代entrySet()，对在set中每一个entry元素调用Map.Entry#hashCode的hashCode()，相加得到结果。
     *
     * @return the hash code value for this map
     * @see Map.Entry#hashCode()
     * @see Object#equals(Object)
     * @see Set#equals(Object)
     */
    public int hashCode() {
        int h = 0;
        Iterator<Entry<K,V>> i = entrySet().iterator();
        while (i.hasNext())
            h += i.next().hashCode();   // 迭代相加每个entry的hash值，结果就作为该map的hash值
        return h;
    }

    /**
     * Returns a string representation of this map.  The string representation
     * consists of a list of key-value mappings in the order returned by the
     * map's <tt>entrySet</tt> view's iterator, enclosed in braces
     * (<tt>"{}"</tt>).  Adjacent mappings are separated by the characters
     * <tt>", "</tt> (comma and space).  Each key-value mapping is rendered as
     * the key followed by an equals sign (<tt>"="</tt>) followed by the
     * associated value.  Keys and values are converted to strings as by
     * {@link String#valueOf(Object)}.
     * 返回一个表示该map的string。
     * string的表现形式为包含key-value映射的列表，按照map的entrySet视图顺序排列，括在{}中。
     * 相邻（adjacent 相邻）的映射通过“, ”分隔（逗号与空格）。
     * 每个key-value映射的渲染都是key后面跟着等号“=”，再跟着对应的value值。
     * key和value通过String#valueOf(Object)转化成string类型。
     *
     * @return a string representation of this map
     */
    public String toString() {
        Iterator<Entry<K,V>> i = entrySet().iterator();
        if (! i.hasNext())
            return "{}";

        StringBuilder sb = new StringBuilder();             // 用StringBuilder来构建字符串，没有同步保证（并发还是用StringBuffer吧）（This class provides an API compatible with StringBuffer, but with no guarantee of synchronization. ）
        sb.append('{');
        for (;;) {
            Entry<K,V> e = i.next();
            K key = e.getKey();
            V value = e.getValue();
            sb.append(key   == this ? "(this Map)" : key);  // 判断了一下是否是this，是的话就不嵌套该map.toString()了（因为调用this.toString()可能造成无限递归？？？）
            sb.append('=');
            sb.append(value == this ? "(this Map)" : value); // sb.append(Object obj)，会调用String.valueOf(obj)转化为String
            if (! i.hasNext())
                return sb.append('}').toString();
            sb.append(',').append(' ');
        }
    }

    /**
     * Returns a shallow copy of this <tt>AbstractMap</tt> instance: the keys
     * and values themselves are not cloned.
     * 返回该AbstractMap实例的浅拷贝：keys与values本身不会拷贝（不会创建keys、values的副本，只是多了个AbstractMap的副本）
     * （堆里有两个AbstractMap实例对象，拷贝后的result没有keys、values了）
     *
     * @return a shallow copy of this map
     */
    protected Object clone() throws CloneNotSupportedException {
        AbstractMap<?,?> result = (AbstractMap<?,?>)super.clone();
        result.keySet = null;
        result.values = null;
        return result;
    }

    /**
     * Utility method for SimpleEntry and SimpleImmutableEntry.
     * Test for equality, checking for nulls.
     * SimpleEntry与SimpleImmutableEntry的实用（Utility）方法。
     * 测试相等性，检查null值。
     *
     * NB: Do not replace with Object.equals until JDK-8015417 is resolved.
     * 注意（NB）：不要用Object.equals来替换该方法，直到JDK-8015417被解决。
     * （不知道是否可用JDK1.7的Objects.equals()来做）
     */
    private static boolean eq(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }

    // Implementation Note: SimpleEntry and SimpleImmutableEntry
    // are distinct unrelated classes, even though they share
    // some code. Since you can't add or subtract final-ness
    // of a field in a subclass, they can't share representations,
    // and the amount of duplicated code is too small to warrant
    // exposing a common abstract class.
    // 实现说明：SimpleEntry（简单Entry）与SimpleImmutableEntry（简单不可变Entry）是不同的不相关的类，即使他们有相同的代码。
    // 由于你不能在子类中添加或者减去字段的最终性（final-ness），所以他们不能共享表示，
    // 并且重复的代码太少，无法保证（warrant）能够从里面抽象出来一个公共类（公共抽象类）。


    /**
     * An Entry maintaining a key and a value.  The value may be
     * changed using the <tt>setValue</tt> method.  This class
     * facilitates the process of building custom map
     * implementations. For example, it may be convenient to return
     * arrays of <tt>SimpleEntry</tt> instances in method
     * <tt>Map.entrySet().toArray</tt>.
     * 维护一个key与value的Entry。
     * 可以使用setValue来修改内部的value值。
     * 该类有助于（facilitate）自定义map的构建过程。
     * 例如：在方法Map.entrySet().toArray返回SimpleEntry数组可能更方便（convenient）。
     *
     * @since 1.6
     */
    public static class SimpleEntry<K,V>
        implements Entry<K,V>, java.io.Serializable // 这个Entry接口，是Map接口里的interface Entry<K,V>
    {
        private static final long serialVersionUID = -8499721149061103585L;

        // 只有两个字段
        private final K key; // key作为final，只能赋值一次
        private V value;     // value可以任意修改

        /**
         * Creates an entry representing a mapping from the specified
         * key to the specified value.
         * 创建一个entry，表示从给定key到给定value的映射。
         *
         * @param key the key represented by this entry
         * @param value the value represented by this entry
         */
        public SimpleEntry(K key, V value) {
            this.key   = key;
            this.value = value;
        }

        /**
         * Creates an entry representing the same mapping as the
         * specified entry.
         * 创建一个entry，表示与给定entry相同的映射。
         *
         * @param entry the entry to copy
         */
        public SimpleEntry(Entry<? extends K, ? extends V> entry) {
            this.key   = entry.getKey();  // 拆解开分别赋值
            this.value = entry.getValue();
        }

        /**
         * Returns the key corresponding to this entry.
         * 返回该entry关联的key
         *
         * @return the key corresponding to this entry
         */
        public K getKey() {
            return key;
        }

        /**
         * Returns the value corresponding to this entry.
         * 返回该entry关联的value
         *
         * @return the value corresponding to this entry
         */
        public V getValue() {
            return value;
        }

        /**
         * Replaces the value corresponding to this entry with the specified
         * value.
         * 用给定value替换该entry关联的value值。
         *
         * @param value new value to be stored in this entry
         *        存入entry的新值
         * @return the old value corresponding to the entry
         *          返回旧值
         */
        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        /**
         * Compares the specified object with this entry for equality.
         * Returns {@code true} if the given object is also a map entry and
         * the two entries represent the same mapping.  More formally, two
         * entries {@code e1} and {@code e2} represent the same mapping
         * if<pre>
         *   (e1.getKey()==null ?
         *    e2.getKey()==null :
         *    e1.getKey().equals(e2.getKey()))
         *   &amp;&amp;
         *   (e1.getValue()==null ?
         *    e2.getValue()==null :
         *    e1.getValue().equals(e2.getValue()))</pre>
         * This ensures that the {@code equals} method works properly across
         * different implementations of the {@code Map.Entry} interface.
         * 比较给定的object与该entry是否相等。
         * 如果给定object也是个map，并且这俩map代表相同映射，则返回true。
         * 更正式的说法，两个entry：e1与e2,代表相同的映射，当且仅当：
         *   (e1.getKey()==null ?
         *    e2.getKey()==null :
         *    e1.getKey().equals(e2.getKey()))
         *   &&
         *   (e1.getValue()==null ?
         *    e2.getValue()==null :
         *    e1.getValue().equals(e2.getValue()))
         * 这确保了equals方法在不同的Map.Entry接口实现中能够正常工作。
         *
         * @param o object to be compared for equality with this map entry
         * @return {@code true} if the specified object is equal to this map
         *         entry
         * @see    #hashCode
         */
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> e = (Map.Entry<?,?>)o;
            return eq(key, e.getKey()) && eq(value, e.getValue()); // 直接通过AbstractMap#eq()方法来简化实现
        }

        /**
         * Returns the hash code value for this map entry.  The hash code
         * of a map entry {@code e} is defined to be: <pre>
         *   (e.getKey()==null   ? 0 : e.getKey().hashCode()) ^
         *   (e.getValue()==null ? 0 : e.getValue().hashCode())</pre>
         * This ensures that {@code e1.equals(e2)} implies that
         * {@code e1.hashCode()==e2.hashCode()} for any two Entries
         * {@code e1} and {@code e2}, as required by the general
         * contract of {@link Object#hashCode}.
         * 返回该map entry的hash code值。
         * map entry--e的hash code定义如下：
         *     (e.getKey()==null   ? 0 : e.getKey().hashCode()) ^
         *     (e.getValue()==null ? 0 : e.getValue().hashCode())
         * （用key的hashCode与value的hashCode做异或操作，作为Entry的hashCode） 
         * 保证了对于任意两个Entry：e1与e2，e1.equals(e2)成立意味着e1.hashCode()==e2.hashCode()，
         * 这也符合Object.hashCode基本约定要求。（即equals相等hashCode必须相等）
         *
         * @return the hash code value for this map entry
         * @see    #equals
         */
        public int hashCode() {
            return (key   == null ? 0 :   key.hashCode()) ^
                   (value == null ? 0 : value.hashCode());
        }

        /**
         * Returns a String representation of this map entry.  This
         * implementation returns the string representation of this
         * entry's key followed by the equals character ("<tt>=</tt>")
         * followed by the string representation of this entry's value.
         * 返回代表该map entry的String字符串。
         * 该实现返回的该entry的key的String表示，后面跟着等号“=”，再跟着entry的value的String表示
         *
         * @return a String representation of this map entry
         */
        public String toString() {
            return key + "=" + value;
        }

    }

    /**
     * An Entry maintaining an immutable key and value.  This class
     * does not support method <tt>setValue</tt>.  This class may be
     * convenient in methods that return thread-safe snapshots of
     * key-value mappings.
     * 维护一个不可变的key与value的Entry。
     * 该类不支持setValue方法。
     * 在返回线程安全的key-value映射的快照方法中使用该类可能更方便。
     *
     * @since 1.6
     */
    public static class SimpleImmutableEntry<K,V>
        implements Entry<K,V>, java.io.Serializable
    {
        private static final long serialVersionUID = 7138329143949025153L;

        private final K key;
        private final V value; // 与SimpleEntry不同的是，value也是final的

        /**
         * Creates an entry representing a mapping from the specified
         * key to the specified value.
         *
         * @param key the key represented by this entry
         * @param value the value represented by this entry
         */
        public SimpleImmutableEntry(K key, V value) {
            this.key   = key;
            this.value = value;
        }

        /**
         * Creates an entry representing the same mapping as the
         * specified entry.
         *
         * @param entry the entry to copy
         */
        public SimpleImmutableEntry(Entry<? extends K, ? extends V> entry) {
            this.key   = entry.getKey();
            this.value = entry.getValue();
        }

        /**
         * Returns the key corresponding to this entry.
         *
         * @return the key corresponding to this entry
         */
        public K getKey() {
            return key;
        }

        /**
         * Returns the value corresponding to this entry.
         *
         * @return the value corresponding to this entry
         */
        public V getValue() {
            return value;
        }

        /**
         * Replaces the value corresponding to this entry with the specified
         * value (optional operation).  This implementation simply throws
         * <tt>UnsupportedOperationException</tt>, as this class implements
         * an <i>immutable</i> map entry.
         * 该实现替换value会抛出UnsupportedOperationException，作为该类实现的不可变（immutable）map entry。
         *
         * @param value new value to be stored in this entry
         * @return (Does not return)
         * @throws UnsupportedOperationException always
         */
        public V setValue(V value) {
            throw new UnsupportedOperationException();
        }

        /**
         * Compares the specified object with this entry for equality.
         * Returns {@code true} if the given object is also a map entry and
         * the two entries represent the same mapping.  More formally, two
         * entries {@code e1} and {@code e2} represent the same mapping
         * if<pre>
         *   (e1.getKey()==null ?
         *    e2.getKey()==null :
         *    e1.getKey().equals(e2.getKey()))
         *   &amp;&amp;
         *   (e1.getValue()==null ?
         *    e2.getValue()==null :
         *    e1.getValue().equals(e2.getValue()))</pre>
         * This ensures that the {@code equals} method works properly across
         * different implementations of the {@code Map.Entry} interface.
         *
         * @param o object to be compared for equality with this map entry
         * @return {@code true} if the specified object is equal to this map
         *         entry
         * @see    #hashCode
         */
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> e = (Map.Entry<?,?>)o;
            return eq(key, e.getKey()) && eq(value, e.getValue());
        }

        /**
         * Returns the hash code value for this map entry.  The hash code
         * of a map entry {@code e} is defined to be: <pre>
         *   (e.getKey()==null   ? 0 : e.getKey().hashCode()) ^
         *   (e.getValue()==null ? 0 : e.getValue().hashCode())</pre>
         * This ensures that {@code e1.equals(e2)} implies that
         * {@code e1.hashCode()==e2.hashCode()} for any two Entries
         * {@code e1} and {@code e2}, as required by the general
         * contract of {@link Object#hashCode}.
         *
         * @return the hash code value for this map entry
         * @see    #equals
         */
        public int hashCode() {
            return (key   == null ? 0 :   key.hashCode()) ^
                   (value == null ? 0 : value.hashCode());
        }

        /**
         * Returns a String representation of this map entry.  This
         * implementation returns the string representation of this
         * entry's key followed by the equals character ("<tt>=</tt>")
         * followed by the string representation of this entry's value.
         *
         * @return a String representation of this map entry
         */
        public String toString() {
            return key + "=" + value;
        }

    }

}
```