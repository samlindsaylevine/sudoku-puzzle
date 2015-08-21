import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/** A representation of the state of a Sudoku puzzle.
 */
public class SudokuPuzzle
{

  /////////////////////////////////////////////////////////////////
  // Constants
  /////////////////////////////////////////////////////////////////

  /** The allowed values to go in a box.
   *  The single-space string has the special meaning 'unfilled.'
   *  (This makes implementing the definitive string representation 
   *  particularly simple.)
   */
  private static final String[] allowedValues =
    { " ",
      "1",
      "2",
      "3",
      "4",
      "5",
      "6",
      "7",
      "8",
      "9" };

  private static final int boxWidth = 3;
  private static final int numBoxes = 3;
  private static final int puzzleWidth = boxWidth * numBoxes;

  /////////////////////////////////////////////////////////////////
  // Private Member Variables
  /////////////////////////////////////////////////////////////////
  
  /** The current state of this puzzle.
   */
  private String[][] boxes;

  /////////////////////////////////////////////////////////////////
  // Constructors
  /////////////////////////////////////////////////////////////////  
  
  /** The default constructor fills every box with the empty state.
   */
  public SudokuPuzzle()
  {
    this.boxes = new String[puzzleWidth][puzzleWidth];
    for (String[] row : boxes) {
      for (int i = 0; i < row.length; i++) {
        row[i] = " ";
      }
    }
  }

  /** The constructor providing an array of box values.
   *
   *  @param inputBoxes An array of string arrays that must be of length
   *                    puzzleWidth (9 for normal Sudoku) with each subarray
   *                    of length 9, or an IllegalArgumentException will be
   *                    thrown. Each value is tested to ensure that it's one
   *                    of the allowed values.
   */
  public SudokuPuzzle(String[][] inputBoxes)
  {
    if (inputBoxes.length != puzzleWidth) {
      String msg = "Input height is " + inputBoxes.length + " but must be " +
                   puzzleWidth + ".";
      throw new IllegalArgumentException(msg);
    }

    for (int i = 0; i < inputBoxes.length; i++) {
      String[] row = inputBoxes[i];
      if (row.length != puzzleWidth) {
        String msg = "Found row of length " + row.length + " but must be " +
                     puzzleWidth + ".";
        throw new IllegalArgumentException(msg);
      }

      for (String boxValue : row) {
        if (!Arrays.asList(allowedValues).contains(boxValue)) {
          String msg = "Unrecognized value " + boxValue + ".";
        }
      }
    }

    this.boxes = inputBoxes;
  }

  /** Copy constructor.
   *
   *  The state of the other puzzle is copied by value, not by reference.
   *
   *  @param other Another puzzle to copy.
   */
  public SudokuPuzzle(SudokuPuzzle other)
  {
    this.boxes = new String[puzzleWidth][puzzleWidth];

    for (int i = 0; i < puzzleWidth; i++) {
      System.arraycopy(other.boxes[i], 0, this.boxes[i], 0, puzzleWidth);
    }
  }

  /////////////////////////////////////////////////////////////////
  // Public Methods
  /////////////////////////////////////////////////////////////////  
  
  /** Returns a representation of the puzzle suitable for printing to the
   *  command line.
   *
   *  For example, one output might be
   *  9   3 1
   * 2 8  73
   * 4     58
   *   45 8
   *  8 9 1 7
   *    7 24
   *  46     2
   *   96  8 1
   *  2 3   5
   *
   * or
   * 697853214
   * 258417369
   * 413296587
   * 974568123
   * 382941675
   * 165732498
   * 546189732
   * 739625841
   * 821374956
   */
  public String toString()
  {
    StringBuilder output = new StringBuilder();
  
    for (String[] row : this.boxes) {
      for (String boxValue : row) {
        output.append(boxValue);
      }
      output.append("\n");
    }

    return output.toString();
  }

  /** Reads, from a provided input stream, text that is formatted according to
   *  the output from toString(), and returns the SudokuPuzzle equivalent.
   */
  public static SudokuPuzzle read(InputStream inputStream)
  {
    BufferedReader reader =
      new BufferedReader(new InputStreamReader(inputStream));

    String[][] boxes = new String[puzzleWidth][puzzleWidth];
    
    for (int i = 0; i < boxes.length; i++) {
      String line;
      try {
        line = reader.readLine();
      } catch (java.io.IOException ex) {
        throw new RuntimeException(ex);
      }

      if (line == null) {
        String msg = "Insufficient number of lines.";
        throw new RuntimeException(msg);
      }

      // Be generous and pad insufficently long lines with the empty string,
      // so that users don't need to carefully count number of spaces at the
      // end of a line.
      while (line.length() < puzzleWidth) {
        line = line + " ";
      }

      if (line.length() != puzzleWidth) {
        String msg = "Wrong length. Must be " + puzzleWidth + " but was " +
                     line.length();
        throw new RuntimeException(msg);
      }

      for (int j = 0; j < puzzleWidth; j++) {
        boxes[i][j] = line.substring(j, j + 1);
      }

    }

    // Lines after the first puzzleWidth number of lines are ignored.    

    return new SudokuPuzzle(boxes);
  }

  /** Returns a solved version of this puzzle, or null if this puzzle is
   *  unsolvable.
   *
   *  This object will not be modified.
   *
   *  This solves the puzzle recursively, with the following algorithm:
   *
   *  1) Iterate through all the boxes and find the first unset box that has
   *     only one possible value. If you find any such box, set it to its
   *     possible value and repeat step 1. This is a speed optimization step to
   *     decrease the amount of branching that the recursion must perform.
   *  2) Find the first unset box. List all its possible values. For each
   *     possible value, create a subpuzzle where that box has that value,
   *     and attempt to solve the subpuzzle. If the subpuzzle has a solution,
   *     that is the solution to this puzzle.
   *  3) If none of the subpuzzles had a solution, this puzzle has no solution.
   */
  public SudokuPuzzle solve()
  {
    // Create a copy of the puzzle to mutate so that this object is unchanged.
    SudokuPuzzle copy = new SudokuPuzzle(this);
    // First iterate through all the boxes and find any that have only one
    // possible value.
    // Loop as long as we are still setting boxes that have only one possible
    // value.
    boolean setABox;
    do {
      setABox = copy.setSingleValueBox();
    } while (setABox);

    if (copy.isSolved()) {
      return copy;
    } 

    // Find the first unset box.
    int firstI = -1;
    int firstJ = -1;
    int i = 0;
    while (i < puzzleWidth && firstI < 0) {
      int j = 0;    
      while (j < puzzleWidth && firstJ < 0) {
        if (copy.boxes[i][j].equals(" ")) {
          firstI = i;
          firstJ = j;
        }
        j++;
      }
      i++;
    }

    List<String> possibleValues = copy.calculatePossibleValues(firstI, firstJ);

    // For each possible value this box could take on, hypothesize that the
    // box takes on that value and attempt to solve that subpuzzle. If any
    // subpuzzle has a solution, that is the solution to this puzzle.
    for (String possibleValue : possibleValues) {
      SudokuPuzzle subPuzzle = new SudokuPuzzle(copy);
      subPuzzle.boxes[firstI][firstJ] = possibleValue;

      SudokuPuzzle result = subPuzzle.solve();

      if (result != null) {
        return result;
      }
    }

    // If none of the hypothesized subpuzzles have a solution, this puzzle has
    // no solution.
    return null;
    
  }

  /////////////////////////////////////////////////////////////////
  // Main Method
  /////////////////////////////////////////////////////////////////  
  
  public static void main(String[] args)
  {
    InputStream inputStream;

    // Read a sudoku puzzle to be solved from standard input if no arguments
    // were given, or using the first provided argument as a file name, if
    // any arguments were given.
    if (args.length == 0) {
      inputStream = System.in;
    } else {
      String fileName = args[0];
      try {
        inputStream = new FileInputStream(fileName);
      } catch (java.io.FileNotFoundException ex) {
        String msg = "No such file " + fileName + ".";
        throw new RuntimeException(msg, ex);
      }
    }
    SudokuPuzzle puzzle = SudokuPuzzle.read(inputStream);

    System.out.println("The solution is:");
    SudokuPuzzle solution = puzzle.solve();
    if (solution == null) {
      System.out.println("No solution");
    } else {
      System.out.println(solution.toString());
    }
  }

  /////////////////////////////////////////////////////////////////
  // Private Methods
  /////////////////////////////////////////////////////////////////  
  
  /** Sets the first box that has only one possible value.
   *
   *  Returns true if any box was changed, false if all the boxes were examined
   *  but had more than one possible value.
   */
  private boolean setSingleValueBox()
  {
    for (int i = 0; i < puzzleWidth; i++) {
      for (int j = 0; j < puzzleWidth; j++) {
        if (boxes[i][j].equals(" ")) {
          List<String> possibleValues = calculatePossibleValues(i, j);
          if (possibleValues.size() == 1) {
            boxes[i][j] = possibleValues.get(0);
            return true;
          }
        }
      }
    }

    return false;
  }

  /** Calculate all the possible values that a particular box could take on.
   */
  private List<String> calculatePossibleValues(int i, int j)
  {
    if (!(boxes[i][j].equals(" "))) {
      return Arrays.asList(boxes[i][j]);
    }
  
    List<String> output = new ArrayList<String>();
    output.addAll(Arrays.asList(allowedValues));
    output.remove(" ");

    for (int k = 0; k < puzzleWidth; k++) {
      output.remove(boxes[k][j]);
      output.remove(boxes[i][k]);
    }

    // Integer division truncates towards 0.
    int iBoxNumber = i / boxWidth;
    int jBoxNumber = j / boxWidth;

    for (int a = iBoxNumber * boxWidth;
         a < iBoxNumber * boxWidth + boxWidth;
         a++) {
      for (int b = jBoxNumber * boxWidth;
           b < jBoxNumber * boxWidth + boxWidth;
           b++) {
      output.remove(boxes[a][b]);
      }
    }

    return output;
  }

  /** Determine whether this puzzle is solved; that is, whether all squares
   *  are filled in.
   */
  private boolean isSolved()
  {
    for (String[] row : boxes) {
      for (String boxValue : row) {
        if (boxValue.equals(" ")) {
          return false;
        }
      }
    }

    return true;
  }

}