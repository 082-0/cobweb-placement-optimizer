package dev.cobweb;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

public final class CobwebConfig {
	public static final int DEFAULT_DELAY_MILLIS = 100;
	public static final int MIN_DELAY_MILLIS = 0;
	public static final int MAX_DELAY_MILLIS = 5000;

	public static volatile int color = 0xA855F7; public static volatile int style = 1;
 public static int tint() {
  if (style == 0) return 0xFFFFFF;
  if (style == 1) return color;
  double wave = (Math.sin(System.nanoTime() / 350_000_000.0) + 1) * 0.25;
  int r=(color >> 16)&255, g=(color >> 8)&255, b=color&255;
  return ((int)(r+(255-r)*wave)<<16)|((int)(g+(255-g)*wave)<<8)|(int)(b+(255-b)*wave);
 }
 private static int delayMillis = DEFAULT_DELAY_MILLIS;
 private static int displacedHotbarIndex = -1;
 private static String displacedHotbarKey = "";

	private CobwebConfig() {
	}

	public static void load() {
		Path path = getConfigPath();
		if (!Files.isRegularFile(path)) {
			return;
		}

		Properties properties = new Properties();
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			properties.load(reader); displacedHotbarIndex = Integer.parseInt(properties.getProperty("displacedHotbarIndex","-1")); displacedHotbarKey = properties.getProperty("displacedHotbarKey",""); color = Integer.parseInt(properties.getProperty("color","A855F7"),16) & 0xFFFFFF; style = Math.max(0,Math.min(2,Integer.parseInt(properties.getProperty("style","1"))));
			delayMillis = clamp(Integer.parseInt(properties.getProperty("delayMillis", String.valueOf(DEFAULT_DELAY_MILLIS))));
		} catch (IOException | NumberFormatException ignored) {
			delayMillis = DEFAULT_DELAY_MILLIS;
		}
	}

	public static int getDisplacedHotbarIndex() { return displacedHotbarIndex; }
 public static String getDisplacedHotbarKey() { return displacedHotbarKey; }
 public static void setDisplacedHotbar(int index, String key) { displacedHotbarIndex=index; displacedHotbarKey=key; save(); }
 public static int getDelayMillis() {
		return delayMillis;
	}

	public static boolean setDelayMillis(int value) {
		if (value < MIN_DELAY_MILLIS || value > MAX_DELAY_MILLIS) {
			return false;
		}
		delayMillis = value;
		return true;
	}

	public static boolean save() {
		Properties properties = new Properties();
		properties.setProperty("displacedHotbarIndex",String.valueOf(displacedHotbarIndex)); properties.setProperty("displacedHotbarKey",displacedHotbarKey);
 properties.setProperty("color",String.format("%06X",color)); properties.setProperty("style",String.valueOf(style));
 properties.setProperty("delayMillis", String.valueOf(delayMillis));

		Path path = getConfigPath();
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				properties.store(writer, "Cobweb settings");
			}
			return true;
		} catch (IOException ignored) {
			return false;
		}
	}

	private static int clamp(int value) {
		return Math.max(MIN_DELAY_MILLIS, Math.min(MAX_DELAY_MILLIS, value));
	}

	private static Path getConfigPath() {
		return FabricLoader.getInstance().getConfigDir().resolve("cobweb.properties");
	}
}





