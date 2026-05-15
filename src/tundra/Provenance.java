package tundra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class Provenance {
    public final String kind;
    public final String description;
    public final List<Provenance> parents;

    private Provenance(String kind, String description, List<Provenance> parents) {
        this.kind = kind;
        this.description = description;
        this.parents = Collections.unmodifiableList(new ArrayList<>(parents));
    }

    public static Provenance leaf(String kind, String description) {
        return new Provenance(kind, description, Collections.emptyList());
    }

    public static Provenance node(String kind, String description, Provenance... parents) {
        List<Provenance> parentList = new ArrayList<>();
        for (Provenance parent : parents) {
            if (parent != null) {
                parentList.add(parent);
            }
        }

        return new Provenance(kind, description, parentList);
    }

    public String originDescription() {
        Provenance origin = origin();
        return origin.description;
    }

    public String formatTree() {
        StringBuilder builder = new StringBuilder();
        appendTree(builder, "", true);
        return builder.toString();
    }

    public List<String> formatCompact() {
        List<String> lines = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        appendCompact(lines, seen);
        return lines;
    }

    private Provenance origin() {
        if (parents.isEmpty()) {
            return this;
        }

        return parents.get(0).origin();
    }

    private void appendTree(StringBuilder builder, String prefix, boolean root) {
        if (!root) {
            builder.append('\n');
        }
        builder.append(prefix).append("- ").append(description);

        for (Provenance parent : parents) {
            parent.appendTree(builder, prefix + "  ", false);
        }
    }

    private void appendCompact(List<String> lines, Set<String> seen) {
        if (isCompactKind() && isUsefulCompactDescription() && seen.add(description)) {
            lines.add(description);
        }

        for (Provenance parent : parents) {
            parent.appendCompact(lines, seen);
        }
    }

    private boolean isCompactKind() {
        return kind.equals("binding")
                || kind.equals("assignment")
                || kind.equals("field")
                || kind.equals("record-field")
                || kind.equals("builtin")
                || kind.equals("literal");
    }

    private boolean isUsefulCompactDescription() {
        if (description.startsWith("literal number 0")) {
            return false;
        }
        if (description.startsWith("literal number ")) {
            return true;
        }
        if (kind.equals("literal")) {
            return description.startsWith("literal string")
                    || description.startsWith("literal bool")
                    || description.equals("literal none");
        }
        if (kind.equals("binding") || kind.equals("assignment")) {
            return !description.contains(" came from {")
                    && !description.contains(" came from [");
        }
        if (kind.equals("record-field")) {
            return !description.contains(" came from {")
                    && !description.contains(" came from [");
        }
        if (kind.equals("builtin")) {
            return description.startsWith("parseNumber(");
        }

        return true;
    }
}
