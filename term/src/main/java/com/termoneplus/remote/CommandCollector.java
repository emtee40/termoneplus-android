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

import com.termoneplus.v1.ICommand;

import java.util.HashMap;


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

        CommandInfo(String app) {
            this.app = app;
        }
    }
}
