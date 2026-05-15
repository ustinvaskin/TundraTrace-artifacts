package tundra;

import java.util.List;

public class TundraFunction implements TundraCallable {
    private final Stmt.Function declaration;
    private final Environment closure;

    public TundraFunction(Stmt.Function declaration, Environment closure) {
        this.declaration = declaration;
        this.closure = closure;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(closure);
        for (int i = 0; i < declaration.params.size(); i++) {
            environment.define(declaration.params.get(i).lexeme, arguments.get(i), false);
        }

        try {
            interpreter.executeBlock(declaration.body.statements, environment);
        } catch (ReturnSignal signal) {
            return signal.value;
        }

        return interpreter.value(null, Provenance.leaf("function", declaration.name.lexeme + " returned none"));
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }
}
