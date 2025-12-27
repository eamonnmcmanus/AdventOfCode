package advent2025;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableRangeSet.toImmutableRangeSet;

import com.google.common.base.Splitter;
import com.google.common.collect.BoundType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * @author Éamonn McManus
 */
public class Puzzle2 {
  private static final String SAMPLE =
      """
      11-22,95-115,998-1012,1188511880-1188511890,222220-222224,\
      1698522-1698528,446443-446449,38593856-38593862,565653-565659,\
      824824821-824824827,2121212118-2121212124
      """;

  private static final Map<String, Callable<Reader>> INPUT_PRODUCERS =
      ImmutableMap.of(
          "sample", () -> new StringReader(SAMPLE),
          "problem", () -> new InputStreamReader(Puzzle2.class.getResourceAsStream("puzzle2.txt")));

  public static void main(String[] args) throws Exception {
    for (var entry : INPUT_PRODUCERS.entrySet()) {
      String name = entry.getKey();
      try (Reader r = entry.getValue().call()) {
        String text = r.readAllAsString().trim();
        RangeSet<Long> ranges = parseRanges(text);
        long part1Sum = 0;
        long part2Sum = 0;
        for (var range : ranges.asRanges()) {
          part1Sum = Math.addExact(part1Sum, part1Sum(range));
          part2Sum = Math.addExact(part2Sum, part2Sum(range));
        }
        System.out.printf("For %s, part 1 sum is %d\n", name, part1Sum);
        System.out.printf("For %s, part 2 sum is %d\n", name, part2Sum);
      }
    }
  }

  /**
   * Returns the sum of all numbers in the given range that consist of the same sequence of digits
   * repeated twice, like 123123. First, if the start of the range has fewer digits than the end, we
   * split into subranges. For example, we would split 90-120 into 90-99 and 100-120. Then, for a
   * range where start and end both have N digits, the sum is obviously 0 if N is odd. Otherwise it
   * is the sum of multiples in the range of the appropriate-sized "duplicate multiplier", such as
   * 11 or 101 or 1001 etc (respectively for 2, 4, or 6 digits). If the range is 1234–5678, the
   * duplicate multiplier is 101, the factor for the first multiple is ⌈1234/101⌉ = 13, and for the
   * last multiple is ⌊5678/101⌋ = 56. They form an arithmetic series, so we could easily compute
   * their sum in a single operation, but out of laziness we just use a loop.
   */
  private static long part1Sum(Range<Long> range) {
    checkArgument(range.lowerBoundType() == BoundType.CLOSED);
    checkArgument(range.upperBoundType() == BoundType.CLOSED);
    int startDigits = digitCount(range.lowerEndpoint());
    int endDigits = digitCount(range.upperEndpoint());
    if (startDigits != endDigits) {
      long nines = Long.parseLong("9".repeat(startDigits));
      return part1Sum(Range.closed(range.lowerEndpoint(), nines))
          + part1Sum(Range.closed(nines + 1, range.upperEndpoint()));
    }
    if (startDigits % 2 == 1) {
      return 0;
    }
    long multiplier = Long.parseLong("1" + "0".repeat(startDigits / 2 - 1) + "1");
    long first = Math.ceilDiv(range.lowerEndpoint(), multiplier);
    long sum = 0;
    for (long i = first * multiplier; i <= range.upperEndpoint(); i += multiplier) {
      sum = Math.addExact(sum, i);
    }
    return sum;
  }

  /**
   * Returns the sum of all numbers in the given range that consist of the same sequence of digits
   * repeated twice or more, like 123123 or 123123123 or 11111. We just use brute force here, which
   * we could have done in the earlier one too.
   */
  private static long part2Sum(Range<Long> range) {
    checkArgument(range.lowerBoundType() == BoundType.CLOSED);
    checkArgument(range.upperBoundType() == BoundType.CLOSED);
    // This is just an optimization, to avoid having to compute digit counts inside the loop.
    int startDigits = digitCount(range.lowerEndpoint());
    int endDigits = digitCount(range.upperEndpoint());
    if (startDigits != endDigits) {
      long nines = Long.parseLong("9".repeat(startDigits));
      return part2Sum(Range.closed(range.lowerEndpoint(), nines))
          + part2Sum(Range.closed(nines + 1, range.upperEndpoint()));
    }
    Set<Integer> divisors = new LinkedHashSet<>();
    for (int i = 1; i < startDigits; i++) {
      if (startDigits % i == 0) {
        divisors.add(i);
      }
    }
    long sum = 0;
    nextNumber:
    for (long i = range.lowerEndpoint(); i <= range.upperEndpoint(); i++) {
      char[] digits = String.valueOf(i).toCharArray();
      nextDivisor:
      for (int d : divisors) {
        for (int j = 0; j < d; j++) {
          char digit = digits[j];
          for (int k = j; k < startDigits; k += d) {
            if (digits[k] != digit) {
              continue nextDivisor;
            }
          }
        }
        sum = Math.addExact(sum, i);
        continue nextNumber;
      }
    }
    return sum;
  }

  private static RangeSet<Long> parseRanges(String text) {
    return Splitter.on(',')
        .splitToStream(text)
        .map(s -> Splitter.on('-').splitToList(s))
        .map(list -> Range.closed(Long.valueOf(list.get(0)), Long.valueOf(list.get(1))))
        .collect(toImmutableRangeSet());
  }

  private static int digitCount(long n) {
    return Long.toString(n).length();
  }
}
