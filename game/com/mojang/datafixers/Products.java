package com.mojang.datafixers;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.kinds.K1;

/**
 * The stand-in for DataFixerUpper's Products: the values grouped by an instance (Kind1.group), applied to a function
 * of them by that instance (RecordCodecBuilder's: its fields and its constructor). Other instances aren't modelled.
 */
public interface Products {
    final class P1<F extends K1, T1> {
        private final App<F, T1> t1;

        public P1(App<F, T1> t1) {
            this.t1 = t1;
        }

        public App<F, T1> t1() {
            return t1;
        }

        public <R> App<F, R> apply(Applicative<F, ?> instance, java.util.function.Function<T1, R> function) {
            return com.mojang.serialization.codecs.RecordCodecBuilder.horizonApply(instance, function, t1);
        }


        public <T2> P2<F, T1, T2> and(App<F, T2> t2) {
            return new P2<>(t1, t2);
        }

        public <T2, T3> P3<F, T1, T2, T3> and(P2<F, T2, T3> p) {
            return new P3<>(t1, p.t1(), p.t2());
        }

        public <T2, T3, T4> P4<F, T1, T2, T3, T4> and(P3<F, T2, T3, T4> p) {
            return new P4<>(t1, p.t1(), p.t2(), p.t3());
        }

        public <T2, T3, T4, T5> P5<F, T1, T2, T3, T4, T5> and(P4<F, T2, T3, T4, T5> p) {
            return new P5<>(t1, p.t1(), p.t2(), p.t3(), p.t4());
        }

        public <T2, T3, T4, T5, T6> P6<F, T1, T2, T3, T4, T5, T6> and(P5<F, T2, T3, T4, T5, T6> p) {
            return new P6<>(t1, p.t1(), p.t2(), p.t3(), p.t4(), p.t5());
        }

        public <T2, T3, T4, T5, T6, T7> P7<F, T1, T2, T3, T4, T5, T6, T7> and(P6<F, T2, T3, T4, T5, T6, T7> p) {
            return new P7<>(t1, p.t1(), p.t2(), p.t3(), p.t4(), p.t5(), p.t6());
        }

        public <T2, T3, T4, T5, T6, T7, T8> P8<F, T1, T2, T3, T4, T5, T6, T7, T8> and(P7<F, T2, T3, T4, T5, T6, T7, T8> p) {
            return new P8<>(t1, p.t1(), p.t2(), p.t3(), p.t4(), p.t5(), p.t6(), p.t7());
        }
    }

    final class P2<F extends K1, T1, T2> {
        private final App<F, T1> t1;
        private final App<F, T2> t2;

        public P2(App<F, T1> t1, App<F, T2> t2) {
            this.t1 = t1;
            this.t2 = t2;
        }

        public App<F, T1> t1() {
            return t1;
        }

        public App<F, T2> t2() {
            return t2;
        }

        public <R> App<F, R> apply(Applicative<F, ?> instance, java.util.function.BiFunction<T1, T2, R> function) {
            return com.mojang.serialization.codecs.RecordCodecBuilder.horizonApply(instance, function, t1, t2);
        }

        public <T3> P3<F, T1, T2, T3> and(App<F, T3> t3) {
            return new P3<>(t1, t2, t3);
        }

        public <T3, T4> P4<F, T1, T2, T3, T4> and(P2<F, T3, T4> p) {
            return new P4<>(t1, t2, p.t1(), p.t2());
        }

        public <T3, T4, T5> P5<F, T1, T2, T3, T4, T5> and(P3<F, T3, T4, T5> p) {
            return new P5<>(t1, t2, p.t1(), p.t2(), p.t3());
        }

        public <T3, T4, T5, T6> P6<F, T1, T2, T3, T4, T5, T6> and(P4<F, T3, T4, T5, T6> p) {
            return new P6<>(t1, t2, p.t1(), p.t2(), p.t3(), p.t4());
        }

        public <T3, T4, T5, T6, T7> P7<F, T1, T2, T3, T4, T5, T6, T7> and(P5<F, T3, T4, T5, T6, T7> p) {
            return new P7<>(t1, t2, p.t1(), p.t2(), p.t3(), p.t4(), p.t5());
        }

        public <T3, T4, T5, T6, T7, T8> P8<F, T1, T2, T3, T4, T5, T6, T7, T8> and(P6<F, T3, T4, T5, T6, T7, T8> p) {
            return new P8<>(t1, t2, p.t1(), p.t2(), p.t3(), p.t4(), p.t5(), p.t6());
        }
    }

    final class P3<F extends K1, T1, T2, T3> {
        private final App<F, T1> t1;
        private final App<F, T2> t2;
        private final App<F, T3> t3;

        public P3(App<F, T1> t1, App<F, T2> t2, App<F, T3> t3) {
            this.t1 = t1;
            this.t2 = t2;
            this.t3 = t3;
        }

        public App<F, T1> t1() {
            return t1;
        }

        public App<F, T2> t2() {
            return t2;
        }

        public App<F, T3> t3() {
            return t3;
        }

        public <R> App<F, R> apply(Applicative<F, ?> instance, com.mojang.datafixers.util.Function3<T1, T2, T3, R> function) {
            return com.mojang.serialization.codecs.RecordCodecBuilder.horizonApply(instance, function, t1, t2, t3);
        }

        public <T4> P4<F, T1, T2, T3, T4> and(App<F, T4> t4) {
            return new P4<>(t1, t2, t3, t4);
        }

        public <T4, T5> P5<F, T1, T2, T3, T4, T5> and(P2<F, T4, T5> p) {
            return new P5<>(t1, t2, t3, p.t1(), p.t2());
        }

        public <T4, T5, T6> P6<F, T1, T2, T3, T4, T5, T6> and(P3<F, T4, T5, T6> p) {
            return new P6<>(t1, t2, t3, p.t1(), p.t2(), p.t3());
        }

        public <T4, T5, T6, T7> P7<F, T1, T2, T3, T4, T5, T6, T7> and(P4<F, T4, T5, T6, T7> p) {
            return new P7<>(t1, t2, t3, p.t1(), p.t2(), p.t3(), p.t4());
        }

        public <T4, T5, T6, T7, T8> P8<F, T1, T2, T3, T4, T5, T6, T7, T8> and(P5<F, T4, T5, T6, T7, T8> p) {
            return new P8<>(t1, t2, t3, p.t1(), p.t2(), p.t3(), p.t4(), p.t5());
        }
    }

    final class P4<F extends K1, T1, T2, T3, T4> {
        private final App<F, T1> t1;
        private final App<F, T2> t2;
        private final App<F, T3> t3;
        private final App<F, T4> t4;

        public P4(App<F, T1> t1, App<F, T2> t2, App<F, T3> t3, App<F, T4> t4) {
            this.t1 = t1;
            this.t2 = t2;
            this.t3 = t3;
            this.t4 = t4;
        }

        public App<F, T1> t1() {
            return t1;
        }

        public App<F, T2> t2() {
            return t2;
        }

        public App<F, T3> t3() {
            return t3;
        }

        public App<F, T4> t4() {
            return t4;
        }

        public <R> App<F, R> apply(Applicative<F, ?> instance, com.mojang.datafixers.util.Function4<T1, T2, T3, T4, R> function) {
            return com.mojang.serialization.codecs.RecordCodecBuilder.horizonApply(instance, function, t1, t2, t3, t4);
        }

        public <T5> P5<F, T1, T2, T3, T4, T5> and(App<F, T5> t5) {
            return new P5<>(t1, t2, t3, t4, t5);
        }

        public <T5, T6> P6<F, T1, T2, T3, T4, T5, T6> and(P2<F, T5, T6> p) {
            return new P6<>(t1, t2, t3, t4, p.t1(), p.t2());
        }

        public <T5, T6, T7> P7<F, T1, T2, T3, T4, T5, T6, T7> and(P3<F, T5, T6, T7> p) {
            return new P7<>(t1, t2, t3, t4, p.t1(), p.t2(), p.t3());
        }

        public <T5, T6, T7, T8> P8<F, T1, T2, T3, T4, T5, T6, T7, T8> and(P4<F, T5, T6, T7, T8> p) {
            return new P8<>(t1, t2, t3, t4, p.t1(), p.t2(), p.t3(), p.t4());
        }
    }

    final class P5<F extends K1, T1, T2, T3, T4, T5> {
        private final App<F, T1> t1;
        private final App<F, T2> t2;
        private final App<F, T3> t3;
        private final App<F, T4> t4;
        private final App<F, T5> t5;

        public P5(App<F, T1> t1, App<F, T2> t2, App<F, T3> t3, App<F, T4> t4, App<F, T5> t5) {
            this.t1 = t1;
            this.t2 = t2;
            this.t3 = t3;
            this.t4 = t4;
            this.t5 = t5;
        }

        public App<F, T1> t1() {
            return t1;
        }

        public App<F, T2> t2() {
            return t2;
        }

        public App<F, T3> t3() {
            return t3;
        }

        public App<F, T4> t4() {
            return t4;
        }

        public App<F, T5> t5() {
            return t5;
        }

        public <R> App<F, R> apply(Applicative<F, ?> instance, com.mojang.datafixers.util.Function5<T1, T2, T3, T4, T5, R> function) {
            return com.mojang.serialization.codecs.RecordCodecBuilder.horizonApply(instance, function, t1, t2, t3, t4, t5);
        }

        public <T6> P6<F, T1, T2, T3, T4, T5, T6> and(App<F, T6> t6) {
            return new P6<>(t1, t2, t3, t4, t5, t6);
        }

        public <T6, T7> P7<F, T1, T2, T3, T4, T5, T6, T7> and(P2<F, T6, T7> p) {
            return new P7<>(t1, t2, t3, t4, t5, p.t1(), p.t2());
        }

        public <T6, T7, T8> P8<F, T1, T2, T3, T4, T5, T6, T7, T8> and(P3<F, T6, T7, T8> p) {
            return new P8<>(t1, t2, t3, t4, t5, p.t1(), p.t2(), p.t3());
        }
    }

    final class P6<F extends K1, T1, T2, T3, T4, T5, T6> {
        private final App<F, T1> t1;
        private final App<F, T2> t2;
        private final App<F, T3> t3;
        private final App<F, T4> t4;
        private final App<F, T5> t5;
        private final App<F, T6> t6;

        public P6(App<F, T1> t1, App<F, T2> t2, App<F, T3> t3, App<F, T4> t4, App<F, T5> t5, App<F, T6> t6) {
            this.t1 = t1;
            this.t2 = t2;
            this.t3 = t3;
            this.t4 = t4;
            this.t5 = t5;
            this.t6 = t6;
        }

        public App<F, T1> t1() {
            return t1;
        }

        public App<F, T2> t2() {
            return t2;
        }

        public App<F, T3> t3() {
            return t3;
        }

        public App<F, T4> t4() {
            return t4;
        }

        public App<F, T5> t5() {
            return t5;
        }

        public App<F, T6> t6() {
            return t6;
        }

        public <R> App<F, R> apply(Applicative<F, ?> instance, com.mojang.datafixers.util.Function6<T1, T2, T3, T4, T5, T6, R> function) {
            return com.mojang.serialization.codecs.RecordCodecBuilder.horizonApply(instance, function, t1, t2, t3, t4, t5, t6);
        }

        public <T7> P7<F, T1, T2, T3, T4, T5, T6, T7> and(App<F, T7> t7) {
            return new P7<>(t1, t2, t3, t4, t5, t6, t7);
        }

        public <T7, T8> P8<F, T1, T2, T3, T4, T5, T6, T7, T8> and(P2<F, T7, T8> p) {
            return new P8<>(t1, t2, t3, t4, t5, t6, p.t1(), p.t2());
        }
    }

    final class P7<F extends K1, T1, T2, T3, T4, T5, T6, T7> {
        private final App<F, T1> t1;
        private final App<F, T2> t2;
        private final App<F, T3> t3;
        private final App<F, T4> t4;
        private final App<F, T5> t5;
        private final App<F, T6> t6;
        private final App<F, T7> t7;

        public P7(App<F, T1> t1, App<F, T2> t2, App<F, T3> t3, App<F, T4> t4, App<F, T5> t5, App<F, T6> t6, App<F, T7> t7) {
            this.t1 = t1;
            this.t2 = t2;
            this.t3 = t3;
            this.t4 = t4;
            this.t5 = t5;
            this.t6 = t6;
            this.t7 = t7;
        }

        public App<F, T1> t1() {
            return t1;
        }

        public App<F, T2> t2() {
            return t2;
        }

        public App<F, T3> t3() {
            return t3;
        }

        public App<F, T4> t4() {
            return t4;
        }

        public App<F, T5> t5() {
            return t5;
        }

        public App<F, T6> t6() {
            return t6;
        }

        public App<F, T7> t7() {
            return t7;
        }

        public <R> App<F, R> apply(Applicative<F, ?> instance, com.mojang.datafixers.util.Function7<T1, T2, T3, T4, T5, T6, T7, R> function) {
            return com.mojang.serialization.codecs.RecordCodecBuilder.horizonApply(instance, function, t1, t2, t3, t4, t5, t6, t7);
        }

        public <T8> P8<F, T1, T2, T3, T4, T5, T6, T7, T8> and(App<F, T8> t8) {
            return new P8<>(t1, t2, t3, t4, t5, t6, t7, t8);
        }
    }

    final class P8<F extends K1, T1, T2, T3, T4, T5, T6, T7, T8> {
        private final App<F, T1> t1;
        private final App<F, T2> t2;
        private final App<F, T3> t3;
        private final App<F, T4> t4;
        private final App<F, T5> t5;
        private final App<F, T6> t6;
        private final App<F, T7> t7;
        private final App<F, T8> t8;

        public P8(App<F, T1> t1, App<F, T2> t2, App<F, T3> t3, App<F, T4> t4, App<F, T5> t5, App<F, T6> t6, App<F, T7> t7, App<F, T8> t8) {
            this.t1 = t1;
            this.t2 = t2;
            this.t3 = t3;
            this.t4 = t4;
            this.t5 = t5;
            this.t6 = t6;
            this.t7 = t7;
            this.t8 = t8;
        }

        public App<F, T1> t1() {
            return t1;
        }

        public App<F, T2> t2() {
            return t2;
        }

        public App<F, T3> t3() {
            return t3;
        }

        public App<F, T4> t4() {
            return t4;
        }

        public App<F, T5> t5() {
            return t5;
        }

        public App<F, T6> t6() {
            return t6;
        }

        public App<F, T7> t7() {
            return t7;
        }

        public App<F, T8> t8() {
            return t8;
        }

        public <R> App<F, R> apply(Applicative<F, ?> instance, com.mojang.datafixers.util.Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> function) {
            return com.mojang.serialization.codecs.RecordCodecBuilder.horizonApply(instance, function, t1, t2, t3, t4, t5, t6, t7, t8);
        }
    }

    final class P9<F extends K1, T1, T2, T3, T4, T5, T6, T7, T8, T9> {
        private final App<F, T1> t1;
        private final App<F, T2> t2;
        private final App<F, T3> t3;
        private final App<F, T4> t4;
        private final App<F, T5> t5;
        private final App<F, T6> t6;
        private final App<F, T7> t7;
        private final App<F, T8> t8;
        private final App<F, T9> t9;

        public P9(App<F, T1> t1, App<F, T2> t2, App<F, T3> t3, App<F, T4> t4, App<F, T5> t5, App<F, T6> t6, App<F, T7> t7, App<F, T8> t8, App<F, T9> t9) {
            this.t1 = t1;
            this.t2 = t2;
            this.t3 = t3;
            this.t4 = t4;
            this.t5 = t5;
            this.t6 = t6;
            this.t7 = t7;
            this.t8 = t8;
            this.t9 = t9;
        }

        public App<F, T1> t1() {
            return t1;
        }

        public App<F, T2> t2() {
            return t2;
        }

        public App<F, T3> t3() {
            return t3;
        }

        public App<F, T4> t4() {
            return t4;
        }

        public App<F, T5> t5() {
            return t5;
        }

        public App<F, T6> t6() {
            return t6;
        }

        public App<F, T7> t7() {
            return t7;
        }

        public App<F, T8> t8() {
            return t8;
        }

        public App<F, T9> t9() {
            return t9;
        }

        public <R> App<F, R> apply(Applicative<F, ?> instance, com.mojang.datafixers.util.Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R> function) {
            return com.mojang.serialization.codecs.RecordCodecBuilder.horizonApply(instance, function, t1, t2, t3, t4, t5, t6, t7, t8, t9);
        }
    }

    final class P10<F extends K1, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> {
        private final App<F, T1> t1;
        private final App<F, T2> t2;
        private final App<F, T3> t3;
        private final App<F, T4> t4;
        private final App<F, T5> t5;
        private final App<F, T6> t6;
        private final App<F, T7> t7;
        private final App<F, T8> t8;
        private final App<F, T9> t9;
        private final App<F, T10> t10;

        public P10(App<F, T1> t1, App<F, T2> t2, App<F, T3> t3, App<F, T4> t4, App<F, T5> t5, App<F, T6> t6, App<F, T7> t7, App<F, T8> t8, App<F, T9> t9, App<F, T10> t10) {
            this.t1 = t1;
            this.t2 = t2;
            this.t3 = t3;
            this.t4 = t4;
            this.t5 = t5;
            this.t6 = t6;
            this.t7 = t7;
            this.t8 = t8;
            this.t9 = t9;
            this.t10 = t10;
        }

        public App<F, T1> t1() {
            return t1;
        }

        public App<F, T2> t2() {
            return t2;
        }

        public App<F, T3> t3() {
            return t3;
        }

        public App<F, T4> t4() {
            return t4;
        }

        public App<F, T5> t5() {
            return t5;
        }

        public App<F, T6> t6() {
            return t6;
        }

        public App<F, T7> t7() {
            return t7;
        }

        public App<F, T8> t8() {
            return t8;
        }

        public App<F, T9> t9() {
            return t9;
        }

        public App<F, T10> t10() {
            return t10;
        }

        public <R> App<F, R> apply(Applicative<F, ?> instance, com.mojang.datafixers.util.Function10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R> function) {
            return com.mojang.serialization.codecs.RecordCodecBuilder.horizonApply(instance, function, t1, t2, t3, t4, t5, t6, t7, t8, t9, t10);
        }
    }

    final class P11<F extends K1, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> {
        private final App<F, T1> t1;
        private final App<F, T2> t2;
        private final App<F, T3> t3;
        private final App<F, T4> t4;
        private final App<F, T5> t5;
        private final App<F, T6> t6;
        private final App<F, T7> t7;
        private final App<F, T8> t8;
        private final App<F, T9> t9;
        private final App<F, T10> t10;
        private final App<F, T11> t11;

        public P11(App<F, T1> t1, App<F, T2> t2, App<F, T3> t3, App<F, T4> t4, App<F, T5> t5, App<F, T6> t6, App<F, T7> t7, App<F, T8> t8, App<F, T9> t9, App<F, T10> t10, App<F, T11> t11) {
            this.t1 = t1;
            this.t2 = t2;
            this.t3 = t3;
            this.t4 = t4;
            this.t5 = t5;
            this.t6 = t6;
            this.t7 = t7;
            this.t8 = t8;
            this.t9 = t9;
            this.t10 = t10;
            this.t11 = t11;
        }

        public App<F, T1> t1() {
            return t1;
        }

        public App<F, T2> t2() {
            return t2;
        }

        public App<F, T3> t3() {
            return t3;
        }

        public App<F, T4> t4() {
            return t4;
        }

        public App<F, T5> t5() {
            return t5;
        }

        public App<F, T6> t6() {
            return t6;
        }

        public App<F, T7> t7() {
            return t7;
        }

        public App<F, T8> t8() {
            return t8;
        }

        public App<F, T9> t9() {
            return t9;
        }

        public App<F, T10> t10() {
            return t10;
        }

        public App<F, T11> t11() {
            return t11;
        }

        public <R> App<F, R> apply(Applicative<F, ?> instance, com.mojang.datafixers.util.Function11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R> function) {
            return com.mojang.serialization.codecs.RecordCodecBuilder.horizonApply(instance, function, t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11);
        }
    }

    final class P12<F extends K1, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> {
        private final App<F, T1> t1;
        private final App<F, T2> t2;
        private final App<F, T3> t3;
        private final App<F, T4> t4;
        private final App<F, T5> t5;
        private final App<F, T6> t6;
        private final App<F, T7> t7;
        private final App<F, T8> t8;
        private final App<F, T9> t9;
        private final App<F, T10> t10;
        private final App<F, T11> t11;
        private final App<F, T12> t12;

        public P12(App<F, T1> t1, App<F, T2> t2, App<F, T3> t3, App<F, T4> t4, App<F, T5> t5, App<F, T6> t6, App<F, T7> t7, App<F, T8> t8, App<F, T9> t9, App<F, T10> t10, App<F, T11> t11, App<F, T12> t12) {
            this.t1 = t1;
            this.t2 = t2;
            this.t3 = t3;
            this.t4 = t4;
            this.t5 = t5;
            this.t6 = t6;
            this.t7 = t7;
            this.t8 = t8;
            this.t9 = t9;
            this.t10 = t10;
            this.t11 = t11;
            this.t12 = t12;
        }

        public App<F, T1> t1() {
            return t1;
        }

        public App<F, T2> t2() {
            return t2;
        }

        public App<F, T3> t3() {
            return t3;
        }

        public App<F, T4> t4() {
            return t4;
        }

        public App<F, T5> t5() {
            return t5;
        }

        public App<F, T6> t6() {
            return t6;
        }

        public App<F, T7> t7() {
            return t7;
        }

        public App<F, T8> t8() {
            return t8;
        }

        public App<F, T9> t9() {
            return t9;
        }

        public App<F, T10> t10() {
            return t10;
        }

        public App<F, T11> t11() {
            return t11;
        }

        public App<F, T12> t12() {
            return t12;
        }

        public <R> App<F, R> apply(Applicative<F, ?> instance, com.mojang.datafixers.util.Function12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R> function) {
            return com.mojang.serialization.codecs.RecordCodecBuilder.horizonApply(instance, function, t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12);
        }
    }

    final class P13<F extends K1, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> {
        private final App<F, T1> t1;
        private final App<F, T2> t2;
        private final App<F, T3> t3;
        private final App<F, T4> t4;
        private final App<F, T5> t5;
        private final App<F, T6> t6;
        private final App<F, T7> t7;
        private final App<F, T8> t8;
        private final App<F, T9> t9;
        private final App<F, T10> t10;
        private final App<F, T11> t11;
        private final App<F, T12> t12;
        private final App<F, T13> t13;

        public P13(App<F, T1> t1, App<F, T2> t2, App<F, T3> t3, App<F, T4> t4, App<F, T5> t5, App<F, T6> t6, App<F, T7> t7, App<F, T8> t8, App<F, T9> t9, App<F, T10> t10, App<F, T11> t11, App<F, T12> t12, App<F, T13> t13) {
            this.t1 = t1;
            this.t2 = t2;
            this.t3 = t3;
            this.t4 = t4;
            this.t5 = t5;
            this.t6 = t6;
            this.t7 = t7;
            this.t8 = t8;
            this.t9 = t9;
            this.t10 = t10;
            this.t11 = t11;
            this.t12 = t12;
            this.t13 = t13;
        }

        public App<F, T1> t1() {
            return t1;
        }

        public App<F, T2> t2() {
            return t2;
        }

        public App<F, T3> t3() {
            return t3;
        }

        public App<F, T4> t4() {
            return t4;
        }

        public App<F, T5> t5() {
            return t5;
        }

        public App<F, T6> t6() {
            return t6;
        }

        public App<F, T7> t7() {
            return t7;
        }

        public App<F, T8> t8() {
            return t8;
        }

        public App<F, T9> t9() {
            return t9;
        }

        public App<F, T10> t10() {
            return t10;
        }

        public App<F, T11> t11() {
            return t11;
        }

        public App<F, T12> t12() {
            return t12;
        }

        public App<F, T13> t13() {
            return t13;
        }

        public <R> App<F, R> apply(Applicative<F, ?> instance, com.mojang.datafixers.util.Function13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R> function) {
            return com.mojang.serialization.codecs.RecordCodecBuilder.horizonApply(instance, function, t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13);
        }
    }

    final class P14<F extends K1, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> {
        private final App<F, T1> t1;
        private final App<F, T2> t2;
        private final App<F, T3> t3;
        private final App<F, T4> t4;
        private final App<F, T5> t5;
        private final App<F, T6> t6;
        private final App<F, T7> t7;
        private final App<F, T8> t8;
        private final App<F, T9> t9;
        private final App<F, T10> t10;
        private final App<F, T11> t11;
        private final App<F, T12> t12;
        private final App<F, T13> t13;
        private final App<F, T14> t14;

        public P14(App<F, T1> t1, App<F, T2> t2, App<F, T3> t3, App<F, T4> t4, App<F, T5> t5, App<F, T6> t6, App<F, T7> t7, App<F, T8> t8, App<F, T9> t9, App<F, T10> t10, App<F, T11> t11, App<F, T12> t12, App<F, T13> t13, App<F, T14> t14) {
            this.t1 = t1;
            this.t2 = t2;
            this.t3 = t3;
            this.t4 = t4;
            this.t5 = t5;
            this.t6 = t6;
            this.t7 = t7;
            this.t8 = t8;
            this.t9 = t9;
            this.t10 = t10;
            this.t11 = t11;
            this.t12 = t12;
            this.t13 = t13;
            this.t14 = t14;
        }

        public App<F, T1> t1() {
            return t1;
        }

        public App<F, T2> t2() {
            return t2;
        }

        public App<F, T3> t3() {
            return t3;
        }

        public App<F, T4> t4() {
            return t4;
        }

        public App<F, T5> t5() {
            return t5;
        }

        public App<F, T6> t6() {
            return t6;
        }

        public App<F, T7> t7() {
            return t7;
        }

        public App<F, T8> t8() {
            return t8;
        }

        public App<F, T9> t9() {
            return t9;
        }

        public App<F, T10> t10() {
            return t10;
        }

        public App<F, T11> t11() {
            return t11;
        }

        public App<F, T12> t12() {
            return t12;
        }

        public App<F, T13> t13() {
            return t13;
        }

        public App<F, T14> t14() {
            return t14;
        }

        public <R> App<F, R> apply(Applicative<F, ?> instance, com.mojang.datafixers.util.Function14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R> function) {
            return com.mojang.serialization.codecs.RecordCodecBuilder.horizonApply(instance, function, t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14);
        }
    }

    final class P15<F extends K1, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> {
        private final App<F, T1> t1;
        private final App<F, T2> t2;
        private final App<F, T3> t3;
        private final App<F, T4> t4;
        private final App<F, T5> t5;
        private final App<F, T6> t6;
        private final App<F, T7> t7;
        private final App<F, T8> t8;
        private final App<F, T9> t9;
        private final App<F, T10> t10;
        private final App<F, T11> t11;
        private final App<F, T12> t12;
        private final App<F, T13> t13;
        private final App<F, T14> t14;
        private final App<F, T15> t15;

        public P15(App<F, T1> t1, App<F, T2> t2, App<F, T3> t3, App<F, T4> t4, App<F, T5> t5, App<F, T6> t6, App<F, T7> t7, App<F, T8> t8, App<F, T9> t9, App<F, T10> t10, App<F, T11> t11, App<F, T12> t12, App<F, T13> t13, App<F, T14> t14, App<F, T15> t15) {
            this.t1 = t1;
            this.t2 = t2;
            this.t3 = t3;
            this.t4 = t4;
            this.t5 = t5;
            this.t6 = t6;
            this.t7 = t7;
            this.t8 = t8;
            this.t9 = t9;
            this.t10 = t10;
            this.t11 = t11;
            this.t12 = t12;
            this.t13 = t13;
            this.t14 = t14;
            this.t15 = t15;
        }

        public App<F, T1> t1() {
            return t1;
        }

        public App<F, T2> t2() {
            return t2;
        }

        public App<F, T3> t3() {
            return t3;
        }

        public App<F, T4> t4() {
            return t4;
        }

        public App<F, T5> t5() {
            return t5;
        }

        public App<F, T6> t6() {
            return t6;
        }

        public App<F, T7> t7() {
            return t7;
        }

        public App<F, T8> t8() {
            return t8;
        }

        public App<F, T9> t9() {
            return t9;
        }

        public App<F, T10> t10() {
            return t10;
        }

        public App<F, T11> t11() {
            return t11;
        }

        public App<F, T12> t12() {
            return t12;
        }

        public App<F, T13> t13() {
            return t13;
        }

        public App<F, T14> t14() {
            return t14;
        }

        public App<F, T15> t15() {
            return t15;
        }

        public <R> App<F, R> apply(Applicative<F, ?> instance, com.mojang.datafixers.util.Function15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R> function) {
            return com.mojang.serialization.codecs.RecordCodecBuilder.horizonApply(instance, function, t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15);
        }
    }

    final class P16<F extends K1, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> {
        private final App<F, T1> t1;
        private final App<F, T2> t2;
        private final App<F, T3> t3;
        private final App<F, T4> t4;
        private final App<F, T5> t5;
        private final App<F, T6> t6;
        private final App<F, T7> t7;
        private final App<F, T8> t8;
        private final App<F, T9> t9;
        private final App<F, T10> t10;
        private final App<F, T11> t11;
        private final App<F, T12> t12;
        private final App<F, T13> t13;
        private final App<F, T14> t14;
        private final App<F, T15> t15;
        private final App<F, T16> t16;

        public P16(App<F, T1> t1, App<F, T2> t2, App<F, T3> t3, App<F, T4> t4, App<F, T5> t5, App<F, T6> t6, App<F, T7> t7, App<F, T8> t8, App<F, T9> t9, App<F, T10> t10, App<F, T11> t11, App<F, T12> t12, App<F, T13> t13, App<F, T14> t14, App<F, T15> t15, App<F, T16> t16) {
            this.t1 = t1;
            this.t2 = t2;
            this.t3 = t3;
            this.t4 = t4;
            this.t5 = t5;
            this.t6 = t6;
            this.t7 = t7;
            this.t8 = t8;
            this.t9 = t9;
            this.t10 = t10;
            this.t11 = t11;
            this.t12 = t12;
            this.t13 = t13;
            this.t14 = t14;
            this.t15 = t15;
            this.t16 = t16;
        }

        public App<F, T1> t1() {
            return t1;
        }

        public App<F, T2> t2() {
            return t2;
        }

        public App<F, T3> t3() {
            return t3;
        }

        public App<F, T4> t4() {
            return t4;
        }

        public App<F, T5> t5() {
            return t5;
        }

        public App<F, T6> t6() {
            return t6;
        }

        public App<F, T7> t7() {
            return t7;
        }

        public App<F, T8> t8() {
            return t8;
        }

        public App<F, T9> t9() {
            return t9;
        }

        public App<F, T10> t10() {
            return t10;
        }

        public App<F, T11> t11() {
            return t11;
        }

        public App<F, T12> t12() {
            return t12;
        }

        public App<F, T13> t13() {
            return t13;
        }

        public App<F, T14> t14() {
            return t14;
        }

        public App<F, T15> t15() {
            return t15;
        }

        public App<F, T16> t16() {
            return t16;
        }

        public <R> App<F, R> apply(Applicative<F, ?> instance, com.mojang.datafixers.util.Function16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R> function) {
            return com.mojang.serialization.codecs.RecordCodecBuilder.horizonApply(instance, function, t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16);
        }
    }

}
