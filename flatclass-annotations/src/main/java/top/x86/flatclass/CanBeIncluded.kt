package top.x86.flatclass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class CanBeIncluded(
    val includeAllProps: Boolean = true,
    val includeAllMethods: Boolean = true,
)