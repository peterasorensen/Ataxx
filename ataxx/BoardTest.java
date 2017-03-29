package ataxx;

import org.junit.Test;
import static org.junit.Assert.*;

/** Tests of the Board class.
 *  @author Peter Sorensen
 */
public class BoardTest {

    private static final String[]
        GAME1 = { "a7-b7", "a1-a2",
                  "a7-a6", "a2-a3",
                  "a6-a5", "a3-a4" };

    private static final String[]
            GAME2 = { "a7-a6", "a1-a2",
                      "a7-a5", "a2-a3",
                      "a5-b3", "a1-b2" };

    private static void makeMoves(Board b, String[] moves) {
        for (String s : moves) {
            b.makeMove(s.charAt(0), s.charAt(1),
                       s.charAt(3), s.charAt(4));
        }
    }

    @Test public void testUndo() {
        Board b0 = new Board();
        Board b1 = new Board(b0);
        makeMoves(b0, GAME1);
        Board b2 = new Board(b0);
        for (int i = 0; i < GAME1.length; i += 1) {
            b0.undo();
        }
        assertEquals("failed to return to start", b1, b0);
        makeMoves(b0, GAME1);
        assertEquals("second pass failed to reach same position", b2, b0);
    }

    @Test public void testExtend() {
        Board b0 = new Board();
        makeMoves(b0, GAME1);
        assertEquals(b0.get(68), PieceColor.BLUE);
        assertEquals(b0.get(57), PieceColor.BLUE);
        assertEquals(b0.get(46), PieceColor.BLUE);
        assertEquals(b0.get(35), PieceColor.BLUE);
        assertEquals(b0.get(24), PieceColor.BLUE);
        assertEquals(b0.get(96), PieceColor.BLUE);
        assertEquals(b0.get(79), PieceColor.RED);
        assertEquals(b0.get(91), PieceColor.RED);
        assertEquals(b0.get(30), PieceColor.RED);
        assertEquals(b0.get(90), PieceColor.RED);
        assertEquals(b0.bluePieces(), 6);
        assertEquals(b0.redPieces(), 4);
    }

    @Test public void testJump() {
        Board b0 = new Board();
        makeMoves(b0, GAME2);
        assertEquals(b0.get(79), PieceColor.RED);
        assertEquals(b0.get(24), PieceColor.BLUE);
        assertEquals(b0.get(35), PieceColor.BLUE);
        assertEquals(b0.get(46), PieceColor.BLUE);
        assertEquals(b0.get(47), PieceColor.BLUE);
        assertEquals(b0.get(36), PieceColor.BLUE);
        assertEquals(b0.bluePieces(), 6);
        assertEquals(b0.redPieces(), 2);
    }

    @Test public void testLegalblock() {
        Board b0 = new Board();
        makeMoves(b0, GAME1);
        assertEquals(b0.legalBlock('a', '4'), false);
        assertEquals(b0.legalBlock('d', '4'), true);
        assertEquals(b0.legalBlock('a', '1'), false);
        assertEquals(b0.legalBlock('b', '2'), true);
        assertEquals(b0.legalBlock('b', '7'), false);
    }

}
