/*
 * Copyright (C) 2023-2024 Roumen Petrov.  All rights reserved.
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

package com.termoneplus.sample.addon;

import android.content.Context;

import java.io.File;


public class Application extends android.app.Application {
    // paths
    public static File rootdir;
    public static File etcdir;
    public static File xbindir;
    public static File libdir;

    private static void setPaths(Context context) {
        if (rootdir != null) return;

        rootdir = context.getFilesDir().getParentFile();
        etcdir = new File(rootdir, "etc");
        libdir = new File(context.getApplicationInfo().nativeLibraryDir);
        xbindir = libdir;
    }


    public void onCreate() {
        super.onCreate();

        setPaths(this);
    }
}
