package basic.src.main.exceptions;

public class BadParseException extends RuntimeException {
    public BadParseException(String message, int line) {
        super(message + " (at line " + line + ")");
    }
    
}
