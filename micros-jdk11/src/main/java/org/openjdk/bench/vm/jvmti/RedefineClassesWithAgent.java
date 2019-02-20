/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.vm.jvmti;

import org.openjdk.bench.util.InMemoryJavaCompiler;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.*;
import java.lang.instrument.*;
import java.lang.management.ManagementFactory;
import com.sun.tools.attach.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.*;

import org.openjdk.bench.util.*;
import org.openjdk.jmh.annotations.*;

// -Xlog:redefine+class+nmethod=debug is helpful when debugging
@State(Scope.Thread)
@Fork(jvmArgsAppend = {"-XX:CompileThreshold=100", "-Djdk.attach.allowAttachSelf=true", "-XX:+EnableDynamicAgentLoading"})
public class RedefineClassesWithAgent {

    @Param({"10", "50", "200"})
    public int numberOfClasses = 50;

    static byte[][] compiledClasses;
    static byte[][] newCompiledClasses;
    static int index = 0;

    static String B(int count) {
        return new String("public class B" + count + " {"
                + "   static int intField;"
                + "   public static void compiledMethod() { "
                + "       intField++;"
                + "   }"
                + "}");
    }

    static String newB(int count) {
        return new String("public class B" + count + " {"
                + "   public static void compiledMethod() { "
                + "       System.out.println(\"compiledMethod called " + count + "\");"
                + "   }"
                + "}");
    }

    // The agent jar is stored as a zip in the uber jar. It will be extracted into
    // the cwd in its own directory.
    private static final String agentZipInUberJar = "redefineagent-1.0-SNAPSHOT.zip";
    private static final String agentJarLocationFromZip = "redefineagent-1.0-SNAPSHOT/redefineagent-1.0-SNAPSHOT.jar";

    @Setup(Level.Trial)
    public void setupAgent() throws FileNotFoundException, IOException,
            AttachNotSupportedException, AgentLoadException, AgentInitializationException {
        boolean extracted = ResourceUtil.extractIfNewer(agentZipInUberJar);

        String thisVm = ManagementFactory.getRuntimeMXBean().getName();
        int p = thisVm.indexOf('@');
        String pid = thisVm.substring(0, p);

        VirtualMachine vm = VirtualMachine.attach(pid);
        vm.loadAgent(agentJarLocationFromZip, "");
        vm.detach();
    }

    @Setup
    public void setupClasses() throws Exception {
        compiledClasses = new byte[numberOfClasses][];
        newCompiledClasses = new byte[numberOfClasses][];
        for (int i = 0; i < numberOfClasses; i++) {
            compiledClasses[i] = InMemoryJavaCompiler.compile("B" + i, B(i));
            newCompiledClasses[i] = InMemoryJavaCompiler.compile("B" + i, B(i));
        }
    }

    static class RedefiningBenchLoader extends ClassLoader {

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (name.equals("B" + index)) {
                assert compiledClasses[index]  != null;
                return defineClass(name, compiledClasses[index] , 0, (compiledClasses[index]).length);
            } else {
                return super.findClass(name);
            }
        }
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @BenchmarkMode(Mode.AverageTime)
    public void benchmark(ThreadState t) throws Exception {
        assert t.defs != null && t.defs.length != 0;
        assert t.defs.length == numberOfClasses;
        RedefineClassHelper.instrumentation.redefineClasses(t.defs);
    }


    @State(Scope.Benchmark)
    public static class ThreadState {

        ClassDefinition[] defs;

        @Setup(Level.Invocation)
        public void setup() throws Exception {
//            long start = System.currentTimeMillis();
            ClassDefinition[] defs = new ClassDefinition[compiledClasses.length];

            RedefineClassesWithAgent.RedefiningBenchLoader loader = new RedefineClassesWithAgent.RedefiningBenchLoader();
            // Load and start all the classes.
            for (index = 0; index < compiledClasses.length; index++) {
                String name = new String("B" + index);
                Class c = loader.findClass(name);

                {
                    Object o = c.newInstance();
                    Method m = c.getMethod("compiledMethod");
                    IntStream.range(0, 2000).parallel().forEach(x -> {
                        try {
                            m.invoke(o);
                        } catch (Exception e) {
                            System.out.println("Exception = " + e);
                            assert true == false;
                        }
                    });
                }

                // Make class definition for redefinition
                defs[index] = new ClassDefinition(c, newCompiledClasses[index]);
                assert defs[index] != null;
            }
            this.defs = defs;
            assert this.defs != null && this.defs.length != 0;
//            long end = System.currentTimeMillis();
//            System.out.println("# Setup:" + (end - start));
        }

        @TearDown(Level.Invocation)
        public void tearDown() throws Exception {
            this.defs = null;
            System.gc();
        }
    }

    static void runCompiledMethodMethods(Class c, int count) throws Exception {
        // Run for a while so they compile.
        Object o = c.newInstance();
        Method m = c.getMethod("compiledMethod");
        for (int i = 0; i < count; i++) {
            m.invoke(o);
        }
    }
}
