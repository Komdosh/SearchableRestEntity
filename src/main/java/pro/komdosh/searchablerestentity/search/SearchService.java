package pro.komdosh.searchablerestentity.search;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * Abstract search service class which provides find all functionality by {@link Specification<E>}
 *
 * @param <E> entity to search
 * @param <D> Dto, corresponding to the entity
 */
public abstract class SearchService<E, D> {

    protected abstract JpaSpecificationExecutor<E> getSearchRepository();

    protected abstract D entityToDto(E entity);

    protected abstract List<SearchCriteria> addRestrictions(List<SearchCriteria> criteriaList, Pageable pageable) throws NoSearchResultException;

    protected Page<D> findAll(List<SearchCriteria> criteria, Pageable pageable) {
        Specification<E> specification = new SearchSpecificationsBuilder<E>()
            .withAll(criteria)
            .build();

        final Page<E> entities = getSearchRepository().findAll(specification, pageable);

        return entities.map(this::entityToDto);
    }
}
