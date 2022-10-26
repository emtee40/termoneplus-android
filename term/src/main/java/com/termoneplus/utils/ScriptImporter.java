/*
 * Copyright (C) 2021-2022 Roumen Petrov.  All rights reserved.
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

package com.termoneplus.utils;

import android.net.Uri;
import android.view.Gravity;
import android.widget.Toast;

import com.termoneplus.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import androidx.appcompat.app.AppCompatActivity;
import jackpal.androidterm.emulatorview.TermSession;


public class ScriptImporter {

    public static void paste(AppCompatActivity activity, Uri uri, TermSession session) {
        if (uri == null) return;

        new Thread(() -> {
            try {
                InputStream inraw = activity.getContentResolver().openInputStream(uri);
                if (inraw == null) throw new IOException("null script input stream");

                copyStream(inraw, session.getTermOut());
            } catch (IOException e) {
                activity.runOnUiThread(() -> {
                    Toast toast = Toast.makeText(activity.getApplicationContext(),
                            R.string.script_import_error, Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.TOP, 0, 0);
                    toast.show();
                });
                e.printStackTrace();
            }
        }).start();
    }

    private static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[4 * 1024];
        while (true) {
            int count = in.read(buf, 0, buf.length);
            if (count < 0) break;
            out.write(buf, 0, count);
        }
        out.flush();
    }
}
