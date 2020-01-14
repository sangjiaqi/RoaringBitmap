package org.roaringbitmap.shortlong;

import org.junit.Before;
import org.junit.Test;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.util.Random;

public class TestRoaring48NavigableMap {

    Roaring48NavigableMap left;
    Roaring48NavigableMap right;

    Roaring48NavigableMap bm1;
    Roaring48NavigableMap bm2;

    @Before
    public void prepare() {

        int n = 100;
        Random rand = new Random();

        left = new Roaring48NavigableMap();
        for (int i = 0; i < n; i++) left.addLong(((long) (rand.nextInt() & 0xFFFF)) << 32);

        right = new Roaring48NavigableMap();
        for (int i = 0; i < n; i++) right.addLong(((long)(rand.nextInt() & 0xFFFF)) << 32);

        bm1 = new Roaring48NavigableMap();
        bm1.addInt(1); bm1.addInt(3); bm1.add(5);

        bm2 = new Roaring48NavigableMap();
        bm2.addInt(1); bm2.addInt(2); bm2.add(5); bm1.add(1L << 42);

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


    }

}
