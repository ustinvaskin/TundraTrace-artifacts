package tundra;

public abstract class TypeAnnotation {
    public interface Visitor<R> {
        R visitNamedType(Named type);
        R visitListType(ListType type);
    }

    public abstract <R> R accept(Visitor<R> visitor);

    public static final class Named extends TypeAnnotation {
        public final Token name;

        public Named(Token name) {
            this.name = name;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitNamedType(this);
        }
    }

    public static final class ListType extends TypeAnnotation {
        public final Token name;
        public final TypeAnnotation elementType;

        public ListType(Token name, TypeAnnotation elementType) {
            this.name = name;
            this.elementType = elementType;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitListType(this);
        }
    }
}
