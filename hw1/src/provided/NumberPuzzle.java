package provided;

import java.util.*;
import java.util.stream.IntStream;

import static provided.NumberPuzzle.BLANK;
import static provided.NumberPuzzle.PUZZLE_WIDTH;

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
    SolutionPath solutionSteps = myPuzzle.solve(BETTER);
    printSteps(solutionSteps);
  }

  NumberPuzzle() {
    tiles = new int[PUZZLE_WIDTH][PUZZLE_WIDTH];
  }

  static NumberPuzzle readPuzzle() {
    NumberPuzzle newPuzzle = new NumberPuzzle();

    Scanner myScanner = new Scanner("10 2 - 4\n" +
            "1 5 3 8\n" +
            "9 7 6 12\n" +
            "13 14 11 15");
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

  // betterH:  if false, use tiles-out-of-place heuristic
  //           if true, use total-manhattan-distance heuristic
  SolutionPath solve(boolean betterH) {
    State startState = new State(this.tiles);
    State endState = startState.GOAL_STATE;
    AStar astar = new AStar();
    SolutionPath path = astar.getSolutionPath(startState, endState, betterH);
    return path;
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

  static void printSteps(SolutionPath steps) {
    System.out.println(steps.toString());
  }

}

class State {
  public static final State GOAL_STATE = theGoalState();
  private int[][] cells;
  private int hashCode = 0;

  public State(int[][] state) {
    this.cells = state;
  }

  private static State theGoalState() {
    int[][] puzzle = new int[PUZZLE_WIDTH][PUZZLE_WIDTH];

    Scanner myScanner = new Scanner("1 2 3 4\n" +
            "5 6 7 8\n" +
            "9 10 11 12\n" +
            "13 14 15 -");
    int row = 0;
    while (myScanner.hasNextLine() && row < PUZZLE_WIDTH) {
      String line = myScanner.nextLine();
      String[] numStrings = line.split(" ");
      for (int i = 0; i < PUZZLE_WIDTH; i++) {
        if (numStrings[i].equals("-")) {
          puzzle[row][i] = BLANK;
        } else {
          puzzle[row][i] = new Integer(numStrings[i]);
        }
      }
      row++;
    }
    return new State(puzzle);
  }

  public static State getGoalState() {
    return GOAL_STATE;
  }

  public int getCellCoords(CellPosn c) {
    return this.getCellCoords(c.getRow(), c.getCol());
  }

  public int getCellCoords(int row, int col) {
    return cells[row][col];
  }

  public CellPosn getEmptyCellLocation() {
    for (int i = 0; i < PUZZLE_WIDTH; i++) {
      for (int j = 0; j < PUZZLE_WIDTH; j++) {
        if (cells[i][j] == 0) {
          return new CellPosn(i, j);
        }
      }
    }

    throw new RuntimeException("Oopsie Woopsie~! UwU we made a fuggie wuckie! No empty cell!");
  }

  public Map<CellPosn, PossibleMovements> getPossibleActions() {
    Map<CellPosn, PossibleMovements> actionMap = new LinkedHashMap<>();
    CellPosn emptyCell = this.getEmptyCellLocation();

    if (emptyCell.getRow() > 0) {
      CellPosn cellUp = new CellPosn(emptyCell.getRow() - 1, emptyCell.getCol());
      actionMap.put(cellUp, PossibleMovements.DOWN);
    }

    if (emptyCell.getRow() < PUZZLE_WIDTH - 1) {
      CellPosn cellDown = new CellPosn(emptyCell.getRow() + 1, emptyCell.getCol());
      actionMap.put(cellDown, PossibleMovements.UP);
    }

    if (emptyCell.getCol() > 0) {
      CellPosn cellLeft = new CellPosn(emptyCell.getRow(), emptyCell.getCol() - 1);
      actionMap.put(cellLeft, PossibleMovements.RIGHT);
    }

    if (emptyCell.getCol() < PUZZLE_WIDTH - 1) {
      CellPosn cellRight = new CellPosn(emptyCell.getRow(), emptyCell.getCol() + 1);
      actionMap.put(cellRight, PossibleMovements.LEFT);
    }

    return actionMap;
  }

  public void setCellVal(CellPosn c, int val) {
    this.cells[c.getRow()][c.getCol()] = val;
  }

  public void switchCellPosns(CellPosn c1, CellPosn c2) {
    int temp = this.cells[c1.getRow()][c1.getCol()];
    this.cells[c1.getRow()][c1.getCol()] = this.cells[c2.getRow()][c2.getCol()];
    this.cells[c2.getRow()][c2.getCol()] = temp;
  }

  public int[][] getCells() {
    int [][] myInt = new int[cells.length][];
    for(int i = 0; i < cells.length; i++)
    {
      int[] aMatrix = cells[i];
      int   aLength = aMatrix.length;
      myInt[i] = new int[aLength];
      System.arraycopy(aMatrix, 0, myInt[i], 0, aLength);
    }
    this.hashCode = 0;
    return myInt;
  }

  @Override
  public String toString() {
    String str = "";
    for (int[] a : this.cells) {
      for (int b : a) {
        if (b == 0) {
          str += "- ";
        } else {
          str += Integer.toString(b) + " ";
        }
      }
      str += "\n";
    }
    return str;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof  State) {
      State stateO = (State)o;
      return Arrays.deepEquals(stateO.getCells(), this.cells);
    }
    return false;
  }

  @Override
  public int hashCode() {
    if (hashCode == 0) {
      int prime = 6157;
      for (int i = 0; i < PUZZLE_WIDTH; i++) {
        for (int j = 0; j < PUZZLE_WIDTH; j++) {
          prime = 49157 * prime + this.cells[i][j];
        }
      }
      this.hashCode = prime;
    }
    return this.hashCode;
  }
}

enum PossibleMovements {
  UP(0,-1), DOWN(0,1), RIGHT(1,0), LEFT(-1,0);

  private int horizontal;
  private int vertical;

  PossibleMovements(int h, int v) {
    this.horizontal = h;
    this.vertical = v;
  }

  public int getHorizontal() {
    return horizontal;
  }

  public int getVertical() {
    return vertical;
  }
}

class CellPosn {
  private int row;
  private int col;

  public CellPosn(int row, int col) {
    this.row = row;
    this.col = col;
  }

  public int getRow() {
    return this.row;
  }

  public int getCol() {
    return col;
  }

  public CellPosn next(PossibleMovements pm) {
    return new CellPosn(this.getRow() + pm.getVertical(), this.getCol() + pm.getHorizontal());
  }
}

class ManhattanDistance implements Comparator<Node> {
  public int h(Node node) {
    int cVal = node.getH();

    if (cVal == -1) {
      cVal = this.calculateMD(node.getState());
      node.setH(cVal);
    }

    return cVal;
  }

  public int calculateMD(State state) {
    int mdTotal = 0;
    int[][] cells = state.getCells();

    for (int i = 0; i < PUZZLE_WIDTH; i++) {
      for (int j = 0; j < PUZZLE_WIDTH; j++) {
        int val = cells[i][j];
        if (val == 0) {
          continue;
        }
        int goalRow = (val - 1) / PUZZLE_WIDTH;
        int goalCol = (val - 1) % PUZZLE_WIDTH;

        int md = Math.abs(i - goalRow) + Math.abs(j - goalCol);
        mdTotal += md;
      }
    }
    return mdTotal;
  }

  @Override
  public int compare(Node n1, Node n2) {
    int cost = (n1.getCost() + this.h(n1)) - (n2.getCost() + this.h(n1));

    if (cost == 0) {
      cost = n2.getCost() - n1.getCost();
    }
    return cost;
  }
}

class HammingDistance implements Comparator<Node> {
  public int h(Node node) {
    int cVal = node.getH();

    if (cVal == -1) {
      cVal = this.calculateHD(node.getState());
      node.setH(cVal);
    }

    return cVal;
  }

  public int calculateHD(State state) {
    int hdTotal = 0;
    int[][] cells = state.getCells();

    for (int i = 0; i < PUZZLE_WIDTH; i++) {
      for (int j = 0; j < PUZZLE_WIDTH; j++) {
        int val = cells[i][j];
        if (val == 0) {
          continue;
        }
        if (val != (i * PUZZLE_WIDTH + j)) {
          hdTotal += 1;
        }
      }
    }
    return hdTotal;
  }

  @Override
  public int compare(Node n1, Node n2) {
    int cost = (n1.getCost() + this.h(n1)) - (n2.getCost() + this.h(n1));

    if (cost == 0) {
      cost = n2.getCost() - n1.getCost();
    }
    return cost;
  }
}

class Node {
  private State state;
  private int cost = 0;
  private int h = -1;
  private Node parent;
  private Map<CellPosn, PossibleMovements> actionMap;

  Node(State s) {
    state = s;
  }


  public void setActionMap(Map<CellPosn, PossibleMovements> a) {
    this.actionMap = a;
  }

  public void setH(int h) {
    this.h = h;
  }

  public void setParent(Node p) {
    this.parent = p;
    this.cost = this.parent.getCost() + 1;
  }
  public State getState() {
    return state;
  }

  public Node getParent() {
    return parent;
  }

  public Map<CellPosn, PossibleMovements> getActionMap() {
    return actionMap;
  }

  public int getCost() {
    return cost;
  }


  public int getH() {
    return h;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Node){
      Node n = (Node)o;
      return this.state.equals(n.getState());
    }
    return false;
  }

  @Override
  public String toString() {
    return this.state.toString();
  }

  @Override
  public int hashCode() {
    return this.state.hashCode();
  }
}

class Steps {
  private Node[] steps;

  Steps(int len) {
    this.steps = new Node[len];
  }

  public static Steps endStep(Node endStep) {
    if (endStep == null) {
      return null;
    }

    Steps s = new Steps(endStep.getCost() + 1);

    Node[] n = s.getNodes();
    n[endStep.getCost()] = endStep;

    Node parent = endStep.getParent();
    Node current;
    while (parent != null) {
      current = parent;
      n[current.getCost()] = current;
      parent = current.getParent();
    }

    return s;
  }

  public int len() {
    return this.steps.length;
  }

  public Node[] getNodes() {
    return this.steps;
  }

  @Override
  public String toString() {
    String str = "";
    for (Node node : this.steps) {
      str += node.toString() + "\n";
    }
    return str;
  }
}

class SolutionPath {
  private Steps steps;
  private long visited;

  SolutionPath() {}

  public void setVisited(long visited) {
    this.visited = visited;
  }

  public void setSteps(Steps s) {
    this.steps = s;
  }

  public Steps getSteps() {
    return steps;
  }

  public long getVisited() {
    return visited;
  }

  @Override
  public String toString() {
    return steps.toString();
  }
}

class AStar {
  private Set<State> visited;
  private Set<Node> frontierPoints;
  private Queue<Node> frontier;
  private State endState;

  public SolutionPath getSolutionPath(State beginning, State end, boolean heuristicFunction) {
    this.init(beginning, end, heuristicFunction);
    Node endNode = this.exec();
    SolutionPath path = new SolutionPath();
    path.setSteps(Steps.endStep(endNode));
    path.setVisited(this.visited.size());

    return path;
  }


  public void init(State beginning, State end, boolean h) {
    this.visited = new HashSet<>();
    if (h) {
      this.frontier = new PriorityQueue<>(100000, new ManhattanDistance());
    } else {
      this.frontier = new PriorityQueue<>(100000, new HammingDistance());
    }
    this.frontierPoints = new HashSet<>();
    this.endState = end;
    Node n = new Node(beginning);
    frontier.add(n);
    frontierPoints.add(n);
  }

  public Node removeFromFrontiers() {
    Node node = frontier.poll();
    frontierPoints.remove(node);
    return node;
  }

//  public void putInFrontiers(Node n) {
//    frontierPoints.add(n);
//    frontier.add(n);
//  }

  public Node exec() {
    while(!frontier.isEmpty()) {
      Node node = removeFromFrontiers();
      if (node == null) {
        continue;
      }
      State s = node.getState();
      if(s.equals(endState)) {
        return node;
      }
      visited.add(s);
      Map<CellPosn, PossibleMovements> actionsMap = s.getPossibleActions();
      for (CellPosn cell : actionsMap.keySet()) {
        State newS = applyActionOn(s, cell, actionsMap.get(cell));
        if(!this.visited.contains(newS)) {
          Node newN = new Node(newS);
          newN.setParent(node);
          Map<CellPosn, PossibleMovements> newActionMap = new HashMap<>();
          newActionMap.put(cell, actionsMap.get(cell));
          newN.setActionMap(newActionMap);
          if (!frontierPoints.contains(newN)) {
            frontier.add(newN);
            frontierPoints.add(newN);
          }
        }
      }
    }
    return null;
  }

  private State applyActionOn(State s, CellPosn c, PossibleMovements pm) {
//    int val = s.getCells()[c.getRow()][c.getCol()];
//    CellPosn next = c.next(pm);
//
//    State newS = new State(s.getCells());
//    newS.setCellVal(next, val);
//    newS.setCellVal(c, 0);
    CellPosn next = c.next(pm);
    State newS = new State(s.getCells());
    newS.switchCellPosns(c, next);

    return newS;
  }
}