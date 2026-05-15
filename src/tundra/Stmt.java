package tundra;

import java.util.List;

public abstract class Stmt {
    public interface Visitor<R> {
        R visitBlockStmt(Block stmt);
        R visitExpressionStmt(Expression stmt);
        R visitForStmt(For stmt);
        R visitFunctionStmt(Function stmt);
        R visitIfStmt(If stmt);
        R visitReturnStmt(Return stmt);
        R visitVarStmt(Var stmt);
        R visitWhileStmt(While stmt);
    }

    public abstract <R> R accept(Visitor<R> visitor);

    public static final class Block extends Stmt {
        public final List<Stmt> statements;

        public Block(List<Stmt> statements) {
            this.statements = statements;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitBlockStmt(this);
        }
    }

    public static final class Expression extends Stmt {
        public final Expr expression;

        public Expression(Expr expression) {
            this.expression = expression;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitExpressionStmt(this);
        }
    }

    public static final class For extends Stmt {
        public final Token variable;
        public final Expr iterable;
        public final Stmt.Block body;

        public For(Token variable, Expr iterable, Stmt.Block body) {
            this.variable = variable;
            this.iterable = iterable;
            this.body = body;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitForStmt(this);
        }
    }

    public static final class Function extends Stmt {
        public final Token name;
        public final List<Token> params;
        public final Stmt.Block body;

        public Function(Token name, List<Token> params, Stmt.Block body) {
            this.name = name;
            this.params = params;
            this.body = body;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitFunctionStmt(this);
        }
    }

    public static final class If extends Stmt {
        public final Expr condition;
        public final Stmt.Block thenBranch;
        public final Stmt.Block elseBranch;

        public If(Expr condition, Stmt.Block thenBranch, Stmt.Block elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitIfStmt(this);
        }
    }

    public static final class Return extends Stmt {
        public final Token keyword;
        public final Expr value;

        public Return(Token keyword, Expr value) {
            this.keyword = keyword;
            this.value = value;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitReturnStmt(this);
        }
    }

    public static final class Var extends Stmt {
        public final Token name;
        public final boolean mutable;
        public final TypeAnnotation typeAnnotation;
        public final Expr initializer;

        public Var(Token name, boolean mutable, TypeAnnotation typeAnnotation, Expr initializer) {
            this.name = name;
            this.mutable = mutable;
            this.typeAnnotation = typeAnnotation;
            this.initializer = initializer;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitVarStmt(this);
        }
    }

    public static final class While extends Stmt {
        public final Expr condition;
        public final Stmt.Block body;

        public While(Expr condition, Stmt.Block body) {
            this.condition = condition;
            this.body = body;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitWhileStmt(this);
        }
    }
}
