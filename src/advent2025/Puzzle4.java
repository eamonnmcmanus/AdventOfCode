package advent2025;

import adventlib.CharGrid;
import adventlib.CharGrid.Coord;
import adventlib.Dir;
import com.google.common.collect.ImmutableMap;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author Éamonn McManus
 */
public class Puzzle4 {
  private static final String SAMPLE =
      """
      ..@@.@@@@.
      @@@.@.@.@@
      @@@@@.@.@@
      @.@@@@..@.
      @@.@@@@.@@
      .@@@@@@@.@
      .@.@.@.@@@
      @.@@@.@@@@
      .@@@@@@@@.
      @.@.@@@.@.
      """;

  private static final Map<String, Callable<Reader>> INPUT_PRODUCERS =
      ImmutableMap.of(
          "sample", () -> new StringReader(SAMPLE),
          "problem", () -> new InputStreamReader(Puzzle4.class.getResourceAsStream("puzzle4.txt")));

  public static void main(String[] args) throws Exception {
    for (var entry : INPUT_PRODUCERS.entrySet()) {
      String name = entry.getKey();
      try (Reader r = entry.getValue().call()) {
        List<String> lines = r.readAllLines();
        CharGrid grid = new CharGrid(lines);
        System.out.printf("For %s, part 1 solution is %d\n", name, part1(grid));
        System.out.printf("For %s, part 2 solution is %d\n", name, part2(grid));
      }
    }
  }

  private static int part1(CharGrid grid) {
    int accessible = 0;
    for (var coord : grid.coords()) {
      if (grid.get(coord) == '@') {
        int occupied = neighbourCount(grid, coord);
        if (occupied < 4) {
          accessible++;
        }
      }
    }
    return accessible;
  }

  private static int neighbourCount(CharGrid grid, Coord coord) {
    int occupied = 0;
    for (var dir : EnumSet.allOf(Dir.class)) {
      var neighbour = dir.move(coord);
      if (grid.get(neighbour) == '@') {
        occupied++;
      }
    }
    return occupied;
  }

  /// For part2, we can maintain an evolving count of the number of neighbouring rolls for each
  /// roll. A negative value in the grid indicates no roll. Then we can remove a roll that has fewer
  /// than 4 neighbours, and subtract 1 from the count for each of its neighbours. (We can subtract
  /// 1 even from the empty neighbours, since that will just make the value more negative.)
  ///
  /// This is not _terribly_ efficient, since we keep returning to the start. But it's fast enough.
  private static int part2(CharGrid grid) {
    int[][] neighbours = new int[grid.height()][grid.width()];
    for (var coord : grid.coords()) {
      neighbours[coord.line()][coord.col()] =
          (grid.get(coord) == '@') ? neighbourCount(grid, coord) : -1;
    }
    // Now repeatedly find a coord with a neighbour count < 4 and remove it, subtracting 1 from each
    // of its neighbours' counts.
    boolean changed;
    int total = 0;
    do {
      changed = false;
      for (var coord : grid.coords()) {
        var count = neighbours[coord.line()][coord.col()];
        if (count >= 0 && count < 4) {
          total++;
          changed = true;
          neighbours[coord.line()][coord.col()] = -1;
          for (var dir : EnumSet.allOf(Dir.class)) {
            var neighbour = dir.move(coord);
            if (grid.valid(neighbour)) {
              neighbours[neighbour.line()][neighbour.col()]--;
            }
          }
          // Now we've removed a roll. We could continue the outer loop here, but actually we might
          // as well continue examining the coords after here without restarting.
        }
      }
    } while (changed);
    return total;
  }
}