package ataxx;

/* Author: P. N. Hilfinger, (C) 2008. */


import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Formatter;
import java.util.Observable;

import static ataxx.PieceColor.*;
import static ataxx.GameException.error;

/** An Ataxx board.   The squares are labeled by column (a char value between
 *  'a' - 2 and 'g' + 2) and row (a char value between '1' - 2 and '7'
 *  + 2) or by linearized index, an integer described below.  Values of
 *  the column outside 'a' and 'g' and of the row outside '1' to '7' denote
 *  two layers of border squares, which are always blocked.
 *  This artificial border (which is never actually printed) is a common
 *  trick that allows one to avoid testing for edge conditions.
 *  For example, to look at all the possible moves from a square, sq,
 *  on the normal board (i.e., not in the border region), one can simply
 *  look at all squares within two rows and columns of sq without worrying
 *  about going off the board. Since squares in the border region are
 *  blocked, the normal logic that prevents moving to a blocked square
 *  will apply.
 *
 *  For some purposes, it is useful to refer to squares using a single
 *  integer, which we call its "linearized index".  This is simply the
 *  number of the square in row-major order (counting from 0).
 *
 *  Moves on this board are denoted by Moves.
 *  @author Peter Sorensen
 */
class Board extends Observable {

    /** Number of squares on a side of the board. */
    static final int SIDE = 7;
    /** Length of a side + an artificial 2-deep border region. */
    static final int EXTENDED_SIDE = SIDE + 4;

    /** Number of non-extending moves before game ends. */
    static final int JUMP_LIMIT = 25;

    /** A new, cleared board at the start of the game. */
    Board() {
        _board = new PieceColor[EXTENDED_SIDE * EXTENDED_SIDE];
        clear();
    }

    /** A copy of B. */
    Board(Board b) {
        _board = b._board.clone();
        _whoseMove = b.whoseMove();
        _numJumps = b._numJumps;
        _numBlue = b._numBlue;
        _numRed = b._numRed;
        _numMoves = b._numMoves;
    }

    /** Return the linearized index of square COL ROW. */
    static int index(char col, char row) {
        return (row - '1' + 2) * EXTENDED_SIDE + (col - 'a' + 2);
    }

    /** Return the linearized index of the square that is DC columns and DR
     *  rows away from the square with index SQ. */
    static int neighbor(int sq, int dc, int dr) {
        return sq + dc + dr * EXTENDED_SIDE;
    }

    /** Convert and return sq number LINEAR to a row number. */
    char linearToR(int linear) {
        return Integer.toString((linear / EXTENDED_SIDE) - 1).charAt(0);
    }

    /** Convert and return sq number LINEAR to a column letter. */
    char linearToC(int linear) {
        return "abcdefg".charAt((linear % EXTENDED_SIDE) - 2);
    }

    /** Clear me to my starting state, with pieces in their initial
     *  positions and no blocks. */
    void clear() {
        _whoseMove = RED;
        _numMoves = 0;
        while (!_moves.empty()) {
            _moves.pop();
        }
        while (!_changedIndices.empty()) {
            _changedIndices.pop();
        }
        for (int i = 0; i < EXTENDED_SIDE * EXTENDED_SIDE; i++) {
            if (i < EXTENDED_SIDE + EXTENDED_SIDE || i > (EXTENDED_SIDE
                    * EXTENDED_SIDE) - EXTENDED_SIDE - EXTENDED_SIDE) {
                set(i, BLOCKED);
            } else if (i % 11 == 0 || i % 11 == 1 || i % 11 == 9
                    || i % 11 == 10) {
                set(i, BLOCKED);
            } else if (i == index('a', '7') || i == index('g', '1')) {
                set(i, RED);
            } else if (i == index('a', '1') || i == index('g', '7')) {
                set(i, BLUE);
            } else {
                set(i, EMPTY);
            }
        }
        setChanged();
        notifyObservers();
    }

    /** Return true iff the game is over: i.e., if neither side has
     *  any moves, if one side has no pieces, or if there have been
     *  MAX_JUMPS consecutive jumps without intervening extends. */
    boolean gameOver() {
        if (_numJumps >= JUMP_LIMIT) {
            return true;
        } else if (redPieces() == 0 || bluePieces() == 0) {
            return true;
        } else if (!canMove(BLUE) && !canMove(RED)) {
            return true;
        } else if (redPieces() + bluePieces() == SIDE * SIDE) {
            return true;
        }
        return false;
    }

    /** Return number of red pieces on the board. */
    int redPieces() {
        return numPieces(RED);
    }

    /** Return number of blue pieces on the board. */
    int bluePieces() {
        return numPieces(BLUE);
    }

    /** Return number of COLOR pieces on the board. */
    int numPieces(PieceColor color) {
        if (color == RED) {
            return _numRed;
        } else if (color == BLUE) {
            return _numBlue;
        } else if (color == BLOCKED) {
            return _numBlocked;
        } else {
            return (EXTENDED_SIDE * EXTENDED_SIDE)
                    - _numRed - _numBlue - _numBlocked;
        }
    }

    /** Increment numPieces(COLOR) by K. */
    private void incrPieces(PieceColor color, int k) {
        if (color == RED) {
            _numRed += k;
        } else if (color == BLUE) {
            _numBlue += k;
        } else {
            _numBlocked += k;
        }
    }

    /** The current contents of square CR, where 'a'-2 <= C <= 'g'+2, and
     *  '1'-2 <= R <= '7'+2.  Squares outside the range a1-g7 are all
     *  BLOCKED.  Returns the same value as get(index(C, R)). */
    PieceColor get(char c, char r) {
        return _board[index(c, r)];
    }

    /** Return the current contents of square with linearized index SQ. */
    PieceColor get(int sq) {
        return _board[sq];
    }

    /** Set get(C, R) to V, where 'a' <= C <= 'g', and
     *  '1' <= R <= '7'. */
    private void set(char c, char r, PieceColor v) {
        set(index(c, r), v);
    }

    /** Set square with linearized index SQ to V. This operation is
     *  undoable. */
    private void set(int sq, PieceColor v) {
        _board[sq] = v;
    }

    /** Set square at C R to V (not undoable). */
    private void unrecordedSet(char c, char r, PieceColor v) {
        _board[index(c, r)] = v;
    }

    /** Set square at linearized index SQ to V (not undoable). */
    private void unrecordedSet(int sq, PieceColor v) {
        _board[sq] = v;
    }

    /** Return true iff MOVE is legal on the current board. */
    boolean legalMove(Move move) {
        return (((move.isExtend() || move.isJump())
                && get(move.fromIndex()) == whoseMove()
                && get(move.toIndex()) == EMPTY)
                || (move.isPass() && !canMove(whoseMove())));
    }

    /** Return true iff player WHO can move, ignoring whether it is
     *  that player's move and whether the game is over. */
    boolean canMove(PieceColor who) {
        ArrayList<Integer> pieces = new ArrayList<Integer>();
        for (char i = '7'; i >= '1'; i--) {
            for (char j = 'a'; j <= 'g'; j++) {
                if (get(j, i) == who) {
                    pieces.add(index(j, i));
                }
            }
            for (int k : pieces) {
                for (int j : extendSpots()) {
                    if (get(k + j) == EMPTY) {
                        return true;
                    }
                }
                for (int m : jumpSpots()) {
                    if (get(k + m) == EMPTY) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Return the color of the player who has the next move.  The
     *  value is arbitrary if gameOver(). */
    PieceColor whoseMove() {
        return _whoseMove;
    }

    /** Return total number of moves and passes since the last
     *  clear or the creation of the board. */
    int numMoves() {
        return _numMoves;
    }

    /** Return number of non-pass moves made in the current game since the
     *  last extend move added a piece to the board (or since the
     *  start of the game). Used to detect end-of-game. */
    int numJumps() {
        return _numJumps;
    }

    /** Perform the move C0R0-C1R1, or pass if C0 is '-'.  For moves
     *  other than pass, assumes that legalMove(C0, R0, C1, R1). */
    void makeMove(char c0, char r0, char c1, char r1) {
        if (c0 == '-') {
            makeMove(Move.pass());
        } else {
            makeMove(Move.move(c0, r0, c1, r1));
        }
    }

    /** Make the MOVE on this Board, assuming it is legal. */
    void makeMove(Move move) {
        assert legalMove(move);
        if (move.isPass()) {
            pass();
            _moves.push(null);
            _changedIndices.push(new ArrayList<Integer>());
            return;
        }
        int to = move.toIndex();
        int increment = 0;
        ArrayList<Integer> indices = new ArrayList<>();
        if (move.isExtend() || move.isJump()) {
            set(to, whoseMove());
            for (int n : extendSpots()) {
                if (get(to + n) == whoseMove().opposite()) {
                    indices.add(to + n);
                    set(to + n, whoseMove());
                    increment++;
                }
            }
            if (move.isJump()) {
                incrPieces(whoseMove(), increment);
                set(move.fromIndex(), EMPTY);
                _numJumps++;
                _numMoves++;
            } else {
                incrPieces(whoseMove(), increment + 1);
                _pastJumps.push(_numJumps);
                _numJumps = 0;
                _numMoves++;
            }
            incrPieces(whoseMove().opposite(), -increment);
            _moves.push(move);
            _changedIndices.push(indices);
        }
        PieceColor opponent = _whoseMove.opposite();
        _whoseMove = opponent;
        setChanged();
        notifyObservers();
    }

    /** Update to indicate that the current player passes, assuming it
     *  is legal to do so.  The only effect is to change whoseMove(). */
    void pass() {
        assert !canMove(_whoseMove);
        _whoseMove = whoseMove().opposite();
        setChanged();
        notifyObservers();
    }

    /** Undo the last move. */
    void undo() {
        _numMoves--;
        Object conv = _moves.pop();
        Move last = (Move) conv;
        _whoseMove = whoseMove().opposite();
        if (last == null) {
            return;
        }
        set(last.toIndex(), EMPTY);
        if (last.isJump()) {
            _numJumps--;
            set(last.fromIndex(), whoseMove());
        } else {
            incrPieces(whoseMove(), -1);
            _numJumps = _pastJumps.pop();
        }

        ArrayList<Integer> revert = _changedIndices.pop();
        for (int each : _extendSpots) {
            if (revert.contains(last.toIndex() + each)) {
                set(last.toIndex() + each, whoseMove().opposite());
                incrPieces(whoseMove(), -1);
                incrPieces(whoseMove().opposite(), 1);
            } else if (last.toIndex() + each == last.fromIndex()) {
                continue;
            }
        }
        setChanged();
        notifyObservers();
    }

    /** Return true iff it is legal to place a block at C R. */
    boolean legalBlock(char c, char r) {
        char index1 = 'g' + 1;
        index1 += -c + 'a' - 1;
        char index2 = '7' + 1;
        index2 += -r + '1' - 1;
        if (get(index1, r) == EMPTY && get(c, index2) == EMPTY
                && get(index1, index2) == EMPTY && get(c, r) == EMPTY) {
            return true;
        }
        return false;
    }

    /** Return true iff it is legal to place a block at CR. */
    boolean legalBlock(String cr) {
        return legalBlock(cr.charAt(0), cr.charAt(1));
    }

    /** Set a block on the square C R and its reflections across the middle
     *  row and/or column, if that square is unoccupied and not
     *  in one of the corners. Has no effect if any of the squares is
     *  already occupied by a block.  It is an error to place a block on a
     *  piece. */
    void setBlock(char c, char r) {
        if (!legalBlock(c, r)) {
            throw error("illegal block placement");
        }
        char index1 = 'g' + 1;
        index1 += -c + 'a' - 1;
        char index2 = '7' + 1;
        index2 += -r + '1' - 1;
        set(c, r, BLOCKED);
        set(index1, r, BLOCKED);
        set(c, index2, BLOCKED);
        set(index1, index2, BLOCKED);
        incrPieces(BLOCKED, 1);
        setChanged();
        notifyObservers();
    }

    /** Place a block at CR. */
    void setBlock(String cr) {
        setBlock(cr.charAt(0), cr.charAt(1));
    }

    /** Return numblocks. */
    int numBlocks() {
        return _numBlocked;
    }

    /** Return a list of all moves made since the last clear (or start of
     *  game). */
    List<Move> allMoves() {
        return _moves;
    }

    /** Return array of extend spots. */
    int[] extendSpots() {
        return _extendSpots;
    }

    /** Return array of jump spots. */
    int[] jumpSpots() {
        return _jumpSpots;
    }

    /** Return array of both extend and jump spots. */
    int[] bothSpots() {
        return _bothSpots;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    /* .equals used only for testing purposes. */
    @Override
    public boolean equals(Object obj) {
        Board other = (Board) obj;
        return Arrays.equals(_board, other._board);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(_board);
    }

    /** Return a text depiction of the board (not a dump).  If LEGEND,
     *  supply row and column numbers around the edges. */
    String toString(boolean legend) {
        Formatter out = new Formatter();
        out.format("===");
        for (char i = '7'; i >= '1'; i--) {
            out.format("%n ");
            for (char j = 'a'; j <= 'g'; j++) {
                if (get(j, i) == RED) {
                    out.format(" r");
                } else if (get(j, i) == BLUE) {
                    out.format(" b");
                } else if (get(j, i) == BLOCKED) {
                    out.format(" X");
                } else {
                    out.format(" -");
                }
            }
        }
        out.format("%n===");
        return out.toString();
    }

    /** For reasons of efficiency in copying the board,
     *  we use a 1D array to represent it, using the usual access
     *  algorithm: row r, column c => index(r, c).
     *
     *  Next, instead of using a 7x7 board, we use an 11x11 board in
     *  which the outer two rows and columns are blocks, and
     *  row 2, column 2 actually represents row 0, column 0
     *  of the real board.  As a result of this trick, there is no
     *  need to special-case being near the edge: we don't move
     *  off the edge because it looks blocked.
     *
     *  Using characters as indices, it follows that if 'a' <= c <= 'g'
     *  and '1' <= r <= '7', then row c, column r of the board corresponds
     *  to board[(c -'a' + 2) + 11 (r - '1' + 2) ], or by a little
     *  re-grouping of terms, board[c + 11 * r + SQUARE_CORRECTION]. */
    private final PieceColor[] _board;

    /** Player that is on move. */
    private PieceColor _whoseMove;

    /** Stack of past moves made. */
    private Stack<Move> _moves = new Stack<>();

    /** Stack of indices that need reversing in undo. */
    private Stack<ArrayList<Integer>> _changedIndices = new Stack<>();

    /** Stack of numJumps before extend move. */
    private Stack<Integer> _pastJumps = new Stack<>();

    /** List of linearized index distances extends away from index 0. */
    private final int[] _extendSpots = new int[]{10, 11, 12, -1
            , 1, -12, -11, -10};

    /** List of linearized index distances jumps away from index 0. */
    private final int[] _jumpSpots = new int[]{20, 21, 22, 23
            , 24, 9, 13, -2, 2, -13, -9, -24, -23, -22, -21, -20};

    /** Combination of jump spots and extend spots. */
    private final int[] _bothSpots = new int[]{10, 11, 12, -1
            , 1, -12, -11, -10
            , 20, 21, 22, 23, 24
            , 9, 13, -2, 2, -13, -9, -24, -23, -22, -21, -20};

    /** Number of moves. */
    private int _numMoves = 0;

    /** Number of jumps. */
    private int _numJumps = 0;

    /** Number of reds. */
    private int _numRed = 2;

    /** Number of blues. */
    private int _numBlue = 2;

    /** Number of blocks. */
    private int _numBlocked = 0;
}
