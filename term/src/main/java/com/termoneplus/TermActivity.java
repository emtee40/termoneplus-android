/*
 * Copyright (C) 2018-2021 Roumen Petrov.  All rights reserved.
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

package com.termoneplus;

import android.content.SharedPreferences;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.termoneplus.utils.ThemeManager;
import com.termoneplus.utils.WrapOpenURL;


public class TermActivity extends jackpal.androidterm.Term {

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.setHeaderTitle(R.string.edit_text);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_session, menu);
        if (!canPaste()) {
            MenuItem item = menu.findItem(R.id.session_paste);
            if (item != null) item.setEnabled(false);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int id = item.getItemId();
        /* NOTE: Resource IDs will be non-final in Android Gradle Plugin version 5.0,
           avoid using them in switch case statements */
        if (id == R.id.session_select_text)
            getCurrentEmulatorView().toggleSelectingText();
        else if (id == R.id.session_copy_all)
            doCopyAll();
        else if (id == R.id.session_paste)
            doPaste();
        else if (id == R.id.session_send_cntr)
            getCurrentEmulatorView().sendControlKey();
        else if (id == R.id.session_send_fn)
            getCurrentEmulatorView().sendFnKey();
        else
            return super.onContextItemSelected(item);
        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // do not process preference "Theme Mode"
        if (ThemeManager.PREF_THEME_MODE.equals(key)) return;

        super.onSharedPreferenceChanged(sharedPreferences, key);
    }

    public void onAppIconClicked(View view) {
        WrapOpenURL.launch(this, urlApplicationSite());
    }

    public void onAppTitleClicked(View view) {
        WrapOpenURL.launch(this, urlApplicationSite());
    }

    public void onEmailAddressClicked(View view) {
        WrapOpenURL.launch(this, urlApplicationMail());
    }

    @Override
    protected void updatePrefs() {
        Integer theme_resid = getThemeId();
        if (theme_resid != null) {
            if (theme_resid != ThemeManager.presetTheme(this, false, theme_resid)) {
                restart(R.string.restart_thememode_change);
                return;
            }
        }
        super.updatePrefs();
    }

    private String urlApplicationSite() {
        return getResources().getString(R.string.application_site);
    }

    private String urlApplicationMail() {
        return "mailto:" + getResources().getString(R.string.application_email);
    }
}
