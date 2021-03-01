/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.bench.java.lang;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * A micro-benchmark for every StrictMath API.
 *
 * @author Charlie Hunt
 */

@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 4, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(2)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
@State(Scope.Thread)
public class StrictMathBench15 {

    public int int1 = 1, int2 = 2, int42 = 42, int5 = 5;
    public long long1 = 1L, long2 = 2L, long747 = 747L, long13 = 13L;
    public float float1 = 1.0f, float2 = 2.0f, floatNegative99 = -99.0f, float7 = 7.0f, eFloat = 2.718f;
    public double double1 = 1.0d, double2 = 2.0d, double81 = 81.0d, doubleNegative12 = -12.0d, double4Dot1 = 4.1d;


    @Benchmark
    public int  absExactInt() {
        return  StrictMath.absExact(int2);
    }

    @Benchmark
    public long  absExactLong() {
        return  StrictMath.absExact(long2);
    }


    @Benchmark
    public int  DecrementExactInt() {
        return  StrictMath.decrementExact(int42);
    }

    @Benchmark
    public long  DecrementExactLong() {
        return  StrictMath.decrementExact(long747);
    }


    @Benchmark
    public int  IncrementExactInt() {
        return  StrictMath.incrementExact(int42);
    }

    @Benchmark
    public long  IncrementExactLong() {
        return  StrictMath.incrementExact(long747);
    }


    @Benchmark
    public int  negateExactInt() {
        return  StrictMath.negateExact(int42);
    }

    @Benchmark
    public long  negateExactLong() {
        return  StrictMath.negateExact(long747);
    }

}
