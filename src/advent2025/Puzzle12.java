package advent2025;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableMultiset.toImmutableMultiset;
import static java.lang.Math.max;
import static java.lang.Math.min;

import com.google.common.base.Splitter;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multiset;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * @author Éamonn McManus
 */
public class Puzzle12 {
  private static final String SAMPLE =
      """
      0:
      ###
      ##.
      ##.

      1:
      ###
      ##.
      .##

      2:
      .##
      ###
      ##.

      3:
      ##.
      ###
      ##.

      4:
      ###
      #..
      ###

      5:
      ###
      .#.
      ###

      4x4: 0 0 0 0 2 0
      12x5: 1 0 1 0 2 2
      12x5: 1 0 1 0 3 2
      """;

  private static final Map<String, Callable<Reader>> INPUT_PRODUCERS =
      ImmutableMap.of(
          "sample", () -> new StringReader(SAMPLE),
          "problem",
              () -> new InputStreamReader(Puzzle12.class.getResourceAsStream("puzzle12.txt")));

  public static void main(String[] args) throws Exception {
    for (var entry : INPUT_PRODUCERS.entrySet()) {
      String name = entry.getKey();
      try (Reader r = entry.getValue().call()) {
        List<String> lines = r.readAllLines();
        Pattern newPolyomino = Pattern.compile("\\d+:");
        ImmutableSetMultimap.Builder<Polyomino, Polyomino> polyominoesBuilder =
            ImmutableSetMultimap.builder();
        int i;
        for (i = 0; i < lines.size() && newPolyomino.matcher(lines.get(i)).matches(); i += 5) {
          Polyomino p = Polyomino.parse(lines.subList(i + 1, i + 4));
          polyominoesBuilder.putAll(p, p.rotationsAndReflections());
        }
        var polyominoes = polyominoesBuilder.build();
        ImmutableList.Builder<Region> regionsBuilder = ImmutableList.builder();
        for (; i < lines.size(); i++) {
          regionsBuilder.add(Region.parse(lines.get(i), polyominoes));
        }
        ImmutableList<Region> regions = regionsBuilder.build();
        int successes = 0;
        for (Region region : regions) {
          boolean solved = region.solve();
          successes += solved ? 1 : 0;
        }
        System.out.printf("For %s, successes: %d\n", name, successes);
      }
    }
  }

  private static class Region {
    private final int width;
    private final int height;
    private final ImmutableSetMultimap<Polyomino, Polyomino> polyominoes;
    private final ImmutableMultiset<Polyomino> presents;
    private final BitSet grid;
    private final int presentTiles;

    private Region(
        int width,
        int height,
        ImmutableSetMultimap<Polyomino, Polyomino> polyominoes,
        ImmutableMultiset<Polyomino> presents) {
      this.width = width;
      this.height = height;
      this.polyominoes = polyominoes;
      this.presents = presents;
      this.grid = new BitSet(width * height);
      this.presentTiles =
          presents.entrySet().stream()
              .mapToInt(entry -> entry.getElement().bitCount * entry.getCount())
              .sum();
    }

    boolean solve() {
      return solve(HashMultiset.create(presents), presentTiles, 0);
    }

    // This brute-force solver would have no chance of succeeding if the problem inputs required the
    // same degree of packing as the samples. But it turns out they do not. :-)
    private boolean solve(Multiset<Polyomino> remainingPresents, int remainingTiles, int startBit) {
      if (remainingPresents.isEmpty()) {
        return true;
      }
      int spaceRemaining = width * height - startBit;
      if (spaceRemaining < remainingTiles) {
        return false;
      }
      ImmutableSet<Polyomino> remainingPresentsCopy =
          ImmutableSet.copyOf(remainingPresents.elementSet());
      int stop = (height - 2) * width;
      for (; startBit < stop; startBit = grid.nextClearBit(startBit + 1)) {
        int startCol = startBit % width;
        for (Polyomino basePolyomino : remainingPresentsCopy) {
          nextShape:
          for (Polyomino p : polyominoes.get(basePolyomino)) {
            int top = p.bits(0);
            // If top starts with 2 zero bits, we have to shift left by 2, etc. The leading bits in
            // the int vary from 29 (meaning top starts with a one bit) to 31 (meaning top starts
            // with two zero bits). For 29, we shift by 0, and for 31, we shift by 2.
            int shift = Integer.numberOfLeadingZeros(top) - 29;
            if (shift > startCol || startCol - shift + 3 > width) {
              // Shape would stick out past the left edge or the right edge, respectively.
              continue;
            }
            for (int i = 0; i < 3; i++) {
              int rowBits = p.bits(i);
              for (int j = 0; j < 3; j++) {
                if ((rowBits & (1 << j)) != 0) {
                  if (grid.get(startBit + i * width - shift + j)) {
                    continue nextShape;
                  }
                }
              }
            }
            for (int i = 0; i < 3; i++) {
              int rowBits = p.bits(i);
              for (int j = 0; j < 3; j++) {
                if ((rowBits & (1 << j)) != 0) {
                  grid.set(startBit + i * width - shift + j);
                }
              }
            }
            remainingPresents.remove(basePolyomino);
            if (solve(
                remainingPresents, remainingTiles - p.bitCount, grid.nextClearBit(startBit + 1))) {
              return true;
            }
            remainingPresents.add(basePolyomino);
            for (int i = 0; i < 3; i++) {
              int rowBits = p.bits(i);
              for (int j = 0; j < 3; j++) {
                if ((rowBits & (1 << j)) != 0) {
                  grid.clear(startBit + i * width - shift + j);
                }
              }
            }
          }
        }
      }
      return false;
    }

    private static final Pattern LINE_PATTERN = Pattern.compile("(\\d+)x(\\d+): (.*)");

    static Region parse(String line, ImmutableSetMultimap<Polyomino, Polyomino> polyominoes) {
      ImmutableList<Polyomino> polyominoList = ImmutableList.copyOf(polyominoes.keySet());
      var matcher = LINE_PATTERN.matcher(line);
      checkArgument(matcher.matches(), "%s does not match %s", line, LINE_PATTERN);
      int w = Integer.parseInt(matcher.group(1));
      int h = Integer.parseInt(matcher.group(2));
      String rest = matcher.group(3);
      List<Integer> quantities =
          Splitter.on(' ').splitToStream(rest).map(Integer::valueOf).toList();
      checkArgument(quantities.size() == polyominoList.size());
      ImmutableMultiset<Polyomino> presents =
          IntStream.range(0, polyominoList.size())
              .mapToObj(i -> i)
              .collect(toImmutableMultiset(i -> polyominoList.get(i), i -> quantities.get(i)));
      // We choose the width to be less than the height so the algorithm will search more
      // effectively.
      return new Region(min(w, h), max(w, h), polyominoes, presents);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < height; i++) {
        for (int j = 0; j < width; j++) {
          sb.append(grid.get(i * width + j) ? '■' : '□');
        }
        sb.append('\n');
      }
      return sb.toString();
    }
  }

  /**
   * A polyomino. Rotations and reflections of a given polyomino are considered distinct, unless of
   * course they look the same.
   */
  private static class Polyomino {
    // The bits of the polyomino when it is seen as being placed on a 3x3 grid.
    // Bit 0 indicates whether the polyomino fills the top left corner of the grid.
    // Bit 1 indicates whether it fills the square immediately to the right of that.
    // Bit 3 indicates whether it fills the square immediately below bit 0.
    // The grid is conceptually 3x3 but there are two restrictions:
    // - the polyomino must touch the top and the left of the grid;
    // - the polyomino must be contiguous.
    // All of the polyominoes in the sample and in the problem meet these restrictions.
    private final int bits;
    private final int bitCount;

    private Polyomino(int bits) {
      this.bits = bits;
      checkArgument(height(bits) == 3, "height(%s) == %s", bits, height(bits));
      checkArgument(width(bits) == 3);
      this.bitCount = Integer.bitCount(bits);
    }

    private static final Pattern LINE_PATTERN = Pattern.compile("[#.]{3}");

    static Polyomino parse(List<String> lines) {
      checkArgument(lines.size() == 3);
      checkArgument(lines.stream().allMatch(line -> LINE_PATTERN.matcher(line).matches()));
      int bits = 0;
      for (int i = 0; i < 3; i++) {
        String line = lines.get(i);
        for (int j = 0; j < 3; j++) {
          if (line.charAt(j) == '#') {
            bits |= 1 << (3 * i + j);
          }
        }
      }
      return new Polyomino(bits);
    }

    int bits() {
      return bits;
    }

    int bits(int row) {
      return (bits >>> (row * 3)) & 7;
    }

    private static int width(int bits) {
      int w = 0;
      for (int row = 0; row < 3; row++) {
        int rowMask = 7 << (3 * row);
        int rowBits = bits & rowMask;
        int shifted = rowBits >> (3 * row);
        w = max(w, 32 - Integer.numberOfLeadingZeros(shifted));
      }
      return w;
    }

    private static int height(int bits) {
      int highestBit = 31 - Integer.numberOfLeadingZeros(bits);
      return 1 + (highestBit / 3);
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof Polyomino that && this.bits == that.bits;
    }

    @Override
    public int hashCode() {
      return bits;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      for (int x = bits; x != 0; x >>>= 3) {
        if (sb.length() > 0) {
          sb.append('\n');
        }
        int rowBits = x & 7;
        for (int i = 0; i < 3; i++) {
          int b = 1 << i;
          char c = ((rowBits & b) == 0) ? '□' : '■';
          sb.append(c);
        }
      }
      return sb.toString();
    }

    /** Horizontal flip. */
    Polyomino flip() {
      int flipped = 0;
      for (int shift = 0; shift < 9; shift += 3) {
        int rowBits = (bits >> shift) & 7;
        int flippedRowBits = Integer.reverse(rowBits) >>> 29;
        // Reversing bit 0 sent to bit 31 but we want it at bit 2
        flipped |= flippedRowBits << shift;
      }
      return new Polyomino(flipped);
    }

    /** Rotate 90° clockwise. */
    Polyomino turn() {
      // r0 c0 goes to r0 c2
      // r0 c1 goes to r1 c2
      // r1 c0 goes to r0 c1
      // rR cC goes to rC c(2-R)
      int turned = 0;
      for (int r = 0; r < 3; r++) {
        for (int c = 0; c < 3; c++) {
          if ((bits & (1 << (r * 3 + c))) != 0) {
            int t = c * 3 + 2 - r;
            turned |= 1 << t;
          }
        }
      }
      return new Polyomino(turned);
    }

    ImmutableSet<Polyomino> rotations() {
      return ImmutableSet.of(this, this.turn(), this.turn().turn(), this.turn().turn().turn());
    }

    ImmutableSet<Polyomino> rotationsAndReflections() {
      return ImmutableSet.<Polyomino>builder()
          .addAll(rotations())
          .addAll(flip().rotations())
          .build();
    }
  }
}