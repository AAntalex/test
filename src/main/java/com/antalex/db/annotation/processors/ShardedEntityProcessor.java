package com.antalex.db.annotation.processors;

import com.antalex.db.annotation.DomainEntity;
import com.antalex.db.annotation.ParentShard;
import com.antalex.db.annotation.ShardEntity;
import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.StorageContext;
import com.antalex.db.model.dto.*;
import com.antalex.db.model.enums.QueryStrategy;
import com.antalex.db.model.enums.QueryType;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.service.ShardEntityManager;
import com.antalex.db.service.ShardEntityRepository;
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
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.persistence.*;
import javax.sql.rowset.serial.SerialClob;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SupportedAnnotationTypes({"com.antalex.db.annotation.ShardEntity", "com.antalex.db.annotation.DomainEntity"})
@AutoService(Processor.class)
public class ShardedEntityProcessor extends AbstractProcessor {
    private static final String CLASS_REPOSITORY_POSTFIX = "$RepositoryImpl";
    private static final String CLASS_INTERCEPT_POSTFIX = "Interceptor$";
    private static final String TABLE_PREFIX = "T_";
    private static final String COLUMN_PREFIX = "C_";

    private static Map<Element, EntityClassDto> entityClasses = new HashMap<>();
    private static Map<Element, DomainClassDto> domainClasses = new HashMap<>();

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        for (TypeElement annotation : set) {
            for (Element annotatedElement : roundEnvironment.getElementsAnnotatedWith(annotation)) {
                try {
                    if (annotatedElement.getAnnotation(ShardEntity.class) != null) {
                        EntityClassDto entityClassDto = getEntityClassDtoByElement(annotatedElement);
                        createInterceptorClass(entityClassDto);
                        createRepositoryClass(entityClassDto);
                    }
                    if (annotatedElement.getAnnotation(DomainEntity.class) != null) {
                        DomainClassDto domainClassDto = getDomainClassDtoByElement(annotatedElement);
                    }
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

    private Element getElement(DomainEntity domainEntity) {
        try {
            domainEntity.value();
        } catch (MirroredTypeException mte) {
            return ((DeclaredType) mte.getTypeMirror()).asElement();
        }
        return null;
    }

    private DomainClassDto getDomainClassDtoByElement(Element classElement) {
        DomainEntity domainEntity = classElement.getAnnotation(DomainEntity.class);
        if (domainEntity == null) {
            return null;
        }
        if (!domainClasses.containsKey(classElement)) {
            String elementName = classElement.getSimpleName().toString();

            EntityClassDto entityClass = Optional.ofNullable(getElement(domainEntity))
                    .map(this::getEntityClassDtoByElement)
                    .orElse(null);

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

            DomainClassDto domainClassDto = DomainClassDto
                    .builder()
                    .targetClassName(elementName)
                    .classPackage(getPackage(classElement.asType().toString()))
                    .entityClass(entityClass)
                    .fields(
                            ElementFilter.fieldsIn(classElement.getEnclosedElements())
                                    .stream()
                                    .map(
                                            fieldElement ->
                                                    DomainFieldDto
                                                            .builder()
                                                            .fieldName(fieldElement.getSimpleName().toString())
                                                            .getter(this.findGetter(getters, fieldElement))
                                                            .setter(this.findSetter(setters, fieldElement))
                                                            .element(fieldElement)
                                                            .build()
                                    )
                                    .collect(Collectors.toList())
                    )
                    .build();
            domainClasses.put(classElement, domainClassDto);
        }
        return domainClasses.get(classElement);
    }

    private EntityClassDto getEntityClassDtoByElement(Element classElement) {
        ShardEntity shardEntity = classElement.getAnnotation(ShardEntity.class);
        if (shardEntity == null) {
            return null;
        }
        if (!entityClasses.containsKey(classElement)) {
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

            EntityClassDto entityClassDto = EntityClassDto
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
                                                    EntityFieldDto
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

            normalizeClassDto(entityClassDto);
            entityClasses.put(classElement, entityClassDto);
        }
        return entityClasses.get(classElement);
    }

    private void normalizeClassDto(EntityClassDto entityClassDto) {
        entityClassDto.setFieldMap(entityClassDto.getFields()
                .stream()
                .collect(Collectors.toMap(EntityFieldDto::getFieldName, it -> it)));

        entityClassDto
                .getIndexes()
                .stream()
                .filter(IndexDto::getUnique)
                .map(IndexDto::getColumnList)
                .map(columnList -> columnList.replace(StringUtils.SPACE, StringUtils.EMPTY))
                .forEach(fields -> {
                    for (String fieldName : fields.split(",")) {
                        EntityFieldDto entityFieldDto = entityClassDto.getFieldMap().get(fieldName);
                        if (Objects.nonNull(entityFieldDto) &&
                                !"id".equals(fieldName) &&
                                Objects.nonNull(entityFieldDto.getGetter()) &&
                                Objects.nonNull(entityFieldDto.getColumnName()) &&
                                !entityFieldDto.getIsLinked())
                        {
                            entityFieldDto.setUnique(true);
                        }
                    }
                });
        entityClassDto.setColumnFields(
                entityClassDto.getFields()
                        .stream()
                        .filter(field -> !field.getIsLinked() && Objects.nonNull(field.getColumnName()))
                        .collect(Collectors.toList())
        );
        IntStream.range(0, entityClassDto.getColumnFields().size())
                .forEach(idx -> entityClassDto.getColumnFields().get(idx).setColumnIndex(idx + 1));
        entityClassDto.setUniqueFields(
                entityClassDto.getColumnFields()
                        .stream()
                        .filter(EntityFieldDto::isUnique)
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

    private static <A extends Annotation> boolean isAnnotationPresentByType(EntityFieldDto entityFieldDto, Class<A> annotation) {
        return Objects.nonNull(
                getDeclaredType(entityFieldDto.getElement())
                        .asElement()
                        .getAnnotation(annotation)
        );
    }

    private static String getTypeField(EntityFieldDto field) {
        DeclaredType type = getDeclaredType(field.getElement());
        return type.getTypeArguments().size() == 1 ?
                String.format(
                        "%s<%s>",
                        type.asElement().getSimpleName(),
                        ((DeclaredType) type.getTypeArguments().get(0)).asElement().getSimpleName()
                ) : type.asElement().getSimpleName().toString();
    }

    private static String getFinalType(EntityFieldDto field) {
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

    private static <A extends Annotation> boolean isAnnotationPresentInArgument(EntityFieldDto entityFieldDto, Class<A> annotation) {
        return Optional.ofNullable(getDeclaredType(entityFieldDto.getElement()))
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

    private EntityFieldDto findFieldByLinkedColumn(EntityFieldDto field) {
        EntityClassDto fieldClass = getEntityClassDtoByElement(
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

    private static Long getUniqueColumnsValueCode(EntityClassDto entityClassDto) {
        long ret = 0L;
        for (int i = 0; i < entityClassDto.getColumnFields().size(); i++) {
            if (entityClassDto.getColumnFields().get(i).isUnique() && i <= Long.SIZE) {
                ret = ret | 1L << i;
            }
        }
        return ret;
    }

    private static String getInsertSQLCode(EntityClassDto entityClassDto, boolean unique) {
        String columns = "SN,ST,SHARD_MAP";
        String values = "0,?,?";
        for (EntityFieldDto field : unique ? entityClassDto.getUniqueFields() : entityClassDto.getColumnFields()) {
            if (Objects.nonNull(field.getGetter())) {
                columns = columns.concat(",").concat(field.getColumnName());
                values = values.concat(",?");
            }
        }
        return "INSERT INTO $$$." + entityClassDto.getTableName() + " (" + columns + ",ID) VALUES (" + values + ",?)";
    }

    private static String getUpdateSQLCode(EntityClassDto entityClassDto) {
        return entityClassDto.getColumnFields()
                .stream()
                .filter(it -> Objects.nonNull(it.getGetter()))
                .map(field -> "," + field.getColumnName() + "=?")
                .reduce(
                        "UPDATE $$$." + entityClassDto.getTableName() + " SET SN=SN+1,ST=?,SHARD_MAP=?",
                        String::concat
                ) + " WHERE ID=?";
    }

    private String getSelectSQLCode(EntityClassDto entityClassDto) {
        String code = "SELECT " + getSelectList(entityClassDto, "x0");
        String fromCode = " FROM $$$." + entityClassDto.getTableName() + " x0";
        int idx = 0;
        for (EntityFieldDto field : entityClassDto.getColumnFields()) {
            if (isEagerField(field)) {
                EntityClassDto entityClassDtoField = getEntityClassDtoByElement(
                        getDeclaredType(field.getElement()).asElement()
                );
                if (Objects.nonNull(entityClassDtoField)) {
                    idx++;
                    code = code.concat("," + getSelectList(entityClassDtoField, "x" + idx));
                    fromCode = fromCode.concat(
                            " LEFT OUTER JOIN $$$." + entityClassDtoField.getTableName() + " x" + idx +
                                    " ON x0." + field.getColumnName() + " = x" + idx + ".ID"
                    );
                }
            }
        }
        return code + fromCode + " WHERE x0.SHARD_MAP>=0";
    }

    private static String getSelectList(EntityClassDto entityClassDto, String alias) {
        return entityClassDto.getColumnFields()
                .stream()
                .filter(it -> Objects.nonNull(it.getSetter()))
                .map(field -> "," + alias + "." + field.getColumnName())
                .reduce(alias + ".ID," + alias + ".SHARD_MAP", String::concat);
    }

    private static String getLockSQLCode(EntityClassDto entityClassDto) {
        return "SELECT ID FROM $$$." + entityClassDto.getTableName() + " WHERE ID=? FOR UPDATE NOWAIT";
    }

    private void createRepositoryClass(EntityClassDto entityClassDto) throws IOException {
        if (entityClassDto == null) {
            return;
        }
        String className = entityClassDto.getTargetClassName() + CLASS_REPOSITORY_POSTFIX;
        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(className);
        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
            out.println("package " + entityClassDto.getClassPackage() + ";");
            out.println();
            out.println(
                    "import " + entityClassDto.getClassPackage() + "." +
                            entityClassDto.getTargetClassName() + CLASS_INTERCEPT_POSTFIX + ";"
            );
            out.println(
                    getImportedTypes(
                            entityClassDto,
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
                                            ShardInstance.class.getCanonicalName(),
                                            SerialClob.class.getCanonicalName()
                                    )
                            )
                    )
            );
            out.println();
            out.println("@Component");
            out.println("public class " +
                    className +
                    " implements ShardEntityRepository<" +
                    entityClassDto.getTargetClassName() + "> {"
            );
            out.println(
                    "    private static final ShardType SHARD_TYPE = ShardType." + entityClassDto.getShardType().name() + ";"
            );

            out.println(
                    "    private static final String UPD_QUERY_PREFIX = \"UPDATE $$$." +
                            entityClassDto.getTableName() + " SET SN=SN+1,ST=?,SHARD_MAP=?\";"
            );
            out.println(
                    "    private static final String INS_QUERY = \"" + getInsertSQLCode(entityClassDto, false) + "\";"
            );
            out.println(
                    "    private static final String UPD_QUERY = \"" + getUpdateSQLCode(entityClassDto) + "\";"
            );
            if (!entityClassDto.getUniqueFields().isEmpty()) {
                out.println(
                        "    private static final String INS_UNIQUE_FIELDS_QUERY = \"" +
                                getInsertSQLCode(entityClassDto, true) + "\";"
                );
            }
            out.println(
                    "    private static final String LOCK_QUERY = \"" + getLockSQLCode(entityClassDto) + "\";"
            );
            out.println(
                    "    private static final String SELECT_QUERY = \"" + getSelectSQLCode(entityClassDto) + "\";"
            );
            if (entityClassDto.getUniqueFields().size() > 0) {
                out.println(
                        "    private static final Long UNIQUE_COLUMNS = " + getUniqueColumnsValueCode(entityClassDto) + "L;"
                );

            }
            out.println();
            out.println(getColumnsCode(entityClassDto));
            out.println("    private Map<Long, String> updateQueries = new HashMap<>();");


            out.println();
            out.println("    @Autowired");
            out.println("    private ShardEntityManager entityManager;");
            out.println("    private final Cluster cluster;");

            out.println();
            out.println(getConstructorCode(entityClassDto, className));
            out.println();
            out.println(getNewEntityCode(entityClassDto));
            out.println();
            out.println(getShardTypeCode());
            out.println();
            out.println(getClusterCode());
            out.println();
            out.println(getSetDependentStorageCode(entityClassDto));
            out.println();
            out.println(getPersistCode(entityClassDto));
            out.println();
            out.println(getGenerateDependentIdCode(entityClassDto));
            out.println();
            out.println(getLockCode(entityClassDto));
            out.println();
            out.println(getExtractValuesCode(entityClassDto));
            out.println();
            out.println(getFindCode(entityClassDto));
            out.println();
            out.println(getFindAllCode(entityClassDto));
            out.println();
            out.println(getFindAllParentCode(entityClassDto));
            out.println();
            out.println(getAdditionalPersistCode(entityClassDto));
            out.println();
            out.println(getFindAllPrivateCode(entityClassDto));
            out.println();
            out.println(getMethodUpdateSQLCode());
            out.println("}");
        }
    }

    private void createInterceptorClass(EntityClassDto entityClassDto) throws IOException {
        if (entityClassDto == null) {
            return;
        }
        String className = entityClassDto.getTargetClassName() + CLASS_INTERCEPT_POSTFIX;
        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(className);
        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
            out.println("package " + entityClassDto.getClassPackage() + ";");
            out.println();
            out.println(
                    getImportedTypes(
                            entityClassDto,
                            new ArrayList<>(
                                    Arrays.asList(
                                            ShardEntityManager.class.getCanonicalName(),
                                            Optional.class.getCanonicalName(),
                                            ShardInstance.class.getCanonicalName()
                                    )
                            )
                    )
            );
            out.println("public class " + className + " extends " + entityClassDto.getTargetClassName() + " {");
            out.println("    private ShardEntityManager entityManager;\n" +
                    "    public void setEntityManager(ShardEntityManager entityManager) {\n" +
                    "        this.entityManager = entityManager;\n" +
                    "    }\n");
            out.println(getLazyFlagsCode(entityClassDto));
            out.println(getInitCode(entityClassDto));
            out.println(getGettersCode(entityClassDto));
            out.println(getSettersCode(entityClassDto));
            out.println("}");
        }
    }

    private static String getImportedTypes(EntityClassDto entityClassDto, List<String> importedTypes) {
        entityClassDto.getFields()
                .stream()
                .filter(field ->
                        !isAnnotationPresent(field.getElement(), Transient.class))
                .map(EntityFieldDto::getElement)
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

    private static boolean isLazyList(EntityFieldDto field) {
        return Optional.ofNullable(field)
                .map(EntityFieldDto::getElement)
                .map(it -> it.getAnnotation(OneToMany.class))
                .map(OneToMany::fetch)
                .filter(it -> it == FetchType.LAZY)
                .isPresent();
    }

    private static boolean isEagerField(EntityFieldDto field) {
        return Optional.ofNullable(field)
                .map(EntityFieldDto::getElement)
                .map(it -> it.getAnnotation(OneToOne.class))
                .map(OneToOne::fetch)
                .filter(it -> it == FetchType.EAGER)
                .isPresent();
    }

    private static String getLazyFlagsCode(EntityClassDto entityClassDto) {
        return entityClassDto.getFields()
                .stream()
                .filter(field ->
                        field.getIsLinked() &&
                                Objects.nonNull(field.getGetter()) &&
                                !isAnnotationPresent(field.getElement(), Transient.class)
                )
                .map(field -> "    private boolean " + field.getFieldName() + "Lazy = false;\n")
                .reduce(StringUtils.EMPTY, String::concat);
    }

    private static String getInitCode(EntityClassDto entityClassDto) {
        return entityClassDto.getFields()
                .stream()
                .filter(field ->
                        field.getIsLinked() &&
                                Objects.nonNull(field.getGetter()) &&
                                !isAnnotationPresent(field.getElement(), Transient.class)
                )
                .map(field ->
                        "\n        this." + field.getFieldName() + "Lazy = true;" +
                                (
                                        !isLazyList(field) ?
                                                "\n        if (!this.isLazy()) {" +
                                                "\n            this." + field.getSetter() + "(entityManager.findAll(" +
                                                        getFinalType(field) + ".class, " +
                                                        (
                                                                isAnnotationPresent(
                                                                        field.getElement(),
                                                                        ParentShard.class
                                                                ) ? "this, " : StringUtils.EMPTY
                                                        ) + "\"x0." + field.getColumnName() + "=?\", this.id));\n" +
                                                        "            this." + field.getFieldName() + "Lazy = false;\n" +
                                                        "        }" :
                                                StringUtils.EMPTY
                                )
                )
                .reduce("    public void init() {", String::concat) + "\n    }" ;
    }

    private static String getGettersCode(EntityClassDto entityClassDto) {
        return entityClassDto.getFields()
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
                                                "        if (this.isLazy()) {\n" +
                                                        "            entityManager.find(this);\n" +
                                                        "        }\n"
                                ) +
                                "        return super." + field.getGetter() + "();\n" +
                                "    }"
                )
                .reduce(StringUtils.EMPTY, String::concat);
    }

    private static String getSettersCode(EntityClassDto entityClassDto) {
        return entityClassDto.getColumnFields()
                .stream()
                .filter(it -> Objects.nonNull(it.getSetter()))
                .map(field ->
                        "\n    public void " + field.getSetter() +
                                "(" + getTypeField(field) +
                                " value, boolean change) {\n" +
                                "        if (this.isLazy()) {\n" +
                                "            entityManager.find(this);\n" +
                                "        }\n" +
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

    private static String getColumnsCode(EntityClassDto entityClassDto) {
        return entityClassDto.getColumnFields()
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

    private static String getConstructorCode(EntityClassDto entityClassDto, String className) {
        return "    @Autowired\n" +
                "    " + className + "(ShardDataBaseManager dataBaseManager) {\n" +
                "        this.cluster = dataBaseManager.getCluster(String.valueOf(\"" + entityClassDto.getCluster() +
                "\"));\n    }";
    }

    private static String getMethodUpdateSQLCode() {
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
                "                    .mapToObj(idx -> \",\" + COLUMNS.get(idx) + \"=?\")\n" +
                "                    .reduce(UPD_QUERY_PREFIX, String::concat) + \" WHERE ID=?\";\n" +
                "            updateQueries.put(changes, sql);\n" +
                "        }\n" +
                "        return sql;\n" +
                "    }";
    }

    private static String getNewEntityCode(EntityClassDto entityClassDto) {
        return "    @Override\n" +
                "    public " + entityClassDto.getTargetClassName() + " newEntity() {\n" +
                "        return new " + entityClassDto.getTargetClassName() + CLASS_INTERCEPT_POSTFIX + "();\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public " + entityClassDto.getTargetClassName() + " getEntity(Long id, StorageContext" +
                " storageContext) {\n" +
                "        " + entityClassDto.getTargetClassName() + CLASS_INTERCEPT_POSTFIX +" entity = new " +
                entityClassDto.getTargetClassName() + CLASS_INTERCEPT_POSTFIX + "();\n" +
                "        entity.setId(id);\n" +
                "        entity.setStorageContext(storageContext);\n" +
                "        entity.setEntityManager(entityManager);\n" +
                "        entity.init();\n" +
                "        return entity;\n" +
                "    }";
    }

    private String getFindCode(EntityClassDto entityClassDto) {
        return  "    @Override\n" +
                "    public " + entityClassDto.getTargetClassName() + " find(" + entityClassDto.getTargetClassName() +
                " entity) {\n" +
                "        try {\n" +
                "            " + entityClassDto.getTargetClassName() + CLASS_INTERCEPT_POSTFIX + " entityInterceptor = (" +
                entityClassDto.getTargetClassName() + CLASS_INTERCEPT_POSTFIX + ") entity;\n" +
                "            ResultQuery result = entityManager\n" +
                "                    .createQuery(entity, SELECT_QUERY + \" and x0.ID=?\", QueryType.SELECT," +
                " QueryStrategy.OWN_SHARD)\n" +
                "                    .bind(entity.getId())\n" +
                "                    .getResult();\n" +
                "            if (result.next()) {\n" +
                getProcessResultCode(entityClassDto) +
                "            } else {\n" +
                "                return null;\n" +
                "            }\n" +
                "        } catch (Exception err) {\n" +
                "            throw new RuntimeException(err);\n" +
                "        }\n" +
                "        return entity;\n" +
                "    }";
    }

    private String getFindAllCode(EntityClassDto entityClassDto) {
        return  "    @Override\n" +
                "    public List<" + entityClassDto.getTargetClassName() +
                "> findAll(String condition, Object... binds) {\n" +
                "        return findAll(\n" +
                "                entityManager\n" +
                "                        .createQuery(\n" +
                "                                " + entityClassDto.getTargetClassName() + ".class, \n" +
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

    private String getFindAllParentCode(EntityClassDto entityClassDto) {
        return  "    @Override\n" +
                "    public List<" + entityClassDto.getTargetClassName() +
                "> findAll(ShardInstance parent, String condition, Object... binds) {\n" +
                "        if (parent.getStorageContext().getCluster() != this.cluster) {\n" +
                "            return findAll(condition, binds);\n" +
                "        }\n" +
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

    private String getFindAllPrivateCode(EntityClassDto entityClassDto) {
        return  "    private List<" + entityClassDto.getTargetClassName() +
                "> findAll(ResultQuery result) {\n" +
                "        List<" + entityClassDto.getTargetClassName() + "> entities = new ArrayList<>();\n" +
                "        try {\n" +
                "            while (result.next()) {\n" +
                "                " + entityClassDto.getTargetClassName() +
                " entity = entityManager.getEntity(" + entityClassDto.getTargetClassName() + ".class, result.getLong(1));\n" +
                getProcessResultCode(entityClassDto) +
                "                entities.add(entity);\n" +
                "            }\n" +
                "        } catch (Exception err) {\n" +
                "            throw new RuntimeException(err);\n" +
                "        }\n" +
                "        return entities;\n" +
                "    }";
    }

    private static int getCountSelectColumns(EntityClassDto entityClassDto) {
        if (entityClassDto == null) {
            return 0;
        }
        return (int) entityClassDto.getColumnFields()
                .stream()
                .filter(it -> Objects.nonNull(it.getSetter()))
                .count() + 2;
    }

    private String getProcessResultCode(EntityClassDto entityClassDto) {
        return entityClassDto.getColumnFields()
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
                                        getEntityClassDtoByElement(getDeclaredType(field.getElement()).asElement())) + ";\n" +
                                "                }\n"
                )
                .reduce(
                        "                int index = 0;\n" +
                                "                extractValues(entity, result, index);\n" +
                                "                index = index + " + getCountSelectColumns(entityClassDto) + ";\n",
                        String::concat
                );
    }

    private static String getResultObjectCode(EntityFieldDto field) {
        Class<?> clazz = getClassByType(field.getElement().asType());
        if (clazz != null) {
            if (clazz.isAssignableFrom(String.class)) {
                return "result.getString(++index)";
            }
            if (clazz.isAssignableFrom(Byte.class)) {
                return "result.getByte(++index)";
            }
            if (clazz.isAssignableFrom(Boolean.class)) {
                return "result.getBoolean(++index)";
            }
            if (clazz.isAssignableFrom(Short.class)) {
                return "result.getShort(++index)";
            }
            if (clazz.isAssignableFrom(Integer.class)) {
                return "result.getInteger(++index)";
            }
            if (clazz.isAssignableFrom(Long.class)) {
                return "result.getLong(++index)";
            }
            if (clazz.isAssignableFrom(Float.class)) {
                return "result.getFloat(++index)";
            }
            if (clazz.isAssignableFrom(Double.class)) {
                return "result.getDouble(++index)";
            }
            if (clazz.isAssignableFrom(BigDecimal.class)) {
                return "result.getBigDecimal(++index)";
            }
            if (clazz.isAssignableFrom(Date.class)) {
                return "result.getDate(++index)";
            }
            if (clazz.isAssignableFrom(Time.class)) {
                return "result.getTime(++index)";
            }
            if (clazz.isAssignableFrom(Timestamp.class)) {
                return "result.getTimestamp(++index)";
            }
            if (clazz.isAssignableFrom(Blob.class)) {
                return "result.getBlob(++index)";
            }
            if (clazz.isAssignableFrom(Clob.class)) {
                return "result.getClob(++index)";
            }
            if (clazz.isAssignableFrom(URL.class)) {
                return "result.getURL(++index)";
            }
            if (clazz.isAssignableFrom(SQLXML.class)) {
                return "result.getSQLXML(++index)";
            }
            if (clazz.isAssignableFrom(LocalDateTime.class)) {
                return "result.getLocalDateTime(++index)";
            }
        }
        return "result.getObject(++index, " + getTypeField(field) + ".class)";
    }

    private static String getExtractValuesCode(EntityClassDto entityClassDto) {
        return entityClassDto.getColumnFields()
                .stream()
                .filter(it -> Objects.nonNull(it.getSetter()))
                .map(field ->
                        "                entityInterceptor." + field.getSetter() +
                                (
                                        isAnnotationPresentByType(field, ShardEntity.class) ?
                                                "(entityManager.getEntity(" + getTypeField(field) +
                                                        ".class, result.getLong(++index)), false);\n" :
                                        "(" + getResultObjectCode(field) + ", false);\n"
                                )
                )
                .reduce(
                        "    @Override\n" +
                                "    public void extractValues(" + entityClassDto.getTargetClassName() + " entity, " +
                                "ResultQuery result, int index) {\n" +
                                "        try {\n" +
                                "            if (result.getLong(++index) != 0L) {\n" +
                                "                " + entityClassDto.getTargetClassName() + CLASS_INTERCEPT_POSTFIX +
                                " entityInterceptor = (" + entityClassDto.getTargetClassName() + CLASS_INTERCEPT_POSTFIX +
                                ") entity;\n" +
                                "                entity.setShardMap(result.getLong(++index));\n",
                        String::concat
                ) +
                "                entity.getStorageContext().setLazy(false);\n" +
                "                entityInterceptor.init();\n" +
                "            }\n" +
                "        } catch (Exception err) {\n" +
                "            throw new RuntimeException(err);\n" +
                "        }\n" +
                "    }";
    }

    private static String getPersistCode(EntityClassDto entityClassDto) {
        StringBuilder code = new StringBuilder(
                "    @Override\n" +
                        "    public void persist(" + entityClassDto.getTargetClassName() + " entity, boolean onlyChanged) {\n"
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
        for (EntityFieldDto field : entityClassDto.getFields()) {
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

    private static String getAdditionalPersistCode(EntityClassDto entityClassDto) {
        return entityClassDto.getColumnFields()
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
                        "    private void additionalPersist(" + entityClassDto.getTargetClassName() + " entity) {\n" +
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
                .concat(entityClassDto.getUniqueFields().isEmpty() ?
                        StringUtils.EMPTY :
                        entityClassDto.getUniqueFields()
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
                                                "        if (!entity.hasMainShard() && (!isUpdate || entity.isChanged" +
                                                "() && (entity.getChanges() & UNIQUE_COLUMNS) > 0L)) {\n" +
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

    private static String getClusterCode() {
        return "    @Override\n" +
                "    public Cluster getCluster() {\n" +
                "        return cluster;\n" +
                "    }";
    }

    private static String getShardTypeCode() {
        return "    @Override\n" +
                "    public ShardType getShardType() {\n" +
                "        return SHARD_TYPE;\n" +
                "    }";
    }

    private static String getSetDependentStorageCode(EntityClassDto entityClassDto) {
        return entityClassDto.getFields()
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
                                                    entityClassDto.getShardType() != ShardType.REPLICABLE ? "entity" : "null"
                                    ) +
                                    ");\n" :
                            ""
                )
                .reduce(
                        "    @Override\n" +
                                "    public void setDependentStorage(" + entityClassDto.getTargetClassName() + " entity) {\n",
                        String::concat
                ) + "    }";
    }


    private String getGenerateDependentIdCode(EntityClassDto entityClassDto) {
        return entityClassDto.getFields()
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
                            EntityFieldDto linkedField = findFieldByLinkedColumn(field);
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
                                "    public void generateDependentId(" + entityClassDto.getTargetClassName() + " entity) {\n",
                        String::concat
                ) + "    }";
    }

    private static String getLockCode(EntityClassDto entityClassDto) {
        return "    @Override\n" +
                "    public void lock(" + entityClassDto.getTargetClassName() + " entity) {\n" +
                "        entityManager\n" +
                "                .createQuery(entity, LOCK_QUERY, QueryType.LOCK, QueryStrategy.OWN_SHARD)\n" +
                "                .bind(entity.getId())\n" +
                "                .execute();\n" +
                "    }\n";
    }
}
