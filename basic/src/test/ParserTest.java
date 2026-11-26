package basic.src.test;

import basic.src.main.compiler.Lexer;
import basic.src.main.compiler.Node;
import basic.src.main.compiler.Parser;
import basic.src.main.exceptions.BadParseException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ParserTest extends CompilerTestBase {

    private static Node.Program parse(String src) {
        Parser p = new Parser();
        p.setTokens(new Lexer().tokenize(src));
        return p.parseProgram();
    }

    private static void assertParses(String src) {
        assertDoesNotThrow(() -> parse(src), "should parse: " + src);
    }

    private static void assertParseFails(String src, String expectedFragment) {
        BadParseException e = assertThrows(BadParseException.class, () -> parse(src),
                "should fail to parse: " + src);
        assertTrue(e.getMessage().contains(expectedFragment),
                "expected message containing \"" + expectedFragment + "\" but got: " + e.getMessage());
    }

    private static String intProgram(String expr) {
        return "DECLARE TYPE int SIZE 4; DECLARE VARIABLE x AS int; x <-" + expr + ";" + "PRINT x;";
    }

    private static void assertInt(int expected, String expr) throws Exception {
        assertBytesEqual(
                bytes(expected >>> 24, (expected >>> 16) & 0xFF, (expected >>> 8) & 0xFF, expected & 0xFF),
                run(intProgram(expr)),
                "expression: " + expr);
    }

    @Test
    void testParsesAssignment() {
        assertParses("x <- 5;");
    }

    @Test
    void testParsesArithmetic() {
        assertParses("x <- a + b * (c - 1) / 2;");
    }

    @Test
    void testParsesFieldAssign() {
        assertParses("x OF p <- a + 1;");
    }

    @Test
    void testParsesStruct() {
        assertParses("DECLARE STRUCT P THEN x AS int; y AS int; END;");
    }

    @Test
    void testParsesNegativeLiteral() {
        assertParses("x <- -5;");
    }

    @Test
    void testParsesComment() {
        assertParses("x <- 5; // this is a comment");
    }

    @Test
    void testSyntaxDoublePlus() {
        assertParseFails("x <- a + + b;", "expected a number, variable");
    }

    @Test
    void testSyntaxLeadingPlus() {
        assertParseFails("x <- + a;", "expected a number, variable");
    }

    @Test
    void testSyntaxMissingOperand() {
        assertParseFails("x <- a + ;", "expected a number, variable");
    }

    @Test
    void testSyntaxAdjacentOperators() {
        assertParseFails("x <- a * / b;", "expected a number, variable");
    }

    @Test
    void testSyntaxUnclosedParen() {
        assertParseFails("x <- (a + b;", "expected ')'");
    }

    @Test
    void testSyntaxExtraCloseParen() {
        assertParseFails("x <- a + b);", "expected ';'");
    }

    @Test
    void testSyntaxEmptyParens() {
        assertParseFails("x <- ();", "expected a number, variable");
    }

    @Test
    void testSyntaxMissingSemicolon() {
        assertParseFails("x <- a + b", "expected ';'");
    }

    @Test
    void testSyntaxLiteralDivByZero() {
        assertParseFails("x <- 5 / 0;", "division by zero");
    }

    @Test
    void testSyntaxLiteralModByZero() {
        assertParseFails("x <- 5 % 0;", "division by zero");
    }

    @Test
    void testSyntaxFoldOverflow() {
        assertParseFails("x <- 9223372036854775807 + 1;", "constant expression out of range");
    }

    @Test
    void testParsesShiftLeft() {
        assertParses("x <- a << 2;");
    }

    @Test
    void testParsesShiftRight() {
        assertParses("x <- a >> b;");
    }

    @Test
    void testParsesBitwiseOps() {
        assertParses("x <- a & b | c ^ d;");
    }

    @Test
    void testParsesBitwisePrecedenceMix() {
        assertParses("x <- a + b << 2 & 3 | 1;");
    }

    @Test
    void testParsesShiftChain() {
        assertParses("x <- a << 1 << 2;");
    }

    @Test
    void testParsesBitwiseParens() {
        assertParses("x <- (a | b) & (c ^ d);");
    }

    @Test
    void testSyntaxShiftCountTooBig() {
        assertParseFails("x <- 1 << 32;", "shift count 32 outside 0..31");
    }

    @Test
    void testSyntaxShiftCountNegative() {
        assertParseFails("x <- 1 << -1;", "shift count -1 outside 0..31");
    }

    @Test
    void testSyntaxShiftFoldOverflow() {
        assertParseFails("x <- 9223372036854775807 << 1;", "constant expression out of range");
    }

    @Test
    void testParsesNot() {
        assertParses("x <- ~a;");
    }

    @Test
    void testParsesDoubleNot() {
        assertParses("x <- ~~a;");
    }

    @Test
    void testParsesNotNegativeLiteral() {
        assertParses("x <- ~-5;");
    }

    @Test
    void testParsesArrayDeclaration() {
        assertParses("DECLARE ARRAY grid AS int LENGTH 10;");
    }

    @Test
    void testParsesArrayAccess() {
        assertParses("grid[0] <- 10;");
    }

    @Test
    void testParsesArrayAccessVariableIndex() {
        assertParses("grid[i] <- 10;");
    }

    @Test
    void testParsesFieldOfArrayElement() {
        assertParses("x OF grid[0] <- 10;");
    }

    @Test
    void testParsesFieldOfArrayElementVariableIndex() {
        assertParses("x OF grid[i] <- 10;");
    }

    @Test
    void testParsesLengthOfArray() {
        assertParses("len <- LENGTH OF grid;");
    }

    @Test
    void testParsesNestedArrayAccess() {
        assertParses("grid[other[i]] <- 10;");
    }

    @Test
    void testParsesArrayAccessWithExpression() {
        assertParses("grid[i + 1] <- 10;");
    }

    @Test
    void testMultiplicativeBindsTighterThanAdditive() throws Exception {
        assertInt(14, "2 + 3 * 4");
    }

    @Test
    void testParenthesesOverridePrecedence() throws Exception {
        assertInt(20, "(2 + 3) * 4");
    }

    @Test
    void testSubtractionIsLeftAssociative() throws Exception {
        assertInt(5, "10 - 3 - 2");
    }

    @Test
    void testDivisionIsLeftAssociative() throws Exception {
        assertInt(10, "100 / 5 / 2");
    }

    @Test
    void testModulo() throws Exception {
        assertInt(2, "17 % 5");
    }

    @Test
    void testShiftBindsLooserThanAdditive() throws Exception {
        assertInt(16, "1 + 1 << 3");
    }

    @Test
    void testShiftIsLeftAssociative() throws Exception {
        assertInt(8, "1 << 1 << 2");
    }

    @Test
    void testAndBindsLooserThanShift() throws Exception {
        assertInt(6, "3 << 1 & 6");
    }

    @Test
    void testAndBindsTighterThanXor() throws Exception {
        assertInt(3, "1 ^ 2 & 3");
    }

    @Test
    void testXorBindsTighterThanOr() throws Exception {
        assertInt(6, "1 ^ 3 | 4");
    }

    @Test
    void testFullPrecedenceChain() throws Exception {
        assertInt(1, "1 + 1 << 2 & 6 | 1");
    }

    @Test
    void testUnaryMinus() throws Exception {
        assertInt(-2, "-5 + 3");
    }

    @Test
    void testBitwiseNot() throws Exception {
        assertInt(255, "~0 & 255");
    }

    @Test
    void testDoubleNotIsIdentity() throws Exception {
        assertInt(42, "~~42");
    }

    @Test
    void testRightShiftIsArithmetic() throws Exception {
        assertInt(-4, "-8 >> 1");
    }

    @Test
    void testBitwiseParens() throws Exception {
        assertInt(2, "(1 | 2) & (3 ^ 1)");
    }

    @Test
    void testCharacterLiteralIsItsCodePoint() throws Exception {
        assertEquals("A", run("""
                DECLARE TYPE byte SIZE 1;
                DECLARE VARIABLE c AS byte;
                c <- 'A';
                PRINT c;
                """));
    }

    @Test
    void testCharacterEscapeSequence() throws Exception {
        assertBytesEqual(bytes(10), run("""
                DECLARE TYPE byte SIZE 1;
                DECLARE VARIABLE c AS byte;
                c <- '\\n';
                PRINT c;
                """));
    }

    @Test
    void testPrintStringLiteral() throws Exception {
        assertEquals("Hello\n", run("""
                PRINT "Hello\\n";
                """));
    }

    @Test
    void testPrintStringEscapes() throws Exception {
        assertEquals("a\tb\0c\"d\\e", run("""
                PRINT "a\\tb\\0c\\"d\\\\e";
                """));
    }

    @Test
    void testPrintStringIsUtf8Encoded() throws Exception {
        assertBytesEqual(bytes(0xE2, 0x82, 0xAC), run("""
                PRINT "€";
                """));
    }

    @Test
    void testMultiplePrintsConcatenate() throws Exception {
        assertEquals("ab", run("""
                PRINT "a";
                PRINT "b";
                """));
    }

    @Test
    void testEmptyProgramProducesNoStatements() {
        assertEquals(0, parse("").statements().size());
    }

    @Test
    void testNewlinesAreNotStatementTerminators() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 9), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                x
                  <-
                  9;
                PRINT x;
                """));
    }

    @Test
    void testMultipleStatementsOnOneLine() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 2), run("""
                DECLARE TYPE int SIZE 4; DECLARE VARIABLE x AS int;
                x <- 1; x <- x + 1; PRINT x;
                """));
    }

    @Test
    void testKeywordsCaseInsensitiveIdentifiersCaseSensitive() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 1), run("""
                DECLARE TYPE int SIZE 4;
                DeClArE VARIABLE myVar AS int;
                declare variable myvar as int;
                myVar <- 1;
                myvar <- 2;
                PRINT myVar;
                """));
    }

    @Test
    void testOfIsRightAssociativeThroughNestedStructs() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 7), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN x AS int; y AS int; END;
                DECLARE STRUCT Rect THEN topLeft AS Point; bottomRight AS Point; END;
                DECLARE VARIABLE r AS Rect;
                x OF topLeft OF r <- 7;
                PRINT x OF topLeft OF r;
                """));
    }

    @Test
    void testStructCopyIsByValue() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 4), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN x AS int; y AS int; END;
                DECLARE VARIABLE p AS Point;
                DECLARE VARIABLE q AS Point;
                x OF p <- 3;
                y OF p <- 4;
                q <- p;
                PRINT y OF q;
                """));
    }

    @Test
    void testIndexingBindsTighterThanOrOf() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 20), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN x AS int; y AS int; END;
                DECLARE ARRAY points AS Point LENGTH 4;
                x OF points[1] <- 10;
                y OF points[1] <- 20;
                PRINT y OF points[1];
                """));
    }

    @Test
    void testVariableArrayIndex() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 99), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LENGTH 10;
                DECLARE VARIABLE i AS int;
                i <- 2;
                grid[i] <- 99;
                PRINT grid[i];
                """));
    }

    @Test
    void testNestedArrayIndex() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 55), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LENGTH 10;
                DECLARE ARRAY other AS int LENGTH 4;
                other[1] <- 3;
                grid[other[1]] <- 55;
                PRINT grid[3];
                """));
    }

    @Test
    void testLengthOfFoldsToConstant() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 40), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LENGTH 10;
                DECLARE VARIABLE total AS int;
                total <- LENGTH OF grid * 4;
                PRINT total;
                """));
    }
}