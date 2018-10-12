/*
 * Copyright (c) 2018 LingoChamp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liulishuo.okdownload;

import com.liulishuo.okdownload.core.connection.DownloadConnection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;

import javax.net.ssl.HttpsURLConnection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RedirectUtilTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void isRedirect() {
        assertTrue(RedirectUtil.isRedirect(HttpURLConnection.HTTP_MOVED_PERM));
        assertTrue(RedirectUtil.isRedirect(HttpURLConnection.HTTP_MOVED_TEMP));
        assertTrue(RedirectUtil.isRedirect(HttpURLConnection.HTTP_SEE_OTHER));
        assertTrue(RedirectUtil.isRedirect(HttpURLConnection.HTTP_MULT_CHOICE));
        assertTrue(RedirectUtil.isRedirect(RedirectUtil.HTTP_TEMPORARY_REDIRECT));
        assertTrue(RedirectUtil.isRedirect(RedirectUtil.HTTP_PERMANENT_REDIRECT));
        assertFalse(RedirectUtil.isRedirect(HttpsURLConnection.HTTP_ACCEPTED));
    }

    @Test
    public void getRedirectedUrl_thrownProtocolException() throws IOException {
        thrown.expect(ProtocolException.class);
        thrown.expectMessage("Response code is 302 but can't find Location field");
        final DownloadConnection.Connected connected = mock(DownloadConnection.Connected.class);
        when(connected.getResponseHeaderField("Location")).thenReturn(null);
        RedirectUtil.getRedirectedUrl(connected, 302);
    }

    @Test
    public void getRedirectUrl() throws IOException {
        final String redirectUrl = "http://redirect";
        final DownloadConnection.Connected connected = mock(DownloadConnection.Connected.class);
        when(connected.getResponseHeaderField("Location")).thenReturn(redirectUrl);
        assertEquals(RedirectUtil.getRedirectedUrl(connected, 302), redirectUrl);
    }

}
