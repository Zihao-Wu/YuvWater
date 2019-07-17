# YuvWater
android camera 视频水印
详情见博客：https://blog.csdn.net/u010521645/article/details/85166237
视频录制目录： /sdcard/yuvVideo ，请手动在设置加相机和存储权限。

如果添加其它的文字水印？如yyyy年mm月dd日

0、右上角进生成yuv页，然后画你想添加的文字，得到文字年月日的yuv数组

1、在YuvOsdUtils.c 的initOsd 方法中，把生成的年月日数组添加到mNumArrays中，参考数字的添加方式

2、在getIndex中，根据jchar c 返回index。这里的index就是添加的index,中文无法用c='年'方式，你需要debug 获取到中文对应的数字

4、在CreateNumberAct 中是通过画bitmap是得到文字的，不同文字宽高会不一样，不好统一计算，所以文字会有切边现象，实际上图片的来源可以是任何地方，如ps的一张文字图片，只要调用bitmapToGrayNV 生成nv12数组就行，这里只是参考.
