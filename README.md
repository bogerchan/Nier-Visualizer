![Nier Visualizer](doc/img/header.jpg)

#### Language Switch: [English](README.md) / [中文](README-zh.md)

# 1. Nier Visualizer [![](https://jitpack.io/v/bogerchan/Nier-Visualizer.svg)](https://jitpack.io/#bogerchan/Nier-Visualizer)

Nier Visualizer is a lightweight and efficient Android visual library written in pure Kotlin.It has an independent rendering thread, compatible with most of the equipment on the market. Nier Visualizer is ideal for audio visualization applications such as music players, recorder, live wallpaper and more.

> Nier Visualizer has six kinds of independent visual effects currently. More effects are under development, welcome to `star` operation to see in time.

# 2. Effect display (constantly updated)

||||
|---|---|---|
|![ColumnarType1Renderer](doc/img/renderer1.gif)|![ColumnarType2Renderer](doc/img/renderer2.gif)|![ColumnarType3Renderer](doc/img/renderer3.gif)|
|ColumnarType1Renderer|ColumnarType2Renderer|ColumnarType3Renderer|
|![LineRenderer](doc/img/renderer4.gif)|![CircleBarRenderer](doc/img/renderer5.gif)|![CircleRenderer](doc/img/renderer6.gif)|
|LineRenderer|CircleBarRenderer|CircleRenderer|
|![Compound effect 1](doc/img/renderer7.gif)|![Compound effect 2](doc/img/renderer8.gif)|Star to see more|
|Compound effect 1|Compound effect2|Developing...|

# 3. How to

## 3.1 Dependencies

1. Add it in your root build.gradle at the end of repositories:

```
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

2. Add the dependency

```
dependencies {
		compile 'com.github.bogerchan:Nier-Visualizer:v0.0.1'
	}
```

## 3.2 Sample project

Nier Visualizer project provides a `demo` module for reference.

## 3.3 Use Kotlin

### 3.3.1 Framework initialization

``` kotlin
val visualizerManager = NierVisualizerManager()

// need a param of audioSession, 0 is output mix
visualizerManager.init(0)
```

### 3.3.2 Framework release

``` kotlin
visualizerManager.release()
```

### 3.3.3 Start rendering

``` kotlin
visualizerManager.start(surfaceView, arrayOf(ColumnarType1Renderer()))
```

### 3.3.4 Stop rendering

``` kotlin
visualizerManager.stop()
```

## 3.4 Use Java

### 3.4.1 Framework initialization

``` java
NierVisualizerManager visualizerManager = new NierVisualizerManager();

// need a param of audioSession, 0 is output mix
visualizerManager.init(0);
```

### 3.4.2 Framework release

``` java
visualizerManager.release();
```

### 3.4.3 Start rendering

``` java
visualizerManager.start(surfaceView, new IRenderer[]{new LineRenderer(true)});
```

### 3.4.4 Stop rendering

``` java
visualizerManager.stop();
```

# 4. Follow-up plan

- Thinking about implementing visual effects like Siri.
- Some gallery of visual effects tailored for DJ music.
- If you are interested in it, welcome to Fork to do it together!

# 5. Thanks

Ported some of the [android-visualizer](https://github.com/felixpalmer/android-visualizer) visual effects, thanks to `felixpalmer`!

# 6. Protocol

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