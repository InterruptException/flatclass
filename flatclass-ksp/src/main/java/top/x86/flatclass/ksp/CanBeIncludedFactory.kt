package top.x86.flatclass_ksp

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import top.x86.flatclass.CanBeIncluded

class CanBeIncludedFactory(private val log: KSPLogger) {
    private val defaultValue = CanBeIncluded()
    fun create(clazz: KSClassDeclaration): CanBeIncluded {
        return CanBeIncluded(
            includeAllProps = getIncludeAllProps(clazz),
            includeAllMethods = getIncludeAllProps(clazz),
        )
    }

    private fun getIncludeAllProps(clazz: KSClassDeclaration): Boolean {
        return clazz.getPropValue(CanBeIncluded::class, defaultValue.includeAllProps) {
            getIncludeAllProps(it)
        }
    }

    private fun getIncludeAllProps(ann: KSAnnotation): Boolean {
        return ann.getArgValue(CanBeIncluded::includeAllProps.name).toString().toBoolean()
    }

    private fun getIncludeAllMethods(clazz: KSClassDeclaration): Boolean {
        return clazz.getPropValue(CanBeIncluded::class, defaultValue.includeAllMethods) {
            getIncludeAllProps(it)
        }
    }

    private fun getIncludeAllMethods(ann: KSAnnotation): Boolean {
        return ann.getArgValue(CanBeIncluded::includeAllMethods.name).toString().toBoolean()
    }

}