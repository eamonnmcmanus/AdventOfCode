
package advent2025;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author Éamonn McManus
 */
public class Puzzle9 {
  private static final String SAMPLE =
      """
      7,1
      11,1
      11,7
      9,7
      9,5
      2,5
      2,3
      7,3
      """;

  private static final Map<String, Callable<Reader>> INPUT_PRODUCERS =
      ImmutableMap.of(
          "sample", () -> new StringReader(SAMPLE),
          "problem", () -> new InputStreamReader(Puzzle1.class.getResourceAsStream("puzzle9.txt")));

  public static void main(String[] args) throws Exception {
    for (var entry : INPUT_PRODUCERS.entrySet()) {
      String name = entry.getKey();
      try (Reader r = entry.getValue().call()) {
        List<String> lines = r.readAllLines();
        List<Point> points = lines.stream().map(Point::parse).toList();
        long max = 0;
        for (int i = 0; i < points.size(); i++) {
          for (int j = i + 1; j < points.size(); j++) {
            max = Math.max(max, points.get(i).areaTo(points.get(j)));
          }
        }
        System.out.printf("For %s, part 1 solution is %d\n", name, max);
      }
    }
  }

  private record Point(int x, int y) {
    static Point parse(String s) {
      List<String> numbers = Splitter.on(',').splitToList(s);
      checkArgument(numbers.size() == 2, "%s", s);
      return new Point(Integer.parseInt(numbers.get(0)), Integer.parseInt(numbers.get(1)));
    }

    long areaTo(Point that) {
      return (long) (Math.abs(this.x - that.x) + 1) * (Math.abs(this.y - that.y) + 1);
    }
  }
}