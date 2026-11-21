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
    void testSyntaxAdjacentIdentifiers() {
        assertParseFails("x <- a b;", "expected ';'");
    }

    @Test
    void testSyntaxUnaryMinusOnVariable() {
        // Current limitation: unary minus only works on literals, not variables
        assertParseFails("x <- -a;", "expected NUMBER");
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

    // ---- invalid assignment operators ----

    @Test
    void testSyntaxLessThanInsteadOfArrow() {
        assertParseFails("x < y;", "expected '<-'");
    }

    @Test
    void testSyntaxEqualsInsteadOfArrow() {
        assertParseFails("x = y;", "expected '<-'");
    }

    @Test
    void testSyntaxDoubleArrow() {
        assertParseFails("x <- <- y;", "expected a number, variable");
    }

    // ---- invalid declarations ----

    @Test
    void testSyntaxMissingAS() {
        assertParseFails("DECLARE VARIABLE x int;", "expected 'AS'");
    }

    @Test
    void testSyntaxMissingSIZE() {
        assertParseFails("DECLARE TYPE int 4;", "expected 'SIZE'");
    }

    @Test
    void testSyntaxMissingTHEN() {
        assertParseFails("DECLARE STRUCT P x AS int; END;", "expected 'THEN'");
    }

    @Test
    void testSyntaxMissingFieldSemicolon() {
        assertParseFails("DECLARE STRUCT P THEN x AS int END;", "expected ';'");
    }

    @Test
    void testSyntaxKeywordAsIdentifier() {
        assertParseFails("DECLARE VARIABLE THEN AS int;", "expected IDENTIFIER");
    }

    @Test
    void testSyntaxMissingDeclSemicolon() {
        assertParseFails("DECLARE VARIABLE x AS int", "expected ';'");
    }

    @Test
    void testSyntaxBadDeclareKind() {
        assertParseFails("DECLARE FUNCTION f;", "expected VARIABLE, TYPE, or STRUCT");
    }
}