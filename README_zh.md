# dashboard_android

[English](README.md) | 简体中文

用于浏览应用市场概况、列表与详情的 Android 原生客户端。

## 项目概述

应用提供原生市场概况、应用详情、设置及相关服务的 WebView 入口。数据/API 层与 Compose 页面分离，实时内容依赖外部 Dashboard 服务。

## 技术栈与依赖

Kotlin、Jetpack Compose/Material 3、AndroidX、Gradle Wrapper。工程声明最低 API 31、目标及编译 API 37，版本目录指定 AGP 9.4.1 和 Kotlin 2.2.10，需使用兼容的 Android Studio/JDK。

## 文件结构

- `app/src/main/` — 应用源码与资源
- `app/build.gradle.kts` — 模块 SDK 与依赖配置
- `gradle/libs.versions.toml` — 依赖版本
- `gradlew` — Gradle 启动器

## 快速上手

用 Android Studio 打开根目录，安装指定 SDK 并同步 Gradle 依赖。生成调试包：

```bash
./gradlew :app:assembleDebug
```

在 API 31 及以上模拟器或设备运行 `app`。加载实时内容前检查数据层服务地址，发行签名在本地配置。
