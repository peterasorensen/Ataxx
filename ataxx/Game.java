package ataxx;

/* Author: P. N. Hilfinger */

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.function.Consumer;

import static ataxx.PieceColor.*;
import static ataxx.Game.State.*;
import static ataxx.Command.Type.*;
import static ataxx.GameException.error;

/** Controls the play of the game.
 *  @author Peter Sorensen
 */
class Game {

    /** States of play. */
    static enum State {
        SETUP, PLAYING, FINISHED;
    }

    /** A new Game, using BOARD to play on, reading initially from
     *  BASESOURCE and using REPORTER for error and informational messages. */
    Game(Board board, CommandSource baseSource, Reporter reporter) {
        _inputs.addSource(baseSource);
        _board = board;
        _reporter = reporter;
    }

    /** Run a session of Ataxx gaming.  Use an AtaxxGUI iff USEGUI. */
    void process(boolean useGUI) {
        Player red, blue;
        GameLoop:
        while (true) {
            doClear(null);
            SetupLoop:
            while (_state == SETUP) {
                doCommand();
            }
            setState(PLAYING);
            if (_blueManual) {
                blue = new Manual(this, BLUE);
            } else {
                blue = new AI(this, BLUE);
            }
            if (_redManual) {
                red = new Manual(this, RED);
            } else {
                red = new AI(this, RED);
            }

            while (_state != SETUP && !_board.gameOver()) {
                Move move;
                if (board().whoseMove() == RED) {
                    move = red.myMove();
                } else {
                    move = blue.myMove();
                }
                if (move == null) {
                    continue;
                }
                if (move.isPass() && !board().canMove(board().whoseMove())) {
                    System.out.println(board().whoseMove() + " passes.");
                    board().pass();
                    continue;
                }
                if (!board().legalMove(move)) {
                    reportError("that move is illegal.", move);
                    continue;
                }
                if (_state == PLAYING) {
                    _board.makeMove(move);
                }
            }

            if (_state != SETUP) {
                reportWinner();
            }

            if (_state == PLAYING) {
                _state = FINISHED;
            }

            while (_state == FINISHED) {
                doCommand();
            }
        }

    }

    /** Return a view of my game board that should not be modified by
     *  the caller. */
    Board board() {
        return _board;
    }

    /** Perform the next command from our input source. */
    void doCommand() {
        try {
            Command cmnd =
                Command.parseCommand(_inputs.getLine("ataxx: "));
            _commands.get(cmnd.commandType()).accept(cmnd.operands());
        } catch (GameException excp) {
            _reporter.errMsg(excp.getMessage());
        }
    }

    /** Read and execute commands until encountering a move or until
     *  the game leaves playing state due to one of the commands. Return
     *  the terminating move command, or null if the game first drops out
     *  of playing mode. If appropriate to the current input source, use
     *  PROMPT to prompt for input. */
    Command getMoveCmnd(String prompt) {
        while (_state == PLAYING) {
            try {
                Command cmnd = Command.parseCommand(_inputs.getLine(prompt));
                if (cmnd.commandType() == PIECEMOVE || cmnd.commandType()
                        == PASS) {
                    return cmnd;
                } else {
                    _commands.get(cmnd.commandType()).accept(cmnd.operands());
                }
            } catch (GameException excp) {
                _reporter.errMsg(excp.getMessage());
            }
        }
        return null;
    }

    /** Return random integer between 0 (inclusive) and MAX>0 (exclusive). */
    int nextRandom(int max) {
        return _randoms.nextInt(max);
    }

    /** Report a move, using a message formed from FORMAT and ARGS as
     *  for String.format. */
    void reportMove(String format, Object... args) {
        _reporter.moveMsg(format, args);
    }

    /** Report an error, using a message formed from FORMAT and ARGS as
     *  for String.format. */
    void reportError(String format, Object... args) {
        _reporter.errMsg(format, args);
    }

    /* Command Processors */

    /** Perform the command 'auto OPERANDS[0]'. */
    void doAuto(String[] operands) {
        if (operands[0].equalsIgnoreCase("blue")) {
            _blueManual = false;
        } else if (operands[0].equalsIgnoreCase("red")) {
            _redManual = false;
        }
    }

    /** Perform a 'help' command. */
    void doHelp(String[] unused) {
        InputStream helpIn =
            Game.class.getClassLoader().getResourceAsStream("ataxx/help.txt");
        if (helpIn == null) {
            System.err.println("No help available.");
        } else {
            try {
                BufferedReader r
                    = new BufferedReader(new InputStreamReader(helpIn));
                while (true) {
                    String line = r.readLine();
                    if (line == null) {
                        break;
                    }
                    System.out.println(line);
                }
                r.close();
            } catch (IOException e) {
                /* Ignore IOException */
            }
        }
    }

    /** Perform the command 'load OPERANDS[0]'. */
    void doLoad(String[] operands) {
        try {
            FileReader reader = new FileReader(operands[0]);
            ReaderSource read = new ReaderSource(reader, true);
            _inputs.addSource(read);
        } catch (IOException e) {
            throw error("Cannot open file %s", operands[0]);
        }
    }

    /** Perform the command 'manual OPERANDS[0]'. */
    void doManual(String[] operands) {
        if (operands[0].equalsIgnoreCase("blue")) {
            _blueManual = true;
        } else if (operands[0].equalsIgnoreCase("red")) {
            _redManual = true;
        }
    }

    /** Exit the program. */
    void doQuit(String[] unused) {
        System.exit(0);
    }

    /** Perform the command 'start'. */
    void doStart(String[] unused) {
        checkState("start", SETUP);
        setState(PLAYING);
    }

    /** Perform the move OPERANDS[0]. */
    void doMove(String[] operands) {
        if (_board.gameOver()) {
            setState(FINISHED);
            return;
        }
        Move what = Move.move(operands[0].charAt(0), operands[1].charAt(0),
                operands[2].charAt(0), operands[3].charAt(0));
        if (what == null || !_board.legalMove(what)) {
            reportError("Illegal move.");
            return;
        }
        _board.makeMove(what);
    }

    /** Cause current player to pass. */
    void doPass(String[] unused) {
        if (!_board.canMove(_board.whoseMove())) {
            System.out.println(_board.whoseMove() + " passes.");
            _board.pass();
        } else {
            reportError("Player can move, so may not pass.", _board);
        }
    }

    /** Perform the command 'clear'. */
    void doClear(String[] unused) {
        _board = new Board();
        setState(SETUP);
    }

    /** Perform the command 'dump'. */
    void doDump(String[] unused) {
        System.out.println(_board.toString());
    }

    /** Execute 'seed OPERANDS[0]' command, where the operand is a string
     *  of decimal digits. Silently substitutes another value if
     *  too large. */
    void doSeed(String[] operands) {
        return;
    }

    /** Execute the command 'block OPERANDS[0]'. */
    void doBlock(String[] operands) {
        if (_state == SETUP) {
            _board.setBlock(operands[0].charAt(0), operands[0].charAt(1));
        } else {
            reportError("'block' command is not allowed now.");
        }
    }

    /** Execute the artificial 'error' command. */
    void doError(String[] unused) {
        throw error("Command not understood");
    }

    /** Report the outcome of the current game. */
    void reportWinner() {
        String msg;
        if (_board.numPieces(BLUE) == _board.numPieces(RED)) {
            msg = "Draw.";
        } else if (_board.numPieces(BLUE) < _board.numPieces(RED)) {
            msg = "Red wins.";
        } else {
            msg = "Blue wins.";
        }
        _reporter.outcomeMsg(msg);
    }

    /** Check that game is currently in one of the states STATES, assuming
     *  CMND is the command to be executed. */
    private void checkState(Command cmnd, State... states) {
        for (State s : states) {
            if (s == _state) {
                return;
            }
        }
        throw error("'%s' command is not allowed now.", cmnd.commandType());
    }

    /** Check that game is currently in one of the states STATES, using
     *  CMND in error messages as the name of the command to be executed. */
    private void checkState(String cmnd, State... states) {
        for (State s : states) {
            if (s == _state) {
                return;
            }
        }
        throw error("'%s' command is not allowed now.", cmnd);
    }

    /** Sets _state to ZERS. */
    void setState(State zers) {
        _state = zers;
    }

    /** Mapping of command types to methods that process them. */
    private final HashMap<Command.Type, Consumer<String[]>> _commands =
        new HashMap<>();

    {
        _commands.put(AUTO, this::doAuto);
        _commands.put(BLOCK, this::doBlock);
        _commands.put(CLEAR, this::doClear);
        _commands.put(DUMP, this::doDump);
        _commands.put(HELP, this::doHelp);
        _commands.put(MANUAL, this::doManual);
        _commands.put(PASS, this::doPass);
        _commands.put(PIECEMOVE, this::doMove);
        _commands.put(SEED, this::doSeed);
        _commands.put(START, this::doStart);
        _commands.put(LOAD, this::doLoad);
        _commands.put(QUIT, this::doQuit);
        _commands.put(ERROR, this::doError);
        _commands.put(EOF, this::doQuit);
    }

    /** Input source. */
    private final CommandSources _inputs = new CommandSources();

    /** My board. */
    private Board _board;
    /** Current game state. */
    private State _state = SETUP;
    /** Used to send messages to the user. */
    private Reporter _reporter;
    /** Source of pseudo-random numbers (used by AIs). */
    private Random _randoms = new Random();

    /** Stores whether Red is a manual or auto player. */
    private boolean _redManual = true;

    /** Stores whether Blue is a manual or auto player. */
    private boolean _blueManual = false;
}
