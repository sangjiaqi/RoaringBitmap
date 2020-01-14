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
        bm1.addInt(1); bm1.addInt(3); bm1.addInt(5); bm1.addLong(12884901888L);

        bm2 = new Roaring40NavigableMap();
        bm2.addInt(1); bm2.addInt(2); bm2.addInt(5); bm2.addLong(77309411328L);

    }

    @Test
    public void test1() {
        System.out.println(left.toString());
        System.out.println(right.toString());

        left.and(right);
        left.or(right);
        left.andNot(right);
        left.xor(right);

        System.out.println(left);
        System.out.println(right);

        System.out.println(bm1);
        System.out.println(bm2);

    }

}
