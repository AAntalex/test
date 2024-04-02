package com.antalex.db.annotation.processors;

import com.antalex.db.annotation.ParentShard;
import com.antalex.db.annotation.ShardEntity;
import com.antalex.db.entity.abstraction.ShardInstance;
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
import com.antalex.db.service.api.ResultQuery;
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
import java.util.stream.IntStream;

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
        classDto.setColumnFields(
                classDto.getFields()
                        .stream()
                        .filter(field -> !field.getIsLinked() && Objects.nonNull(field.getColumnName()))
                        .collect(Collectors.toList())
        );
        IntStream.range(0, classDto.getColumnFields().size())
                .forEach(idx -> classDto.getColumnFields().get(idx).setColumnIndex(idx + 1));
        classDto.setUniqueFields(
                classDto.getColumnFields()
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

    private static String getFinalType(FieldDto field) {
        DeclaredType type = getDeclaredType(field.getElement());
        return type.getTypeArguments().size() > 0 ?
                ((DeclaredType) type.getTypeArguments().get(0)).asElement().getSimpleName().toString() :
                type.asElement().getSimpleName().toString();
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

    private FieldDto findFieldByLinkedColumn(FieldDto field) {
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
                        ) &&
                                (
                                        isAnnotationPresentByType(it, ShardEntity.class) ||
                                                getClassByType(it.getElement().asType()) == Long.class
                                )
                )
                .filter(it -> Objects.nonNull(it.getSetter()))
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

    private static Long getUniqueColumnsValueCode(ClassDto classDto) {
        long ret = 0L;
        for (int i = 0; i < classDto.getColumnFields().size(); i++) {
            if (classDto.getColumnFields().get(i).isUnique() && i <= Long.SIZE) {
                ret = ret | 1L << i;
            }
        }
        return ret;
    }

    private static String getInsertSQLCode(ClassDto classDto, boolean unique) {
        String columns = "SN,ST,SHARD_MAP";
        String values = "0,?,?";
        for (FieldDto field : unique ? classDto.getUniqueFields() : classDto.getColumnFields()) {
            if (Objects.nonNull(field.getGetter())) {
                columns = columns.concat(",").concat(field.getColumnName());
                values = values.concat(",?");
            }
        }
        return "INSERT INTO $$$." + classDto.getTableName() + " (" + columns + ",ID) VALUES (" + values + ",?)";
    }

    private static String getUpdateSQLCode(ClassDto classDto) {
        return classDto.getColumnFields()
                .stream()
                .filter(it -> Objects.nonNull(it.getGetter()))
                .map(field -> "," + field.getColumnName() + "=?")
                .reduce(
                        "UPDATE $$$." + classDto.getTableName() + " SET SN=SN+1,ST=?,SHARD_MAP=?",
                        String::concat
                ) + " WHERE ID=?";
    }

    private String getSelectSQLCode(ClassDto classDto) {
        String code = "SELECT " + getSelectList(classDto, "x0");
        String fromCode = " FROM $$$." + classDto.getTableName() + " x0";
        int idx = 0;
        for (FieldDto field : classDto.getColumnFields()) {
            if (isEagerField(field)) {
                ClassDto classDtoField = getClassDtoByElement(getDeclaredType(field.getElement()).asElement());
                if (Objects.nonNull(classDtoField)) {
                    idx++;
                    code = code.concat("," + getSelectList(classDtoField, "x" + idx));
                    fromCode = fromCode.concat(
                            " LEFT OUTER JOIN $$$." + classDtoField.getTableName() + " x" + idx +
                                    " ON x0." + field.getColumnName() + " = x" + idx + ".ID"
                    );
                }
            }
        }
        return code + fromCode + " WHERE x0.SHARD_MAP>=0";
    }

    private static String getSelectList(ClassDto classDto, String alias) {
        return classDto.getColumnFields()
                .stream()
                .filter(it -> Objects.nonNull(it.getSetter()))
                .map(field -> "," + alias + "." + field.getColumnName())
                .reduce(alias + ".ID," + alias + ".SHARD_MAP", String::concat);
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
            out.println(
                    getImportedTypes(
                            classDto,
                            new ArrayList<>(
                                    Arrays.asList(
                                            QueryType.class.getCanonicalName(),
                                            QueryStrategy.class.getCanonicalName(),
                                            ShardEntityRepository.class.getCanonicalName(),
                                            ShardEntityManager.class.getCanonicalName(),
                                            Component.class.getCanonicalName(),
                                            Autowired.class.getCanonicalName(),
                                            ShardType.class.getCanonicalName(),
                                            Cluster.class.getCanonicalName(),
                                            ShardDataBaseManager.class.getCanonicalName(),
                                            StorageContext.class.getCanonicalName(),
                                            Optional.class.getCanonicalName(),
                                            ResultQuery.class.getCanonicalName(),
                                            List.class.getCanonicalName(),
                                            ArrayList.class.getCanonicalName(),
                                            StringUtils.class.getCanonicalName(),
                                            IntStream.class.getCanonicalName(),
                                            Objects.class.getCanonicalName(),
                                            Arrays.class.getCanonicalName(),
                                            Map.class.getCanonicalName(),
                                            HashMap.class.getCanonicalName(),
                                            ShardInstance.class.getCanonicalName()
                                    )
                            )
                    )
            );
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
                    "    private static final String UPD_QUERY_PREFIX = \"UPDATE $$$." +
                            classDto.getTableName() + " SET SN=SN+1,ST=?,SHARD_MAP=?\";"
            );
            out.println(
                    "    private static final String INS_QUERY = \"" + getInsertSQLCode(classDto, false) + "\";"
            );
            out.println(
                    "    private static final String UPD_QUERY = \"" + getUpdateSQLCode(classDto) + "\";"
            );
            if (!classDto.getUniqueFields().isEmpty()) {
                out.println(
                        "    private static final String INS_UNIQUE_FIELDS_QUERY = \"" +
                                getInsertSQLCode(classDto, true) + "\";"
                );
            }
            out.println(
                    "    private static final String LOCK_QUERY = \"" + getLockSQLCode(classDto) + "\";"
            );
            out.println(
                    "    private static final String SELECT_QUERY = \"" + getSelectSQLCode(classDto) + "\";"
            );
            if (classDto.getUniqueFields().size() > 0) {
                out.println(
                        "    private static final Long UNIQUE_COLUMNS = " + getUniqueColumnsValueCode(classDto) + "L;"
                );

            }
            out.println();
            out.println(getColumnsCode(classDto));
            out.println("    private Map<Long, String> updateQueries = new HashMap<>();");


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
            out.println(getExtractValuesCode(classDto));
            out.println();
            out.println(getFindCode(classDto));
            out.println();
            out.println(getFindAllCode(classDto));
            out.println();
            out.println(getFindAllParentCode(classDto));
            out.println();
            out.println(getAdditionalPersistCode(classDto));
            out.println();
            out.println(getFindAllPrivateCode(classDto));
            out.println();
            out.println(getMethodUpdateSQLCode(classDto));
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
            out.println(
                    getImportedTypes(
                            classDto,
                            new ArrayList<>(
                                    Arrays.asList(
                                            ShardEntityManager.class.getCanonicalName(),
                                            Optional.class.getCanonicalName(),
                                            ShardInstance.class.getCanonicalName()
                                    )
                            )
                    )
            );
            out.println("public class " + className + " extends " + classDto.getTargetClassName() + " {");
            out.println("    private ShardEntityManager entityManager;\n" +
                    "    public void setEntityManager(ShardEntityManager entityManager) {\n" +
                    "        this.entityManager = entityManager;\n" +
                    "    }\n");
            out.println();
            out.println(getLazyFlagsCode(classDto));
            out.println(getGettersCode(classDto));
            out.println(getSettersCode(classDto));
            out.println("}");
        }
    }

    private static String getImportedTypes(ClassDto classDto, List<String> importedTypes) {
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

    private static boolean isLazyList(FieldDto field) {
        return Optional.ofNullable(field)
                .map(FieldDto::getElement)
                .map(it -> it.getAnnotation(OneToMany.class))
                .map(OneToMany::fetch)
                .filter(it -> it == FetchType.LAZY)
                .isPresent();
    }

    private static boolean isEagerField(FieldDto field) {
        return Optional.ofNullable(field)
                .map(FieldDto::getElement)
                .map(it -> it.getAnnotation(OneToOne.class))
                .map(OneToOne::fetch)
                .filter(it -> it == FetchType.EAGER)
                .isPresent();
    }
    private String getLazyFlagsCode(ClassDto classDto) {
        return classDto.getFields()
                .stream()
                .filter(field ->
                        field.getIsLinked() &&
                                Objects.nonNull(field.getGetter()) &&
                                !isAnnotationPresent(field.getElement(), Transient.class) &&
                                isLazyList(field)
                )
                .map(field -> "    private boolean " + field.getFieldName() + "Lazy = true;\n")
                .reduce(StringUtils.EMPTY, String::concat);
    }

    private String getGettersCode(ClassDto classDto) {
        return classDto.getFields()
                .stream()
                .filter(field ->
                        !isAnnotationPresent(field.getElement(), Transient.class) &&
                                Objects.nonNull(field.getGetter())
                )
                .map(field ->
                        "\n    @Override\n" +
                                "    public " + getTypeField(field) + " " + field.getGetter() + "() {\n" +
                                (
                                        field.getIsLinked() &&
                                                isAnnotationPresentInArgument(field, ShardEntity.class) ?
                                                (
                                                        isLazyList(field) ?
                                                                "        if (" + field.getFieldName() + "Lazy) {\n" +
                                                                        "            this." + field.getSetter() +
                                                                        "(entityManager.findAll(" +
                                                                        getFinalType(field) + ".class, " +
                                                                        (
                                                                                isAnnotationPresent(
                                                                                        field.getElement(),
                                                                                        ParentShard.class
                                                                                ) ?
                                                                                        "this, " :
                                                                                        StringUtils.EMPTY
                                                                        ) +
                                                                        "\"x0." +
                                                                        field.getColumnName() + "=?\", this.id));\n" +
                                                                        "            this." + field.getFieldName() +
                                                                        "Lazy = false;\n" +
                                                                        "        }\n" :
                                                                StringUtils.EMPTY
                                                ) :
                                                "        if (this.isLazy()) {\n" +
                                                        "            entityManager.find(this);\n" +
                                                        "        }\n"
                                ) +
                                "        return super." + field.getGetter() + "();\n" +
                                "    }"
                )
                .reduce(StringUtils.EMPTY, String::concat);
    }

    private static String getSettersCode(ClassDto classDto) {
        return classDto.getColumnFields()
                .stream()
                .filter(it -> Objects.nonNull(it.getSetter()))
                .map(field ->
                        "\n    public void " + field.getSetter() +
                                "(" + getTypeField(field) +
                                " value, boolean change) {\n" +
                                "        if (change) {\n" +
                                "            this.setChanges(" + field.getColumnIndex() + ");\n" +
                                "        }\n" +
                                "        super." + field.getSetter() + "(value);\n" +
                                "    }\n" +
                                "    @Override\n" +
                                "    public void " + field.getSetter() +
                                "(" + getTypeField(field) + " value) {\n" +
                                "        " + field.getSetter() + "(value, true);\n" +
                                "    }"
                )
                .reduce(StringUtils.EMPTY, String::concat);
    }

    private static String getColumnsCode(ClassDto classDto) {
        return classDto.getColumnFields()
                .stream()
                .map(field ->
                        (field.getColumnIndex() == 1 ? StringUtils.EMPTY : ",") +
                                "\n            \"" + field.getColumnName() + "\""
                )
                .reduce(
                        "    private static final List<String> COLUMNS = Arrays.asList(",
                        String::concat
                ) + "\n    );";
    }

    private static String getConstructorCode(ClassDto classDto, String className) {
        return "    @Autowired\n" +
                "    " + className + "(ShardDataBaseManager dataBaseManager) {\n" +
                "        this.cluster = dataBaseManager.getCluster(String.valueOf(\"" + classDto.getCluster() +
                "\"));\n    }";
    }

    private static String getMethodUpdateSQLCode(ClassDto classDto) {
        return "    private String getUpdateSQL(Long changes) {\n" +
                "        if (\n" +
                "                Optional.ofNullable(changes)\n" +
                "                .map(it -> it.equals(0L) && COLUMNS.size() <= Long.SIZE)\n" +
                "                .orElse(true)) \n" +
                "        {\n" +
                "            return null;\n" +
                "        }\n" +
                "        String sql = updateQueries.get(changes);\n" +
                "        if (Objects.isNull(sql)) {\n" +
                "            sql = IntStream.range(0, COLUMNS.size())\n" +
                "                    .filter(idx -> idx > Long.SIZE || (changes & (1L << idx)) > 0L)\n" +
                "                    .mapToObj(idx -> \",\" + updateQueries.get(idx) + \"=?\")\n" +
                "                    .reduce(UPD_QUERY_PREFIX, String::concat) + \" WHERE ID=?\";\n" +
                "            updateQueries.put(changes, sql);\n" +
                "        }\n" +
                "        return sql;\n" +
                "    }";
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
                "        " + classDto.getTargetClassName() + CLASS_INTERCEPT_POSTFIX +" entity = new " +
                classDto.getTargetClassName() + CLASS_INTERCEPT_POSTFIX + "();\n" +
                "        entity.setId(id);\n" +
                "        entity.setStorageContext(storageContext);\n" +
                "        entity.setEntityManager(entityManager);\n" +
                "        return entity;\n" +
                "    }";
    }

    private String getFindCode(ClassDto classDto) {
        return  "    @Override\n" +
                "    public " + classDto.getTargetClassName() + " find(" + classDto.getTargetClassName() +
                " entity) {\n" +
                "        try {\n" +
                "            " + classDto.getTargetClassName() + CLASS_INTERCEPT_POSTFIX + " entityInterceptor = (" +
                classDto.getTargetClassName() + CLASS_INTERCEPT_POSTFIX + ") entity;\n" +
                "            ResultQuery result = entityManager\n" +
                "                    .createQuery(entity, SELECT_QUERY + \" and x0.ID=?\", QueryType.SELECT," +
                " QueryStrategy.OWN_SHARD)\n" +
                "                    .bind(entity.getId())\n" +
                "                    .getResult();\n" +
                "            if (result.next()) {\n" +
                getProcessResultCode(classDto) +
                "            } else {\n" +
                "                return null;\n" +
                "            }\n" +
                "        } catch (Exception err) {\n" +
                "            throw new RuntimeException(err);\n" +
                "        }\n" +
                "        return entity;\n" +
                "    }";
    }

    private String getFindAllCode(ClassDto classDto) {
        return  "    @Override\n" +
                "    public List<" + classDto.getTargetClassName() +
                "> findAll(String condition, Object... binds) {\n" +
                "        return findAll(\n" +
                "                entityManager\n" +
                "                        .createQuery(\n" +
                "                                " + classDto.getTargetClassName() + ".class, \n" +
                "                                SELECT_QUERY +\n" +
                "                                        Optional.ofNullable(condition).map(it -> \" and \" + it)" +
                ".orElse(StringUtils.EMPTY),\n" +
                "                                QueryType.SELECT\n" +
                "                        )\n" +
                "                        .bindAll(binds)\n" +
                "                        .getResult()\n" +
                "        );\n" +
                "    }";
    }

    private String getFindAllParentCode(ClassDto classDto) {
        return  "    @Override\n" +
                "    public List<" + classDto.getTargetClassName() +
                "> findAll(ShardInstance parent, String condition, Object... binds) {\n" +
                "        return findAll(\n" +
                "                entityManager\n" +
                "                        .createQuery(\n" +
                "                                parent,\n" +
                "                                SELECT_QUERY +\n" +
                "                                        Optional.ofNullable(condition).map(it -> \" and \" + it)" +
                ".orElse(StringUtils.EMPTY),\n" +
                "                                QueryType.SELECT\n" +
                "                        )\n" +
                "                        .bindAll(binds)\n" +
                "                        .getResult()\n" +
                "        );\n" +
                "    }";
    }

    private String getFindAllPrivateCode(ClassDto classDto) {
        return  "    private List<" + classDto.getTargetClassName() +
                "> findAll(ResultQuery result) {\n" +
                "        List<" + classDto.getTargetClassName() + "> entities = new ArrayList<>();\n" +
                "        try {\n" +
                "            while (result.next()) {\n" +
                "                " + classDto.getTargetClassName() +
                " entity = entityManager.newEntity(" + classDto.getTargetClassName() + ".class, result.getLong(1));\n" +
                getProcessResultCode(classDto) +
                "                entities.add(entity);\n" +
                "            }\n" +
                "        } catch (Exception err) {\n" +
                "            throw new RuntimeException(err);\n" +
                "        }\n" +
                "        return entities;\n" +
                "    }";
    }

    private static int getCountSelectColumns(ClassDto classDto) {
        if (classDto == null) {
            return 0;
        }
        return (int) classDto.getColumnFields()
                .stream()
                .filter(it -> Objects.nonNull(it.getSetter()))
                .count() + 2;
    }

    private String getProcessResultCode(ClassDto classDto) {
        return classDto.getColumnFields()
                .stream()
                .filter(it -> Objects.nonNull(it.getSetter()) && isEagerField(it))
                .map(field ->
                        "                if (\n" +
                                "                        result.getLong(index+1) != 0L &&\n" +
                                "                                Optional.ofNullable(entity." + field.getGetter() +
                                "()).map(ShardInstance::isLazy).orElse(false))\n" +
                                "                {\n" +
                                "                    entityManager.extractValues(entity." + field.getGetter() +
                                "(), result, index);\n" +
                                "                    index = index + " +
                                getCountSelectColumns(
                                        getClassDtoByElement(getDeclaredType(field.getElement()).asElement())) + ";\n" +
                                "                }\n"
                )
                .reduce(
                        "                int index = 0;\n" +
                                "                extractValues(entity, result, index);\n" +
                                "                index = index + " + getCountSelectColumns(classDto) + ";\n",
                        String::concat
                );
    }

    private static String getExtractValuesCode(ClassDto classDto) {
        return classDto.getColumnFields()
                .stream()
                .filter(it -> Objects.nonNull(it.getSetter()))
                .map(field ->
                        "                entityInterceptor." + field.getSetter() +
                                (
                                        isAnnotationPresentByType(field, ShardEntity.class) ?
                                                "(entityManager.newEntity(" + getTypeField(field) +
                                                        ".class, result.getLong(++index)), false);\n" :
                                        "((" + getTypeField(field) + ") result.getObject(++index), false);\n"
                                )
                )
                .reduce(
                        "    @Override\n" +
                                "    public void extractValues(" + classDto.getTargetClassName() + " entity, " +
                                "ResultQuery result, int index) {\n" +
                                "        try {\n" +
                                "            if (result.getLong(++index) != 0L) {\n" +
                                "                " + classDto.getTargetClassName() + CLASS_INTERCEPT_POSTFIX +
                                " entityInterceptor = (" + classDto.getTargetClassName() + CLASS_INTERCEPT_POSTFIX +
                                ") entity;\n" +
                                "                entity.setShardMap(result.getLong(++index));\n",
                        String::concat
                ) +
                "                entity.getStorageContext().setLazy(false);\n" +
                "            }\n" +
                "        } catch (Exception err) {\n" +
                "            throw new RuntimeException(err);\n" +
                "        }\n" +
                "    }";
    }

    private static String getPersistCode(ClassDto classDto) {
        StringBuilder code = new StringBuilder(
                "    @Override\n" +
                        "    public void persist(" + classDto.getTargetClassName() + " entity, boolean onlyChanged) {\n"
        );
        StringBuilder persistCode =
                new StringBuilder(
                        "        String sql = entity.isStored() ? (onlyChanged ? getUpdateSQL(entity.getChanges())" +
                                " : UPD_QUERY) : INS_QUERY;\n" +
                                "        if (Objects.nonNull(sql)) {\n" +
                                "            boolean checkChanges = onlyChanged && entity.isStored();\n" +
                                "            entityManager\n" +
                                "                    .createQueries(entity, sql, QueryType.DML)\n" +
                                "                    .forEach(query ->\n" +
                                "                            query\n" +
                                "                                    .bind(entityManager.getTransactionUUID())\n" +
                                "                                    .bindShardMap(entity)\n"
                );

        StringBuilder childPersistCode = new StringBuilder(StringUtils.EMPTY);
        for (FieldDto field : classDto.getFields()) {
            if (Objects.nonNull(field.getGetter())) {
                if (isAnnotationPresentByType(field, ShardEntity.class)) {
                    code
                            .append("        entityManager.persist(entity.")
                            .append(field.getGetter())
                            .append("(), onlyChanged);\n");
                }
                if (isAnnotationPresentInArgument(field, ShardEntity.class)) {
                    childPersistCode
                            .append("        entityManager.persistAll(entity.")
                            .append(field.getGetter())
                            .append("(), onlyChanged);\n");
                }

                if (field.getColumnIndex() > 0) {
                    persistCode
                            .append("                                    .bind(entity.")
                            .append(field.getGetter())
                            .append(
                                    isAnnotationPresentByType(field, ShardEntity.class) ?
                                            "().getId()" :
                                            "()"
                            )
                            .append(", checkChanges && !entity.isChanged(")
                            .append(field.getColumnIndex())
                            .append("))\n");
                }
            }
        }
        return code
                .append(persistCode)
                .append(
                        "                                    .bind(entity.getId())\n" +
                                "                                    .addBatch()\n" +
                                "                    );\n" +
                                "        }\n" +
                                "        additionalPersist(entity);\n"
                )
                .append(childPersistCode)
                .append("    }")
                .toString();
    }

    private static String getAdditionalPersistCode(ClassDto classDto) {
        return classDto.getColumnFields()
                .stream()
                .filter(field -> Objects.nonNull(field.getGetter()))
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
                                "                                    .bindShardMap(entity)\n",
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
                                                                "().getId()" :
                                                                "()"
                                                ) + ", isUpdate && !entity.isChanged(" + field.getColumnIndex() + "))\n"
                                )
                                .reduce(
                                        "        boolean isUpdate = entity.isStored();\n" +
                                                "        if (!entity.hasMainShard() && (!isUpdate || " +
                                                "(entity.getChanges() & UNIQUE_COLUMNS) > 0L)) {\n" +
                                                "            entityManager\n" +
                                                "                    .createQuery(\n" +
                                                "                            entity,\n" +
                                                "                            isUpdate ?\n" +
                                                "                                    getUpdateSQL(entity.getChanges()" +
                                                " & UNIQUE_COLUMNS) :\n" +
                                                "                                    INS_UNIQUE_FIELDS_QUERY,\n" +
                                                "                            QueryType.DML,\n" +
                                                "                            QueryStrategy.MAIN_SHARD\n" +
                                                "                    )\n" +
                                                "                    .bind(entityManager.getTransactionUUID())\n" +
                                                "                    .bindShardMap(entity)\n",
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
                            FieldDto linkedField = findFieldByLinkedColumn(field);
                            if (Objects.nonNull(linkedField)) {
                                code = code + "        entity." + field.getGetter() + "()\n" +
                                        "                .stream()\n" +
                                        "                .filter(child -> \n" +
                                        "                        Optional.ofNullable(child." + linkedField.getGetter() + "())\n" +
                                        (
                                                isAnnotationPresentByType(linkedField, ShardEntity.class) ?
                                                        "                                .map(it -> it.getId())\n" :
                                                        StringUtils.EMPTY
                                        ) +
                                        "                                .map(it -> !it.equals(entity.getId()))\n" +
                                        "                                .orElse(true)\n" +
                                        "                )\n" +
                                        "                .forEach(it -> it." + linkedField.getSetter() + "(entity" +
                                        (
                                                isAnnotationPresentByType(linkedField, ShardEntity.class) ?
                                                        StringUtils.EMPTY :
                                                        ".getId()"
                                        ) + "));\n";
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
