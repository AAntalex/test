package com.antalex.db.annotation.processors;

import com.antalex.db.annotation.ParentShard;
import com.antalex.db.annotation.ShardEntity;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.StorageContext;
import com.antalex.db.model.dto.IndexDto;
import com.antalex.db.model.enums.QueryStrategy;
import com.antalex.db.model.enums.QueryType;
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

    private List<IndexDto> getIndexes(Index[] indexes) {
        List<IndexDto> result = new ArrayList<>();
        for(Index index : indexes) {
            result.add(
                    IndexDto
                            .builder()
                            .name(index.name())
                            .unique(index.unique())
                            .columnList(index.columnList())
                            .build()
            );
        }
        return result;
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

            ClassDto classDto = ClassDto
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
                    .indexes(
                            Optional.ofNullable(classElement.getAnnotation(Table.class))
                                    .map(Table::indexes)
                                    .map(this::getIndexes)
                                    .orElse(Collections.emptyList())
                    )
                    .build();

            normalizeClassDto(classDto);
            annotatedClasses.put(classElement, classDto);
        }
        return annotatedClasses.get(classElement);
    }

    private void normalizeClassDto(ClassDto classDto) {
        classDto.setFieldMap(classDto.getFields()
                .stream()
                .collect(Collectors.toMap(FieldDto::getFieldName, it -> it)));

        classDto
                .getIndexes()
                .stream()
                .filter(IndexDto::getUnique)
                .map(IndexDto::getColumnList)
                .map(columnList -> columnList.replace(StringUtils.SPACE, StringUtils.EMPTY))
                .forEach(fields -> {
                    for (String fieldName : fields.split(",")) {
                        FieldDto fieldDto = classDto.getFieldMap().get(fieldName);
                        if (Objects.nonNull(fieldDto) &&
                                !"id".equals(fieldName) &&
                                Objects.nonNull(fieldDto.getGetter()) &&
                                Objects.nonNull(fieldDto.getColumnName()) &&
                                !fieldDto.getIsLinked())
                        {
                            fieldDto.setUnique(true);
                        }
                    }
                });
        classDto.setUniqueFields(
                classDto.getFields()
                        .stream()
                        .filter(FieldDto::isUnique)
                        .collect(Collectors.toList())
        );
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

    private static <A extends Annotation> boolean isAnnotationPresentByType(FieldDto fieldDto, Class<A> annotation) {
        return Objects.nonNull(
                getDeclaredType(fieldDto.getElement())
                        .asElement()
                        .getAnnotation(annotation)
        );
    }

    private static String getTypeField(FieldDto field) {
        DeclaredType type = getDeclaredType(field.getElement());
        return type.getTypeArguments().size() > 0 ?
                String.format(
                        "%s<%s>",
                        type.asElement().getSimpleName(),
                        ((DeclaredType) type.getTypeArguments().get(0)).asElement().getSimpleName()
                ) : type.asElement().getSimpleName().toString();
    }

    private static DeclaredType getDeclaredType(Element element) {
        return (DeclaredType) Optional.ofNullable(element)
                .map(Element::asType)
                .orElse(null);
    }

    private static <A extends Annotation> boolean isAnnotationPresentInArgument(FieldDto fieldDto, Class<A> annotation) {
        return Optional.ofNullable(getDeclaredType(fieldDto.getElement()))
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
                ((DeclaredType) getDeclaredType(field.getElement()).getTypeArguments().get(0)).asElement()
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

    private static String getInsertSQLCode(ClassDto classDto, boolean unique) {
        String columns = "SN,ST,SHARD_MAP";
        String values = "0,?,?";
        for (FieldDto field : unique ? classDto.getUniqueFields() : classDto.getFields()) {
            if (field.getColumnName() != null && !field.getIsLinked()) {
                columns = columns.concat(",").concat(field.getColumnName());
                values = values.concat(",?");
            }
        }
        return "INSERT INTO $$$." + classDto.getTableName() + " (" + columns + ",ID) VALUES (" + values + ",?)";
    }

    private static String getUpdateSQLCode(ClassDto classDto, boolean unique) {
        return (unique ? classDto.getUniqueFields() : classDto.getFields())
                .stream()
                .filter(it -> it.getColumnName() != null && !it.getIsLinked())
                .map(field -> "," + field.getColumnName() + "=?")
                .reduce(
                        "UPDATE $$$." + classDto.getTableName() + " SET SN=SN+1,ST=?,SHARD_MAP=?",
                        String::concat
                ) + " WHERE ID=?";
    }

    private static String getSelectSQLCode(ClassDto classDto) {
        int idx = 0;
        String alias = "x" + idx;
        return "SELECT " + getSelectList(classDto, alias) + " FROM $$$." +
                classDto.getTableName() + " " + alias + " WHERE " + alias + ".ID=?";
    }

    private static String getSelectList(ClassDto classDto, String alias) {
        return classDto.getFields()
                .stream()
                .filter(it -> it.getColumnName() != null && !it.getIsLinked())
                .map(field -> "," + alias + "." + field.getColumnName())
                .reduce(
                        alias + ".ID," + alias + ".SHARD_MAP",
                        String::concat
                );
    }

    private static String getLockSQLCode(ClassDto classDto) {
        return "SELECT ID FROM $$$." + classDto.getTableName() + " WHERE ID=? FOR UPDATE NOWAIT";
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
            out.println("import " + QueryType.class.getCanonicalName() + ";");
            out.println("import " + QueryStrategy.class.getCanonicalName() + ";");
            out.println("import " + ShardEntityRepository.class.getCanonicalName() + ";");
            out.println("import " + ShardEntityManager.class.getCanonicalName() + ";");
            out.println("import " + Component.class.getCanonicalName() + ";");
            out.println("import " + Autowired.class.getCanonicalName() + ";");
            out.println("import " + ShardType.class.getCanonicalName() + ";");
            out.println("import " + Cluster.class.getCanonicalName() + ";");
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
                    "    private static final String INS_QUERY = \"" + getInsertSQLCode(classDto, false) + "\";"
            );
            out.println(
                    "    private static final String UPD_QUERY = \"" + getUpdateSQLCode(classDto, false) + "\";"
            );
            if (!classDto.getUniqueFields().isEmpty()) {
                out.println(
                        "    private static final String INS_UNIQUE_FIELDS_QUERY = \"" +
                                getInsertSQLCode(classDto, true) + "\";"
                );
                out.println(
                        "    private static final String UPD_UNIQUE_FIELDS_QUERY = \"" +
                                getUpdateSQLCode(classDto, true) + "\";"
                );
            }
            out.println(
                    "    private static final String LOCK_QUERY = \"" + getLockSQLCode(classDto) + "\";"
            );
            out.println(
                    "    private static final String SELECT_QUERY = \"" + getSelectSQLCode(classDto) + "\";"
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
            out.println(getShardTypeCode(classDto));
            out.println();
            out.println(getClusterCode(classDto));
            out.println();
            out.println(getSetDependentStorageCode(classDto));
            out.println();
            out.println(getPersistCode(classDto));
            out.println();
            out.println(getGenerateDependentIdCode(classDto));
            out.println();
            out.println(getLockCode(classDto));
            out.println();
            out.println(getAdditionalPersistCode(classDto));
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
            out.println("import " + Optional.class.getCanonicalName() + ";");
            out.println("import " + StorageContext.class.getCanonicalName() + ";");
            out.println(getImportedTypes(classDto));
            out.println("public class " + className + " extends " + classDto.getTargetClassName() + " {");
            out.println(getGettersCode(classDto));
            out.println(getSettersCode(classDto));
            out.println("}");
        }
    }

    private static String getImportedTypes(ClassDto classDto) {
        List<String> importedTypes = new ArrayList<>();
        classDto.getFields()
                .stream()
                .filter(field ->
                        !isAnnotationPresent(field.getElement(), Transient.class))
                .map(FieldDto::getElement)
                .map(ShardedEntityProcessor::getDeclaredType)
                .forEach(type -> {
                    importedTypes.add(type.asElement().toString());
                    if (type.getTypeArguments().size() > 0) {
                        importedTypes.add(((DeclaredType) type.getTypeArguments().get(0)).asElement().toString());
                    }
                });
        return importedTypes.stream()
                .filter(it -> !it.startsWith("java.lang."))
                .distinct()
                .sorted()
                .map(it -> "import " + it + ";\n")
                .reduce(StringUtils.EMPTY, String::concat);
    }

    private static String getGettersCode(ClassDto classDto) {
        return classDto.getFields()
                .stream()
                .filter(field ->
                        !isAnnotationPresent(field.getElement(), Transient.class) &&
                                Objects.nonNull(field.getGetter())
                )
                .map(field ->
                        "\n   @Override\n" +
                                "   public " + getTypeField(field) + " " + field.getGetter() + "() {\n" +
                                "       return super." + field.getGetter() + "();\n" +
                                "   }"
                )
                .reduce(StringUtils.EMPTY, String::concat);
    }

    private static String getSettersCode(ClassDto classDto) {
        return classDto.getFields()
                .stream()
                .filter(field ->
                        !isAnnotationPresent(field.getElement(), Transient.class) &&
                                Objects.nonNull(field.getSetter())
                )
                .map(field ->
                        "\n   @Override\n" +
                                "   public void " + field.getSetter() + "(" + getTypeField(field) + " value) {\n" +
                                "       this.setChanged();\n" +
                                "       super." + field.getSetter() + "(value);\n" +
                                "   }"
                )
                .reduce(StringUtils.EMPTY, String::concat);
    }

    private static String getConstructorCode(ClassDto classDto, String className) {
        return "    @Autowired\n" +
                "    " + className + "(ShardDataBaseManager dataBaseManager) {\n" +
                "        this.cluster = dataBaseManager.getCluster(String.valueOf(\"" + classDto.getCluster() +
                "\"));\n    }";
    }

    private static String getNewEntityCode(ClassDto classDto) {
        return "    @Override\n" +
                "    public " + classDto.getTargetClassName() + " newEntity() {\n" +
                "        return new " + classDto.getTargetClassName() + CLASS_INTERCEPT_POSTFIX + "();\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public " + classDto.getTargetClassName() + " newEntity(Long id, StorageContext" +
                " storageContext) {\n" +
                "        " + classDto.getTargetClassName() + " entity = new " + classDto.getTargetClassName() +
                CLASS_INTERCEPT_POSTFIX + "();\n" +
                "        entity.setId(id);\n" +
                "        entity.setStorageContext(storageContext);\n" +
                "        return entity;\n" +
                "    }";
    }

    private static String getFindCode(ClassDto classDto) {
        return "    @Override\n" +
                "    public TestCShardEntity find(Long id, StorageContext storageContext) {\n" +
                "        return find(newEntity(id, storageContext));\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public TestCShardEntity find(TestCShardEntity entity) {\n" +
                "        try {\n" +
                "            ResultSet resultSet = (ResultSet) entityManager\n" +
                "                    .createQuery(entity, SELECT_QUERY, QueryType.SELECT, QueryStrategy.OWN_SHARD)\n" +
                "                    .bind(entity.getId())\n" +
                "                    .getResult();\n" +
                "            if (resultSet.next()) {\n" +
                "                entity.getStorageContext().setShardMap(resultSet.getLong(1));\n" +
                "                entity.setValue((String) resultSet.getObject(2));\n" +
                "                entity.setNewValue((String) resultSet.getObject(3));\n" +
                "                entity.setB((Long) resultSet.getObject(4));\n" +
                "            } else {\n" +
                "                return null;\n" +
                "            }\n" +
                "        } catch (SQLException err) {\n" +
                "            throw new RuntimeException(err);\n" +
                "        }\n" +
                "        return entity;\n" +
                "    }\n";
    }

    private static String getPersistCode(ClassDto classDto) {
        StringBuilder code = new StringBuilder(
                "    @Override\n" +
                        "    public void persist(" + classDto.getTargetClassName() + " entity) {\n"
        );
        StringBuilder persistCode =
                new StringBuilder(
                        "        entityManager\n" +
                                "                .createQueries(entity, entity.isStored() ? UPD_QUERY : INS_QUERY," +
                                " QueryType.DML)\n" +
                                "                .forEach(query ->\n" +
                                "                        query\n" +
                                "                                .bind(entityManager.getTransactionUUID())\n" +
                                "                                .bind(entity.getStorageContext().getShardMap())\n"
                );

        StringBuilder childPersistCode = new StringBuilder(StringUtils.EMPTY);
        for (FieldDto field : classDto.getFields()) {
            if (Objects.nonNull(field.getGetter())) {
                if (isAnnotationPresentByType(field, ShardEntity.class)) {
                    code
                            .append("        entityManager.persist(entity.")
                            .append(field.getGetter())
                            .append("());\n");
                }
                if (isAnnotationPresentInArgument(field, ShardEntity.class)) {
                    childPersistCode
                            .append("        entityManager.persistAll(entity.")
                            .append(field.getGetter())
                            .append("());\n");
                }

                if (Objects.nonNull(field.getColumnName()) && !field.getIsLinked()) {
                    persistCode
                            .append("                                .bind(entity.")
                            .append(field.getGetter())
                            .append(
                                    isAnnotationPresentByType(field, ShardEntity.class) ?
                                            "().getId())\n" :
                                            "())\n"
                            );
                }
            }
        }
        return code
                .append(persistCode)
                .append(
                        "                                .bind(entity.getId())\n" +
                                "                                .addBatch()\n" +
                                "                );\n" +
                                "        additionalPersist(entity);\n"
                )
                .append(childPersistCode)
                .append("    }")
                .toString();
    }

    private static String getAdditionalPersistCode(ClassDto classDto) {
        return classDto.getFields()
                .stream()
                .filter(field ->
                        Objects.nonNull(field.getGetter()) &&
                                Objects.nonNull(field.getColumnName()) &&
                                !field.getIsLinked()
                )
                .map(field ->
                        "                                    .bind(entity." +
                                field.getGetter() +
                                (
                                        isAnnotationPresentByType(field, ShardEntity.class) ?
                                                "().getId())\n" :
                                                "())\n"
                                )
                )
                .reduce(
                        "    private void additionalPersist(" + classDto.getTargetClassName() + " entity) {\n" +
                                "        if (entity.hasNewShards()) {\n" +
                                "            entityManager\n" +
                                "                    .createQueries(entity, INS_QUERY, QueryType.DML," +
                                " QueryStrategy.NEW_SHARDS)\n" +
                                "                    .forEach(query ->\n" +
                                "                            query\n" +
                                "                                    .bind(entityManager.getTransactionUUID())\n" +
                                "                                    .bind(entity.getStorageContext()." +
                                "getShardMap())\n",
                        String::concat
                )
                .concat(
                        "                                    .bind(entity.getId())\n" +
                                "                                    .addBatch()\n" +
                                "                    );\n" +
                                "        }\n"
                )
                .concat(classDto.getUniqueFields().isEmpty() ?
                        StringUtils.EMPTY :
                        classDto.getUniqueFields()
                                .stream()
                                .map(field ->
                                        "                    .bind(entity." +
                                                field.getGetter() +
                                                (
                                                        isAnnotationPresentByType(field, ShardEntity.class) ?
                                                                "().getId())\n" :
                                                                "())\n"
                                                )
                                )
                                .reduce(
                                        "        if (!entity.hasMainShard()) {\n" +
                                                "            entityManager\n" +
                                                "                    .createQuery(entity, entity.isStored()" +
                                                " ? UPD_UNIQUE_FIELDS_QUERY : INS_UNIQUE_FIELDS_QUERY, QueryType.DML" +
                                                ", QueryStrategy.MAIN_SHARD)\n" +
                                                "                    .bind(entityManager." +
                                                "getTransactionUUID())\n" +
                                                "                    .bind(entity.getStorageContext()." +
                                                "getShardMap())\n",
                                        String::concat
                                )
                                .concat(
                                        "                    .bind(entity.getId())\n" +
                                                "                    .addBatch();\n" +
                                                "        }\n"
                                )
                )
                .concat("    }");
    }

    private static String getClusterCode(ClassDto classDto) {
        return "    @Override\n" +
                "    public Cluster getCluster() {\n" +
                "        return cluster;\n" +
                "    }";
    }

    private static String getShardTypeCode(ClassDto classDto) {
        return "    @Override\n" +
                "    public ShardType getShardType() {\n" +
                "        return SHARD_TYPE;\n" +
                "    }";
    }

    private static String getSetDependentStorageCode(ClassDto classDto) {
        return classDto.getFields()
                .stream()
                .filter(it -> Objects.nonNull(it.getGetter()))
                .map(field ->
                    isAnnotationPresent(field.getElement(), ParentShard.class) ||
                            isAnnotationPresentByType(field, ShardEntity.class) ||
                            isAnnotationPresentInArgument(field, ShardEntity.class) ?
                            "        entityManager." +
                                    (isAnnotationPresentInArgument(field, ShardEntity.class) ?
                                            "setAllStorage" :
                                            "setStorage"
                                    ) + "(entity." + field.getGetter() + "(), " +
                                    (
                                            isAnnotationPresent(field.getElement(), ParentShard.class) &&
                                                    classDto.getShardType() != ShardType.REPLICABLE ? "entity" : "null"
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
                    if (isAnnotationPresentByType(field, ShardEntity.class)) {
                        code = "        entityManager.generateId(entity." + field.getGetter() + "());\n";
                    }
                    if (isAnnotationPresentInArgument(field, ShardEntity.class)) {
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

    private static String getLockCode(ClassDto classDto) {
        return "    @Override\n" +
                "    public void lock(" + classDto.getTargetClassName() + " entity) {\n" +
                "        entityManager\n" +
                "                .createQuery(entity, LOCK_QUERY, QueryType.LOCK, QueryStrategy.OWN_SHARD)\n" +
                "                .bind(entity.getId())\n" +
                "                .execute();\n" +
                "    }\n";
    }
}
