import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentGroupQuery;
import info.kgeorgiy.java.advanced.student.StudentQuery;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements StudentGroupQuery {

    private static final String EMPTY_STRING = "";
    private final String SPACE = " ";

    private static final Comparator<Student> studentComparator = Comparator.
            comparing(Student::getLastName, String::compareTo).
            thenComparing(Student::getFirstName, String::compareTo).
            thenComparing(Student::getId, Integer::compareTo);

    private static List<Student> sortWithMyComparatorInListWithFilter(Stream<Student> streamOfStudents, Predicate<Student> filter, Comparator<Student> comparator) {
        return streamOfStudents.filter(filter).sorted(comparator).collect(Collectors.toList());
    }

    private static <T extends Collection<String>> T studentsInCollection(List<Student> students,
                                                                  Function<Student, String> mapper,
                                                                  Supplier<T> collection) {

        return students.stream().map(mapper).collect(Collectors.toCollection(collection));
    }

    @Override
    public  List<String> getFirstNames(List<Student> students) {
        return studentsInCollection(students, Student::getFirstName, ArrayList::new);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return studentsInCollection(students, Student::getLastName, ArrayList::new);
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return studentsInCollection(students, Student::getGroup, ArrayList::new);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return studentsInCollection(students, Student -> Student.getFirstName() + SPACE + Student.getLastName(), ArrayList::new);
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return studentsInCollection(students, Student::getFirstName, TreeSet::new);
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortWithMyComparatorInListWithFilter(students.stream(), Student -> true, Student::compareTo);
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortWithMyComparatorInListWithFilter(students.stream(), Student -> true, studentComparator);
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return sortWithMyComparatorInListWithFilter(students.stream(), Student -> Student.getFirstName().equals(name), studentComparator);
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return sortWithMyComparatorInListWithFilter(students.stream(), Student -> Student.getLastName().equals(name), studentComparator);
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return sortWithMyComparatorInListWithFilter(students.stream(), Student -> Student.getGroup().equals(group), studentComparator);
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return students.stream().sorted(Student::compareTo).map(Student::getFirstName).findFirst().orElse(EMPTY_STRING);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return students.stream().
                filter(Student -> Student.getGroup().equals(group)).
                collect(Collectors.toMap(Student::getLastName, Student::getFirstName, BinaryOperator.minBy(String::compareTo)));
    }
}