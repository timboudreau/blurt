/*
 * The MIT License
 *
 * Copyright 2015 Tim Boudreau.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.mastfrog.blurt.collector;

import com.google.inject.Inject;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.ActeurFactory;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.Page;
import static com.mastfrog.acteur.headers.Headers.CONTENT_TYPE;
import com.mastfrog.acteur.headers.Method;
import com.mastfrog.mime.MimeType;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

/**
 *
 * @author Tim Boudreau
 */
public class StreamResource extends Page {

    @Inject
    StreamResource(ActeurFactory af) {
        add(af.matchMethods(Method.GET));
        add(af.matchPath("^stream$"));
        add(StreamActeur.class);
    }

    private static final class StreamActeur extends Acteur implements ChannelFutureListener {

        private final Publisher publisher;

        @Inject
        StreamActeur(Publisher publisher, Page page, HttpEvent evt) {
            setChunked(false);
            add(CONTENT_TYPE, MimeType.EVENT_STREAM);
            setState(new RespondWith(OK));
            setResponseBodyWriter(this);
            this.publisher = publisher;
        }

        @Override
        public void operationComplete(ChannelFuture f) throws Exception {
            publisher.subscribe(f.channel());
        }
    }
}
