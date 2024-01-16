package com.antalex.db.annotation.processors;

import com.antalex.db.annotation.ShardEntity;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.StorageAttributes;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.service.ShardEntityRepository;
import com.antalex.db.model.dto.ClassDto;
import com.antalex.db.model.dto.FieldDto;
import com.google.auto.service.AutoService;
import org.springframework.stereotype.Repository;

import javax.annotation.processing.*;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.persistence.Column;
import javax.persistence.Table;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
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
                                            annotatedElement.getEnclosedElements().stream()
                                                    .filter(this::isField)
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

    private String getColumnName(Element element) {
        return COLUMN_PREFIX + element.getSimpleName().toString().toUpperCase();
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

    private String getInsertSQL(ClassDto classDto) {
        String sql = "INSERT INTO $$$." + classDto.getTableName() + " (";
        String columns = "";
        String values = "";
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
            out.println("import " + Repository.class.getCanonicalName() + ";");
            out.println("import " + ShardType.class.getCanonicalName() + ";");
            out.println("import " + Cluster.class.getCanonicalName() + ";");
            out.println("import " + StorageAttributes.class.getCanonicalName() + ";");
            out.println("import " + ShardDataBaseManager.class.getCanonicalName() + ";");
            out.println();
            out.println("@Repository");
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
            out.println("    private final ShardDataBaseManager dataBaseManager;");
            out.println("    private final Cluster cluster;");

            out.println();
            out.println("    " + classDto.getClassName() + "(ShardDataBaseManager dataBaseManager) {");
            out.println("       this.dataBaseManager = dataBaseManager;");
            out.println(
                    "       this.cluster = dataBaseManager.getCluster(String.valueOf(\"" +
                    classDto.getCluster() +
                    "\"));"
            );
            out.println("    }");

            out.println();
            out.println("    @Override");
            out.println("    public " + classDto.getTargetClassName() +
                    " save(" + classDto.getTargetClassName() + " entity) {"
            );
            out.println("       return null;");
            out.println("   }");
            out.println();

            out.println("    @Override");
            out.println("    public ShardType getShardType(" + classDto.getTargetClassName() + " entity) {");
            out.println("       return SHARD_TYPE;");
            out.println("   }");
            out.println();

            out.println();
            out.println("}");
            out.println();
        }
    }
}
