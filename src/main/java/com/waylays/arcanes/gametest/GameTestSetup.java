package com.waylays.arcanes.gametest;

/** Timing shared by the addon's GameTests. */
final class GameTestSetup {
    /**
     * Ticks between a test's platform being placed and the test starting. Entities added in the very tick the
     * platform's chunks are loaded land where the level cannot see them yet (searches miss them, a summon cannot find
     * its summoner), which made tests fail at random.
     */
    static final long TICKS = 5;

    private GameTestSetup() {}
}
