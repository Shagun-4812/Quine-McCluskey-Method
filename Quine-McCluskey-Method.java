import java.util.*;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Ask the user if they want to enter a name or directly give minterms as integers
        System.out.println("Do you want to enter a name or directly give minterms as integers? (Enter 'name' or 'minterms'):");
        String inputType = scanner.nextLine();

        int[] minterms;
        if (inputType.equalsIgnoreCase("name")) {
            // Input a name and treat the ASCII value of each alphabet as a minterm
            System.out.println("Enter the name:");
            String name = scanner.nextLine();
            minterms = new int[name.length()];
            for (int i = 0; i < name.length(); i++) {
                minterms[i] = (int) name.charAt(i);
            }
        } else {
            // Input minterms from the user
            System.out.println("Enter the number of variables:");
            int numVariables = scanner.nextInt();
            scanner.nextLine(); // consume newline

            System.out.println("Enter the minterms separated by spaces:");
            String mintermsInput = scanner.nextLine();
            String[] mintermStrings = mintermsInput.split("\\s+");
            minterms = new int[mintermStrings.length];

            for (int i = 0; i < mintermStrings.length; i++) {
                minterms[i] = Integer.parseInt(mintermStrings[i]);
            }
        }

        // Ask for don't care terms
        System.out.println("Enter the don't care terms separated by spaces:");
        String dontCaresInput = scanner.nextLine();
        String[] dontCareStrings = dontCaresInput.split("\\s+");
        int[] dontCares = new int[dontCareStrings.length];

        for (int i = 0; i < dontCareStrings.length; i++) {
            dontCares[i] = Integer.parseInt(dontCareStrings[i]);
        }

        // Calculate the number of variables needed based on the highest minterm
        int maxTerm = Arrays.stream(minterms).max().orElse(0);
        int numVariables = Integer.toBinaryString(maxTerm).length();

        // Generate variable names a, b, c, ...
        String[] variables = new String[numVariables];
        for (int i = 0; i < numVariables; i++) {
            variables[i] = Character.toString((char) ('a' + i));
        }

        // Combine minterms and don't cares for the initial grouping
        List<Integer> combinedTerms = new ArrayList<>();
        for (int minterm : minterms) {
            combinedTerms.add(minterm);
        }
        for (int dontCare : dontCares) {
            combinedTerms.add(dontCare);
        }

        // Step 1: Convert terms to binary strings
        List<String> binaryTerms = new ArrayList<>();
        for (int term : combinedTerms) {
            binaryTerms.add(toBinaryString(term, variables.length));
        }

        System.out.println("Step 1: Binary representations of minterms and don't cares");
        for (String term : binaryTerms) {
            System.out.println(term);
        }

        // Step 2: Group terms by the number of 1s in their binary representation
        Map<Integer, List<String>> groups = new TreeMap<>();
        for (String term : binaryTerms) {
            int ones = countOnes(term);
            groups.computeIfAbsent(ones, k -> new ArrayList<>()).add(term);
        }

        System.out.println("\nStep 2: Group terms by the number of 1s");
        for (Map.Entry<Integer, List<String>> entry : groups.entrySet()) {
            System.out.println("Group " + entry.getKey() + ": " + entry.getValue());
        }

        // Step 3: Combine terms iteratively
        List<String> primeImplicants = new ArrayList<>();
        int step = 3;
        while (!groups.isEmpty()) {
            Map<Integer, List<String>> newGroups = new TreeMap<>();
            Set<String> combined = new HashSet<>();

            System.out.println("\nStep " + step + ": Combine terms");
            for (int i : groups.keySet()) {
                List<String> group = groups.get(i);
                List<String> nextGroup = groups.get(i + 1);
                if (nextGroup != null) {
                    for (String term1 : group) {
                        for (String term2 : nextGroup) {
                            String combinedTerm = combine(term1, term2);
                            if (combinedTerm != null) {
                                System.out.println(term1 + " + " + term2 + " = " + combinedTerm);
                                combined.add(term1);
                                combined.add(term2);
                                newGroups.computeIfAbsent(countOnes(combinedTerm), k -> new ArrayList<>()).add(combinedTerm);
                            }
                        }
                    }
                }
            }

            for (List<String> group : groups.values()) {
                for (String term : group) {
                    if (!combined.contains(term)) {
                        primeImplicants.add(term);
                    }
                }
            }

            groups = newGroups;
            step++;
        }

        System.out.println("\nPrime Implicants: " + primeImplicants);

        // Step 4: Create prime implicant chart
        Map<String, List<Integer>> primeImplicantChart = new LinkedHashMap<>();
        for (String prime : primeImplicants) {
            List<Integer> coveredMinterms = new ArrayList<>();
            for (int minterm : minterms) {
                if (matches(prime, toBinaryString(minterm, variables.length))) {
                    coveredMinterms.add(minterm);
                }
            }
            primeImplicantChart.put(prime, coveredMinterms);
        }

        System.out.println("\nStep 4: Prime Implicant Chart");
        for (Map.Entry<String, List<Integer>> entry : primeImplicantChart.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }

        // Step 5: Extract essential prime implicants
        Set<String> essentialPrimeImplicants = new LinkedHashSet<>();
        Set<Integer> coveredMinterms = new HashSet<>();
        while (!primeImplicantChart.isEmpty()) {
            String essential = null;
            int maxCoverage = 0;

            for (Map.Entry<String, List<Integer>> entry : primeImplicantChart.entrySet()) {
                List<Integer> mintermsList = new ArrayList<>(entry.getValue());
                mintermsList.removeAll(coveredMinterms);
                if (mintermsList.size() > maxCoverage) {
                    essential = entry.getKey();
                    maxCoverage = mintermsList.size();
                }
            }

            if (essential != null) {
                essentialPrimeImplicants.add(essential);
                coveredMinterms.addAll(primeImplicantChart.remove(essential));
                System.out.println("Selected essential prime implicant: " + essential);
            } else {
                break;
            }
        }

        System.out.println("\nEssential Prime Implicants: " + essentialPrimeImplicants);

        // Convert prime implicants to boolean expression
        String result = convertToBooleanExpression(essentialPrimeImplicants, variables);
        System.out.println("\nMinimal Quine-McCluskey Expression: " + result);
    }

    private static String toBinaryString(int num, int length) {
        String binary = Integer.toBinaryString(num);
        while (binary.length() < length) {
            binary = "0" + binary;
        }
        return binary;
    }

    private static int countOnes(String binary) {
        int count = 0;
        for (char c : binary.toCharArray()) {
            if (c == '1') {
                count++;
            }
        }
        return count;
    }

    private static String combine(String term1, String term2) {
        StringBuilder combined = new StringBuilder();
        boolean foundDifference = false;

        for (int i = 0; i < term1.length(); i++) {
            if (term1.charAt(i) == term2.charAt(i)) {
                combined.append(term1.charAt(i));
            } else if (foundDifference) {
                return null;
            } else {
                combined.append('-');
                foundDifference = true;
            }
        }

        return combined.toString();
    }

    private static boolean matches(String term, String minterm) {
        for (int i = 0; i < term.length(); i++) {
            if (term.charAt(i) != '-' && term.charAt(i) != minterm.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static String convertToBooleanExpression(Set<String> primeImplicants, String[] variables) {
        StringBuilder expression = new StringBuilder();

        for (String implicant : primeImplicants) {
            if (expression.length() > 0) {
                expression.append(" + ");
            }

            for (int i = 0; i < implicant.length(); i++) {
                if (implicant.charAt(i) == '1') {
                    expression.append(variables[i]);
                } else if (implicant.charAt(i) == '0') {
                    expression.append(variables[i]).append("'");
                }
            }
        }

        return expression.toString();
    }
}
