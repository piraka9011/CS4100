package com;

import java.util.*;

// Solving the 16-puzzle with A* using two heuristics:
// tiles-out-of-place and total-distance-to-move

public class NumberPuzzle {
    public static final int PUZZLE_WIDTH = 4;
    public static final int BLANK = 0;
    // BETTER:  false for tiles-displaced heuristic, true for Manhattan distance
    public static boolean BETTER = false;

    // You can change this representation if you prefer.
    // If you don't, be careful about keeping the tiles and the blank
    // row and column consistent.
    private int[][] tiles;  // [row][column]
    private int blank_r, blank_c;   // blank row and column

    private int[][] solution;

    public static void main(String[] args) {
        NumberPuzzle myPuzzle = readPuzzle();
        LinkedList<NumberPuzzle> solutionSteps = myPuzzle.solve(BETTER);
        printSteps(solutionSteps);
    }

    NumberPuzzle() {
        tiles = new int[PUZZLE_WIDTH][PUZZLE_WIDTH];
        solution = new int[][]{{1, 2, 3, 4},
                               {5, 6, 7, 8},
                               {9, 10, 11, 12},
                               {13, 14, 15, 0}};
    }

    static NumberPuzzle readPuzzle() {
        NumberPuzzle newPuzzle = new NumberPuzzle();

        Scanner myScanner = new Scanner(System.in);
        int row = 0;
        while (myScanner.hasNextLine() && row < PUZZLE_WIDTH) {
            String line = myScanner.nextLine();
            String[] numStrings = line.split(" ");
            for (int i = 0; i < PUZZLE_WIDTH; i++) {
                if (numStrings[i].equals("-")) {
                    newPuzzle.tiles[row][i] = BLANK;
                    newPuzzle.blank_r = row;
                    newPuzzle.blank_c = i;
                } else {
                    newPuzzle.tiles[row][i] = new Integer(numStrings[i]);
                }
            }
            row++;
        }
        return newPuzzle;
    }

    public String toString() {
        String out = "";
        for (int i = 0; i < PUZZLE_WIDTH; i++) {
            for (int j = 0; j < PUZZLE_WIDTH; j++) {
                if (j > 0) {
                    out += " ";
                }
                if (tiles[i][j] == BLANK) {
                    out += "-";
                } else {
                    out += tiles[i][j];
                }
            }
            out += "\n";
        }
        return out;
    }

    public NumberPuzzle copy() {
        NumberPuzzle clone = new NumberPuzzle();
        clone.blank_r = blank_r;
        clone.blank_c = blank_c;
        for (int i = 0; i < PUZZLE_WIDTH; i++) {
            for (int j = 0; j < PUZZLE_WIDTH; j++) {
                clone.tiles[i][j] = this.tiles[i][j];
            }
        }
        return clone;
    }

/*------------------ Implementation ------------------*/

    class Node {
        NumberPuzzle state;
        int f = 0, g;
        Node parent;

        Node(NumberPuzzle state, Node parent, int cost) {
            this.g = (parent != null) ? parent.g+cost : cost;
            this.state = state;
            this.parent = parent;
            this.f = g + cost;
        }
    }

    public Set<Node> getNeighbors(Node n) {
        int b_row = n.state.blank_r;
        int b_col = n.state.blank_c;
        Set<Node> neighbors = new HashSet<Node>();
        // Top

        return neighbors;
    }

    public int[] getGoalCoord(int num) {
        // Check bounds
        if (num < 0 || num > 15)
            throw new IndexOutOfBoundsException("Please choose a number between 0 and 15");
        // Init result
        int[] coords = new int[]{0, 0};
        // Loop through solution table
        loop:
        for (int row = 0; row < PUZZLE_WIDTH; row++) {
            for (int col = 0; col < PUZZLE_WIDTH; col++) {
                if (solution[row][col] == num){
                    coords = new int[]{row, col};
                    break loop;
                }
            }
        }
        return coords;
    }



//    public int getManhattanDistance(Node n) {
//        int[] goalPos = getGoalCoord(n.value);
//        int goalRow = goalPos[0], goalCol = goalPos[1];
//        int result = Math.abs(goalRow - n.position[0]) + Math.abs(goalCol - n.position[1]);
//        return result;
//    }

    public int manhattanHeuristic(int currRow, int currCol) {
        // Get the number we're at now
        int currNum = tiles[currRow][currCol];
        // Get the goal coordinates of the number
        int[] goalPos = getGoalCoord(currNum);
        int goalRow = goalPos[0], goalCol = goalPos[1];
        // Calculate heuristic
        int result = Math.abs(goalRow - currRow) + Math.abs(goalCol - currCol);
        return result;
    }

    private PriorityQueue<Node> initQueue() {
        return new PriorityQueue<>(32, new Comparator<Node>() {
            public int compare(Node n1, Node n2) {
                if (n1.f > n2.f)
                    return 1;
                else if (n1.f < n2.f)
                    return -1;
                else
                    return 0;
            }
        });
    }

    public boolean isSolved(Node n) {
        NumberPuzzle np = n.state;
        int shouldBe = 1;
        for (int row = 0; row < PUZZLE_WIDTH; row++) {
            for (int col = 0; col < PUZZLE_WIDTH; col++) {
                if (np.tiles[row][col] != shouldBe) {
                    return false;
                } else {
                    // Take advantage of BLANK == 0
                    shouldBe = (shouldBe + 1) % (PUZZLE_WIDTH*PUZZLE_WIDTH);
                }
            }
        }
        return true;
    }

    // Heuristic that returns the number of tiles out of place.
    public int placeHeuristic() {
        int numOutOfPlace = 0;
        int iterationNum = 1;
        for (int col = 0; col < PUZZLE_WIDTH; col++) {
            for (int row = 0; row < PUZZLE_WIDTH; row++) {
                if (tiles[row][col] != iterationNum) {
                    numOutOfPlace++;
                }
                iterationNum++;
            }
        }
        return numOutOfPlace;
    }

    // betterH:  if false, use tiles-out-of-place heuristic
    //           if true, use total-manhattan-distance heuristic
    LinkedList<NumberPuzzle> solve(boolean betterH) {
        // Setup
        Node current;
        int[] goalCoordinates;

        // Init Queue and explored nodes
        Set<Node> closedList = new HashSet<Node>();
        Queue<Node> openList = initQueue();

        // Set starting node
        Node startNode = new Node(copy(), null, 0);
        openList.add(startNode);

        // Aystaaah
        while(!openList.isEmpty()) {
            current = openList.poll();    // Remove lowest cost node
            closedList.add(current);

            if (!closedList.contains(current)) {
                closedList.add(current);
                // If goal is reached, return
                if (isSolved(current)) {
                    break;
                }

                Set<Node>
            }




            // For each neighbor of n:

        }
        return new LinkedList<NumberPuzzle>();
    }

    public boolean solved() {
        int shouldBe = 1;
        for (int i = 0; i < PUZZLE_WIDTH; i++) {
            for (int j = 0; j < PUZZLE_WIDTH; j++) {
                if (tiles[i][j] != shouldBe) {
                    return false;
                } else {
                    // Take advantage of BLANK == 0
                    shouldBe = (shouldBe + 1) % (PUZZLE_WIDTH*PUZZLE_WIDTH);
                }
            }
        }
        return true;
    }

    static void printSteps(LinkedList<NumberPuzzle> steps) {
        for (NumberPuzzle s : steps) {
            System.out.println(s);
        }
    }

}
