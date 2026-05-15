package tundra;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class Tundra {
    private static boolean hadError = false;
    private static boolean hadRuntimeError = false;

    private Tundra() {
        // Prevent creating Tundra objects.
    }

    public static void main(String[] args) throws IOException {
        if (args.length > 2) {
            System.err.println("Usage: tundra [--scan|--ast] [script]");
            System.exit(64);
        } else if (args.length == 2) {
            if (!args[0].equals("--scan") && !args[0].equals("--ast")) {
                System.err.println("Usage: tundra [--scan|--ast] [script]");
                System.exit(64);
            }
            runFile(args[1], modeFromFlag(args[0]));
        } else if (args.length == 1) {
            if (args[0].equals("--scan")) {
                runPrompt(RunMode.SCAN);
            } else if (args[0].equals("--ast")) {
                runPrompt(RunMode.AST);
            } else {
                runFile(args[0], RunMode.EXECUTE);
            }
        } else {
            runPrompt(RunMode.EXECUTE);
        }
    }

    private static void runFile(String path, RunMode mode) throws IOException {
        String source = Files.readString(Path.of(path), StandardCharsets.UTF_8);
        run(source, mode);

        if (hadError) {
            System.exit(65);
        }
        if (hadRuntimeError) {
            System.exit(70);
        }
    }

    private static void runPrompt(RunMode mode) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8)
        );

        while (true) {
            System.out.print("> ");
            String line = reader.readLine();

            if (line == null) {
                break;
            }

            run(line, mode);
            hadError = false;
            hadRuntimeError = false;
        }
    }

    private static void run(String source, RunMode mode) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        if (mode == RunMode.SCAN) {
            for (Token token : tokens) {
                System.out.println(token);
            }
            return;
        }

        if (hadError) {
            return;
        }

        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        if (hadError) {
            return;
        }

        if (mode == RunMode.AST) {
            AstPrinter printer = new AstPrinter();
            for (Stmt statement : statements) {
                System.out.println(printer.print(statement));
            }
            return;
        }

        Interpreter interpreter = new Interpreter();
        interpreter.interpret(statements);
    }

    public static void error(int line, String message) {
        System.err.println("[line " + line + "] Error: " + message);
        hadError = true;
    }

    public static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    private static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    public static void runtimeError(RuntimeError error) {
        String type = error.errorType.equals("RuntimeError") ? "Runtime error" : error.errorType;
        if (error.token == null) {
            System.err.println(type + ": " + error.getMessage());
        } else {
            System.err.println("[line " + error.token.line + "] " + type + ": " + error.getMessage());
        }
        if (error.expression != null) {
            System.err.println();
            System.err.println("Failing expression:");
            System.err.println(error.expression);
        }
        if (!error.details.isEmpty()) {
            System.err.println();
            for (String detail : error.details) {
                System.err.println(detail);
            }
        }
        if (error.provenance != null) {
            List<String> provenanceLines = error.provenance.formatCompact();
            if (!provenanceLines.isEmpty()) {
                System.err.println();
                System.err.println("Value provenance:");
                for (String line : provenanceLines) {
                    System.err.println("- " + line);
                }
            }
        }
        hadRuntimeError = true;
    }

    private static RunMode modeFromFlag(String flag) {
        if (flag.equals("--scan")) return RunMode.SCAN;
        return RunMode.AST;
    }

    private enum RunMode {
        SCAN,
        AST,
        EXECUTE
    }
}
