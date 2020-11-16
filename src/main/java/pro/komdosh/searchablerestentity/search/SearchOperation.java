package pro.komdosh.searchablerestentity.search;

/**
 * Supported search operations
 */
public enum SearchOperation {
    GREATER,
    LESS,
    GREATER_EQUALS,
    LESS_EQUALS,
    NOT_EQUALS,
    EQUALS,
    LIKE,
    LIKE_START,
    LIKE_END,

    EXCLUDE_IN,
    EXCLUDE_LIKE,

    IN,
    NOT_IN,
    JSON_LIKE,

    /**
     * Matches json contains elements.
     * <p>
     * Example request: {key: "json->name" value: "Mark", "operation":"JSON_CONTAINS"}
     * will match {"name": "Mark"}
     */
    JSON_CONTAINS,

    /**
     * Matches all json arrays that contain any of provided values ignoring case.
     * <p>
     * I.e. both arrays ["a"], ["b"] will match the given value ["a", "b"]
     */
    JSON_ARRAY_CONTAINS_ANY_IGNORE_CASE
}
