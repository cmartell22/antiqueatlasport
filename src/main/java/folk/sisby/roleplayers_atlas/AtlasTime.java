package folk.sisby.roleplayers_atlas;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Dating for everything drawn on the map.
 * <p>
 * The day comes from the world's <em>game</em> time rather than its time of
 * day: a server can freeze the day cycle or hand each player their own time of
 * day, while game time keeps ticking, is the same for everyone connected, and
 * survives restarts. The real-world moment is stored as an instant so it reads
 * correctly in whatever zone the viewer happens to be in — but it is only ever
 * a caption, never something the mod sorts or reasons by, because the clock it
 * came from belongs to whoever wrote the mark.
 */
public final class AtlasTime {
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

	private AtlasTime() {
	}

	public static long gameDay() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null) return 0;
		return Math.max(0, client.world.getTime() / 24000L);
	}

	public static long realMillis() {
		return System.currentTimeMillis();
	}

	public static String realDate(long millis) {
		if (millis <= 0) return "";
		try {
			return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DATE);
		} catch (Exception e) {
			return "";
		}
	}

	/** "Day 214 ((25.07.2026))" — the real date is dropped when it isn't ours to show. */
	public static Text stamp(long day, long realTime) {
		String date = realDate(realTime);
		return date.isEmpty()
			? Text.translatable("gui.roleplayers_atlas.marker.stampPlain", day)
			: Text.translatable("gui.roleplayers_atlas.marker.stamp", day, date);
	}

	/** "As told by Name, day 214 ((25.07.2026))" for marks copied from someone else's scroll. */
	public static Text hearsay(String author, Long day, Long realTime) {
		if (day == null) return Text.translatable("gui.roleplayers_atlas.marker.hearsayPlain", author);
		String date = realTime == null ? "" : realDate(realTime);
		return date.isEmpty()
			? Text.translatable("gui.roleplayers_atlas.marker.hearsayDay", author, day)
			: Text.translatable("gui.roleplayers_atlas.marker.hearsay", author, day, date);
	}

	/** The name this client signs its scrolls with. */
	public static String selfName() {
		MinecraftClient client = MinecraftClient.getInstance();
		return client.player == null ? "" : client.player.getGameProfile().name();
	}

	/** Whether a mark came from someone else's hand. Stays true after it's verified — who told you doesn't change. */
	public static boolean isHearsay(folk.sisby.surveyor.landmark.Landmark landmark) {
		String source = landmark.get(AtlasComponents.SOURCE);
		return source != null && !source.isEmpty() && !source.equals(selfName());
	}

	/** Hearsay nobody has gone and checked yet — this is what the map draws faint. */
	public static boolean isUnverified(folk.sisby.surveyor.landmark.Landmark landmark) {
		return isHearsay(landmark) && landmark.get(AtlasComponents.CONFIRMED_DAY) == null;
	}
}
