package advent2025;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.stream.Collectors.joining;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Éamonn McManus
 */
public class Puzzle10 {
  private static final String SAMPLE =
      """
      [.##.] (3) (1,3) (2) (2,3) (0,2) (0,1) {3,5,4,7}
      [...#.] (0,2,3,4) (2,3) (0,4) (0,1,2) (1,2,3,4) {7,5,12,7,2}
      [.###.#] (0,1,2,3,4) (0,3,4) (0,1,2,4,5) (1,2) {10,11,11,5,10,5}
      """;

  private static final Map<String, Callable<Reader>> INPUT_PRODUCERS =
      ImmutableMap.of(
          "sample", () -> new StringReader(SAMPLE),
          "problem",
              () -> new InputStreamReader(Puzzle1.class.getResourceAsStream("puzzle10.txt")));

  public static void main(String[] args) throws Exception {
    for (var entry : INPUT_PRODUCERS.entrySet()) {
      String name = entry.getKey();
      try (Reader r = entry.getValue().call()) {
        List<String> lines = r.readAllLines();
        List<Machine> machines = lines.stream().map(Machine::parse).toList();
        System.out.printf("For %s, part 1 solution is %d\n", name, part1(machines));
      }
    }
  }

  private static int part1(List<Machine> machines) {
    int total = 0;
    for (Machine machine : machines) {
      total += minPushesFor(machine);
    }
    return total;
  }

  private static int minPushesFor(Machine machine) {
    int nMasks = machine.buttonMasks.size();
    for (int pushes = 1; pushes <= nMasks; pushes++) {
      for (int i = 1; i < 1 << nMasks; i++) {
        if (Integer.bitCount(i) == pushes) {
          int lights = 0;
          for (int j = 0; j < nMasks; j++) {
            if ((i & (1 << j)) != 0) {
              lights ^= machine.buttonMasks.get(j);
            }
          }
          if (lights == machine.targetLights) {
            return pushes;
          }
        }
      }
    }
    throw new IllegalStateException("No solution for " + machine);
  }

  private record Machine(
      int nLights,
      int targetLights,
      ImmutableList<Integer> buttonMasks,
      ImmutableList<Integer> joltages) {
    private static final Pattern PATTERN =
        Pattern.compile("\\[(.+)\\] ((?:\\s\\([^)]*\\))+) \\s\\{(.*)\\}", Pattern.COMMENTS);

    static Machine parse(String line) {
      Matcher matcher = PATTERN.matcher(line);
      checkState(matcher.matches(), "%s", line);
      return new Machine(
          matcher.group(1).length(),
          parseTargetLights(matcher.group(1)),
          parseButtonMasks(matcher.group(2)),
          parseJoltages(matcher.group(3)));
    }

    private static int parseTargetLights(String s) {
      return Integer.parseInt(
          new StringBuilder(s.replace('.', '0').replace('#', '1')).reverse().toString(), 2);
    }

    private static ImmutableList<Integer> parseButtonMasks(String s) {
      return Splitter.on(' ')
          .omitEmptyStrings()
          .splitToStream(s)
          .map(Machine::parseButtonMask)
          .collect(toImmutableList());
    }

    private static int parseButtonMask(String s) {
      checkArgument(s.startsWith("(") && s.endsWith(")"));
      return Splitter.on(',')
          .splitToStream(s.substring(1, s.length() - 1))
          .mapToInt(n -> 1 << Integer.parseInt(n))
          .reduce(0, (x, y) -> x | y);
    }

    private static ImmutableList<Integer> parseJoltages(String s) {
      return Splitter.on(',').splitToStream(s).map(Integer::valueOf).collect(toImmutableList());
    }

    @Override
    public String toString() {
      return "[%s] %s {%s}"
          .formatted(
              new StringBuilder(Integer.toString((1 << nLights) | targetLights, 2))
                  .reverse()
                  .toString()
                  .replace('0', '.')
                  .replace('1', '#')
                  .replaceAll("#$", ""),
              buttonMasks.stream()
                  .map(i -> BitSet.valueOf(new long[] {i}))
                  .map(set -> set.toString().replaceAll("[{} ]", ""))
                  .map(s -> "(" + s + ")")
                  .collect(joining(" ")),
              joltages.stream().map(Object::toString).collect(joining(",")));
    }
  }
}