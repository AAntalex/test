package com.antalex.db.annotation.processors;

import com.antalex.db.annotation.ShardEntity;
import com.antalex.db.service.ShardEntityRepository;
import com.antalex.db.model.dto.ClassDto;
import com.antalex.db.model.dto.FieldDto;
import com.google.auto.service.AutoService;
import org.springframework.stereotype.Repository;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("ShardEntity")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class ShardedEntityProcessor extends AbstractProcessor {
    private static final String CLASS_POSTFIX = "RepositoryImpl$";

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        for (TypeElement annotation : set) {
            for (Element annotatedElement : roundEnvironment.getElementsAnnotatedWith(annotation)) {
                final String annotatedElementName = annotatedElement.getSimpleName().toString();
                final ShardEntity settings = annotatedElement.getAnnotation(ShardEntity.class);

                try {
                    writeBuilderFile(
                            ClassDto
                                    .builder()
                                    .className(annotatedElementName + CLASS_POSTFIX)
                                    .targetClassName(annotatedElementName)
                                    .classPackage(getPackage(annotatedElement.asType().toString()))
                                    .fields(
                                            annotatedElement.getEnclosedElements().stream()
                                                    .filter(this::isField)
                                                    .map(
                                                            element ->
                                                                    FieldDto
                                                                            .builder()
                                                                            .fieldName(element.getSimpleName().toString())
                                                                            .build()
                                                    )
                                                    .collect(Collectors.toList())
                                    )
                                    .build()
                    );
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    private boolean isField(Element element) {
        return element != null && element.getKind().isField();
    }

    private static String getPackage(String className) {
        return Optional.of(className.lastIndexOf('.'))
                .filter(it -> it > 0)
                .map(it -> className.substring(0, it))
                .orElse(null);
    }

    private void writeBuilderFile(ClassDto classDto) throws IOException {
        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(classDto.getClassName());
        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
            out.println("package " + classDto.getClassPackage() + ";");
            out.println();
            out.println("import " + ShardEntityRepository.class.getCanonicalName() + ";");
            out.println("import " + Repository.class.getCanonicalName() + ";");
            out.println();
            out.println("@Repository");
            out.println("public class " +
                    classDto.getClassName() +
                    " implements ShardEntityRepository<" +
                    classDto.getTargetClassName() + "> {"
            );
            out.println();
            out.println("    @Override");
            out.println("    public " + classDto.getTargetClassName() +
                    " save(" + classDto.getTargetClassName() + " entity) {"
            );
            out.println("       return null;");
            out.println("   }");
            out.println();

            out.println();
            out.println("}");
            out.println();
        }
    }
}
