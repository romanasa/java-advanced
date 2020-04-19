package ru.ifmo.rain.korobkov.arrayset;

import java.util.*;

public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {
    private final List<T> data;
    private Comparator<? super T> cmp = null;

    public ArraySet() {
        data = List.of();
    }

    public ArraySet(final Collection<? extends T> c) {
        data = List.copyOf(new TreeSet<>(c));
    }

    public ArraySet(final Collection<? extends T> c, final Comparator<? super T> cmp) {
        this.cmp = cmp;
        final TreeSet<T> t = new TreeSet<>(cmp);
        t.addAll(c);
        data = List.copyOf(t);
    }

    public ArraySet(final List<T> data, final Comparator<? super T> cmp) {
        this.cmp = cmp;
        this.data = Collections.unmodifiableList(data);
    }

    private boolean checkInd(final int ind) {
        return 0 <= ind && ind < size();
    }

    private T getByInd(final int ind) {
        return checkInd(ind) ? data.get(ind) : null;
    }

    private int getElemInd(final T t, final int foundAdd, final int notFoundAdd) {
        final int ind = Collections.binarySearch(data, t, cmp);
        return ind < 0 ? -ind - 1 + notFoundAdd : ind + foundAdd;
    }

    private T getElem(final T t, final int foundAdd, final int notFoundAdd) {
        return getByInd(getElemInd(t, foundAdd, notFoundAdd));
    }

    @Override
    public T lower(final T t) {
        return getElem(t, -1, -1);
    }

    @Override
    public T floor(final T t) {
        return getElem(t, 0, -1);
    }

    @Override
    public T ceiling(final T t) {
        return getElem(t, 0, 0);
    }

    @Override
    public T higher(final T t) {
        return getElem(t, 1, 0);
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<T> iterator() {
        return data.iterator();
    }

    @Override
    public NavigableSet<T> descendingSet() {
        return new ArraySet<>(new FastReversedList<>(data), Collections.reverseOrder(cmp));
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }

    @SuppressWarnings("unchecked")
    private int compare(final T a, final T b) {
        if (cmp == null) {
            return ((Comparable<T>) a).compareTo(b);
        }
        return cmp.compare(a, b);
    }

    private void checkOrder(final T fromElement, final T toElement) {
        if (compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public NavigableSet<T> subSet(final T fromElement, final boolean fromInclusive, final T toElement, final boolean toInclusive) {
        checkOrder(fromElement, toElement);

        final int l = getElemInd(fromElement, fromInclusive ? 0 : 1, 0);
        final int r = getElemInd(toElement, toInclusive ? 0 : -1, -1);

        if (!checkInd(l) || !checkInd(r) || l > r) {
            return new ArraySet<>(Collections.emptyList(), cmp);
        }
        return new ArraySet<>(data.subList(l, r + 1), cmp);
    }

    @Override
    public NavigableSet<T> headSet(final T toElement, final boolean inclusive) {
        if (data.isEmpty() || compare(first(), toElement) > 0) {
            return new ArraySet<>(Collections.emptyList(), cmp);
        }
        return subSet(first(), true, toElement, inclusive);
    }

    @Override
    public NavigableSet<T> tailSet(final T fromElement, final boolean inclusive) {
        if (data.isEmpty() || compare(fromElement, last()) > 0) {
            return new ArraySet<>(Collections.emptyList(), cmp);
        }
        return subSet(fromElement, inclusive, last(), true);
    }

    @Override
    public Comparator<? super T> comparator() {
        return cmp;
    }

    @Override
    public SortedSet<T> subSet(final T fromElement, final T toElement) {
        checkOrder(fromElement, toElement);
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<T> headSet(final T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(final T fromElement) {
        return tailSet(fromElement, true);
    }

    private void notEmpty() {
        if (data.isEmpty()) {
            throw new NoSuchElementException();
        }
    }

    @Override
    public T first() {
        notEmpty();
        return data.get(0);
    }

    @Override
    public T last() {
        notEmpty();
        return data.get(data.size() - 1);
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(final Object o) {
        return Collections.binarySearch(data, (T) o, cmp) >= 0;
    }
}
