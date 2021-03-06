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
    public static int NUM_QUESTIONS = 0;

    public static void main(String[] args) {
        try {
//            Scanner scanner = new Scanner(System.in);
            File myFile = new File("adult.data.csv");
//            File myFile = new File("test1.txt");
//            File myFile = new File("test2.txt");
//            File myFile = new File("testSig.txt");

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
            System.out.printf("Num Questions: %d", NUM_QUESTIONS);
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

    /**----- Implementation -----**/

    /** Calculates log base 2 of a number */
    public double log2(double x) {
        if (x <= 0)
            return 0.0;
        else
            return Math.log(x) / Math.log(2);
    }

    /** Returns the number of true targets in an example set */
    public double getTrueTargetCount(ArrayList<Example> examples) {
        double numTrue = 0;
        for (Example example : examples) {
            if (example.target)
                numTrue++;
        }
        return numTrue;
    }

    /** Returns the number of false targets in an example set */
    public double getFalseTargetCount(ArrayList<Example> examples) {
        double numFalse = 0;
        for (Example example : examples) {
            if (!example.target)
                numFalse++;
        }
        return numFalse;
    }

    /** Gets the entropy of an example set */
    public double getEntropy(ArrayList<Example> examples) {
        if(examples.size() == 1)
            return 0.0;
        else {
            double numTrue = getTrueTargetCount(examples);
            double total = examples.size();
            double probabilityTrue = numTrue/total;
            double probabilityFalse = 1 - probabilityTrue;
            return -(probabilityTrue * log2(probabilityTrue)) - (probabilityFalse * log2(probabilityFalse));
        }
    }

    /** Splits and example set based on a certain feature */
    public ArrayList<ArrayList<Example>> splitExamples(ArrayList<Example> examples, Feature feature) {
        // Setup
        ArrayList<Example> positiveExamples = new ArrayList<>();
        ArrayList<Example> negativeExamples = new ArrayList<>();
        ArrayList<ArrayList<Example>> splitExamples = new ArrayList<>();

        // Iterate over all examples and get only ones with feature we want
        for (Example example: examples) {
            if (feature.apply(example))
                positiveExamples.add(example);
            else
                negativeExamples.add(example);
        }
        splitExamples.add(positiveExamples);
        splitExamples.add(negativeExamples);
        return splitExamples;
    }

    /** Finds the example split that leads to the largest information gain */
    public Feature bestSplit(ArrayList<Example> examples, HashSet<Feature> features) {
        // Setup
        Feature bestFeature = null;
        double minGain = Double.POSITIVE_INFINITY;
        ArrayList<ArrayList<Example>> splitExamples;
        double positiveEntropy, negativeEntropy, positiveProbability, negativeProbability;
        double currentGain;

        // Iterate over all features and find the one with the least entropy
        for (Feature feature : features) {
            // Split the examples based on the current feature
            splitExamples = splitExamples(examples, feature);
            ArrayList<Example> positiveExamples = splitExamples.get(0);
            ArrayList<Example> negativeExamples = splitExamples.get(1);
            // Calculate entropy for positive and negative examples
            positiveEntropy = getEntropy(positiveExamples);
            negativeEntropy = getEntropy(negativeExamples);
            // Calculate probability of positive and negative examples
            positiveProbability = positiveExamples.size() / examples.size();
            negativeProbability = negativeExamples.size() / examples.size();
            // Calculate information gain
            currentGain = (positiveEntropy * positiveProbability) + (negativeEntropy * negativeProbability);
            // Minimize
            if (currentGain < minGain) {
                minGain = currentGain;
                bestFeature = feature;
            }
        }

        return bestFeature;
    }

    /** Checks if an example set is pure (All examples have the same target) */
    public boolean isPure(ArrayList<Example> examples) {
        // Get the first target element
        boolean result = examples.get(0).target;
        // If one of the examples are different, we don't have a pure set
        for (Example example: examples) {
            if (result != example.target)
                return false;
        }
        // All examples have the same target
        return true;
    }

    /** Gets the target value that the majority of the examples have */
    public boolean getMajorityTarget(ArrayList<Example> examples) {
        int numTrue = 0, numFalse = 0;
        // Iterate over all examples and count the number of T/F targets
        for (Example example : examples) {
            if (example.target)
                numTrue++;
            else
                numFalse++;
        }
        return numTrue >= numFalse;
    }

    /** Creates a set of all features in the current example list */
    public static HashSet<Feature> generateFeatures(ArrayList<Example> examples) {
        // Setup
        HashSet<Feature> allFeatures  = new HashSet<>();
        // Iterate over all examples
        for (Example example : examples) {
            // Iterate over all columns
            for (int i = 0; i < example.strings.length; i++) {
                // Skip the target column
                if (Feature.featureNames[i].equals("Target"))
                    continue;
                // Otherwise, add feature if its either numerical or string
                if (Feature.isNumerical[i]){
                    Feature newFeature = new Feature(i, example.numericals[i]);
                    allFeatures.add(newFeature);
                }
                else {
                    Feature newFeature = new Feature(i, example.strings[i]);
                    allFeatures.add(newFeature);
                }
            }
        }
        return allFeatures;
    }

    /** Conducts the Chi-Square test on an example set and its split */
    public double chiSquareTest(ArrayList<ArrayList<Example>> splitExamples, ArrayList<Example> examples) {
        ArrayList<Example> positiveExamples = splitExamples.get(0);
        ArrayList<Example> negativeExamples = splitExamples.get(1);
        // Setup counts
        double total = examples.size();
        double originalTrue = getTrueTargetCount(examples);
        double originalFalse = getFalseTargetCount(examples);
        double totalPositive = positiveExamples.size();
        double totalNegative = negativeExamples.size();
        // TP: True Target AND Positive Feature    TN: True Target AND Negative Feature
        // FP: False Target AND Positive Feature    FN: False Target AND Negative Feature
        /// Observed Counts
        double observedTP = getTrueTargetCount(positiveExamples);
        double observedTN = getTrueTargetCount(negativeExamples);
        double observedFP = getFalseTargetCount(positiveExamples);
        double observedFN = getFalseTargetCount(negativeExamples);
        /// Expected Counts
        double expectedTP = (originalTrue * totalPositive) / total;
        double expectedTN = (originalTrue * totalNegative) / total;
        double expectedFP = (originalFalse * totalPositive) / total;
        double expectedFN = (originalFalse * totalPositive) / total;
        // Chi-Square formula
        double chi1 = Math.pow(observedTP - expectedTP, 2) / expectedTP;
        double chi2 = Math.pow(observedTN - expectedTN, 2) / expectedTN;
        double chi3 = Math.pow(observedFP - expectedFP, 2) / expectedFP;
        double chi4 = Math.pow(observedFN - expectedFN, 2) / expectedFN;
        return chi1 + chi2 + chi3 + chi4;
    }

    // This constructor should create the whole decision tree recursively.
    DecisionTree(ArrayList<Example> examples) {
        // If we have a pure example set, we don't need to split and we have our decision
        if (isPure(examples)){
            this.decision = examples.get(0).target;
            this.feature = null;
        }
        else {
            // Get all the features in our example set
            HashSet<Feature> allFeatures = generateFeatures(examples);
            // Find the best feature to split on based on max information gain
            Feature bestSplitFeature = bestSplit(examples, allFeatures);
            // If there is no best feature to split on, then return the majority target
            if (bestSplitFeature == null) {
                this.decision = getMajorityTarget(examples);
                this.feature = null;
            }
            else {
                // Split the examples based on the best feature
                ArrayList<ArrayList<Example>> splitExamples = splitExamples(examples, bestSplitFeature);
                this.feature = bestSplitFeature;
                ///----- Pruning -----///
                if (PRUNE) {
                    double threshold = chiSquareTest(splitExamples, examples);
                    if (threshold < CHI_THRESH) {
                        this.decision = getMajorityTarget(examples);
                        this.feature = null;
                    } else {
                        NUM_QUESTIONS++;
                        if (!splitExamples.get(0).isEmpty())
                            yesBranch = new DecisionTree(splitExamples.get(0));
                        if (!splitExamples.get(1).isEmpty())
                            noBranch = new DecisionTree(splitExamples.get(1));
                    }
                }
                ///----- Non-pruned tree -----///
                else {
                    NUM_QUESTIONS++;
                    // Recurse and split branches down as needed
                    if (!splitExamples.get(0).isEmpty())
                        yesBranch = new DecisionTree(splitExamples.get(0));
                    if (!splitExamples.get(1).isEmpty())
                        noBranch = new DecisionTree(splitExamples.get(1));
                }
            }
        }
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

    public boolean recursiveClassify(DecisionTree branch, Feature feature, Example example) {
        // Leaf node is when the feature is null
        if (feature == null)
            return branch.decision;
        // Otherwise keep going down the tree
        else {
            // yesBranch if feature applies, noBranch if it doesn't
            if (feature.apply(example))
                return recursiveClassify(branch.yesBranch, branch.yesBranch.feature, example);
            else
                return recursiveClassify(branch.noBranch, branch.noBranch.feature, example);
        }
    }

    public Results classify(ArrayList<Example> examples) {
        Results results = new Results();

        boolean result;
        for (Example example : examples) {
            // Recurse down the branches to get decision
            if (feature == null)
                result = this.decision;
            else if (feature.apply(example))
                result = recursiveClassify(yesBranch, yesBranch.feature, example);
            else
                result = recursiveClassify(noBranch, noBranch.feature, example);

            /// Results
            // True Positive: If event and prediction positive
            if (example.target && result)
                results.true_positive++;
            // True Negative: If event negative and prediction negative
            if (!example.target && !result)
                results.true_negative++;
            // False Positive: If event negative and prediction positive
            if (!example.target && result)
                results.false_positive++;
            // False Negative: If event positive and prediction negative
            if (example.target && !result)
                results.false_negative++;
        }
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