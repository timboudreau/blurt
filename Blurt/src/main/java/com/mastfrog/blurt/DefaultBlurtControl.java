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
package com.mastfrog.blurt;

import com.google.inject.Inject;
import com.mastfrog.util.ConfigurationError;
import java.io.IOException;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;

/**
 *
 * @author Tim Boudreau
 */
class DefaultBlurtControl implements BlurtControl {
    private final BlurtControl delegate;
    @Inject
    DefaultBlurtControl(Blurt blurt) {
        if (!(blurt instanceof BlurtControl)) {
            throw new ConfigurationError(blurt + " does not implement BlurtControl - cannot use it");
        }
        delegate = (BlurtControl) blurt;
    }

    @Override
    public void stop() throws IOException, InterruptedException {
        delegate.stop();
    }

    @Override
    public void start() throws ClosedChannelException, SocketException, IOException {
        delegate.start();
    }
}
