package com.mastfrog.blurt.collector;

import com.google.inject.Inject;
import com.mastfrog.acteur.ActeurFactory;
import com.mastfrog.acteur.Page;
import com.mastfrog.acteur.headers.Method;

/**
 *
 * @author Tim Boudreau
 */
public class QueryResource extends Page {

    @Inject
    QueryResource(ActeurFactory af) {
        add(af.matchMethods(Method.GET));
        add(af.matchPath("^query$"));
    }
}
