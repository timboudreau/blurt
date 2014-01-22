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
        for (Map.Entry<String, String> e : evt.getParametersAsMap().entrySet()) {
            q.put(e.getKey(), e.getValue());
        }
        setState(new ConsumedLockedState(q));
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
            String val = evt.getParameter(name);
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
