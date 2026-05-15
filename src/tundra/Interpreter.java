package tundra;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    private final Environment globals = new Environment();
    private final SourcePrinter sourcePrinter = new SourcePrinter();
    private Environment environment = globals;

    public Interpreter() {
        globals.define("print", new TundraCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                System.out.println(interpreter.stringify(arguments.get(0)));
                return interpreter.value(null, Provenance.leaf("builtin", "print returned none"));
            }

            @Override
            public String toString() {
                return "<native fn print>";
            }
        }, false);

        globals.define("len", new TundraCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                Object value = interpreter.unwrap(arguments.get(0));
                if (value instanceof String) {
                    return interpreter.value(
                            (double) ((String) value).length(),
                            Provenance.node("builtin", "len(" + interpreter.displayValue(arguments.get(0)) + ")", interpreter.provenanceOf(arguments.get(0)))
                    );
                }
                if (value instanceof List) {
                    return interpreter.value(
                            (double) ((List<?>) value).size(),
                            Provenance.node("builtin", "len(" + interpreter.displayValue(arguments.get(0)) + ")", interpreter.provenanceOf(arguments.get(0)))
                    );
                }

                throw new RuntimeError(null, "len() expects a string or list.");
            }

            @Override
            public String toString() {
                return "<native fn len>";
            }
        }, false);

        globals.define("parseNumber", new TundraCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                Object value = interpreter.unwrap(arguments.get(0));
                if (!(value instanceof String)) {
                    throw new RuntimeError(null, "parseNumber() expects a string.");
                }

                try {
                    return interpreter.value(
                            Double.parseDouble((String) value),
                            Provenance.node("builtin", "parseNumber(" + interpreter.displayValue(arguments.get(0)) + ")", interpreter.provenanceOf(arguments.get(0)))
                    );
                } catch (NumberFormatException error) {
                    throw new RuntimeError(null, "Cannot parse number from '" + value + "'.");
                }
            }

            @Override
            public String toString() {
                return "<native fn parseNumber>";
            }
        }, false);

        globals.define("origin", new TundraCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                Provenance provenance = interpreter.provenanceOf(arguments.get(0));
                String origin = provenance == null ? "unknown origin" : provenance.originDescription();
                return interpreter.value(origin, Provenance.leaf("builtin", "origin(...)"));
            }

            @Override
            public String toString() {
                return "<native fn origin>";
            }
        }, false);

        globals.define("history", new TundraCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                Provenance provenance = interpreter.provenanceOf(arguments.get(0));
                String history = provenance == null ? "- unknown history" : provenance.formatTree();
                return interpreter.value(history, Provenance.leaf("builtin", "history(...)"));
            }

            @Override
            public String toString() {
                return "<native fn history>";
            }
        }, false);
    }

    public void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Tundra.runtimeError(error);
        } catch (ReturnSignal signal) {
            Tundra.runtimeError(new RuntimeError(signal.keyword, "Cannot return from top-level code."));
        }
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    public void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;
            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitForStmt(Stmt.For stmt) {
        Object iterable = evaluate(stmt.iterable);
        Object rawIterable = unwrap(iterable);
        if (!(rawIterable instanceof List)) {
            throw new RuntimeError(stmt.variable, "Can only iterate over lists.");
        }

        for (Object item : (List<?>) rawIterable) {
            Environment loopEnvironment = new Environment(environment);
            loopEnvironment.define(
                    stmt.variable.lexeme,
                    withProvenance(item, Provenance.node("for", stmt.variable.lexeme + " came from " + sourcePrinter.print(stmt.iterable), provenanceOf(item))),
                    false
            );
            executeBlock(stmt.body.statements, loopEnvironment);
        }

        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        environment.define(stmt.name.lexeme, new TundraFunction(stmt, environment), false);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }

        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) {
            value = evaluate(stmt.value);
        }

        throw new ReturnSignal(stmt.keyword, value);
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = evaluate(stmt.initializer);
        environment.define(
                stmt.name.lexeme,
                withProvenance(value, Provenance.node("binding", stmt.name.lexeme + " came from " + sourcePrinter.print(stmt.initializer), provenanceOf(value))),
                stmt.mutable
        );
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }

        return null;
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        Object assigned = withProvenance(value, Provenance.node("assignment", expr.name.lexeme + " came from " + sourcePrinter.print(expr.value), provenanceOf(value)));
        environment.assign(expr.name, assigned);
        return assigned;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        Object leftData = unwrap(left);
        Object rightData = unwrap(right);

        switch (expr.operator.type) {
            case BANG_EQUAL:
                return value(!isEqual(left, right), binaryProvenance(expr, left, right));
            case EQUAL_EQUAL:
                return value(isEqual(left, right), binaryProvenance(expr, left, right));
            case GREATER:
                checkNumberOperands(expr, left, right);
                return value((double) leftData > (double) rightData, binaryProvenance(expr, left, right));
            case GREATER_EQUAL:
                checkNumberOperands(expr, left, right);
                return value((double) leftData >= (double) rightData, binaryProvenance(expr, left, right));
            case LESS:
                checkNumberOperands(expr, left, right);
                return value((double) leftData < (double) rightData, binaryProvenance(expr, left, right));
            case LESS_EQUAL:
                checkNumberOperands(expr, left, right);
                return value((double) leftData <= (double) rightData, binaryProvenance(expr, left, right));
            case MINUS:
                checkNumberOperands(expr, left, right);
                return value((double) leftData - (double) rightData, binaryProvenance(expr, left, right));
            case PLUS:
                if (leftData instanceof Double && rightData instanceof Double) {
                    return value((double) leftData + (double) rightData, binaryProvenance(expr, left, right));
                }
                if (leftData instanceof String && rightData instanceof String) {
                    return value((String) leftData + (String) rightData, binaryProvenance(expr, left, right));
                }

                throw binaryTypeError(expr, "Cannot add " + typeName(left) + " and " + typeName(right) + ".", left, right);
            case SLASH:
                checkNumberOperands(expr, left, right);
                if ((double) rightData == 0) {
                    throw new RuntimeError(
                            expr.operator,
                            "ZeroDivisionError",
                            "Cannot divide by zero.",
                            sourcePrinter.print(expr),
                            binaryDetails(left, right),
                            binaryProvenance(expr, left, right)
                    );
                }
                return value((double) leftData / (double) rightData, binaryProvenance(expr, left, right));
            case STAR:
                checkNumberOperands(expr, left, right);
                return value((double) leftData * (double) rightData, binaryProvenance(expr, left, right));
            default:
                return null;
        }
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof TundraCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions.");
        }

        TundraCallable function = (TundraCallable) callee;
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(
                    expr.paren,
                    "Expected " + function.arity() + " arguments but got " + arguments.size() + "."
            );
        }

        return function.call(this, arguments);
    }

    @Override
    public Object visitFieldExpr(Expr.Field expr) {
        Object object = evaluate(expr.object);
        Object rawObject = unwrap(object);
        if (!(rawObject instanceof Map)) {
            throw new RuntimeError(
                    expr.name,
                    "FieldError",
                    "Only records have fields.",
                    sourcePrinter.print(expr),
                    detail("Object value", object),
                    Provenance.node("field-error", sourcePrinter.print(expr), provenanceOf(object))
            );
        }

        Map<?, ?> record = (Map<?, ?>) rawObject;
        if (!record.containsKey(expr.name.lexeme)) {
            throw new RuntimeError(
                    expr.name,
                    "FieldError",
                    "Undefined field '" + expr.name.lexeme + "'.",
                    sourcePrinter.print(expr),
                    detail("Object value", object),
                    Provenance.node("field-error", sourcePrinter.print(expr), provenanceOf(object))
            );
        }

        Object fieldValue = record.get(expr.name.lexeme);
        return withProvenance(
                fieldValue,
                Provenance.node("field", sourcePrinter.print(expr) + " came from field " + expr.name.lexeme, provenanceOf(fieldValue), provenanceOf(object))
        );
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitIndexExpr(Expr.Index expr) {
        Object object = evaluate(expr.object);
        Object index = evaluate(expr.index);
        Object rawObject = unwrap(object);
        Object rawIndexObject = unwrap(index);

        if (!(rawObject instanceof List)) {
            throw new RuntimeError(
                    expr.bracket,
                    "IndexError",
                    "Only lists can be indexed.",
                    sourcePrinter.print(expr),
                    detail("Object value", object),
                    Provenance.node("index-error", sourcePrinter.print(expr), provenanceOf(object), provenanceOf(index))
            );
        }
        if (!(rawIndexObject instanceof Double)) {
            throw new RuntimeError(
                    expr.bracket,
                    "IndexError",
                    "List index must be a number.",
                    sourcePrinter.print(expr),
                    detail("Index value", index),
                    Provenance.node("index-error", sourcePrinter.print(expr), provenanceOf(object), provenanceOf(index))
            );
        }

        double rawIndex = (double) rawIndexObject;
        int listIndex = (int) rawIndex;
        if (rawIndex != listIndex) {
            throw new RuntimeError(
                    expr.bracket,
                    "IndexError",
                    "List index must be a whole number.",
                    sourcePrinter.print(expr),
                    detail("Index value", index),
                    Provenance.node("index-error", sourcePrinter.print(expr), provenanceOf(object), provenanceOf(index))
            );
        }

        List<?> list = (List<?>) rawObject;
        if (listIndex < 0 || listIndex >= list.size()) {
            throw new RuntimeError(
                    expr.bracket,
                    "IndexError",
                    "List index " + listIndex + " out of bounds.",
                    sourcePrinter.print(expr),
                    detail("Index value", index),
                    Provenance.node("index-error", sourcePrinter.print(expr), provenanceOf(object), provenanceOf(index))
            );
        }

        Object indexedValue = list.get(listIndex);
        return withProvenance(
                indexedValue,
                Provenance.node("index", sourcePrinter.print(expr) + " came from index " + listIndex, provenanceOf(indexedValue), provenanceOf(object), provenanceOf(index))
        );
    }

    @Override
    public Object visitListExpr(Expr.ListLiteral expr) {
        List<Object> elements = new ArrayList<>();
        for (Expr element : expr.elements) {
            elements.add(evaluate(element));
        }

        return value(elements, Provenance.leaf("list", sourcePrinter.print(expr)));
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return value(expr.value, literalProvenance(expr.value));
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }

        return evaluate(expr.right);
    }

    @Override
    public Object visitRecordExpr(Expr.RecordLiteral expr) {
        Map<String, Object> fields = new LinkedHashMap<>();
        for (Expr.RecordField field : expr.fields) {
            Object fieldValue = evaluate(field.value);
            fields.put(
                    field.name.lexeme,
                    withProvenance(fieldValue, Provenance.node("record-field", field.name.lexeme + " came from " + sourcePrinter.print(field.value), provenanceOf(fieldValue)))
            );
        }

        return value(fields, Provenance.leaf("record", sourcePrinter.print(expr)));
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);
        Object rightData = unwrap(right);

        switch (expr.operator.type) {
            case BANG:
                return value(!isTruthy(right), Provenance.node("unary", sourcePrinter.print(expr), provenanceOf(right)));
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return value(-(double) rightData, Provenance.node("unary", sourcePrinter.print(expr), provenanceOf(right)));
            default:
                return null;
        }
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }

    private void execute(Stmt statement) {
        statement.accept(this);
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private boolean isTruthy(Object value) {
        value = unwrap(value);
        if (value == null) return false;
        if (value instanceof Boolean) return (boolean) value;
        return true;
    }

    private boolean isEqual(Object left, Object right) {
        left = unwrap(left);
        right = unwrap(right);
        if (left == null && right == null) return true;
        if (left == null) return false;
        return left.equals(right);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        operand = unwrap(operand);
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Expr.Binary expr, Object left, Object right) {
        if (unwrap(left) instanceof Double && unwrap(right) instanceof Double) return;
        throw binaryTypeError(
                expr,
                "Operands must be numbers, got " + typeName(left) + " and " + typeName(right) + ".",
                left,
                right
        );
    }

    private RuntimeError binaryTypeError(Expr.Binary expr, String message, Object left, Object right) {
        return new RuntimeError(
                expr.operator,
                "TypeError",
                message,
                sourcePrinter.print(expr),
                binaryDetails(left, right),
                binaryProvenance(expr, left, right)
        );
    }

    private List<String> binaryDetails(Object left, Object right) {
        List<String> details = new ArrayList<>();
        details.add("Left value: " + displayValue(left) + " : " + typeName(left));
        details.add("Right value: " + displayValue(right) + " : " + typeName(right));
        return details;
    }

    private List<String> detail(String label, Object value) {
        List<String> details = new ArrayList<>();
        details.add(label + ": " + displayValue(value) + " : " + typeName(value));
        return details;
    }

    private Provenance binaryProvenance(Expr.Binary expr, Object left, Object right) {
        return Provenance.node("binary", sourcePrinter.print(expr), provenanceOf(left), provenanceOf(right));
    }

    private Provenance literalProvenance(Object value) {
        if (value == null) {
            return Provenance.leaf("literal", "literal none");
        }
        if (value instanceof String) {
            return Provenance.leaf("literal", "literal string \"" + value + "\"");
        }
        if (value instanceof Double) {
            return Provenance.leaf("literal", "literal number " + stringify(value));
        }
        if (value instanceof Boolean) {
            return Provenance.leaf("literal", "literal bool " + value);
        }

        return Provenance.leaf("literal", "literal " + stringify(value));
    }

    TundraValue value(Object data, Provenance provenance) {
        return new TundraValue(data, provenance);
    }

    private Object withProvenance(Object value, Provenance provenance) {
        if (value instanceof TundraValue) {
            return ((TundraValue) value).withProvenance(provenance);
        }

        return new TundraValue(value, provenance);
    }

    Object unwrap(Object value) {
        if (value instanceof TundraValue) {
            return ((TundraValue) value).data;
        }

        return value;
    }

    Provenance provenanceOf(Object value) {
        if (value instanceof TundraValue) {
            return ((TundraValue) value).provenance;
        }

        return null;
    }

    private String stringify(Object value) {
        value = unwrap(value);
        if (value == null) return "none";

        if (value instanceof Double) {
            String text = value.toString();
            if (text.endsWith(".0")) {
                return text.substring(0, text.length() - 2);
            }
            return text;
        }

        if (value instanceof List) {
            List<String> parts = new ArrayList<>();
            for (Object item : (List<?>) value) {
                parts.add(stringify(item));
            }
            return "[" + String.join(", ", parts) + "]";
        }

        if (value instanceof Map) {
            List<String> parts = new ArrayList<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                parts.add(entry.getKey() + ": " + stringify(entry.getValue()));
            }
            return "{" + String.join(", ", parts) + "}";
        }

        return value.toString();
    }

    String displayValue(Object value) {
        value = unwrap(value);
        if (value instanceof String) {
            return "\"" + value + "\"";
        }

        return stringify(value);
    }

    private String typeName(Object value) {
        value = unwrap(value);
        if (value == null) return "None";
        if (value instanceof Double) return "Number";
        if (value instanceof String) return "String";
        if (value instanceof Boolean) return "Bool";
        if (value instanceof List) return "List";
        if (value instanceof Map) return "Record";
        if (value instanceof TundraCallable) return "Function";
        return value.getClass().getSimpleName();
    }
}
