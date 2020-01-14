/*
 * (c) the authors Licensed under the Apache License, Version 2.0.
 */
package org.roaringbitmap.bytelong;

import java.math.BigInteger;
import java.util.Comparator;

/**
 * 这个类的作用是，切分 Long 数据类型成为 Byte 和 Integer 类型，一个座位 key 另一个加入一个 bitmap
 * Used to hold the logic packing 2 integers in a long, and separating a long in two integers. It is
 * useful in {@link Roaring40NavigableMap} as the implementation split the input long in two
 * integers, one used as key of a NavigableMap while the other is added in a Bitmap
 *
 * @author Benoit Lacelle
 *
 */
class RoaringIntPacking {

  /**
   * 返回一个 long 类型的 32 - 40 位 short 值
   * @param id any long, positive or negative
   * @return an int holding the 8 highest order bits of information of the input long
   */
  public static byte high(long id) {
    return (byte) ((id >> 32) & 0xff);
  }

  /**
   * 返回一个 long 类型的低 32 位 int 值
   * @param id any long, positive or negative
   * @return an int holding the 32 lowest order bits of information of the input long
   */
  public static int low(long id) {
    return (int) id;
  }

  /**
   * 将 byte 类型的高 18 位和 int 类型的低 32 位拼成 long 类型值
   * @param high an integer representing the highest order bits of the output long
   * @param low an integer representing the lowest order bits of the output long
   * @return a long packing together the integers as computed by
   *         {@link RoaringIntPacking#high(long)} and {@link RoaringIntPacking#low(long)}
   */
  // https://stackoverflow.com/questions/12772939/java-storing-two-ints-in-a-long
  public static long pack(byte high, int low) {
    return (((long) high) << 32) | (low & 0xffffffffL);
  }


  /**
   * 获得一个 byte 类型的高位的最大值
   * @param signedLongs true if long put in a {@link Roaring40NavigableMap} should be considered as
   *        signed long.
   * @return the int representing the highest value which can be set as high value in a
   *         {@link Roaring40NavigableMap}
   */
  public static byte highestHigh(boolean signedLongs) {
    if (signedLongs) {
      return Byte.MAX_VALUE;
    } else {
      return -1;
    }
  }

  /**
   * 一个无符号 long 的比较器
   * @return A comparator for unsigned longs: a negative long is a long greater than Long.MAX_VALUE
   */
  public static Comparator<Byte> unsignedComparator() {
    return new Comparator<Byte>() {

      @Override
      public int compare(Byte o1, Byte o2) {
        return compareUnsigned(o1, o2);
      }
    };
  }

  /**
   * 将两个 short 类型值转换为无符号值进行比较
   * Compares two {@code short} values numerically treating the values as unsigned.
   *
   * @param x the first {@code short} to compare
   * @param y the second {@code short} to compare
   * @return the value {@code 0} if {@code x == y}; a value less than {@code 0} if {@code x < y} as
   *         unsigned values; and a value greater than {@code 0} if {@code x > y} as unsigned values
   * @since 1.8
   */
  // Duplicated from jdk8 Integer.compareUnsigned
  public static int compareUnsigned(byte x, byte y) {
    return Byte.compare((byte) (x + Byte.MIN_VALUE), (byte) (y + Byte.MIN_VALUE));
  }

  /** the constant 2^40 */
  private static final BigInteger TWO_40 = BigInteger.ONE.shiftLeft(40);

  /**
   * JDK8 Long.toUnsignedString was too complex to backport. Go for a slow version relying on
   * BigInteger
   */
  // https://stackoverflow.com/questions/7031198/java-signed-long-to-unsigned-long-string
  static String toUnsignedString(long l) {
    BigInteger b = BigInteger.valueOf(l);
    if (b.signum() < 0) {
      b = b.add(TWO_40);
    }
    return b.toString();
  }
}
