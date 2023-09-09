package top.x86.flatclass

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class FlatIt(
    val alwaysAddFlatNamespace: Boolean = false, //true表示总是为混入的字段和方法添加命名空间，false表示冲突时才按需添加
    val flatNamespace: String = "", //如果混入多个类中有重名字段、方法时，为该方法添加到自定义前缀的前面。
    val prefix: String = "", //自定义统一前缀，无论是否重名都会添加的前缀
    val suffix: String = "", //自定义统一后缀，无论是否重名都会添加的后缀
    val excludeAllMethods: Boolean = true,//默认排除所有方法
    val excludeAllProps: Boolean = false,//默认不排除所有属性
    val excludedProps: Array<String> = [], //需要排除的属性名列表
    val excludedMethods: Array<String> = [], //需要排除的方法名列表
    val renameProps: Array<String> = [],//重命名属性名， 如：["oldProp1 newProp1", "oldProp2 newProp2", ...]
    val renameMethods: Array<String> = []//重命名方法名，如["oldMethod1 newMethod1", "oldMethod2 newMethod2", ...]
)