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
    private Node[][] node_tiles;

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

    class Edge {
        private int cost = 0;
        private Node target;

        Edge (Node targetNode) {
            target = targetNode;
        }
    }

    class Node {
        private int value;
        private int x;
        private int y;
        private int h;
        private int f = 0;
        private int g;
        private Edge[] neighbors;
        private Node parent;

        Node(int val, int row, int col) {
            value = val;
            x = row;
            y = col;
        }
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

    public void initNodes() {
        for (int i = 0; i < PUZZLE_WIDTH; i++) {
            for (int j = 0; j < PUZZLE_WIDTH; j++) {
                node_tiles[i][j] = new Node(tiles[i][j], i, j);
            }
        }
    }

    public void initNeighbors() {
        for (int i = 0; i < PUZZLE_WIDTH; i++) {
            for (int j = 0; j < PUZZLE_WIDTH; j++) {
                node_tiles[i][j].neighbors = new Edge[]{
                        new Edge(node_tiles[i+1][j]),   // Top
                        new Edge(node_tiles[i-1][j]),   // Bottom
                        new Edge(node_tiles[i][j+1]),   // right
                        new Edge(node_tiles[i][j-1]),   // Left
                        new Edge(node_tiles[i+1][j+1]), // Diagonals
                        new Edge(node_tiles[i+1][j-1]),
                        new Edge(node_tiles[i-1][j+1]),
                        new Edge(node_tiles[i-1][j-1])
                }
            }
        }
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

//    private PriorityQueue<Node> initManhattanQueue() {
//        return new PriorityQueue<>(32, new Comparator<Node>() {
//            public int compare(Node o1, Node o2) {
//                int d1 = getManhattanDistance(o1);
//                int d2 = getManhattanDistance(o2);
//                if (d1 < d2)
//                {
//                    return 1;
//                }
//                return 0;
//            }
//        });
//    }



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
        initNodes();
        initNeighbors();

        // Init Queue and explored nodes
        Set<Node> exploredNodes = new HashSet<Node>();
        Queue<Node> priorityQueue = initQueue();

        // Set starting node
        Node startNode = new Node(BLANK, blank_r, blank_c);
        priorityQueue.add(startNode);

        // Aystaaah
        while(!priorityQueue.isEmpty()) {
            current = priorityQueue.poll();    // Remove lowest cost node

            // If goal is reached, return
            if (solved()) {
                break;
            }

            // For each neighbor of n:
            for (Edge neighbor : current.neighbors) {
                Node child = neighbor.target;
                int cost = neighbor.cost;
                
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
