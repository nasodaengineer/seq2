package com.github.wolray.seq;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;

/**
 * @author wolray
 */
public interface Reducer<T, V> {
    Supplier<V> supplier();
    BiConsumer<V, T> accumulator();
    Consumer<V> finisher();

    static <T> Transducer<T, double[], Double> average(ToDoubleFunction<T> function, ToDoubleFunction<T> weightFunction) {
        BiConsumer<double[], T> biConsumer;
        if (weightFunction != null) {
            biConsumer = (a, t) -> {
                double v = function.applyAsDouble(t);
                double w = weightFunction.applyAsDouble(t);
                a[0] += v * w;
                a[1] += w;
            };
        } else {
            biConsumer = (a, t) -> {
                a[0] += function.applyAsDouble(t);
                a[1] += 1;
            };
        }
        return Transducer.of(() -> new double[2], biConsumer, a -> a[1] != 0 ? a[0] / a[1] : 0);
    }

    static <T, C extends Collection<T>> Reducer<T, C> collect(Supplier<C> des) {
        return of(des, Collection::add);
    }

    static <T> Transducer<T, int[], Integer> count() {
        return Transducer.of(() -> new int[1], (a, t) -> a[0]++, a -> a[0]);
    }

    static <T> Transducer<T, int[], Integer> count(Predicate<T> predicate) {
        return Transducer.of(() -> new int[1], (a, t) -> {
            if (predicate.test(t)) {
                a[0]++;
            }
        }, a -> a[0]);
    }

    static <T> Transducer<T, int[], Integer> countNot(Predicate<T> predicate) {
        return count(predicate.negate());
    }

    static <T> Reducer<T, BatchedSeq<T>> filtering(Predicate<T> predicate) {
        return filtering(predicate, toBatched());
    }

    static <T, V> Reducer<T, V> filtering(Predicate<T> predicate, Reducer<T, V> reducer) {
        BiConsumer<V, T> accumulator = reducer.accumulator();
        return of(reducer.supplier(), (v, t) -> {
            if (predicate.test(t)) {
                accumulator.accept(v, t);
            }
        }, reducer.finisher());
    }

    static <T> Transducer<T, Mutable<T>, T> fold(BinaryOperator<T> binaryOperator) {
        return reduce((m, t) -> {
            if (m.isSet) {
                m.it = binaryOperator.apply(m.it, t);
            } else {
                m.set(t);
            }
        });
    }

    static <T> Transducer<T, StringJoiner, String> join(String sep, Function<T, String> function) {
        return Transducer.of(() -> new StringJoiner(sep), (j, t) -> j.add(function.apply(t)), StringJoiner::toString);
    }

    static <T, E> Reducer<T, ArraySeq<E>> mapping(Function<T, E> mapper) {
        return mapping(mapper, toList());
    }

    static <T, E, V> Reducer<T, V> mapping(Function<T, E> mapper, Reducer<E, V> reducer) {
        BiConsumer<V, E> accumulator = reducer.accumulator();
        return of(reducer.supplier(), (v, t) -> {
            E e = mapper.apply(t);
            accumulator.accept(v, e);
        }, reducer.finisher());
    }

    static <T> Transducer<T, Mutable<T>, T> max(Comparator<T> comparator) {
        return reduce((m, t) -> {
            if (m.it == null || comparator.compare(m.it, t) < 0) {
                m.it = t;
            }
        });
    }

    static <T, V extends Comparable<V>> Reducer<T, AtomicReference<Pair<T, V>>> maxAtomicBy(V initValue, Function<T, V> function) {
        return of(() -> new AtomicReference<>(new Pair<>(null, initValue)), (ref, t) -> {
            V v = function.apply(t);
            ref.updateAndGet(p -> {
                if (p.second.compareTo(v) < 0) {
                    p.set(t, v);
                }
                return p;
            });
        });
    }

    static <T, V extends Comparable<V>> Reducer<T, Pair<T, V>> maxBy(Function<T, V> function) {
        return of(() -> new Pair<>(null, null), (p, t) -> {
            V v = function.apply(t);
            if (p.second == null || p.second.compareTo(v) < 0) {
                p.set(t, v);
            }
        });
    }

    static <T> Reducer<T, IntPair<T>> maxByInt(ToIntFunction<T> function) {
        return of(() -> new IntPair<>(0, null), (p, t) -> {
            int v = function.applyAsInt(t);
            if (p.it == null || p.intVal < v) {
                p.intVal = v;
                p.it = t;
            }
        });
    }

    static <T> Reducer<T, DoublePair<T>> maxByDouble(ToDoubleFunction<T> function) {
        return of(() -> new DoublePair<>(0, null), (p, t) -> {
            double v = function.applyAsDouble(t);
            if (p.it == null || p.doubleVal < v) {
                p.doubleVal = v;
                p.it = t;
            }
        });
    }

    static <T> Reducer<T, LongPair<T>> maxByLong(ToLongFunction<T> function) {
        return of(() -> new LongPair<>(0, null), (p, t) -> {
            long v = function.applyAsLong(t);
            if (p.it == null || p.longVal < v) {
                p.longVal = v;
                p.it = t;
            }
        });
    }

    static <T> Transducer<T, Mutable<T>, T> min(Comparator<T> comparator) {
        return reduce((m, t) -> {
            if (m.it == null || comparator.compare(m.it, t) > 0) {
                m.it = t;
            }
        });
    }

    static <T, V extends Comparable<V>> Reducer<T, AtomicReference<Pair<T, V>>> minAtomicBy(V initValue, Function<T, V> function) {
        return of(() -> new AtomicReference<>(new Pair<>(null, initValue)), (ref, t) -> {
            V v = function.apply(t);
            ref.updateAndGet(p -> {
                if (p.second.compareTo(v) > 0) {
                    p.set(t, v);
                }
                return p;
            });
        });
    }

    static <T, V extends Comparable<V>> Reducer<T, Pair<T, V>> minBy(Function<T, V> function) {
        return of(() -> new Pair<>(null, null), (p, t) -> {
            V v = function.apply(t);
            if (p.second == null || p.second.compareTo(v) > 0) {
                p.set(t, v);
            }
        });
    }

    static <T> Reducer<T, IntPair<T>> minByInt(ToIntFunction<T> function) {
        return of(() -> new IntPair<>(0, null), (p, t) -> {
            int v = function.applyAsInt(t);
            if (p.it == null || p.intVal > v) {
                p.intVal = v;
                p.it = t;
            }
        });
    }

    static <T> Reducer<T, DoublePair<T>> minByDouble(ToDoubleFunction<T> function) {
        return of(() -> new DoublePair<>(0, null), (p, t) -> {
            double v = function.applyAsDouble(t);
            if (p.it == null || p.doubleVal > v) {
                p.doubleVal = v;
                p.it = t;
            }
        });
    }

    static <T> Reducer<T, LongPair<T>> minByLong(ToLongFunction<T> function) {
        return of(() -> new LongPair<>(0, null), (p, t) -> {
            long v = function.applyAsLong(t);
            if (p.it == null || p.longVal > v) {
                p.longVal = v;
                p.it = t;
            }
        });
    }

    static <T, V> Reducer<T, V> of(Supplier<V> supplier, BiConsumer<V, T> accumulator) {
        return of(supplier, accumulator, null);
    }

    static <T, V> Reducer<T, V> of(Supplier<V> supplier, BiConsumer<V, T> accumulator, Consumer<V> finisher) {
        return new Reducer<T, V>() {
            @Override
            public Supplier<V> supplier() {
                return supplier;
            }

            @Override
            public BiConsumer<V, T> accumulator() {
                return accumulator;
            }

            @Override
            public Consumer<V> finisher() {
                return finisher;
            }
        };
    }

    static <T> Reducer<T, Pair<BatchedSeq<T>, BatchedSeq<T>>> partition(Predicate<T> predicate) {
        return partition(predicate, toBatched());
    }

    static <T, V> Reducer<T, Pair<V, V>> partition(Predicate<T> predicate, Reducer<T, V> reducer) {
        BiConsumer<V, T> accumulator = reducer.accumulator();
        Supplier<V> supplier = reducer.supplier();
        Consumer<V> finisher = reducer.finisher();
        return of(() -> new Pair<>(supplier.get(), supplier.get()),
            (p, t) -> accumulator.accept(predicate.test(t) ? p.first : p.second, t),
            finisher == null ? null : p -> {
                finisher.accept(p.first);
                finisher.accept(p.second);
            });
    }

    static <T, V, R> Transducer<T, Pair<V, V>, Pair<R, R>> partition(Predicate<T> predicate, Transducer<T, V, R> transducer) {
        Function<V, R> transformer = transducer.transformer();
        return Transducer.of(partition(predicate, transducer.reducer()),
            p -> new Pair<>(transformer.apply(p.first), transformer.apply(p.second)));
    }

    static <T, V> Transducer<T, Mutable<V>, V> reduce(BiConsumer<Mutable<V>, T> accumulator) {
        return Transducer.of(() -> new Mutable<>(null), accumulator, Mutable::get);
    }

    static <T> Reducer<T, ArraySeq<T>> reverse() {
        return Reducer.<T>toList().then(Collections::reverse);
    }

    static <T> Reducer<T, ArraySeq<T>> sort() {
        return sort((Comparator<T>)null);
    }

    static <T> Reducer<T, ArraySeq<T>> sort(Comparator<T> comparator) {
        return Reducer.<T>toList().then(ts -> ts.sort(comparator));
    }

    static <T, V extends Comparable<V>> Reducer<T, ArraySeq<T>> sort(Function<T, V> function) {
        return sort(Comparator.comparing(function));
    }

    static <T> Reducer<T, ArraySeq<T>> sortDesc() {
        return sort(Collections.reverseOrder());
    }

    static <T> Reducer<T, ArraySeq<T>> sortDesc(Comparator<T> comparator) {
        return sort(comparator.reversed());
    }

    static <T, V extends Comparable<V>> Reducer<T, ArraySeq<T>> sortDesc(Function<T, V> function) {
        return sort(Comparator.comparing(function).reversed());
    }

    static <T> Transducer<T, double[], Double> sum(ToDoubleFunction<T> function) {
        return Transducer.of(() -> new double[1], (a, t) -> a[0] += function.applyAsDouble(t), a -> a[0]);
    }

    static <T> Transducer<T, int[], Integer> sumInt(ToIntFunction<T> function) {
        return Transducer.of(() -> new int[1], (a, t) -> a[0] += function.applyAsInt(t), a -> a[0]);
    }

    static <T> Transducer<T, long[], Long> sumLong(ToLongFunction<T> function) {
        return Transducer.of(() -> new long[1], (a, t) -> a[0] += function.applyAsLong(t), a -> a[0]);
    }

    static <T> Reducer<T, BatchedSeq<T>> toBatched() {
        return of(BatchedSeq::new, BatchedSeq::add);
    }

    static <T> Reducer<T, ConcurrentSeq<T>> toConcurrent() {
        return of(ConcurrentSeq::new, ConcurrentSeq::add);
    }

    static <T> Reducer<T, LinkedSeq<T>> toLinked() {
        return of(LinkedSeq::new, LinkedSeq::add);
    }

    static <T> Reducer<T, ArraySeq<T>> toList() {
        return of(ArraySeq::new, ArraySeq::add);
    }

    static <T> Reducer<T, ArraySeq<T>> toList(int initialCapacity) {
        return of(() -> new ArraySeq<>(initialCapacity), ArraySeq::add);
    }

    static <T> Reducer<T, SeqSet<T>> toSet() {
        return of(LinkedSeqSet::new, Set::add);
    }

    static <T> Reducer<T, SeqSet<T>> toSet(int initialCapacity) {
        return of(() -> new LinkedSeqSet<>(initialCapacity), Set::add);
    }

    default Reducer<T, V> then(Consumer<V> action) {
        Consumer<V> finisher = finisher();
        return of(supplier(), accumulator(), finisher == null ? action : finisher.andThen(action));
    }
}
