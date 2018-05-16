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

    public static void main(String[] args) {
        NumberPuzzle myPuzzle = readPuzzle();
        LinkedList<NumberPuzzle> solutionSteps = myPuzzle.solve(BETTER);
        printSteps(solutionSteps);
    }

    NumberPuzzle() {
        tiles = new int[PUZZLE_WIDTH][PUZZLE_WIDTH];
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

/**----------------- Implementation -----------------**/
/*----------------------- Node -----------------------*/
    class Node {
        NumberPuzzle state;
        int f = 0, g, h = 0;
        Node parent;

        Node(NumberPuzzle state, Node parent, int cost) {
            this.g = (parent != null) ? parent.g+cost : cost;
            this.state = state;
            this.parent = parent;
            setCost();
        }

        private void setCost() {
            if (BETTER)
                this.f = g + getManhattanDistance();
            else
                this.f = g + getPlaceHeuristic();
        }

        public boolean isSolved() {
            return state.solved();
        }

        private int[] getCoordinates(int num) {
            for (int row = 0; row < PUZZLE_WIDTH; row++) {
                for (int col = 0; col < PUZZLE_WIDTH; col++) {
                    if (state.tiles[row][col] == num)
                        return new int[]{row, col};
                }
            }
            return new int[]{-1, -1};
        }

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

        // Heuristic that returns the number of tiles out of place.
        private int getPlaceHeuristic() {
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
    }
/*----------------------- Node -----------------------*/

    /** Checks if row/col are within puzzle bounds **/
    private boolean isInBounds(int i, int j) {
        return ((i >= 0 && i <= PUZZLE_WIDTH-1) && (j >= 0 &&  j <= PUZZLE_WIDTH-1));
    }

    /** Returns a list of node neighbors (possible tile moves) **/
    public LinkedList<Node> getNeighbors(Node n) {
        int bRow = n.state.blank_r;
        int bCol = n.state.blank_c;
        int newRow, newCol;
        Node newNode;
        LinkedList<Node> neighbors = new LinkedList<Node>();

        // Iterate through possible movements
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                // Avoid diagonal moves
                if ((i*j != 0) || (i == j))
                    continue;
                // Check bounds
                newRow = i + bRow;
                newCol = j + bCol;
                if (isInBounds(newRow, newCol)) {
                    // Create a new neighbor
                    newNode = new Node(n.state.copy(), n, n.g);
                    // Move the tile
                    newNode.state.tiles[bRow][bCol] = n.state.tiles[newRow][newCol];
                    newNode.state.tiles[newRow][newCol] = BLANK;
                    // Add to list of neighbors
                    neighbors.add(newNode);
                }
            }
        }
        return neighbors;
    }

    /** Iterates from solution through parents to get a path **/
    private LinkedList<NumberPuzzle> reconstructPath (Node n) {
        LinkedList<NumberPuzzle> puzzlePath = new LinkedList<NumberPuzzle>();
        Node currNode = n;
        while (currNode.parent != null) {
            puzzlePath.add(currNode.state);
            currNode = currNode.parent;
        }
        return puzzlePath;
    }

    /** Initialize a priority queue that sorts according to F score**/
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
        int[] goalCoordinates;

        // Init Queue and explored nodes
        Queue<Node> openList = initQueue();

        // Set starting node
        Node startNode = new Node(copy(), null, 0);
        openList.add(startNode);

        // Aystaaah
        while(!openList.isEmpty()) {
            current = openList.poll();    // Remove lowest cost node

            if (current.isSolved())
                return reconstructPath(current);

            LinkedList<Node> neighbors = getNeighbors(current);
            for (Node neighbor: neighbors) {
                openList.add(neighbor);
            }

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
