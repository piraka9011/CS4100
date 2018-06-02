package com;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;

// An assignment on decision trees, using the "Adult" dataset from
// the UCI Machine Learning Repository.  The dataset predicts
// whether someone makes over $50K a year from their census data.
//
// Input data is a comma-separated values (CSV) file where the
// target classification is given a label of "Target."
// The other headers in the file are the feature names.
//
// Features are assumed to be strings, with comparison for equality
// against one of the values as a decision, unless the value can
// be parsed as a double, in which case the decisions are < comparisons
// against the values seen in the data.

public class DecisionTree {

    public Feature feature;   // if true, follow the yes branch
    public boolean decision;  // for leaves
    public DecisionTree yesBranch;
    public DecisionTree noBranch;

    public static double CHI_THRESH = 3.84;  // chi-square test critical value
    public static double EPSILON = 0.00000001; // for determining whether vals roughly equal
    public static boolean PRUNE = true;  // prune with chi-square test or not

    public static void main(String[] args) {
        try {
//            Scanner scanner = new Scanner(System.in);
//            File myFile = new File("adult.data.csv");
            File myFile = new File("test1.txt");

            Scanner scanner = new Scanner(myFile);
            // Keep header line around for interpreting decision trees
            String header = scanner.nextLine();
            Feature.featureNames = header.split(",");
            System.err.println("Reading training examples...");
            ArrayList<Example> trainExamples = readExamples(scanner);
            // We'll assume a delimiter of "---" separates train and test as before
            DecisionTree tree = new DecisionTree(trainExamples);
            System.out.println(tree);
            System.out.println("Training data results: ");
            System.out.println(tree.classify(trainExamples));
            System.err.println("Reading test examples...");
            ArrayList<Example> testExamples = readExamples(scanner);
            Results results = tree.classify(testExamples);
            System.out.println("Test data results: ");
            System.out.print(results);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<Example> readExamples(Scanner scanner) {
        ArrayList<Example> examples = new ArrayList<Example>();
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.startsWith("---")) {
                break;
            }
            // Skip missing data lines
            if (!line.contains("?")) {
                Example newExample = new Example(line);
                examples.add(newExample);
            }
        }
        return examples;
    }

    public static class Example {
        // Not all features will use both arrays.  The Feature.isNumerical static
        // array will determine whether the numericals array can be used.  If not,
        // the strings array will be used.  The indices correspond to the columns
        // of the input, and thus the different features.  "target" is special
        // as it gives the desired classification of the example.
        public String[] strings;     // Use only if isNumerical[i] is false
        public double[] numericals;  // Use only if isNumerical[i] is true
        boolean target;

        // Construct an example from a CSV input line
        public Example(String dataline) {
            // Assume a basic CSV with no double-quotes to handle real commas
            strings = dataline.split(",");
            // We'll maintain a separate array with everything that we can
            // put into numerical form, in numerical form.
            // No real need to distinguish doubles from ints.
            numericals = new double[strings.length];
            if (Feature.isNumerical == null) {
                // First data line; we're determining types
                Feature.isNumerical = new boolean[strings.length];
                for (int i = 0; i < strings.length; i++) {
                    if (Feature.featureNames[i].equals("Target")) {
                        target = strings[i].equals("1");
                    } else {
                        try {
                            numericals[i] = Double.parseDouble(strings[i]);
                            Feature.isNumerical[i] = true;
                        } catch (NumberFormatException e) {
                            Feature.isNumerical[i] = false;
                            // string stays where it is, in strings
                        }
                    }
                }
            } else {
                for (int i = 0; i < strings.length; i++) {
                    if (i >= Feature.isNumerical.length) {
                        System.err.println("Too long line: " + dataline);
                    } else if (Feature.featureNames[i].equals("Target")) {
                        target = strings[i].equals("1");
                    } else if (Feature.isNumerical[i]) {
                        try {
                            numericals[i] = Double.parseDouble(strings[i]);
                        } catch (NumberFormatException e) {
                            Feature.isNumerical[i] = false;
                            // string stays where it is
                        }
                    }
                }
            }
        }

        // Possibly of help in debugging:  a way to print examples
        public String toString() {
            String out = "";
            for (int i = 0; i < Feature.featureNames.length; i++) {
                out += Feature.featureNames[i] + "=" + strings[i] + ";";
            }
            return out;
        }
    }

    public static class Feature {
        // Which feature are we talking about?  Can index into Feature.featureNames
        // to get name of the feature, or into strings and numericals arrays of example
        // to get feature value
        public int featureNum;
        // WLOG assume numerical features are "less than"
        // and String features are "equal to"
        public String svalue;  // the string value to compare a string feature against
        public double dvalue;  // the numerical threshold to compare a numerical feature against
        public static String[] featureNames;  // extracted from the header
        public static boolean[] isNumerical = null;  // need to read a line to see the size

        public Feature(int featureNum, String value) {
            this.featureNum = featureNum;
            this.svalue = value;
        }

        public Feature(int featureNum, double value) {
            this.featureNum = featureNum;
            this.dvalue = value;
        }

        // Ask whether the answer is "yes" or "no" to the question implied by this feature
        // when applied to a particular example
        public boolean apply(Example e) {
            if (Feature.isNumerical[featureNum]) {
                return (e.numericals[featureNum] < dvalue);
            } else {
                return (e.strings[featureNum].equals(svalue));
            }
        }

        // It's suggested that when you generate a collection of potential features, you
        // use a HashSet to avoid duplication of features.  The equality and hashCode operators
        // that follow can help you with this.
        public boolean equals(Object o) {
            if (!(o instanceof Feature)) {
                return false;
            }
            Feature otherFeature = (Feature) o;
            if (featureNum != otherFeature.featureNum) {
                return false;
            } else if (Feature.isNumerical[featureNum]) {
                if (Math.abs(dvalue - otherFeature.dvalue) < EPSILON) {
                    return true;
                }
                return false;
            } else {
                if (svalue.equals(otherFeature.svalue)) {
                    return true;
                }
                return false;
            }
        }

        public int hashCode() {
            return (featureNum + (svalue == null ? 0 : svalue.hashCode()) + (int) (dvalue * 10000));
        }

        // Print feature's check; called when printing decision trees
        public String toString() {
            if (Feature.isNumerical[featureNum]) {
                return Feature.featureNames[featureNum] + " < " + dvalue;
            } else {
                return Feature.featureNames[featureNum] + " = " + svalue;
            }
        }
    }

    public HashSet<Feature> featureSet = new HashSet<Feature>();
    // This constructor should create the whole decision tree recursively.
    DecisionTree(ArrayList<Example> examples) {
        System.out.println(examples.get(0));
        Example e = examples.get(0);

        decision = false;  // placeholder to compile
    }

    public static class Results {
        public int true_positive;  // correctly classified "yes"
        public int true_negative;  // correctly classified "no"
        public int false_positive; // incorrectly classified "yes," should be "no"
        public int false_negative; // incorrectly classified "no", should be "yes"

        public Results() {
            true_positive = 0;
            true_negative = 0;
            false_positive = 0;
            false_negative = 0;
        }

        public String toString() {
            String out = "Precision: ";
            out += String.format("%.4f", true_positive/(double)(true_positive + false_positive));
            out += "\nRecall: " + String.format("%.4f",true_positive/(double)(true_positive + false_negative));
            out += "\n";
            out += "Accuracy: ";
            out += String.format("%.4f", (true_positive + true_negative)/(double)(true_positive + true_negative + false_positive + false_negative));
            out += "\n";
            return out;
        }
    }

    public Results classify(ArrayList<Example> examples) {
        Results results = new Results();
        // TODO your code here, classifying each example with the tree and comparing to
        // the truth to populate the results structure
        return results;
    }

    public String toString() {
        return toString(0);
    }

    // Print the decision tree as a set of nested if/else statements.
    // This is a little easier than trying to print with the root at the top.
    public String toString(int depth) {
        String out = "";
        for (int i = 0; i < depth; i++) {
            out += "    ";
        }
        if (feature == null) {
            out += (decision ? "YES" : "NO");
            out += "\n";
            return out;
        }
        out += "if " + feature + "\n";
        out += yesBranch.toString(depth+1);
        for (int i = 0; i < depth; i++) {
            out += "    ";
        }
        out += "else\n";
        out += noBranch.toString(depth+1);
        return out;
    }

}
