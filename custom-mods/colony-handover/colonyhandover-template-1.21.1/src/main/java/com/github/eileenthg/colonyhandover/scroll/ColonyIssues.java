package com.github.eileenthg.colonyhandover.scroll;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A whole colony's outstanding work, carried on the scroll itself.
 *
 * <h2>Why this lives on the item</h2>
 *
 * The screen that shows it is client-side; the survey that produces it can only run
 * server-side, where the blocks are. The usual bridge is a custom network packet -
 * but an {@code ItemStack}'s data components are already synchronised to whoever
 * holds it, for free and correctly. Writing the survey onto the stack means the
 * screen has nothing to request: it reads what it is holding.
 *
 * <p>That also makes the scroll honest about time. What you see is what the colony
 * looked like when you last surveyed, not a live view pretending to be current, and
 * refreshing is an explicit act.
 *
 * <h2>Why the roster is stored, not just the problems</h2>
 *
 * After the handover there is no colony left to enumerate (finding #1), and no hut
 * blocks to find (finding #2) - which is exactly when this list matters most, while
 * you are furnishing rooms. Every building is recorded with its position and hut id,
 * so a refresh re-surveys from what the scroll remembers rather than from a colony
 * that no longer exists.
 */
public record ColonyIssues(String colonyName, BlockPos center, List<Entry> buildings,
                           long surveyedAt) {

    /**
     * @param surveyedAt game time of the survey. Its job is to make every re-survey a
     *                   visible change: the client opens the window once the scroll's
     *                   survey differs from the one it had, and an unchanged colony
     *                   would otherwise produce an identical record and never open.
     */
    public ColonyIssues {
        colonyName = colonyName == null ? "" : colonyName;
        buildings = buildings == null ? List.of() : List.copyOf(buildings);
    }

    /** What is wrong with one building, or that nothing is. */
    public enum Status implements StringRepresentable {
        /** Registers as its MCA type already. */
        DONE("done"),
        /** Sealed, but short of blocks. */
        NEEDS_BLOCKS("needs_blocks"),
        /** The survey walks out of it - seal it before furnishing. */
        NOT_SEALED("not_sealed"),
        /** Never becomes an MCA building; its workstation is all it needs. */
        NOT_A_ROOM("not_a_room"),
        /** Our mapping has no block for something the type wants. */
        UNMAPPABLE("unmappable"),
        /**
         * Nothing the survey can reach or point at.
         *
         * <p>Either no roofed, standable cell exists near the hut at all - typically a
         * space walled off on every side - or our walk found it sealed but MCA's own
         * scan still refuses it. What the two share is that there is no gap coordinate,
         * so {@code marks} has nothing to beam. Listing them as "not sealed" sent people
         * hunting for a hole that marks could never show them.
         */
        INACCESSIBLE("inaccessible");

        public static final Codec<Status> CODEC = StringRepresentable.fromEnum(Status::values);
        private final String name;

        Status(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public boolean needsWork() {
            return this == NEEDS_BLOCKS || this == NOT_SEALED;
        }
    }

    /**
     * @param gap      where the survey walks out, when it does - somewhere to walk to
     * @param missing  requirement group path to how many are still wanted
     * @param roomFor  how many of those would currently fit
     */
    public record Entry(String hutId, BlockPos pos, String mcaType, Status status,
                        Optional<BlockPos> gap, Map<String, Integer> missing, int roomFor) {

        /**
         * Nulls are coerced away here, at the one place every entry is built.
         *
         * <p>Huts that never become an MCA building - job sites, marker swaps,
         * skipped huts - have no MCA type, so {@code plan.mcaType()} is null for
         * them. {@code Codec.STRING} cannot encode a null, and this record is saved
         * with the player's inventory: storing one crashed the server the moment
         * the world saved with a bound scroll in it. Normalising in the constructor
         * means no caller can reintroduce it.
         */
        public Entry {
            hutId = hutId == null ? "" : hutId;
            mcaType = mcaType == null ? "" : mcaType;
            gap = gap == null ? Optional.empty() : gap;
            missing = missing == null ? Map.of() : Map.copyOf(missing);
        }

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("hut").forGetter(Entry::hutId),
                BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
                Codec.STRING.fieldOf("type").forGetter(Entry::mcaType),
                Status.CODEC.fieldOf("status").forGetter(Entry::status),
                BlockPos.CODEC.optionalFieldOf("gap").forGetter(Entry::gap),
                Codec.unboundedMap(Codec.STRING, Codec.INT)
                        .fieldOf("missing").forGetter(Entry::missing),
                Codec.INT.fieldOf("room").forGetter(Entry::roomFor)
        ).apply(i, Entry::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC =
                StreamCodec.of((buf, e) -> {
                    buf.writeUtf(e.hutId());
                    buf.writeBlockPos(e.pos());
                    buf.writeUtf(e.mcaType());
                    buf.writeEnum(e.status());
                    buf.writeOptional(e.gap(), (b, p) -> b.writeBlockPos(p));
                    buf.writeMap(e.missing(), (b, k) -> b.writeUtf(k), (b, v) -> b.writeVarInt(v));
                    buf.writeVarInt(e.roomFor());
                }, buf -> new Entry(
                        buf.readUtf(),
                        buf.readBlockPos(),
                        buf.readUtf(),
                        buf.readEnum(Status.class),
                        buf.readOptional(b -> b.readBlockPos()),
                        buf.readMap(b -> b.readUtf(), b -> b.readVarInt()),
                        buf.readVarInt()));

        public int wanted() {
            return missing.values().stream().mapToInt(Integer::intValue).sum();
        }

        public boolean cramped() {
            return roomFor < wanted();
        }
    }

    public static final Codec<ColonyIssues> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("colony").forGetter(ColonyIssues::colonyName),
            BlockPos.CODEC.fieldOf("center").forGetter(ColonyIssues::center),
            Entry.CODEC.listOf().fieldOf("buildings").forGetter(ColonyIssues::buildings),
            // Optional so scrolls surveyed before this field existed still load.
            Codec.LONG.optionalFieldOf("at", 0L).forGetter(ColonyIssues::surveyedAt)
    ).apply(i, ColonyIssues::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ColonyIssues> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, ColonyIssues::colonyName,
                    BlockPos.STREAM_CODEC, ColonyIssues::center,
                    Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), ColonyIssues::buildings,
                    ByteBufCodecs.VAR_LONG, ColonyIssues::surveyedAt,
                    ColonyIssues::new);

    /** Buildings with work the player can actually do - what the counts report. */
    public List<Entry> outstanding() {
        return listed().stream().filter(e -> e.status().needsWork()).toList();
    }

    /**
     * Everything the list screen shows, in the order the work should happen.
     *
     * <p>Unsealed first, because furnishing a room you can walk out of is wasted
     * effort, and those are exactly the ones {@code marks} can beam. Then rooms short
     * of blocks. Inaccessible last and greyed: kept visible so nothing silently
     * disappears, but out of the way of work that can be done.
     */
    public List<Entry> listed() {
        return buildings.stream()
                .filter(e -> e.status().needsWork() || e.status() == Status.INACCESSIBLE)
                .sorted((a, b) -> {
                    if (a.status() != b.status()) {
                        return Integer.compare(rank(a.status()), rank(b.status()));
                    }
                    return a.hutId().compareTo(b.hutId());
                })
                .toList();
    }

    public long inaccessibleCount() {
        return buildings.stream().filter(e -> e.status() == Status.INACCESSIBLE).count();
    }

    private static int rank(Status status) {
        return switch (status) {
            case NOT_SEALED -> 0;
            case NEEDS_BLOCKS -> 1;
            default -> 2;
        };
    }
}
