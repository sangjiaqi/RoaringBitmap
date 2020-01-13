/*
 * (c) the authors Licensed under the Apache License, Version 2.0.
 */

package org.roaringbitmap.shortlong;

/**
 * Representing a general bitmap interface.
 *
 */
public interface LongBitmapDataProvider extends ImmutableLongBitmapDataProvider {
  /**
   * 设置值为 true，不管其是否已经出现
   * set the value to "true", whether it already appears or not.
   *
   * @param x long value
   */
  public void addLong(long x);

  /**
   * 删除当前有效的整数
   * If present remove the specified integers (effectively, sets its bit value to false)
   *
   * @param x long value representing the index in a bitmap
   */
  public void removeLong(long x);


  /**
   * 恢复已分配但未使用的内存
   * Recover allocated but unused memory.
   */
  public void trim();
}
