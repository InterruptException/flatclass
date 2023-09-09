package top.x86.flatclass_ksp

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.Modifier
import top.x86.flatclass.IncludeClass
import com.squareup.kotlinpoet.ksp.toClassName
import kotlin.reflect.KClass

fun KSAnnotation.getArgValue(name: String): Any? {
    return getArg(name)?.value
}
fun KSAnnotation.getArg(name: String): KSValueArgument? {
    return this.arguments.firstOrNull {
        it.name?.asString() == name
    }
}

fun KSClassDeclaration.getProp(propType: KClass<*>) : KSAnnotation? {
    return this.annotations.firstOrNull {
        it.annotationType.resolve().declaration.qualifiedName?.asString() == propType.qualifiedName
    }
}

fun <T> KSClassDeclaration.getPropValue(propType: KClass<*>, defaultValue: T, block: (KSAnnotation)->T) : T {
    return getProp(propType)?.let {
        block(it)
    }?:defaultValue
}

fun KSClassDeclaration.getPkgFullName(): String {
    return this.packageName.asString()
}

fun KSClassDeclaration.checkIsDataClass(): Boolean {
    return this.modifiers.contains(Modifier.DATA)
}

fun KSClassDeclaration.getProps() : Map<String, String> {
    return this.getDeclaredProperties().map {
        Pair<String, String>(
            it.simpleName.asString(),
            it.type.resolve().toClassName().canonicalName
        )
    }.toMap()
}

fun KSClassDeclaration.getAllIncludedClasses(logger: KSPLogger)
    : List<Pair<IncludeClass, KSClassDeclaration>>
{
    return IncludeClassFactory(logger).getAllIncludeClasses(this)
}