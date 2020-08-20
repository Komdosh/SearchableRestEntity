package pro.komdosh.searchablerestentity.search;

import lombok.NonNull;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Build request from list of {@link SearchCriteria}
 * Outer array items compose as <i>and</i> operation.
 *
 * @param <E> represents corresponding entity type.
 */
class SearchSpecificationsBuilder<E> {

    private final List<SearchCriteria> params;

    SearchSpecificationsBuilder() {
        params = new ArrayList<>();
    }

    SearchSpecificationsBuilder<E> withAll(@NonNull List<SearchCriteria> criteriaList) {
        params.addAll(criteriaList);
        return this;
    }

    SearchSpecificationsBuilder<E> with(@NonNull SearchCriteria criteria) {
        params.add(criteria);
        return this;
    }

    public Specification<E> build() {
        if (params.isEmpty()) {
            return null;
        }

        return compose(params, (specHolder, sc) -> specHolder.specification.and(sc));
    }

    private Specification<E> compose(List<SearchCriteria> criteriaList, BiFunction<SpecificationHolder, Specification<E>, Specification<E>> compose) {
        if (criteriaList == null || criteriaList.isEmpty()) {
            return null;
        }

        SpecificationHolder sh = new SpecificationHolder();

        criteriaList.forEach(a -> {
            Specification<E> spec = new SearchSpecification<>(a);

            if (a.getAnd() != null) {
                spec = spec.and(compose(a.getAnd(), (specHolder, sc) -> specHolder.specification.and(sc)));
            }
            if (a.getOr() != null) {
                spec = spec.or(compose(a.getOr(), (specHolder, sc) -> specHolder.specification.or(sc)));
            }
            if (sh.specification != null) {
                sh.specification = compose.apply(sh, spec);
            } else {
                sh.specification = spec;
            }
        });
        return sh.specification;
    }

    class SpecificationHolder {
        Specification<E> specification = null;
    }
}
