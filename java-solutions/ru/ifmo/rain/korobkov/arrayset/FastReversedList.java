package ru.ifmo.rain.korobkov.arrayset;

import java.util.AbstractList;
import java.util.List;

public class FastReversedList<T> extends AbstractList<T> {
    private final boolean reversed;
    private final List<T> data;

    public FastReversedList(final List<T> data) {
        if (data instanceof FastReversedList) {
            this.data = ((FastReversedList<T>) data).data;
            this.reversed = !((FastReversedList<T>) data).reversed;
        } else {
            this.data = data;
            reversed = true;
        }
    }

    @Override
    public T get(int index) {
        if (reversed) {
            index = size() - 1 - index;
        }
        return data.get(index);
    }

    @Override
    public int size() {
        return data.size();
    }
}
