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
package org.openjdk.bench.java.io;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A microbenchmark testing the throughput of ObjectInputStream
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class Serial {

    private static final int N_BUNDLES = 5000;
    private static final int N_OBJECTS = 30;
    private static final int SIZE_ESTIMATE = 32;

    public Object[] objs;
    public Object[] ress;

    public UnsyncByteArrayOutputStream bout;
    public UnsyncByteArrayInputStream bin;

    @Setup(Level.Iteration)
    public void setup() {
        objs = new Object[N_BUNDLES];
        ress = new Object[N_BUNDLES];

        ObjectFactory factory = new ObjectFactory();
        for (int i = 0; i < N_BUNDLES; i++) {
            objs[i] = factory.newInstance(N_OBJECTS);
        }

        bout = new UnsyncByteArrayOutputStream(N_BUNDLES * SIZE_ESTIMATE * N_OBJECTS);
        bin = new UnsyncByteArrayInputStream();
    }

    @TearDown(Level.Iteration)
    public void downStreams() {
        bout = null;
        bin = null;
    }

    @Benchmark
    @OperationsPerInvocation(N_BUNDLES * SIZE_ESTIMATE * N_OBJECTS)
    public void test() throws IOException, ClassNotFoundException {
        ObjectOutputStream oos = new ObjectOutputStream(bout);
        for (int i = 0; i < N_BUNDLES; i++) {
            oos.writeObject(objs[i]);
        }

        byte[] b = bout.getByteArray();
        bin.setBytes(b, 0, b.length);

        ObjectInputStream oin = new ObjectInputStream(bin);

        for (int i = 0; i < N_BUNDLES; i++) {
            ress[i] = oin.readObject();
        }
        bout.reset();
    }

    public static class UnsyncByteArrayOutputStream extends OutputStream {
        protected byte[] buf;

        protected int count;

        public UnsyncByteArrayOutputStream(int size) {
            if (size < 0) {
                throw new IllegalArgumentException("Negative initial size: " + size);
            }
            buf = new byte[size];
        }

        public void write(int b) {
            int newcount = count + 1;
            if (newcount > buf.length) {
                byte newbuf[] = new byte[Math.max(buf.length << 1, newcount)];
                System.arraycopy(buf, 0, newbuf, 0, count);
                buf = newbuf;
            }
            buf[count] = (byte) b;
            count = newcount;
        }

        public void write(byte b[], int off, int len) {
            if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return;
            }
            int newcount = count + len;
            if (newcount > buf.length) {
                byte newbuf[] = new byte[Math.max(buf.length << 1, newcount)];
                System.arraycopy(buf, 0, newbuf, 0, count);
                buf = newbuf;
            }
            System.arraycopy(b, off, buf, count, len);
            count = newcount;
        }

        public void reset() {
            count = 0;
        }

        public byte[] getByteArray() {
            return buf;
        }

        public int size() {
            return count;
        }

        public String toString() {
            return new String(buf, 0, count);
        }

        public String toString(String enc) throws UnsupportedEncodingException {
            return new String(buf, 0, count, enc);
        }

        @SuppressWarnings("deprecation")
        public String toString(int hibyte) {
            return new String(buf, hibyte, 0, count);
        }

        public void close() throws IOException {
        }
    }

    public class UnsyncByteArrayInputStream extends InputStream {
        protected byte buf[];

        protected int pos;

        protected int mark = 0;

        protected int count;

        public UnsyncByteArrayInputStream() {
            this.buf = null;
            this.pos = 0;
            this.count = 0;
        }

        public void setBytes(byte buf[], int off, int len) {
            this.buf = buf;
            this.pos = off;
            this.count = Math.min(off + len, buf.length);
        }

        public int read() {
            return (pos < count) ? (buf[pos++] & 0xff) : -1;
        }

        public int read(byte[] b, int off, int len) {
            if (b == null) {
                throw new NullPointerException();
            } else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            }
            if (pos >= count) {
                return -1;
            }
            if (pos + len > count) {
                len = count - pos;
            }
            if (len <= 0) {
                return 0;
            }
            System.arraycopy(buf, pos, b, off, len);
            pos += len;
            return len;
        }

        public long skip(long n) {
            if (pos + n > count) {
                n = count - pos;
            }
            if (n < 0) {
                return 0;
            }
            pos += n;
            return n;
        }

        public int available() {
            return count - pos;
        }

        public boolean markSupported() {
            return true;
        }

        public void mark(int readAheadLimit) {
            mark = pos;
        }

        public void reset() {
            pos = mark;
        }

        public void close() throws IOException {
        }
    }

    public static class ObjectFactory {
        private static int indexC = 0;

        @SuppressWarnings("serial")
        public static class Lus implements Serializable {
            String str1;

            String str2;

            int index;

            int i1, i2, i3;

            short s1, s2;

            char c1;

            byte b1;

            double d1, d2;

            float f1, f2;

            public Lus() {
                str1 = new String("kossa");
                str2 = str1 + System.currentTimeMillis();

                index = indexC++;

                i1 = str2.hashCode();
                i2 = i1 - System.identityHashCode(str1);
                i3 = i1 % (i2 + 1);
                s1 = (short) str2.length();
                s2 = (short) (s1 - i3);
                c1 = str2.charAt(s1 - 1);
                b1 = (byte) (c1 % (i3 + 1));
                d1 = i1 * b1;
                d2 = i2 / (b1 + 1);
                f1 = (float) d1 / (s1 + 1);
                f2 = (float) d2 * s2;
            }

            public String toString() {
                return getClass().toString() + " ['" + str1 + "' '" + str2 + ", " + index + ", " +
                        i1 + ", " + i2 + ", " + i3 + ", " + s1 + ", " + s2 + ", " + c1 + ", " + b1 + ", " +
                        d1 + ", " + d2 + ", " + f1 + ", " + f2 + ']';
            }

            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }

                if (o instanceof Lus) {
                    Lus m = (Lus) o;

                    try {
                        if (str1.equals(m.str1) && str2.equals(m.str2) && index == m.index && i1 == m.i1 && i2 == m.i2
                                && i3 == m.i3 && s1 == m.s1 && s2 == m.s2 && c1 == m.c1 && b1 == m.b1 && d1 == m.d1
                                && d2 == m.d2 && f1 == m.f1 && f2 == m.f2) {
                            return true;
                        }
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }
        }

        public static class Ko extends Lus implements Externalizable {
            private Lus l1, l2;

            public boolean equals(Object o) {
                if (super.equals(o)) {
                    if (o instanceof Ko) {
                        Ko k = (Ko) o;
                        return (l1 == k.l1 || l1.equals(k.l1)) && (l2 == k.l2 || l2.equals(k.l2));
                    }
                }
                return false;
            }

            public String toString() {
                return "[" + super.toString() + " \n\t" + l1 + " \n\t" + l2 + "\n]";
            }

            public Ko() {
                l1 = null;
                l2 = null;
            }

            public Ko(Lus l) {
                l1 = l;
                l2 = new Lus();
            }

            public Ko(Lus l1, Lus l2) {
                this.l1 = l1;
                this.l2 = l2;
            }

            public void writeExternal(ObjectOutput out) throws IOException {
                out.writeUTF(str1);
                out.writeObject(str2);
                out.writeInt(index);
                out.writeInt(i1);
                out.writeInt(i2);
                out.writeInt(i3);
                out.writeShort(s1);
                out.writeShort(s2);
                out.writeChar(c1);
                out.writeByte(b1);
                out.writeDouble(d1);
                out.writeDouble(d2);
                out.writeFloat(f1);
                out.writeFloat(f2);
                out.writeObject(l1);
                out.writeObject(l2);
            }

            public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
                str1 = in.readUTF();
                str2 = (String) in.readObject();
                index = in.readInt();
                i1 = in.readInt();
                i2 = in.readInt();
                i3 = in.readInt();
                s1 = in.readShort();
                s2 = in.readShort();
                c1 = in.readChar();
                b1 = in.readByte();
                d1 = in.readDouble();
                d2 = in.readDouble();
                f1 = in.readFloat();
                f2 = in.readFloat();
                l1 = (Lus) in.readObject();
                l2 = (Lus) in.readObject();
            }
        }

        @SuppressWarnings("serial")
        public static class Bonde extends Lus {
            private transient Lus l1, l2;

            public Bonde(Lus l) {
                l1 = l;
                l2 = new Lus();
            }

            public Bonde(Lus l1, Lus l2) {
                this.l1 = l1;
                this.l2 = l2;
            }

            public boolean equals(Object o) {
                if (super.equals(o)) {
                    if (o instanceof Bonde) {
                        Bonde k = (Bonde) o;

                        return (l1 == k.l1 || l1.equals(k.l1)) && (l2 == k.l2 || l2.equals(k.l2));
                    }
                }
                return false;
            }

            public String toString() {
                return "[" + super.toString() + " \n\t" + l1 + " \n\t" + l2 + "\n]";
            }

            private void writeObject(ObjectOutputStream stream) throws IOException {
                stream.defaultWriteObject();
                stream.writeObject(l1);
                stream.writeObject(l2);
            }

            private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
                stream.defaultReadObject();
                l1 = (Lus) stream.readObject();
                l2 = (Lus) stream.readObject();
            }
        }

        public Object[] newInstance(int size) {
            Lus res[] = new Lus[size];
            size--;
            int n = size;
            size--;

            res[size--] = new Lus();
            res[size--] = new Bonde(res[size]);
            res[size--] = new Ko(res[size]);
            res[n] = new Ko(res[size], res[n - 1]);

            for (int i = size; i >= 0; i--) {
                if ((i % 2) == 0) {
                    res[size] = new Ko(res[i + 4], res[i + 1]);
                } else {
                    res[size] = new Bonde(res[i + 2], res[i + 3]);
                }
            }
            List<Lus> v = new ArrayList<>();
            Map<String, Lus> t = new HashMap<>();

            for (Lus re : res) {
                if (re != null) {
                    v.add(re);
                    t.put(re.toString(), re);
                }
            }

            return new Object[]{v, t};
        }
    }

}
