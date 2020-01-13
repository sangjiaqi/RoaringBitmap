package org.roaringbitmap.buffer;

import org.junit.Test;
import org.roaringbitmap.PeekableIntIterator;
import org.roaringbitmap.RoaringBitmap;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Random;

public class Test_Sang {

    @Test
    public void test() throws IOException {

        Random rand = new Random();

        // 创建MutableRoaringBitmap并写到内存
        MutableRoaringBitmap mbm1 = new MutableRoaringBitmap();
        for(int k = 0; k < 100; k++) mbm1.add((rand.nextInt() & 0xFFFF) << 16);

        MutableRoaringBitmap mbm2 = new MutableRoaringBitmap();
        for(int k = 0; k < 100; k++) mbm2.add((rand.nextInt() & 0xFFFF) << 16);

        ByteArrayOutputStream bos1 = new ByteArrayOutputStream();
        ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
        DataOutputStream dos1 = new DataOutputStream(bos1);
        DataOutputStream dos2 = new DataOutputStream(bos2);

        mbm1.serialize(dos1);
        mbm2.serialize(dos2);

        dos1.close();
        dos2.close();

        // 从内存中读出并创建ImmutableRoaringBitmap
        ByteBuffer bb1 = ByteBuffer.wrap(bos1.toByteArray());
        ByteBuffer bb2 = ByteBuffer.wrap(bos2.toByteArray());

        ImmutableRoaringBitmap imbm1 = new ImmutableRoaringBitmap(bb1);
        ImmutableRoaringBitmap imbm2 = new ImmutableRoaringBitmap(bb2);

        System.out.println(mbm1.toString());
        System.out.println(mbm2.toString());
        System.out.println(imbm1.toString());
        System.out.println(imbm2.toString());

        // 使用ImmutableRoaringBitmap计算
        System.out.println(imbm1.contains(54853632));
        PeekableIntIterator ii = imbm2.getIntIterator();
        while (ii.hasNext()) System.out.print(ii.next() + ";");
        System.out.println();

        ArrayList<Long> list1 = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            long f1 = System.currentTimeMillis();
            ImmutableRoaringBitmap.and(imbm1, imbm2);
            ImmutableRoaringBitmap.andNot(imbm1, imbm2);
            ImmutableRoaringBitmap.or(imbm1, imbm2);
            ImmutableRoaringBitmap.xor(imbm1, imbm2);
            long f2 = System.currentTimeMillis();
            list1.add((f2 - f1));
        }

        System.out.println(list1.toString());
        System.out.println("Mean1: " + mean(list1));

        // 转化为MutableRoaringBitmap计算
        MutableRoaringBitmap cmbm1 = imbm1.toMutableRoaringBitmap();
        MutableRoaringBitmap cmbm2 = imbm2.toMutableRoaringBitmap();

        System.out.println(cmbm1.contains(1));
        PeekableIntIterator ii2 = cmbm2.getIntIterator();
        while (ii2.hasNext()) System.out.print(ii2.next() + ";");
        System.out.println();

        ArrayList<Long> list2 = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            long f3 = System.currentTimeMillis();
            MutableRoaringBitmap.and(cmbm1, cmbm2);
            MutableRoaringBitmap.andNot(cmbm1, cmbm2);
            MutableRoaringBitmap.or(cmbm1, cmbm2);
            MutableRoaringBitmap.xor(cmbm1, cmbm2);
            long f4 = System.currentTimeMillis();
            list2.add((f4 - f3));
        }

        System.out.println(list2.toString());
        System.out.println("Mean2: " + mean(list2));

        // 转成RoaringBitmap计算

        RoaringBitmap cbm1 = imbm1.toRoaringBitmap();
        RoaringBitmap cbm2 = imbm2.toRoaringBitmap();

        System.out.println(cbm1.contains(1));
        PeekableIntIterator ii3 = cbm2.getIntIterator();
        while (ii3.hasNext()) System.out.print(ii3.next() + ";");
        System.out.println();

        ArrayList<Long> list3 = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            long f5 = System.currentTimeMillis();
            RoaringBitmap.and(cbm1, cbm2);
            RoaringBitmap.andNot(cbm1, cbm2);
            RoaringBitmap.or(cbm1, cbm2);
            RoaringBitmap.xor(cbm1, cbm2);
            long f6 = System.currentTimeMillis();
            list3.add((f6 - f5));
        }

        System.out.println(list3.toString());
        System.out.println("Mean3: " + mean(list3));



    }

    Double mean(ArrayList<Long> list) {
        long sum = 0;
        double count = 0;
        for (int i = 0; i < list.size(); i++) {
            sum += list.get(i);
            count += 1;
        }
        return sum / count;
    }

}