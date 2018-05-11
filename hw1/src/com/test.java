package com;

public class test {
    public static void main(String[] args) {
        int[][] solution = new int[4][4];
        for (int row = 0; row < 4; row++) {
             for (int col = 0; col < 4; col++) {
                solution[row][col] = row*3 + col + 1;
                System.out.printf("%d, ", solution[row][col]);
            }
            System.out.println();
        }
    }
}
