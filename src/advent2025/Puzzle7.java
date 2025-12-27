package advent2025;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableMap;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author Éamonn McManus
 */
public class Puzzle7 {
  private static final String SAMPLE =
      """
      .......S.......
      ...............
      .......^.......
      ...............
      ......^.^......
      ...............
      .....^.^.^.....
      ...............
      ....^.^...^....
      ...............
      ...^.^...^.^...
      ...............
      ..^...^.....^..
      ...............
      .^.^.^.^.^...^.
      ...............
      """;

  private static final Map<String, Callable<Reader>> INPUT_PRODUCERS =
      ImmutableMap.of(
          "sample", () -> new StringReader(SAMPLE),
          "problem", () -> new InputStreamReader(Puzzle7.class.getResourceAsStream("puzzle7.txt")));

  public static void main(String[] args) throws Exception {
    for (var entry : INPUT_PRODUCERS.entrySet()) {
      String name = entry.getKey();
      try (Reader r = entry.getValue().call()) {
        List<String> lines = r.readAllLines();
        checkState(lines.stream().allMatch(line -> line.length() == lines.getFirst().length()));
        System.out.printf("For %s, %d splits\n", name, part1(lines));
        System.out.printf("For %s, %d timelines\n", name, part2(lines));
      }
    }
  }

  private static int part1(List<String> lines) {
    BitSet beams = new BitSet();
    beams.set(lines.get(0).indexOf('S'));
    int splits = 0;
    for (String line : lines) {
      BitSet nextBeams = new BitSet();
      for (int i = 0; i < line.length(); i++) {
        if (line.charAt(i) == '^') {
          if (beams.get(i)) {
            nextBeams.set(i - 1);
            nextBeams.set(i + 1);
            splits++;
          }
        } else {
          nextBeams.set(i, nextBeams.get(i) | beams.get(i));
        }
      }
      beams = nextBeams;
    }
    return splits;
  }

  private static long part2(List<String> lines) {
    int len = lines.getFirst().length();
    long[] timelines = new long[len];
    timelines[lines.getFirst().indexOf('S')] = 1;
    for (String line : lines) {
      long[] nextTimelines = new long[len];
      for (int i = 0; i < line.length(); i++) {
        if (line.charAt(i) == '^') {
          nextTimelines[i - 1] += timelines[i];
          nextTimelines[i + 1] += timelines[i];
        } else {
          nextTimelines[i] += timelines[i];
        }
      }
      timelines = nextTimelines;
    }
    return Arrays.stream(timelines).sum();
  }
}