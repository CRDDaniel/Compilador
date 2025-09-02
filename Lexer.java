import java.util.*;

enum TokenType {
    IDENTIFIER,     // nombres de variables, funciones
    KEYWORD,        // var, print, etc.
    NUMBER,         // 123, 45.67
    SYMBOL,         // + - * / = ; , . == != <= >=
    PAREN,          // ( )
    BRACE,          // { }
    EOF             // fin de archivo
}

class Token {
    final TokenType type;
    final String lexeme;
    final int line;
    final int column;

    Token(TokenType type, String lexeme, int line, int column) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
        this.column = column;
    }

    @Override
    public String toString() {
        return "<" + type + ", '" + lexeme + "', line=" + line + ", col=" + column + ">";
    }
}

class LexicalException extends RuntimeException {
    LexicalException(String message) {
        super(message);
    }
}

public class Lexer {
    private final String source;
    private final List<String> lines; // para mostrar la línea en errores
    private int pos = 0;
    private int line = 1;
    private int column = 1;

    // Palabras reservadas básicas (puedes ampliar esta lista)
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
        "var", "print"
    ));

    // Símbolos permitidos de un carácter
    private static final Set<Character> SINGLE_SYMBOLS = new HashSet<>(Arrays.asList(
        '+','-','*','/','%','=', ';', ',', '.', ':'
    ));

    // Operadores de dos caracteres soportados
    private static final Set<String> DOUBLE_SYMBOLS = new HashSet<>(Arrays.asList(
        "==","!=", "<=", ">="
    ));

    public Lexer(String source) {
        this.source = source;
        this.lines = Arrays.asList(source.split("\n", -1));
    }

    public List<Token> scanTokens() {
        List<Token> tokens = new ArrayList<>();
        while (!isAtEnd()) {
            skipWhitespace();
            if (isAtEnd()) break;

            char c = peek();

            int startLine = line;
            int startCol = column;

            // Identificadores o palabras reservadas
            if (isIdentifierStart(c)) {
                String ident = readIdentifier();
                TokenType type = KEYWORDS.contains(ident) ? TokenType.KEYWORD : TokenType.IDENTIFIER;
                tokens.add(new Token(type, ident, startLine, startCol));
                continue;
            }

            // Números (enteros y decimales)
            if (Character.isDigit(c)) {
                String number = readNumber();
                tokens.add(new Token(TokenType.NUMBER, number, startLine, startCol));
                continue;
            }

            // Paréntesis
            if (c == '(' || c == ')') {
                advance();
                tokens.add(new Token(TokenType.PAREN, String.valueOf(c), startLine, startCol));
                continue;
            }

            // Llaves
            if (c == '{' || c == '}') {
                advance();
                tokens.add(new Token(TokenType.BRACE, String.valueOf(c), startLine, startCol));
                continue;
            }

            // Operadores dobles (==, !=, <=, >=)
            if (!isAtEnd() && !isAtEndNext()) {
                String two = lookahead2();
                if (DOUBLE_SYMBOLS.contains(two)) {
                    advance(); // consume first
                    advance(); // consume second
                    tokens.add(new Token(TokenType.SYMBOL, two, startLine, startCol));
                    continue;
                }
            }

            // Símbolos de un carácter
            if (SINGLE_SYMBOLS.contains(c)) {
                advance();
                tokens.add(new Token(TokenType.SYMBOL, String.valueOf(c), startLine, startCol));
                continue;
            }

            // Si llegamos aquí, es un carácter inválido -> detener con error
            errorInvalidChar(c, startLine, startCol);
        }

        tokens.add(new Token(TokenType.EOF, "", line, column));
        return tokens;
    }

    // === Helpers de lectura ===

    private boolean isAtEnd() {
        return pos >= source.length();
    }

    private boolean isAtEndNext() {
        return pos + 1 >= source.length();
    }

    private char peek() {
        return source.charAt(pos);
    }

    private char peekNext() {
        return source.charAt(pos + 1);
    }

    private void advance() {
        char c = source.charAt(pos++);
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
    }

    private void skipWhitespace() {
        while (!isAtEnd()) {
            char c = peek();
            if (c == ' ' || c == '\r' || c == '\t' || c == '\n') {
                advance();
            } else {
                break;
            }
        }
    }

    private boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private String readIdentifier() {
        int start = pos;
        advance(); // ya sabemos que el primero es válido
        while (!isAtEnd() && isIdentifierPart(peek())) {
            advance();
        }
        return source.substring(start, pos);
    }

    private String readNumber() {
        int start = pos;
        boolean seenDot = false;

        while (!isAtEnd()) {
            char c = peek();
            if (Character.isDigit(c)) {
                advance();
            } else if (c == '.' && !seenDot) {
                // permitir un solo punto decimal
                seenDot = true;
                advance();
                // opcional: exigir al menos un dígito después del punto
                if (!isAtEnd() && !Character.isDigit(peek())) {
                    errorInvalidChar(peek(), line, column);
                }
            } else {
                break;
            }
        }
        return source.substring(start, pos);
    }

    private String lookahead2() {
        if (isAtEnd() || isAtEndNext()) return "";
        return "" + peek() + peekNext();
    }

    private void errorInvalidChar(char c, int errLine, int errCol) {
        String lineText = (errLine - 1 >= 0 && errLine - 1 < lines.size()) ? lines.get(errLine - 1) : "";
        String pointer = makePointer(errCol);

        StringBuilder sb = new StringBuilder();
        sb.append("❌ Error léxico: carácter inválido '")
          .append(printable(c)).append("' en línea ").append(errLine)
          .append(", columna ").append(errCol).append("\n");
        sb.append(lineText).append("\n").append(pointer);

        throw new LexicalException(sb.toString());
    }

    private String makePointer(int col) {
        StringBuilder p = new StringBuilder();
        for (int i = 1; i < col; i++) p.append(' ');
        p.append('^');
        return p.toString();
    }

    private String printable(char c) {
        if (Character.isISOControl(c)) {
            return String.format("\\u%04x", (int)c);
        }
        return String.valueOf(c);
    }

    // === Programa de ejemplo ===
    public static void main(String[] args) {
        System.out.println("Pega tu código. Termina con una línea que contenga solo: EOF");
        Scanner sc = new Scanner(System.in);
        StringBuilder sb = new StringBuilder();
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if (line.equals("EOF")) break;
            sb.append(line).append('\n');
        }
        String code = sb.toString();

        try {
            Lexer lexer = new Lexer(code);
            List<Token> tokens = lexer.scanTokens();
            System.out.println("\n=== TOKENS ===");
            for (Token t : tokens) {
                if (t.type != TokenType.EOF) {
                    System.out.println(t);
                }
            }
        } catch (LexicalException e) {
            System.err.println(e.getMessage());
            // código de salida opcional distinto de 0
            System.exit(1);
        }
    }
}
