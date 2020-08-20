package pro.komdosh.searchablerestentity.search;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.util.List;

/**
 * Search API Controller.
 * <p>
 * Consider controller C of base URL "/entities". If C extends given class,
 * there would be a search method available:
 * <ul>
 * <li>POST /entities/search with payload as {@link List<SearchCriteria>}</li>
 * </ul>
 *
 * @param <D> Dto, corresponding to the searched entity
 * @param <S> search service corresponding to the entity
 */
public abstract class SearchController<D, S extends SearchService<?, D>> {

    public static final String PAGE_DEFAULT = "0";
    public static final String SIZE_DEFAULT = "20";
    public static final int MAX_PAGE_SIZE = 100;

    private final S searchService;

    public SearchController(S searchService) {
        this.searchService = searchService;
    }

    @PostMapping(value = "/search")
    @ResponseBody
    public Page<D> search(@RequestBody List<@Valid SearchCriteria> criteriaList,
                          @RequestParam(required = false, defaultValue = PAGE_DEFAULT)
                          @PositiveOrZero int page,
                          @RequestParam(required = false, defaultValue = SIZE_DEFAULT)
                          @Positive @Max(MAX_PAGE_SIZE) int size,
                          Sort sort) {

        return searchService.findAll(criteriaList, PageRequest.of(page, size, sort));
    }
}
