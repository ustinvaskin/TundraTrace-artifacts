package tundra;

class ReturnSignal extends RuntimeException {
    final Token keyword;
    final Object value;

    ReturnSignal(Token keyword, Object value) {
        super(null, null, false, false);
        this.keyword = keyword;
        this.value = value;
    }
}
