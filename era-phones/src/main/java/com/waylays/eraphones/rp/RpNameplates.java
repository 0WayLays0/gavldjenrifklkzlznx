package com.waylays.eraphones.rp;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The names over the heads hidden for everyone (/pseudos cacher): every player is put in a team whose names are never
 * shown, which the game itself honours on every client (with or without this mod, and for what other mods hang on the
 * name, such as Essential's); the clients with this mod are told as well.
 */
public final class RpNameplates {
    private static final String TEAM = "eraphones_rp";

    private RpNameplates() {}

    public static void set(MinecraftServer server, boolean hidden) {
        RpData.get(server).hideNames(hidden);
        ServerScoreboard sb = server.getScoreboard();
        PlayerTeam team = sb.getPlayerTeam(TEAM);
        if (hidden) {
            if (team == null) {
                team = sb.addPlayerTeam(TEAM);
            }
            team.setNameTagVisibility(Team.Visibility.NEVER);
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                sb.addPlayerToTeam(p.getScoreboardName(), team);
            }
        } else if (team != null) {
            sb.removePlayerTeam(team);
        }
        PacketDistributor.sendToAllPlayers(new RpNet.Nameplates(hidden));
    }

    /** A player joining: in the team if names are hidden, and told so. */
    static void joined(ServerPlayer p) {
        boolean hidden = RpData.get(p.server).hideNames();
        if (hidden) {
            ServerScoreboard sb = p.server.getScoreboard();
            PlayerTeam team = sb.getPlayerTeam(TEAM);
            if (team == null) {
                team = sb.addPlayerTeam(TEAM);
                team.setNameTagVisibility(Team.Visibility.NEVER);
            }
            sb.addPlayerToTeam(p.getScoreboardName(), team);
        }
        PacketDistributor.sendToPlayer(p, new RpNet.Nameplates(hidden));
    }
}
