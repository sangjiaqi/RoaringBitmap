/*
 * (c) the authors Licensed under the Apache License, Version 2.0.
 */

package org.roaringbitmap.bytelong;

import java.io.DataOutput;
import java.io.IOException;

/**
 * Interface representing an immutable bitmap.
 *
 */
public interface ImmutableLongBitmapDataProvider {
  /**
   * 检查是否包含值，这相当于检查是否设置了相应的位
   * Checks whether the value in included, which is equivalent to checking if the corresponding bit
   * is set (get in BitSet class).
   *
   * @param x long value
   * @return whether the long value is included.
   */
  public boolean contains(long x);

  /**
   * 返回添加到位图的不同整数的数量，这将返回一个完整的64位结果
   * Returns the number of distinct integers added to the bitmap (e.g., number of bits set). This
   * returns a full 64-bit result.
   *
   * @return the cardinality
   */
  public long getLongCardinality();

  /**
   * 访问位图中的所有值并将它们传递给使用者
   * Visit all values in the bitmap and pass them to the consumer.
   * 
   * * Usage:
   * 
   * <pre>
   * {@code
   *  bitmap.forEach(new LongConsumer() {
   *
   *    {@literal @}Override
   *    public void accept(long value) {
   *      // do something here
   *      
   *    }});
   *   }
   * }
   * </pre>
   * 
   * @param lc the consumer
   */
  public void forEach(LongConsumer lc);

  /**
   * 返回一个 Long 类型的迭代器
   * For better performance, consider the Use the {@link #forEach forEach} method.
   * 
   * @return a custom iterator over set bits, the bits are traversed in ascending sorted order
   */
  // RoaringBitmap proposes a PeekableLongIterator
  public LongIterator getLongIterator();

  /**
   * 返回一个反向 Long 类型的迭代器
   * @return a custom iterator over set bits, the bits are traversed in descending sorted order
   */
  // RoaringBitmap proposes a PeekableLongIterator
  public LongIterator getReverseLongIterator();

  /**
   * 估计此数据结构的内存使用情况
   * Estimate of the memory usage of this data structure.
   * 
   * Internally, this is computed as a 64-bit counter.
   *
   * @return estimated memory usage.
   */
  public int getSizeInBytes();

  /**
   * 估计此数据结构的内存使用情况，提供完整的64位数字
   * Estimate of the memory usage of this data structure. Provides full 64-bit number.
   *
   * @return estimated memory usage.
   */
  public long getLongSizeInBytes();

  /**
   * 检查 bitmap 是否为空
   * Checks whether the bitmap is empty.
   *
   * @return true if this bitmap contains no set bit
   */
  public boolean isEmpty();

  /**
   * 创建同一个类的新位图，最多包含 maxcardinality 个整数
   * Create a new bitmap of the same class, containing at most maxcardinality integers.
   *
   * @param x maximal cardinality
   * @return a new bitmap with cardinality no more than maxcardinality
   */
  public ImmutableLongBitmapDataProvider limit(long x);

  /**
   * 排序返回小于或等于 x 个的整数
   * Rank returns the number of integers that are smaller or equal to x (Rank(infinity) would be
   * GetCardinality()).
   * 
   * The value is a full 64-bit value.
   * 
   * @param x upper limit
   *
   * @return the rank
   */
  public long rankLong(long x);

  /**
   * 返回排序后第 j 个值
   * Return the jth value stored in this bitmap.
   *
   * @param j index of the value
   *
   * @return the value
   */
  public long select(long j);

  /**
   * 序列化 bitmap
   * Serialize this bitmap.
   *
   * The current bitmap is not modified.
   *
   * @param out the DataOutput stream
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public void serialize(DataOutput out) throws IOException;

  /**
   * 返回序列化 bitmap 所需要的字节数
   * Report the number of bytes required to serialize this bitmap. This is the number of bytes
   * written out when using the serialize method. When using the writeExternal method, the count
   * will be higher due to the overhead of Java serialization.
   *
   * @return the size in bytes
   */
  public long serializedSizeInBytes();

  /**
   * 返回设置值的数组，是按顺序排序的
   * Return the set values as an array. The integer values are in sorted order.
   *
   * @return array representing the set values.
   */
  public long[] toArray();

}
