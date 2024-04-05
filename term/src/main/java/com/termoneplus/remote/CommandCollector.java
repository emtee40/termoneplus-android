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

package com.termoneplus.remote;

import android.content.Context;
import android.os.RemoteException;

import com.termoneplus.v1.ICommand;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;

import androidx.annotation.NonNull;


/**
 * Functionality that replaces broadcast based "path collection".
 * New command collection is based on remote procedure calls to
 * set of trusted applications that extract command details.
 * Commands are stored as public list of pairs. Each pair describes
 * command as key and command information details application
 * and other command attributes.
 */
public class CommandCollector {
    public static final HashMap<String, CommandInfo> list = new HashMap<>();
    private int pending = 0;
    private OnCommandsConnectedListener callback;

    public static void writeCommandPath(@NonNull ArrayList<String> args, OutputStream out) {
        //noinspection SizeReplaceableByIsEmpty
        if (args.size() < 1) return;

        String cmd = args.get(0);
        CommandInfo info = list.get(cmd);
        if (info == null) return;

        info.getPath(cmd);
        if (info.path == null) return;

        PrintStream prn = new PrintStream(out);
        prn.println(info.path);
    }

    public static void writeCommandEnvironment(@NonNull ArrayList<String> args, OutputStream out) {
        //noinspection SizeReplaceableByIsEmpty
        if (args.size() < 1) return;

        String cmd = args.get(0);
        CommandInfo info = list.get(cmd);
        if (info == null) return;

        info.getEnv(cmd);
        if (info.env == null) return;

        PrintStream prn = new PrintStream(out);
        for (String item : info.env) {
            prn.println(item);
        }
    }

    public static void printExternalAliases(PrintStream out) {
        for (String app : TrustedApplications.list.keySet()) {
            ICommand remote = TrustedApplications.getRemote(app);
            if (remote == null) continue;

            String[] app_cmds;
            try {
                app_cmds = remote.getCommands();
            } catch (RemoteException ignore) {
                continue;
            }
            if (app_cmds == null) continue;

            for (String cmd : app_cmds) {
                CommandInfo info = list.get(cmd);
                if (info != null) continue;
                list.put(cmd, new CommandInfo(app));
            }
        }

        for (String cmd : list.keySet()) {
            out.println("alias " + cmd + "='cmdexec " + cmd + "'");
        }
        out.flush();
    }

    public void start(Context context) {
        pending = TrustedApplications.list.size();
        new Thread(() -> {
            for (String app : TrustedApplications.list.keySet()) {
                ICommand remote = TrustedApplications.getRemote(app);
                if (remote == null) {
                    boolean flag = TrustedApplications.bind(context, app, this::onApplicationConnectionNotification);
                    if (flag) continue;
                }
                onApplicationConnectionNotification();
            }
        }).start();
    }

    public void setOnCommandsConnectedListener(OnCommandsConnectedListener listener) {
        callback = listener;
    }

    private void onApplicationConnectionNotification() {
        if (--pending > 0) return;
        if (callback == null) return;
        callback.onCommandsConnected();
    }


    public interface OnCommandsConnectedListener {
        void onCommandsConnected();
    }


    private static class CommandInfo {
        private final String app;
        private String path;
        private String[] env;

        CommandInfo(String app) {
            this.app = app;
        }

        private void getPath(String cmd) {
            if (path != null) {
                File exe = new File(path);
                if (!exe.exists()) {
                    // path may change on trusted application upgrade/preinstallation
                    path = null;
                }
            }
            if (path != null) return;

            ICommand remote = TrustedApplications.getRemote(app);
            if (remote == null) return;

            {
                try {
                    // trust result
                    path = remote.getPath(cmd);
                } catch (RemoteException ignore) {
                }
            }
        }

        private void getEnv(String cmd) {
            if (env != null) return;

            ICommand remote = TrustedApplications.getRemote(app);
            if (remote == null) return;

            {
                try {
                    // trust result
                    env = remote.getEnvironment(cmd);
                } catch (RemoteException ignore) {
                }
            }
        }
    }
}
