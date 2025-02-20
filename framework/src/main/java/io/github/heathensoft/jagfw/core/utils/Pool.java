package io.github.heathensoft.jagfw.core.utils;

import io.github.heathensoft.jagfw.core.Disposable;

/**
 * Frederik Dahl 2/16/2025
 */
public abstract class Pool<I> implements Disposable {

    protected I[] items;
    protected int front;
    protected int rear;
    protected int size;
    protected int peak;
    protected final int max;

    public Pool() {
        this(16);
    }

    public Pool(int cap) {
        this(cap,Integer.MAX_VALUE);
    }

    @SuppressWarnings("unchecked")
    public Pool(int cap, int max) {
        if (cap < 0) throw new NegativeArraySizeException("cap < 0: " + cap);
        this.max = Math.max(Math.max(cap,1),max);
        this.items = (I[])new Object[cap];
    }

    protected abstract I newObject();

    public I obtain() {
        return size == 0 ? newObject() : dequeue();
    }

    public void free(I object) {
        if (object == null) throw new IllegalArgumentException("attempted to free null pointer");
        if (size < max) {
            enqueue(object);
            if (object instanceof Poolable poolable) poolable.onPooled();
        } else if (object instanceof Disposable disposable) disposable.dispose();
    }

    public void preFill(int count) {
        int nem_size = Math.min(max,count + size);
        ensureCapacity(nem_size);
        for (int i = 0; i < count && size < max; i++) {
            enqueue(newObject());
        }
    }

    public void clear() {
        if (size != 0) {
            for (int i = 0; i < items.length; i++) {
                if (items[i] != null) items[i] = null;
            } size = front = rear = 0;
        }
    }

    public void dispose() {
        while (size > 0) {
            I obj = dequeue();
            if (obj instanceof Disposable i) i.dispose();
        }
    }

    @SuppressWarnings("unchecked")
    private void enqueue(I obj) {
        if (size == items.length) {
            I[] tmp = items;
            int new_size = size == 0 ? 1 : size * 2;
            items = (I[])new Object[new_size];
            for (int i = 0; i < size; i++) {
                items[i] = tmp[(front + i) % size];
            } rear = size; front = 0;
        } items[rear] = obj;
        rear = (rear + 1) % items.length;
        peak = Math.max(peak,++size);
    }

    private I dequeue() {
        I obj = items[front];
        items[front] = null;
        if (--size == 0) {
            front = 0;
            rear = 0;
        } else front = (front + 1) % items.length;
        return obj;
    }

    @SuppressWarnings("unchecked")
    private void ensureCapacity(int size) {
        if (size > items.length) {
            I[] tmp = items;
            items = (I[])new Object[size];
            for (int i = 0; i < this.size; i++) {
                items[i] = tmp[(front + i) % this.size];
            } rear = this.size; front = 0;
        }
    }

    public int size() {
        return size;
    }

    public int peak() {
        return peak;
    }

    public int maxCapacity() {
        return max;
    }

    public int capacity() {
        return items.length;
    }

    public float loadFactor() {
        return items.length == 0 ? 1 : (float) size / items.length;
    }

    /**
     * Pool calls the Poolable "onPooled" method when an Object implementing Poolable object is
     * freed (returned to a Pool for reuse)
     * Frederik Dahl 2/16/2025
     */
    public interface Poolable {

        /**
         * Called when object is returned to a Pool.
         * Useful to reset object state before reuse.
         */
        default void onPooled() { /* */ }
    }

}
