package com.waylays.arcanes.network;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.phone.PhoneClient;
import com.waylays.arcanes.phone.PhoneServer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Arcanophone's packets. The phone asks (to send a message, open a conversation, add a contact, post an echo, move
 * emeralds, share its position, learn the coming weather) and the server answers with what the phone shows: the
 * conversations, a conversation's messages, the contacts, the feed, the account, the positions shared with it.
 */
public final class PhonePackets {
    public static final int NAME = 16, TEXT = 256, POST = 160;

    private PhonePackets() {}

    public static void register(PayloadRegistrar r) {
        r.playToServer(Ask.TYPE, Ask.CODEC, (p, c) -> server(c, pl -> PhoneServer.ask(pl, p)));
        r.playToServer(Send.TYPE, Send.CODEC, (p, c) -> server(c, pl -> PhoneServer.send(pl, p)));
        r.playToServer(Contact.TYPE, Contact.CODEC, (p, c) -> server(c, pl -> PhoneServer.contact(pl, p)));
        r.playToServer(Echo.TYPE, Echo.CODEC, (p, c) -> server(c, pl -> PhoneServer.echo(pl, p)));
        r.playToServer(Bank.TYPE, Bank.CODEC, (p, c) -> server(c, pl -> PhoneServer.bank(pl, p)));
        r.playToClient(Inbox.TYPE, Inbox.CODEC, (p, c) -> PhoneClient.inbox(p));
        r.playToClient(Thread.TYPE, Thread.CODEC, (p, c) -> PhoneClient.thread(p));
        r.playToClient(Contacts.TYPE, Contacts.CODEC, (p, c) -> PhoneClient.contacts(p));
        r.playToClient(Feed.TYPE, Feed.CODEC, (p, c) -> PhoneClient.feed(p));
        r.playToClient(Account.TYPE, Account.CODEC, (p, c) -> PhoneClient.account(p));
        r.playToClient(Positions.TYPE, Positions.CODEC, (p, c) -> PhoneClient.positions(p));
        r.playToClient(Weather.TYPE, Weather.CODEC, (p, c) -> PhoneClient.weather(p));
        r.playToClient(Notice.TYPE, Notice.CODEC, (p, c) -> PhoneClient.notice(p));
    }

    private static void server(IPayloadContext context, java.util.function.Consumer<ServerPlayer> handler) {
        if (context.player() instanceof ServerPlayer player) {
            handler.accept(player);
        }
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payload(String name) {
        return new CustomPacketPayload.Type<>(ArcanesOublies.id("phone_" + name));
    }

    // ---------------------------------------------------------------- client to server

    /** What the phone asks for: 0 everything (on opening), 1 a conversation ({@code arg} the other's name), 2 the weather, 3 share position on, 4 off. */
    public record Ask(int what, String arg) implements CustomPacketPayload {
        public static final Type<Ask> TYPE = payload("ask");
        public static final StreamCodec<FriendlyByteBuf, Ask> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeVarInt(p.what);
            b.writeUtf(p.arg, NAME);
        }, b -> new Ask(b.readVarInt(), b.readUtf(NAME)));

        @Override
        public Type<Ask> type() {
            return TYPE;
        }
    }

    /** A message to the player named {@code to}. */
    public record Send(String to, String text) implements CustomPacketPayload {
        public static final Type<Send> TYPE = payload("send");
        public static final StreamCodec<FriendlyByteBuf, Send> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeUtf(p.to, NAME);
            b.writeUtf(p.text, TEXT);
        }, b -> new Send(b.readUtf(NAME), b.readUtf(TEXT)));

        @Override
        public Type<Send> type() {
            return TYPE;
        }
    }

    /** Adds ({@code add}) or removes the player named {@code name} from the contacts. */
    public record Contact(String name, boolean add) implements CustomPacketPayload {
        public static final Type<Contact> TYPE = payload("contact");
        public static final StreamCodec<FriendlyByteBuf, Contact> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeUtf(p.name, NAME);
            b.writeBoolean(p.add);
        }, b -> new Contact(b.readUtf(NAME), b.readBoolean()));

        @Override
        public Type<Contact> type() {
            return TYPE;
        }
    }

    /** Posts {@code text} to the Echoes feed ({@code like} >= 0: likes or unlikes that post instead). */
    public record Echo(String text, int like) implements CustomPacketPayload {
        public static final Type<Echo> TYPE = payload("echo");
        public static final StreamCodec<FriendlyByteBuf, Echo> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeUtf(p.text, POST);
            b.writeVarInt(p.like + 1);
        }, b -> new Echo(b.readUtf(POST), b.readVarInt() - 1));

        @Override
        public Type<Echo> type() {
            return TYPE;
        }
    }

    /** A bank operation: 0 deposit, 1 withdraw, 2 send to {@code to}; {@code amount} emeralds. */
    public record Bank(int action, int amount, String to) implements CustomPacketPayload {
        public static final Type<Bank> TYPE = payload("bank");
        public static final StreamCodec<FriendlyByteBuf, Bank> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeVarInt(p.action);
            b.writeVarInt(p.amount);
            b.writeUtf(p.to, NAME);
        }, b -> new Bank(b.readVarInt(), b.readVarInt(), b.readUtf(NAME)));

        @Override
        public Type<Bank> type() {
            return TYPE;
        }
    }

    // ---------------------------------------------------------------- server to client

    public record Conv(String name, UUID id, String last, boolean lastMine, long time, int unread, boolean online) {}

    /** The conversations, latest first. */
    public record Inbox(List<Conv> convs) implements CustomPacketPayload {
        public static final Type<Inbox> TYPE = payload("inbox");
        public static final StreamCodec<FriendlyByteBuf, Inbox> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeVarInt(p.convs.size());
            for (Conv c : p.convs) {
                b.writeUtf(c.name, NAME);
                b.writeUUID(c.id);
                b.writeUtf(c.last, TEXT);
                b.writeBoolean(c.lastMine);
                b.writeLong(c.time);
                b.writeVarInt(c.unread);
                b.writeBoolean(c.online);
            }
        }, b -> {
            int n = b.readVarInt();
            List<Conv> l = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                l.add(new Conv(b.readUtf(NAME), b.readUUID(), b.readUtf(TEXT), b.readBoolean(), b.readLong(), b.readVarInt(), b.readBoolean()));
            }
            return new Inbox(l);
        });

        @Override
        public Type<Inbox> type() {
            return TYPE;
        }
    }

    public record Line(boolean mine, String text, long time) {}

    /** The messages with {@code with}. */
    public record Thread(String with, UUID id, List<Line> lines) implements CustomPacketPayload {
        public static final Type<Thread> TYPE = payload("thread");
        public static final StreamCodec<FriendlyByteBuf, Thread> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeUtf(p.with, NAME);
            b.writeUUID(p.id);
            b.writeVarInt(p.lines.size());
            for (Line l : p.lines) {
                b.writeBoolean(l.mine);
                b.writeUtf(l.text, TEXT);
                b.writeLong(l.time);
            }
        }, b -> {
            String w = b.readUtf(NAME);
            UUID id = b.readUUID();
            int n = b.readVarInt();
            List<Line> l = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                l.add(new Line(b.readBoolean(), b.readUtf(TEXT), b.readLong()));
            }
            return new Thread(w, id, l);
        });

        @Override
        public Type<Thread> type() {
            return TYPE;
        }
    }

    public record Person(String name, UUID id, boolean online, boolean sharing) {}

    /** The contacts, and the players online (to add). */
    public record Contacts(List<Person> contacts, List<Person> online) implements CustomPacketPayload {
        public static final Type<Contacts> TYPE = payload("contacts");
        public static final StreamCodec<FriendlyByteBuf, Contacts> CODEC = CustomPacketPayload.codec((p, b) -> {
            write(b, p.contacts);
            write(b, p.online);
        }, b -> new Contacts(read(b), read(b)));

        private static void write(FriendlyByteBuf b, List<Person> l) {
            b.writeVarInt(l.size());
            for (Person x : l) {
                b.writeUtf(x.name, NAME);
                b.writeUUID(x.id);
                b.writeBoolean(x.online);
                b.writeBoolean(x.sharing);
            }
        }

        private static List<Person> read(FriendlyByteBuf b) {
            int n = b.readVarInt();
            List<Person> l = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                l.add(new Person(b.readUtf(NAME), b.readUUID(), b.readBoolean(), b.readBoolean()));
            }
            return l;
        }

        @Override
        public Type<Contacts> type() {
            return TYPE;
        }
    }

    public record PostView(int id, String author, UUID authorId, String text, long time, int likes, boolean liked) {}

    /** The Echoes feed, latest first. */
    public record Feed(List<PostView> posts) implements CustomPacketPayload {
        public static final Type<Feed> TYPE = payload("feed");
        public static final StreamCodec<FriendlyByteBuf, Feed> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeVarInt(p.posts.size());
            for (PostView x : p.posts) {
                b.writeVarInt(x.id);
                b.writeUtf(x.author, NAME);
                b.writeUUID(x.authorId);
                b.writeUtf(x.text, POST);
                b.writeLong(x.time);
                b.writeVarInt(x.likes);
                b.writeBoolean(x.liked);
            }
        }, b -> {
            int n = b.readVarInt();
            List<PostView> l = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                l.add(new PostView(b.readVarInt(), b.readUtf(NAME), b.readUUID(), b.readUtf(POST), b.readLong(), b.readVarInt(), b.readBoolean()));
            }
            return new Feed(l);
        });

        @Override
        public Type<Feed> type() {
            return TYPE;
        }
    }

    public record TxnView(String kind, String other, long amount, long time) {}

    /** The emerald account. */
    public record Account(long balance, List<TxnView> history) implements CustomPacketPayload {
        public static final Type<Account> TYPE = payload("account");
        public static final StreamCodec<FriendlyByteBuf, Account> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeLong(p.balance);
            b.writeVarInt(p.history.size());
            for (TxnView t : p.history) {
                b.writeUtf(t.kind, 16);
                b.writeUtf(t.other, NAME);
                b.writeLong(t.amount);
                b.writeLong(t.time);
            }
        }, b -> {
            long bal = b.readLong();
            int n = b.readVarInt();
            List<TxnView> l = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                l.add(new TxnView(b.readUtf(16), b.readUtf(NAME), b.readLong(), b.readLong()));
            }
            return new Account(bal, l);
        });

        @Override
        public Type<Account> type() {
            return TYPE;
        }
    }

    public record Spot(String name, UUID id, double x, double z, String dimension) {}

    /** Where the contacts who share their position are. */
    public record Positions(List<Spot> spots) implements CustomPacketPayload {
        public static final Type<Positions> TYPE = payload("positions");
        public static final StreamCodec<FriendlyByteBuf, Positions> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeVarInt(p.spots.size());
            for (Spot s : p.spots) {
                b.writeUtf(s.name, NAME);
                b.writeUUID(s.id);
                b.writeDouble(s.x);
                b.writeDouble(s.z);
                b.writeUtf(s.dimension, 64);
            }
        }, b -> {
            int n = b.readVarInt();
            List<Spot> l = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                l.add(new Spot(b.readUtf(NAME), b.readUUID(), b.readDouble(), b.readDouble(), b.readUtf(64)));
            }
            return new Positions(l);
        });

        @Override
        public Type<Positions> type() {
            return TYPE;
        }
    }

    /** The coming weather: ticks of clear weather left, of rain, of thunder (0 when not counting), and whether it rains or thunders. */
    public record Weather(int clear, int rain, int thunder, boolean raining, boolean thundering) implements CustomPacketPayload {
        public static final Type<Weather> TYPE = payload("weather");
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
     * Something to show on the phone: {@code kind} "message" (a new message: {@code title} its sender, {@code text} it),
     * "echo" (a new post), "bank" (emeralds received), or "info" ({@code text} a translation key, {@code title} its argument).
     */
    public record Notice(String kind, String title, String text) implements CustomPacketPayload {
        public static final Type<Notice> TYPE = payload("notice");
        public static final StreamCodec<FriendlyByteBuf, Notice> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeUtf(p.kind, 16);
            b.writeUtf(p.title, 64);
            b.writeUtf(p.text, TEXT);
        }, b -> new Notice(b.readUtf(16), b.readUtf(64), b.readUtf(TEXT)));

        @Override
        public Type<Notice> type() {
            return TYPE;
        }
    }
}
