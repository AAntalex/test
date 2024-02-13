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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.processing.*;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.persistence.*;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("com.antalex.db.annotation.ShardEntity")
@AutoService(Processor.class)
public class ShardedEntityProcessor extends AbstractProcessor {
    private static final String CLASS_REPOSITORY_POSTFIX = "$RepositoryImpl";
    private static final String CLASS_INTERCEPT_POSTFIX = "Interceptor$";
    private static final String TABLE_PREFIX = "T_";
    private static final String COLUMN_PREFIX = "C_";

    private Map<Element, ClassDto> annotatedClasses = new HashMap<>();

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        for (TypeElement annotation : set) {
            for (Element annotatedElement : roundEnvironment.getElementsAnnotatedWith(annotation)) {
                try {
                    ClassDto classDto = getClassDtoByElement(annotatedElement);
                    createInterceptorClass(classDto);
                    createRepositoryClass(classDto);
                } catch (IOException err) {
                    err.printStackTrace();
                }
            }
        }
        return true;
    }

    private ClassDto getClassDtoByElement(Element classElement) {
        ShardEntity shardEntity = classElement.getAnnotation(ShardEntity.class);
        if (shardEntity == null) {
            return null;
        }
        if (!annotatedClasses.containsKey(classElement)) {
            String elementName = classElement.getSimpleName().toString();

            Map<String, String> getters = ElementFilter.methodsIn(classElement.getEnclosedElements())
                    .stream()
                    .map(e -> e.getSimpleName().toString())
                    .filter(it -> it.startsWith("get"))
                    .collect(Collectors.toMap(String::toLowerCase, it -> it));

            Map<String, String> setters = ElementFilter.methodsIn(classElement.getEnclosedElements())
                    .stream()
                    .map(e -> e.getSimpleName().toString())
                    .filter(it -> it.startsWith("set"))
                    .collect(Collectors.toMap(String::toLowerCase, it -> it));

            annotatedClasses.put(
                    classElement,
                    ClassDto
                            .builder()
                            .targetClassName(elementName)
                            .tableName(
                                    Optional.ofNullable(classElement.getAnnotation(Table.class))
                                            .map(Table::name)
                                            .orElse(getTableName(classElement))
                            )
                            .classPackage(getPackage(classElement.asType().toString()))
                            .cluster(shardEntity.cluster())
                            .shardType(shardEntity.type())
                            .fields(
                                    ElementFilter.fieldsIn(classElement.getEnclosedElements())
                                            .stream()
                                            .map(
                                                    fieldElement ->
                                                            FieldDto
                                                                    .builder()
                                                                    .fieldName(fieldElement.getSimpleName().toString())
                                                                    .columnName(getColumnName(fieldElement))
                                                                    .isLinked(isLinkedField(fieldElement))
                                                                    .getter(this.findGetter(getters, fieldElement))
                                                                    .setter(this.findSetter(setters, fieldElement))
                                                                    .element(fieldElement)
                                                                    .build()
                                            )
                                            .collect(Collectors.toList())
                            )
                            .build()
            );
        }
        return annotatedClasses.get(classElement);
    }

    private boolean isLinkedField(Element element) {
        return isAnnotationPresent(element, OneToMany.class)
                && isAnnotationPresent(element, JoinColumn.class);
    }

    private String findGetter(Map<String, String> getters, Element element) {
        return Optional.ofNullable(
                getters.get("get" + element.getSimpleName().toString().toLowerCase())
        ).orElse(null);
    }

    private String findSetter(Map<String, String> setters, Element element) {
        return Optional.ofNullable(
                setters.get("set" + element.getSimpleName().toString().toLowerCase())
        ).orElse(null);
    }

    private static String getColumnName(Element element) {
        if (isAnnotationPresent(element, Transient.class)) {
            return null;
        }
        String columnName =
                Optional.ofNullable(element.getAnnotation(Column.class))
                        .map(Column::name)
                        .orElse(
                                Optional.ofNullable(element.getAnnotation(JoinColumn.class))
                                        .map(JoinColumn::name)
                                        .orElse(StringUtils.EMPTY)
                        );
        return StringUtils.isEmpty(columnName) ?
                COLUMN_PREFIX +
                        CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, element.getSimpleName().toString()) :
                columnName;
    }

    private static String getTableName(Element element) {
        return TABLE_PREFIX + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE,
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

    private String findSetterByLinkedColumn(FieldDto field) {
        ClassDto fieldClass = getClassDtoByElement(
                ((DeclaredType) ((DeclaredType) field.getElement().asType()).getTypeArguments().get(0)).asElement()
        );
        if (fieldClass == null || fieldClass.getFields() == null) {
            return null;
        }
        return fieldClass.getFields()
                .stream()
                .filter(it ->
                        field.getColumnName().equals(
                                Optional.ofNullable(it.getColumnName()).orElse(StringUtils.EMPTY)
                        ) && getClassByType(it.getElement().asType()) == Long.class
                )
                .map(FieldDto::getSetter)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private static Class<?> getClassByType(TypeMirror type) {
        try {
            return Class.forName(type.toString());
        } catch (ClassNotFoundException err) {
            return null;
        }
    }

    private static String getInsertSQL(ClassDto classDto) {
        String columns = "ID,SHARD_VALUE";
        String values = "?,?";
        for (int i = 0; i < classDto.getFields().size(); i++) {
            if (classDto.getFields().get(i).getColumnName() != null && !classDto.getFields().get(i).getIsLinked()) {
                columns = columns.concat(",").concat(classDto.getFields().get(i).getColumnName());
                values = values.concat(",?");
            }
        }
        return "INSERT INTO $$$." + classDto.getTableName() + " (" + columns + ") VALUES (" + values + ")";
    }

    private void createRepositoryClass(ClassDto classDto) throws IOException {
        if (classDto == null) {
            return;
        }
        String className = classDto.getTargetClassName() + CLASS_REPOSITORY_POSTFIX;
        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(className);
        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
            out.println("package " + classDto.getClassPackage() + ";");
            out.println();
            out.println(
                    "import " + classDto.getClassPackage() + "." +
                            classDto.getTargetClassName() + CLASS_INTERCEPT_POSTFIX + ";"
            );
            out.println("import " + ShardEntityRepository.class.getCanonicalName() + ";");
            out.println("import " + ShardEntityManager.class.getCanonicalName() + ";");
            out.println("import " + Component.class.getCanonicalName() + ";");
            out.println("import " + Autowired.class.getCanonicalName() + ";");
            out.println("import " + ShardType.class.getCanonicalName() + ";");
            out.println("import " + Cluster.class.getCanonicalName() + ";");
            out.println("import " + StorageAttributes.class.getCanonicalName() + ";");
            out.println("import " + ShardDataBaseManager.class.getCanonicalName() + ";");
            out.println();
            out.println("import " + Objects.class.getCanonicalName() + ";");
            out.println();
            out.println("@Component");
            out.println("public class " +
                    className +
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
            out.println(getConstructorCode(classDto, className));
            out.println();
            out.println(getNewEntityCode(classDto));
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
        }
    }

    private void createInterceptorClass(ClassDto classDto) throws IOException {
        if (classDto == null) {
            return;
        }
        String className = classDto.getTargetClassName() + CLASS_INTERCEPT_POSTFIX;
        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(className);
        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
            out.println("package " + classDto.getClassPackage() + ";");
            out.println();
            out.println("public class " + className + " extends " + classDto.getTargetClassName() + " {");
            out.println();
            out.println("}");
        }
    }

    private static String getConstructorCode(ClassDto classDto, String className) {
        return "    @Autowired\n" +
                "    " + className + "(ShardDataBaseManager dataBaseManager) {\n" +
                "       this.cluster = dataBaseManager.getCluster(String.valueOf(\"" + classDto.getCluster() +
                "\"));\n    }";
    }

    private static String getNewEntityCode(ClassDto classDto) {
        return "    @Override\n" +
                "    public " + classDto.getTargetClassName() + " newEntity(Class<" +
                classDto.getTargetClassName() + "> clazz) {\n" +
                "       return new " + classDto.getTargetClassName() + CLASS_INTERCEPT_POSTFIX + "();\n" +
                "    }";
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


    private String getGenerateDependentIdCode(ClassDto classDto) {
        return classDto.getFields()
                .stream()
                .filter(it -> Objects.nonNull(it.getGetter()))
                .map(field -> {
                    String code = "";
                    if (isAnnotationPresentByType(field.getElement().asType(), ShardEntity.class)) {
                        code = "        entityManager.generateId(entity." + field.getGetter() + "());\n";
                    }
                    if (isAnnotationPresentInArgument(field.getElement().asType(), ShardEntity.class)) {
                        code = "        entityManager.generateAllId(entity." + field.getGetter() + "());\n";
                        if (field.getIsLinked()) {
                            String linkedSetter = findSetterByLinkedColumn(field);
                            if (linkedSetter != null) {
                                code = code + "        entity." + field.getGetter() +
                                        "().forEach(it -> it." + linkedSetter + "(entity.getId()));\n";
                            }
                        }
                    }
                    return code;
                })
                .reduce(
                        "    @Override\n" +
                                "    public void generateDependentId(" + classDto.getTargetClassName() + " entity) {\n",
                        String::concat
                ) + "    }";
    }
}
