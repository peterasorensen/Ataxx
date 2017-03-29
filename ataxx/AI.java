package ataxx;

import java.util.ArrayList;
import java.util.Random;

import static ataxx.PieceColor.*;

/** A Player that computes its own moves.
 *  @author Peter Sorensen
 */
class AI extends Player {

    /** Maximum minimax search depth before going to static evaluation. */
    private static final int MAX_DEPTH = 4;
    /** A position magnitude indicating a win (for red if positive, blue
     *  if negative). */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 1;
    /** A magnitude greater than a normal value. */
    private static final int INFTY = Integer.MAX_VALUE;

    /** A new AI for GAME that will play MYCOLOR. */
    AI(Game game, PieceColor myColor) {
        super(game, myColor);
    }

    @Override
    Move myMove() {
        if (!board().canMove(myColor())) {
            return Move.pass();
        }
        Move move = findMove();
        System.out.println(myColor() + " moves "
                + move.col0()
                + move.row0()
                + "-" + move.col1()
                + move.row1() + ".");
        return move;
    }

    /** Return a move for me from the current position, assuming there
     *  is a move. */
    private Move findMove() {
        Board b = new Board(board());
        if (myColor() == RED) {
            findMove(b, MAX_DEPTH, true, 1, -INFTY, INFTY);
        } else {
            findMove(b, MAX_DEPTH, true, -1, -INFTY, INFTY);
        }
        return _lastFoundMove;
    }

    /** Used to communicate best moves found by findMove, when asked for. */
    private Move _lastFoundMove;

    /** Find a move from position BOARD and return its value, recording
     *  the move found in _lastFoundMove iff SAVEMOVE. The move
     *  should have maximal value or have value >= BETA if SENSE==1,
     *  and minimal value or value <= ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels before using a static estimate. */
    private int findMove(Board board, int depth, boolean saveMove, int sense,
                         int alpha, int beta) {
        if (saveMove) {
            _lastFoundMove = findMax(board, MAX_DEPTH, alpha, beta);
        }
        return 0;
    }

    /** Finds the best possible move on BOARD looking DEPTH moves ahead
     *  with ALPHA and BETA and returns the move.
     * @param board
     * @param depth
     * @param alpha
     * @param beta
     * @return
     */
    Move findMax(Board board, int depth, int alpha, int beta) {
        if (depth == 0
                || board.SIDE * board.SIDE
                - (board.redPieces() + board.bluePieces()) <= MAX_DEPTH
                || (board.SIDE * board.SIDE
                - (board.redPieces() + board.bluePieces()
                + board.numBlocks()) > 5)) {
            return simpleFindMax(board, alpha, beta);
        }
        ArrayList<Move> ret = new ArrayList<>();
        for (char i = '7'; i >= '1'; i--) {
            for (char j = 'a'; j <= 'g'; j++) {
                if (board.get(j, i) == myColor()) {
                    ArrayList<Integer> moves = new ArrayList<>();
                    for (int dis : board.bothSpots()) {
                        if (board.get(board.index(j, i) + dis) == EMPTY) {
                            moves.add(board.index(j, i) + dis);
                        }
                    }
                    int increase = 1;
                    for (int k = 0; k < moves.size(); k += increase) {
                        Move movin = Move.move(j, i, board.linearToC(
                                moves.get(k))
                                , board.linearToR(moves.get(k)));
                        board.makeMove(movin);
                        if (board.gameOver()) {
                            board.undo();
                            return movin;
                        }
                        setColor(myColor().opposite());
                        Move response = findMax(board, depth - 1, 0, beta);
                        if (board.gameOver()) {
                            setColor(myColor().opposite());
                            return movin;
                        }
                        board.makeMove(response);
                        setColor(myColor().opposite());
                        if (board.numPieces(myColor()) > alpha) {
                            ret.clear();
                            ret.add(movin);
                            alpha = board.numPieces(myColor());
                        } else if (board.numPieces(myColor()) == alpha) {
                            ret.add(movin);
                        }
                        board.undo();
                        board.undo();
                    }
                }
            }
        }
        if (ret.isEmpty()) {
            return Move.PASS;
        }
        Random randomizer = new Random();
        Move random = ret.get(randomizer.nextInt(ret.size()));
        return random;
    }

    /** Simple version of findMax. Returns the best Move
     *  on BOARD when considering a depth of 0, ALPHA, BETA.
     * @param board
     * @param alpha
     * @param beta
     * @return
     */
    Move simpleFindMax(Board board, int alpha, int beta) {
        int currPieces = board.numPieces(myColor());
        ArrayList<Move> ret = new ArrayList<>();
        for (char i = '7'; i >= '1'; i--) {
            for (char j = 'a'; j <= 'g'; j++) {
                if (board.get(j, i) == myColor()) {
                    ArrayList<Integer> moves = new ArrayList<>();
                    for (int dis : board.bothSpots()) {
                        if (board.get(board.index(j, i) + dis) == EMPTY) {
                            moves.add(board.index(j, i) + dis);
                        }
                    }
                    for (Integer each : moves) {
                        Move movin = Move.move(j, i, board.linearToC(each)
                                , board.linearToR(each));
                        board.makeMove(movin);
                        if (board.gameOver()) {
                            return movin;
                        } else if (board.numPieces(myColor()) > alpha) {
                            ret.clear();
                            ret.add(movin);
                            alpha = board.numPieces(myColor());
                        } else if (board.numPieces(myColor()) == alpha) {
                            ret.add(movin);
                        }
                        board.undo();
                    }
                }
            }
        }
        if (ret.isEmpty()) {
            return Move.PASS;
        }
        Random randomizer = new Random();
        Move random = ret.get(randomizer.nextInt(ret.size()));
        return random;
    }

    /** Return a heuristic value for BOARD. */
    private int staticScore(Board board) {
        return 0;
    }
}
