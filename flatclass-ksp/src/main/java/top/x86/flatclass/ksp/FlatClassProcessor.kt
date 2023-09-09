package top.x86.flatclass_ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toAnnotationSpec
import top.x86.flatclass.GenerateFlatClass
import top.x86.flatclass.IncludeClass
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import top.x86.flatclass.ExcludeIt
import kotlin.concurrent.thread

class FlatClassProcessor(private val env: SymbolProcessorEnvironment) : SymbolProcessor {
    private val codegen = env.codeGenerator
    private val log = env.logger
    val opts = env.options
    private val generateFlatClassFactory = GenerateFlatClassFactory()
    private val includeClassFactory = IncludeClassFactory(env.logger)
    val canBeIncludedFactory = CanBeIncludedFactory(env.logger)
    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val maybeTargets = resolver.getSymbolsWithAnnotation(GenerateFlatClass::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()

        val failedTargets = maybeTargets.filter {
            it.validate() && it.isAnnotationPresent(IncludeClass::class)
        }.let {
            processTarget(it).onEach {
                warn("该类处理失败：${it.toClassName()::canonicalName}")
            }
        }


        val nonTargets = maybeTargets.filter { !it.validate() }.filterNot { it.isAnnotationPresent(IncludeClass::class) }
            .onEach {
                log.warn("[FlatClass]发现无法处理的目标类: ${it.toClassName()}", it)
            }
            .toList()
        return nonTargets + failedTargets
    }

    private fun processTarget(targetClasses: Sequence<KSClassDeclaration>):  List<KSClassDeclaration> {
        val failedTargets = mutableListOf<KSClassDeclaration>()
        targetClasses.forEach { targetClass ->
            try {
                processTargetClass(targetClass)
            } catch (e: NullPointerException) {
                failedTargets.add(targetClass)
            }
        }
        return failedTargets
    }

    private fun processTargetClass(targetClass: KSClassDeclaration) {
        val targetClassName = targetClass.toClassName().canonicalName
        val targetClassSimpleName = targetClass.toClassName().simpleName

        warn("发现可处理的目标类: $targetClassName", targetClass)
        warn("$targetClass 所在的包为 ${targetClass.getPkgFullName()}")
        //            targetClass.getProps().forEach {
        //                warn("$targetClassName 包含以下属性：属性名 = ${it.key} : 属性类型 = ${it.value}")
        //            }


        val genClass = targetClass.getGenerateClass()
        val newClassName = if (genClass.newClassName.isNotBlank()) {
            genClass.newClassName
        } else {
            genClass.newClassNamePrefix + targetClassSimpleName + genClass.newClassNameSuffix
        }
        warn("新类名为：genClass.newClassName = ${genClass.newClassName}\t; newClassName = $newClassName")
        val newPkgName = if (genClass.newClassName == ".") {
            targetClass.getPkgFullName()
        } else if (genClass.newPackageName.startsWith(".")) {
            targetClass.getPkgFullName() + genClass.newPackageName
        } else {
            genClass.newPackageName
        }
        val newClass = ClassName(newPkgName, newClassName)

        val newClassCtor = FunSpec.constructorBuilder()

        val selfProps = targetClass.getPropMetadataList()
            .filter { !it.isExcludeIt }
            .onEach {
                newClassCtor.addParameter(it.propName, it.declaration.type.toTypeName())
            }.map {
                PropertySpec.builder(it.propName, it.declaration.type.toTypeName())
                    .addAnnotations(it.annotations.map { an -> an.rawData.toAnnotationSpec() })
                    .initializer(it.propName)
                    .build()
            }.toList()

        val allIncludedClasses = targetClass.getAllIncludedClasses(log)
        warn("${targetClassName}包含了${allIncludedClasses.size}个类")

        val allIncludedClassesProps = allIncludedClasses.flatMap { (clazz, props)->
            props.getPropMetadataList().filter { prop -> !clazz.excludedProps.contains(prop.propName) }
        }

        //处理被包含的外部属性
        val includedClassProps = allIncludedClassesProps.onEach {
            newClassCtor.addParameter(it.propName, it.declaration.type.toTypeName())
        }.map {
            PropertySpec.builder(it.propName, it.declaration.type.toTypeName())
                .addAnnotations(it.annotations.map { an -> an.rawData.toAnnotationSpec() })
                .initializer(it.propName)
                .build()
        }.toList()

        val secondaryCtor = FunSpec.constructorBuilder()

        secondaryCtor.addParameter("p0", targetClass.toClassName())

        allIncludedClasses.toList().forEachIndexed { index, (clazz, clsDeclare) ->
            val pn = "p${index + 1}"
            warn("添加参数 ${pn}")
            secondaryCtor.addParameter(pn, clsDeclare.toClassName())
        }

        //处理被包含的自身属性
        selfProps.map {
            "${it.name} = p0.${it.name}"
        }.plus(
            allIncludedClasses.toList().flatMapIndexed { i, (ic, decl) ->
                decl.getPropMetadataList()
                    .filter { !ic.excludedProps.contains(it.propName)  }
                    .map {
                        "${it.propName} = p${i + 1}.${it.propName}"
                    }
            }
        ).toTypedArray().let {
            secondaryCtor.callThisConstructor(*it)
        }

        val newFile = FileSpec.builder(newClass)
            .addType(
                TypeSpec.classBuilder(newClassName).let {
                    if (genClass.genDataClass) {
                        it.addModifiers(KModifier.DATA)
                    } else {
                        it
                    }
                }
                    .primaryConstructor(newClassCtor.build())
                    .addProperties(selfProps)
                    .addProperties(includedClassProps)
                    .let { it ->
                        val annSpec = targetClass.annotations.filter {
                            val annName = it.shortName.asString()
                            annName != IncludeClass::class.simpleName && annName != GenerateFlatClass::class.simpleName
                        }.map {
                            val ann = it.annotationType.resolve()
                            val annName = ann.toClassName().canonicalName
                            if (annName == "kotlinx.serialization.Serializable") {
                                warn("发现kotlinx.serialization.Serializable注解")
                                if (it.arguments == it.defaultArguments) {
                                    warn("需要绕过Kapt的泛型注解参数的官方Bug")
                                    AnnotationSpec.builder(ann.toClassName())
                                        .build()
                                } else {
                                    warn("保持原参数列表")
                                    it.toAnnotationSpec()
                                }
                            } else {
                                it.toAnnotationSpec()
                            }
                        }.toList()
                        it.addAnnotations(annSpec)
                    }.addFunction(secondaryCtor.build())
                    .build()
            ).build()


        val sb = StringBuffer()
        newFile.writeTo(sb)
        warn("生成以下新类：\n${sb}")

        thread {
            val dependencies = Dependencies(false, targetClass.containingFile!!)
            try {
//                codegen.createNewFile(dependencies, packageName = newPkgName, fileName = newClassName)
//                    .bufferedWriter()
//                    .use {
//                        newFile.writeTo(it)
//                        it.flush()
//                    }
                newFile.writeTo(codegen, dependencies)
            } catch (e: Exception) {
                e.message?.let {
                    warn(it)
                }
            } finally {

            }
    //                env.codeGenerator.createNewFile(
    //                    dependencies = dependencies,
    //                    packageName = newPkgName,
    //                    fileName = newClassName,
    //                    extensionName = "kt"
    //                ).apply{
    //
    //                }.bufferedWriter().use {writer ->
    //                    try {
    //                        newFile.writeTo(writer)
    //                        warn("文件写入路径是：  ")
    //                    } catch (e: Exception) {
    //                        e.message?.let {
    //                            warn(it)
    //                        }
    //                    } finally {
    //                        writer.flush()
    //                        writer.close()
    //                    }
    //                }
        }

        targetClass.getAnnotationMetadataList().forEach {
            warn(
                "$targetClassName 使用了以下注解： @${it.annotationName}(${
                    it.arguments.map { it.toKVPair() }.joinToString(", ")
                }})"
            )
        }

        targetClass.getPropMetadataList().forEach { pm ->
            warn("$targetClassName 包含以下属性：属性名 = ${pm.propName} : 属性类型 = ${pm.propType}")
            pm.declaration.getAnnotationMetadataList().forEach { am ->
                warn("$targetClassName 的属性 ${pm.propName} 使用了注解 ${am}")
            }
        }
        generateFlatClassFactory.create(targetClass).let {
            warn("generateFlatClassFactory: $it")
        }
        includeClassFactory.getAllIncludeClasses(targetClass).also {
            warn("getAllIncludeClasses: $it")
        }.forEach { (includedClassAnnotation, includedClassDeclaration) ->
            includedClassDeclaration.getProps().forEach {
                warn("被包含的类 $includedClassDeclaration 声明了以下属性：${it}")
            }
            includedClassDeclaration.getPropMetadataList().forEach { pm ->
                pm.declaration.getAnnotationMetadataList().forEach { am ->
                    warn(
                        "属性${pm.propName} 使用了以下注解：@${am.annotationName}(${
                            am.arguments.map { it.toKVPair() }.toList().joinToString(",")
                        })}"
                    )
                }
            }
        }
    }

    private fun warn(msg: String, node: KSNode? = null) {
        log.warn("[FlatClass] $msg", node)
    }

    override fun finish() {
        super.finish()
        log.warn("[FlatClass]类成员展开已完成")
    }

    override fun onError() {
        super.onError()
        log.warn("[FlatClass]类成员展开发生错误")
    }
}

data class PropMetadata(
    val propName: String,
    val propType: String,
    val propValue: Any? = null,
    val modifiers: List<Modifier> = emptyList(),
    val annotations: List<AnnotationMetadata> = emptyList(),
    val declaration: KSPropertyDeclaration,
    val isExcludeIt: Boolean = false
)

data class ArgumentMetadata(
    val argName: String,
    val argValue: Any?,
    val argType: String?,
) {
    fun toKVPair(): String {
        val v = when (argValue!!) {
            is Boolean -> "${argValue as Boolean}"
            is Float -> "${argValue as Float}"
            is Double -> "${argValue as Double}"
            is Int -> "${argValue as Int}"
            is Short -> "${argValue as Short}"
            is Long -> "${argValue as Long}"
            is String -> "\"${argValue as String}\""
            is Char -> "'${argValue as Char}'"
            //is ArrayList<String> -> "[${(argValue as java.util.ArrayList<String>).joinToString(", ")}]"
            is java.util.ArrayList<*> -> {
                (argValue as java.util.ArrayList<*>).map {
                    when (it) {
                        is String -> "\"$it\""
                        is KSType -> "${it.toClassName().canonicalName}::class"
                        else -> "$it"
                    }
                }.joinToString(prefix = "[", separator = ", ", postfix = "]")
            }
            is KSType -> "${((argValue as KSType).toClassName().canonicalName)}::class"
            else -> try {
                "else $argValue, ${(argValue.javaClass)}"
            } catch (_ : Exception) {
                "catch $argValue"
            }
        }
        return "$argName = $v"
    }
}
data class AnnotationMetadata(
    val type : KSType,
    val annotationName: String,
    val arguments: List<ArgumentMetadata>,
    val rawData: KSAnnotation
) {
    override fun toString(): String {
        return "@${annotationName}(${arguments.map { it.toKVPair() }.joinToString(", ")})"
    }
}

@OptIn(KspExperimental::class)
fun KSClassDeclaration.getPropMetadataList(): List<PropMetadata> {
    return this.getDeclaredProperties().map {
        PropMetadata(
            propName =  it.simpleName.asString(),
            propType = it.type.resolve().toClassName().canonicalName,
            declaration = it,
            modifiers = it.modifiers.toList(),
            annotations = it.getAnnotationMetadataList(),
            isExcludeIt = it.isAnnotationPresent(ExcludeIt::class)
        )
    }.toList()
}


fun KSClassDeclaration.getAnnotationMetadataList(): List<AnnotationMetadata> {
    return this.annotations.map {
        val ann = it.annotationType.resolve()
        AnnotationMetadata(
            type = ann,
            annotationName = ann.toClassName().canonicalName,
            arguments = it.arguments.map {arg->
                ArgumentMetadata(arg.name!!.getShortName(), arg.value, arg.value?.javaClass?.canonicalName)
            }.toList(),
            rawData = it
        )
    }.toList()
}
fun KSPropertyDeclaration.getAnnotationMetadataList(): List<AnnotationMetadata> {
    return this.annotations.map {
        val ann = it.annotationType.resolve()
        AnnotationMetadata(
            type = ann,
            annotationName = ann.toClassName().canonicalName,
            arguments = it.arguments.map {arg->
                ArgumentMetadata(arg.name!!.getShortName(), arg.value, arg.value?.javaClass?.canonicalName)
            }.toList(),
            rawData = it
        )
    }.toList()
}

fun KSClassDeclaration.getGenerateClass(): GenerateFlatClass {
    return GenerateFlatClassFactory().create(this)
}
