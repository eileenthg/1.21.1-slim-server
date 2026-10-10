package com.github.eileen.llvoices.voice;

import com.github.eileen.llvoices.logic.ChangeCause;
import com.github.eileen.llvoices.logic.DriftLevel;
import com.github.eileen.llvoices.logic.DriftZone;
import com.github.eileen.llvoices.logic.HungerStage;

import java.util.Locale;

/**
 * Builds what the maid is told about her situation. The text only describes what is happening and how she feels;
 * the voice itself comes from the maid's own TLM character setting, so every character answers differently.
 */
public final class VoicePrompts {
    /** Opening line, so the history entry TLM keeps for this turn does not read as something the master said. */
    static final String HEADER = "[Not said by your master. This is what is happening to you right now.]";
    static final String INSTRUCTION = "Say one short line out loud about it: one or two sentences, under 25 words. "
            + "Speak exactly the way YOUR character would, with your own personality, speech habits and way of "
            + "addressing your master from your character setting. Do not fall back on generic, polite servant "
            + "phrasing unless that really is your character. Never mention numbers, stats or game mechanics, and "
            + "do not describe actions in brackets.";

    private VoicePrompts() {
    }

    public static String wrap(String situation) {
        return HEADER + "\n" + situation + "\n" + INSTRUCTION;
    }

    // ---------------------------------------------------------------- needs

    public static VoiceRequest hungerPeckish(long tick) {
        return need(VoiceKind.HUNGER_PECKISH, tick, "Your stomach has started to feel a little empty. It is not "
                + "urgent yet, but food keeps drifting into your thoughts. Hint at it rather than demanding anything.");
    }

    public static VoiceRequest hungerNoFood(long tick, HungerStage stage) {
        String how = stage == HungerStage.STARVING ? "You are starving and weak"
                : stage == HungerStage.VERY_HUNGRY ? "You are very hungry" : "You are getting hungry";
        return need(VoiceKind.HUNGER_NO_FOOD, tick, how + ", and there is no food in your hands or your bag, so "
                + "you cannot feed yourself. Ask your master for something to eat.");
    }

    public static VoiceRequest hungerSated(long tick) {
        return need(VoiceKind.HUNGER_SATED, tick, "You were really hungry a little while ago, but you have just "
                + "eaten and feel properly fed again.");
    }

    public static VoiceRequest hungerFull(long tick) {
        return need(VoiceKind.HUNGER_FULL, tick, "You have eaten so much that you are stuffed. Any more and you "
                + "would feel sick, so you do not want more food right now.");
    }

    public static VoiceRequest hurtLow(long tick) {
        return need(VoiceKind.HURT_LOW, tick, "You are hurt. About half your strength is gone and your wounds "
                + "sting.");
    }

    public static VoiceRequest hurtCritical(long tick) {
        return need(VoiceKind.HURT_CRITICAL, tick, "You are badly wounded and can barely stay on your feet. You "
                + "need help or rest right now.");
    }

    public static VoiceRequest healed(long tick) {
        return need(VoiceKind.HEALED, tick, "The wounds you had earlier have mostly healed and you feel much "
                + "better.");
    }

    private static VoiceRequest need(VoiceKind kind, long tick, String situation) {
        return new VoiceRequest(kind, kind.key(), situation, "llvoices.line." + kind.key(), tick);
    }

    // ---------------------------------------------------------------- status check-in

    public static VoiceRequest status(long tick, HungerStage hunger, float healthRatio, String tendency) {
        String hungerText = switch (hunger) {
            case STARVING -> "you are starving";
            case VERY_HUNGRY -> "you are very hungry";
            case HUNGRY -> "you are a bit hungry";
            case SATISFIED -> "you are comfortably fed";
            case FULL -> "you are full";
            case OVERFED -> "you have eaten far too much";
        };
        String healthText = healthRatio < 0.25F ? "you are badly hurt"
                : healthRatio < 0.5F ? "you are hurt"
                : healthRatio < 0.9F ? "you have a few scrapes" : "you are unhurt";
        String situation = "A quiet moment with your master. How you are doing: " + hungerText + ", " + healthText
                + ", and " + tendencyText(tendency) + ". Bring up whatever matters most to you right now: a "
                + "need, something you want, or simply how you feel.";
        String fallback = hunger.isHungry() ? "hungry" : healthRatio < 0.5F ? "hurt" : "fine";
        return new VoiceRequest(VoiceKind.STATUS, VoiceKind.STATUS.key(), situation,
                "llvoices.line.status." + fallback, tick);
    }

    // ---------------------------------------------------------------- mood (L&L tendency)

    public static VoiceRequest moodShift(long tick, String from, String to) {
        String situation = "Your feelings toward your master have shifted. Before, " + tendencyText(from)
                + ". Now, " + tendencyText(to) + ". Let a hint of this change show in what you say. You may "
                + "not fully understand it yourself.";
        return new VoiceRequest(VoiceKind.MOOD_SHIFT, VoiceKind.MOOD_SHIFT.key(), situation,
                "llvoices.line.mood." + to.toLowerCase(Locale.ROOT), tick);
    }

    static String tendencyText(String tendency) {
        return switch (tendency.toUpperCase(Locale.ROOT)) {
            case "BOND" -> "you feel deeply bonded with your master, closer than any ordinary master and servant";
            case "FRIENDLY" -> "you like your master and feel comfortable relying on them";
            case "STRANGER" -> "you feel distant from your master, polite but not close";
            case "FEARFUL" -> "you are somewhat afraid of your master and tread carefully around them";
            case "TERRIFIED" -> "you are terrified of your master and want to stay away";
            case "CONFLICTED" -> "you depend on your master but also fear them, and feel torn";
            default -> "you have an ordinary, everyday relationship with your master";
        };
    }

    // ---------------------------------------------------------------- drift toward L&L's thresholds

    public static VoiceRequest drift(long tick, DriftZone zone, DriftLevel level, boolean lingering) {
        VoiceKind kind = lingering ? VoiceKind.DRIFT_LINGER
                : level == DriftLevel.BRINK ? VoiceKind.DRIFT_BRINK : VoiceKind.DRIFT_NEAR;
        String situation = driftText(zone, level);
        if (lingering) {
            situation = "This feeling has not gone away. " + situation;
        }
        String levelKey = level == DriftLevel.BRINK ? "brink" : "near";
        return new VoiceRequest(kind, kind.key() + "/" + zone.key(), situation,
                "llvoices.line.drift." + zone.key() + "." + levelKey, tick);
    }

    public static VoiceRequest easing(long tick, DriftZone zone) {
        String situation = switch (zone) {
            case BETRAYAL -> "The worst of your resentment and dread toward your master has started to ease. You "
                    + "can breathe again, though you are still wary.";
            case FORGETTING -> "You have started to feel something for your master again. The numbness is "
                    + "lifting a little.";
            case DOTING -> "Your fixation on having your master all to yourself has cooled a little. You still "
                    + "care deeply, but you feel calmer.";
            case DEVOTED -> "The feverish intensity of your loyalty has eased slightly and you feel steadier.";
            case PANIC -> "You have calmed down around your master. Your heart is not racing any more.";
        };
        return new VoiceRequest(VoiceKind.DRIFT_EASING, VoiceKind.DRIFT_EASING.key() + "/" + zone.key(), situation,
                "llvoices.line.drift." + zone.key() + ".easing", tick);
    }

    static String driftText(DriftZone zone, DriftLevel level) {
        boolean brink = level == DriftLevel.BRINK;
        return switch (zone) {
            case BETRAYAL -> brink
                    ? "You are at breaking point with your master. Fear has hardened into something cold, and "
                    + "one more cruelty might make you turn on them. Let it show how close to the edge you are: "
                    + "a warning, a tremble, or a quiet threat."
                    : "Resentment and fear toward your master have been piling up. You have not snapped, but you "
                    + "catch yourself flinching when they move, biting back sharp words, and wondering how much "
                    + "more you can take. Let one of these signs slip out, subtly.";
            case FORGETTING -> brink
                    ? "You barely feel anything for your master any more. Part of you is already thinking about "
                    + "simply leaving and letting all of this fade. It sounds like the start of a goodbye, even "
                    + "though you have not decided."
                    : "You feel strangely numb about your master lately. You do not fear them, but you do not "
                    + "feel much for them either, and you have started to wonder whether you matter here at all. "
                    + "Let that hollowness show.";
            case DOTING -> brink
                    ? "You adore your master so much it is turning possessive. The thought of them caring for "
                    + "another maid makes your chest tight, and you want them all to yourself. Be sweet but "
                    + "unmistakably possessive."
                    : "You are growing very attached to your master. You want to be near them all the time and "
                    + "feel a little jealous when they pay attention to anyone else. Let that clinginess slip out.";
            case DEVOTED -> brink
                    ? "You would throw your life away for your master without a second thought, and that "
                    + "frightens you a little. Show how fanatically devoted you have become."
                    : "Your loyalty to your master is turning intense. You trust them completely, yet you also fear "
                    + "them deeply, and the mix makes you want to prove yourself at any cost. Let that fervour show.";
            case PANIC -> brink
                    ? "You are close to panic whenever your master comes near, and every instinct tells you to run. "
                    + "Say something shaky that shows you are about to bolt."
                    : "Being around your master puts you on edge. Your heart races when they come close and you "
                    + "keep a step of distance without meaning to. Let your nerves show.";
        };
    }

    // ---------------------------------------------------------------- passive emotion changes

    public static VoiceRequest passive(long tick, ChangeCause cause, int deltaTrust, int deltaFear) {
        String situation = causeText(cause) + " " + deltaText(deltaTrust, deltaFear) + " React to it.";
        return new VoiceRequest(VoiceKind.PASSIVE, VoiceKind.PASSIVE.key(), situation,
                "llvoices.line.passive." + cause.key(), tick);
    }

    static String causeText(ChangeCause cause) {
        return switch (cause) {
            case HURT_BY_OWNER -> "Your master just hit you.";
            case HURT -> "You just got hurt.";
            case MAID_DIED_NEARBY -> "A fellow maid just died close to you.";
            case FED -> "You just had something to eat.";
            case SAT_DOWN -> "Your master just told you to sit down and stay put.";
            case SLEPT -> "You have just lain down to sleep and are drifting off.";
            case ARMOR_COMPLETE -> "Your master has just finished kitting you out in a full set of armour.";
            case IGNORED -> "Your master has not paid you any attention for a while.";
            case TIME_TOGETHER -> "You have been spending time around your master, and being together is slowly "
                    + "starting to feel natural.";
            case WORKING -> "You have been busy working, and being useful keeps your mind off your worries.";
            case RESTING -> "You are on a break and taking it easy for a moment.";
            case COMPANIONS -> "Other maids are nearby, and having company makes you feel safer.";
            case BUFFED -> "You feel a pleasant boost from a helpful effect.";
            default -> "Something just shifted in how you feel about your master.";
        };
    }

    static String deltaText(int deltaTrust, int deltaFear) {
        StringBuilder text = new StringBuilder("Because of this,");
        boolean any = false;
        if (deltaTrust != 0) {
            text.append(" you trust your master ").append(amount(deltaTrust)).append(deltaTrust > 0 ? " more" : " less");
            any = true;
        }
        if (deltaFear != 0) {
            text.append(any ? " and" : "").append(" you feel ").append(amount(deltaFear))
                    .append(deltaFear > 0 ? " more afraid of them" : " less afraid of them");
            any = true;
        }
        return any ? text.append('.').toString() : "";
    }

    private static String amount(int delta) {
        int size = Math.abs(delta);
        return size >= 5 ? "much" : size >= 3 ? "noticeably" : "a little";
    }
}
