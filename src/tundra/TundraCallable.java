package tundra;

import java.util.List;

public interface TundraCallable {
    int arity();
    Object call(Interpreter interpreter, List<Object> arguments);
}
