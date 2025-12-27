package advent2025;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Booleans;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;

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
          "problem", () -> new InputStreamReader(Puzzle9.class.getResourceAsStream("puzzle9.txt")));

  public static void main(String[] args) throws Exception {
    for (var entry : INPUT_PRODUCERS.entrySet()) {
      String name = entry.getKey();
      try (Reader r = entry.getValue().call()) {
        List<String> lines = r.readAllLines();
        List<Point> points = lines.stream().map(Point::parse).toList();
        System.out.printf("For %s, part 1 solution is %d\n", name, part1(points));
        System.out.printf("For %s, part 2 solution is %d\n", name, part2(points));
      }
    }
  }

  private static long part1(List<Point> points) {
    long max = 0;
    for (int i = 0; i < points.size(); i++) {
      for (int j = i + 1; j < points.size(); j++) {
        max = Math.max(max, points.get(i).areaTo(points.get(j)));
      }
    }
    return max;
  }

  // The problem here is to take an orthogonal path joining the given points, fill it, and determine
  // the largest fully-filled area between any two of the original points. The points have 5-digit
  // coordinates so doing this literally would be very expensive. Instead, we observe that if we
  // have two points where no other point has an x coordinate between them, we can shrink the
  // intervening gap to just 1, and similarly for y. Then we can fill the resulting grid. If we map
  // from original points to points in the shrunk grid correctly, we can determine whether the area
  // between two original points is filled, by checking whether the area between the corresponding
  // mapped points is filled, which should be much cheaper. If we have nx distinct x coordinates and
  // ny distinct y coordinates, a grid of size (2*nx - 1) × (2*ny - 1) suffices.

  private static long part2(List<Point> points) {
    return new Part2(points).result();
  }

  private static class Part2 {
    private final ImmutableList<Point> points;
    private final ImmutableList<Point> shrunkPoints;
    private final boolean[][] grid;

    Part2(List<Point> points) {
      this.points = ImmutableList.copyOf(points);

      List<Integer> xs = points.stream().map(Point::x).sorted().distinct().toList();
      List<Integer> ys = points.stream().map(Point::y).sorted().distinct().toList();
      Map<Integer, Integer> xToShrunkX =
          IntStream.range(0, xs.size())
              .mapToObj(i -> i)
              .collect(toImmutableMap(i -> xs.get(i), i -> 2 * i));
      Map<Integer, Integer> yToShrunkY =
          IntStream.range(0, ys.size())
              .mapToObj(i -> i)
              .collect(toImmutableMap(i -> ys.get(i), i -> 2 * i));
      shrunkPoints =
          points.stream()
              .map(p -> new Point(xToShrunkX.get(p.x), yToShrunkY.get(p.y)))
              .collect(toImmutableList());
      this.grid = new boolean[2 * xs.size() - 1][2 * ys.size() - 1];
    }

    private Point unitVector(Point from, Point to) {
      if (from.x == to.x) {
        checkState(from.y != to.y);
        return new Point(0, Integer.signum(to.y - from.y));
      } else if (from.y == to.y) {
        return new Point(Integer.signum(to.x - from.x), 0);
      } else {
        throw new IllegalStateException("Both x and y differ: " + from + " - " + to);
      }
    }

    private void fillPath() {
      Point prev = shrunkPoints.getLast();
      for (Point p : shrunkPoints) {
        Point unit = unitVector(prev, p);
        for (Point q = prev; !q.equals(p); q = q.plus(unit)) {
          grid[q.x][q.y] = true;
        }
        prev = p;
      }
    }

    private static final ImmutableList<Point> UNITS =
        ImmutableList.of(new Point(-1, 0), new Point(+1, 0), new Point(0, -1), new Point(0, +1));

    /// Fills the region inside the path.
    private void fill() {
      // We know that the x=0 line must include a top-left corner, and we can start filling from the
      // point that is down and to the right from that corner.
      int startY = Booleans.indexOf(grid[0], true);
      checkState(startY >= 0);
      Point start = new Point(1, startY + 1);
      checkState(!grid[start.x][start.y]);
      Queue<Point> queue = new ArrayDeque<>(Set.of(start));
      while (!queue.isEmpty()) {
        Point p = queue.remove();
        if (!grid[p.x][p.y]) {
          grid[p.x][p.y] = true;
          for (Point unit : UNITS) {
            queue.add(p.plus(unit));
          }
        }
      }
    }

    /// True if the rectangular region between two shrunk points is entirely filled.
    private boolean isFilled(Point a, Point b) {
      int startX = Math.min(a.x, b.x);
      int stopX = Math.max(a.x, b.x);
      int startY = Math.min(a.y, b.y);
      int stopY = Math.max(a.y, b.y);
      for (int x = startX; x <= stopX; x++) {
        for (int y = startY; y <= stopY; y++) {
          if (!grid[x][y]) {
            return false;
          }
        }
      }
      return true;
    }

    long result() {
      fillPath();
      fill();
      long max = 0;
      for (int i = 0; i < points.size(); i++) {
        for (int j = i + 1; j < points.size(); j++) {
          if (isFilled(shrunkPoints.get(i), shrunkPoints.get(j))){
            max = Math.max(max, points.get(i).areaTo(points.get(j)));
          }
        }
      }
      return max;
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

    Point plus(Point vector) {
      return new Point(x + vector.x, y + vector.y);
    }
  }
}