/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.java.lang.reflect;

import org.openjdk.bench.java.lang.ArrayCopy;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@EmptyAnnotation
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class GetAnnotation {

    public Class<?> c;
    public Map<Class<?>, Method[]> cmap;
    public Map<Class<?>, HashMap<Method, Annotation>> amap;
    public Annotation noAnnotation;
    public Method[] methods;

    @Setup
    public void setup() {
        c = ArrayCopy.class;
        cmap = new HashMap<>();
        amap = new HashMap<>();
        noAnnotation = GetAnnotation.class.getDeclaredAnnotations()[0];
        methods = c.getDeclaredMethods();
    }

    @Benchmark
    public void doReflection(Blackhole bh) {
        Method[] methods = c.getDeclaredMethods();
        for (Method m : methods) {
            bh.consume(getAnnotation(m, c));
        }
    }

    @Benchmark
    public void doReflectionCached(Blackhole bh) {
        Method[] methods = cmap.get(c);
        if (methods == null) {
            methods = c.getDeclaredMethods();
            cmap.put(c, methods);
        }
        for (Method m : methods) {
            bh.consume(getAnnotationCached(m, c));
        }
    }

    @Benchmark
    public void doReflectionNoMethodAlloc(Blackhole bh) {
        for (Method m : methods) {
            bh.consume(getAnnotation(m, c));
        }
    }

    @Benchmark
    public void doReflectionCachedNoMethodAlloc(Blackhole bh) {
        for (Method m : methods) {
            bh.consume(getAnnotationCached(m, c));
        }
    }

    @SuppressWarnings("unchecked")
    public Annotation getAnnotation(Method m, @SuppressWarnings("rawtypes") Class c) {
        return m.getAnnotation(c);
    }

    @SuppressWarnings("unchecked")
    public Annotation getAnnotationCached(Method m, @SuppressWarnings("rawtypes") Class c) {
        HashMap<Method, Annotation> map = amap.get(c);
        if (map == null) {
            map = new HashMap<>();
            amap.put(c, map);
        }

        Annotation a = map.get(m);
        if (a == null) {
            a = m.getAnnotation(c);

            if (a == null) {
                map.put(m, noAnnotation);
            }
        } else if (a == noAnnotation) {
            return null;
        }

        return a;
    }
}

