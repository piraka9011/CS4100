package com;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

public class OnIce {

    public static final double GOLD_REWARD = 100.0;
    public static final double PIT_REWARD = -150.0;
    public static final double DISCOUNT_FACTOR = 0.5;
    public static final double EXPLORE_PROB = 0.2;  // for Q-learning
    public static final double LEARNING_RATE = 0.1;
    public static final int ITERATIONS = 10000;
    public static final int MAX_MOVES = 1000;
    public static int MAX_ROW;
    public static int MAX_COL;
    public static String[] DIRECTIONS = {"U", "D", "L", "R"};

    // Using a fixed random seed so that the behavior is a little
    // more reproducible across runs & students
    public static Random rng = new Random(2018);

    public static void main(String[] args) {
        try {
            File myFile = new File("bigQ.txt");
//            File myFile = new File("0mdp.txt");
//            File myFile = new File("3q.txt");
            Scanner myScanner = new Scanner(myFile);
            Problem problem = new Problem(myScanner);
            Policy policy = problem.solve(ITERATIONS);
            if (policy == null) {
                System.err.println("No policy.  Invalid solution approach?");
            } else {
                System.out.println(policy);
            }
            if (args.length > 0 && args[0].equals("eval")) {
                System.out.println("Average utility per move: "
                        + tryPolicy(policy, problem));
            }

        }
            catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    public static class Problem {
        public String approach;
        public double[] moveProbs;
        public ArrayList<ArrayList<String>> map;

        // Format looks like
        // MDP    [approach to be used]
        // 0.7 0.2 0.1   [probability of going 1, 2, 3 spaces]
        // - - - - - - P - - - -   [space-delimited map rows]
        // - - G - - - - - P - -   [G is gold, P is pit]
        //
        // You can assume the maps are rectangular, although this isn't enforced
        // by this constructor.

        Problem (Scanner sc) {
            approach = sc.nextLine();
            String probsString = sc.nextLine();
            String[] probsStrings = probsString.split(" ");
            moveProbs = new double[probsStrings.length];
            for (int i = 0; i < probsStrings.length; i++) {
                try {
                    moveProbs[i] = Double.parseDouble(probsStrings[i]);
                } catch (NumberFormatException e) {
                    break;
                }
            }
            map = new ArrayList<ArrayList<String>>();
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                String[] squares = line.split(" ");
                ArrayList<String> row = new ArrayList<String>(Arrays.asList(squares));
                map.add(row);
            }
        }

        Policy solve(int iterations) {
            if (approach.equals("MDP")) {
                MDPSolver mdp = new MDPSolver(this);
                return mdp.solve(this, iterations);
            } else if (approach.equals("Q")) {
                QLearner q = new QLearner(this);
                return q.solve(this, iterations);
            }
            return null;
        }

    }

    public static class Policy {
        public String[][] bestActions;

        public Policy(Problem prob) {
            bestActions = new String[prob.map.size()][prob.map.get(0).size()];
        }

        public String toString() {
            String out = "";
            for (int r = 0; r < bestActions.length; r++) {
                for (int c = 0; c < bestActions[0].length; c++) {
                    if (c != 0) {
                        out += " ";
                    }
                    out += bestActions[r][c];
                }
                out += "\n";
            }
            return out;
        }
    }

    // Returns the average utility per move of the policy,
    // as measured from ITERATIONS random drops of an agent onto
    // empty spaces
    public static double tryPolicy(Policy policy, Problem prob) {
        int totalUtility = 0;
        int totalMoves = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            // Random empty starting loc
            int row, col;
            do {
                row = rng.nextInt(prob.map.size());
                col = rng.nextInt(prob.map.get(0).size());
            } while (!prob.map.get(row).get(col).equals("-"));
            // Run until pit, gold, or MAX_MOVES timeout
            // (in case policy recommends driving into wall repeatedly,
            // for example)
            for (int moves = 0; moves < MAX_MOVES; moves++) {
                totalMoves++;
                String policyRec = policy.bestActions[row][col];
                // Determine how far we go in that direction
                int displacement = 1;
                double totalProb = 0;
                double moveSample = rng.nextDouble();
                for (int p = 0; p <= prob.moveProbs.length; p++) {
                    totalProb += prob.moveProbs[p];
                    if (moveSample <= totalProb) {
                        displacement = p+1;
                        break;
                    }
                }
                int new_row = row;
                int new_col = col;
                if (policyRec.equals("U")) {
                    new_row -= displacement;
                    if (new_row < 0) {
                        new_row = 0;
                    }
                } else if (policyRec.equals("R")) {
                    new_col += displacement;
                    if (new_col >= prob.map.get(0).size()) {
                        new_col = prob.map.get(0).size()-1;
                    }
                } else if (policyRec.equals("D")) {
                    new_row += displacement;
                    if (new_row >= prob.map.size()) {
                        new_row = prob.map.size()-1;
                    }
                } else if (policyRec.equals("L")) {
                    new_col -= displacement;
                    if (new_col < 0) {
                        new_col = 0;
                    }
                }
                row = new_row;
                col = new_col;
                if (prob.map.get(row).get(col).equals("G")) {
                    totalUtility += GOLD_REWARD;
                    // End the current trial
                    break;
                } else if (prob.map.get(row).get(col).equals("P")) {
                    totalUtility += PIT_REWARD;
                    break;
                }
            }
        }

        return totalUtility/(double)totalMoves;
    }

    static int fixRow(int row) {
        if (row >= MAX_ROW)
            return MAX_ROW - 1;
        if (row < 0)
            return 0;
        return row;
    }

    static int fixCol(int col) {
        if (col >= MAX_COL)
            return MAX_COL - 1;
        if (col < 0)
            return 0;
        return col;
    }

    static int[] fixRowCol(int row, int col) {
        int[] results = new int[2];
        row = fixRow(row);
        col = fixCol(col);
        results[0] = row;
        results[1] = col;
        return results;
    }

    static Policy updatePolicy(Policy policy, int bestAction, int row, int col) {
        policy.bestActions[row][col] = DIRECTIONS[bestAction];
        return policy;
    }

    static boolean elementExists(double[] probs, int index) {
        try {
            double x = probs[index];
            return true;
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }
    }

    public static class MDPSolver {

        // We'll want easy access to the real rewards while iterating, so
        // we'll keep both of these around
        public double[][] utilities;
        public double[][] rewards;
        public final int UP = 0, DOWN = 1, LEFT = 2, RIGHT = 3;
        public final int ONE_STEP = 0, TWO_STEP = 1, THREE_STEP = 2;
        public int[][][] dims = { { {-1, 0}, {-2, 0}, {-3, 0}}, {}};
        public MDPSolver(Problem prob) {
            utilities = new double[prob.map.size()][prob.map.get(0).size()];
            rewards = new double[prob.map.size()][prob.map.get(0).size()];
            // Initialize utilities to the rewards in their spaces,
            // else 0
            for (int r = 0; r < utilities.length; r++) {
                for (int c = 0; c < utilities[0].length; c++) {
                    String spaceContents = prob.map.get(r).get(c);
                    if (spaceContents.equals("G")) {
                        utilities[r][c] = GOLD_REWARD;
                        rewards[r][c] = GOLD_REWARD;
                    } else if (spaceContents.equals("P")) {
                        utilities[r][c] = PIT_REWARD;
                        rewards[r][c] = PIT_REWARD;
                    } else {
                        utilities[r][c] = 0.0;
                        rewards[r][c] = 0.0;
                    }
                }
            }
        }

        double move(int direction, int row, int col) {
            // Adjust row and col if they are out of bounds
            int[] fixedRowCol = fixRowCol(row, col);
            row = fixedRowCol[0];   col = fixedRowCol[1];

            switch (direction) {
                case UP:
                    row = fixRow(row);
                    return utilities[row][col];
                case DOWN:
                    row = fixRow(row);
                    return utilities[row][col];
                case LEFT:
                    col = fixCol(col);
                    return utilities[row][col];
                case RIGHT:
                    col = fixCol(col);
                    return utilities[row][col];
            }
            return Double.NEGATIVE_INFINITY;
        }

        double getDirectionUtility(Problem prob, int dir, int row, int col) {
            double result = 0;
            int[][] rowDims = { {-1, -2, -3 }, {1, 2, 3}, {0, 0, 0}, {0, 0, 0} };
            int[][] colDims = { {0, 0, 0}, {0, 0, 0}, {-1, -2, -3}, {1, 2, 3} };

            // Check if we have probability of moving one, two, or three steps
            if (elementExists(prob.moveProbs, ONE_STEP)) {
                result += (prob.moveProbs[ONE_STEP] *
                        move(dir, row + rowDims[dir][ONE_STEP], col + colDims[dir][ONE_STEP]));
            }
            if (elementExists(prob.moveProbs, TWO_STEP)) {
                result += (prob.moveProbs[TWO_STEP] *
                        move(dir, row + rowDims[dir][TWO_STEP], col + colDims[dir][TWO_STEP]));
            }
            if (elementExists(prob.moveProbs, THREE_STEP)) {
                result += (prob.moveProbs[THREE_STEP] *
                        move(dir, row + rowDims[dir][THREE_STEP], col + colDims[dir][THREE_STEP]));
            }

            return result;
        }

        double[] getActionUtilities(Problem prob, int row, int col) {
            // Find utility of each action
            double[] actions = new double[4];
            actions[UP] = getDirectionUtility(prob, UP, row, col);
            actions[DOWN] = getDirectionUtility(prob, DOWN, row, col);
            actions[LEFT] = getDirectionUtility(prob, LEFT, row, col);
            actions[RIGHT] = getDirectionUtility(prob, RIGHT, row, col);
            return actions;
        }

        int getBestAction(double[] actions) {
            double maxAction = Double.NEGATIVE_INFINITY;
            int maxI = 0;
            for (int i = 0; i < actions.length; i++) {
                if (actions[i] > maxAction) {
                    maxAction = actions[i];
                    maxI = i;
                }
            }
            return maxI;
        }

        void updateUtility(double action, int row, int col) {
            utilities[row][col] = rewards[row][col] + (DISCOUNT_FACTOR * action);
        }

        Policy solve(Problem prob, int iterations) {
            /// Setup
            Policy policy = new Policy(prob);
            double[] actions;
            int bestAction;
            // Init. max row and col sizes
            MAX_ROW = prob.map.size();
            MAX_COL = prob.map.get(0).size();

            // Iterate `iterations` number of times
            for (int i = 0; i < iterations; i++) {
                // Iterate over all states
                for (int row = 0; row < MAX_ROW; row++) {
                    for (int col = 0; col < MAX_COL; col++) {
                        // Skip if a reward state
                        if (rewards[row][col] == GOLD_REWARD)
                            policy.bestActions[row][col] = "G";
                        else if (rewards[row][col] == PIT_REWARD)
                            policy.bestActions[row][col] = "P";
                        // Compute otherwise
                        else {
                            // Get the utilities of all actions
                            actions = getActionUtilities(prob, row, col);
                            // Find which one is the best action
                            bestAction = getBestAction(actions);
                            // Update the utility and policy
                            updateUtility(actions[bestAction], row, col);
                            policy = updatePolicy(policy, bestAction, row, col);
                        }
                    }
                }
            }
            return policy;
        }

    }

    // QLearner:  Same problem as MDP, but the agent doesn't know what the
    // world looks like, or what its actions do.  It can learn the utilities of
    // taking actions in particular states through experimentation, but it
    // has no way of realizing what the general action model is
    // (like "Right" increasing the column number in general).
    public static class QLearner {

        // Use these to index into the first index of utilities[][][]
        public static final int UP = 0;
        public static final int DOWN = 1;
        public static final int LEFT = 2;
        public static final int RIGHT = 3;
        public static final int ACTIONS = 4;

        public double utilities[][][];  // utilities of actions
        public double rewards[][];

        public QLearner(Problem prob) {
            utilities = new double[ACTIONS][prob.map.size()][prob.map.get(0).size()];
            // Rewards are for convenience of lookup; the learner doesn't
            // actually "know" they're there until encountering them
            rewards = new double[prob.map.size()][prob.map.get(0).size()];
            for (int r = 0; r < rewards.length; r++) {
                for (int c = 0; c < rewards[0].length; c++) {
                    String locType = prob.map.get(r).get(c);
                    if (locType.equals("G")) {
                        rewards[r][c] = GOLD_REWARD;
                    } else if (locType.equals("P")) {
                        rewards[r][c] = PIT_REWARD;
                    } else {
                        rewards[r][c] = 0.0; // not strictly necessary to init
                    }
                }
            }
            // Java: default init utilities to 0
        }

        int updateRow(int action, int row) {
            switch (action) {
                case UP:
                    row = fixRow(row - 1);
                    break;
                case DOWN:
                    row = fixRow(row + 1);
                    break;
                case LEFT: case RIGHT:
                    break;
            }
            return row;
        }

        int updateCol(int action, int col) {
            switch (action) {
                case UP: case DOWN:
                    break;
                case LEFT:
                    col = fixCol(col - 1);
                    break;
                case RIGHT:
                    col = fixCol(col + 1);
                    break;
            }
            return col;
        }

        int bestAction(int row, int col) {
            double maxQ = Double.NEGATIVE_INFINITY;
            int maxA = 0;
            for (int a = 0; a < ACTIONS; a++) {
                double currentUtil = utilities[a][row][col];
                if (currentUtil > maxQ) {
                    maxQ = currentUtil;
                    maxA = a;
                }
            }
            return maxA;
        }

        double Q(int action, int row, int col) {
            return utilities[action][row][col];
        }

        double R(int row, int col) {
            return rewards[row][col];
        }

        void updateQ(int action, int row, int col, int nextAction, int newRow, int newCol) {
            double QPrime = Q(nextAction, newRow, newCol);
            double Q = Q(action, row, col);
            double R = R(row, col);
            utilities[action][row][col] = Q + (LEARNING_RATE * (R + DISCOUNT_FACTOR * QPrime - Q));
        }

        boolean isTerminalState(int row, int col) {
            return rewards[row][col] == GOLD_REWARD || rewards[row][col] == PIT_REWARD;
        }

        Policy findPolicy(Policy policy) {
            int action;

            // Iterate over all possible states
            for (int row = 0; row < MAX_ROW; row++) {
                for (int col = 0; col < MAX_COL; col++) {
                    // Find the best action from the current state
                    action = bestAction(row, col);
                    // Add to path
                    if (rewards[row][col] == GOLD_REWARD)
                        policy.bestActions[row][col] = "G";
                    else if (rewards[row][col] == PIT_REWARD)
                        policy.bestActions[row][col] = "P";
                    else
                        policy.bestActions[row][col] = DIRECTIONS[action];
                }
            }
            return policy;
        }

        public Policy solve(Problem prob, int iterations) {
            Policy policy = new Policy(prob);
            // Init. max row and col sizes
            MAX_ROW = prob.map.size();
            MAX_COL = prob.map.get(0).size();
            int row, col;
            int newRow, newCol;
            int action, nextAction;

            for (int i = 0; i < iterations; i++){
                // Start randomly each iteration
                row = rng.nextInt(MAX_ROW);
                col = rng.nextInt(MAX_COL);
                // Play the game until we win/lose
                while (!isTerminalState(row, col)) {
                    // Otherwise compute Q(s, a)
                    // Decide whether to move in a random direction...
                    if (rng.nextDouble() < EXPLORE_PROB)
                        action = rng.nextInt(ACTIONS);
                    // Or the best Q-Value of its current square
                    else
                        action = bestAction(row, col);

                    // Find the new position
                    newRow = updateRow(action, row);
                    newCol = updateCol(action, col);

                    // Get the best action in the next state
                    nextAction = bestAction(newRow, newCol);

                    // Update utility
                    updateQ(action, row, col, nextAction, newRow, newCol);
                    // Update State
                    row = newRow;
                    col = newCol;
                    }

                // Set all actions to reward value
                if (isTerminalState(row, col)) {
                    for (int a = 0; a < ACTIONS; a++) {
                        utilities[a][row][col] = rewards[row][col];
                    }
                }
            }

            policy = findPolicy(policy);
            return policy;
        }
    }
}
