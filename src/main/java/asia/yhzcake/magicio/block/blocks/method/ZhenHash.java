package asia.yhzcake.magicio.block.blocks.method;

import asia.yhzcake.magicio.MagicIO;
import java.util.HashMap;
import java.util.Map;

public class ZhenHash {
  // 阵法类型对应tick事件的map
  public static ZhenHash ZHENMETHOD = new ZhenHash();
  // 下面是上面的实现,看不懂,能用就行(((
  public Map<String, Runnable> zhenMap;

  public ZhenHash() {
    zhenMap = new HashMap<>();
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
