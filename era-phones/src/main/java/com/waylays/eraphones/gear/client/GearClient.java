package com.waylays.eraphones.gear.client;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.gear.GearNet;
import com.waylays.eraphones.gear.Stations;
import com.waylays.eraphones.gear.block.RadioBlockEntity;
import com.waylays.eraphones.registry.GearRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The things on the players' side: their faces, the stations on the air, the radios' hiss between stations. */
@EventBusSubscriber(modid = EraPhones.MODID, value = Dist.CLIENT)
public final class GearClient {
    private static final Set<Integer> ON_AIR = new HashSet<>();
    private static final Map<BlockPos, RadioHiss> HISSES = new HashMap<>();

    private GearClient() {}

    public static void openWalkie(InteractionHand hand) {
        Minecraft.getInstance().setScreen(new WalkieScreen(hand));
    }

    public static void openRadio(BlockPos pos) {
        Minecraft.getInstance().setScreen(new RadioScreen(pos));
    }

    public static void openVcr(BlockPos pos) {
        Minecraft.getInstance().setScreen(new VcrScreen(pos));
    }

    public static void openNewspaper(String title, String text) {
        Minecraft.getInstance().setScreen(new NewspaperScreen(title, text));
    }

    /** A sheet of paper used: written on if it is not signed yet, else read. */
    public static void openNote(InteractionHand hand, net.minecraft.world.item.ItemStack stack) {
        if (com.waylays.eraphones.gear.NoteItem.signed(stack)) {
            Minecraft.getInstance().setScreen(new NoteReadScreen(NotePage.Data.of(stack)));
        } else {
            Minecraft.getInstance().setScreen(new NoteEditScreen(hand, stack));
        }
    }

    public static void stations(GearNet.Stations p) {
        ON_AIR.clear();
        ON_AIR.addAll(p.frequencies());
    }

    /** Whether a radio set to {@code freq} picks up a station. */
    public static boolean onAir(int freq) {
        for (int f : ON_AIR) {
            if (Stations.tuned(freq, f)) {
                return true;
            }
        }
        return false;
    }

    /** Each tick of a radio in the world: its hiss started when it is switched on. */
    public static void radioTick(RadioBlockEntity r) {
        BlockPos pos = r.getBlockPos().immutable();
        RadioHiss h = HISSES.get(pos);
        if (r.on() && (h == null || h.isStopped())) {
            h = new RadioHiss(r);
            HISSES.put(pos, h);
            Minecraft.getInstance().getSoundManager().play(h);
        }
    }

    @SubscribeEvent
    public static void onLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        ON_AIR.clear();
        HISSES.clear();
    }

    /** A radio's hiss: loud off station, faint under a station, gone when the radio is off or gone. */
    static final class RadioHiss extends AbstractTickableSoundInstance {
        private final RadioBlockEntity radio;

        RadioHiss(RadioBlockEntity radio) {
            super(GearRegistry.RADIO_STATIC.get(), SoundSource.RECORDS, RandomSource.create());
            this.radio = radio;
            this.looping = true;
            this.delay = 0;
            this.x = radio.getBlockPos().getX() + .5;
            this.y = radio.getBlockPos().getY() + .5;
            this.z = radio.getBlockPos().getZ() + .5;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
            this.volume = level();
        }

        private float level() {
            float v = radio.volume() / 10f;
            return v * (onAir(radio.frequency()) ? .06f : .45f);
        }

        @Override
        public void tick() {
            if (radio.isRemoved() || !radio.on() || Minecraft.getInstance().level == null || Minecraft.getInstance().level.getBlockEntity(radio.getBlockPos()) != radio) {
                stop();
                return;
            }
            volume = Math.max(.001f, level());
        }
    }

    static List<Integer> stations() {
        return List.copyOf(ON_AIR);
    }
}
