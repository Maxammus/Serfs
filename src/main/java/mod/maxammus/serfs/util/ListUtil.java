package mod.maxammus.serfs.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class ListUtil {
    public static <E> E getOrNull(List<E> list, int index) {
        if(index >= 0 && list.size() >= index + 1)
            return list.get(index);
        return null;
    }

    public static <E> E getOrDefault(List<E> list, int index, E _default) {
        if(index >= 0 && list.size() >= index + 1)
            return list.get(index);
        return _default;
    }

    public static <E> E findOrNull(Collection<E> list, Predicate<? super E> predicate) {
        return list.stream()
                .filter(predicate)
                .findFirst()
                .orElse(null);
    }

    public static <E> E findOrNull(E[] array, Predicate<? super E> predicate) {
        return Arrays.stream(array)
                .filter(predicate)
                .findFirst()
                .orElse(null);
    }

    public static <E> E findOrDefault(Collection<E> list, Predicate<? super E> predicate, E _default) {
        return list.stream()
                .filter(predicate)
                .findFirst()
                .orElse(_default);
    }

    public static <E> E findOrDefault(E[] array, Predicate<? super E> predicate, E _default) {
        return Arrays.stream(array)
                .filter(predicate)
                .findFirst()
                .orElse(_default);
    }
}
