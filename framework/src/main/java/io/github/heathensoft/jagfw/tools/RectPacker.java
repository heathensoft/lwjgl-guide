package io.github.heathensoft.jagfw.tools;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Frederik Dahl 3/3/2025
 */
public class RectPacker {

    /**
     * Packs a number of rectangles inside the least possible space (power of 2)
     * @param rectangles Rectangles of format: [id | width | height]
     *               Even though the input stride is 3, the capacity of
     *               the rectangles' buffer has to accommodate for an output
     *               stride of 5: [id | width | height | x | y]
     *               So the size of the buffer should be: num rectangles * 5
     * @param bounds buffer of size 2 for the resulting pack: width and height
     */
    public static void pack(IntBuffer rectangles, IntBuffer bounds) {
        int num_rectangles = rectangles.limit() / 3;
        List<Rect> open = new ArrayList<>(num_rectangles);
        List<Rect> closed = new ArrayList<>(num_rectangles);

        for (int i = 0; i < num_rectangles; i++) {
            int offset = i * 3;
            int id = rectangles.get(offset);
            int wi = rectangles.get(offset + 1);
            int he = rectangles.get(offset + 2);
            open.add(new Rect(id,wi,he));
        } open.sort(Comparator.naturalOrder());

        float width_height_ratio = 0;
        int rect_h_max = open.get(0).h;
        int rect_h_min = open.get(num_rectangles - 1).h;
        int rect_w_min = Integer.MAX_VALUE;
        int rect_w_max = 0;
        int min_area = 0;

        for (Rect rect : open) {
            int w = rect.w;
            int h = rect.h;
            min_area += (w * h);
            width_height_ratio += ((float) w / h);
            rect_w_min = Math.min(rect_w_min,w);
            rect_w_max = Math.max(rect_w_max,w);
        } width_height_ratio /= num_rectangles;

        int pack_w_pow2, pack_h_pow2, pack_area_pow2;

        if (rect_h_max > rect_w_max) {
            pack_h_pow2 = nextPowerOfTwo(rect_h_max);
            pack_w_pow2 = pack_h_pow2;
        } else { pack_w_pow2 = nextPowerOfTwo(rect_w_max);
            pack_h_pow2 = pack_w_pow2;
        } pack_area_pow2 = pack_w_pow2 * pack_h_pow2;

        while (pack_area_pow2 < min_area) {
            if (pack_w_pow2 == pack_h_pow2) {
                if (width_height_ratio >= 1) {
                    pack_w_pow2 = nextPowerOfTwo(++pack_w_pow2);
                } else pack_h_pow2 = nextPowerOfTwo(++pack_h_pow2);
            } else {
                if (pack_w_pow2 > pack_h_pow2) {
                    pack_h_pow2 = nextPowerOfTwo(++pack_h_pow2);
                } else pack_w_pow2 = nextPowerOfTwo(++pack_w_pow2);
            } pack_area_pow2 = pack_w_pow2 * pack_h_pow2;
        }

        boolean packed = false;

        while (!packed) {

            packed = true;
            int pos_x = 0;
            int pos_y = 0;
            int row_h = 0;
            Strip gaps = null;
            Stack<Rect> rowRectangles = new Stack<>(16);

            for (Rect rect : open) {

                if (pos_x + rect.w > pack_w_pow2) { // reaching the end of a row
                    {
                        // if there is any space left behind the last rect of a row,
                        // we add the leftover space as a strip to "gaps"
                        // note: rowRectangles will never be empty atp
                        Rect last = rowRectangles.peak();
                        int remaining_w = pack_w_pow2 - (last.x + last.w);
                        if (remaining_w >= rect_w_min) {
                            Strip leftOver = new Strip(pos_x,pos_y,remaining_w,last.h);
                            gaps = gaps == null ? leftOver : gaps.addStrip(leftOver);
                        }
                    }
                    Strip currentGap = null;
                    while (!rowRectangles.isEmpty()) {
                        Rect r = rowRectangles.pop();
                        int h = row_h - r.h;
                        if (h >= rect_h_min) {
                            if (currentGap == null) {
                                int x = r.x;
                                int w = pack_w_pow2 - x;
                                int y = pos_y + r.h;
                                currentGap = new Strip(x,y,w,h);
                                gaps = gaps == null ? currentGap : gaps.addStrip(currentGap);
                            }
                            else {
                                // Expand strip (combine gaps)
                                if (currentGap.h == h) {
                                    currentGap.w += r.w;
                                    currentGap.x -= r.w;
                                }  else {
                                    int x = r.x;
                                    int w = r.w;
                                    int y = pos_y + r.h;
                                    currentGap = new Strip(x,y,w,h);
                                    gaps.addStrip(currentGap);
                                }
                            }
                        }
                        // adding directly to closed list;
                        closed.add(r);
                    }
                    // prepare for next row.
                    pos_y += row_h;
                    pos_x = 0;
                    row_h = 0;
                }
                // if able extend the packed area in the y-axis
                // else reset the process and repack
                // Repacking very rarely happens.
                if (pos_y + rect.h > pack_h_pow2) {
                    if (pack_h_pow2 < pack_w_pow2)  {
                        pack_h_pow2 = nextPowerOfTwo(pack_h_pow2 + rect.h);
                    } else if (pack_h_pow2 == pack_w_pow2) {
                        if (width_height_ratio < 1) {
                            pack_h_pow2 = nextPowerOfTwo(pack_h_pow2 + rect.h);
                        } else {
                            packed = false;
                            break;
                        }
                    }  else {
                        packed = false;
                        break;
                    }
                }
                // first try to fit in a gap
                // left over by previous rows
                if (gaps != null) {
                    if (gaps.tryAddRect(rect,closed)) {
                        continue;
                    }
                }
                if (rect.h > row_h) {
                    row_h = rect.h;
                }
                // push rectangle onto row
                rect.x = pos_x;
                rect.y = pos_y;
                pos_x += rect.w;
                rowRectangles.push(rect);
            }
            if (packed) {
                while (!rowRectangles.isEmpty()) {
                    closed.add(rowRectangles.pop());
                }
            }
            else  {
                closed.clear();
                if (pack_w_pow2 == pack_h_pow2) {
                    if (width_height_ratio >= 1) {
                        pack_w_pow2 = nextPowerOfTwo(++pack_w_pow2);
                    } else pack_h_pow2 = nextPowerOfTwo(++pack_h_pow2);
                }  else { // note: pack_w_pow2 will never be > pack_h_pow2 atp.
                    pack_w_pow2 = nextPowerOfTwo(++pack_w_pow2);
                } //pack_area_pow2 = pack_w_pow2 * pack_h_pow2;
            }
        }
        int w = 0; int h = 0;
        rectangles.clear();
        for (Rect r : closed) {
            rectangles.put(r.id);
            rectangles.put(r.w);
            rectangles.put(r.h);
            rectangles.put(r.x);
            rectangles.put(r.y);
            w = Math.max(r.x + r.w,w);
            h = Math.max(r.y + r.y,h);
        } bounds.put(nextPowerOfTwo(w)).put(nextPowerOfTwo(h)).flip();
        rectangles.flip();
    }

    private static int nextPowerOfTwo(int value) {
        if (value-- == 0) return 1;
        value |= value >>> 1;
        value |= value >>> 2;
        value |= value >>> 4;
        value |= value >>> 8;
        value |= value >>> 16;
        return value + 1;
    }

    private static abstract class RectADT {
        int x, y, w, h;
        RectADT() { /* */ }
        RectADT(int w, int h) { this(0,0,w,h); }
        RectADT(int x, int y, int w, int h) {
            this.x = x; this.y = y; this.w = w; this.h = h;
        }
    }

    private static final class Rect extends RectADT implements Comparable<Rect> {
        final int id;
        Rect(int id, int w, int h) {
            super(w, h); this.id = id;
        } public int compareTo(Rect o) {
            return Integer.compare(o.h,h);
        }
    }

    private static final class Strip extends RectADT {
        Strip next;
        int used_width;
        Strip(int x, int y, int w, int h) {
            super(x,y,w,h);
        } boolean tryAddRect(Rect rect, List<Rect> closed) {
            if (rect.h > h) return false;
            int remaining = w - used_width;
            if (rect.w <= remaining) {
                rect.x = x + used_width;
                rect.y = y;
                used_width += rect.w;
                closed.add(rect);
                return true;
            } if (next == null) return false;
            return next.tryAddRect(rect,closed);
        } Strip addStrip(Strip strip) {
            //Adds strip to sorted (by height) linked list
            //returns the new head of the linked list (the tallest strip)
            Strip head = this;
            if (strip.h > h) {
                strip.next = head;
                head = strip;
            } else { Strip next = this.next;
                this.next = strip;
                if (next != null)
                    this.next = next.addStrip(strip);
            } return head;
        } int remaining() {
            return w - used_width;
        }
    }

    @SuppressWarnings("unchecked")
    private static final class Stack<E>  {
        E[] items; int p;
        Stack(int cap) {
            this.items = (E[])new Object[Math.max(cap,1)];
        } public void push(E item) {
            if (item == null) throw new IllegalArgumentException("item == null");
            if (p == length()) { E[] tmp = items;
                items = (E[])new Object[p * 2];
                System.arraycopy(tmp,0, items,0,tmp.length);
            } items[p++] = item;
        } public E pop() {
            E item = items[--p];
            items[p] = null;
            return item;
        } public E peak() {
            return items[p - 1];
        }
        public void ensureCapacity(int size) {
            if (size > items.length) grow(size);
        }
        public void clear() {
            while (p > 0) pop();
        }
        public int size() {
            return p;
        }
        public int length() {
            return items.length;
        }
        public boolean isEmpty() {
            return size() == 0;
        }
        private void grow(int size) {
            E[] tmp = items;
            items = (E[])new Object[size];
            System.arraycopy(tmp, 0, items, 0, p);
        }
    }
}
