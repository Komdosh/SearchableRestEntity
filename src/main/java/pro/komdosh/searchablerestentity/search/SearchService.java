package pro.komdosh.searchablerestentity.search;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * Abstract base class for services willing to provide search functionality.
 * <p>
 * Skeletal implementation merely using {@link JpaSpecificationExecutor#findAll(Specification, Pageable)}.
 * If you need some effective filtering, e.g. for security purposes, you would need to override some methods.
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
