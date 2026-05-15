package tundra;

public final class Token {
    public final TokenType type;
    public final String lexeme;
    public final Object literal;
    public final int line;
    public final int startOffset;
    public final int endOffset;

    public Token(
            TokenType type,
            String lexeme,
            Object literal,
            int line,
            int startOffset,
            int endOffset
    ) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    @Override
    public String toString() {
        return type + " " + lexeme + " " + literal
                + " [line=" + line
                + ", start=" + startOffset
                + ", end=" + endOffset + "]";
    }
}