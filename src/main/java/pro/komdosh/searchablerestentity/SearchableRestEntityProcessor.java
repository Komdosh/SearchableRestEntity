package pro.komdosh.searchablerestentity;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.auto.service.AutoService;
import org.springframework.core.annotation.Order;

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

    private static final String controllerTemplate = "generation/controller.mustache";
    private static final String repositoryTemplate = "generation/repository.mustache";
    private static final String dtoTemplate = "generation/dto.mustache";
    private static final String serviceTemplate = "generation/service.mustache";
    private static final String mapperTemplate = "generation/mapper.mustache";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                try {
                    String path = "";
                    if(annotation instanceof SearchableRestEntity){
                        path = ((SearchableRestEntity) annotation).path();
                    }
                    processEntity(element, path);
                } catch (IOException e) {
                    error(e);
                }
            }
        }
        return true;
    }

    private void processEntity(Element element, String path) throws IOException {
        if (isTypeElement(element)) {
            TypeElement typeElement = (TypeElement) element;
            EntityScope scope = createModel(typeElement, path);
            writeClass(element, scope, new DefaultMustacheFactory().compile(dtoTemplate), scope.getEntityDtoClassNameWithPackage());
            writeClass(element, scope, new DefaultMustacheFactory().compile(mapperTemplate), scope.getEntityMapperClassNameWithPackage());
            writeClass(element, scope, new DefaultMustacheFactory().compile(repositoryTemplate), scope.getRepositoryClassNameWithPackage());
            writeClass(element, scope, new DefaultMustacheFactory().compile(serviceTemplate), scope.getEntityServiceClassNameWithPackage());
            writeClass(element, scope, new DefaultMustacheFactory().compile(controllerTemplate), scope.getControllerClassNameWithPackage());
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
