package pro.komdosh.searchablerestentity;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({"pro.komdosh.searchablerestentity.SearchableRestEntity"})
public class SearchableRestEntityProcessor extends AbstractProcessor {

    private static final String CONTROLLER_TEMPLATE = "generation/controller.mustache";
    private static final String REPOSITORY_TEMPLATE = "generation/repository.mustache";
    private static final String DTO_TEMPLATE = "generation/dto.mustache";
    private static final String SERVICE_TEMPLATE = "generation/service.mustache";
    private static final String MAPPER_TEMPLATE = "generation/mapper.mustache";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                try {
                    String path = "";
                    boolean useEntityAsDto = true;
                    if (annotation instanceof SearchableRestEntity) {
                        path = ((SearchableRestEntity) annotation).path();
                        useEntityAsDto = ((SearchableRestEntity) annotation).useEntityAsDto();
                    }
                    processEntity(element, path, useEntityAsDto);
                } catch (IOException e) {
                    error(e);
                }
            }
        }
        return true;
    }

    private void processEntity(Element element, String path, boolean useEntityAsDto) throws IOException {
        if (isTypeElement(element)) {
            TypeElement typeElement = (TypeElement) element;
            EntityScope scope = createModel(typeElement, path);
            if (useEntityAsDto) {
                writeClass(element, scope, new DefaultMustacheFactory().compile(DTO_TEMPLATE), scope.getEntityDtoClassNameWithPackage());
            }
            writeClass(element, scope, new DefaultMustacheFactory().compile(MAPPER_TEMPLATE), scope.getEntityMapperClassNameWithPackage());
            writeClass(element, scope, new DefaultMustacheFactory().compile(REPOSITORY_TEMPLATE), scope.getRepositoryClassNameWithPackage());
            writeClass(element, scope, new DefaultMustacheFactory().compile(SERVICE_TEMPLATE), scope.getEntityServiceClassNameWithPackage());
            writeClass(element, scope, new DefaultMustacheFactory().compile(CONTROLLER_TEMPLATE), scope.getControllerClassNameWithPackage());
        }
    }

    private void writeClass(Element element, EntityScope scope, Mustache template, String name) throws IOException {
        Filer filer = processingEnv.getFiler();
        JavaFileObject fileObject = filer.createSourceFile(name, element);
        try (Writer writer = fileObject.openWriter()) {
            template.execute(writer, scope);
        }
    }

    private EntityScope createModel(TypeElement element, String path) {
        String packageName = getPackageName(element);
        String sourceClassName = getSimpleNameAsString(element);

        return new EntityScope(packageName, sourceClassName, path);
    }

    private String getSimpleNameAsString(Element element) {
        return element.getSimpleName().toString();
    }

    private String getPackageName(TypeElement classElement) {
        return ((PackageElement) classElement.getEnclosingElement()).getQualifiedName().toString();
    }

    private boolean isTypeElement(Element element) {
        return element instanceof TypeElement;
    }

    private void error(IOException e) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "failed to write extension file: " + e.getMessage());
    }
}
