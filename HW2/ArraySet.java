import java.util.*;

/**
 * @author Sergey Fadeev
 * 02.2020
 */

public class ArraySet<T> extends AbstractSet<T> implements SortedSet<T> {
    private List<T> data;
    private Comparator<? super T> comparator;

    public ArraySet() {
        this(new ArrayList<>(), null);
    }

    public ArraySet(final Collection<? extends T> data) {
        this(data, null);
    }

    public ArraySet(final Comparator<? super T> comparator) {
        this(Collections.emptyList(), comparator);
    }

    public ArraySet(final Collection<? extends T> data, final Comparator<? super T> comparator) {
        Set<T> tempSortSet = new TreeSet<>(comparator);
        tempSortSet.addAll(data);
        this.data = new ArrayList<>(tempSortSet);
        this.comparator = comparator;
    }

    private ArraySet(final List<T> data, final Comparator<? super T> comparator) {
        this.data = data;
        this.comparator = comparator;
    }

    @Override
    public Iterator<T> iterator() {
        return Collections.unmodifiableList(data).iterator();
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    @Override
    public T first() {
        if (!isEmpty()) {
            return data.get(0);
        } else {
            throw new NoSuchElementException("Set is empty");
        }
    }

    @Override
    public T last() {
        if (!isEmpty()) {
            return data.get(size() - 1);
        } else {
            throw new NoSuchElementException("Set is empty");
        }
    }

    @Override
    public SortedSet<T> subSet(final T fromElement, final T toElement) {
        if (!argsIsIllegal(fromElement, toElement)) {
            int from = Collections.binarySearch(this.data, fromElement, comparator);
            int to = Collections.binarySearch(this.data, toElement, comparator);
            return subSet(from, to);
        } else {
            throw new IllegalArgumentException("Illegal arguments in subSet");
        }
    }

    @Override
    public SortedSet<T> headSet(final T toElement) {
        int to = Collections.binarySearch(data, toElement, comparator);
        return isEmpty() ? new ArraySet<>(comparator) : subSet(0, to);
    }

    @Override
    public SortedSet<T> tailSet(final T fromElement) {
        int from = Collections.binarySearch(data, fromElement, comparator);
        return isEmpty() ? new ArraySet<>(comparator) : subSet(from, size());
    }

    @SuppressWarnings("unchecked cast")
    public boolean contains(Object element) {

        return Collections.binarySearch(this.data, (T) element, this.comparator) >= 0;
    }

    //private
    private int getValidPosition(final int position) {
        return position < 0 ? -(position + 1) : position;
    }

    private SortedSet<T> subSet(int from, int to) {
        from = getValidPosition(from);
        to = getValidPosition(to);

        return new ArraySet<>(data.subList(from, to), comparator);
    }

    @SuppressWarnings("unchecked cast")
    private boolean argsIsIllegal(final T fromElement, final T toElement) {
        int resultOfCompare;
        if (comparator == null) {
            resultOfCompare = ((Comparable<? super T>) fromElement).compareTo(toElement);
        } else {
            resultOfCompare = comparator.compare(fromElement, toElement);
        }
        return resultOfCompare > 0;
    }
}
