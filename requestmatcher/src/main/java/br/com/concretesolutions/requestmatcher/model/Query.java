package br.com.concretesolutions.requestmatcher.model;

public final class Query {

    public static Query of(String key, String value) {
        return new Query(key, value);
    }

    private final String key, value;

    private Query(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return "Query{" + key + '=' + value + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Query query = (Query) o;

        if (key != null ? !key.equals(query.key) : query.key != null) return false;
        return !(value != null ? !value.equals(query.value) : query.value != null);
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
