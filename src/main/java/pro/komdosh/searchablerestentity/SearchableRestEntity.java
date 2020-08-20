package pro.komdosh.searchablerestentity;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Generates service, repository and controller for entity
 * to create search POST endpoint for your Spring Boot application.
 */
@Documented
@Target(ElementType.TYPE)
public @interface SearchableRestEntity {

    /**
     * You can setup path prefix for search controller `{path}/search`
     * default path is `/api/v1/{entityName}/search`
     *
     * @return the path prefix for entity controller
     */
    String path() default "";

    /**
     * You can provide your own dto implementation, just put it on the same package
     * where do you have your entity and set this field to false
     * <code>@SearchableRestEntity(useEntityAsDto=false)</code>
     *
     * @return value that indicates create dto from entity or not
     */
    boolean useEntityAsDto() default true;

}
