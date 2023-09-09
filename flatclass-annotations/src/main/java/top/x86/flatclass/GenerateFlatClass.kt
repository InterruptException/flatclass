package top.x86.flatclass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class GenerateFlatClass(
    val genDataClass: Boolean = true, //生成data class
    val newPackageName: String = ".flat",//"."开头表示注解使用的位置所在的当前包路径的相对路径，否则为绝对路径
    val newClassName: String = "", //""表示自动生成。
    val newClassNamePrefix: String = "", //如果newClassName指定了非空字符串，则该参数被忽略
    val newClassNameSuffix: String = "Flat" //如果newClassName指定了非空字符串，则该参数被忽略
)