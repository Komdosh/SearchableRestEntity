package pro.komdosh.searchablerestentity.search;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;
import javax.persistence.criteria.*;
import javax.persistence.metamodel.Attribute;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.*;

import static pro.komdosh.searchablerestentity.search.SearchCriteria.ENTITY_JSON_FIELD_DELIMITER;

@Slf4j
@Getter
@RequiredArgsConstructor
public class SearchSpecification<T> implements Specification<T> {

    private final transient SearchCriteria criteria;
    private final String ONLY_STRINGS_ERROR = "Operation %s is applicable only for strings";

    /**
     * Splits the criteria key by dots and joins the internal entities if needed.
     *
     * @param root the base entity type.
     * @param <X>  the type referenced by the resulted Path.
     * @return path for the field which could be located in the root type, or any internal types in the root.
     */
    @Nonnull
    public static <X> Path<X> computeFieldPath(@Nonnull Root<?> root, @Nonnull String key) {
        if (key.contains(ENTITY_JSON_FIELD_DELIMITER)) {
            return root.get(key.split(ENTITY_JSON_FIELD_DELIMITER)[0]);
        }

        // If the key is non-composite, then return a field from the root type:
        if (!key.contains(".")) {
            return root.get(key);
        }

        // Otherwise we split the key by dots and process the internal entities:
        final StringTokenizer tokenizer = new StringTokenizer(key, ".");

        Join<?, ?> join = getOrCreateJoin(root, tokenizer.nextToken());

        while (tokenizer.hasMoreTokens()) {
            final String internalEntityField = tokenizer.nextToken();
            if (!tokenizer.hasMoreTokens()) {
                return join.get(internalEntityField);
            } else {
                join = getOrCreateJoin(join, internalEntityField);
            }
        }

        throw new IllegalStateException("There is wrong number of tokens");
    }

    private static Join<?, ?> getOrCreateJoin(From<?, ?> from, String attribute) {

        for (Join<?, ?> join : from.getJoins()) {

            boolean sameName = join.getAttribute().getName().equals(attribute);

            if (sameName) {
                return join;
            }
        }

        return from.join(attribute);
    }

    @Override
    public Predicate toPredicate(@Nonnull Root<T> root,
                                 @Nonnull CriteriaQuery<?> query,
                                 @Nonnull CriteriaBuilder builder) {
        try {
            query.distinct(true);
            return getPredicate(root, builder);
        } catch (IllegalArgumentException ex) {
            log.error(ex.getMessage(), ex);
            throw new IllegalStateException("Wrong search field " + criteria.getKey());
        }
    }

    @Nonnull
    private Predicate getPredicate(@Nonnull Root<T> root, @Nonnull CriteriaBuilder builder) {
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

            case NOT_EQUAL:
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
        if (Set.of(".", ENTITY_JSON_FIELD_DELIMITER).stream().anyMatch(t -> criteria.getKey().contains(t))) {
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
        return computeFieldPath(root, criteria.getKey());
    }
}

