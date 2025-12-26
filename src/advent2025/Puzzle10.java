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
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
        System.out.printf("For %s, part 2 solution is %d\n", name, part2(machines));
      }
    }
  }

  private static int part1(List<Machine> machines) {
    int total = 0;
    for (Machine machine : machines) {
      total += minPart1PushesFor(machine);
    }
    return total;
  }

  private static int minPart1PushesFor(Machine machine) {
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

  private static int part2(List<Machine> machines) throws InterruptedException, ExecutionException {
    int total = 0;
    ExecutorService executor =
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    CompletionService<Response> service = new ExecutorCompletionService<>(executor);
    for (int i = 0; i < machines.size(); i++) {
      int ii = i;
      Machine machine = machines.get(i);
      service.submit(new Solver(machine, ii));
    }
    long startTime = System.nanoTime();
    for (int remaining = machines.size(); remaining > 0; --remaining) {
      var response = service.take().get();
      long elapsed = System.nanoTime() - startTime;
      total += response.bestPushes;
      System.out.printf(
          "...machine #%d solved in %.2fs to get %d, elapsed time %.0fs, %d remaining\n",
          response.machineIndex,
          response.elapsed / 1e9,
          response.bestPushes,
          elapsed / 1e9,
          remaining - 1);
    }
    executor.shutdown();
    return total;
  }

  private record Response(int machineIndex, int bestPushes, long elapsed) {}

  private static class Solver implements Callable<Response> {
    private final Machine machine;
    private final int machineIndex;
    private final long startTime;
    private final int[] buttonMasks;

    Solver(Machine machine, int machineIndex) {
      this.machine = machine;
      this.machineIndex = machineIndex;
      this.startTime = System.nanoTime();
      // Sort the button masks so the ones that increment the most joltages come first.
      this.buttonMasks =
          machine.buttonMasks.stream()
              .sorted((a, b) -> Integer.compare(Integer.bitCount(b), Integer.bitCount(a)))
              .mapToInt(i -> i)
              .toArray();
    }

    @Override
    public Response call() throws Exception {
      int bestPushes = minPart2PushesFor(machine);
      long elapsed = System.nanoTime() - startTime;
      return new Response(machineIndex, bestPushes, elapsed);
    }

    private int minPart2PushesFor(Machine machine) {
      int[] joltages = machine.joltages.stream().mapToInt(i -> i).toArray();
      return solve(0, 0, Integer.MAX_VALUE, joltages);
    }

    private int solve(int buttonIndex, int pushesSoFar, int bestPushes, int[] joltages) {
      if (pushesSoFar >= bestPushes) {
        return bestPushes;
      }
      if (allZero(joltages)) {
        return pushesSoFar;
      }
      if (buttonIndex >= buttonMasks.length
          || cannotImprove(buttonIndex, joltages, bestPushes - pushesSoFar)) {
        return bestPushes;
      }
      // Determine how many times buttonIndex can be pushed without sending any joltages below zero.
      // This is simply the smallest value of any joltage that is affected by the button.
      int max = Integer.MAX_VALUE;
      for (int mask = buttonMasks[buttonIndex]; mask != 0; mask &= ~Integer.lowestOneBit(mask)) {
        int j = Integer.numberOfTrailingZeros(mask);
        max = Math.min(max, joltages[j]);
        if (max == 0) {
          break;
        }
      }
      for (int p = Math.min(max, bestPushes - pushesSoFar); p >= 0; --p) {
        if (buttonIndex == 0) {
          long elapsed = System.nanoTime() - startTime;
          if (elapsed > 600_000_000_000L) {
            System.out.printf(
                ".....machine #%d still running after %.0fs, level 0 iterations remaining: %d\n",
                machineIndex, elapsed / 1e9, p + 1);
          }
        }
        int[] newJoltages;
        if (p == 0) {
          newJoltages = joltages;
        } else {
          newJoltages = joltages.clone();
          for (int mask = buttonMasks[buttonIndex];
              mask != 0;
              mask &= ~Integer.lowestOneBit(mask)) {
            int j = Integer.numberOfTrailingZeros(mask);
            newJoltages[j] -= p;
          }
        }
        bestPushes = solve(buttonIndex + 1, pushesSoFar + p, bestPushes, newJoltages);
      }
      return bestPushes;
    }

    private boolean cannotImprove(int buttonIndex, int[] joltages, int remainingPushes) {
      // We can stop searching on this path if:
      // (1) there is a non-zero joltage which is not affected by any remaining button; or
      // (2) there is a joltage that would require more button pushes than the best number of button
      //     pushes we have already seen on another path.
      int mask = 0;
      for (int i = buttonIndex; i < buttonMasks.length; i++) {
        mask |= buttonMasks[i];
      }
      for (int i = 0; i < joltages.length; i++) {
        int j = joltages[i];
        if (j != 0 && (mask & (1 << i)) == 0) {
          return true;
        }
        if (j >= remainingPushes) {
          return true;
        }
      }
      return false;
    }

    private static boolean allZero(int[] joltages) {
      for (int i : joltages) {
        if (i != 0) {
          return false;
        }
      }
      return true;
    }
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