<div align="center">

# 拾光南邮 (Shiguang Schedule · NJUPT Edition)

![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Compose%20Multiplatform-blue)
![License](https://img.shields.io/badge/License-GPL--3.0-green)
![NJUPT](https://img.shields.io/badge/NJUPT-南京邮电大学定制-orange)

专为**南京邮电大学（NJUPT）**学子深度定制的跨平台开源课程表应用。  
基于原版 [拾光课程表](https://github.com/ShiGuangSchedule/shiguangschedule) 进行专属二次开发，打通南邮教务正方系统、全校期末考务一键在线同步与离线兜底、桌面小组件倒计时联动及专属校园视觉定制。

</div>

---

## 🌟 南邮专属深度定制特性

### 1. 🎓 全校班级考务一键在线同步（首创）
- **数据直连**：无缝对接南邮 StuDev 官方考务静态镜像与教务处集中考试安排；
- **免手动敲字**：输入或记忆行政班级代码（如 `B241505`，默认占位 `B000000`），1 秒自动检索全学期考试科目、起止时间、考场校区及教室；
- **智能课表匹配**：自动比对当前学期所选课程代码，已选科目默认打勾预选，点击“一键导入”直接入库；
- **离线 .ics 兜底**：若遇断网或无网络环境，支持直接导入群文件中的 `.ics` 考务日历文件。

### 2. 🏛️ 南邮正方教务课表极速导入
- 教务导入列表首位**强力置顶“南京邮电大学正方教务（V9）”**，省去在全国千所院校中反复检索；
- 深度兼容南邮正方系统特性：单双周合并单元格（`rowspan="2"`）、调课标记（`【调】`）及混合周次（`1-3周(单),4-18周`）解析。

### 3. ⏰ 内置南邮标准 12 节作息时间
- 默认预设仙林/三牌楼校区标准的 12 节课作息：
  - 上午：第 1~5 节（08:00 - 12:15）
  - 下午：第 6~9 节（13:45 - 17:10）
  - 晚间：第 10~12 节（18:30 - 21:05）
- 新建或重置课表自动贴合本校作息，无需手动逐节调节。

### 4. 📱 桌面小组件全面联动
- **平滑顺滑滚动**：彻底修复原版考试倒计时卡片强行吸顶遮挡课程的交互问题，整体列表自然流畅滚动；
- **小组件实时协同**：桌面 4xN / 2x2 列表小组件自动同步今日课程、今日补课、今日待办以及**即将到来的下一场期末考试倒计时（距考试还有 N 天）**。

### 5. 🎨 全新南邮专属视觉设计
- **专属桌面图标**：经典南邮蓝（`#0E54B6`）底色融合南京邮电大学校徽与现代日程卡片设计；
- **桌面精炼排版**：图标名称统一为“**拾光南邮**”，工整不被手机桌面省略号截断。

---

## 🛠️ 构建与本地运行

项目基于 **Kotlin Multiplatform (KMP)** 与 **Jetpack Compose / Compose Multiplatform** 构建。

### 环境要求
- JDK 21（推荐 Eclipse Adoptium Temurin 21）
- Android SDK 34+
- Gradle 9.8+（工程自带 `gradlew` wrapper）

### 命令行构建 Release APK
```bash
# 构建 Android 专属正式安装包
./gradlew :androidApp:assembleRelease
```

产物路径：`androidApp/build/outputs/apk/release/`

---

## 📄 开源与致谢

- 本项目基于 [ShiGuangSchedule/shiguangschedule](https://github.com/ShiGuangSchedule/shiguangschedule) 遵循 GPL-3.0 协议进行二次开发与深度定制；
- 考务数据同步算法借鉴了 [NJUPT-CS-StuDev/studev](https://github.com/NJUPT-CS-StuDev/studev) 的开源成果；
- 感谢原项目团队及南邮开源社区的无私奉献。