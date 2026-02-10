package ru.lab;

import java.util.*;

public class ResolutionMethod {

    static class Clause {
        Set<String> positive = new HashSet<>();
        Set<String> negative = new HashSet<>();

        Clause() {}

        Clause(String[] pos, String[] neg) {
            positive.addAll(Arrays.asList(pos));
            negative.addAll(Arrays.asList(neg));
        }

        boolean isEmpty() {
            return positive.isEmpty() && negative.isEmpty();
        }

        boolean isTautology() {
            for (String p : positive) {
                if (negative.contains(p)) return true;
            }
            return false;
        }

        Clause resolveWith(Clause other) {
            for (String literal : this.positive) {
                if (other.negative.contains(literal)) {
                    return merge(this, other, literal, true);
                }
            }

            for (String literal : this.negative) {
                if (other.positive.contains(literal)) {
                    return merge(this, other, literal, false);
                }
            }

            return null;
        }

        private Clause merge(Clause c1, Clause c2, String literal, boolean fromPositive) {
            Clause result = new Clause();

            for (String p : c1.positive) {
                if (!p.equals(literal) || !fromPositive) {
                    result.positive.add(p);
                }
            }
            for (String n : c1.negative) {
                if (!n.equals(literal) || fromPositive) {
                    result.negative.add(n);
                }
            }

            for (String p : c2.positive) {
                if (!p.equals(literal) || fromPositive) {
                    result.positive.add(p);
                }
            }
            for (String n : c2.negative) {
                if (!n.equals(literal) || !fromPositive) {
                    result.negative.add(n);
                }
            }

            return result;
        }

        @Override
        public String toString() {
            if (isEmpty()) return "[]";

            List<String> literals = new ArrayList<>(positive);
            for (String n : negative) literals.add("¬" + n);

            return literals.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Clause other)) return false;
            return positive.equals(other.positive) && negative.equals(other.negative);
        }
    }


    static class Formula {
        String type; // "var", "not", "and", "or", "impl", "equiv"
        String var;
        Formula left, right;

        Formula(String type, Formula left, Formula right) {
            this.type = type;
            this.left = left;
            this.right = right;
        }

        Formula(String var) {
            this.type = "var";
            this.var = var;
        }

        Formula removeImplications() {
            if (type.equals("var")) return this;

            if (type.equals("impl")) {
                return new Formula("or",
                        new Formula("not", left.removeImplications(), null),
                        right.removeImplications());
            }

            if (type.equals("equiv")) {
                return new Formula("and",
                        new Formula("impl", left, right).removeImplications(),
                        new Formula("impl", right, left).removeImplications());
            }

            Formula newLeft = (left != null) ? left.removeImplications() : null;
            Formula newRight = (right != null) ? right.removeImplications() : null;
            return new Formula(type, newLeft, newRight);
        }

        Formula pushNegations() {
            if (type.equals("var")) return this;

            if (type.equals("not")) {
                Formula inner = left;

                if (inner.type.equals("not")) {
                    return inner.left.pushNegations();
                }

                if (inner.type.equals("and")) {
                    return new Formula("or",
                            new Formula("not", inner.left, null).pushNegations(),
                            new Formula("not", inner.right, null).pushNegations());
                }

                if (inner.type.equals("or")) {
                    return new Formula("and",
                            new Formula("not", inner.left, null).pushNegations(),
                            new Formula("not", inner.right, null).pushNegations());
                }
            }

            Formula newLeft = (left != null) ? left.pushNegations() : null;
            Formula newRight = (right != null) ? right.pushNegations() : null;
            return new Formula(type, newLeft, newRight);
        }

        Formula applyDistributivity() {
            if (type.equals("var")) return this;

            Formula newLeft = (left != null) ? left.applyDistributivity() : null;
            Formula newRight = (right != null) ? right.applyDistributivity() : null;
            Formula result = new Formula(type, newLeft, newRight);

            if (type.equals("or")) {
                boolean changed;
                do {
                    changed = false;

                    if (result.right.type.equals("and")) {
                        result = new Formula("and",
                                new Formula("or", result.left, result.right.left),
                                new Formula("or", result.left, result.right.right));
                        changed = true;
                    } else if (result.left.type.equals("and")) {
                        result = new Formula("and",
                                new Formula("or", result.left.left, result.right),
                                new Formula("or", result.left.right, result.right));
                        changed = true;
                    }

                    if (changed) {
                        result = result.applyDistributivity();
                    }
                } while (changed);
            }

            return result;
        }

        Formula toCNF() {
            return this.removeImplications()
                    .pushNegations()
                    .applyDistributivity();
        }

        List<Clause> toClauses() {
            List<Clause> clauses = new ArrayList<>();
            collectClauses(this, clauses);
            return clauses;
        }

        private void collectClauses(Formula f, List<Clause> clauses) {
            if (f.type.equals("and")) {
                collectClauses(f.left, clauses);
                collectClauses(f.right, clauses);
            } else {
                Clause clause = new Clause();
                extractLiterals(f, clause);
                if (!clause.isTautology()) {
                    clauses.add(clause);
                }
            }
        }

        private void extractLiterals(Formula f, Clause clause) {
            if (f.type.equals("var")) {
                clause.positive.add(f.var);
            } else if (f.type.equals("not")) {
                if (f.left.type.equals("var")) {
                    clause.negative.add(f.left.var);
                }
            } else if (f.type.equals("or")) {
                extractLiterals(f.left, clause);
                extractLiterals(f.right, clause);
            }
        }

        @Override
        public String toString() {
            if (type.equals("var")) return var;
            if (type.equals("not")) return "¬" + left;

            String op = switch (type) {
                case "and" -> " ∧ ";
                case "or" -> " ∨ ";
                case "impl" -> " → ";
                default -> "";
            };

            return "(" + left + op + right + ")";
        }
    }

    public static boolean resolution(List<Clause> clauses) {
        Set<Clause> allClauses = new HashSet<>(clauses);
        List<Clause> clauseList = new ArrayList<>(clauses);

        for (int i = 0; i < clauseList.size(); i++) {
            for (int j = 0; j < clauseList.size(); j++) {
                if (i == j) continue;

                Clause resolvent = clauseList.get(i).resolveWith(clauseList.get(j));

                if (resolvent != null && !resolvent.isTautology()) {
                    if (resolvent.isEmpty()) {
                        return true;
                    }

                    if (!allClauses.contains(resolvent)) {
                        allClauses.add(resolvent);
                        clauseList.add(resolvent);
                    }
                }
            }
        }

        return false;
    }

    public static void main(String[] args) {
        System.out.println("Утверждение:");
        System.out.println("1. Ни один человек не является четвероногим: P -> ¬Q = ¬P ∨ ¬Q");
        System.out.println("2. Все дети это люди: C → P = ¬C v P");
        System.out.println("Доказать: Дети не являются четвероногими: C → ¬Q = ¬C ∨ ¬Q ");

        Formula premise1 = new Formula("or",
                new Formula("not", new Formula("P"), null),
                new Formula("not", new Formula("Q"), null));

        Formula premise2 = new Formula("or",
                new Formula("not", new Formula("C"), null),
                new Formula("P"));

        Formula knowledgeBase = new Formula("and", premise1, premise2);

        Formula conclusion = new Formula("or",
                new Formula("not", new Formula("C"), null),
                new Formula("not", new Formula("Q"), null));

        proveTheorem(knowledgeBase, conclusion);

        System.out.println("Должно быть: СЛЕДУЕТ");
    }

    private static void proveTheorem(Formula kb1, Formula theorem1) {
        Formula combined1 = new Formula("and",
                kb1,
                new Formula("not", theorem1, null));

        System.out.println("Формула: " + combined1);

        Formula cnf1 = combined1.toCNF();
        System.out.println("КНФ: " + cnf1);

        List<Clause> clauses1 = cnf1.toClauses();
        System.out.println("Дизъюнкты: " + clauses1);

        boolean result1 = resolution(clauses1);
        System.out.println("Результат: " + (result1 ? "СЛЕДУЕТ" : "НЕ СЛЕДУЕТ"));
    }
}