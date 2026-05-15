package tundra;

import java.util.ArrayList;
import java.util.List;

public class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String>, TypeAnnotation.Visitor<String> {
    public String print(Stmt statement) {
        if (statement == null) {
            return "(parse-error)";
        }

        return statement.accept(this);
    }

    @Override
    public String visitBlockStmt(Stmt.Block stmt) {
        List<String> parts = new ArrayList<>();
        parts.add("block");
        for (Stmt statement : stmt.statements) {
            parts.add(statement.accept(this));
        }

        return parenthesize(parts);
    }

    @Override
    public String visitExpressionStmt(Stmt.Expression stmt) {
        return parenthesize("expr", stmt.expression);
    }

    @Override
    public String visitForStmt(Stmt.For stmt) {
        return parenthesize("for " + stmt.variable.lexeme, stmt.iterable, stmt.body);
    }

    @Override
    public String visitFunctionStmt(Stmt.Function stmt) {
        List<String> parts = new ArrayList<>();
        parts.add("def " + stmt.name.lexeme);
        parts.add(params(stmt.params));
        parts.add(stmt.body.accept(this));
        return parenthesize(parts);
    }

    @Override
    public String visitIfStmt(Stmt.If stmt) {
        if (stmt.elseBranch == null) {
            return parenthesize("if", stmt.condition, stmt.thenBranch);
        }

        return parenthesize("if", stmt.condition, stmt.thenBranch, stmt.elseBranch);
    }

    @Override
    public String visitReturnStmt(Stmt.Return stmt) {
        if (stmt.value == null) {
            return "(return)";
        }

        return parenthesize("return", stmt.value);
    }

    @Override
    public String visitVarStmt(Stmt.Var stmt) {
        String keyword = stmt.mutable ? "var" : "val";
        String name = keyword + " " + stmt.name.lexeme;
        if (stmt.typeAnnotation != null) {
            name += " : " + stmt.typeAnnotation.accept(this);
        }

        return parenthesize(name, stmt.initializer);
    }

    @Override
    public String visitWhileStmt(Stmt.While stmt) {
        return parenthesize("while", stmt.condition, stmt.body);
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return parenthesize("= " + expr.name.lexeme, expr.value);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitCallExpr(Expr.Call expr) {
        List<String> parts = new ArrayList<>();
        parts.add("call");
        parts.add(expr.callee.accept(this));
        for (Expr argument : expr.arguments) {
            parts.add(argument.accept(this));
        }

        return parenthesize(parts);
    }

    @Override
    public String visitFieldExpr(Expr.Field expr) {
        List<String> parts = new ArrayList<>();
        parts.add(".");
        parts.add(expr.object.accept(this));
        parts.add(expr.name.lexeme);
        return parenthesize(parts);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitIndexExpr(Expr.Index expr) {
        return parenthesize("index", expr.object, expr.index);
    }

    @Override
    public String visitListExpr(Expr.ListLiteral expr) {
        List<String> parts = new ArrayList<>();
        parts.add("list");
        for (Expr element : expr.elements) {
            parts.add(element.accept(this));
        }

        return parenthesize(parts);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "none";
        if (expr.value instanceof String) {
            return "\"" + escapeString((String) expr.value) + "\"";
        }
        return expr.value.toString();
    }

    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitRecordExpr(Expr.RecordLiteral expr) {
        List<String> parts = new ArrayList<>();
        parts.add("record");
        for (Expr.RecordField field : expr.fields) {
            parts.add(parenthesize("field " + field.name.lexeme, field.value));
        }

        return parenthesize(parts);
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return expr.name.lexeme;
    }

    @Override
    public String visitNamedType(TypeAnnotation.Named type) {
        return type.name.lexeme;
    }

    @Override
    public String visitListType(TypeAnnotation.ListType type) {
        return "List<" + type.elementType.accept(this) + ">";
    }

    private String params(List<Token> parameters) {
        List<String> names = new ArrayList<>();
        names.add("params");
        for (Token parameter : parameters) {
            names.add(parameter.lexeme);
        }

        return parenthesize(names);
    }

    private String parenthesize(String name, Expr... exprs) {
        List<String> parts = new ArrayList<>();
        parts.add(name);
        for (Expr expr : exprs) {
            parts.add(expr.accept(this));
        }

        return parenthesize(parts);
    }

    private String parenthesize(String name, Stmt... statements) {
        List<String> parts = new ArrayList<>();
        parts.add(name);
        for (Stmt statement : statements) {
            parts.add(statement.accept(this));
        }

        return parenthesize(parts);
    }

    private String parenthesize(String name, Expr expr, Stmt statement) {
        return parenthesize(name, expr, statement, null);
    }

    private String parenthesize(String name, Expr expr, Stmt first, Stmt second) {
        List<String> parts = new ArrayList<>();
        parts.add(name);
        parts.add(expr.accept(this));
        parts.add(first.accept(this));
        if (second != null) {
            parts.add(second.accept(this));
        }

        return parenthesize(parts);
    }

    private String parenthesize(List<String> parts) {
        StringBuilder builder = new StringBuilder();
        builder.append('(');
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(parts.get(i));
        }
        builder.append(')');
        return builder.toString();
    }

    private String escapeString(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}
