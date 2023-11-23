package com.antalex.annotation.processors;

import com.antalex.annotation.ShardEntity;
import com.antalex.model.dto.ClassDto;
import com.antalex.model.dto.FieldDto;
import com.google.auto.service.AutoService;

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

@SupportedAnnotationTypes("com.antalex.annotation.ShardEntity")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class ShardedEntityProcessor extends AbstractProcessor {
    private static final String CLASS_POSTFIX = "Tbl$";

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
            out.print("public class " + classDto.getClassName() + " {");
            out.println();
            out.println();

            out.println();
            out.println("}");
            out.println();
        }
    }
}
