package com.antalex.annotation.processors;

import com.antalex.annotation.ShardEntity;
import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.Set;

@SupportedAnnotationTypes("com.antalex.annotation.ShardEntity")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class ShardedEntityProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        for (TypeElement annotation : set) {
            for (Element annotatedElement : roundEnvironment.getElementsAnnotatedWith(annotation)) {
                final TypeMirror mirror = annotatedElement.asType();
                final String annotatedElementName = annotatedElement.getSimpleName().toString();
                final ShardEntity settings = annotatedElement.getAnnotation(ShardEntity.class);

                /*
                final String newClassName = annotatedElementName + settings.postfix();
                final Set fields = annotatedElement.getEnclosedElements().stream()
                        .filter(this::isField)
                        .map(
                                element -> {
                                    final String fieldName = element.getSimpleName().toString();
                                    final String fieldStringName = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, fieldName);
                                    return FieldDto.of(fieldStringName, fieldName);
                                }
                        ).collect(Collectors.toSet());

                final ClassDto newClass = new ClassDto();
                newClass.setClassName(newClassName);
                newClass.setFields(fields);
                newClass.setClassPackage(getPackage(mirror));
                ClassCreator.record(newClass, processingEnv);
*/
/*
                try {
                    writeBuilderFile(className, stringGetters);
                } catch (IOException e) {
                    e.printStackTrace();
                }
*/
            }
        }
        return true;
    }
/*
    private void writeBuilderFile(String className) throws IOException {

        String packageName = null;
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0) {
            packageName = className.substring(0, lastDot);
        }

        String simpleClassName = className.substring(lastDot + 1);
        String toStringsClassName = "ToStrings";

        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(toStringsClassName);
        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {

            if (packageName != null) {
                out.print("package ");
                out.print(packageName);
                out.println(";");
                out.println();
            }

            out.print("public class ");
            out.print(toStringsClassName);
            out.println(" {");
            out.println();

            out.print(" public static String toString("+simpleClassName+" cat){");
            out.println();
            out.print(" return ");

            String result = getters.stream().map(m -> "cat." + m + "()").collect(Collectors.joining("+\",\"+"));
            out.println(result + ";");

            out.println("    }");
            out.println("}");

        }
    }
    */
}
