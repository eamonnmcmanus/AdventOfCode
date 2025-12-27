package advent2025;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableMap;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author Éamonn McManus
 */
public class Puzzle6 {
  private static final String SAMPLE =
      """
      123 328  51 64\s
       45 64  387 23\s
        6 98  215 314
      *   +   *   + \s
      """;

  private static final Map<String, Callable<Reader>> INPUT_PRODUCERS =
      ImmutableMap.of(
          "sample", () -> new StringReader(SAMPLE),
          "problem", () -> new InputStreamReader(Puzzle6.class.getResourceAsStream("puzzle6.txt")));

  public static void main(String[] args) throws Exception {
    for (var entry : INPUT_PRODUCERS.entrySet()) {
      String name = entry.getKey();
      try (Reader r = entry.getValue().call()) {
        List<String> lines = r.readAllLines();
        String operations = lines.getLast().replace(" ", "");
        lines = lines.subList(0, lines.size() - 1);
        String firstLine = lines.getFirst();
        int len = firstLine.length();
        checkState(lines.stream().allMatch(line -> line.length() == len));
        List<Integer> gaps = new ArrayList<>();
        for (int i = 0; i < len; i++) {
          int ii = i;
          if (firstLine.charAt(i) == ' '
              && lines.stream().allMatch(line -> line.charAt(ii) == ' ')) {
            gaps.add(i);
          }
        }
        gaps.add(firstLine.length());
        System.out.printf("For %s, part 1 sum is %d\n", name, part1Sum(lines, gaps, operations));
        System.out.printf("For %s, part 2 sum is %d\n", name, part2Sum(lines, gaps, operations));
      }
    }
  }

  private static long part1Sum(List<String> lines, List<Integer> gaps, String operations) {
    List<List<Long>> numbers = new ArrayList<>();
    for (String line : lines) {
      List<Long> lineNumbers = new ArrayList<>();
      int prev = 0;
      for (int gap : gaps) {
        lineNumbers.add(Long.valueOf(line.substring(prev, gap).trim()));
        prev = gap + 1;
      }
      numbers.add(lineNumbers);
    }
    checkState(numbers.stream().allMatch(list -> list.size() == operations.length()));
    long total = 0;
    for (int i = 0; i < operations.length(); i++) {
      int ii = i;
      char operation = operations.charAt(i);
      List<Long> operands = numbers.stream().map(list -> list.get(ii)).toList();
      long result =
          switch (operation) {
            case '+' -> operands.stream().reduce(0L, Math::addExact);
            case '*' -> operands.stream().reduce(1L, Math::multiplyExact);
            default -> throw new IllegalStateException(String.valueOf(operation));
          };
      total = Math.addExact(total, result);
    }
    return total;
  }

  private static long part2Sum(List<String> lines, List<Integer> gaps, String operations) {
    List<List<String>> numbers = new ArrayList<>();
    for (String line : lines) {
      List<String> lineNumbers = new ArrayList<>();
      int prev = 0;
      for (int gap : gaps) {
        lineNumbers.add(line.substring(prev, gap));
        prev = gap + 1;
      }
      numbers.add(lineNumbers);
    }
    checkState(numbers.stream().allMatch(list -> list.size() == operations.length()));
    long total = 0;
    for (int i = 0; i < operations.length(); i++) {
      int ii = i;
      char operation = operations.charAt(i);
      List<String> rows = numbers.stream().map(list -> list.get(ii)).toList();
      int len = rows.getFirst().length();
      checkState(rows.stream().allMatch(row -> row.length() == len));
      List<Long> operands = new ArrayList<>();
      for (int j = 0; j < len; j++) {
        StringBuilder sb = new StringBuilder();
        for (String row : rows) {
          sb.append(row.charAt(j));
        }
        operands.add(Long.valueOf(sb.toString().trim()));
      }
      long result =
          switch (operation) {
            case '+' -> operands.stream().reduce(0L, Math::addExact);
            case '*' -> operands.stream().reduce(1L, Math::multiplyExact);
            default -> throw new IllegalStateException(String.valueOf(operation));
          };
      total = Math.addExact(total, result);
    }
    return total;
  }
}