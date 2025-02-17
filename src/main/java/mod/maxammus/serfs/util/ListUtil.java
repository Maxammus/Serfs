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
}
