package com.waylays.cinematic.studio;

import java.util.UUID;

/** Where an actor finds its take and the scene's current time (the server's playback, or the client's view of it). */
public interface FrameSource {
    Track track(UUID actor);

    /** Current scene tick for this actor. */
    int tick(UUID actor);
}
