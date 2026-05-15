package tundra;

public final class TundraValue {
    public final Object data;
    public final Provenance provenance;

    public TundraValue(Object data, Provenance provenance) {
        this.data = data;
        this.provenance = provenance;
    }

    public TundraValue withProvenance(Provenance provenance) {
        return new TundraValue(data, provenance);
    }
}
