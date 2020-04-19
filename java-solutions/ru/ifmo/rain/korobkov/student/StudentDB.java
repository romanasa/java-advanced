package ru.ifmo.rain.korobkov.student;

import info.kgeorgiy.java.advanced.student.AdvancedStudentGroupQuery;
import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements AdvancedStudentGroupQuery {

    private <C extends Collection<String>> C mapStudentsCollection(final List<Student> students, final Function<Student, String> mapper,
                                                                   final Supplier<C> collectionFactory) {
        return students.stream().map(mapper).collect(Collectors.toCollection(collectionFactory));
    }

    private List<String> mapStudentsList(final List<Student> students, final Function<Student, String> mapper) {
        return mapStudentsCollection(students, mapper, ArrayList::new);
    }

    @Override
    public List<String> getFirstNames(final List<Student> students) {
        return mapStudentsList(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(final List<Student> students) {
        return mapStudentsList(students, Student::getLastName);
    }

    @Override
    public List<String> getGroups(final List<Student> students) {
        return mapStudentsList(students, Student::getGroup);
    }

    private String getFullName(final Student student) {
        return student.getFirstName() + " "  + student.getLastName();
    }

    @Override
    public List<String> getFullNames(final List<Student> students) {
        return mapStudentsList(students, this::getFullName);
    }

    @Override
    public Set<String> getDistinctFirstNames(final List<Student> students) {
        return mapStudentsCollection(students, Student::getFirstName, TreeSet::new);
    }

    @Override
    public String getMinStudentFirstName(final List<Student> students) {
        return students.stream().min(Student::compareTo).map(Student::getFirstName).orElse("");
    }

    @Override
    public List<Student> sortStudentsById(final Collection<Student> students) {
        return students.stream().sorted(Student::compareTo).collect(Collectors.toList());
    }

    private List<Student> sortStudents(final Stream<Student> stream) {
        return stream.sorted(
                Comparator.comparing(Student::getLastName, String::compareTo)
                        .thenComparing(Student::getFirstName, String::compareTo)
                        .thenComparingInt(Student::getId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> sortStudentsByName(final Collection<Student> students) {
        return sortStudents(students.stream());
    }

    private List<Student> filterAndSort(final Collection<Student> students, final Predicate<Student> p) {
        return sortStudents(students.stream().filter(p));
    }

    private List<Student> findStudentsByFunctionValue(final Collection<Student> students, final Function<Student, String> f,
                                                      final String value) {
        return filterAndSort(students, student -> value.equals(f.apply(student)));
    }

    @Override
    public List<Student> findStudentsByFirstName(final Collection<Student> students, final String name) {
        return findStudentsByFunctionValue(students, Student::getFirstName, name);
    }

    @Override
    public List<Student> findStudentsByLastName(final Collection<Student> students, final String name) {
        return findStudentsByFunctionValue(students, Student::getLastName, name);
    }

    @Override
    public List<Student> findStudentsByGroup(final Collection<Student> students, final String group) {
        return findStudentsByFunctionValue(students, Student::getGroup, group);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(final Collection<Student> students, final String group) {
        return students.stream()
                .filter(student -> group.equals(student.getGroup()))
                .collect(Collectors.toMap(Student::getLastName, Student::getFirstName, BinaryOperator.minBy(String::compareTo)));
    }

    private Stream<Map.Entry<String, List<Student>>> getGroupsStream(final Collection<Student> students) {
        return students.stream()
                .collect(Collectors.groupingBy(Student::getGroup, TreeMap::new, Collectors.toList()))
                .entrySet().stream();
    }

    private List<Group> getGroupsByFunction(final Collection<Student> students, final Function<List<Student>, List<Student>> sorter) {
        return getGroupsStream(students).map(x -> new Group(x.getKey(), sorter.apply(x.getValue())))
                .collect(Collectors.toList());
    }

    @Override
    public List<Group> getGroupsByName(final Collection<Student> students) {
        return getGroupsByFunction(students, this::sortStudentsByName);
    }

    @Override
    public List<Group> getGroupsById(final Collection<Student> students) {
        return getGroupsByFunction(students, this::sortStudentsById);
    }

    private String getLargestGroupByFunction(final Collection<Student> students, final ToIntFunction<List<Student>> f) {
        return getGroupsStream(students)
                .max(Comparator.<Map.Entry<String, List<Student>>>comparingInt(
                        group -> f.applyAsInt(group.getValue()))
                        .thenComparing(Map.Entry::getKey, Collections.reverseOrder(String::compareTo)))
                .map(Map.Entry::getKey).orElse("");
    }

    @Override
    public String getLargestGroup(final Collection<Student> students) {
        return getLargestGroupByFunction(students, List::size);
    }

    @Override
    public String getLargestGroupFirstName(final Collection<Student> students) {
        return getLargestGroupByFunction(students, list -> getDistinctFirstNames(list).size());
    }

    @Override
    public String getMostPopularName(final Collection<Student> students) {
        return students.stream().collect(
                Collectors.groupingBy(this::getFullName,
                        Collectors.mapping(Student::getGroup,
                                Collectors.collectingAndThen(Collectors.toSet(), Set::size))))
                .entrySet().stream().max(Map.Entry.<String, Integer>comparingByValue()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey).orElse("");
    }

    private List<String> getFunctionValuesByIndices(final Collection<Student> students, final int[] indices,
                                                    final Function<Student, String> f) {
        return Arrays.stream(indices)
                .mapToObj(List.copyOf(students)::get)
                .map(f)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getFirstNames(final Collection<Student> students, final int[] indices) {
        return getFunctionValuesByIndices(students, indices, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(final Collection<Student> students, final int[] indices) {
        return getFunctionValuesByIndices(students, indices, Student::getLastName);
    }

    @Override
    public List<String> getGroups(final Collection<Student> students, final int[] indices) {
        return getFunctionValuesByIndices(students, indices, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(final Collection<Student> students, final int[] indices) {
        return getFunctionValuesByIndices(students, indices, this::getFullName);
    }
}
