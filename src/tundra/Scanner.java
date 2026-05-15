package tundra;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scanner {

    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    private int start = 0;        // start offset of current lexeme
    private int current = 0;      // current character being looked at
    private int line = 1;         // current line number
    private int startLine = 1;    // line where current token started

    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();

    static {
        KEYWORDS.put("and",    TokenType.AND);
        KEYWORDS.put("or",     TokenType.OR);
        KEYWORDS.put("if",     TokenType.IF);
        KEYWORDS.put("else",   TokenType.ELSE);
        KEYWORDS.put("while",  TokenType.WHILE);
        KEYWORDS.put("for",    TokenType.FOR);
        KEYWORDS.put("in",     TokenType.IN);
        KEYWORDS.put("def",    TokenType.DEF);
        KEYWORDS.put("return", TokenType.RETURN);
        KEYWORDS.put("true",   TokenType.TRUE);
        KEYWORDS.put("false",  TokenType.FALSE);
        KEYWORDS.put("none",   TokenType.NONE);
        KEYWORDS.put("val",    TokenType.VAL);
        KEYWORDS.put("var",    TokenType.VAR);
    }

    public Scanner(String source) {
        this.source = source;
    }

    // -------------------------------------------------------------------------
    // Public entry point
    // -------------------------------------------------------------------------

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            startLine = line;
            scanToken();
        }

        tokens.add(new Token(TokenType.EOF, "", null, line, current, current));
        return tokens;
    }

    // -------------------------------------------------------------------------
    // Core scan loop
    // -------------------------------------------------------------------------

    private void scanToken() {
        char c = advance();

        switch (c) {
            // Single-character tokens
            case '(': addToken(TokenType.LEFT_PAREN);    break;
            case ')': addToken(TokenType.RIGHT_PAREN);   break;
            case '{': addToken(TokenType.LEFT_BRACE);    break;
            case '}': addToken(TokenType.RIGHT_BRACE);   break;
            case '[': addToken(TokenType.LEFT_BRACKET);  break;
            case ']': addToken(TokenType.RIGHT_BRACKET); break;
            case ',': addToken(TokenType.COMMA);         break;
            case '.': addToken(TokenType.DOT);           break;
            case ';': addToken(TokenType.SEMICOLON);     break;
            case ':': addToken(TokenType.COLON);         break;
            case '+': addToken(TokenType.PLUS);          break;
            case '-': addToken(TokenType.MINUS);         break;
            case '*': addToken(TokenType.STAR);          break;

            // One-or-two-character tokens
            case '!':
                addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
                break;

            case '=':
                addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
                break;

            case '<':
                addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
                break;

            case '>':
                addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
                break;

            // Slash or comment
            case '/':
                if (match('/')) {
                    // Line comment: consume until end of line.
                    while (peek() != '\n' && !isAtEnd()) {
                        advance();
                    }
                } else {
                    addToken(TokenType.SLASH);
                }
                break;

            // Whitespace
            case ' ':
            case '\r':
            case '\t':
                break;

            case '\n':
                line++;
                break;

            // String literals
            case '"':
                string();
                break;

            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    Tundra.error(line, "Unexpected character: '" + c + "'.");
                }
                break;
        }
    }

    // -------------------------------------------------------------------------
    // String literals
    //
    // Tundra 0.1 allows multiline strings.
    // Escape sequences are not supported yet.
    // -------------------------------------------------------------------------

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') {
                line++;
            }

            advance();
        }

        if (isAtEnd()) {
            Tundra.error(startLine, "Unterminated string.");
            return;
        }

        advance(); // consume the closing "

        // Trim the surrounding quotes to get the actual string value.
        String value = source.substring(start + 1, current - 1);
        addToken(TokenType.STRING, value);
    }

    // -------------------------------------------------------------------------
    // Number literals
    // -------------------------------------------------------------------------

    private void number() {
        while (isDigit(peek())) {
            advance();
        }

        // Look for a decimal part.
        if (peek() == '.' && isDigit(peekNext())) {
            advance(); // consume the '.'

            while (isDigit(peek())) {
                advance();
            }
        }

        double value = Double.parseDouble(source.substring(start, current));
        addToken(TokenType.NUMBER, value);
    }

    // -------------------------------------------------------------------------
    // Identifiers and keywords
    // -------------------------------------------------------------------------

    private void identifier() {
        while (isAlphaNumeric(peek())) {
            advance();
        }

        String text = source.substring(start, current);
        TokenType type = KEYWORDS.getOrDefault(text, TokenType.IDENTIFIER);

        addToken(type);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        return source.charAt(current++);
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z')
            || (c >= 'A' && c <= 'Z')
            || c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, startLine, start, current));
    }
}