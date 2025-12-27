package advent2025;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.Callable;

/**
 * @author Éamonn McManus
 */
public class Puzzle8 {
  private static final String SAMPLE =
      """
      162,817,812
      57,618,57
      906,360,560
      592,479,940
      352,342,300
      466,668,158
      542,29,236
      431,825,988
      739,650,466
      52,470,668
      216,146,977
      819,987,18
      117,168,530
      805,96,715
      346,949,466
      970,615,88
      941,993,340
      862,61,35
      984,92,344
      425,690,689
      """;

  private static final Map<String, Callable<Reader>> INPUT_PRODUCERS =
      ImmutableMap.of(
          "sample", () -> new StringReader(SAMPLE),
          "problem", () -> new InputStreamReader(Puzzle8.class.getResourceAsStream("puzzle8.txt")));

  public static void main(String[] args) throws Exception {
    for (var entry : INPUT_PRODUCERS.entrySet()) {
      String name = entry.getKey();
      try (Reader r = entry.getValue().call()) {
        List<String> lines = r.readAllLines();
        List<Point> points = lines.stream().map(Point::parse).toList();
        NavigableMap<Long, PointPair> distances = new TreeMap<>();
        for (int i = 0; i < points.size(); i++) {
          Point pI = points.get(i);
          for (int j = i + 1; j < points.size(); j++) {
            PointPair pair = new PointPair(pI, points.get(j));
            PointPair old = distances.put(pair.squaredDistance(), pair);
            checkState(
                old == null, "%s and %s both have distance %s", pair, old, pair.squaredDistance());
          }
        }
        Map<Point, ImmutableSet<Point>> circuits = new HashMap<>();
        for (Point p : points) {
          circuits.put(p, ImmutableSet.of(p));
        }
        int target = name.equals("sample") ? 10 : 1000;
        int connections = 0;
        for (PointPair pair : distances.values()) {
          var set1 = circuits.get(pair.p1);
          var set2 = circuits.get(pair.p2);
          ImmutableSet<Point> union = ImmutableSet.copyOf(Sets.union(set1, set2));
          for (Point p : union) {
            circuits.put(p, union);
          }
          if (++connections == target) {
            long biggestProduct =
                circuits.values().stream()
                    .distinct()
                    .map(ImmutableSet::size)
                    .sorted(Comparator.reverseOrder())
                    .limit(3)
                    .reduce(1, Math::multiplyExact);
            System.out.printf("Part 1 solution for %s is %d\n", name, biggestProduct);
          }
          if (circuits.get(pair.p1).size() == points.size()) {
            System.out.printf("Part 2 solution for %s is %d\n", name, pair.p1.x * pair.p2.x);
            break;
          }
        }
      }
    }
  }

  private record Point(int x, int y, int z) implements Comparable<Point> {
    static Point parse(String s) {
      List<String> numbers = Splitter.on(',').splitToList(s);
      checkArgument(numbers.size() == 3, "%s", s);
      return new Point(
          Integer.parseInt(numbers.get(0)),
          Integer.parseInt(numbers.get(1)),
          Integer.parseInt(numbers.get(2)));
    }

    long squaredDistanceTo(Point that) {
      long xx = this.x - that.x;
      long yy = this.y - that.y;
      long zz = this.z - that.z;
      return Math.addExact(
          Math.addExact(Math.multiplyExact(xx, xx), Math.multiplyExact(yy, yy)),
          Math.multiplyExact(zz, zz));
    }

    private static final Comparator<Point> COMPARATOR =
        Comparator.comparingInt(Point::x).thenComparingInt(Point::y).thenComparingInt(Point::z);

    @Override
    public int compareTo(Point that) {
      return COMPARATOR.compare(this, that);
    }
  }

  private record PointPair(Point p1, Point p2) {
    PointPair {
      if (p1.compareTo(p2) > 0) {
        Point p = p1;
        p1 = p2;
        p2 = p;
      }
    }

    long squaredDistance() {
      return p1.squaredDistanceTo(p2);
    }
  }
}
