package org.roaringbitmap.shortlong;

import org.roaringbitmap.BitmapDataProvider;
import org.roaringbitmap.BitmapDataProviderSupplier;
import org.roaringbitmap.IntConsumer;
import org.roaringbitmap.IntIterator;
import org.roaringbitmap.RoaringBitmap;
import org.roaringbitmap.RoaringBitmapSupplier;
import org.roaringbitmap.Util;
import org.roaringbitmap.buffer.MutableRoaringBitmap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

public class Roaring48NavigableMap implements Externalizable, LongBitmapDataProvider {

    // 定义 ShortInteger 类型的储存结构
    private NavigableMap<Short, BitmapDataProvider> highToBitmap;

    // 是否为符号 Long 类型
    private boolean signedLongs = false;

    // 声明 bitmap 的供应
    private BitmapDataProviderSupplier supplier;

    // 默认情况下，缓存基数
    private transient boolean doCacheCardinalities = true;

    // 取出高位（当连续请求排名时，防止重新计算所有的基数）
    private transient short firstHighNotValid = (short) (highestHigh() + 1);

    // 标识积累的基数是否全部有效
    private transient boolean allValid = false;

    // 对累计的基数进行排序
    private transient long[] sortedCumlatedCardinality = new long[0];

    // 对高位进行排序
    private transient short[] sortedHighs = new short[0];

    // 最新的储存值
    private transient Map.Entry<Short, BitmapDataProvider> latestAddedHigh = null;

    // 定义排序是否有符号
    private static final boolean DEFAULT_ORDER_IS_SIGNED = false;

    // 定义基数是否缓存
    private static final boolean DEFAULT_CARDINALITIES_ARE_CACHED = true;

    public Roaring48NavigableMap() { this(DEFAULT_ORDER_IS_SIGNED); }

    public Roaring48NavigableMap(boolean signedLongs) { this(signedLongs, DEFAULT_CARDINALITIES_ARE_CACHED); }

    public Roaring48NavigableMap(boolean signedLongs, boolean cacheCardinalities) {
        this(signedLongs, cacheCardinalities, new RoaringBitmapSupplier());
    }

    public Roaring48NavigableMap(BitmapDataProviderSupplier supplier) {
        this(DEFAULT_ORDER_IS_SIGNED, DEFAULT_CARDINALITIES_ARE_CACHED, supplier);
    }

    public Roaring48NavigableMap(boolean signedLongs, BitmapDataProviderSupplier supplier) {
        this(signedLongs, DEFAULT_CARDINALITIES_ARE_CACHED, supplier);
    }

    public Roaring48NavigableMap(boolean signedLongs, boolean cacheCardinalities,
                                 BitmapDataProviderSupplier supplier) {
        this.signedLongs = signedLongs;
        this.supplier = supplier;

        if (signedLongs) {
            highToBitmap = new TreeMap<>();
        } else {
            highToBitmap = new TreeMap<>(RoaringIntPacking.unsignedComparator());
        }

        this.doCacheCardinalities = cacheCardinalities;
        resetPerfHelpers();
    }

    /**
     * 初始化结构
     */
    private void resetPerfHelpers() {
        firstHighNotValid = (short) (RoaringIntPacking.highestHigh(signedLongs) + 1);
        allValid = false;

        sortedCumlatedCardinality = new long[0];
        sortedHighs = new short[0];

        latestAddedHigh = null;
    }

    /**
     * 取出 NavigableMap 对象
     * @return NavigableMap 对象
     */
    NavigableMap<Short, BitmapDataProvider> getHighToBitmap() { return highToBitmap; }

    /**
     * 取出高位
     * @return 高 16 位
     */
    short getLowestInvalidHigh() { return firstHighNotValid; }

    /**
     * 取出排序后全部基数
     * @return 排序的基数数组
     */
    long[] getSortedCumlatedCardinality() { return sortedCumlatedCardinality; }

    /**
     * 添加 integer 类型值到容器中，不论其是否已经出现过
     * @param x
     */
    public void addInt(int x) { addLong(Util.toUnsignedLong(x)); }

    /**
     * 返回添加到位图的不同整数的 int 数量
     * @return
     *
    public int getIntCardinality() {
        long cardinality = getLongCardinality();

        if (cardinality > Integer.MAX_VALUE) {
            throw new UnsupportedOperationException("Can not call .getIntCardinality as the cardinality is bigger than Integer.MAX_VALUE");
        }

        return (int) cardinality;
    }

    /**
     * 返回 Long 类型的迭代器（该方法效率比 forEach 差）
     * @return
     */
    public Iterator<Long> iterator() {
        final LongIterator it = getLongIterator();

        return new Iterator<Long>() {

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Long next() {
                return it.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * 按位进行 OR 操作（并集）。当前位图会被修改
     * @param x2
     */
    public void or(final Roaring48NavigableMap x2) {
        boolean firstBucket = true;

        for (Map.Entry<Short, BitmapDataProvider> e2 : x2.highToBitmap.entrySet()) {
            Short high = e2.getKey();

            BitmapDataProvider lowBitmap1 = this.highToBitmap.get(high);

            BitmapDataProvider lowBitmap2 = e2.getValue();

            if ((lowBitmap1 == null || lowBitmap1 instanceof RoaringBitmap)
                    && lowBitmap2 instanceof RoaringBitmap) {
                if (lowBitmap1 == null) {
                    RoaringBitmap lowBitmap2Clone = ((RoaringBitmap) lowBitmap2).clone();
                    pushBitmapForHigh(high, lowBitmap2Clone);
                } else {
                    ((RoaringBitmap) lowBitmap1).or((RoaringBitmap) lowBitmap2);
                }
            } else if ((lowBitmap1 == null || lowBitmap1 instanceof MutableRoaringBitmap)
                    && lowBitmap2 instanceof MutableRoaringBitmap) {
                if (lowBitmap1 == null) {
                    BitmapDataProvider lowBitmap2Clone = ((MutableRoaringBitmap) lowBitmap2).clone();
                    pushBitmapForHigh(high, lowBitmap2Clone);
                } else {
                    ((MutableRoaringBitmap) lowBitmap1).or((MutableRoaringBitmap) lowBitmap2);
                }
            } else {
                throw new UnsupportedOperationException(
                        ".or is not between " + this.getClass() + " and " + lowBitmap2.getClass());
            }

            if (firstBucket) {
                firstBucket = false;

                firstHighNotValid = (short) Math.min(firstHighNotValid, high);
                allValid = false;
            }
        }
    }

    /**
     * 按位进行 XOR 操作（对称差集）。当前位图会被修改
     * @param x2
     */
    public void xor(final Roaring48NavigableMap x2) {
        boolean firstBucket = true;

        for (Map.Entry<Short, BitmapDataProvider> e2 : x2.highToBitmap.entrySet()) {
            Short high = e2.getKey();

            BitmapDataProvider lowBitmap1 = this.highToBitmap.get(high);

            BitmapDataProvider lowBitmap2 = e2.getValue();

            if ((lowBitmap1 == null || lowBitmap1 instanceof RoaringBitmap)
                    && lowBitmap2 instanceof RoaringBitmap) {
                if (lowBitmap1 == null) {
                    RoaringBitmap lowBitmap2Clone = ((RoaringBitmap) lowBitmap2).clone();
                    pushBitmapForHigh(high, lowBitmap2Clone);
                } else {
                    ((RoaringBitmap) lowBitmap1).xor((RoaringBitmap) lowBitmap2);
                }
            } else if ((lowBitmap1 == null || lowBitmap1 instanceof MutableRoaringBitmap)
                    && lowBitmap2 instanceof MutableRoaringBitmap) {
                if (lowBitmap1 == null) {
                    BitmapDataProvider lowBitmap2Clone = ((MutableRoaringBitmap) lowBitmap2).clone();
                    pushBitmapForHigh(high, lowBitmap2Clone);
                } else {
                    ((MutableRoaringBitmap) lowBitmap1).xor((MutableRoaringBitmap) lowBitmap2);
                }
            } else {
                throw new UnsupportedOperationException(
                        ".or is not between " + this.getClass() + " and " + lowBitmap2.getClass());
            }

            if (firstBucket) {
                firstBucket = false;

                firstHighNotValid = (short) Math.min(firstHighNotValid, high);
                allValid = false;
            }
        }
    }

    /**
     * 按位进行 AND 操作（交集）。当前位图被修改
     * @param x2
     */
    public void and(final Roaring48NavigableMap x2) {
        boolean firstBucket = true;

        Iterator<Map.Entry<Short, BitmapDataProvider>> thisIterator = highToBitmap.entrySet().iterator();
        while (thisIterator.hasNext()) {
            Map.Entry<Short, BitmapDataProvider> e1 = thisIterator.next();

            Short high = e1.getKey();
            BitmapDataProvider lowBitmap2 = x2.highToBitmap.get(high);

            if (lowBitmap2 == null) {
                thisIterator.remove();
            } else {
                BitmapDataProvider lowBitmap1 = e1.getValue();

                if (lowBitmap2 instanceof RoaringBitmap && lowBitmap1 instanceof RoaringBitmap) {
                    ((RoaringBitmap) lowBitmap1).and((RoaringBitmap) lowBitmap2);
                } else if (lowBitmap2 instanceof MutableRoaringBitmap && lowBitmap1 instanceof MutableRoaringBitmap) {
                    ((MutableRoaringBitmap) lowBitmap1).and((MutableRoaringBitmap) lowBitmap2);
                } else {
                    throw new UnsupportedOperationException(
                            ".or is not between " + this.getClass() + " and " + lowBitmap2.getClass());
                }
            }

            if (firstBucket) {
                firstBucket = false;

                firstHighNotValid = (short) Math.min(firstHighNotValid, high);
                allValid = false;
            }
        }

    }

    /**
     * 按位进行 ANDNOT 操作（差集）。当前位图被修改
     * @param x2
     */
    public void andNot(final Roaring48NavigableMap x2) {
        boolean firstBucket = true;

        Iterator<Map.Entry<Short, BitmapDataProvider>> thisIterator = highToBitmap.entrySet().iterator();
        while (thisIterator.hasNext()) {
            Map.Entry<Short, BitmapDataProvider> e1 = thisIterator.next();

            Short high = e1.getKey();
            BitmapDataProvider lowBitmap2 = x2.highToBitmap.get(high);

            if (lowBitmap2 != null) {
                BitmapDataProvider lowBitmap1 = e1.getValue();

                if (lowBitmap2 instanceof RoaringBitmap && lowBitmap1 instanceof RoaringBitmap) {
                    ((RoaringBitmap) lowBitmap1).and((RoaringBitmap) lowBitmap2);
                } else if (lowBitmap2 instanceof MutableRoaringBitmap && lowBitmap1 instanceof MutableRoaringBitmap) {
                    ((MutableRoaringBitmap) lowBitmap1).and((MutableRoaringBitmap) lowBitmap2);
                } else {
                    throw new UnsupportedOperationException(
                            ".or is not between " + this.getClass() + " and " + lowBitmap2.getClass());
                }
            }

            if (firstBucket) {
                firstBucket = false;

                firstHighNotValid = (short) Math.min(firstHighNotValid, high);
                allValid = false;
            }
        }

    }

    /**
     * 重写 toString 方法
     * @return
     */
    @Override
    public String toString() {
        final StringBuilder answer = new StringBuilder();
        final LongIterator i = this.getLongIterator();

        answer.append("{");
        if (i.hasNext()) {
            if (signedLongs) {
                answer.append(i.next());
            } else {
                answer.append(RoaringIntPacking.toUnsignedString(i.next()));
            }
        }
        while (i.hasNext()) {
            answer.append(",");
            if (answer.length() > 0x80000) {
                answer.append("...");
                break;
            }
            if (signedLongs) {
                answer.append(i.next());
            } else {
                answer.append(RoaringIntPacking.toUnsignedString(i.next()));
            }
        }
        answer.append("}");
        return answer.toString();
    }

    /**
     * 使用估计更节省空间的运行长度编码
     * @return
     */
    public boolean runOptimize() {
        boolean hasChanged = false;
        for (BitmapDataProvider lowBitmap : highToBitmap.values()) {
            if (lowBitmap instanceof RoaringBitmap) {
                hasChanged |= ((RoaringBitmap) lowBitmap).runOptimize();
            } else if (lowBitmap instanceof MutableRoaringBitmap) {
                hasChanged |= ((MutableRoaringBitmap) lowBitmap).runOptimize();
            }
        }
        return hasChanged;
    }

    /**
     * 反序列化 bitmap
     * @param in
     * @throws IOException
     */
    public void deserialize(DataInput in) throws IOException {
        this.clear();

        signedLongs = in.readBoolean();

        int nbHighs = in.readInt();

        if (signedLongs) {
            highToBitmap = new TreeMap<>();
        } else {
            highToBitmap = new TreeMap<>(RoaringIntPacking.unsignedComparator());
        }

        for (int i = 0; i < nbHighs; i++) {
            short high = in.readShort();
            RoaringBitmap provider = new RoaringBitmap();
            provider.deserialize(in);

            highToBitmap.put(high, provider);
        }

        resetPerfHelpers();
    }

    /**
     * 重新设置为空 bitmap
     */
    public void clear() {
        this.highToBitmap.clear();
        resetPerfHelpers();
    }

    /**
     * 多个 long 类型值生成 bitmap
     * @param dat
     * @return
     */
    public static Roaring48NavigableMap bitmapOf(final long... dat) {
        final Roaring48NavigableMap ans = new Roaring48NavigableMap();
        ans.add(dat);
        return ans;
    }

    /**
     * 添加多个 long 类型的值
     * @param dat
     */
    public void add(long... dat) {
        for (long oneLong : dat) {
            addLong(oneLong);
        }
    }

    /**
     * 添加 [rangeStart, rangeEnd) 中的所有值到 bitmap 中
     * @param rangeStart
     * @param rangeEnd
     */
    public void add(final long rangeStart, final long rangeEnd) {
        short startHigh = high(rangeStart);
        int startLow = low(rangeStart);

        short endHigh = high(rangeEnd);
        int endLow = low(rangeEnd);

        for (int high = startHigh; high <= endHigh; high++) {
            final int currentStartLow;
            if (startHigh == (short) high) {
                currentStartLow = startLow;
            } else {
                currentStartLow = 0;
            }

            long startLowAsLong = Util.toUnsignedLong(currentStartLow);

            final long endLowAsLong;
            if (endHigh == (short) high) {
                endLowAsLong = Util.toUnsignedLong(endLow);
            } else {
                endLowAsLong = Util.toUnsignedLong(-1) + 1;
            }

            if (endLowAsLong > startLowAsLong) {
                BitmapDataProvider bitmap = highToBitmap.get((short) high);
                if (bitmap == null) {
                    bitmap = new MutableRoaringBitmap();
                    pushBitmapForHigh((short) high, bitmap);
                }

                if (bitmap instanceof RoaringBitmap) {
                    ((RoaringBitmap) bitmap).add(startLowAsLong, endLowAsLong);
                } else if (bitmap instanceof MutableRoaringBitmap) {
                    ((MutableRoaringBitmap) bitmap).add(startLowAsLong, endLowAsLong);
                } else {
                    throw new UnsupportedOperationException("TODO. Not for " + bitmap.getClass());
                }
            }
        }

        invalidateAboveHigh(startHigh);
    }

    /**
     * 如果之前不存在则添加值，否则移除它
     * @param x
     */
    public void flip (final long x) {
        short high = RoaringIntPacking.high(x);
        BitmapDataProvider lowBitmap = highToBitmap.get(high);
        if (lowBitmap == null) {
            addLong(x);
        } else {
            int low = RoaringIntPacking.low(x);

            if (lowBitmap instanceof RoaringBitmap) {
                ((RoaringBitmap) lowBitmap).flip(low);
            } else if (lowBitmap instanceof MutableRoaringBitmap) {
                ((MutableRoaringBitmap) lowBitmap).flip(low);
            } else {
                if (lowBitmap.contains(low)) lowBitmap.remove(low);
                else lowBitmap.add(low);
            }
        }

        invalidateAboveHigh(high);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        serialize(out);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        deserialize(in);
    }

    @Override
    public void addLong(long x) {

        long num = 1L;

        if (x > (num << 48)) throw new IllegalArgumentException("The param is large than 48 bit");

        short high = high(x);
        int low = low(x);

        Map.Entry<Short, BitmapDataProvider> local = latestAddedHigh;

        BitmapDataProvider bitmap;
        if (local != null && local.getKey().shortValue() == high) {
            bitmap = local.getValue();
        } else {
            bitmap = highToBitmap.get(high);
            if (bitmap == null) {
                bitmap = newRoaringBitmap();
                pushBitmapForHigh(high, bitmap);
            }
            latestAddedHigh = new AbstractMap.SimpleImmutableEntry<>(high, bitmap);
        }
        bitmap.add(low);

        invalidateAboveHigh(high);
    }

    @Override
    public void removeLong(long x) {
        short high = high(x);

        BitmapDataProvider bitmap = highToBitmap.get(high);

        if (bitmap != null) {
            int low = low(x);
            bitmap.remove(low);

            invalidateAboveHigh(high);
        }
    }

    @Override
    public void trim() {
        for (BitmapDataProvider bitmap : highToBitmap.values()) {
            bitmap.trim();
        }
    }

    @Override
    public boolean contains(long x) {
        short high = RoaringIntPacking.high(x);
        BitmapDataProvider lowBitmap = highToBitmap.get(high);
        if (lowBitmap == null) {
            return false;
        }

        int low = RoaringIntPacking.low(x);
        return lowBitmap.contains(low);
    }

    @Override
    public long getLongCardinality() {
        if (doCacheCardinalities) {
            if (highToBitmap.isEmpty()) {
                return 0L;
            }
            int indexOk = ensureCumulatives(highestHigh());

            if (highToBitmap.isEmpty()) {
                return 0L;
            }

            return sortedCumlatedCardinality[indexOk - 1];
        } else {
            long cardinality = 0L;
            for (BitmapDataProvider bitmap : highToBitmap.values()) {
                cardinality += bitmap.getLongCardinality();
            }
            return cardinality;
        }
    }

    @Override
    public void forEach(LongConsumer lc) {
        for (final Map.Entry<Short, BitmapDataProvider> highEntry : highToBitmap.entrySet()) {
            highEntry.getValue().forEach(new IntConsumer() {
                @Override
                public void accept(int low) {
                    lc.accept(RoaringIntPacking.pack(highEntry.getKey(), low));
                }
            });
        }
    }

    @Override
    public LongIterator getLongIterator() {
        final Iterator<Map.Entry<Short, BitmapDataProvider>> it = highToBitmap.entrySet().iterator();

        return toIterator(it, false);
    }

    @Override
    public LongIterator getReverseLongIterator() {
        return toIterator(highToBitmap.descendingMap().entrySet().iterator(), true);
    }

    @Override
    public int getSizeInBytes() {
        return (int) getLongSizeInBytes();
    }

    @Override
    public long getLongSizeInBytes() {
        long size = 8;

        size += highToBitmap.values().stream().mapToLong(p -> p.getLongSizeInBytes()).sum();

        size += 8 + 40 * highToBitmap.size();

        size += 16 * highToBitmap.size();

        size += 8 * sortedCumlatedCardinality.length;
        size += 4 * sortedHighs.length;

        return size;
    }

    @Override
    public boolean isEmpty() {
        return getLongCardinality() == 0L;
    }

    @Override
    public ImmutableLongBitmapDataProvider limit(long x) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public long rankLong(long id) {
        short high = RoaringIntPacking.high(id);
        int low = RoaringIntPacking.low(id);

        if (!doCacheCardinalities) return rankLongNoCache(high, low);

        int indexOk = ensureCumulatives(high);

        int highPosition = binarySearch(sortedHighs, 0, indexOk, high);

        if (highPosition >= 0) {
            final long previousBucketCardinality;
            if (highPosition == 0) previousBucketCardinality = 0;
            else previousBucketCardinality = sortedCumlatedCardinality[highPosition - 1];

            BitmapDataProvider lowBitmap = highToBitmap.get(sortedHighs[highPosition]);

            return previousBucketCardinality + lowBitmap.rankLong(low);
        } else {
            int insertionPoint = -highPosition - 1;

            if (insertionPoint == 0) return 0;
            else return sortedCumlatedCardinality[insertionPoint - 1];
        }
    }

    @Override
    public long select(long j) {
        if (!doCacheCardinalities) {
            return selectNoCache(j);
        }

        int indexOk = ensureCumulatives(highestHigh());

        if (highToBitmap.isEmpty()) {
            return throwSelectInvalidIndex(j);
        }

        int position = Arrays.binarySearch(sortedCumlatedCardinality, 0, indexOk, j);

        if (position >= 0) {
            if (position == indexOk - 1) {
                return throwSelectInvalidIndex(j);
            }

            short high = sortedHighs[position + 1];
            BitmapDataProvider nextBitmap = highToBitmap.get(high);
            return RoaringIntPacking.pack(high, nextBitmap.select(0));
        } else {
            int insertionPoint = -position - 1;

            final long previousBucketCardinality;
            if (insertionPoint == 0) {
                previousBucketCardinality = 0L;
            } else if (insertionPoint >= indexOk) {
                return throwSelectInvalidIndex(j);
            } else {
                previousBucketCardinality = sortedCumlatedCardinality[insertionPoint - 1];
            }

            final int givenBitmapSelect = (int) (j - previousBucketCardinality);

            short high = sortedHighs[insertionPoint];
            BitmapDataProvider lowBitmap = highToBitmap.get(high);
            int low = lowBitmap.select(givenBitmapSelect);

            return RoaringIntPacking.pack(high, low);
        }

    }

    @Override
    public void serialize(DataOutput out) throws IOException {
        out.writeBoolean(signedLongs);

        out.writeInt(highToBitmap.size());

        for (Map.Entry<Short, BitmapDataProvider> entry : highToBitmap.entrySet()) {
            out.writeShort(entry.getKey().shortValue());
            entry.getValue().serialize(out);
        }
    }

    @Override
    public long serializedSizeInBytes() {
        long nbBytes = 0L;

        nbBytes += 1;
        nbBytes += 4;

        for (Map.Entry<Short, BitmapDataProvider> entry : highToBitmap.entrySet()) {
            nbBytes += 4;
            nbBytes += entry.getValue().serializedSizeInBytes();
        }

        return nbBytes;
    }

    @Override
    public long[] toArray() {
        long cardinality = this.getLongCardinality();
        if (cardinality > Integer.MAX_VALUE) {
            throw new IllegalStateException("The cardinality does not fit in an array");
        }

        final long[] array = new long[(int) cardinality];

        int pos = 0;
        org.roaringbitmap.shortlong.LongIterator it = getLongIterator();

        while (it.hasNext()) {
            array[pos++] = it.next();
        }
        return array;
    }

    @Override
    public int hashCode() {
        return highToBitmap.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Roaring48NavigableMap other = (Roaring48NavigableMap) obj;
        return Objects.equals(highToBitmap, other.highToBitmap);
    }

    /**
     * 将 Map 的迭代器变换为自定义的迭代器
     * @param it
     * @param reversed
     * @return
     */
    protected LongIterator toIterator(final Iterator<Map.Entry<Short, BitmapDataProvider>> it, final boolean reversed) {
        return new LongIterator() {
            protected short currentKey;
            protected IntIterator currentIt;

            @Override
            public LongIterator clone() {
                throw new UnsupportedOperationException("TODO");
            }

            @Override
            public boolean hasNext() {
                if (currentIt == null) {
                    if (!moveToNextEntry(it)) {
                        return false;
                    }
                }

                while (true) {
                    if (currentIt.hasNext()) {
                        return true;
                    } else {
                        if (!moveToNextEntry(it)) {
                            return false;
                        }
                    }
                }
            }

            private boolean moveToNextEntry(Iterator<Map.Entry<Short, BitmapDataProvider>> it) {
                if (it.hasNext()) {
                    Map.Entry<Short, BitmapDataProvider> next = it.next();
                    currentKey = next.getKey();
                    if (reversed) {
                        currentIt = next.getValue().getReverseIntIterator();
                    } else {
                        currentIt = next.getValue().getIntIterator();
                    }

                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public long next() {
                if (hasNext()) {
                    return RoaringIntPacking.pack(currentKey, currentIt.next());
                } else {
                    throw new IllegalStateException("empty");
                }
            }
        };
    }

    /**
     * 寻找最大的有效位置索引
     * @param high
     * @return
     */
    protected int ensureCumulatives(short high) {
        if (allValid) {
            return highToBitmap.size();
        } else if (compare(high, firstHighNotValid) < 0) {
            int position = binarySearch(sortedHighs, high);

            if (position >= 0) {
                return position + 1;
            } else {
                int insertionPosition = -position - 1;
                return insertionPosition;
            }
        } else {
            SortedMap<Short, BitmapDataProvider> tailMap =
                highToBitmap.tailMap(firstHighNotValid, true);

            int indexOk = highToBitmap.size() - tailMap.size();

            Iterator<Map.Entry<Short, BitmapDataProvider>> it = tailMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Short, BitmapDataProvider> e = it.next();
                short currentHigh = e.getKey();

                if (compare(currentHigh, high) > 0) {
                    break;
                } else if (e.getValue().isEmpty()) {
                    if (latestAddedHigh != null && latestAddedHigh.getKey().shortValue() == currentHigh) {
                        latestAddedHigh = null;
                    }
                    it.remove();
                } else {
                    ensureOne(e, currentHigh, indexOk);

                    indexOk++;
                }
            }

            if (highToBitmap.isEmpty() || indexOk == highToBitmap.size()) allValid = true;

            return indexOk;
        }
    }

    /**
     * 返回 short 类型的高位最大值
     * @return
     */
    private short highestHigh() {
        return RoaringIntPacking.highestHigh(signedLongs);
    }

    /**
     * 返回一个 long 类型的高 16 位 short 值
     * @param id
     * @return 16 位 short 值
     */
    private short high(long id) {
        return RoaringIntPacking.high(id);
    }

    /**
     * 返回一个 long 类型的低 32 位 int 值
     * @param id
     * @return 32 位 int 值
     */
    private int low(long id) {
        return RoaringIntPacking.low(id);
    }

    /**
     * 返回一个空的 BitmapDataProvider
     * @return
     */
    private BitmapDataProvider newRoaringBitmap() {
        return supplier.newEmpty();
    }

    /**
     * 通过高位添加低位的 bitmap
     * @param high
     * @param bitmap
     */
    private void pushBitmapForHigh(short high, BitmapDataProvider bitmap) {
        BitmapDataProvider previous = highToBitmap.put(high, bitmap);
        assert previous == null : "should push only not-existing high";
    }

    private void invalidateAboveHigh(short high) {
        if (compare(firstHighNotValid, high) > 0) {
            firstHighNotValid = high;
            int indexNotValid = binarySearch(sortedHighs, firstHighNotValid);

            final int indexAfterWhichToReset;
            if (indexNotValid >= 0) {
                indexAfterWhichToReset = indexNotValid;
            } else {
                indexAfterWhichToReset = -indexNotValid - 1;
            }

            Arrays.fill(sortedHighs, indexAfterWhichToReset, sortedHighs.length, highestHigh());
        }
        allValid = false;
    }

    /**
     * 有无符号的 short 类型大小比较
     * @param x
     * @param y
     * @return
     */
    private int compare(short x, short y) {
        if (signedLongs) {
            return Short.compare(x, y);
        } else {
            return RoaringIntPacking.compareUnsigned(x, y);
        }
    }

    /**
     * 根据有无符号类型进行二分查找
     * @param array
     * @param key
     * @return
     */
    private int binarySearch(short[] array, short key) {
        if (signedLongs) {
            return Arrays.binarySearch(array, key);
        } else {
            return unsignedBinarySearch(array, 0, array.length, key,
                    RoaringIntPacking.unsignedComparator());
        }
    }

    /**
     * 根据有无符号类型的限制范围的二分查找
     * @param array
     * @param from
     * @param to
     * @param key
     * @return
     */
    private int binarySearch(short[] array, int from, int to, short key) {
        if (signedLongs) {
            return Arrays.binarySearch(array, from , to, key);
        } else {
            return unsignedBinarySearch(array, from, to, key, RoaringIntPacking.unsignedComparator());
        }
    }

    /**
     * 符号类型自定义 Comparator 的二分查找
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param key
     * @param c
     * @return
     */
    private static int unsignedBinarySearch(short[] a, int fromIndex, int toIndex, short key,
                                            Comparator<? super Short> c) {
        int low = fromIndex;
        int high = toIndex - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            short midVal = a[mid];
            int cmp = c.compare(midVal, key);
            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return mid;
            }
        }

        return -(low + 1);
    }

    /**
     * 抛出选择有效索引异常
     * @param j
     * @return
     */
    private long throwSelectInvalidIndex(long j) {
        throw new IllegalArgumentException(
                "select " + j + " when the cardinality is " + this.getLongCardinality());
    }

    /**
     * 非缓存查找
     * @param j
     * @return
     */
    private long selectNoCache(long j) {
        long left = j;

        for (Map.Entry<Short, BitmapDataProvider> entry : highToBitmap.entrySet()) {
            long lowCardinality = entry.getValue().getCardinality();

            if (left >= lowCardinality) {
                left -= lowCardinality;
            } else {
                int leftAsUnsignedInt = (int) left;
                return RoaringIntPacking.pack(entry.getKey(), entry.getValue().select(leftAsUnsignedInt));
            }
        }

        return throwSelectInvalidIndex(j);
    }

    /**
     * 非缓存对数值进行排序
     * @param high
     * @param low
     * @return
     */
    private long rankLongNoCache(short high, int low) {
        long result = 0L;

        BitmapDataProvider lastBitmap = highToBitmap.get(high);
        if (lastBitmap == null) {
            if (lastBitmap == null) {
                for (Map.Entry<Short, BitmapDataProvider> bitmap : highToBitmap.entrySet()) {
                    if (bitmap.getKey().shortValue() > high) break;
                    else result += bitmap.getValue().getLongCardinality();
                }
            }
        } else {
            for (BitmapDataProvider bitmap : highToBitmap.values()) {
                if (bitmap == lastBitmap) {
                    result += bitmap.rankLong(low);
                    break;
                } else {
                    result += bitmap.getLongCardinality();
                }
            }
        }

        return result;
    }

    /**
     *
     * @param e
     * @param currentHigh
     * @param indexOk
     */
    private void ensureOne(Map.Entry<Short, BitmapDataProvider> e, short currentHigh, int indexOk) {
        assert indexOk <= sortedHighs.length : indexOk + " is bigger than " + sortedHighs.length;

        final int index;
        if (indexOk == 0) {
            if (sortedHighs.length == 0) index = -1;
            else index = -1;
        } else if (indexOk < sortedHighs.length) {
            index = -indexOk - 1;
        } else {
            index = -sortedHighs.length - 1;
        }

        assert index == binarySearch(sortedHighs, 0, indexOk, currentHigh) : "Computed " + index
                + " differs from dummy binary-search index: "
                + binarySearch(sortedHighs, 0, indexOk, currentHigh);

        if (index >= 0) {
            throw new IllegalStateException("Unexpectedly found " + currentHigh + " in "
                    + Arrays.toString(sortedHighs) + " strictly before index" + indexOk);
        } else {
            int insertionPosition = -index - 1;

            if (insertionPosition >= sortedHighs.length) {
                int previousSize = sortedHighs.length;

                int newSize = Math.min(Integer.MAX_VALUE, sortedHighs.length * 2 + 1);

                sortedHighs = Arrays.copyOf(sortedHighs, newSize);
                sortedCumlatedCardinality = Arrays.copyOf(sortedCumlatedCardinality, newSize);

                Arrays.fill(sortedHighs, previousSize, sortedHighs.length, highestHigh());
                Arrays.fill(sortedCumlatedCardinality, previousSize, sortedHighs.length, Long.MAX_VALUE);
            }
            sortedHighs[insertionPosition] = currentHigh;

            final long previousCardinality;
            if (insertionPosition >= 1) {
                previousCardinality = sortedCumlatedCardinality[insertionPosition - 1];
            } else {
                previousCardinality = 0;
            }

            sortedCumlatedCardinality[insertionPosition] =
                    previousCardinality + e.getValue().getLongCardinality();

            if (currentHigh == highestHigh()) firstHighNotValid = currentHigh;
            else firstHighNotValid = (short) (currentHigh + 1);
        }
    }

}
