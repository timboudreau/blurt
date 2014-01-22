package com.mastfrog.blurt.collector;

import com.google.common.net.MediaType;
import com.google.inject.Inject;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.ActeurFactory;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.Page;
import com.mastfrog.acteur.headers.Method;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.HttpResponseStatus;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import io.netty.util.CharsetUtil;

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
            page.getResponseHeaders().setContentType(MediaType.parse("text/event-stream").withCharset(CharsetUtil.UTF_8));
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
