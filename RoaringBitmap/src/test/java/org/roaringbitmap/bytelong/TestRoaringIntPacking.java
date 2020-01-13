package org.roaringbitmap.bytelong;

import org.junit.Test;

public class TestRoaringIntPacking {

    @Test
    public void test() {

        long num = (((long) 27) << 32);
        byte high = RoaringIntPacking.high(num);
        int low = RoaringIntPacking.low(num);

        long num2 = RoaringIntPacking.pack(high, low);

        System.out.println(num);
        System.out.println(high);
        System.out.println(low);
        System.out.println(num2);

    }

}
