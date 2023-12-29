package advent2023;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Éamonn McManus
 */
public class Puzzle19 {
  public static void main(String[] args) throws Exception {
    try (InputStream in = Puzzle19.class.getResourceAsStream("puzzle19-small.txt")) {
      String lineString = new String(in.readAllBytes(), UTF_8);
      List<String> lines = List.of(lineString.split("\n"));
      int empty = lines.indexOf("");
      assert empty > 0;
      Map<String, Workflow> workflows = parseWorkflows(lines.subList(0, empty));
      List<Part> parts = parseParts(lines.subList(empty + 1, lines.size()));
      int sum = 0;
      for (Part part : parts) {
        if (accept(part, workflows)) {
          sum += part.rating();
        }
      }
      System.out.println(STR."Rating total \{sum}");

      ConstraintSet accepted = allAcceptedBy(workflows);
      int newSum = 0;
      for (Part part : parts) {
        if (accepted.matches(part)) {
          newSum += part.rating();
        } else if (accept(part, workflows)) {
          throw new AssertionError(STR."Should have been accepted: \{part} given \{accepted}");
        }
      }
      System.out.println(STR."Rating total \{newSum}");
      System.out.println(STR."Accepting \{accepted}");
    }
  }

  private static boolean accept(Part part, Map<String, Workflow> workflows) {
    Workflow current = workflows.get("in");
    while (true) {
      String newLabel = current.apply(part);
      switch (newLabel) {
        case "A":
          return true;
        case "R":
          return false;
        default:
          current = workflows.get(newLabel);
      }
    }
  }

  static ConstraintSet allAcceptedBy(Map<String, Workflow> workflows) {
    return allAcceptedBy(workflows, ConstraintSet.MATCH_ALL, "in");
  }

  static ConstraintSet allAcceptedBy(
      Map<String, Workflow> workflows, ConstraintSet current, String startLabel) {
    switch (startLabel) {
      case "A" -> {
        return current;
      }
      case "R" -> {
        return ConstraintSet.EMPTY;
      }
    }
    ConstraintSet accepted = ConstraintSet.EMPTY;
    Workflow workflow = workflows.get(startLabel);
    for (Rule rule : workflow.rules) {
      // If the condition in the rule is true (intersection) then we'll pass the intersection of
      // `current` and that condition into a recursive call to find everything that matches that.
      // Otherwise, we'll update `current` with the complement of that condition.
      accepted = accepted.union(allAcceptedBy(workflows, current.intersection(rule.constraints), rule.target));
      current = current.minus(rule.constraints);
      // This isn't right. If the rule says a<1000, we want to subtract just a<1000, not the other
      // conditions.
    }
    return accepted.union(allAcceptedBy(workflows, current, workflow.defaultTarget));
  }

  private static final Pattern WORKFLOW_PATTERN = Pattern.compile("([a-z]+)\\{(.*)\\}");

  // px{a<2006:qkq,m>2090:A,rfg}
  static Map<String, Workflow> parseWorkflows(List<String> lines) {
    Map<String, Workflow> map = new TreeMap<>();
    for (String line : lines) {
      Matcher matcher = WORKFLOW_PATTERN.matcher(line);
      if (!matcher.matches()) {
        throw new AssertionError(line);
      }
      var old = map.put(matcher.group(1), parseWorkflow(matcher.group(2)));
      assert old == null;
    }
    return map;
  }

  static Workflow parseWorkflow(String line) {
    List<String> ruleStrings = List.of(line.split(","));
    List<Rule> rules = ruleStrings.stream().limit(ruleStrings.size() - 1).map(Puzzle19::parseRule).toList();
    return new Workflow(rules, ruleStrings.getLast());
  }

  static Rule parseRule(String ruleString) {
    char category = ruleString.charAt(0);
    char ltgt = ruleString.charAt(1);
    assert ltgt == '<' || ltgt == '>';
    int colon = ruleString.indexOf(':');
    assert colon > 0;
    int value = Integer.parseInt(ruleString.substring(2, colon));
    String target = ruleString.substring(colon + 1);
    return Rule.of(category, ltgt, value, target);
  }

  private static List<Part> parseParts(List<String> lines) {
    return lines.stream().map(Puzzle19::parsePart).toList();
  }

  private static final Pattern PART_PATTERN = Pattern.compile("\\{x=([0-9]+),m=([0-9]+),a=([0-9]+),s=([0-9]+)\\}");

  private static Part parsePart(String line) {
    Matcher matcher = PART_PATTERN.matcher(line);
    if (!matcher.matches()) {
      throw new AssertionError(line);
    }
    List<String> groups = List.of(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4));
    List<Integer> values = groups.stream().map(Integer::parseInt).toList();
    return Part.of(values.get(0), values.get(1), values.get(2), values.get(3));
  }

  record Workflow(List<Rule> rules, String defaultTarget) {
    String apply(Part part) {
      for (Rule rule : rules) {
        if (rule.matches(part)) {
          return rule.target();
        }
      }
      return defaultTarget;
    }
  }

  record Rule(Constraints constraints, String target) {
    boolean matches(Part part) {
      return constraints.matches(part);
    }

    static Rule of(char category, char ltgt, int value, String target) {
      int moreThan = 0, lessThan = 4001;
      switch (ltgt) {
        case '<' -> lessThan = value;
        case '>' -> moreThan = value;
      }
      Constraints constraints = Constraints.MATCH_ALL.with(category, new Constraint(moreThan, lessThan));
      return new Rule(constraints, target);
    }
  }

  static class Part extends LinkedHashMap<Character, Integer> {
    Part(Map<Character, Integer> map) {
      super(map);
    }

    static Part of(int x, int m, int a, int s) {
      return new Part(ImmutableMap.of('x', x, 'm', m, 'a', a, 's', s));
    }

    int rating() {
      return get('x') + get('m') + get('a') + get('s');
    }

    private static final long serialVersionUID = 0;
  }

  record Constraint(int moreThan, int lessThan) {
    /** A constraint that matches nothing. */
    static final Constraint EMPTY = new Constraint(0, 0);

    Constraint {
      boolean empty = moreThan == Integer.MAX_VALUE || lessThan == Integer.MIN_VALUE
          || moreThan + 1 >= lessThan;
      if (empty) {
        moreThan = lessThan = 0;
      }
    }

    boolean matches(int value) {
      return value > moreThan && value < lessThan;
    }

    /**
     * The number of values that match the constraint. For {@literal 1 < x < 10}, the number of
     * values is 8. (This value plainly doesn't need to be a {@code long} but is declared so to
     * avoid the risk of {@code int} overflow in expressions involving it.)
     */
    long size() {
      return Math.max(lessThan - moreThan - 1, 0);
    }

    boolean isEmpty() {
      return size() == 0;
    }

    /**
     * A new constraint that matches anything that both {@code this} and {@code that} match.
     */
    Constraint intersection(Constraint that) {
      Constraint result = new Constraint(
          Math.max(this.moreThan, that.moreThan),
          Math.min(this.lessThan, that.lessThan));
      return result.isEmpty() ? EMPTY : result;
    }

    /**
     * A set of constraints such that a value matches one constraint in the set if {@code this}
     * matches it but {@code that} doesn't. The set will have zero to two elements.
     */
    Set<Constraint> minus(Constraint that) {
      // If this says 2 < x < 10 and that says 4 < x < 8, then the before is 2 < x < 5 and the
      // after is 7 < x < 10. If this says 2 < x < 10 and that says 1 < x < 6, then the before is
      // 2 < x < 2 (which is empty) and the after is 5 < x < 10.
      Constraint before = new Constraint(this.moreThan, that.moreThan + 1);
      Constraint after = new Constraint(that.lessThan - 1, this.lessThan);
      return List.of(before, after).stream().filter(c -> !c.isEmpty()).collect(toImmutableSet());
    }
  }

  record Constraints(Map<Character, Constraint> map) {
    static final Constraints EMPTY = new Constraints(
        ImmutableMap.of('x', Constraint.EMPTY, 'm', Constraint.EMPTY, 'a', Constraint.EMPTY, 's', Constraint.EMPTY));

    static final Constraints MATCH_ALL = Constraints.of(0, 4001, 0, 4001, 0, 4001, 0, 4001);

    static Constraints of(
        int xMoreThan, int xLessThan, int mMoreThan, int mLessThan,
        int aMoreThan, int aLessThan, int sMoreThan, int sLessThan) {
      return new Constraints(
          ImmutableMap.of(
              'x', new Constraint(xMoreThan, xLessThan),
              'm', new Constraint(mMoreThan, mLessThan),
              'a', new Constraint(aMoreThan, aLessThan),
              's', new Constraint(sMoreThan, sLessThan)));
    }

    Constraints with(char c, Constraint constraint) {
      if (false) {
        return new Constraints(
            ImmutableMap.<Character, Constraint>builder()
                .putAll(map)
                .put(c, constraint)
                .buildKeepingLast());
      } else {
        LinkedHashMap<Character, Constraint> newMap = new LinkedHashMap<>(map);
        newMap.put(c, constraint);
        return new Constraints(newMap);
      }
    }

    boolean matches(Part part) {
      return part.keySet().stream().allMatch(c -> map.get(c).matches(part.get(c)));
    }

    /**
     * The number of combinations of variable values that match the constraints.
     */
    long size() {
      return map.values().stream().mapToLong(Constraint::size).reduce(1, (a, b) -> a * b);
    }

    boolean isEmpty() {
      return size() == 0;
    }

    /**
     * An instance that matches the values that both {@code this} and {@code that} match.
     */
    Constraints intersection(Constraints that) {
      Map<Character, Constraint> result =
          map.keySet().stream().map(c -> Map.entry(c, this.map.get(c).intersection(that.map.get(c))))
              .collect(toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
      boolean empty = result.values().stream().anyMatch(Constraint::isEmpty);
      return empty ? EMPTY : new Constraints(result);
    }

    /**
     * Returns a {@link ConstraintSet} that matches any value matched by {@code this} but not by
     * {@code that}. In general this will consist of up to 16 constraints, the product of
     * two constraints for each variable, matching values less than the intersection or more than
     * the intersection.
     */
    ConstraintSet minus(Constraints that) {
      Set<Constraint> xMinus = this.map.get('x').minus(that.map.get('x'));
      Set<Constraint> mMinus = this.map.get('m').minus(that.map.get('m'));
      Set<Constraint> aMinus = this.map.get('a').minus(that.map.get('a'));
      Set<Constraint> sMinus = this.map.get('s').minus(that.map.get('s'));
      ImmutableSet.Builder<Constraints> newConstraints = ImmutableSet.builder();
      for (Constraint x : xMinus) {
        for (Constraint m : mMinus) {
          for (Constraint a : aMinus) {
            for (Constraint s : sMinus) {
              Constraints constraints = new Constraints(ImmutableMap.of('x', x, 'm', m, 'a', a, 's', s));
              newConstraints.add(constraints);
            }
          }
        }
      }
      return new ConstraintSet(newConstraints.build());
    }

    @Override
    public String toString() {
      return map.entrySet().stream()
          .map(e -> e.getValue().moreThan + "<" + e.getKey() + "<" + e.getValue().lessThan)
          .collect(joining(",", "{", "}"));
    }
  }

  /**
   * A set of constraints, such that a {@link Part} matches the set if it matches any element of the
   * set. The sets must not overlap.
   */
  record ConstraintSet(Set<Constraints> constraintSet) {
    ConstraintSet {
      List<Constraints> a = new ArrayList<>(constraintSet);
      for (int i = 0; i < a.size(); i++) {
        for (int j = i + 1; j < a.size(); j++) {
          Constraints intersection = a.get(i).intersection(a.get(j));
          if (!intersection.isEmpty()) {
            throw new IllegalArgumentException(
                STR."Overlapping sets \{a.get(i)} and \{a.get(j)} => \{intersection}");
          }
        }
      }
    }

    static final ConstraintSet EMPTY = new ConstraintSet(Set.of());

    static final ConstraintSet MATCH_ALL = new ConstraintSet(Set.of(Constraints.MATCH_ALL));

    static ConstraintSet of(Constraints constraints) {
      return new ConstraintSet(Set.of(constraints));
    }

    boolean matches(Part part) {
      return constraintSet.stream().anyMatch(c -> c.matches(part));
    }

    /**
     * Return a new {@link ConstraintSet} that matches everything this one does, and also matches
     * anything matched by the given {@code constraints}.
     */
    ConstraintSet plus(Constraints constraints) {
      // We want to remove from `constraints` any ranges that are already present in `constraintSet`.
      // The result is in general a set of disjoint Contraints. For each Constraints in
      // `constraintSet`, we will remove its elements from `remaining`. The end result is a set
      // where no element has any values in common with `constraintSet`.
      Set<Constraints> remaining = Set.of(constraints);
      for (Constraints oldConstraints : constraintSet) {
        Set<Constraints> newRemaining = new LinkedHashSet<>();
        for (Constraints r : remaining) {
          newRemaining.addAll(r.minus(oldConstraints).constraintSet);
        }
        remaining = newRemaining;
      }
      return new ConstraintSet(
          ImmutableSet.<Constraints>builder().addAll(constraintSet).addAll(remaining).build());
    }

    ConstraintSet minus(Constraints constraints) {
      Set<Constraints> newConstraints = constraintSet.stream()
          .flatMap(c -> c.minus(constraints).constraintSet.stream())
          .collect(toImmutableSet());
      return new ConstraintSet(newConstraints);
    }

    ConstraintSet intersection(Constraints constraints) {
      ImmutableSet<Constraints> newConstraints = constraintSet.stream()
          .map(c -> c.intersection(constraints))
          .filter(c -> !c.isEmpty())
          .collect(toImmutableSet());
      return new ConstraintSet(newConstraints);
    }

    ConstraintSet union(ConstraintSet that) {
      ConstraintSet u = this;
      for (Constraints constraints : that.constraintSet) {
        u = u.plus(constraints);
      }
      return u;
    }

    @Override
    public String toString() {
      return constraintSet.stream().map(Object::toString).collect(joining(" or ", "{", "}"));
    }
  }
}
