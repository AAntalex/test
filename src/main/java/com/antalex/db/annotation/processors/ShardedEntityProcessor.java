package com.antalex.db.annotation.processors;

import com.antalex.db.annotation.ParentShard;
import com.antalex.db.annotation.ShardEntity;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.StorageAttributes;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.service.ShardEntityManager;
import com.antalex.db.service.ShardEntityRepository;
import com.antalex.db.model.dto.ClassDto;
import com.antalex.db.model.dto.FieldDto;
import com.google.auto.service.AutoService;
import com.google.common.base.CaseFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.processing.*;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.persistence.Column;
import javax.persistence.Table;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("com.antalex.db.annotation.ShardEntity")
@AutoService(Processor.class)
public class ShardedEntityProcessor extends AbstractProcessor {
    private static final String CLASS_POSTFIX = "RepositoryImpl$";
    private static final String TABLE_PREFIX = "T_";
    private static final String COLUMN_PREFIX = "C_";

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        for (TypeElement annotation : set) {
            for (Element annotatedElement : roundEnvironment.getElementsAnnotatedWith(annotation)) {
                final String annotatedElementName = annotatedElement.getSimpleName().toString();
                final ShardEntity shardEntity = annotatedElement.getAnnotation(ShardEntity.class);

                Map<String, String> getters = ElementFilter.methodsIn(annotatedElement.getEnclosedElements())
                        .stream()
                        .map(e -> e.getSimpleName().toString())
                        .filter(it -> it.startsWith("get"))
                        .collect(Collectors.toMap(String::toLowerCase, it -> it));

                try {
                    writeBuilderFile(
                            ClassDto
                                    .builder()
                                    .className(annotatedElementName + CLASS_POSTFIX)
                                    .targetClassName(annotatedElementName)
                                    .tableName(
                                            Optional.ofNullable(annotatedElement.getAnnotation(Table.class))
                                                    .map(Table::name)
                                                    .orElse(TABLE_PREFIX + annotatedElementName.toUpperCase())
                                    )
                                    .classPackage(getPackage(annotatedElement.asType().toString()))
                                    .cluster(shardEntity.cluster())
                                    .shardType(shardEntity.type())
                                    .fields(
                                            ElementFilter.fieldsIn(annotatedElement.getEnclosedElements())
                                                    .stream()
                                                    .map(
                                                            e ->
                                                                    FieldDto
                                                                            .builder()
                                                                            .fieldName(
                                                                                    e.getSimpleName().toString()
                                                                            )
                                                                            .columnName(
                                                                                    Optional.ofNullable(
                                                                                            e.getAnnotation(
                                                                                                    Column.class
                                                                                            )
                                                                                    )
                                                                                            .map(Column::name)
                                                                                            .orElse(getColumnName(e))
                                                                            )
                                                                            .getter(this.findGetter(getters, e))
                                                                            .element(e)
                                                                            .build()
                                                    )
                                                    .collect(Collectors.toList())
                                    )
                                    .build()
                    );
                } catch (IOException err) {
                    err.printStackTrace();
                }
            }
        }
        return true;
    }

    private String findGetter(Map<String, String> getters, Element element) {
        return Optional.ofNullable(
                getters.get("get" + element.getSimpleName().toString().toLowerCase())
        ).orElse(null);
    }

    private static String getColumnName(Element element) {
        return COLUMN_PREFIX + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE,
                element.getSimpleName().toString());
    }

    private static String getPackage(String className) {
        return Optional.of(className.lastIndexOf('.'))
                .filter(it -> it > 0)
                .map(it -> className.substring(0, it))
                .orElse(null);
    }

    private static <A extends Annotation> boolean isAnnotationPresent(Element element, Class<A> annotation) {
        return Optional.ofNullable(element.getAnnotation(annotation))
                .isPresent();
    }

    private static <A extends Annotation> boolean isAnnotationPresentByType(TypeMirror type, Class<A> annotation) {
        return Optional.ofNullable(type)
                .map(it -> (DeclaredType) type)
                .filter(it -> Objects.nonNull(it.asElement().getAnnotation(annotation)))
                .isPresent();
    }

    private static <A extends Annotation> boolean isAnnotationPresentInArgument(TypeMirror type, Class<A> annotation) {
        return Optional.ofNullable(type)
                .map(it -> (DeclaredType) type)
                .filter(it ->
                        it.getTypeArguments().size() > 0 &&
                                Objects.nonNull(
                                        ((DeclaredType) it.getTypeArguments().get(0))
                                                .asElement()
                                                .getAnnotation(annotation)
                                )
                )
                .isPresent();
    }

    private static String getInsertSQL(ClassDto classDto) {
        String sql = "INSERT INTO $$$." + classDto.getTableName() + " (";
        String columns = "ID,SHARD_VALUE,";
        String values = "?,?,";
        for (int i = 0; i < classDto.getFields().size(); i++) {
            columns = columns.concat(i == 0 ? "" : ",").concat(classDto.getFields().get(i).getColumnName());
            values = values.concat(i == 0 ? "?" : ",?");
        }
        return "INSERT INTO $$$." + classDto.getTableName() + " (" + columns + ") VALUES (" + values + ")";
    }

    private void writeBuilderFile(ClassDto classDto) throws IOException {
        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(classDto.getClassName());
        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
            out.println("package " + classDto.getClassPackage() + ";");
            out.println();
            out.println("import " + ShardEntityRepository.class.getCanonicalName() + ";");
            out.println("import " + ShardEntityManager.class.getCanonicalName() + ";");
            out.println("import " + Component.class.getCanonicalName() + ";");
            out.println("import " + Autowired.class.getCanonicalName() + ";");
            out.println("import " + Lazy.class.getCanonicalName() + ";");
            out.println("import " + ShardType.class.getCanonicalName() + ";");
            out.println("import " + Cluster.class.getCanonicalName() + ";");
            out.println("import " + StorageAttributes.class.getCanonicalName() + ";");
            out.println("import " + ShardDataBaseManager.class.getCanonicalName() + ";");
            out.println();
            out.println("import " + Objects.class.getCanonicalName() + ";");
            out.println();
            out.println("@Component");
            out.println("public class " +
                    classDto.getClassName() +
                    " implements ShardEntityRepository<" +
                    classDto.getTargetClassName() + "> {"
            );
            out.println(
                    "    private static final ShardType SHARD_TYPE = ShardType." + classDto.getShardType().name() + ";"
            );
            out.println(
                    "    private static final String INS_QUERY = \"" + getInsertSQL(classDto) + "\";"
            );

            out.println();
            out.println("    @Autowired");
            out.println("    private ShardEntityManager entityManager;");
            out.println("    private final Cluster cluster;");

            out.println();
            out.println(getConstructorCode(classDto));
            out.println();
            out.println(getSaveCode(classDto));
            out.println();
            out.println(getShardTypeCode(classDto));
            out.println();
            out.println(getClusterCode(classDto));
            out.println();
            out.println(getSetDependentStorageCode(classDto));
            out.println();
            out.println(getGenerateDependentIdCode(classDto));
            out.println("}");
            out.println();
        }
    }

    private static String getConstructorCode(ClassDto classDto) {
        return "    @Autowired\n" +
                "    " + classDto.getClassName() + "(ShardDataBaseManager dataBaseManager) {\n" +
                "       this.cluster = dataBaseManager.getCluster(String.valueOf(\"" + classDto.getCluster() +
                "\"));\n    }";
    }

    private static String getSaveCode(ClassDto classDto) {
        return "    @Override\n" +
                "    public " + classDto.getTargetClassName() +
                " save(" + classDto.getTargetClassName() + " entity) {\n" +
                "       entityManager.setStorage(entity, null, true);\n" +
                "       entityManager.generateId(entity, true);\n" +
                "       return entity;\n" +
                "   }";
    }

    private static String getClusterCode(ClassDto classDto) {
        return "    @Override\n" +
                "    public Cluster getCluster(" + classDto.getTargetClassName() + " entity) {\n" +
                "       return cluster;\n" +
                "    }";
    }

    private static String getShardTypeCode(ClassDto classDto) {
        return "    @Override\n" +
                "    public ShardType getShardType(" + classDto.getTargetClassName() + " entity) {\n" +
                "       return SHARD_TYPE;\n" +
                "    }";
    }

    private static String getSetDependentStorageCode(ClassDto classDto) {
        return classDto.getFields()
                .stream()
                .filter(it -> Objects.nonNull(it.getGetter()))
                .map(field ->
                    isAnnotationPresent(field.getElement(), ParentShard.class) ||
                            isAnnotationPresentByType(field.getElement().asType(), ShardEntity.class) ||
                            isAnnotationPresentInArgument(field.getElement().asType(), ShardEntity.class) ?
                            "        entityManager." +
                                    (isAnnotationPresentInArgument(field.getElement().asType(), ShardEntity.class) ?
                                            "setAllStorage" :
                                            "setStorage"
                                    ) + "(entity." + field.getGetter() + "(), " +
                                    (
                                            isAnnotationPresent(field.getElement(), ParentShard.class) ?
                                                    "entity.getStorageAttributes()" :
                                                    "null"
                                    ) +
                                    ");\n" :
                            ""
                )
                .reduce(
                        "    @Override\n" +
                                "    public void setDependentStorage(" + classDto.getTargetClassName() + " entity) {\n",
                        String::concat
                ) + "    }";
    }
    private static String getGenerateDependentIdCode(ClassDto classDto) {
        return classDto.getFields()
                .stream()
                .filter(it -> Objects.nonNull(it.getGetter()))
                .map(field ->
                                isAnnotationPresentByType(field.getElement().asType(), ShardEntity.class) ||
                                        isAnnotationPresentInArgument(field.getElement().asType(), ShardEntity.class) ?
                                        "        entityManager." +
                                                (
                                                        isAnnotationPresentInArgument(
                                                                field.getElement().asType(),
                                                                ShardEntity.class
                                                        ) ? "generateAllId" : "generateId"
                                                ) +
                                                "(entity." + field.getGetter() + "());\n" :
                                        ""
                )
                .reduce(
                        "    @Override\n" +
                                "    public void generateDependentId(" + classDto.getTargetClassName() + " entity) {\n",
                        String::concat
                ) + "    }";
    }
}
