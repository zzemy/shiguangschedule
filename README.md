<div align="center">

# 拾光南邮 (Shiguang Schedule · NJUPT Edition)

![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Compose%20Multiplatform-blue)
![License](https://img.shields.io/badge/License-GPL--3.0-green)
![NJUPT](https://img.shields.io/badge/NJUPT-南京邮电大学定制-orange)

面向**南京邮电大学（NJUPT）**的跨平台开源课程表应用。  
基于 [拾光课程表](https://github.com/ShiGuangSchedule/shiguangschedule) 进行本校深度定制，集成南邮正方教务系统、全校班级考务同步、智能日程添加、桌面小组件联动及校园视觉。

</div>

---

## 🌟 功能特性

### 1. 🎓 全校班级考务在线同步
- **在线同步**：输入行政班级代码即可同步全学期考试科目、起止时间、考场校区及教室；
- **智能比对**：自动比对当前学期所选课程，预选对应考试科目后一键导入；
- **离线支持**：支持导入 `.ics` 日历文件。

### 2. ✨ 智能自然语言日程添加
- **一句话添加**：输入自然语言句子（例如“明天下午五点去教三206上课”），系统自动识别日期、时段、地点、事件类型并提取事件标题；
- **一键填入**：识别结果提供推荐卡片，点击即可自动填入表单；
- **便捷修改**：支持点击日程与考试卡片进行编辑修改与管理。

### 3. ⏱️ 统一今日时间线
- **自然时序**：当天的课程与带有时间的日程、考试、补课按开始时间统一排序，形成清晰的时序流；
- **全天事项**：未指定具体起止时间的待办事项收纳在时间线上方的全天区域；
- **倒计时展示**：未来考试自动计算剩余天数并在顶部区域展示倒计时。

### 4. 🏛️ 南邮正方教务课表导入
- **快速定位**：教务导入列表置顶“南京邮电大学正方教务（V9）”；
- **完整排课解析**：支持单双周合并单元格、调课标记及多周次排课信息解析。

### 5. ⏰ 南邮标准 12 节作息时间
- 预设仙林与三牌楼校区标准的 12 节课作息时间：
  - 上午：第 1~5 节（08:00 - 12:15）
  - 下午：第 6~9 节（13:45 - 17:10）
  - 晚间：第 10~12 节（18:30 - 21:05）

### 6. 📱 桌面小组件联动
- 桌面小组件支持实时同步展示今日课程、待办事项、补课安排及近期考试倒计时。

### 7. 🎨 南邮专属视觉
- 桌面图标采用南邮蓝底色并融入校徽元素，桌面应用名称为“**拾光南邮**”。

---

## 🛠️ 构建与运行

项目基于 **Kotlin Multiplatform (KMP)** 与 **Jetpack Compose / Compose Multiplatform** 构建。

### 环境要求
- JDK 21（推荐 Eclipse Adoptium Temurin 21）
- Android SDK 34+
- Gradle 9.8+

### 命令行构建 Release APK
```bash
./gradlew :androidApp:assembleRelease
```

产物路径：`androidApp/build/outputs/apk/release/`

---

## 📄 开源与致谢

- 本项目基于 [ShiGuangSchedule/shiguangschedule](https://github.com/ShiGuangSchedule/shiguangschedule) 遵循 GPL-3.0 协议进行二次开发；
- 考务数据同步算法借鉴了 [NJUPT-CS-StuDev/studev](https://github.com/NJUPT-CS-StuDev/studev) 的开源实现；
- 感谢原项目团队及南邮开源社区的贡献。
