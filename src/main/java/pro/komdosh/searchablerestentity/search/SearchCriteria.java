package pro.komdosh.searchablerestentity.search;

import lombok.*;

import javax.validation.constraints.NotNull;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class SearchCriteria {

    public static final String ENTITY_JSON_FIELD_DELIMITER = "->";

    @NotNull
    private String key;

    @NotNull
    private SearchOperation operation;

    @NotNull
    private Object value;

}
