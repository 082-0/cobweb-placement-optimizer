package dev.cobweb;
import net.minecraft.util.Util;
import java.net.URI;
public final class CobwebLinks {
 public static boolean openProfile() {
  Util.getOperatingSystem().open(URI.create("https://github.com/082-0/cobweb-placement-optimizer"));
  return true;
 }
}

