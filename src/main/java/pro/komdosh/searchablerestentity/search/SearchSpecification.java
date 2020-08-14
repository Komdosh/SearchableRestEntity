package pro.komdosh.searchablerestentity.search;

import lombok.Getter;
import lombok.NonNull;
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

@RequiredArgsConstructor
@Getter
@Slf4j
public class SearchSpecification<T> implements Specification<T> {

    private final transient SearchCriteria criteria;

    /**
     * Splits the criteria key by dots and joins the internal entities if needed.
     *
     * @param root the base entity type.
     * @param <X>  the type referenced by the resulted Path.
     * @return path for the field which could be located in the root type, or any internal types in the root.
     */
    @NonNull
    public static <X> Path<X> computeFieldPath(@NotNull Root<?> root, @NotNull String key) {
        if (key.contains(ENTITY_JSON_FIELD_DELIMITER)) {
            return root.get(key.split(ENTITY_JSON_FIELD_DELIMITER)[0]);
        }

        // If the key is non-composite, then return a field from the root type:
        if (!key.contains(".")) {
            return root.get(key);
        }

        // Otherwise we split the key by dots and process the internal entities:
        final StringTokenizer tokenizer = new StringTokenizer(key, ".");
        Join<?, ?> join = root.join(tokenizer.nextToken());

        while (tokenizer.hasMoreTokens()) {
            final String internalEntityField = tokenizer.nextToken();
            if (!tokenizer.hasMoreTokens()) {
                return join.get(internalEntityField);
            } else {
                join = join.join(internalEntityField);
            }
        }

        throw new IllegalStateException("Should not be thrown");
    }

    @Override
    public Predicate toPredicate(@Nonnull Root<T> root,
                                 @Nonnull CriteriaQuery<?> query,
                                 @Nonnull CriteriaBuilder builder) {
        try {
            return getPredicate(root, builder);
        } catch (IllegalArgumentException ex) {
            log.error(ex.getMessage(), ex);
            throw new IllegalStateException("Wrong search field " + criteria.getKey());
        }
    }

    @NonNull
    private Predicate getPredicate(@NotNull Root<T> root, @NotNull CriteriaBuilder builder) {
        Predicate predicate;

        switch (criteria.getOperation()) {
            case GREATER:
                if (criteria.getValue() instanceof Instant) {
                    predicate = builder.greaterThan(computeFieldPath(root), (Instant) criteria.getValue());
                } else {
                    predicate = builder.greaterThan(computeFieldPath(root), criteria.getValue().toString());
                }
                return predicate;

            case LESS:
                if (criteria.getValue() instanceof Instant) {
                    predicate = builder.lessThan(computeFieldPath(root), (Instant) criteria.getValue());
                } else {
                    predicate = builder.lessThan(computeFieldPath(root), criteria.getValue().toString());
                }
                return predicate;

            case EQUALS:
                Object criteriaValue = correctValueAccordingType(root);
                return builder.equal(computeFieldPath(root), criteriaValue);

            case LIKE:
                if (!(criteria.getValue() instanceof String)) {
                    final String message = String.format("Operation %s is applicable only for strings",
                        SearchOperation.LIKE.name());
                    throw new IllegalStateException(message);
                }
                return builder.like(computeFieldPath(root), "%" + criteria.getValue() + "%");

            case IN:
                if (!(criteria.getValue() instanceof Collection)) {
                    final String message = String.format("Operation %s is applicable only for arrays",
                        SearchOperation.IN.name());
                    throw new IllegalStateException(message);
                }
                return computeFieldPath(root).in((Collection<?>) criteria.getValue());

            case JSON_LIKE:
                if (!(criteria.getValue() instanceof String)) {
                    final String message = String.format("Operation %s is applicable only for strings",
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
                    builder.literal("%" + criteria.getValue() + "%"),
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

    @NonNull
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

    @NonNull
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

    protected Object correctValueAccordingType(@NotNull Root<T> root) {
        if (Set.of(".", SearchCriteria.ENTITY_JSON_FIELD_DELIMITER)
            .stream().anyMatch(t -> criteria.getKey().contains(t))) {
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

    @NonNull
    private <X> Path<X> computeFieldPath(@NotNull Root<T> root) {
        return computeFieldPath(root, criteria.getKey());
    }
}
