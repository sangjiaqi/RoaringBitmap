package org.roaringbitmap.bytelong;

import org.junit.Before;
import org.junit.Test;
import org.roaringbitmap.shortlong.Roaring48NavigableMap;

import java.util.Random;

public class TestRoaring40NavigableMap {

    Roaring40NavigableMap left;
    Roaring40NavigableMap right;

    Roaring40NavigableMap bm1;
    Roaring40NavigableMap bm2;

    @Before
    public void prepare() {

        int n = 100;
        Random rand = new Random();

        left = new Roaring40NavigableMap();
        for (int i = 0; i < n; i++) left.addLong(((long) (rand.nextInt() & 0xFF)) << 32);

        right = new Roaring40NavigableMap();
        for (int i = 0; i < n; i++) right.addLong(((long)(rand.nextInt() & 0xFF)) << 32);

        bm1 = new Roaring40NavigableMap();
        bm1.addInt(1); bm1.addInt(3); bm1.add(5);

        bm2 = new Roaring40NavigableMap();
        bm2.addInt(1); bm2.addInt(2); bm2.add(5);

    }

    @Test
    public void test1() {
        System.out.println(left.toString());
        System.out.println(right.toString());
        bm1.and(bm2);
        System.out.println(bm2);
    }

}
