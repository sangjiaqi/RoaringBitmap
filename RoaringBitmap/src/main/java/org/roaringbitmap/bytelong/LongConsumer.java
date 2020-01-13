/*
 * (c) the authors Licensed under the Apache License, Version 2.0.
 */
package org.roaringbitmap.bytelong;

/**
 * An LongConsumer receives the long values contained in a data structure. Each value is visited
 * once.
 * 
 * Usage:
 * 
 * <pre>
 * {@code
 *  bitmap.forEach(new LongConsumer() {
 *
 *    public void accept(long value) {
 *      // do something here
 *      
 *    }});
 *   }
 * }
 * </pre>
 */
public interface LongConsumer {
  /**
   * 接受 Long 类型值并进行操作
   * Receives the long
   * 
   * @param value the long value
   */
  void accept(long value);
}
