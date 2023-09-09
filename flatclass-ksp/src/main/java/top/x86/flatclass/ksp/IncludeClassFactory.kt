package top.x86.flatclass_ksp

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import top.x86.flatclass.IncludeClass
import com.squareup.kotlinpoet.ksp.toClassName
import java.util.stream.Collectors.toMap

class IncludeClassFactory(private val log: KSPLogger) {
    private val defaultValue = IncludeClass(Nothing::class)

    fun getAllIncludeClasses(clazz: KSClassDeclaration) : List<Pair<IncludeClass, KSClassDeclaration>> {
        return clazz.annotations.filter {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == IncludeClass::class.qualifiedName
        }.map {
            create(it)
        }.toList()
    }
    fun create(ann: KSAnnotation): Pair<IncludeClass, KSClassDeclaration> {

        return Pair(
            IncludeClass(
                targetClass = Nothing::class,
                genConstructor = genConstructor(ann),
                fieldName = fieldName(ann),
                flat = flat(ann),
                alwaysAddFlatNamespace = alwaysAddFlatNamespace(ann),

                flatNamespace = flatNamespace(ann),
                prefix = prefix(ann),
                suffix = suffix(ann),
                excludeAllMethods = excludeAllMethods(ann),

                excludeAllProps = excludeAllProps(ann),
                excludedProps = excludedProps(ann),
                excludedMethods = excludedMethods(ann),
                renameProps = renameProps(ann),
                renameMethods = renameMethods(ann),
            ),
            targetClass(ann)
        )
    }

    private fun targetClass(ann: KSAnnotation): KSClassDeclaration {
        val t =  ann.getArgValue(IncludeClass::targetClass.name) as KSType
        val d = t.declaration as KSClassDeclaration
        log.warn("[FlatClass]targetClass = ${d.toClassName().canonicalName}")
        return d
    }

    private fun genConstructor(clazz: KSClassDeclaration): Boolean {
        return clazz.getPropValue(IncludeClass::class, defaultValue.genConstructor) {
            genConstructor(it)
        }
    }

    private fun genConstructor(ann: KSAnnotation): Boolean {
        return ann.getArgValue(IncludeClass::genConstructor.name).toString().toBoolean()
    }


    private fun fieldName(clazz: KSClassDeclaration): String {
        return clazz.getPropValue(IncludeClass::class, defaultValue.fieldName) {
            fieldName(it)
        }
    }

    private fun fieldName(ann: KSAnnotation): String {
        return ann.getArgValue(IncludeClass::fieldName.name).toString()
    }

    private fun flat(clazz: KSClassDeclaration): Boolean {
        return clazz.getPropValue(IncludeClass::class, defaultValue.flat) {
            flat(it)
        }
    }

    private fun flat(ann: KSAnnotation): Boolean {
        return ann.getArgValue(IncludeClass::flat.name).toString().toBoolean()
    }

    private fun alwaysAddFlatNamespace(clazz: KSClassDeclaration): Boolean {
        return clazz.getPropValue(IncludeClass::class, defaultValue.alwaysAddFlatNamespace) {
            alwaysAddFlatNamespace(it)
        }
    }

    private fun alwaysAddFlatNamespace(ann: KSAnnotation): Boolean {
        return ann.getArgValue(IncludeClass::alwaysAddFlatNamespace.name).toString().toBoolean()
    }

    private fun flatNamespace(clazz: KSClassDeclaration): String {
        return clazz.getPropValue(IncludeClass::class, defaultValue.flatNamespace) {
            flatNamespace(it)
        }
    }

    private fun flatNamespace(ann: KSAnnotation): String {
        return ann.getArgValue(IncludeClass::flatNamespace.name).toString()
    }

    private fun prefix(clazz: KSClassDeclaration): String {
        return clazz.getPropValue(IncludeClass::class, defaultValue.prefix) {
            prefix(it)
        }
    }

    private fun prefix(ann: KSAnnotation): String {
        return ann.getArgValue(IncludeClass::prefix.name).toString()
    }

    private fun suffix(clazz: KSClassDeclaration): String {
        return clazz.getPropValue(IncludeClass::class, defaultValue.suffix) {
            suffix(it)
        }
    }

    private fun suffix(ann: KSAnnotation): String {
        return ann.getArgValue(IncludeClass::suffix.name).toString()
    }

    private fun excludeAllMethods(clazz: KSClassDeclaration): Boolean {
        return clazz.getPropValue(IncludeClass::class, defaultValue.excludeAllMethods) {
            excludeAllMethods(it)
        }
    }

    private fun excludeAllMethods(ann: KSAnnotation): Boolean {
        return ann.getArgValue(IncludeClass::excludeAllMethods.name).toString().toBoolean()
    }

    private fun excludeAllProps(clazz: KSClassDeclaration): Boolean {
        return clazz.getPropValue(IncludeClass::class, defaultValue.excludeAllProps) {
            excludeAllProps(it)
        }
    }

    private fun excludeAllProps(ann: KSAnnotation): Boolean {
        return ann.getArgValue(IncludeClass::excludeAllProps.name).toString().toBoolean()
    }

    private fun excludedProps(clazz: KSClassDeclaration): Array<String> {
        return clazz.getPropValue(IncludeClass::class, defaultValue.excludedProps) {
            excludedProps(it)
        }
    }

    private fun excludedProps(ann: KSAnnotation): Array<String> {
        val result = ann.getArgValue(IncludeClass::excludedProps.name) as ArrayList<String>
        return result.toTypedArray()
    }

    private fun excludedMethods(clazz: KSClassDeclaration): Array<String> {
        return clazz.getPropValue(IncludeClass::class, defaultValue.excludedMethods) {
            excludedMethods(it)
        }
    }

    private fun excludedMethods(ann: KSAnnotation): Array<String> {
        val result = ann.getArgValue(IncludeClass::excludedMethods.name) as ArrayList<String>
        return result.toTypedArray()
    }

    private fun renameProps(clazz: KSClassDeclaration): Array<String> {
        return clazz.getPropValue(IncludeClass::class, defaultValue.renameProps) {
            renameProps(it)
        }
    }

    private fun renameProps(ann: KSAnnotation): Array<String> {
        val result = ann.getArgValue(IncludeClass::renameProps.name) as ArrayList<String>

        return result.toTypedArray()
    }

    private fun renameMethods(clazz: KSClassDeclaration): Array<String>  {
        return clazz.getPropValue(IncludeClass::class, defaultValue.renameMethods) {
            renameMethods(it)
        }
    }

    private fun renameMethods(ann: KSAnnotation): Array<String>  {
        val result = ann.getArgValue(IncludeClass::renameMethods.name) as ArrayList<String>
        return result.toTypedArray()
    }
}