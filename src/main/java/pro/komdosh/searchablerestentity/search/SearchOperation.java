package pro.komdosh.searchablerestentity.search;

public enum SearchOperation {
    GREATER,
    LESS,
    GREATER_EQUALS,
    LESS_EQUALS,
    NOT_EQUAL,
    EQUALS,
    LIKE,
    LIKE_START,
    LIKE_END,

    IN,
    NOT_IN,
    JSON_LIKE,

    /**
     * Matches json elements.
     * <p>
     * For example, key="json->a" value="123"  will match {"a": 123}
     */
    JSON_CONTAINS,

    /**
     * Matches all json arrays that contain any of provided values ignoring case.
     * <p>
     * I.e. both arrays ["a"], ["b"] will match the given value ["a", "b"]
     */
    JSON_ARRAY_CONTAINS_ANY_IGNORE_CASE
}
