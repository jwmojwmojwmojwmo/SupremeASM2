package basic.src.test;

import org.junit.jupiter.api.Test;

public class ParserTest extends CompilerTestBase {

    // ---- valid syntax ----

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

    // ---- invalid expressions ----

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

    // ---- bitwise: valid syntax ----

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

    // ---- bitwise: constant folding errors ----

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
        // round-trip check catches bits shifted out of the long
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

    // ---- array syntax ----

    @Test
    void testParsesArrayDeclaration() {
        assertParses("DECLARE ARRAY grid AS int LEN 10;");
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
    void testParsesLenOfArray() {
        assertParses("len <- LEN OF grid;");
    }

    @Test
    void testParsesNestedArrayAccess() {
        assertParses("grid[other[i]] <- 10;");
    }

    @Test
    void testParsesArrayAccessWithExpression() {
        assertParses("grid[i + 1] <- 10;");
    }
}