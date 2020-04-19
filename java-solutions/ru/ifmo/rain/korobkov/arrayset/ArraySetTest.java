package ru.ifmo.rain.korobkov.arrayset;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.NavigableSet;
import java.util.SortedSet;

public class ArraySetTest {
    public static void main(final String[] args) {

        final ArrayList<Integer> l = new ArrayList<>();
        l.add(1);
        l.add(2);
        l.add(3);

        final NavigableSet<Integer> s = new ArraySet<>(l);
        final NavigableSet<Integer> tail = s.tailSet(-1, false);
        System.out.println(tail.first().toString());
    }
}
