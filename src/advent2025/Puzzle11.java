package advent2025;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.google.common.graph.ImmutableGraph;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Éamonn McManus
 */
public class Puzzle11 {
  private static final String SAMPLE1 =
      """
      aaa: you hhh
      you: bbb ccc
      bbb: ddd eee
      ccc: ddd eee fff
      ddd: ggg
      eee: out
      fff: out
      ggg: out
      hhh: ccc fff iii
      iii: out
      """;

  private static final String SAMPLE2 =
      """
      svr: aaa bbb
      aaa: fft
      fft: ccc
      bbb: tty
      tty: ccc
      ccc: ddd eee
      ddd: hub
      hub: fff
      eee: dac
      dac: fff
      fff: ggg hhh
      ggg: out
      hhh: out
      """;

  private static Reader problemReader() {
    return new InputStreamReader(Puzzle11.class.getResourceAsStream("puzzle11.txt"));
  }

  private static final Map<String, Callable<Reader>> PART1_INPUT_PRODUCERS =
      ImmutableMap.of(
          "sample", () -> new StringReader(SAMPLE1), "problem", Puzzle11::problemReader);

  private static final Map<String, Callable<Reader>> PART2_INPUT_PRODUCERS =
      ImmutableMap.of(
          "sample", () -> new StringReader(SAMPLE2), "problem", Puzzle11::problemReader);

  public static void main(String[] args) throws Exception {
    for (var entry : PART1_INPUT_PRODUCERS.entrySet()) {
      String name = entry.getKey();
      try (Reader r = entry.getValue().call()) {
        List<String> lines = r.readAllLines();
        ImmutableGraph<String> graph = parseGraph(lines);
        System.out.printf("For %s, part 1 solution is %d\n", name, countPaths(graph, "you", "out"));
      }
    }
    for (var entry : PART2_INPUT_PRODUCERS.entrySet()) {
      String name = entry.getKey();
      try (Reader r = entry.getValue().call()) {
        List<String> lines = r.readAllLines();
        ImmutableGraph<String> graph = parseGraph(lines);
        String start, end;
        if (connected(graph, "dac", "fft")) {
          start = "dac";
          end = "fft";
        } else if (connected(graph, "fft", "dac")) {
          start = "fft";
          end = "dac";
        } else {
          throw new IllegalStateException("dac and fft are not connected");
        }
        long paths =
            Math.multiplyExact(
                countPaths(graph, "svr", start),
                Math.multiplyExact(countPaths(graph, start, end), countPaths(graph, end, "out")));
        System.out.printf("For %s, part 2 solution is %d\n", name, paths);
      }
    }
  }

  private static long countPaths(ImmutableGraph<String> graph, String start, String end) {
    return countPaths(graph, start, end, new HashMap<>());
  }

  private static long countPaths(
      ImmutableGraph<String> graph, String start, String end, Map<String, Long> pathsToEnd) {
    if (start.equals(end)) {
      return 1;
    }
    Long cached = pathsToEnd.get(start);
    if (cached != null) {
      return cached;
    }
    long paths = 0;
    for (String successor : graph.successors(start)) {
      paths += countPaths(graph, successor, end, pathsToEnd);
    }
    pathsToEnd.put(start, paths);
    return paths;
  }

  private static boolean connected(ImmutableGraph<String> graph, String from, String to) {
    if (from.equals(to)) {
      return true;
    }
    for (String successor : graph.successors(from)) {
      if (connected(graph, successor, to)) {
        return true;
      }
    }
    return false;
  }

  private static ImmutableGraph<String> parseGraph(List<String> lines) {
    ImmutableGraph.Builder<String> builder = GraphBuilder.directed().immutable();
    Pattern pattern = Pattern.compile("([a-z][a-z][a-z]):((?: [a-z][a-z][a-z])+)");
    for (String line : lines) {
      Matcher matcher = pattern.matcher(line);
      checkState(matcher.matches(), "line %s", line);
      String source = matcher.group(1);
      for (String dest : Splitter.on(' ').omitEmptyStrings().splitToList(matcher.group(2))) {
        builder.putEdge(source, dest);
      }
    }
    return builder.build();
  }
}