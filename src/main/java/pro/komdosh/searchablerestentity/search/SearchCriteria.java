package pro.komdosh.searchablerestentity.search;

import lombok.*;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class SearchCriteria {

    public static final String ENTITY_JSON_FIELD_DELIMITER = "->";

    @Nullable
    private List<SearchCriteria> and;
    @Nullable
    private List<SearchCriteria> or;

    @NotNull
    private String key;

    @NotNull
    private SearchOperation operation;

    @NotNull
    private Object value;

}
