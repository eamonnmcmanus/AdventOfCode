package advent2025;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableRangeSet.toImmutableRangeSet;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author Éamonn McManus
 */
public class Puzzle5 {
  private static final String SAMPLE =
      """
      3-5
      10-14
      16-20
      12-18

      1
      5
      8
      11
      17
      32
      """;

  private static final Map<String, Callable<Reader>> INPUT_PRODUCERS =
      ImmutableMap.of(
          "sample", () -> new StringReader(SAMPLE),
          "problem", () -> new InputStreamReader(Puzzle1.class.getResourceAsStream("puzzle5.txt")));

  public static void main(String[] args) throws Exception {
    for (var entry : INPUT_PRODUCERS.entrySet()) {
      String name = entry.getKey();
      try (Reader r = entry.getValue().call()) {
        List<String> lines = r.readAllLines();
        int empty = lines.indexOf("");
        checkState(empty > 0);
        RangeSet<Long> ranges = TreeRangeSet.create();
        lines.subList(0, empty).stream()
            .map(line -> Splitter.on('-').splitToList(line))
            .map(list -> Range.closed(Long.valueOf(list.get(0)), Long.valueOf(list.get(1))))
            .forEach(ranges::add);
        long count =
            lines.subList(empty + 1, lines.size()).stream()
                .map(Long::valueOf)
                .filter(ranges::contains)
                .count();
        System.out.printf("Part 1 solution for %s is %d\n", name, count);
        long total =
            ranges.asRanges().stream()
                .mapToLong(range -> range.upperEndpoint() - range.lowerEndpoint() + 1)
                .sum();
        System.out.printf("Part 2 solution for %s is %d\n", name, total);
      }
    }
  }
}