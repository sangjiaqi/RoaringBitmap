/*
 * (c) the authors Licensed under the Apache License, Version 2.0.
 */



/**
 * ShortLong 类型拓展包
 * The org.roaringbitmap.shortlong package  provides
 * one class ({@link org.roaringbitmap.shortlong.Roaring48NavigableMap}) that   users
 * can rely upon for fast set of 48-bit integers.
 * 
 * 
 * <pre>
 * {@code
 *      import org.roaringbitmap.shortlong.*;
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
package org.roaringbitmap.shortlong;

