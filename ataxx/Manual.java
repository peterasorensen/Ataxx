package ataxx;

/** A Player that receives its moves from its Game's getMoveCmnd method.
 *  @author Peter Sorensen
 */
class Manual extends Player {

    /** A Player that will play MYCOLOR on GAME, taking its moves from
     *  GAME. */
    Manual(Game game, PieceColor myColor) {
        super(game, myColor);
    }

    @Override
    Move myMove() {
        Command var = game().getMoveCmnd(myColor() + ": ");
        if (var == null) {
            return null;
        }
        if (var.commandType() == Command.Type.PASS) {
            return Move.pass();
        }
        return Move.move(var.operands()[0].charAt(0),
                var.operands()[1].charAt(0),
                var.operands()[2].charAt(0),
                var.operands()[3].charAt(0));
    }
}

