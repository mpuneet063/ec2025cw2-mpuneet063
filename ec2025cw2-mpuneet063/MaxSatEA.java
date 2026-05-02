import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MaxSatEA {
    static class CNF {
        int varCount = 0;
        List<List<Integer>> formula = new ArrayList<>();
    }

    static CNF loadCnfData(String filepath) {
        CNF cnf = new CNF();
        try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("c")) continue;
                if (line.startsWith("p")) {
                    String[] parts = line.split("\\s+");
                    cnf.varCount = Integer.parseInt(parts[2]);
                    continue;
                }
                String[] tokens = line.split("\\s+");
                if (tokens.length > 1) {
                    List<Integer> clause = new ArrayList<>();
                    // Skip index 0 assuming it represents weight in WDIMACS
                    for (int i = 1; i < tokens.length; i++) {
                        if (!tokens[i].equals("0")) {
                            clause.add(Integer.parseInt(tokens[i]));
                        }
                    }
                    cnf.formula.add(clause);
                }
            }
        } catch (IOException e) {
            System.err.println("File missing: " + filepath);
            System.exit(1);
        }
        return cnf;
    }

    static int isClauseTrue(List<Integer> clause, char[] binaryString) {
        for (int literal : clause) {
            boolean isPositive = literal > 0;
            int index = Math.abs(literal) - 1;
            if (index >= binaryString.length) continue;
            
            char bit = binaryString[index];
            if ((isPositive && bit == '1') || (!isPositive && bit == '0')) {
                return 1;
            }
        }
        return 0;
    }

    static int computeOverallFitness(List<List<Integer>> formula, char[] binaryString) {
        int count = 0;
        for (List<Integer> c : formula) {
            count += isClauseTrue(c, binaryString);
        }
        return count;
    }

    static void executeGeneticAlgorithm(int varLimit, List<List<Integer>> formula, double timeLimit, int popCap, double mutRate, int tournSize) {
        long tStart = System.currentTimeMillis();
        Random rand = new Random();
        
        List<char[]> swarm = new ArrayList<>(popCap);
        int[] scores = new int[popCap];
        
        for (int i = 0; i < popCap; i++) {
            char[] ind = new char[varLimit];
            for (int j = 0; j < varLimit; j++) {
                ind[j] = rand.nextBoolean() ? '1' : '0';
            }
            swarm.add(ind);
            scores[i] = computeOverallFitness(formula, ind);
        }
        
        int topScore = -1;
        char[] topSolution = null;
        for (int i = 0; i < popCap; i++) {
            if (scores[i] > topScore) {
                topScore = scores[i];
                topSolution = swarm.get(i).clone();
            }
        }
        
        int iterationCount = 0;
        
        while ((System.currentTimeMillis() - tStart) / 1000.0 < timeLimit) {
            List<char[]> nextGen = new ArrayList<>(popCap);
            nextGen.add(topSolution.clone()); 
            
            while (nextGen.size() < popCap) {
                int parentA = -1;
                int bestScoreA = -1;
                for (int k = 0; k < tournSize; k++) {
                    int idx = rand.nextInt(popCap);
                    if (scores[idx] > bestScoreA) {
                        bestScoreA = scores[idx];
                        parentA = idx;
                    }
                }
                
                int parentB = -1;
                int bestScoreB = -1;
                for (int k = 0; k < tournSize; k++) {
                    int idx = rand.nextInt(popCap);
                    if (scores[idx] > bestScoreB) {
                        bestScoreB = scores[idx];
                        parentB = idx;
                    }
                }
                
                char[] offspring = new char[varLimit];
                char[] pA = swarm.get(parentA);
                char[] pB = swarm.get(parentB);
                
                for (int i = 0; i < varLimit; i++) {
                    offspring[i] = rand.nextBoolean() ? pA[i] : pB[i];
                    if (rand.nextDouble() < mutRate) {
                        offspring[i] = offspring[i] == '1' ? '0' : '1';
                    }
                }
                nextGen.add(offspring);
            }
            
            swarm = nextGen;
            for (int i = 0; i < popCap; i++) {
                scores[i] = computeOverallFitness(formula, swarm.get(i));
                if (scores[i] > topScore) {
                    topScore = scores[i];
                    topSolution = swarm.get(i).clone();
                }
            }
            iterationCount++;
        }
        
        int evalTotal = iterationCount * popCap;
        System.out.println(evalTotal + "\t" + topScore + "\t" + new String(topSolution));
    }

    public static void main(String[] args) {
        int question = 0;
        String assignment = null;
        String clauseStr = null;
        String wdimacs = null;
        double timeBudget = 0.0;
        int repetitions = 1;
        int popSize = 50;
        Double pm = null;
        int k = 2;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-question": question = Integer.parseInt(args[++i]); break;
                case "-assignment": assignment = args[++i]; break;
                case "-clause": clauseStr = args[++i]; break;
                case "-wdimacs": wdimacs = args[++i]; break;
                case "-time_budget": timeBudget = Double.parseDouble(args[++i]); break;
                case "-repetitions": repetitions = Integer.parseInt(args[++i]); break;
                case "-pop_size": popSize = Integer.parseInt(args[++i]); break;
                case "-pm": pm = Double.parseDouble(args[++i]); break;
                case "-k": k = Integer.parseInt(args[++i]); break;
            }
        }

        if (question == 1) {
            String[] rawTokens = clauseStr.split("\\s+");
            List<Integer> parsedLits = new ArrayList<>();
            for (int i = 1; i < rawTokens.length; i++) {
                if (!rawTokens[i].equals("0")) {
                    parsedLits.add(Integer.parseInt(rawTokens[i]));
                }
            }
            System.out.println(isClauseTrue(parsedLits, assignment.toCharArray()));
        } else if (question == 2) {
            CNF cnf = loadCnfData(wdimacs);
            System.out.println(computeOverallFitness(cnf.formula, assignment.toCharArray()));
        } else if (question == 3 || question == 5) {
            CNF cnf = loadCnfData(wdimacs);
            double mutationProb = (pm != null) ? pm : 1.0 / cnf.varCount;
            
            for (int i = 0; i < repetitions; i++) {
                executeGeneticAlgorithm(cnf.varCount, cnf.formula, timeBudget, popSize, mutationProb, k);
            }
        }
    }
}
