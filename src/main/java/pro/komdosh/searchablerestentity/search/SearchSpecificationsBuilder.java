package pro.komdosh.searchablerestentity.search;

import lombok.NonNull;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class SearchSpecificationsBuilder<T> {

    private final List<SearchCriteria> params;

    SearchSpecificationsBuilder() {
        params = new ArrayList<>();
    }

    SearchSpecificationsBuilder<T> withAll(@NonNull List<SearchCriteria> criteriaList) {
        for (SearchCriteria criteria : criteriaList) {
            with(criteria);
        }
        return this;
    }

    private SearchSpecificationsBuilder<T> with(@NonNull SearchCriteria criteria) {
        params.add(criteria);
        return this;
    }

    public Specification<T> build() {
        if (params.isEmpty()) {
            return null;
        }

        List<Specification<T>> specs = params.stream()
                .map(SearchSpecification<T>::new)
                .collect(Collectors.toList());

        Specification<T> result = specs.get(0);

        for (int i = 1; i < params.size(); i++) {
            result = Specification.where(result).and(specs.get(i));
        }
        return result;
    }
}
