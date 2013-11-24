package com.mastfrog.blurt;

import com.google.inject.ImplementedBy;
import java.io.IOException;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;

/**
 * Control interface for starting/stopping the blurt service.  Separate
 * interface for those things that want to control starting and stopping the
 * blurt interface, as opposed to those clients that just want to send or
 * receive messages.
 *
 * @author Tim Boudreau
 */
@ImplementedBy(DefaultBlurtControl.class)
public interface BlurtControl {

    public void stop() throws IOException, InterruptedException;

    public void start() throws ClosedChannelException, SocketException, IOException;
}
