# 意图桥 (IntentBridge)

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green?style=flat-square" alt="Platform">
  <img src="https://img.shields.io/badge/Language-Kotlin-blue?style=flat-square" alt="Language">
  <img src="https://img.shields.io/badge/License-MIT-yellow?style=flat-square" alt="License">
</p>

## 简介

**意图桥**是一款专为自闭症（ASD）儿童设计的沟通辅助应用。通过直观的视觉卡片系统，帮助孩子表达基本需求，如吃饭、喝水、上厕所等。

### 核心特点

- 🐷 **佩奇语音** - 使用小猪佩奇风格的声音（阿里云CosyVoice语音克隆）
- ⚡ **极速响应** - 本地音频缓存，点击即播
- 👨‍👩‍👧 **家长模式** - 家长可自定义卡片内容
- 🎨 **简洁UI** - 大图标、高对比度，适合特殊儿童

## 界面预览

```
┌─────────────────────────────────────┐
│  👤 ⚙️ ✏️     意图桥              │ ← 家长模式才显示设置/编辑
├─────────────────────────────────────┤
│  ┌─────┐ ┌─────┐ ┌─────┐         │
│  │ 🚽 │ │ 💧 │ │ 🍽️ │         │ ← 紧急卡片（红色）
│  │尿尿 │ │喝水 │ │吃   │         │
│  └─────┘ └─────┘ └─────┘         │
├─────────────────────────────────────┤
│  ┌─────┐ ┌─────┐ ┌─────┐         │
│  │ 🍽️ │ │ 👀 │ │ 🙋 │         │ ← 一级卡片（紫色）
│  │ 吃  │ │ 看  │ │ 要  │         │
│  └─────┘ └─────┘ └─────┘         │
│  ┌─────┐ ┌─────┐ ┌─────┐         │
│  │ 🙅 │ │ 🏃 │ │ ✨ │         │
│  │不要 │ │ 去  │ │其他 │         │
│  └─────┘ └─────┘ └─────┘         │
└─────────────────────────────────────┘
```

## 功能说明

### 1. 两级沟通系统

- **一级卡片（动词）**: 吃、看、要、不要、去、其他
- **二级卡片（具体需求）**: 点击一级卡片后显示的具体表达

示例流程：
1. 点击「吃」→ 显示面包、饼干、水果等
2. 点击「面包」→ 播放「妈妈，我想吃面包！」

### 2. 紧急卡片

红色高亮显示，如「尿尿」「喝水」，一键点击即可表达需求。

### 3. 佩奇语音模式

使用阿里云CosyVoice语音克隆技术，生成小猪佩奇风格的语音。

### 4. 本地音频缓存

首次保存卡片时自动生成音频并缓存本地，后续点击即时播放，无需等待网络请求。

### 5. 家长模式

点击右上角 👤 图标进入家长模式，可：
- ⚙️ 配置阿里云API（语音合成）
- ✏️ 添加/编辑/删除卡片
- 📝 自定义卡片文字、语音

## 技术栈

| 类别 | 技术 |
|------|------|
| 框架 | Jetpack Compose |
| 语言 | Kotlin |
| 架构 | MVVM + Hilt DI |
| 数据库 | Room |
| 语音 | 阿里云 CosyVoice (NUI SDK) |
| 图片加载 | Coil |

## 快速开始

### 前提条件

- Android Studio Arctic Fox+
- Java 17
- Android SDK 34

### 编译

```bash
# 克隆项目
git clone https://github.com/dreamerzho/IntentBridge.git
cd IntentBridge

# 编译Debug APK
./gradlew assembleDebug

# APK位置
# app/build/outputs/apk/debug/app-debug.apk
```

### 配置阿里云API

1. 获取 [阿里云DashScope API Key](https://dashscope.console.aliyun.com/)
2. 进入设置页面，输入 API Key
3. 使用语音克隆功能需要先在阿里云创建音色

## 项目结构

```
app/src/main/java/com/intentbridge/
├── data/
│   ├── local/         # Room数据库
│   ├── model/        # 数据模型
│   └── repository/   # 数据仓库
├── di/                # Hilt依赖注入
├── service/          # 业务服务
│   ├── AliyunTTSService.kt    # 阿里云TTS
│   └── AudioCacheManager.kt   # 音频缓存
└── ui/
    ├── components/   # 可复用组件
    ├── screens/     # 页面
    └── theme/       # 主题样式
```

## 默认卡片

| 一级 | 二级 |
|------|------|
| 吃 | 面包、饼干、水果、奶酪、米饭、面条 |
| 看 | 佩奇、汪汪队、动画片、儿歌 |
| 要 | 玩具、书、气球 |
| 不要 | 这个、不吃、不玩 |
| 去 | 外面、公园、商场、学校 |
| 其他 | 抱抱、尿尿、喝水、睡觉、帮帮我 |

## 常见问题

### Q: 为什么没有声音？
A: 请确保已配置阿里云API Key，并处于家长模式下点击右上角 ⚙️ 图标进入设置。

### Q: 首次点击响应慢？
A: 首次点击会在线生成音频并缓存，之后将从本地极速播放。

### Q: 如何添加自定义卡片？
A: 进入家长模式 → 点击编辑图标 → 添加新卡片 → 填写标签和语音文字

## 许可证

MIT License - 查看 [LICENSE](LICENSE) 了解详情

## 致谢

- [阿里云DashScope](https://dashscope.console.aliyun.com/) - 语音合成服务
- [Peppa Pig](https://www.peppapig.com/) - 角色灵感
