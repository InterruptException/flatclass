package top.x86.flatclass_ksp

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import top.x86.flatclass.GenerateFlatClass

class GenerateFlatClassFactory {
    private val defaultValue = GenerateFlatClass()
    fun create(clazz: KSClassDeclaration): GenerateFlatClass {
        return GenerateFlatClass(
            genDataClass = getGenDataClass(clazz),
            newPackageName = getNewPackageName(clazz),
            newClassName = getNewClassName(clazz),
            newClassNamePrefix = getNewClassNamePrefix(clazz),
            newClassNameSuffix = getNewClassNameSuffix(clazz)
        )
    }

    private fun getGenDataClass(clazz: KSClassDeclaration): Boolean {
        return clazz.getPropValue(GenerateFlatClass::class, defaultValue.genDataClass) {
            getGenDataClass(it)
        }
    }

    private fun getGenDataClass(ann: KSAnnotation): Boolean {
        return ann.getArgValue(GenerateFlatClass::genDataClass.name).toString().toBoolean()
    }

    private fun getNewPackageName(clazz: KSClassDeclaration): String {
        return clazz.getPropValue(GenerateFlatClass::class, defaultValue.newPackageName) {
            getNewPackageName(it)
        }
    }

    private fun getNewPackageName(ann: KSAnnotation): String {
        return ann.getArgValue(GenerateFlatClass::newPackageName.name).toString()
    }

    private fun getNewClassName(clazz: KSClassDeclaration): String {
        return clazz.getPropValue(GenerateFlatClass::class, defaultValue.newClassName) {
            getNewClassName(it)
        }
    }

    private fun getNewClassName(ann: KSAnnotation): String {
        return ann.getArgValue(GenerateFlatClass::newClassName.name).toString()
    }

    private fun getNewClassNamePrefix(clazz: KSClassDeclaration): String {
        return clazz.getPropValue(GenerateFlatClass::class, defaultValue.newClassNamePrefix) {
            getNewClassNamePrefix(it)
        }
    }

    private fun getNewClassNamePrefix(ann: KSAnnotation): String {
        return ann.getArgValue(GenerateFlatClass::newClassNamePrefix.name).toString()
    }

    private fun getNewClassNameSuffix(clazz: KSClassDeclaration): String {
        return clazz.getPropValue(GenerateFlatClass::class, defaultValue.newClassNameSuffix) {
            getNewClassNameSuffix(it)
        }
    }

    private fun getNewClassNameSuffix(ann: KSAnnotation): String {
        return ann.getArgValue(GenerateFlatClass::newClassNameSuffix.name).toString()
    }
}