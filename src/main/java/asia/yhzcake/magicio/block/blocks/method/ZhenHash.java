package asia.yhzcake.magicio.block.blocks.method;

import asia.yhzcake.magicio.MagicIO;
import java.util.HashMap;
import java.util.Map;

public class ZhenHash {
  public Map<String, Runnable> zhenMap;

  public ZhenHash() {
    zhenMap = new HashMap<>();
  }

  public static void defaultMethod() {
    ZhenMethod.ZHENMETHOD.addMethod("wind", windMethod::windtick);
  }

  public void addMethod(String name, Runnable runnable) {
    zhenMap.put(name, runnable);
  }

  public void executeMethod(String name) {
    Runnable runnable = zhenMap.get(name);
    if (runnable != null) {
      runnable.run();
    } else {
      MagicIO.LOGGER.error("Method not found: {}", name);
    }
  }
}
