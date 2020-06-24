# android-OpenSource

当你制作jar或者aar时 想要依赖多热门框架  但又不希望与别人冲突时  可以参考这个

将多个框架的源码修改包名 避免冲突;


修改包名方法:

1.直接使用Android studio的 refactor 工具修改包名(yourpck)  改成自定义包名

  由于代码量较大时间可能会很久

2.如果出现部分 yourpck 没有替换  可使用replace工具替换

使用方法:

1.直接运行 app 即可

2.如果要输出jar  使用 ./gradlew makejar 编译即可

3.也可以将源码直接拷贝到别的项目使用 使用源码时注意不要遗漏项目的一些配置  不然会出错


相关源码都可以在github下载

本项目包含的开源工具及对应版本:

 1.rxjava         2.2.8

 2.rxandroid      2.1.0

 3.gson           2.8.2

 4.retrofit       2.5.0

 5.okhttp3        3.12.3



更多源码:

1.可下载更多开源框架源码 填入并且修改包名等;

2.开源代码编译一般比较费时间  取决于源码本身依赖的多少


注意项:

1.关于okhttp3

    在3.13.x 开始不再支持android5以下 需要android5.0及以上 和java8

	如果需要支持android5以下  请使用3.12.x版本

	参考网址:https://www.jianshu.com/p/62407dc7aa89

2.尽量不要参与混淆 会有问题的
