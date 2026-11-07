package com.waylays.eraphones.network;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.client.PhoneClient;
import com.waylays.eraphones.server.CallService;
import com.waylays.eraphones.server.PhoneService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * The phones' packets. The phone opened tells the server which of the carried phones it is ({@link Open}, by
 * inventory slot); everything after is asked for that phone's number, and the server answers with what that number
 * holds. Numbers and the names their owners saved are all that ever travel: no account name.
 */
public final class PhoneNet {
    public static final int NAME = 32, TEXT = 256, POST = 200, SMS_GSM = 160;

    private PhoneNet() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        r.playToServer(Open.TYPE, Open.CODEC, (p, c) -> server(c, pl -> PhoneService.open(pl, p.slot())));
        r.playToServer(Close.TYPE, Close.CODEC, (p, c) -> server(c, PhoneService::close));
        r.playToServer(Ask.TYPE, Ask.CODEC, (p, c) -> server(c, pl -> PhoneService.ask(pl, p.what(), p.arg())));
        r.playToServer(Sms.TYPE, Sms.CODEC, (p, c) -> server(c, pl -> PhoneService.sms(pl, p.to(), p.text())));
        r.playToServer(ContactEdit.TYPE, ContactEdit.CODEC, (p, c) -> server(c, pl -> PhoneService.contact(pl, p.name(), p.number(), p.remove())));
        r.playToServer(Post.TYPE, Post.CODEC, (p, c) -> server(c, pl -> PhoneService.post(pl, p.text(), p.like())));
        r.playToServer(Handle.TYPE, Handle.CODEC, (p, c) -> server(c, pl -> PhoneService.handle(pl, p.handle())));
        r.playToServer(Dial.TYPE, Dial.CODEC, (p, c) -> server(c, pl -> CallService.dial(pl, p.number())));
        r.playToServer(Answer.TYPE, Answer.CODEC, (p, c) -> server(c, pl -> CallService.answer(pl, p.accept())));
        r.playToServer(HangUp.TYPE, HangUp.CODEC, (p, c) -> server(c, CallService::hangUp));
        r.playToServer(Speak.TYPE, Speak.CODEC, (p, c) -> server(c, pl -> CallService.speak(pl, p.text())));
        r.playToClient(Hello.TYPE, Hello.CODEC, (p, c) -> PhoneClient.hello(p));
        r.playToClient(Inbox.TYPE, Inbox.CODEC, (p, c) -> PhoneClient.inbox(p));
        r.playToClient(Thread.TYPE, Thread.CODEC, (p, c) -> PhoneClient.thread(p));
        r.playToClient(Contacts.TYPE, Contacts.CODEC, (p, c) -> PhoneClient.contacts(p));
        r.playToClient(CallLog.TYPE, CallLog.CODEC, (p, c) -> PhoneClient.callLog(p));
        r.playToClient(Feed.TYPE, Feed.CODEC, (p, c) -> PhoneClient.feed(p));
        r.playToClient(NewsList.TYPE, NewsList.CODEC, (p, c) -> PhoneClient.news(p));
        r.playToClient(Positions.TYPE, Positions.CODEC, (p, c) -> PhoneClient.positions(p));
        r.playToClient(Weather.TYPE, Weather.CODEC, (p, c) -> PhoneClient.weather(p));
        r.playToClient(Notice.TYPE, Notice.CODEC, (p, c) -> PhoneClient.notice(p));
        r.playToClient(CallState.TYPE, CallState.CODEC, (p, c) -> PhoneClient.callState(p));
        r.playToClient(CallLine.TYPE, CallLine.CODEC, (p, c) -> PhoneClient.callLine(p));
        r.playToClient(Haunt.TYPE, Haunt.CODEC, (p, c) -> PhoneClient.haunt(p));
    }

    private static void server(IPayloadContext context, java.util.function.Consumer<ServerPlayer> handler) {
        if (context.player() instanceof ServerPlayer player) {
            handler.accept(player);
        }
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String name) {
        return new CustomPacketPayload.Type<>(EraPhones.id(name));
    }

    private static <T> void list(FriendlyByteBuf b, List<T> l, BiConsumer<FriendlyByteBuf, T> w) {
        b.writeVarInt(l.size());
        for (T t : l) {
            w.accept(b, t);
        }
    }

    private static <T> List<T> list(FriendlyByteBuf b, Function<FriendlyByteBuf, T> r) {
        int n = Math.min(b.readVarInt(), 512);
        List<T> l = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            l.add(r.apply(b));
        }
        return l;
    }

    // ---------------------------------------------------------------- phone to server

    /** The phone in {@code slot} (inventory index, 40 the off hand) is being used. */
    public record Open(int slot) implements CustomPacketPayload {
        public static final Type<Open> TYPE = type("open");
        public static final StreamCodec<FriendlyByteBuf, Open> CODEC = CustomPacketPayload.codec((p, b) -> b.writeVarInt(p.slot), b -> new Open(b.readVarInt()));

        @Override
        public Type<Open> type() {
            return TYPE;
        }
    }

    public record Close() implements CustomPacketPayload {
        public static final Type<Close> TYPE = type("close");
        public static final StreamCodec<FriendlyByteBuf, Close> CODEC = StreamCodec.unit(new Close());

        @Override
        public Type<Close> type() {
            return TYPE;
        }
    }

    /**
     * What the phone asks for: 1 a conversation ({@code arg} the other number), 2 the weather, 3 share the position,
     * 4 stop sharing it, 5 the social feed, 6 the news, 7 silent, 8 ringing again, 9 the call log, 10 the directory,
     * 11 the conversations.
     */
    public record Ask(int what, String arg) implements CustomPacketPayload {
        public static final Type<Ask> TYPE = type("ask");
        public static final StreamCodec<FriendlyByteBuf, Ask> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeVarInt(p.what);
            b.writeUtf(p.arg, NAME);
        }, b -> new Ask(b.readVarInt(), b.readUtf(NAME)));

        @Override
        public Type<Ask> type() {
            return TYPE;
        }
    }

    public record Sms(String to, String text) implements CustomPacketPayload {
        public static final Type<Sms> TYPE = type("sms");
        public static final StreamCodec<FriendlyByteBuf, Sms> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeUtf(p.to, NAME);
            b.writeUtf(p.text, TEXT);
        }, b -> new Sms(b.readUtf(NAME), b.readUtf(TEXT)));

        @Override
        public Type<Sms> type() {
            return TYPE;
        }
    }

    /** Saves {@code number} in the directory as {@code name} (or removes it). */
    public record ContactEdit(String name, String number, boolean remove) implements CustomPacketPayload {
        public static final Type<ContactEdit> TYPE = type("contact");
        public static final StreamCodec<FriendlyByteBuf, ContactEdit> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeUtf(p.name, NAME);
            b.writeUtf(p.number, NAME);
            b.writeBoolean(p.remove);
        }, b -> new ContactEdit(b.readUtf(NAME), b.readUtf(NAME), b.readBoolean()));

        @Override
        public Type<ContactEdit> type() {
            return TYPE;
        }
    }

    /** Posts {@code text} on the social network ({@code like} >= 0: likes or unlikes that post instead). */
    public record Post(String text, int like) implements CustomPacketPayload {
        public static final Type<Post> TYPE = type("post");
        public static final StreamCodec<FriendlyByteBuf, Post> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeUtf(p.text, POST);
            b.writeVarInt(p.like + 1);
        }, b -> new Post(b.readUtf(POST), b.readVarInt() - 1));

        @Override
        public Type<Post> type() {
            return TYPE;
        }
    }

    /** The name this line signs its posts with. */
    public record Handle(String handle) implements CustomPacketPayload {
        public static final Type<Handle> TYPE = type("handle");
        public static final StreamCodec<FriendlyByteBuf, Handle> CODEC = CustomPacketPayload.codec((p, b) -> b.writeUtf(p.handle, NAME), b -> new Handle(b.readUtf(NAME)));

        @Override
        public Type<Handle> type() {
            return TYPE;
        }
    }

    public record Dial(String number) implements CustomPacketPayload {
        public static final Type<Dial> TYPE = type("dial");
        public static final StreamCodec<FriendlyByteBuf, Dial> CODEC = CustomPacketPayload.codec((p, b) -> b.writeUtf(p.number, NAME), b -> new Dial(b.readUtf(NAME)));

        @Override
        public Type<Dial> type() {
            return TYPE;
        }
    }

    public record Answer(boolean accept) implements CustomPacketPayload {
        public static final Type<Answer> TYPE = type("answer");
        public static final StreamCodec<FriendlyByteBuf, Answer> CODEC = CustomPacketPayload.codec((p, b) -> b.writeBoolean(p.accept), b -> new Answer(b.readBoolean()));

        @Override
        public Type<Answer> type() {
            return TYPE;
        }
    }

    public record HangUp() implements CustomPacketPayload {
        public static final Type<HangUp> TYPE = type("hang_up");
        public static final StreamCodec<FriendlyByteBuf, HangUp> CODEC = StreamCodec.unit(new HangUp());

        @Override
        public Type<HangUp> type() {
            return TYPE;
        }
    }

    /** A line said into the phone during a call. */
    public record Speak(String text) implements CustomPacketPayload {
        public static final Type<Speak> TYPE = type("speak");
        public static final StreamCodec<FriendlyByteBuf, Speak> CODEC = CustomPacketPayload.codec((p, b) -> b.writeUtf(p.text, TEXT), b -> new Speak(b.readUtf(TEXT)));

        @Override
        public Type<Speak> type() {
            return TYPE;
        }
    }

    // ---------------------------------------------------------------- server to phone

    /** The line of the phone opened: its number, its era, its social name, whether it shares its position, whether it is silent. */
    public record Hello(String number, int era, String handle, boolean sharing, boolean silent) implements CustomPacketPayload {
        public static final Type<Hello> TYPE = type("hello");
        public static final StreamCodec<FriendlyByteBuf, Hello> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeUtf(p.number, NAME);
            b.writeVarInt(p.era);
            b.writeUtf(p.handle, NAME);
            b.writeBoolean(p.sharing);
            b.writeBoolean(p.silent);
        }, b -> new Hello(b.readUtf(NAME), b.readVarInt(), b.readUtf(NAME), b.readBoolean(), b.readBoolean()));

        @Override
        public Type<Hello> type() {
            return TYPE;
        }
    }

    public record Conv(String number, String display, String last, boolean lastMine, long time, int unread) {}

    public record Inbox(List<Conv> convs) implements CustomPacketPayload {
        public static final Type<Inbox> TYPE = type("inbox");
        public static final StreamCodec<FriendlyByteBuf, Inbox> CODEC = CustomPacketPayload.codec((p, b) -> list(b, p.convs, (o, c) -> {
            o.writeUtf(c.number, NAME);
            o.writeUtf(c.display, NAME);
            o.writeUtf(c.last, TEXT);
            o.writeBoolean(c.lastMine);
            o.writeLong(c.time);
            o.writeVarInt(c.unread);
        }), b -> new Inbox(list(b, o -> new Conv(o.readUtf(NAME), o.readUtf(NAME), o.readUtf(TEXT), o.readBoolean(), o.readLong(), o.readVarInt()))));

        @Override
        public Type<Inbox> type() {
            return TYPE;
        }
    }

    public record Line(boolean mine, String text, long time) {}

    public record Thread(String number, String display, List<Line> lines) implements CustomPacketPayload {
        public static final Type<Thread> TYPE = type("thread");
        public static final StreamCodec<FriendlyByteBuf, Thread> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeUtf(p.number, NAME);
            b.writeUtf(p.display, NAME);
            list(b, p.lines, (o, l) -> {
                o.writeBoolean(l.mine);
                o.writeUtf(l.text, TEXT);
                o.writeLong(l.time);
            });
        }, b -> new Thread(b.readUtf(NAME), b.readUtf(NAME), list(b, o -> new Line(o.readBoolean(), o.readUtf(TEXT), o.readLong()))));

        @Override
        public Type<Thread> type() {
            return TYPE;
        }
    }

    /** A directory entry; {@code sharing}: that number shares where it is with this one. */
    public record ContactView(String name, String number, boolean sharing) {}

    public record Contacts(List<ContactView> contacts) implements CustomPacketPayload {
        public static final Type<Contacts> TYPE = type("contacts");
        public static final StreamCodec<FriendlyByteBuf, Contacts> CODEC = CustomPacketPayload.codec((p, b) -> list(b, p.contacts, (o, c) -> {
            o.writeUtf(c.name, NAME);
            o.writeUtf(c.number, NAME);
            o.writeBoolean(c.sharing);
        }), b -> new Contacts(list(b, o -> new ContactView(o.readUtf(NAME), o.readUtf(NAME), o.readBoolean()))));

        @Override
        public Type<Contacts> type() {
            return TYPE;
        }
    }

    /** A call of the log: {@code kind} 0 made, 1 received, 2 missed, 3 refused. */
    public record CallView(String number, String display, int kind, long time, int seconds) {}

    public record CallLog(List<CallView> calls) implements CustomPacketPayload {
        public static final Type<CallLog> TYPE = type("call_log");
        public static final StreamCodec<FriendlyByteBuf, CallLog> CODEC = CustomPacketPayload.codec((p, b) -> list(b, p.calls, (o, c) -> {
            o.writeUtf(c.number, NAME);
            o.writeUtf(c.display, NAME);
            o.writeVarInt(c.kind);
            o.writeLong(c.time);
            o.writeVarInt(c.seconds);
        }), b -> new CallLog(list(b, o -> new CallView(o.readUtf(NAME), o.readUtf(NAME), o.readVarInt(), o.readLong(), o.readVarInt()))));

        @Override
        public Type<CallLog> type() {
            return TYPE;
        }
    }

    public record PostView(int id, String handle, String text, long time, int likes, boolean liked, boolean mine) {}

    public record Feed(List<PostView> posts) implements CustomPacketPayload {
        public static final Type<Feed> TYPE = type("feed");
        public static final StreamCodec<FriendlyByteBuf, Feed> CODEC = CustomPacketPayload.codec((p, b) -> list(b, p.posts, (o, x) -> {
            o.writeVarInt(x.id);
            o.writeUtf(x.handle, NAME);
            o.writeUtf(x.text, POST);
            o.writeLong(x.time);
            o.writeVarInt(x.likes);
            o.writeBoolean(x.liked);
            o.writeBoolean(x.mine);
        }), b -> new Feed(list(b, o -> new PostView(o.readVarInt(), o.readUtf(NAME), o.readUtf(POST), o.readLong(), o.readVarInt(), o.readBoolean(), o.readBoolean()))));

        @Override
        public Type<Feed> type() {
            return TYPE;
        }
    }

    public record NewsView(String title, String text, long time) {}

    public record NewsList(List<NewsView> news) implements CustomPacketPayload {
        public static final Type<NewsList> TYPE = type("news");
        public static final StreamCodec<FriendlyByteBuf, NewsList> CODEC = CustomPacketPayload.codec((p, b) -> list(b, p.news, (o, x) -> {
            o.writeUtf(x.title, 96);
            o.writeUtf(x.text, 512);
            o.writeLong(x.time);
        }), b -> new NewsList(list(b, o -> new NewsView(o.readUtf(96), o.readUtf(512), o.readLong()))));

        @Override
        public Type<NewsList> type() {
            return TYPE;
        }
    }

    public record Spot(String display, String number, double x, double z, String dimension) {}

    public record Positions(List<Spot> spots) implements CustomPacketPayload {
        public static final Type<Positions> TYPE = type("positions");
        public static final StreamCodec<FriendlyByteBuf, Positions> CODEC = CustomPacketPayload.codec((p, b) -> list(b, p.spots, (o, s) -> {
            o.writeUtf(s.display, NAME);
            o.writeUtf(s.number, NAME);
            o.writeDouble(s.x);
            o.writeDouble(s.z);
            o.writeUtf(s.dimension, 64);
        }), b -> new Positions(list(b, o -> new Spot(o.readUtf(NAME), o.readUtf(NAME), o.readDouble(), o.readDouble(), o.readUtf(64)))));

        @Override
        public Type<Positions> type() {
            return TYPE;
        }
    }

    /** The coming weather: ticks of clear weather left, of rain, of thunder, and whether it rains or thunders. */
    public record Weather(int clear, int rain, int thunder, boolean raining, boolean thundering) implements CustomPacketPayload {
        public static final Type<Weather> TYPE = type("weather");
        public static final StreamCodec<FriendlyByteBuf, Weather> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeVarInt(p.clear);
            b.writeVarInt(p.rain);
            b.writeVarInt(p.thunder);
            b.writeBoolean(p.raining);
            b.writeBoolean(p.thundering);
        }, b -> new Weather(b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readBoolean(), b.readBoolean()));

        @Override
        public Type<Weather> type() {
            return TYPE;
        }
    }

    /**
     * Something for the phone to show: {@code kind} "sms" (from {@code number}, shown as {@code title}), "post", "news",
     * "missed" (a missed call), or "info" ({@code text} a translation key, {@code title} its argument).
     */
    public record Notice(String kind, String title, String text, String number) implements CustomPacketPayload {
        public static final Type<Notice> TYPE = type("notice");
        public static final StreamCodec<FriendlyByteBuf, Notice> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeUtf(p.kind, 16);
            b.writeUtf(p.title, 96);
            b.writeUtf(p.text, TEXT);
            b.writeUtf(p.number, NAME);
        }, b -> new Notice(b.readUtf(16), b.readUtf(96), b.readUtf(TEXT), b.readUtf(NAME)));

        @Override
        public Type<Notice> type() {
            return TYPE;
        }
    }

    /**
     * Where a call stands for this phone: {@code state} 0 over ({@code reason} why), 1 calling out (ringing at the
     * other end), 2 ringing here, 3 talking since {@code since} (ms); {@code mine} this end's number, {@code number}
     * and {@code display} the other end.
     */
    public record CallState(int state, String mine, String number, String display, long since, String reason) implements CustomPacketPayload {
        public static final Type<CallState> TYPE = type("call_state");
        public static final StreamCodec<FriendlyByteBuf, CallState> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeVarInt(p.state);
            b.writeUtf(p.mine, NAME);
            b.writeUtf(p.number, NAME);
            b.writeUtf(p.display, NAME);
            b.writeLong(p.since);
            b.writeUtf(p.reason, 32);
        }, b -> new CallState(b.readVarInt(), b.readUtf(NAME), b.readUtf(NAME), b.readUtf(NAME), b.readLong(), b.readUtf(32)));

        @Override
        public Type<CallState> type() {
            return TYPE;
        }
    }

    /** A line said during the call ({@code mine}: by this phone's holder). */
    public record CallLine(boolean mine, String text) implements CustomPacketPayload {
        public static final Type<CallLine> TYPE = type("call_line");
        public static final StreamCodec<FriendlyByteBuf, CallLine> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeBoolean(p.mine);
            b.writeUtf(p.text, TEXT);
        }, b -> new CallLine(b.readBoolean(), b.readUtf(TEXT)));

        @Override
        public Type<CallLine> type() {
            return TYPE;
        }
    }

    /** The phone goes wrong for {@code ticks}: static, a shape in it, and {@code text} if any (a game master's doing). */
    public record Haunt(int ticks, String text) implements CustomPacketPayload {
        public static final Type<Haunt> TYPE = type("haunt");
        public static final StreamCodec<FriendlyByteBuf, Haunt> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeVarInt(p.ticks);
            b.writeUtf(p.text, TEXT);
        }, b -> new Haunt(b.readVarInt(), b.readUtf(TEXT)));

        @Override
        public Type<Haunt> type() {
            return TYPE;
        }
    }
}
