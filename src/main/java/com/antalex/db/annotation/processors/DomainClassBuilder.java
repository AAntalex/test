package com.antalex.db.annotation.processors;

import com.antalex.db.annotation.*;
import com.antalex.db.domain.abstraction.Domain;
import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.dto.*;
import com.antalex.db.model.enums.MappingType;
import com.antalex.db.service.DomainEntityManager;
import com.antalex.db.service.DomainEntityMapper;
import com.antalex.db.service.ShardEntityManager;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.util.ElementFilter;
import javax.persistence.Transient;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

public class DomainClassBuilder {
    private static Map<Element, DomainClassDto> domainClasses = new HashMap<>();

    public static DomainClassDto getClassDtoByElement(Element classElement) {
        DomainEntity domainEntity = classElement.getAnnotation(DomainEntity.class);
        if (domainEntity == null) {
            return null;
        }
        if (!domainClasses.containsKey(classElement)) {
            String elementName = classElement.getSimpleName().toString();

            EntityClassDto entityClass = Optional.ofNullable(getElement(domainEntity))
                    .map(EntityClassBuilder::getClassDtoByElement)
                    .orElse(null);

            Map<String, String> getters = ProcessorUtils.getMethodsByPrefix(classElement, "get");
            Map<String, String> setters = ProcessorUtils.getMethodsByPrefix(classElement, "set");
            StorageDto mainStorage = getMainStorage(elementName, domainEntity);
            Map<String, StorageDto> storageDtoMap = getStorageMap(mainStorage, domainEntity);

            DomainClassDto domainClassDto = DomainClassDto
                    .builder()
                    .targetClassName(elementName)
                    .classPackage(ProcessorUtils.getPackage(classElement.asType().toString()))
                    .entityClass(entityClass)
                    .storage(mainStorage)
                    .classElement(classElement)
                    .storageMap(storageDtoMap)
                    .fields(
                            ElementFilter.fieldsIn(classElement.getEnclosedElements())
                                    .stream()
                                    .map(
                                            fieldElement ->
                                                    DomainFieldDto
                                                            .builder()
                                                            .fieldName(fieldElement.getSimpleName().toString())
                                                            .getter(ProcessorUtils.findGetter(getters, fieldElement))
                                                            .setter(ProcessorUtils.findSetter(setters, fieldElement))
                                                            .element(fieldElement)
                                                            .entityField(getEntityField(fieldElement, entityClass))
                                                            .storage(
                                                                    getStorage(fieldElement, mainStorage, storageDtoMap)
                                                            )
                                                            .build()
                                    )
                                    .collect(Collectors.toList())
                    )
                    .build();

            int idx = 0;
            for (DomainFieldDto fieldDto : domainClassDto.getFields()) {
                if (Objects.nonNull(fieldDto.getEntityField())) {
                    fieldDto.setFieldIndex(++idx);
                }
            }

            domainClasses.put(classElement, domainClassDto);
        }
        return domainClasses.get(classElement);
    }

    private static StorageDto getStorageDto(Storage storage) {
        return StorageDto.builder()
                .name(storage.value())
                .dataFormat(storage.dataFormat())
                .cluster(storage.cluster())
                .shardType(storage.shardType())
                .build();
    }

    private static StorageDto getMainStorage(String elementName, DomainEntity domainEntity) {
        StorageDto storageDto = getStorageDto(domainEntity.storage());
        if ("<DEFAULT>".equals(storageDto.getName())) {
            storageDto.setName(elementName);
        }
        return storageDto;
    }

    private static Map<String, StorageDto> getStorageMap(StorageDto mainStorage, DomainEntity domainEntity) {
        Map<String, StorageDto> storageDtoMap = new HashMap<>();
        storageDtoMap.put(mainStorage.getName(), mainStorage);
        for (Storage storage : domainEntity.additionalStorage()) {
            storageDtoMap.put(storage.value(), getStorageDto(storage));
        }
        return storageDtoMap;
    }

    private static EntityFieldDto getEntityField(Element element, EntityClassDto entityClass) {
        return Optional.ofNullable(element.getAnnotation(Attribute.class))
                .filter(a -> a.mappingType() == MappingType.ENTITY)
                .map(a ->
                        entityClass.getFieldMap().get(
                                a.name().isEmpty() ?
                                        element.getSimpleName().toString() :
                                        a.name()
                        )
                )
                .orElse(null);
    }

    private static StorageDto getStorage(
            Element element,
            StorageDto mainStorage,
            Map<String, StorageDto> storageDtoMap)
    {
        return Optional.ofNullable(element.getAnnotation(Attribute.class))
                .filter(a -> a.mappingType() == MappingType.STORAGE)
                .map(a -> {
                    StorageDto storage = a.storage().isEmpty() ? mainStorage : storageDtoMap.get(a.storage());
                    if (Objects.isNull(storage)) {
                        throw new IllegalArgumentException(
                                String.format(
                                        "Хнинилище с именем %s отсутсвует в описани @DomainEntity(additionalStorage " +
                                                "= ...",
                                        a.storage()
                                )
                        );
                    }
                    return storage;
                })
                .orElse(null);
    }

    private static Element getElement(DomainEntity domainEntity) {
        try {
            domainEntity.value();
        } catch (MirroredTypeException mte) {
            return ((DeclaredType) mte.getTypeMirror()).asElement();
        }
        return null;
    }

    public static void createInterceptorClass(
            Element annotatedElement,
            ProcessingEnvironment processingEnv) throws IOException
    {
        DomainClassDto domainClassDto = getClassDtoByElement(annotatedElement);
        if (domainClassDto == null) {
            return;
        }
        String className = domainClassDto.getTargetClassName() + ProcessorUtils.CLASS_INTERCEPT_POSTFIX;
        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(className);
        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
            out.println("package " + domainClassDto.getClassPackage() + ";");
            out.println();
            out.println(
                    getImportedTypes(
                            domainClassDto,
                            new ArrayList<>(
                                    Arrays.asList(
                                            ShardEntityManager.class.getCanonicalName(),
                                            Optional.class.getCanonicalName(),
                                            ShardInstance.class.getCanonicalName(),
                                            DomainEntityManager.class.getCanonicalName()
                                    )
                            )
                    )
            );
            out.println(
                    "public class " + className + " extends " + domainClassDto.getTargetClassName() + " {\n" +
                            "    private DomainEntityManager domainManager;\n" +
                            getLazyFlagsCode(domainClassDto) +
                            "\n    " + className + "(" + domainClassDto.getEntityClass().getTargetClassName() +
                            " entity, DomainEntityManager domainManager) {\n" +
                            "        this.entity = entity;\n" +
                            "        this.domainManager = domainManager;\n" +
                            "    }\n"
            );
            out.println(getSetLazyCode(domainClassDto));
            out.println(getReadEntityCode(domainClassDto));
            out.println(getGettersCode(domainClassDto));
            out.println(getSettersCode(domainClassDto));
            out.println("}");
        }
    }

    public static void createMapperClass(
            Element annotatedElement,
            ProcessingEnvironment processingEnv) throws IOException
    {
        DomainClassDto domainClassDto = getClassDtoByElement(annotatedElement);
        if (domainClassDto == null) {
            return;
        }
        String className = domainClassDto.getTargetClassName() + ProcessorUtils.CLASS_MAPPER_POSTFIX;
        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(className);
        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
            out.println("package " + domainClassDto.getClassPackage() + ";");
            out.println();

            out.println(
                    "import " + domainClassDto.getClassPackage() + "." +
                            domainClassDto.getTargetClassName() + ProcessorUtils.CLASS_INTERCEPT_POSTFIX + ";"
            );
            out.println(
                    getImportedTypes(
                            domainClassDto,
                            new ArrayList<>(
                                    Arrays.asList(
                                            "java.util.*",
                                            ShardInstance.class.getCanonicalName(),
                                            Domain.class.getCanonicalName(),
                                            Autowired.class.getCanonicalName(),
                                            DomainEntityMapper.class.getCanonicalName(),
                                            DomainEntityManager.class.getCanonicalName(),
                                            Component.class.getCanonicalName()
                                    )
                            )
                    )
            );
            out.println(
                    "@Component\n" +
                    "public class " + className + " implements DomainEntityMapper<" +
                            domainClassDto.getTargetClassName() + ", " +
                            domainClassDto.getEntityClass().getTargetClassName() + "> {\n" +
                            "    @Autowired\n" +
                            "    private DomainEntityManager domainManager;\n\n" +
                            "    private ThreadLocal<Map<Long, Domain>> domains = " +
                            "ThreadLocal.withInitial(HashMap::new);\n\n" +
                            "    @Override\n" +
                            "    public " + domainClassDto.getTargetClassName() + " newDomain(" +
                            domainClassDto.getEntityClass().getTargetClassName() + " entity) {\n" +
                            "        return new " + domainClassDto.getTargetClassName() +
                            ProcessorUtils.CLASS_INTERCEPT_POSTFIX + "(entity, domainManager);\n" +
                            "    }\n"
            );
            out.println(getMapToEntityCode(domainClassDto));
            out.println();
            out.println(getMapToDomainCode(domainClassDto));
            out.println("}");
        }
    }

    private static String getImportedTypes(DomainClassDto domainClassDto, List<String> importedTypes) {
        domainClassDto.getFields()
                .stream()
                .filter(field -> ProcessorUtils.isAnnotationPresent(field.getElement(), Attribute.class))
                .map(DomainFieldDto::getElement)
                .map(ProcessorUtils::getDeclaredType)
                .forEach(type -> {
                    importedTypes.add(type.asElement().toString());
                    if (type.getTypeArguments().size() > 0) {
                        type.getTypeArguments()
                                .stream()
                                .map(it -> (DeclaredType) it)
                                .map(DeclaredType::asElement)
                                .forEach(element ->
                                        importedTypes.add(element.toString())
                                );
                    }
                });
        importedTypes.add(domainClassDto.getEntityClass().getClassElement().asType().toString());
        return ProcessorUtils.getImportedTypes(importedTypes);
    }

    private static String getGettersCode(DomainClassDto classDto) {
        return classDto.getFields()
                .stream()
                .filter(field ->
                        ProcessorUtils.isAnnotationPresent(field.getElement(), Attribute.class) &&
                                Objects.nonNull(field.getGetter())
                )
                .map(field ->
                        "\n    @Override\n" +
                                "    public " + ProcessorUtils.getTypeField(field.getElement()) + " " +
                                field.getGetter() + "() {\n" +
                                (
                                        Objects.nonNull(field.getEntityField()) ?

                                                (
                                                        ProcessorUtils.isAnnotationPresentInArgument(
                                                                field.getElement(),
                                                                DomainEntity.class
                                                        ) ?
                                                                "        if (" + field.getFieldName() + "Lazy) {\n" +
                                                                        "            this." + field.getSetter() +
                                                                        "(domainManager.mapAllToDomains(" +
                                                                        ProcessorUtils.getFinalType(
                                                                                field.getElement()
                                                                        ) +
                                                                        ".class, ((" +
                                                                        classDto.getEntityClass().getTargetClassName() +
                                                                        ") entity)." +
                                                                        field.getEntityField().getGetter() + "()));\n" +
                                                                        "        }\n" :
                                                                "        if (isLazy) {\n" +
                                                                        "            readEntity();\n" +
                                                                        "        }\n"
                                                ) :
                                                StringUtils.EMPTY
                                        ) +


                                "        return super." + field.getGetter() + "();\n" +
                                "    }\n"
                )
                .reduce(StringUtils.EMPTY, String::concat);
    }

    private static String getSettersCode(DomainClassDto classDto) {
        return classDto.getFields()
                .stream()
                .filter(it ->
                        ProcessorUtils.isAnnotationPresent(it.getElement(), Attribute.class) &&
                                Objects.nonNull(it.getSetter()) &&
                                !ProcessorUtils.isAnnotationPresentInArgument(it.getElement(), DomainEntity.class)
                )
                .map(field ->
                        "\n    public void " + field.getSetter() +
                                "(" + ProcessorUtils.getTypeField(field.getElement()) +
                                " value, boolean change) {\n" +
                                (
                                        Objects.nonNull(field.getEntityField()) ?
                                                "        if (this.isLazy()) {\n" +
                                                        "            readEntity();\n" +
                                                        "        }\n" +
                                                        "        if (change) {\n" +
                                                        "            this.setChanges(" + field.getFieldIndex() +
                                                        ");\n" +
                                                        "        }\n" :
                                                StringUtils.EMPTY
                                        ) +
                                "        super." + field.getSetter() + "(value);\n" +
                                "    }\n" +
                                "    @Override\n" +
                                "    public void " + field.getSetter() +
                                "(" + ProcessorUtils.getTypeField(field.getElement()) + " value) {\n" +
                                "        " + field.getSetter() + "(value, true);\n" +
                                "    }\n"
                )
                .reduce(StringUtils.EMPTY, String::concat);
    }

    private static String getReadEntityCode(DomainClassDto classDto) {
        return classDto.getFields()
                .stream()
                .filter(field ->
                        Objects.nonNull(field.getEntityField()) &&
                                Objects.nonNull(field.getEntityField().getGetter()) &&
                                Objects.nonNull(field.getSetter()) &&
                                !ProcessorUtils.isAnnotationPresentInArgument(field.getElement(), DomainEntity.class)
                )
                .map(field ->
                                        "\n        this." + field.getSetter() + "(" +
                                        (
                                                ProcessorUtils.isAnnotationPresentByType(
                                                        field.getEntityField().getElement(),
                                                        ShardEntity.class
                                                ) ?

                                                        "domainManager.map(" +
                                                                ProcessorUtils.getTypeField(field.getElement()) +
                                                                ".class, entity." +
                                                                field.getEntityField().getGetter() + "()" :
                                                        "entity." + field.getEntityField().getGetter() + "("
                                        ) +
                                        "), false);"
                )
                .reduce(
                        "    @Override\n" +
                                "    public void readEntity() {\n" +
                                "        " + classDto.getEntityClass().getTargetClassName() + " entity = (" +
                                classDto.getEntityClass().getTargetClassName() + ") this.entity;",
                        String::concat
                ) +
                "\n        this.isLazy = false;" +
                "\n    }";
    }

    private static String getMapToDomainCode(DomainClassDto classDto) {
        return
                "    @Override\n" +
                        "    public " + classDto.getTargetClassName() + " map(" +
                        classDto.getEntityClass().getTargetClassName() + " entity) {\n" +
                        "        if (!Optional.ofNullable(entity).map(ShardInstance::getId).isPresent()) {\n" +
                        "            return null;\n" +
                        "        }\n" +
                        "        " + classDto.getTargetClassName() + " domain = (" + classDto.getTargetClassName() +
                        ") domains.get().get(entity.getId());\n" +
                        "        if (Objects.isNull(domain)) {\n" +
                        "            domain = newDomain(entity);\n" +
                        "            domains.get().put(entity.getId(), domain);\n" +
                        "        }\n" +
                        "        domain.setLazy(true);\n" +
                        "        return domain;\n" +
                        "    }";
    }

    private static String getMapToEntityCode(DomainClassDto classDto) {
        return classDto.getFields()
                .stream()
                .filter(field ->
                        Objects.nonNull(field.getEntityField()) &&
                                Objects.nonNull(field.getEntityField().getSetter()) &&
                                Objects.nonNull(field.getGetter())
                )
                .map(field ->
                                ProcessorUtils.isAnnotationPresentInArgument(field.getElement(), DomainEntity.class) ?
                                        "\n        entity." + field.getEntityField().getSetter() +
                                                "(domainManager.mapAllToEntities(" +
                                                ProcessorUtils.getFinalType(field.getElement()) +
                                                ".class, domain." +  field.getGetter() + "()));" :
                                        "\n        if (domain.isChanged(" + field.getFieldIndex() + ")) {\n" +
                                                "            entity." + field.getEntityField().getSetter() +
                                                (
                                                        ProcessorUtils.isAnnotationPresentByType(
                                                                field.getElement(),
                                                                DomainEntity.class
                                                        ) ?
                                                                "(domainManager.map(" +
                                                                        ProcessorUtils.getFinalType(
                                                                                field.getElement()
                                                                        ) +
                                                                        ".class, domain." + field.getGetter() +
                                                                        "()));\n" :
                                                                "(domain." + field.getGetter() + "());\n"
                                                ) + "        }"
                )
                .reduce(
                        "    @Override\n" +
                                "    public " + classDto.getEntityClass().getTargetClassName() +
                                " map(" + classDto.getTargetClassName() + " domain) {\n" +
                                "        " + classDto.getEntityClass().getTargetClassName() +
                                " entity = domain.getEntity();",
                        String::concat
                ) +
                "\n        domain.dropChanges();\n" +
                "        return entity;\n" +
                "    }";
    }

    private static String getLazyFlagsCode(DomainClassDto classDto) {
        return classDto.getFields()
                .stream()
                .filter(field ->
                        Objects.nonNull(field.getEntityField()) &&
                                ProcessorUtils.isAnnotationPresentInArgument(field.getElement(), DomainEntity.class)
                )
                .map(field -> "    private boolean " + field.getFieldName() + "Lazy = false;\n")
                .reduce(StringUtils.EMPTY, String::concat);
    }

    private static String getSetLazyCode(DomainClassDto classDto) {
        return classDto.getFields()
                .stream()
                .filter(field ->
                        Objects.nonNull(field.getEntityField()) &&
                                ProcessorUtils.isAnnotationPresentInArgument(field.getElement(), DomainEntity.class)
                )
                .map(field -> "        this." + field.getFieldName() + "Lazy = lazy;\n")
                .reduce(
                        "    @Override\n" +
                                "    public void setLazy(boolean lazy) {\n",
                        String::concat
                ) +
                "        super.setLazy(lazy);\n" +
                "    }";
    }
}
