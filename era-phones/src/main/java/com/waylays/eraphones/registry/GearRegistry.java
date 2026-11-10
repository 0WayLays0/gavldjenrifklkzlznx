package com.waylays.eraphones.registry;

import com.mojang.serialization.Codec;
import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.gear.BatteryItem;
import com.waylays.eraphones.gear.CassetteItem;
import com.waylays.eraphones.gear.FlashlightItem;
import com.waylays.eraphones.gear.IdCardItem;
import com.waylays.eraphones.gear.NewspaperItem;
import com.waylays.eraphones.gear.WalkieItem;
import com.waylays.eraphones.gear.block.RadioBlock;
import com.waylays.eraphones.gear.block.RadioBlockEntity;
import com.waylays.eraphones.gear.block.VcrBlock;
import com.waylays.eraphones.gear.block.VcrBlockEntity;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/**
 * The things of the role play besides the phones: a torch and its batteries, a walkie-talkie, a table radio, a video
 * recorder and its cassettes, money, an identity card, a newspaper; what they carry (power, channel, the tape, the
 * card's holder, the paper's article); their sounds; and the creative tabs, one per kind of thing.
 */
public final class GearRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EraPhones.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EraPhones.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, EraPhones.MODID);
    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, EraPhones.MODID);

    /** Writing on a wall (/ecrire). */
    public static final DeferredHolder<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<com.waylays.eraphones.bike.BikeEntity>> BIKE =
            ENTITIES.register("velo", () -> net.minecraft.world.entity.EntityType.Builder.<com.waylays.eraphones.bike.BikeEntity>of(
                    com.waylays.eraphones.bike.BikeEntity::new, net.minecraft.world.entity.MobCategory.MISC).sized(.8f, 1.1f).clientTrackingRange(10)
                    .build("velo"));
    public static final DeferredHolder<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<com.waylays.eraphones.horror.WallWriting>> WRITING =
            ENTITIES.register("ecriture", () -> net.minecraft.world.entity.EntityType.Builder.<com.waylays.eraphones.horror.WallWriting>of(
                    com.waylays.eraphones.horror.WallWriting::new, net.minecraft.world.entity.MobCategory.MISC).sized(.5f, .5f).clientTrackingRange(8)
                    .updateInterval(20).noSummon().build("ecriture"));

    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> component(String name, Codec<T> codec,
            net.minecraft.network.codec.StreamCodec<? super net.minecraft.network.RegistryFriendlyByteBuf, T> stream) {
        return EraRegistry.COMPONENTS.register(name, () -> DataComponentType.<T>builder().persistent(codec).networkSynchronized(stream).build());
    }

    private static DeferredHolder<DataComponentType<?>, DataComponentType<String>> text(String name) {
        return component(name, Codec.STRING, ByteBufCodecs.STRING_UTF8);
    }

    /** Whether a torch or a walkie-talkie is switched on. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> POWER = component("power", Codec.BOOL, ByteBufCodecs.BOOL);
    /** A torch broken by a game master (/lampe casser): it no longer lights. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> BROKEN = component("broken", Codec.BOOL, ByteBufCodecs.BOOL);
    /** A walkie-talkie's channel, 1 to 16. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CHANNEL = component("channel", Codec.INT, ByteBufCodecs.VAR_INT);
    /** A cassette's recording (a file the game masters put on the server), its label, how far the tape is wound (samples). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> TAPE = text("tape");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> TAPE_LABEL = text("tape_label");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> TAPE_POS = component("tape_pos", Codec.LONG, ByteBufCodecs.VAR_LONG);
    /** An identity card's holder (their account, their character's name), its number, what it says of them. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> CARD_OWNER = text("card_owner");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> CARD_NAME = text("card_name");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> CARD_NUMBER = text("card_number");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> CARD_DESC = text("card_desc");
    /** A sheet of paper: its title and text, its paper (0-4), its ink (0-4), written by hand or typed, who signed it. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> NOTE_TITLE = text("note_title");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> NOTE_TEXT = text("note_text");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> NOTE_STYLE = component("note_style", Codec.INT, ByteBufCodecs.VAR_INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> NOTE_INK = component("note_ink", Codec.INT, ByteBufCodecs.VAR_INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> NOTE_TYPED = component("note_typed", Codec.BOOL, ByteBufCodecs.BOOL);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> NOTE_AUTHOR = text("note_author");
    /** A photograph: its picture (squeezed), when it came out of the camera (game ticks), who took it; the shots left in a camera. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.waylays.eraphones.gear.Picture>> PHOTO = component("photo",
            com.waylays.eraphones.gear.Picture.CODEC, com.waylays.eraphones.gear.Picture.STREAM_CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> PHOTO_TIME = component("photo_time", Codec.LONG, ByteBufCodecs.VAR_LONG);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> PHOTO_BY = text("photo_by");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FILM = component("film", Codec.INT, ByteBufCodecs.VAR_INT);
    /** A newspaper's headline and article. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> NEWS_TITLE = text("news_title");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> NEWS_TEXT = text("news_text");

    public static final DeferredHolder<SoundEvent, SoundEvent> RADIO_STATIC = sound("radio_static");
    public static final DeferredHolder<SoundEvent, SoundEvent> TALKIE_SQUELCH = sound("talkie_squelch");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_BOX = sound("boite_a_musique");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEARTBEAT = sound("coeur_peur");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHUTTER = sound("photo_declic");
    public static final DeferredHolder<SoundEvent, SoundEvent> BIKE_BELL = sound("velo_sonnette");
    public static final DeferredHolder<SoundEvent, SoundEvent> LAMP_BUZZ = sound("lampadaire_bourdonnement");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_BEEP = sound("repondeur_bip");
    public static final DeferredHolder<SoundEvent, SoundEvent> LAMP_CLICK = sound("lampadaire_claquement");
    public static final DeferredHolder<SoundEvent, SoundEvent> POWER_DOWN = sound("coupure_courant");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOOTH_RING = sound("cabine_sonnerie");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOOTH_COIN = sound("cabine_piece");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOOTH_KEY = sound("cabine_touche");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(EraPhones.id(name)));
    }

    public static final DeferredBlock<RadioBlock> RADIO = BLOCKS.register("radio",
            () -> new RadioBlock(BlockBehaviour.Properties.of().strength(1.2f).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<VcrBlock> VCR = BLOCKS.register("magnetoscope",
            () -> new VcrBlock(BlockBehaviour.Properties.of().strength(1.2f).sound(SoundType.METAL).noOcclusion()));

    public static final DeferredBlock<com.waylays.eraphones.gear.block.TvBlock> TV = BLOCKS.register("television",
            () -> new com.waylays.eraphones.gear.block.TvBlock(BlockBehaviour.Properties.of().strength(1.5f).sound(SoundType.WOOD).noOcclusion()
                    .lightLevel(s -> s.getValue(com.waylays.eraphones.gear.block.TvBlock.LIT) ? 7 : 0)));
    public static final DeferredBlock<com.waylays.eraphones.horror.block.MusicBoxBlock> MUSIC_BOX_BLOCK = BLOCKS.register("boite_a_musique",
            () -> new com.waylays.eraphones.horror.block.MusicBoxBlock(BlockBehaviour.Properties.of().strength(.8f).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<com.waylays.eraphones.horror.block.DollBlock> DOLL = BLOCKS.register("poupee",
            () -> new com.waylays.eraphones.horror.block.DollBlock(BlockBehaviour.Properties.of().strength(.6f).sound(SoundType.WOOL).noOcclusion()));

    public static final DeferredBlock<com.waylays.eraphones.gear.block.BoothBlock> BOOTH = BLOCKS.register("cabine_telephonique",
            () -> new com.waylays.eraphones.gear.block.BoothBlock(BlockBehaviour.Properties.of().strength(2.5f, 6f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(s -> s.getValue(com.waylays.eraphones.gear.block.BoothBlock.PART) == 2
                            && s.getValue(com.waylays.eraphones.gear.block.BoothBlock.POWERED) ? 13 : 0)));
    public static final DeferredBlock<com.waylays.eraphones.gear.block.AnswerMachineBlock> MACHINE = BLOCKS.register("repondeur",
            () -> new com.waylays.eraphones.gear.block.AnswerMachineBlock(BlockBehaviour.Properties.of().strength(1f).sound(SoundType.STONE).noOcclusion()
                    .lightLevel(s -> s.getValue(com.waylays.eraphones.gear.block.AnswerMachineBlock.NEW) ? 3 : 0)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.waylays.eraphones.gear.block.AnswerMachineBlockEntity>> MACHINE_BE = BLOCK_ENTITIES.register(
            "repondeur", () -> BlockEntityType.Builder.of(com.waylays.eraphones.gear.block.AnswerMachineBlockEntity::new, MACHINE.get()).build(null));
    public static final DeferredBlock<com.waylays.eraphones.gear.block.LampPostBlock> LAMP_POST = BLOCKS.register("lampadaire",
            () -> new com.waylays.eraphones.gear.block.LampPostBlock(BlockBehaviour.Properties.of().strength(3f, 6f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(s -> s.getValue(com.waylays.eraphones.gear.block.LampPostBlock.LIT) ? 15 : 0)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.waylays.eraphones.gear.block.LampPostBlockEntity>> LAMP_POST_BE = BLOCK_ENTITIES.register(
            "lampadaire", () -> BlockEntityType.Builder.of(com.waylays.eraphones.gear.block.LampPostBlockEntity::new, LAMP_POST.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.waylays.eraphones.gear.block.BoothBlockEntity>> BOOTH_BE = BLOCK_ENTITIES.register(
            "cabine_telephonique", () -> BlockEntityType.Builder.of(com.waylays.eraphones.gear.block.BoothBlockEntity::new, BOOTH.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.waylays.eraphones.gear.block.TvBlockEntity>> TV_BE = BLOCK_ENTITIES.register("television",
            () -> BlockEntityType.Builder.of(com.waylays.eraphones.gear.block.TvBlockEntity::new, TV.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.waylays.eraphones.horror.block.DollBlockEntity>> DOLL_BE = BLOCK_ENTITIES.register("poupee",
            () -> BlockEntityType.Builder.of(com.waylays.eraphones.horror.block.DollBlockEntity::new, DOLL.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RadioBlockEntity>> RADIO_BE = BLOCK_ENTITIES.register("radio",
            () -> BlockEntityType.Builder.of(RadioBlockEntity::new, RADIO.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VcrBlockEntity>> VCR_BE = BLOCK_ENTITIES.register("magnetoscope",
            () -> BlockEntityType.Builder.of(VcrBlockEntity::new, VCR.get()).build(null));

    public static final DeferredHolder<Item, Item> FLASHLIGHT = EraRegistry.ITEMS.register("lampe_torche",
            () -> new FlashlightItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> BATTERIES = EraRegistry.ITEMS.register("piles", () -> new BatteryItem(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, Item> WALKIE = EraRegistry.ITEMS.register("talkie_walkie",
            () -> new WalkieItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredHolder<Item, Item> RADIO_ITEM = EraRegistry.ITEMS.register("radio", () -> new BlockItem(RADIO.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> VCR_ITEM = EraRegistry.ITEMS.register("magnetoscope", () -> new BlockItem(VCR.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> CAMCORDER = EraRegistry.ITEMS.register("camescope",
            () -> new com.waylays.eraphones.gear.CamcorderItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> MACHINE_ITEM = EraRegistry.ITEMS.register("repondeur", () -> new BlockItem(MACHINE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> LAMP_POST_ITEM = EraRegistry.ITEMS.register("lampadaire",
            () -> new BlockItem(LAMP_POST.get(), new Item.Properties()));
    /** The four bicycles, in the order of {@link com.waylays.eraphones.bike.BikeEntity#KINDS}. */
    public static final List<DeferredHolder<Item, Item>> BIKE_ITEMS = new ArrayList<>();

    static {
        for (int i = 0; i < com.waylays.eraphones.bike.BikeEntity.KINDS.length; i++) {
            int kind = i;
            BIKE_ITEMS.add(EraRegistry.ITEMS.register(com.waylays.eraphones.bike.BikeEntity.KINDS[i],
                    () -> new com.waylays.eraphones.bike.BikeItem(kind, new Item.Properties().stacksTo(1))));
        }
    }

    public static final DeferredHolder<Item, Item> BIKE_ITEM = BIKE_ITEMS.get(0);
    public static final DeferredHolder<Item, Item> BOOTH_ITEM = EraRegistry.ITEMS.register("cabine_telephonique",
            () -> new BlockItem(BOOTH.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> TV_ITEM = EraRegistry.ITEMS.register("television", () -> new BlockItem(TV.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> MUSIC_BOX_ITEM = EraRegistry.ITEMS.register("boite_a_musique",
            () -> new BlockItem(MUSIC_BOX_BLOCK.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> DOLL_ITEM = EraRegistry.ITEMS.register("poupee", () -> new BlockItem(DOLL.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> BALLOON = EraRegistry.ITEMS.register("ballon_rouge",
            () -> new com.waylays.eraphones.horror.BalloonItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> REMOTE = EraRegistry.ITEMS.register("telecommande_mj",
            () -> new com.waylays.eraphones.horror.RemoteItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredHolder<Item, Item> CASSETTE = EraRegistry.ITEMS.register("cassette", () -> new CassetteItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> ID_CARD = EraRegistry.ITEMS.register("carte_identite", () -> new IdCardItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> NOTE = EraRegistry.ITEMS.register("feuille",
            () -> new com.waylays.eraphones.gear.NoteItem(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, Item> CAMERA = EraRegistry.ITEMS.register("appareil_photo",
            () -> new com.waylays.eraphones.gear.CameraItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> FILM_PACK = EraRegistry.ITEMS.register("pellicule", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, Item> PHOTOGRAPH = EraRegistry.ITEMS.register("photo",
            () -> new com.waylays.eraphones.gear.PhotoItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> NEWSPAPER = EraRegistry.ITEMS.register("journal_papier", () -> new NewspaperItem(new Item.Properties().stacksTo(16)));

    /** The money: dollar bills and coins, by face value in cents. */
    public static final int[] MONEY_CENTS = {10, 25, 100, 500, 1000, 2000, 5000, 10000};
    public static final List<DeferredHolder<Item, Item>> MONEY = new ArrayList<>();

    static {
        for (int cents : MONEY_CENTS) {
            MONEY.add(EraRegistry.ITEMS.register(moneyId(cents), () -> new Item(new Item.Properties().stacksTo(64))));
        }
    }

    public static String moneyId(int cents) {
        return cents < 100 ? "piece_" + cents + "c" : "billet_" + cents / 100;
    }

    // ---------------------------------------------------------------- the creative tabs, one per kind of thing

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB_LIGHT = EraRegistry.TABS.register("lumiere_radio", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.eraphones.lumiere_radio"))
            .icon(() -> new ItemStack(FLASHLIGHT.get()))
            .withTabsBefore(EraRegistry.TAB.getId())
            .displayItems((p, out) -> {
                out.accept(FLASHLIGHT.get());
                out.accept(BATTERIES.get());
                out.accept(WALKIE.get());
                out.accept(RADIO_ITEM.get());
            }).build());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB_TAPE = EraRegistry.TABS.register("son_image", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.eraphones.son_image"))
            .icon(() -> new ItemStack(VCR_ITEM.get()))
            .withTabsBefore(TAB_LIGHT.getId())
            .displayItems((p, out) -> {
                out.accept(VCR_ITEM.get());
                out.accept(TV_ITEM.get());
                out.accept(CASSETTE.get());
                out.accept(CAMCORDER.get());
            }).build());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB_DAILY = EraRegistry.TABS.register("vie_quotidienne", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.eraphones.vie_quotidienne"))
            .icon(() -> new ItemStack(MONEY.get(5).get()))
            .withTabsBefore(TAB_TAPE.getId())
            .displayItems((p, out) -> {
                for (DeferredHolder<Item, Item> m : MONEY) {
                    out.accept(m.get());
                }
                out.accept(ID_CARD.get());
                out.accept(CAMERA.get());
                out.accept(FILM_PACK.get());
                out.accept(BOOTH_ITEM.get());
                for (DeferredHolder<Item, Item> b : BIKE_ITEMS) {
                    out.accept(b.get());
                }
                out.accept(LAMP_POST_ITEM.get());
                out.accept(MACHINE_ITEM.get());
                out.accept(NOTE.get());
                out.accept(NEWSPAPER.get());
            }).build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB_HORROR = EraRegistry.TABS.register("horreur", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.eraphones.horreur"))
            .icon(() -> new ItemStack(BALLOON.get()))
            .withTabsBefore(TAB_DAILY.getId())
            .displayItems((p, out) -> {
                out.accept(REMOTE.get());
                out.accept(BALLOON.get());
                out.accept(DOLL_ITEM.get());
                out.accept(MUSIC_BOX_ITEM.get());
            }).build());

    private GearRegistry() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
        SOUNDS.register(bus);
        ENTITIES.register(bus);
    }
}
