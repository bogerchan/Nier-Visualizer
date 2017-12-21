![Nier Visualizer](doc/img/header.jpg)

#### 语言切换: [English](README.md) / [中文](README-zh.md)

# 1. Nier Visualizer ![](https://jitpack.io/v/bogerchan/Nier-Visualizer.svg)

Nier Visualizer 是一款纯 Kotlin 编写的轻量高效的 Android 可视化库。采用独立渲染线程，兼容绝大部分设备。适用于音频可视化的应用场景，如音乐播放器、录音应用、动态壁纸等。

> 目前已经有6种独立的可视化效果，更多效果开发中，欢迎 `start` 关注。

# 2. 效果图（不断更新中）

||||
|---|---|---|
|![ColumnarType1Renderer](doc/img/renderer1.gif)|![ColumnarType2Renderer](doc/img/renderer2.gif)|![ColumnarType3Renderer](doc/img/renderer3.gif)|
|ColumnarType1Renderer|ColumnarType2Renderer|ColumnarType3Renderer|
|![LineRenderer](doc/img/renderer4.gif)|![CircleBarRenderer](doc/img/renderer5.gif)|![CircleRenderer](doc/img/renderer6.gif)|
|LineRenderer|CircleBarRenderer|CircleRenderer|
|![复合效果1](doc/img/renderer7.gif)|![复合效果2](doc/img/renderer8.gif)| star 关注后续更新...|
|复合效果1|复合效果2|开发中...|

# 3. 如何接入

## 3.1 依赖方式

1. 在项目根目录的 build.gradle 的 repositories 末尾加上该仓库：

```
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

2. 添加下列依赖：

```
dependencies {
		compile 'com.github.bogerchan:Nier-Visualizer:v0.0.1'
	}
```

## 3.2 项目实践

项目的 `demo` 模块可供接入参考。

## 3.3 Kotlin 接入

### 3.3.1 初始化框架

``` kotlin
val visualizerManager = NierVisualizerManager()

// 传入 audioSession, 0 为 output mix
visualizerManager.init(0)
```

### 3.3.2 销毁框架实例，释放资源

``` kotlin
visualizerManager.release()
```

### 3.3.3 启动渲染

``` kotlin
visualizerManager.start(surfaceView, arrayOf(ColumnarType1Renderer()))
```

### 3.3.4 停止渲染

``` kotlin
visualizerManager.stop()
```

## 3.4 Java 接入

### 3.4.1 初始化框架

``` java
NierVisualizerManager visualizerManager = new NierVisualizerManager();

// need a param of audioSession, 0 is output mix
visualizerManager.init(0);
```

### 3.4.2 销毁框架实例，释放资源

``` java
visualizerManager.release();
```

### 3.4.3 启动渲染

``` java
visualizerManager.start(surfaceView, new IRenderer[]{new LineRenderer(true)});
```

### 3.4.4 停止渲染

``` java
visualizerManager.stop();
```

# 4. 后续计划

- 类似 Siri 的可视化效果，构思中...
- 为 DJ 音乐定制的可视化效果库
- 如果你有兴趣，欢迎 Fork 一起搞

# 5. 致谢

部分效果参考 [android-visualizer](https://github.com/felixpalmer/android-visualizer)，感谢 `felixpalmer`！

# 6. 开放协议

```
Copyright 2017 Boger Chan

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```