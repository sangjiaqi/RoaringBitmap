/*
 * (c) the authors Licensed under the Apache License, Version 2.0.
 */



/**
 * ByteLong 类型拓展包
 * The org.roaringbitmap.bytelong package  provides
 * one class ({@link org.roaringbitmap.bytelong.Roaring40NavigableMap}) that   users
 * can rely upon for fast set of 40-bit integers.
 * 
 * 
 * <pre>
 * {@code
 *      import org.roaringbitmap.bytelong.*;
 *
 *      //...
 *
 *      Roaring48NavigableMap r1 = new Roaring48NavigableMap();
 *      for(long k = 4000l; k<4255l;++k) r1.addLong(k);
 *      
 * }
 * </pre>
 *
 */
package org.roaringbitmap.bytelong;

