package advent2025;

import static java.lang.Math.floorMod;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author Éamonn McManus
 */
public class Puzzle1 {
  private static final String SAMPLE =
      """
      L68
      L30
      R48
      L5
      R60
      L55
      L1
      L99
      R14
      L82
      """;

  private static final Map<String, Callable<Reader>> INPUT_PRODUCERS =
      ImmutableMap.of(
          "sample", () -> new StringReader(SAMPLE),
          "problem", () -> new InputStreamReader(Puzzle1.class.getResourceAsStream("puzzle1.txt")));

  public static void main(String[] args) throws Exception {
    for (var entry : INPUT_PRODUCERS.entrySet()) {
      String name = entry.getKey();
      try (Reader r = entry.getValue().call()) {
        List<String> lines = CharStreams.readLines(r);
        int zeroes = 0;
        int pastZeroes = 0;
        int position = 50;
        for (String line : lines) {
          int sign = switch (line.charAt(0)) {
            case 'R' -> +1;
            case 'L' -> -1;
            default -> throw new IllegalArgumentException(line);
          };
          int amount = Integer.parseInt(line.substring(1));
          Result state = turn(position, sign * amount);
          position = state.position;
          if (position == 0) {
            zeroes++;
          }
          pastZeroes += state.zeroes;
        }
        System.out.printf("For %s, stopped at zero %d, passed zero %d\n", name, zeroes, pastZeroes);
      }
    }
  }

  record Result(int zeroes, int position) {}

  private static Result turn(int position, int amount) {
    if (amount > 0) {
      int newPosition = position + amount;
      return new Result(newPosition / 100, floorMod(newPosition, 100));
    } else if (amount < 0) {
      int newPosition = position + amount;
      // If we started from zero, then we passed zero as many times as there are multiples of 100
      // in newPosition. If we started from a positive number, then if we're still positive we
      // did not pass zero; if we're zero then that's one zero; and if we're negative we passed zero
      // newPosition / -100 + 1 times. That reduces to (newPosition - 100) / -100.
      int zeroes = position == 0 ? newPosition / -100 : (newPosition - 100) / -100;
      return new Result(zeroes, floorMod(newPosition, 100));
    } else {
      throw new IllegalStateException("Zero turns");
    }
  }
}
