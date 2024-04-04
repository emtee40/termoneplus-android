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

#include <sys/types.h>
#include <sys/stat.h>
#include <memory.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>

#include "appinfo.h"

extern char *uid;


const char *MSG_END = "<eol>\n";
#define MSG_ENDLEN      (5/*<eol>*/+ 1/*\n*/)


/* get path to executable
 * - request
 *   get cmd_path
 *   [command]
 *   <eol>
 * - response
 *   [path to executable]
 *   <eol>
 */
static char *
get_binary(const char *cmd) {
    char buf[4096];
    int sock;
    char *path = NULL;
    size_t path_len = 0;

    sock = open_appsocket();
    if (sock == -1) return NULL;

    {
        char msg[1024];
        int msgres;

        msgres = snprintf(msg, sizeof(msg), "get cmd_path\n%s\n%s", cmd, MSG_END);
        if (msgres < 0) {
            fprintf(stderr, "binary: snprintf() fail: %d(%s)\n", errno, strerror(errno));
            goto done;
        }
        if (msgres >= sizeof(msg)) {
            fprintf(stderr, "binary: too long command\n");
            goto done;
        }
        if (!write_msg(sock, msg)) goto done;
    }

    while (1) {
        size_t len;
        int read_errno;

        errno = 0;
        len = atomicio(read, sock, buf, sizeof(buf));
        read_errno = errno;
        if (len > 0) {
            path = realloc(path, path_len + len);
            if (path == NULL) {
                fprintf(stderr, "binary: out of memory\n");
                goto done;
            }
            memmove(path + path_len, buf, len);
            path_len += len;
        }
        if (read_errno == EPIPE) break;
    }
    if (path == NULL) {
        fprintf(stderr, "binary: missing path\n");
        goto done;
    }
    if (path_len <= MSG_ENDLEN) {
        fprintf(stderr, "binary: unfinished response\n");
        goto err;
    }
    path_len -= MSG_ENDLEN;
    if (strncmp(path + path_len, MSG_END, MSG_ENDLEN) != 0) {
        fprintf(stderr, "binary: incorrect response end\n");
        goto err;
    }
    --path_len; /*end of line ?*/
    if (path[path_len] != '\n') {
        fprintf(stderr, "binary: incorrect response path end\n");
        goto err;
    }
    path[path_len] = '\0';
    if (strlen(path) < strlen(cmd)) {
        fprintf(stderr, "binary: path too short\n");
        goto err;
    }

    done:
    close(sock);
    return path;

    err:
    free(path);
    close(sock);
    return NULL;
}


#include <sysexits.h>

int
main(int argc, char *argv[]) {
    char *cmd;

    /* arguments:
     * 1 - uid
     * 2 - command name
     * 3+ - command arguments
     */
    if (argc < 3) exit(EX_USAGE);

    uid = argv[1];

    cmd = get_binary(argv[2]);
    if (cmd == NULL)
        return EX_SOFTWARE;

    argv += 2;/*skip "wrapper" command and uid*/;

    (void) execv(cmd, argv);
    return EX_SOFTWARE;
}
