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
import com.mastfrog.acteur.HttpEvent;
import com.mongodb.BasicDBObject;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Tim Boudreau
 */
public class QueryFromRequest extends Acteur {

    @Inject
    QueryFromRequest(HttpEvent evt) {
        BasicDBObject q = new BasicDBObject();
        for (Map.Entry<String, String> e : evt.urlParametersAsMap().entrySet()) {
            q.put(e.getKey(), e.getValue());
        }
        next(q);
    }

    private static enum Patterns {

        GTE(">[e=](\\d+)$"),
        LTE("<[e=](\\d+)$"),
        GT(">(\\d+)$"),
        LT("<(\\d+)$");
        private final Pattern pattern;

        Patterns(String pattern) {
            this.pattern = Pattern.compile(pattern);
        }

        boolean process(BasicDBObject ob, String name, HttpEvent evt) {
            String val = evt.urlParameter(name);
            if (val != null) {
                return decorate(ob, name, val);
            }
            return false;
        }

        private Long get(String val) {
            Matcher m = pattern.matcher(val);
            boolean found = m.find();
            if (found) {
                return Long.parseLong(m.group(1));
            }
            return null;
        }

        boolean decorate(BasicDBObject ob, String name, String val) {
            Long value = get(val);
            if (value != null) {
                ob.put(name, new BasicDBObject(toString(), value));
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return '$' + name().toLowerCase();
        }
    }
}
