package advent2025;

import com.google.common.collect.ImmutableMap;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author Éamonn McManus
 */
public class Puzzle3 {
  private static final String SAMPLE =
      """
      987654321111111
      811111111111119
      234234234234278
      818181911112111
      """;

  private static final Map<String, Callable<Reader>> INPUT_PRODUCERS =
      ImmutableMap.of(
          "sample", () -> new StringReader(SAMPLE),
          "problem", () -> new InputStreamReader(Puzzle3.class.getResourceAsStream("puzzle3.txt")));

  public static void main(String[] args) throws Exception {
    for (var entry : INPUT_PRODUCERS.entrySet()) {
      String name = entry.getKey();
      try (Reader r = entry.getValue().call()) {
        List<String> lines = r.readAllLines();
        long part1Sum = 0;
        long part2Sum = 0;
        for (String line : lines) {
          List<Integer> digits = line.chars().map(i -> i - '0').mapToObj(i -> i).toList();
          part1Sum += part1Sum(digits);
          part2Sum += part2Sum(digits, 12);
        }
        System.out.printf("part 1 sum for %s is %d\n", name, part1Sum);
        System.out.printf("part 2 sum for %s is %d\n", name, part2Sum);
      }
    }
  }

  private static long part1Sum(List<Integer> digits) {
    int max = Integer.MIN_VALUE;
    int maxI = -1;
    for (int i = 0; i < digits.size() - 1; i++) {
      if (digits.get(i) > max) {
        max = digits.get(i);
        maxI = i;
      }
    }
    int max2 = Collections.max(digits.subList(maxI + 1, digits.size()));
    return 10 * max + max2;
  }

  /// The principle here and in [#part1Sum] is that the largest number must start with the largest
  /// digit that has sufficiently many digits after it. So in the 12-digit case, the first digit
  /// must be the largest digit in the list, as long as it has at least 11 other digits after it.
  /// And we should use the first occurrence of that digit, so as to leave the most possibilities
  /// for the remaining digits. Then it's just recursion to find each of the remaining digits.
  private static long part2Sum(List<Integer> digits, int digitCount) {
    if (digitCount == 0) {
      return 0;
    }
    long multiplier = Math.powExact(10L, digitCount - 1);
    int stop = digits.size() - digitCount + 1; // digitCount = 2 => stop before size - 1
    int max = Integer.MIN_VALUE;
    int maxI = -1;
    for (int i = 0; i < stop; i++) {
      if (digits.get(i) > max) {
        max = digits.get(i);
        maxI = i;
      }
    }
    long rest = part2Sum(digits.subList(maxI + 1, digits.size()), digitCount - 1);
    return max * multiplier + rest;
  }
}
