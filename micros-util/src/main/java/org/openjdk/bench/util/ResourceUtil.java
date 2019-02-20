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
package org.openjdk.bench.util;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ResourceUtil {

    public static boolean extractIfNewer(String zipResource) throws FileNotFoundException, IOException {
        return extractIfNewer(zipResource, "");
    }

    public static boolean extractIfNewer(String zipResource, String dstFolder) throws FileNotFoundException, IOException {
        Path root = Paths.get(dstFolder).toAbsolutePath();
        try ( ZipInputStream zis = new ZipInputStream(new BufferedInputStream(ResourceUtil.class.getClassLoader().getResourceAsStream(zipResource)))) {
            byte[] buf = new byte[4096];
            for (ZipEntry entry; (entry = zis.getNextEntry()) != null;) {
                Path dst = root.resolve(Paths.get(entry.getName()));
                if (!Files.exists(dst) || Files.getLastModifiedTime(dst).toMillis() != entry.getTime()) {
                    //System.out.println(dst);
                    if (entry.isDirectory()) {
                        Files.createDirectories(dst);
                    } else {
                        try ( OutputStream out = Files.newOutputStream(dst)) {
                            for (int len; (len = zis.read(buf)) > 0; out.write(buf, 0, len));
                        }
                    }
                    Files.setLastModifiedTime(dst, FileTime.fromMillis(entry.getTime()));
                } else {
                    return false;
                }
            }
            return true;
        }
    }

    public static void clearExtracted(String zipResource) throws FileNotFoundException, IOException {
        clearExtracted(zipResource, "");
    }

    public static void clearExtracted(String zipResource, String dstFolder) throws FileNotFoundException, IOException {
        Path root = Paths.get(dstFolder).toAbsolutePath();
        try ( ZipInputStream zis = new ZipInputStream(new BufferedInputStream(ResourceUtil.class.getClassLoader().getResourceAsStream(zipResource)))) {
            for (ZipEntry entry; (entry = zis.getNextEntry()) != null;) {
                Path dst = root.resolve(Paths.get(entry.getName()));
                if (Files.exists(dst)) {
                    //System.out.println(dst);
                    if (entry.isDirectory()) {
                        Files.walkFileTree(dst, DEL);
                    } else {
                        Files.delete(dst);
                    }
                }
            }
        }
    }

    private static final FileVisitor<Path> DEL = new SimpleFileVisitor<Path>() {

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path directory, IOException e) throws IOException {
            Files.delete(directory);
            return FileVisitResult.CONTINUE;
        }
    };
}
