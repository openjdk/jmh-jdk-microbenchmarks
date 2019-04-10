/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package org.openjdk.bench.vm.runtime;

import org.openjdk.bench.util.InMemoryJavaCompiler;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

// This benchmark is related to JDK-8059357

@State(Scope.Thread)
public class VerifySignatures {

    static final int numberOfClasses = 1;

    static byte[][] compiledClasses;
    static int index = 0;

    static final String targetClassName = "VerifySignaturesTarget";

    // The target class has a method with many call sites to the same methods
    // with many parameters.
    static final String baseString
            = "class " + targetClassName + " {"
            + "  byte w = 5;"
            + "  int x = 1;"
            + "  long y = 2;"
            + "  short z = 3;"
            + "  String s = \"foo\";"
            + "  Boolean bb = false;"
            + "  Long ll = new Long(-1);"
            + "  Character cc = 'a';"
            + "  Integer ii = 6;"
            + " "
            + "public boolean method0(byte w, int x, long y, short z, String s, Boolean bo, Long ll, Character cc, Integer ii,"
            + "        byte bb2, int xx2, long yy2, short zz2, String ss2, Boolean bobo, Long ll2, Character cc2, Integer ii2 ) {"
            + "    return true;"
            + "}"
            + " "
            + "public boolean method1(int x, long y, short z, String s, Boolean bo, Long ll, Character cc, Integer ii,"
            + "        byte bb2, int xx2, long yy2, short zz2, String ss2, Boolean bobo, Long ll2, Character cc2, Integer ii2 ) {"
            + "    return true;"
            + "}"
            + " "
            + "public boolean doIt( ) {"
            + "     return "
            + "           method0(w, x, y, z, s, bb, ll, cc, ii, w, x, y, z, s, bb, ll, cc, ii)"
            + "           | method1(x, y, z, s, bb, ll, cc, ii, w, x, y, z, s, bb, ll, cc, ii)"
            + "           | method0(w, x, y, z, s, bb, ll, cc, ii, w, x, y, z, s, bb, ll, cc, ii)"
            + "           | method1(x, y, z, s, bb, ll, cc, ii, w, x, y, z, s, bb, ll, cc, ii)"
            + "           "
            + "           | method0(w, x, y, z, s, bb, ll, cc, ii, w, x, y, z, s, bb, ll, cc, ii)"
            + "           | method1(x, y, z, s, bb, ll, cc, ii, w, x, y, z, s, bb, ll, cc, ii)"
            + "           "
            + "           | method0(w, x, y, z, s, bb, ll, cc, ii, w, x, y, z, s, bb, ll, cc, ii)"
            + "           | method1(x, y, z, s, bb, ll, cc, ii, w, x, y, z, s, bb, ll, cc, ii)"
            + "           "
            + "           | method0(w, x, y, z, s, bb, ll, cc, ii, w, x, y, z, s, bb, ll, cc, ii)"
            + "           | method1(x, y, z, s, bb, ll, cc, ii, w, x, y, z, s, bb, ll, cc, ii)"
            + "           "
            + "           | method0(w, x, y, z, s, bb, ll, cc, ii, w, x, y, z, s, bb, ll, cc, ii)"
            + "           | method1(x, y, z, s, bb, ll, cc, ii, w, x, y, z, s, bb, ll, cc, ii)"
            + "           | method0(w, x, y, z, s, bb, ll, cc, ii, w, x, y, z, s, bb, ll, cc, ii)"
            + "           | method1(x, y, z, s, bb, ll, cc, ii, w, x, y, z, s, bb, ll, cc, ii)"
            + "           "
            + "           | method0(w, x, y, z, s, bb, ll, cc, ii, w, x, y, z, s, bb, ll, cc, ii)"
            + "           | method1(x, y, z, s, bb, ll, cc, ii, w, x, y, z, s, bb, ll, cc, ii)"
            + "           "
            + "           | method0(w, x, y, z, s, bb, ll, cc, ii, w, x, y, z, s, bb, ll, cc, ii)"
            + "           | method1(x, y, z, s, bb, ll, cc, ii, w, x, y, z, s, bb, ll, cc, ii)"
            + "           "
            + "           | method0(w, x, y, z, s, bb, ll, cc, ii, w, x, y, z, s, bb, ll, cc, ii)"
            + "           | method1(x, y, z, s, bb, ll, cc, ii, w, x, y, z, s, bb, ll, cc, ii)"
            + "           "
            + "           | method0(w, x, y, z, s, bb, ll, cc, ii, w, x, y, z, s, bb, ll, cc, ii)"
            + "           | method1(x, y, z, s, bb, ll, cc, ii, w, x, y, z, s, bb, ll, cc, ii)"
            + "           "
            + "           | method0(w, x, y, z, s, bb, ll, cc, ii, w, x, y, z, s, bb, ll, cc, ii)"
            + "           | method1(x, y, z, s, bb, ll, cc, ii, w, x, y, z, s, bb, ll, cc, ii);"
            + "}"
            + "}";

    @Setup
    public void setupClasses() throws Exception {
        compiledClasses = new byte[numberOfClasses][];
        for (int i = 0; i < numberOfClasses; i++) {
            compiledClasses[i] = InMemoryJavaCompiler.compile(targetClassName, baseString);
        }
    }

    static class BenchLoader extends ClassLoader {

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            assert compiledClasses[index] != null;
            return defineClass(name, compiledClasses[index], 0, (compiledClasses[index]).length);
        }
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public Class measureLoadAndVerify() throws Exception {
        BenchLoader loader = new BenchLoader();
        Class c = Class.forName(targetClassName, true, loader);
        return c;
    }

}
