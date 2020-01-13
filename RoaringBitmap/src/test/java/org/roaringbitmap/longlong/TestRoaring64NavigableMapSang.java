package org.roaringbitmap.longlong;

import org.junit.Test;
import org.roaringbitmap.RoaringBitmap;
import org.roaringbitmap.longlong.LongIterator;
import org.roaringbitmap.longlong.Roaring64NavigableMap;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class TestRoaring64NavigableMapSang {

    @Test
    public void test_and() {

        Roaring64NavigableMap left = new Roaring64NavigableMap();

        left.addLong(214748364);
        left.addLong(123);
        left.addLong(4314);

        System.out.println(left.toString());

        Roaring64NavigableMap right = new Roaring64NavigableMap();

        right.addLong(4347284);
        right.addLong(123);
        right.addLong(45423454);

        System.out.println(right.toString());

        left.and(right);

        System.out.println(left.toString());
        System.out.println(right.toString());

    }

    @Test
    public void test_or() {

        Roaring64NavigableMap left = new Roaring64NavigableMap();

        left.addLong(214748364);
        left.addLong(123);
        left.addLong(4314);

        System.out.println(left.toString());

        Roaring64NavigableMap right = new Roaring64NavigableMap();

        right.addLong(4347284);
        right.addLong(123);
        right.addLong(45423454);

        System.out.println(right.toString());

        left.or(right);

        System.out.println(left.toString());
        System.out.println(right.toString());

    }

    @Test
    public void test_anNot() {

        Roaring64NavigableMap left = new Roaring64NavigableMap();

        left.addLong(214748364);
        left.addLong(123);
        left.addLong(4314);

        System.out.println("left: " + left.toString());

        Roaring64NavigableMap right = new Roaring64NavigableMap();

        right.addLong(4347284);
        right.addLong(123);
        right.addLong(45423454);

        System.out.println("right: " + right.toString());

        left.andNot(right);

        System.out.println(left.toString());
        System.out.println(right.toString());

    }

    @Test
    public void test_xor() {

        Roaring64NavigableMap left = new Roaring64NavigableMap();

        left.addLong(214748364);
        left.addLong(123);
        left.addLong(4314);

        System.out.println("left: " + left.toString());

        Roaring64NavigableMap right = new Roaring64NavigableMap();

        right.addLong(4347284);
        right.addLong(123);
        right.addLong(45423454);

        System.out.println("right: " + right.toString());

        left.xor(right);

        System.out.println(left.toString());
        System.out.println(right.toString());

    }

    @Test
    public void test() throws IOException {

        Roaring64NavigableMap left = new Roaring64NavigableMap();

        left.addLong(214748364);
        left.addLong(123);
        left.addLong(4314);

        System.out.println("left: " + left.toString());

        Roaring64NavigableMap right = new Roaring64NavigableMap();

        right.addLong(4347284);
        right.addLong(123);
        right.addLong(45423454);

        System.out.println("right: " + right.toString());

        long longCardinality = left.getLongCardinality();

        System.out.println("longCardinality: " + longCardinality);

        LongIterator longIterator = left.getLongIterator();

        while (longIterator.hasNext()) {
            System.out.print(longIterator.next() + ";");
        }

        System.out.println("bytes: " + right.serializedSizeInBytes() + "; ");
        right.serialize(new DataOutputStream(new FileOutputStream("/Users/growingio/data")));

        System.out.println(right.select(1));
        System.out.println(right.contains(123));

    }

    @Test
    public void test_long() {

        int count = 10000;

        Random rand = new Random();
        ArrayList<Long> longs = new ArrayList<Long>();
        ArrayList<Long> ints = new ArrayList<Long>();

        ArrayList<Long> lv = new ArrayList<Long>();
        ArrayList<Long> iv = new ArrayList<Long>();

        for (int i = 0; i < 100; i++) {

            Roaring64NavigableMap left = new Roaring64NavigableMap();
            for(int k = 0; k < count; k++) left.addLong((rand.nextInt() & 0xFFFF) << 16);

            lv.add(left.getLongSizeInBytes());

            Roaring64NavigableMap right = new Roaring64NavigableMap();
            for(int k = 0; k < count; k++) right.addLong((rand.nextInt() & 0xFFFF) << 16);

            long t1 = System.currentTimeMillis();
            left.and(right);
            left.andNot(right);
            left.or(right);
            left.xor(right);
            long t2 = System.currentTimeMillis() - t1;

            longs.add(t2);

            RoaringBitmap lefts = new RoaringBitmap();
            for(int k = 0; k < count; k++) lefts.add((rand.nextInt() & 0xFFFF) << 16);

            iv.add(lefts.getLongSizeInBytes());

            RoaringBitmap rights = new RoaringBitmap();
            for(int k = 0; k < count; k++) rights.add((rand.nextInt() & 0xFFFF) << 16);

            long t3 = System.currentTimeMillis();
            lefts.and(rights);
            lefts.andNot(rights);
            lefts.or(rights);
            lefts.xor(rights);
            long t4 = System.currentTimeMillis() - t3;

            ints.add(t4);

        }

        double ltime = mean(longs);
        double itime = mean(ints);

        double lvs = mean(lv);
        double ivs = mean(iv);

        System.out.println("The time: ");
        System.out.println("The long time: " + ltime);
        System.out.println("The int time: " + itime);
        System.out.println("\r\n");

        System.out.println("The jvm: ");
        System.out.println("The long jvm: " + lvs);
        System.out.println("The int jvm: " + ivs);
        System.out.println("\r\n");

        System.out.println("The times: ");
        System.out.println(longs.toString());
        System.out.println(ints.toString());


    }

    public Double mean(ArrayList<Long> list) {

        long times = 0;
        double cnt = 0;

        for (Long l : list) {
            times += l;
            cnt += 1;
        }

        return times / cnt;
    }

    @Test
    public void testPack() {

        long num = ((long) 27 ) << 32;
        System.out.println(num);
        int high = RoaringIntPacking.high(num);
        int low = RoaringIntPacking.low(num);
        System.out.println(high);
        System.out.println(low);
        long num2 = RoaringIntPacking.pack(high, low);
        System.out.println(num2);

    }


}