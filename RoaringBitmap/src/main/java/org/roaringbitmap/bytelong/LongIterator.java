/*
 * (c) the authors Licensed under the Apache License, Version 2.0.
 */
package org.roaringbitmap.bytelong;

/**
 * A simple iterator over long values. Using an IntIterator instead of Java's Iterator&lt;Long&gt;
 * avoids the overhead of the Long class: on some tests, LongIterator is nearly twice as fast as
 * Iterator&lt;Long&gt;.
 */
public interface LongIterator extends Cloneable {
  /**
   * 创建一个迭代器的复制
   * Creates a copy of the iterator.
   * 
   * @return a clone of the current iterator
   */
  LongIterator clone();

  /**
   * 是否有下一个值
   * @return whether there is another value
   */
  boolean hasNext();

  /**
   * 下一个值
   * @return next long value
   */
  long next();

}
