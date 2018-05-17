/**
 * @author: Anas Abou Allaban
 * Username: piraka9011
 *
 * Question:
 * If we use the euclidean distance, we will be overly optimistic in estimating the number of moves
 * to a certain location. Moving diagonally for example requires 2 moves, which the Manhattan
 * heuristic correctly calculates, while the Euclidean heuristic would calculate it as 1 move.
 * This will still work (tested with example function below).
 * This will likely be less optimal than the Manhattan heuristic but will still be faster than the
 * Hamming heuristic (see below times).
 *
 * Times (Worst of 5):
 *
 * Heuristic: Manhattan, Hamming, Euclidean
 * Time : 99.614723 ms,  , 101.711868 ms
 */
package com;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

// Solving the 16-puzzle with A* using two heuristics:
// tiles-out-of-place and total-distance-to-move

public class NumberPuzzle {
    public static final int PUZZLE_WIDTH = 4;
    public static final int BLANK = 0;
    // BETTER:  false for tiles-displaced heuristic, true for Manhattan distance
    public static boolean BETTER = true;

    // You can change this representation if you prefer.
    // If you don't, be careful about keeping the tiles and the blank
    // row and column consistent.
    private int[][] tiles;  // [row][column]
    private int blank_r, blank_c;   // blank row and column

    public static void main(String[] args) {
        float startTime = System.nanoTime();
        NumberPuzzle myPuzzle = readPuzzle();
        LinkedList<NumberPuzzle> solutionSteps = myPuzzle.solve(BETTER);
        printSteps(solutionSteps);
        float endTime = System.nanoTime();
        float duration = (endTime - startTime) / 1000000;
        String heuristic = (BETTER) ? "Manhattan" : "Tile Place";
        System.out.printf("Heuristic: %s\n", heuristic);
        System.out.printf("Time: %f ms\n", duration);
    }

    NumberPuzzle() {
        tiles = new int[PUZZLE_WIDTH][PUZZLE_WIDTH];
    }

    static NumberPuzzle readPuzzle() {
        NumberPuzzle newPuzzle = new NumberPuzzle();
        try {
            File file = new File("/home/piraka9011/IdeaProjects/CS4100/hw1/src/com/sixteenMoves.txt");
            Scanner myScanner = new Scanner(file);
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
        }
        catch (FileNotFoundException e) {
            System.out.println("No file found");
        }

//        Scanner myScanner = new Scanner(System.in);
//
//        int row = 0;
//        while (myScanner.hasNextLine() && row < PUZZLE_WIDTH) {
//            String line = myScanner.nextLine();
//            String[] numStrings = line.split(" ");
//            for (int i = 0; i < PUZZLE_WIDTH; i++) {
//                if (numStrings[i].equals("-")) {
//                    newPuzzle.tiles[row][i] = BLANK;
//                    newPuzzle.blank_r = row;
//                    newPuzzle.blank_c = i;
//                } else {
//                    newPuzzle.tiles[row][i] = new Integer(numStrings[i]);
//                }
//            }
//            row++;
//        }

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

/**----------------- Implementation -----------------**/
/*----------------------- Node -----------------------*/

    class Node {
    /**
     * A Node class that stores the information of the puzzle state
     * @param state: The current state of the puzzle tiles
     * @param f: Current score of the node
     * @param g: Cost to travel to node (usually 1)
     * @param parent: The parent node, used to back track
     */
        NumberPuzzle state;
        int f = 0, g;
        Node parent;

        Node(NumberPuzzle state, Node parent, int cost) {
            this.g = (parent != null) ? parent.g+cost : cost;
            this.state = state;
            this.parent = parent;
            setCost();
        }

        private void setCost() {
            if (BETTER)
                this.f = this.g + getManhattanDistance();
            else
                this.f = this.g + getHammingDistance();
        }

        public boolean isSolved() {
            return state.solved();
        }

        /** Gets the (x,y) coordinates of a value*/
        private int[] getCoordinates(int num) {
            for (int row = 0; row < PUZZLE_WIDTH; row++) {
                for (int col = 0; col < PUZZLE_WIDTH; col++) {
                    if (state.tiles[row][col] == num)
                        return new int[]{row, col};
                }
            }
            return new int[]{-1, -1};
        }

        /** Returns the Manhattan Distance heuristic */
        private int getManhattanDistance() {
            int score = 0;
            int shouldBe = 1;
            int[] currCoords;
            int currRow, currCol;
            for (int goalRow = 0; goalRow < PUZZLE_WIDTH; goalRow++) {
                for (int goalCol = 0; goalCol < PUZZLE_WIDTH; goalCol++) {
                    currCoords = getCoordinates(shouldBe);
                    currRow = currCoords[0]; currCol = currCoords[1];
                    score += Math.abs(goalRow - currRow) + Math.abs(goalCol - currCol);
                    shouldBe = (shouldBe + 1) % (PUZZLE_WIDTH*PUZZLE_WIDTH);
                }
            }
            return score;
        }

        /** Returns the Hamming Distance heuristic */
        private int getHammingDistance() {
            int numOutOfPlace = 0;
            int shouldBe = 1;
            for (int i = 0; i < PUZZLE_WIDTH; i++) {
                for (int j = 0; j < PUZZLE_WIDTH; j++) {
                    if (tiles[i][j] != shouldBe)
                        numOutOfPlace++;
                    // Take advantage of BLANK == 0
                    shouldBe = (shouldBe + 1) % (PUZZLE_WIDTH*PUZZLE_WIDTH);
                }
            }
            return numOutOfPlace;
        }

        /** Returns the Euclidean distance heuristic*/
        private double getEuclideanDistance() {
            double score = 0.0;
            int shouldBe = 1;
            int[] currCoords;
            int currRow, currCol;
            double x, y;
            for (int goalRow = 0; goalRow < PUZZLE_WIDTH; goalRow++) {
                for (int goalCol = 0; goalCol < PUZZLE_WIDTH; goalCol++) {
                    // Get where the solution tile actually is now
                    currCoords = getCoordinates(shouldBe);
                    currRow = currCoords[0]; currCol = currCoords[1];
                    // Calc. euclidean distance
                    x = Math.pow(goalRow - currRow, 2);
                    y = Math.pow(goalCol - currCol, 2);
                    score += Math.sqrt(x + y);
                    shouldBe = (shouldBe + 1) % (PUZZLE_WIDTH*PUZZLE_WIDTH);
                }
            }
            return score;
        }
    }
/*----------------------- Node -----------------------*/

    /** Checks if row/col are within puzzle bounds */
    private boolean isOutOfBounds(int i, int j) {
        return ((i < 0) || (j < 0) || (i >= PUZZLE_WIDTH) || (j >= PUZZLE_WIDTH));
    }

    /** Returns a list of node neighbors (possible tile moves) */
    public LinkedList<Node> getNeighbors(Node n) {
        int bRow = n.state.blank_r;
        int bCol = n.state.blank_c;
        int newRow, newCol;
        Node newNode;
        NumberPuzzle newState;
        LinkedList<Node> neighbors = new LinkedList<Node>();

        // Iterate through possible movements
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                // Avoid same tile and diagonal moves
                if ((i*j != 0) || (i == j))
                    continue;
                // Check bounds
                newRow = i + bRow;
                newCol = j + bCol;
                if (!isOutOfBounds(newRow, newCol)) {
                    // Create a new neighbor
                    newState = n.state.copy();
                    // Move the tile
                    newState.tiles[bRow][bCol] = n.state.tiles[newRow][newCol];
                    newState.tiles[newRow][newCol] = BLANK;
                    // Add to list of neighbors and update blank location.
                    // We add the new node after updating the tiles since the
                    // cost is calculated only when the node is generated.
                    // The cost (g) of going to a new state is 1 (not f!)
                    newNode = new Node(newState, n, 1);
                    newNode.state.blank_c = newCol;
                    newNode.state.blank_r = newRow;
                    neighbors.add(newNode);
                }
            }
        }
        return neighbors;
    }

    /** Iterates from solution through parents to get a path */
    private LinkedList<NumberPuzzle> reconstructPath (Node n) {
        LinkedList<NumberPuzzle> puzzlePath = new LinkedList<NumberPuzzle>();
        Node currNode = n;
        // For hacker rank...
        puzzlePath.addFirst(currNode.state);
        // Loop till the end
        while (currNode.parent != null) {
            puzzlePath.addFirst(currNode.parent.state);
            currNode = currNode.parent;
        }
        return puzzlePath;
    }

    /** Initialize a priority queue that sorts according to F score */
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

    // betterH:  if false, use tiles-out-of-place heuristic
    //           if true, use total-manhattan-distance heuristic
    LinkedList<NumberPuzzle> solve(boolean betterH) {
        // Setup
        Node current;

        // Init Queue and explored nodes
        Queue<Node> openList = initQueue();
        HashSet<Node> closedList = new HashSet<Node>();

        // Set starting node
        Node startNode = new Node(copy(), null, 0);
        openList.add(startNode);

        // Aystaaah
        while(!openList.isEmpty()) {
            current = openList.poll();    // Remove lowest cost node
            // Check if solved
            if (current.isSolved())
                return reconstructPath(current);
            // Add node to list of visited nodes
            closedList.add(current);
            // Iterate through neighbors
            LinkedList<Node> neighbors = getNeighbors(current);
            for (Node neighbor: neighbors) {
                // Check if already visited
                if (closedList.contains(neighbor))
                    continue;
                // Check if not in the frontier
                if (!openList.contains(neighbor))
                    openList.add(neighbor);
            }
        }
        return new LinkedList<NumberPuzzle>();
    }

}
