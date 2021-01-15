package pro.komdosh.searchablerestentity.search;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;
import javax.persistence.criteria.*;
import javax.persistence.metamodel.Attribute;
import javax.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.*;

import static pro.komdosh.searchablerestentity.search.SearchCriteria.ENTITY_JSON_FIELD_DELIMITER;

@Slf4j
@Getter
@RequiredArgsConstructor
public class SearchSpecification<T> implements Specification<T> {

    private final static String ONLY_STRINGS_ERROR = "Operation %s is applicable only for strings";
    private final transient SearchCriteria criteria;

    /**
     * Splits the criteria key by dots and joins the internal entities if needed.
     *
     * @param root the base entity type.
     * @param <X>  the type referenced by the resulted Path.
     * @return path for the field which could be located in the root type, or any internal types in the root.
     */
    @Nonnull
    public static <X> Path<X> computeFieldPath(@Nonnull Root<?> root, @Nonnull String key, String alias) {
        if (key.contains(ENTITY_JSON_FIELD_DELIMITER)) {
            return root.get(key.split(ENTITY_JSON_FIELD_DELIMITER)[0]);
        }

        // If the key is non-composite, then return a field from the root type:
        if (!key.contains(".")) {
            return root.get(key);
        }

        // Otherwise we split the key by dots and process the internal entities:
        final StringTokenizer tokenizer = new StringTokenizer(key, ".");

        String joinRef = tokenizer.nextToken();
        From<?, ?> join = root;
        do {
            final String internalEntityField = tokenizer.nextToken();
            if (!tokenizer.hasMoreTokens()) {
                return getOrCreateJoin(join, joinRef, alias).get(internalEntityField);
            } else {
                join = getOrCreateJoin(join, joinRef, joinRef);

                joinRef = internalEntityField;
            }
        } while (tokenizer.hasMoreTokens());

        throw new IllegalStateException("There is wrong number of tokens");
    }

    private static Join<?, ?> getOrCreateJoin(From<?, ?> from, String attribute, String alias) {
        if (StringUtils.isBlank(alias)) {
            return from.join(attribute, JoinType.LEFT);
        }

        for (Join<?, ?> join : from.getJoins()) {

            boolean sameName = join.getAlias().equals(alias) && join.getJoinType() == JoinType.LEFT;

            if (sameName) {
                return join;
            }
        }

        final Join<?, ?> join = from.join(attribute, JoinType.LEFT);
        join.alias(alias);
        return join;
    }

    @Override
    public Predicate toPredicate(@Nonnull Root<T> root,
                                 @Nonnull CriteriaQuery<?> query,
                                 @Nonnull CriteriaBuilder builder) {
        try {
            query.distinct(true);
            return getPredicate(root, query, builder);
        } catch (IllegalArgumentException ex) {
            log.error(ex.getMessage(), ex);
            throw new IllegalStateException("Wrong search field " + criteria.getKey());
        }
    }

    @Nonnull
    private Predicate getPredicate(@Nonnull Root<T> root, @Nonnull CriteriaQuery<?> criteriaQuery, @Nonnull CriteriaBuilder builder) {
        Predicate predicate;

        final Object criteriaValue = criteria.getValue();

        switch (criteria.getOperation()) {
            case GREATER:
                if (criteriaValue instanceof Instant) {
                    predicate = builder.greaterThan(computeFieldPath(root), (Instant) criteriaValue);
                } else {
                    predicate = builder.greaterThan(computeFieldPath(root), criteriaValue.toString());
                }
                return predicate;

            case LESS:
                if (criteriaValue instanceof Instant) {
                    predicate = builder.lessThan(computeFieldPath(root), (Instant) criteriaValue);
                } else {
                    predicate = builder.lessThan(computeFieldPath(root), criteriaValue.toString());
                }
                return predicate;
            case GREATER_EQUALS:
                if (criteriaValue instanceof Instant) {
                    predicate = builder.greaterThanOrEqualTo(computeFieldPath(root), (Instant) criteriaValue);
                } else {
                    predicate = builder.greaterThanOrEqualTo(computeFieldPath(root), criteriaValue.toString());
                }
                return predicate;

            case LESS_EQUALS:
                if (criteriaValue instanceof Instant) {
                    predicate = builder.lessThanOrEqualTo(computeFieldPath(root), (Instant) criteriaValue);
                } else {
                    predicate = builder.lessThanOrEqualTo(computeFieldPath(root), criteriaValue.toString());
                }
                return predicate;

            case EQUALS:
                return builder.equal(computeFieldPath(root), correctValueAccordingType(root));

            case NOT_EQUALS:
                return builder.notEqual(computeFieldPath(root), correctValueAccordingType(root));

            case LIKE:
                if (!(criteriaValue instanceof String)) {
                    final String message = String.format(ONLY_STRINGS_ERROR,
                        SearchOperation.LIKE.name());
                    throw new IllegalStateException(message);
                }
                return builder.like(computeFieldPath(root), "%" + criteriaValue + "%");
            case LIKE_START:
                if (!(criteriaValue instanceof String)) {
                    final String message = String.format(ONLY_STRINGS_ERROR,
                        SearchOperation.LIKE.name());
                    throw new IllegalStateException(message);
                }
                return builder.like(computeFieldPath(root), criteriaValue + "%");
            case LIKE_END:
                if (!(criteriaValue instanceof String)) {
                    final String message = String.format(ONLY_STRINGS_ERROR,
                        SearchOperation.LIKE.name());
                    throw new IllegalStateException(message);
                }
                return builder.like(computeFieldPath(root), "%" + criteriaValue);
            case IN:
                if (!(criteriaValue instanceof Collection)) {
                    final String message = String.format("Operation %s is applicable only for arrays",
                        SearchOperation.IN.name());
                    throw new IllegalStateException(message);
                }
                return computeFieldPath(root).in((Collection<?>) criteriaValue);

            case NOT_IN:
                if (!(criteriaValue instanceof Collection)) {
                    final String message = String.format("Operation %s is applicable only for arrays",
                        SearchOperation.IN.name());
                    throw new IllegalStateException(message);
                }
                return computeFieldPath(root).in((Collection<?>) criteriaValue).not();
            case EXCLUDE_IN:
                if (!(criteriaValue instanceof Collection)) {
                    final String message = String.format("Operation %s is applicable only for arrays",
                        SearchOperation.EXCLUDE_IN.name());
                    throw new IllegalStateException(message);
                }
                Subquery sq = criteriaQuery.subquery(criteriaQuery.getResultType());

                final Root<?> from = sq.from(computeFieldPath(root).getParentPath().getJavaType());

                Field field = Arrays.stream(from.getJavaType().getDeclaredFields())
                    .filter(t -> t.getType().equals(root.getJavaType())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("There is no %s field on class %s",
                        root.getJavaType(), from.getJavaType())));

                Join sqEmp = from.join(field.getName());
                sq.select(sqEmp).where(from.get(StringUtils.substringAfterLast(criteria.getKey(), ".")).in(criteriaValue));

                return builder.in(root).value(sq).not();
            case EXCLUDE_LIKE:
                if (!(criteriaValue instanceof String)) {
                    final String message = String.format("Operation %s is applicable only for strings",
                        SearchOperation.EXCLUDE_LIKE.name());
                    throw new IllegalStateException(message);
                }
                Subquery likeSubquery = criteriaQuery.subquery(criteriaQuery.getResultType());

                final Root<?> fromSubquery = likeSubquery.from(computeFieldPath(root).getParentPath().getJavaType());

                Field fieldSubquery = Arrays.stream(fromSubquery.getJavaType().getDeclaredFields())
                    .filter(t -> t.getType().equals(root.getJavaType())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("There is no %s field on class %s",
                        root.getJavaType(), fromSubquery.getJavaType())));

                Join sqJoin = fromSubquery.join(fieldSubquery.getName());
                likeSubquery.select(sqJoin).where(builder.like(fromSubquery.get(StringUtils.substringAfterLast(criteria.getKey(), ".")), "%" + criteriaValue + "%"));
                return builder.in(root).value(likeSubquery).not();
            case JSON_LIKE:
                if (!(criteriaValue instanceof String)) {
                    final String message = String.format(ONLY_STRINGS_ERROR,
                        SearchOperation.JSON_LIKE);
                    throw new IllegalStateException(message);
                }

                String jsonPath;
                int jsonColumnPropertySeparatorIndex = criteria.getKey().indexOf(ENTITY_JSON_FIELD_DELIMITER);

                jsonPath = "$." + criteria.getKey().substring(jsonColumnPropertySeparatorIndex + ENTITY_JSON_FIELD_DELIMITER.length());

                return builder.function(
                    "jsonSearchIgnoreCase",
                    String.class,
                    computeFieldPath(root),
                    builder.literal("%" + criteriaValue + "%"),
                    builder.literal(jsonPath)
                )
                    .isNotNull();

            case JSON_CONTAINS:
                return getJsonContainsPredicate(root, builder);

            case JSON_ARRAY_CONTAINS_ANY_IGNORE_CASE:
                return getJsonArrayContainsAnyPredicate(root, builder);
            default:
                throw new IllegalStateException("Unknown operation " + criteria.getOperation().name());
        }
    }

    @Nonnull
    private Predicate getJsonContainsPredicate(@NotNull Root<T> root, @NotNull CriteriaBuilder builder) {
        String jsonPath;
        int jsonColumnPropertySeparatorIndex = criteria.getKey().indexOf(ENTITY_JSON_FIELD_DELIMITER);

        int arrowLastCharPosition = jsonColumnPropertySeparatorIndex
            + ENTITY_JSON_FIELD_DELIMITER.length();
        jsonPath = "$." + criteria.getKey().substring(arrowLastCharPosition);

        return builder.function(
            "json_contains",
            Integer.class,
            computeFieldPath(root),
            builder.literal("\"" + criteria.getValue() + "\""),
            builder.literal(jsonPath)
        )
            .isNotNull();
    }

    @Nonnull
    private Predicate getJsonArrayContainsAnyPredicate(@NotNull Root<T> root,
                                                       @NotNull CriteriaBuilder builder) {
        if (!(criteria.getValue() instanceof List)) {
            final String message = String.format("Operation %s is applicable only for List",
                SearchOperation.JSON_ARRAY_CONTAINS_ANY_IGNORE_CASE);
            throw new IllegalStateException(message);
        }

        final List<?> listValue = (List<?>) criteria.getValue();

        if (listValue == null) {
            throw new IllegalStateException("Provided List must not be null");
        }

        Predicate[] predicates = new Predicate[listValue.size()];
        final Iterator<?> valuesIterator = listValue.iterator();
        for (int i = 0; i < listValue.size(); i++) {
            final Object value = valuesIterator.next();
            predicates[i] = createPredicateForJsonContains(builder, root, value);
        }
        return builder.or(predicates);
    }

    private Predicate createPredicateForJsonContains(CriteriaBuilder builder, Root<T> root, Object value) {
        return builder.equal(
            builder.function(
                "json_contains",
                Integer.class,
                builder.function(
                    "lower",
                    String.class,
                    computeFieldPath(root)
                ),
                builder.function(
                    "lower",
                    String.class,
                    builder.literal("\"" + value + "\"")
                ),
                builder.literal("$")
            ), 1);
    }

    protected Object correctValueAccordingType(@Nonnull Root<T> root) {
        if (criteria.getKey().contains(".") || criteria.getKey().contains(ENTITY_JSON_FIELD_DELIMITER)) {
            return criteria.getValue();
        }
        final Attribute<? super T, ?> attr = root.getModel().getAttributes()
            .stream().filter(attribute -> attribute.getName().equals(criteria.getKey())).findFirst()
            .orElseThrow(() -> new IllegalStateException(String.format("No field %s found", criteria.getKey())));
        Object criteriaValue = criteria.getValue();
        if (attr.getJavaType().isEnum() && criteriaValue instanceof String) {
            // Some fields can be enums. We have to make appropriate enum value from string
            @SuppressWarnings("unchecked") final Class<Enum> javaType = (Class<Enum>) attr.getJavaType();
            criteriaValue = Enum.valueOf(javaType, criteriaValue.toString());
        } else if (attr.getJavaType() == String.class && criteriaValue.getClass().isEnum()) {
            // we can also compare string fields with enum names
            criteriaValue = ((Enum<?>) criteriaValue).name();
        }
        return criteriaValue;
    }

    @Nonnull
    private <X> Path<X> computeFieldPath(@Nonnull Root<T> root) {
        return computeFieldPath(root, criteria.getKey(), criteria.getAlias());
    }
}

