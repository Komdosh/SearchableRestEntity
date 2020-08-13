package pro.komdosh.searchablerestentity.search;

public class NoSearchResultException extends Exception {

    private NoSearchResultException() {
        super("No search result");
    }

    public static NoSearchResultException getInstance() {
        return Holder.INSTANCE;
    }

    private static class Holder {
        private static final NoSearchResultException INSTANCE = new NoSearchResultException();
    }
}
