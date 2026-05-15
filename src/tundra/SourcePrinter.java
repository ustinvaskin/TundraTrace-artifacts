package tundra;

import java.util.ArrayList;
import java.util.List;

public class SourcePrinter implements Expr.Visitor<String> {
    public String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return expr.name.lexeme + " = " + print(expr.value);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return print(expr.left) + " " + expr.operator.lexeme + " " + print(expr.right);
    }

    @Override
    public String visitCallExpr(Expr.Call expr) {
        List<String> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(print(argument));
        }

        return print(expr.callee) + "(" + String.join(", ", arguments) + ")";
    }

    @Override
    public String visitFieldExpr(Expr.Field expr) {
        return print(expr.object) + "." + expr.name.lexeme;
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return "(" + print(expr.expression) + ")";
    }

    @Override
    public String visitIndexExpr(Expr.Index expr) {
        return print(expr.object) + "[" + print(expr.index) + "]";
    }

    @Override
    public String visitListExpr(Expr.ListLiteral expr) {
        List<String> elements = new ArrayList<>();
        for (Expr element : expr.elements) {
            elements.add(print(element));
        }

        return "[" + String.join(", ", elements) + "]";
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "none";
        if (expr.value instanceof String) {
            return "\"" + escapeString((String) expr.value) + "\"";
        }
        if (expr.value instanceof Double) {
            return stringifyNumber((double) expr.value);
        }
        return expr.value.toString();
    }

    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return print(expr.left) + " " + expr.operator.lexeme + " " + print(expr.right);
    }

    @Override
    public String visitRecordExpr(Expr.RecordLiteral expr) {
        List<String> fields = new ArrayList<>();
        for (Expr.RecordField field : expr.fields) {
            fields.add(field.name.lexeme + ": " + print(field.value));
        }

        return "{ " + String.join(", ", fields) + " }";
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return expr.operator.lexeme + print(expr.right);
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return expr.name.lexeme;
    }

    private String escapeString(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    private String stringifyNumber(double value) {
        String text = Double.toString(value);
        if (text.endsWith(".0")) {
            return text.substring(0, text.length() - 2);
        }
        return text;
    }
}
