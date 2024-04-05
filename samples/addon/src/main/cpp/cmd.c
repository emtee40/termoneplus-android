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

#include <stdio.h>
#include <sysexits.h>
#include <string.h>
#include <stdlib.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>

extern char **environ;

static
const char *android_system_env[] = {
        "ANDROID_BOOTLOGO",
        "ANDROID_ROOT",
        "ANDROID_ASSETS",
        "ANDROID_DATA",
        "ANDROID_STORAGE",
        "ANDROID_ART_ROOT",
        "ANDROID_I18N_ROOT",
        "ANDROID_TZDATA_ROOT",
        "EXTERNAL_STORAGE",
        "ASEC_MOUNTPOINT",
        "BOOTCLASSPATH",
        "DEX2OATBOOTCLASSPATH",
        "SYSTEMSERVERCLASSPATH",
        /*Zygote defaults*/
        "STANDALONE_SYSTEMSERVER_JARS",
        "ANDROID_SOCKET_zygote",
        "ANDROID_SOCKET_zygote_secondary",
        "ANDROID_SOCKET_usap_pool_primary",
        "ANDROID_SOCKET_usap_pool_secondary",
        /*obsolete?*/
        "ANDROID_RUNTIME_ROOT",
        "LOOP_MOUNTPOINT",
        "ANDROID_PROPERTY_WORKSPACE",
        "SECONDARY_STORAGE",
        "SD_EXT_DIRECTORY"
};

static int/*bool*/
is_system_env(char *env) {
    int k;
    for (k = 0; k < sizeof(android_system_env) / sizeof(android_system_env[0]); k++) {
        const char *s = android_system_env[k];
        size_t l = strlen(s);
        if ((strncmp(env, s, l) == 0) && (env[l] == '='))
            return 1;
    }
    return 0;
}

int
main(int argc, char *argv[]/*, char *envp[]*/) {
    int k;
    char **env;

    printf("Hello world!\n");
    for (k = 0; k < argc; k++) {
        printf("arg[%d]: '%s'\n", k, argv[k]);
    }
    for (env = environ; *env != NULL; env++) {
        /*exclude "system" environment for demo command*/
        if (is_system_env(*env)) continue;
        printf("env: '%s'\n", *env);
    }
    {
        char *conf = getenv("ADDON_CONF");
        if (conf != NULL) {
            int fd = open(conf, O_CLOEXEC);
            if (fd == -1)
                fprintf(stderr, "open fail: %s\n", strerror(errno));
            else {
                printf("conf '%s':\n", conf);
                while (1) {
                    char buf[4096];
                    ssize_t len = read(fd, buf, sizeof(buf));
                    if (len <= 0) break;
                    write(STDOUT_FILENO, buf, len);
                }
                fsync(STDOUT_FILENO);
                close(fd);
            }
        }
    }

    return EX_OK;
}
