package tundra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RuntimeError extends RuntimeException {
    public final Token token;
    public final String errorType;
    public final String expression;
    public final List<String> details;
    public final Provenance provenance;

    public RuntimeError(Token token, String message) {
        this(token, "RuntimeError", message, null, Collections.emptyList(), null);
    }

    public RuntimeError(Token token, String errorType, String message, String expression, List<String> details) {
        this(token, errorType, message, expression, details, null);
    }

    public RuntimeError(
            Token token,
            String errorType,
            String message,
            String expression,
            List<String> details,
            Provenance provenance
    ) {
        super(message);
        this.token = token;
        this.errorType = errorType;
        this.expression = expression;
        this.details = Collections.unmodifiableList(new ArrayList<>(details));
        this.provenance = provenance;
    }
}
