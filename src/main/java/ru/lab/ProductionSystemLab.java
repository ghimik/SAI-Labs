package ru.lab;

import java.util.*;


class Fact {
    String name;
    Object value;

    Fact(String name, Object value) {
        this.name = name;
        this.value = value;
    }
}
class Condition {
    String factName;
    String operator; // "=", "!=", ">", "<", ">=", "<=", "in", "contains", "startsWith", "endsWith", "matches"
    Object expectedValue;

    Condition(String factName, String operator, Object expectedValue) {
        this.factName = factName;
        this.operator = operator;
        this.expectedValue = expectedValue;
    }

    boolean evaluate(Map<String, Object> workingMemory) {
        Object actualValue = workingMemory.get(factName);
        if (actualValue == null) return false;

        return switch (operator) {
            case "=" -> actualValue.equals(expectedValue);
            case "!=" -> !actualValue.equals(expectedValue);
            case ">" -> compareNumbers(actualValue, expectedValue) > 0;
            case "<" -> compareNumbers(actualValue, expectedValue) < 0;
            case ">=" -> compareNumbers(actualValue, expectedValue) >= 0;
            case "<=" -> compareNumbers(actualValue, expectedValue) <= 0;
            case "in" -> checkInCollection(actualValue, expectedValue);
            case "contains" -> checkContains(actualValue, expectedValue);
            case "startsWith" -> checkStartsWith(actualValue, expectedValue);
            case "endsWith" -> checkEndsWith(actualValue, expectedValue);
            case "matches" -> checkMatches(actualValue, expectedValue);
            case "exists" -> true;
            case "not_exists" -> false;
            default -> false;
        };
    }

    private int compareNumbers(Object a, Object b) {
        if (a instanceof Number && b instanceof Number) {
            double aVal = ((Number) a).doubleValue();
            double bVal = ((Number) b).doubleValue();
            return Double.compare(aVal, bVal);
        }
        return 0;
    }

    private boolean checkInCollection(Object actual, Object expected) {
        if (expected instanceof Collection) {
            return ((Collection<?>) expected).contains(actual);
        }
        if (expected instanceof Object[]) {
            return Arrays.asList((Object[]) expected).contains(actual);
        }
        return false;
    }

    private boolean checkContains(Object actual, Object expected) {
        if (actual instanceof String && expected instanceof String) {
            return ((String) actual).contains((String) expected);
        }
        if (actual instanceof Collection) {
            return ((Collection<?>) actual).contains(expected);
        }
        return false;
    }

    private boolean checkStartsWith(Object actual, Object expected) {
        if (actual instanceof String && expected instanceof String) {
            return ((String) actual).startsWith((String) expected);
        }
        return false;
    }

    private boolean checkEndsWith(Object actual, Object expected) {
        if (actual instanceof String && expected instanceof String) {
            return ((String) actual).endsWith((String) expected);
        }
        return false;
    }

    private boolean checkMatches(Object actual, Object expected) {
        if (actual instanceof String && expected instanceof String) {
            return ((String) actual).matches((String) expected);
        }
        return false;
    }

    @Override
    public String toString() {
        return factName + " " + operator + " " + expectedValue;
    }
}

class Action {
    String factName;
    Object value;

    Action(String factName, Object value) {
        this.factName = factName;
        this.value = value;
    }

    void execute(Map<String, Object> workingMemory) {
        workingMemory.put(factName, value);
        System.out.println(" Установлен факт: " + factName + " = " + value);
    }
}

class Rule {
    String name;
    List<Condition> conditions;
    List<Action> actions;
    int priority;

    Rule(String name, List<Condition> conditions, List<Action> actions, int priority) {
        this.name = name;
        this.conditions = conditions;
        this.actions = actions;
        this.priority = priority;
    }

    boolean isApplicable(Map<String, Object> workingMemory) {
        for (Condition cond : conditions) {
            if (!cond.evaluate(workingMemory)) {
                return false;
            }
        }
        return true;
    }

    void execute(Map<String, Object> workingMemory) {
        System.out.println("Применено правило: " + name);
        for (Action action : actions) {
            action.execute(workingMemory);
        }
    }
}

enum ConflictResolutionStrategy {
    FIRST_MATCH,      // первое подходящее
    HIGHEST_PRIORITY, // наивысший приоритет
    MOST_SPECIFIC,    // наиболее специфичное (больше условий)
    RANDOM           // случайный выбор
}

class ProductionSystem {
    private final List<Rule> rules = new ArrayList<>();
    private final Map<String, Object> workingMemory = new HashMap<>();
    private ConflictResolutionStrategy strategy = ConflictResolutionStrategy.FIRST_MATCH;

    public void addRule(Rule rule) {
        rules.add(rule);
    }

    public void setFact(String name, Object value) {
        workingMemory.put(name, value);
        System.out.println("Факт установлен: " + name + " = " + value);
    }

    public boolean forwardChaining(Set<String> goals) {
        System.out.println("\nПРЯМОЙ ВЫВОД");
        int iteration = 0;

        List<Rule> availableRules = new ArrayList<>(rules);

        while (iteration < 100) {
            iteration++;
            System.out.println("\nИтерация " + iteration);

            List<Rule> applicableRules = new ArrayList<>();
            for (Rule rule : availableRules) {
                if (rule.isApplicable(workingMemory)) {
                    applicableRules.add(rule);
                }
            }

            System.out.println("Применимых правил: " + applicableRules.size());

            if (applicableRules.isEmpty()) {
                System.out.println("Нет применимых правил - остановка");
                break;
            }

            while (!applicableRules.isEmpty()) {
                Rule selectedRule = resolveConflict(applicableRules);

                selectedRule.execute(workingMemory);

                availableRules.remove(selectedRule);
                applicableRules.remove(selectedRule);

                if (goalsAchieved(goals)) {
                    System.out.println("Цели достигнуты на итерации " + iteration);
                    return true;
                }
            }
        }

        System.out.println("Не удалось достичь целей за " + iteration + " итераций");
        return false;
    }

    public boolean backwardChaining(String goal) {
        System.out.println("\nОБРАТНЫЙ ВЫВОД для цели: " + goal);
        return backwardChainingRecursive(goal, new HashSet<>());
    }

    private boolean backwardChainingRecursive(String goal, Set<String> visited) {
        if (workingMemory.containsKey(goal)) {
            Object value = workingMemory.get(goal);
            if (value != null) {
                if (value instanceof Boolean) {
                    if ((Boolean) value) {
                        System.out.println("Цель '" + goal + "' уже достигнута (true)");
                        return true;
                    } else {
                        System.out.println("Цель '" + goal + "' установлена в false");
                        return false;
                    }
                }
                System.out.println("Факт '" + goal + "' уже установлен: " + value);
                return true;
            }
            System.out.println("Факт '" + goal + "' установлен в null");
            return false;
        }


        if (visited.contains(goal)) {
            System.out.println("Обнаружен цикл при проверке цели: " + goal);
            return false;
        }
        visited.add(goal);

        List<Rule> rulesProducingGoal = new ArrayList<>();
        for (Rule rule : rules) {
            for (Action action : rule.actions) {
                if (action.factName.equals(goal)) {
                    rulesProducingGoal.add(rule);
                    break;
                }
            }
        }

        if (rulesProducingGoal.isEmpty()) {
            System.out.println("Нет правил для вывода цели: " + goal);
            return false;
        }

        for (Rule rule : rulesProducingGoal) {
            System.out.println("Пробуем правило: " + rule.name + " для цели: " + goal);

            boolean allConditionsMet = true;
            for (Condition cond : rule.conditions) {
                if (!backwardChainingRecursive(cond.factName, visited)) {
                    allConditionsMet = false;
                    break;
                }
            }

            if (allConditionsMet) {
                System.out.println("Все условия правила '" + rule.name + "' выполнены");
                rule.execute(workingMemory);
                return true;
            }
        }

        System.out.println("Не удалось достичь цели: " + goal);
        return false;
    }

    private Rule resolveConflict(List<Rule> applicableRules) {
        System.out.println("Конфликтное множество: " + applicableRules.size() + " правил");

        return switch (strategy) {
            case FIRST_MATCH -> applicableRules.get(0);
            case HIGHEST_PRIORITY -> Collections.max(applicableRules,
                    Comparator.comparingInt(r -> r.priority));
            case MOST_SPECIFIC -> Collections.max(applicableRules,
                    Comparator.comparingInt(r -> r.conditions.size()));
            case RANDOM -> applicableRules.get(new Random().nextInt(applicableRules.size()));
            default -> applicableRules.get(0);
        };
    }

    private boolean goalsAchieved(Set<String> goals) {
        for (String goal : goals) {
            if (!workingMemory.containsKey(goal)) {
                return false;
            }

            Object value = workingMemory.get(goal);
            if (value == null) {
                return false;
            }

            if (value instanceof Boolean && !(Boolean) value) {
                return false;
            }
        }
        return true;
    }

    public void setStrategy(ConflictResolutionStrategy strategy) {
        this.strategy = strategy;
    }

    public void printWorkingMemory() {
        System.out.println("\n=== РАБОЧАЯ ПАМЯТЬ ===");
        for (Map.Entry<String, Object> entry : workingMemory.entrySet()) {
            System.out.println(entry.getKey() + " = " + entry.getValue());
        }
    }
}

public class ProductionSystemLab {
    public static void main(String[] args) {
        ProductionSystem cpuAdvisor = new ProductionSystem();
        cpuAdvisor.setStrategy(ConflictResolutionStrategy.HIGHEST_PRIORITY);

        loadCPURules(cpuAdvisor);

        System.out.println("КОНСУЛЬТАНТ ПО ВЫБОРУ ПРОЦЕССОРА\n");

        cpuAdvisor.setFact("тип_задачи", "работа");
        cpuAdvisor.setFact("бюджет_руб", 165000);
        cpuAdvisor.setFact("требуемая_производительность", "высокая");
        cpuAdvisor.setFact("интегрированная_графика_нужна", false);
        cpuAdvisor.setFact("энергоэффективность_важна", false);
        cpuAdvisor.setFact("потребность_многопоточность", true);

        Set<String> goals = new HashSet<>();
        goals.add("рекомендация_cpu");
        goals.add("бюджет_подходящий");

        cpuAdvisor.forwardChaining(goals);

        System.out.println("\nИТОГОВАЯ РЕКОМЕНДАЦИЯ");
        cpuAdvisor.printWorkingMemory();

        System.out.println("\nПРОВЕРКА ОБРАТНЫМ ВЫВОДОМ");
        cpuAdvisor.setFact("рекомендация_cpu", null);

        cpuAdvisor.backwardChaining("рекомендация_cpu");
    }

    private static void loadCPURules(ProductionSystem system) {

        system.addRule(new Rule("Бюджет для начального уровня",
                Arrays.asList(
                        new Condition("требуемая_категория", "=", "игровой_начальный"),
                        new Condition("бюджет_руб", ">=", 15000),
                        new Condition("бюджет_руб", "<=", 30000)
                ),
                List.of(
                        new Action("бюджет_достаточный", true)
                ),
                2));

        system.addRule(new Rule("Бюджет для среднего уровня",
                Arrays.asList(
                        new Condition("требуемая_категория", "=", "игровой_средний"),
                        new Condition("бюджет_руб", ">=", 25000),
                        new Condition("бюджет_руб", "<=", 60000)
                ),
                List.of(
                        new Action("бюджет_достаточный", true)
                ),
                2));

        system.addRule(new Rule("Бюджет для топового уровня",
                Arrays.asList(
                        new Condition("требуемая_категория", "=", "игровой_топовый"),
                        new Condition("бюджет_руб", ">=", 50000)
                ),
                List.of(
                        new Action("бюджет_достаточный", true)
                ),
                2));

        system.addRule(new Rule("Определить игровую задачу",
                Arrays.asList(
                        new Condition("тип_задачи", "=", "игры"),
                        new Condition("требуемая_производительность", "=", "минимальная")
                ),
                Arrays.asList(
                        new Action("требуемая_категория", "игровой_начальный"),
                        new Action("задача_определена", true)
                ),
                2));

        system.addRule(new Rule("Игры среднего уровня",
                Arrays.asList(
                        new Condition("тип_задачи", "=", "игры"),
                        new Condition("требуемая_производительность", "=", "средняя"),
                        new Condition("бюджет_руб", ">=", 25000)
                ),
                Arrays.asList(
                        new Action("требуемая_категория", "игровой_средний"),
                        new Action("задача_определена", true)
                ),
                2));

        system.addRule(new Rule("Профессиональные игры",
                Arrays.asList(
                        new Condition("тип_задачи", "=", "игры"),
                        new Condition("требуемая_производительность", "=", "высокая"),
                        new Condition("бюджет_руб", ">=", 50000)
                ),
                Arrays.asList(
                        new Action("требуемая_категория", "игровой_топовый"),
                        new Action("задача_определена", true)
                ),
                2));

        system.addRule(new Rule("Офисные задачи",
                Arrays.asList(
                        new Condition("тип_задачи", "=", "работа"),
                        new Condition("требуемая_производительность", "=", "минимальная")
                ),
                Arrays.asList(
                        new Action("требуемая_категория", "офисный"),
                        new Action("задача_определена", true)
                ),
                2));

        system.addRule(new Rule("Программирование/дизайн",
                Arrays.asList(
                        new Condition("тип_задачи", "=", "работа"),
                        new Condition("требуемая_производительность", "=", "средняя"),
                        new Condition("потребность_многопоточность", "=", true)
                ),
                Arrays.asList(
                        new Action("требуемая_категория", "рабочая_станция"),
                        new Action("задача_определена", true)
                ),
                3));

        system.addRule(new Rule("Рендеринг/моделирование",
                Arrays.asList(
                        new Condition("тип_задачи", "=", "работа"),
                        new Condition("требуемая_производительность", "=", "высокая"),
                        new Condition("потребность_многопоточность", "=", true)
                ),
                Arrays.asList(
                        new Action("требуемая_категория", "профессиональный"),
                        new Action("задача_определена", true)
                ),
                3));


        system.addRule(new Rule("Бюджет слишком мал",
                Arrays.asList(
                        new Condition("требуемая_категория", "in",
                                Arrays.asList("игровой_топовый", "профессиональный")),
                        new Condition("бюджет_руб", "<", 40000)
                ),
                Arrays.asList(
                        new Action("бюджет_достаточный", false)
                ),
                1));

        system.addRule(new Rule("Бюджет подходит",
                Arrays.asList(
                        new Condition("требуемая_категория", "=", "игровой_начальный"),
                        new Condition("бюджет_руб", ">=", 15000)
                ),
                Arrays.asList(
                        new Action("бюджет_достаточный", true)
                ),
                1));


        system.addRule(new Rule("Рекомендовать AMD Ryzen 5 для игр",
                Arrays.asList(
                        new Condition("требуемая_категория", "=", "игровой_начальный"),
                        new Condition("бюджет_достаточный", "=", true),
                        new Condition("энергоэффективность_важна", "=", true)
                ),
                Arrays.asList(
                        new Action("рекомендация_cpu", "AMD Ryzen 5 7600"),
                        new Action("бюджет_подходящий", true)
                ),
                3));

        system.addRule(new Rule("Рекомендовать Intel i3 для начального уровня",
                Arrays.asList(
                        new Condition("требуемая_категория", "=", "игровой_начальный"),
                        new Condition("бюджет_достаточный", "=", true),
                        new Condition("интегрированная_графика_нужна", "=", true)
                ),
                Arrays.asList(
                        new Action("рекомендация_cpu", "Intel Core i3-12100"),
                        new Action("бюджет_подходящий", true)
                ),
                3));

        system.addRule(new Rule("Рекомендовать i5 для средних игр",
                Arrays.asList(
                        new Condition("требуемая_категория", "=", "игровой_средний"),
                        new Condition("бюджет_достаточный", "=", true),
                        new Condition("совместимость_сокет", "=", "LGA1700")
                ),
                Arrays.asList(
                        new Action("рекомендация_cpu", "Intel Core i5-13400F"),
                        new Action("бюджет_подходящий", true)
                ),
                4));

        system.addRule(new Rule("Рекомендовать Ryzen 5 для AM5",
                Arrays.asList(
                        new Condition("требуемая_категория", "=", "игровой_средний"),
                        new Condition("бюджет_достаточный", "=", true),
                        new Condition("совместимость_сокет", "=", "AM5")
                ),
                Arrays.asList(
                        new Action("рекомендация_cpu", "AMD Ryzen 5 7600X"),
                        new Action("бюджет_подходящий", true)
                ),
                4));

        system.addRule(new Rule("Рекомендовать i7 для требовательных игр",
                Arrays.asList(
                        new Condition("требуемая_категория", "=", "игровой_топовый"),
                        new Condition("бюджет_достаточный", "=", true)
                ),
                Arrays.asList(
                        new Action("рекомендация_cpu", "Intel Core i7-14700K"),
                        new Action("бюджет_подходящий", true)
                ),
                5));

        system.addRule(new Rule("Рекомендовать Pentium Gold для офиса",
                Arrays.asList(
                        new Condition("требуемая_категория", "=", "офисный"),
                        new Condition("интегрированная_графика_нужна", "=", true)
                ),
                Arrays.asList(
                        new Action("рекомендация_cpu", "Intel Pentium Gold G7400"),
                        new Action("бюджет_подходящий", true)
                ),
                2));

        system.addRule(new Rule("Рекомендовать Ryzen 7 для работы",
                Arrays.asList(
                        new Condition("требуемая_категория", "=", "рабочая_станция"),
                        new Condition("потребность_многопоточность", "=", true)
                ),
                Arrays.asList(
                        new Action("рекомендация_cpu", "AMD Ryzen 7 9700X"),
                        new Action("бюджет_подходящий", true)
                ),
                4));

        system.addRule(new Rule("Рекомендовать Threadripper",
                Arrays.asList(
                        new Condition("требуемая_категория", "=", "профессиональный"),
                        new Condition("бюджет_руб", ">=", 100000)
                ),
                Arrays.asList(
                        new Action("рекомендация_cpu", "AMD Ryzen Threadripper 9960X"),
                        new Action("бюджет_подходящий", true)
                ),
                5));

        system.addRule(new Rule("Рекомендовать Apple M4",
                Arrays.asList(
                        new Condition("тип_задачи", "=", "работа"),
                        new Condition("энергоэффективность_важна", "=", true),
                        new Condition("требуемая_производительность", "=", "высокая")
                ),
                Arrays.asList(
                        new Action("рекомендация_cpu", "Apple M4 Pro"),
                        new Action("бюджет_подходящий", true)
                ),
                3));
    }
}