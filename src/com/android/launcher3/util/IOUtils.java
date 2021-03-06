/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.util;

import android.database.Cursor;

import com.android.launcher3.Utilities;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Supports various IO utility functions
 */
public class IOUtils {

    private static final int BUF_SIZE = 0x1000; // 4K

    public static byte[] toByteArray(File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            return toByteArray(in);
        }
    }

    public static byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(in, out);
        return out.toByteArray();
    }

    public static long copy(InputStream from, OutputStream to) throws IOException {
        byte[] buf = new byte[BUF_SIZE];
        long total = 0;
        int r;
        while ((r = from.read(buf)) != -1) {
            to.write(buf, 0, r);
            total += r;
        }
        return total;
    }

    public static boolean copy(File from, File to) {
        if (!from.exists()) {
            return false;
        }
        if (!from.isFile()) {
            return false;
        }
        if (!from.canRead()) {
            return false;
        }
        if (!to.getParentFile().exists()) {
            to.getParentFile().mkdirs();
        }
        if (to.exists()) {
            to.delete();
        }
        java.io.FileInputStream fosfrom = null;
        java.io.FileOutputStream fosto = null;
        try {
            fosfrom = new java.io.FileInputStream(
                    from);

            fosto = new FileOutputStream(to);

            byte bt[] = new byte[1024];

            int c;

            while ((c = fosfrom.read(bt)) > 0) {

                fosto.write(bt, 0, c); // 将内容写到新文件当中

            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (fosfrom != null) {
                    fosfrom.close();
                }
                if (fosto != null) {
                    fosto.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }



    public static void close(Cursor cursor) {
        if (Utilities.isNotNull(cursor)) {
            cursor.close();
        }
    }

    public static void close(InputStream is) {
        try {
            if (Utilities.isNotNull(is)) {
                is.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void close(OutputStream os) {
        try {
            if (Utilities.isNotNull(os)) {
                os.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
