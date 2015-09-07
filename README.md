# PhotoView 图片浏览缩放控件

和普通的ImageView一样的使用方法

如使用过程中有任何bug，意见或建议，可邮件给我 bmme@vip.qq.com

# 效果图
![PhotoView](./demo2.gif) ![PhotoView](./demo1.gif)

#使用
1.添加Gradle依赖 (推荐)
```gradle
dependencies {
    compile 'com.bm.photoview:library:2.0.4'
}
```
(或者也可以将项目下载下来，将Info.java和PhotoView.java两个文件拷贝到你的项目中，不推荐)

2.xml添加，注意，在定义PhotoView宽高时，不能使用wrap_content，可以指定固定dp的宽高或者match_parent。 wrap_content属性的支持会在稍后加上
```xml
 <com.bm.library.PhotoView
     android:id="@+id/img"
     android:layout_width="match_parent"
     android:layout_height="match_parent"
     android:scaleType="centerInside"
     android:src="@drawable/bitmap1" />
```

3.java代码
```java
PhotoView photoView = (PhotoView) findViewById(R.id.img);
// 启用图片缩放功能
photoView.enable();
// 禁用图片缩放功能 (默认为禁用，会跟普通的ImageView一样，缩放功能需手动调用enable()启用)
photoView.disenable();
// 获取图片信息
Info info = photoView.getInfo();
// 从一张图片信息变化到现在的图片，用于图片点击后放大浏览，具体使用可以参照demo的使用
photoView.animaFrom(info);
// 从现在的图片变化到所给定的图片信息，用于图片放大后点击缩小到原来的位置，具体使用可以参照demo的使用
photoView.animaTo(info,new Runnable() {
       @Override
       public void run() {
           //动画完成监听
       }
   });
```

# 版本

v2.0.4
   修复某些情况下会闪动
   增加对ScaleType.FIT_START,FIT_END对animaFrom的支持

v2.0.0  
   * 添加animaTo,animaFrom方法，支持图片点击放大缩小浏览功能
   * 添加enable()和disenable() 打开和关闭触摸缩放方法，默认打开 (当普通ImageView使用的时候建议关闭触摸缩放功能)
   * 支持所有ScaleType属性

v1.0
