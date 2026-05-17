# 指南书系统使用文档

## 概述

指南书系统是一个 JSON 驱动的动态内容管理系统，支持多章节、多页面、物品引用、链接跳转等功能。

## 文件结构

```
src/main/resources/assets/simukraft/guidebook/
├── index.json          # 索引文件，定义页面列表
├── directory.json      # 目录页面
├── prologue.json       # 序章
├── city.json           # 城市系统章节
├── commerce.json       # 商业系统章节
├── logistics.json      # 物流系统章节
└── lang/
    ├── zh_cn.json      # 中文语言文件
    └── en_us.json      # 英文语言文件
```

## 索引文件 (index.json)

定义指南书包含的所有页面文件：

```json
{
  "index_page": "directory",
  "pages": [
    "directory.json",
    "prologue.json",
    "city.json",
    "commerce.json",
    "logistics.json"
  ]
}
```

## 章节文件格式

### 基本结构

```json
{
  "parent": "directory",           // 父页面ID，目录页为根
  "tab_icon": "tab_city",          // 标签图标名称
  "tab_index": 1,                  // 标签排序索引
  "pages": [                       // 页面列表
    {
      "left_title": "guidebook.chapter.city.page1.left_title",
      "right_title": "guidebook.chapter.city.page1.right_title",
      "left_content": [...],       // 左侧内容元素
      "right_content": [...]       // 右侧内容元素
    }
  ]
}
```

### 内容元素类型

#### 1. 文本元素 (text)

```json
{
  "type": "text",
  "content": "guidebook.chapter.city.page1.left_1",
  "spacing": 8
}
```

#### 2. 提示元素 (hint)

```json
{
  "type": "hint",
  "content": "guidebook.chapter.city.page1.right_1",
  "spacing": 0
}
```

#### 3. 物品引用 (item)

```json
{
  "type": "item",
  "item_id": "simukraft:city_core",
  "spacing": 4
}
```

#### 4. 链接元素 (link)

```json
{
  "type": "link",
  "content": "guidebook.directory.city.name",
  "target": "city",
  "spacing": 4
}
```

## 语言文件格式

语言文件位于 `guidebook/lang/` 目录下：

```json
{
  "_comment": "指南书独立语言文件 - 中文",

  "guidebook.title": "新模拟大都市开拓手册",
  "guidebook.chapter.city.page1.left_1": "1. 放置城市核心并完成城市创建。",

  // 支持变量替换
  "guidebook.chapter.prologue.page1.left_1": "$player_name，你也许依旧厌倦了四处奔走的日子，"
}
```

### 支持的变量

- `$player_name` - 当前玩家名称

## 标签图标

标签图标纹理位于 `textures/gui/guide_book/` 目录：

- `tab_city.png` - 城市系统
- `tab_commerce.png` - 商业系统
- `tab_logistics.png` - 物流系统
- `tab_return.png` - 返回按钮

## 热重载

在游戏中修改 JSON 文件后，执行以下指令热重载：

```
/simukraft reload
```

系统会向所有在线客户端发送重载指令，无需重启游戏即可看到更新。

## 代码架构

### 核心类

| 类名 | 职责 |
|------|------|
| `GuideBookScreen` | 指南书UI渲染 |
| `GuideBookPage` | 页面数据模型 |
| `GuideBookLoader` | JSON资源加载 |
| `GuideBookLang` | 语言文件管理 |

### 数据流

1. 游戏启动时，`GuideBookLoader` 从资源包加载所有 JSON 文件
2. `GuideBookLang` 加载对应语言的文本
3. 打开指南书时，`GuideBookScreen` 根据当前章节渲染页面
4. 翻页时更新 `currentPageIndex`，重新渲染内容

## 最佳实践

1. **章节顺序**: 使用 `tab_index` 控制章节在侧边栏的显示顺序
2. **页面规划**: 每个章节可包含多页，通过 `pages` 数组定义
3. **语言分离**: 所有文本内容放在语言文件中，JSON 只引用语言键
4. **图标复用**: 可复用现有图标，或添加新图标到纹理目录
5. **链接导航**: 使用 `link` 类型元素实现章节间跳转

## 调试

启用详细日志查看加载过程：

```
[GuideBookLoader] Hot reloading guidebook pages...
[GuideBookLoader] Loading index from simukraft:guidebook/index.json
[GuideBookLoader] Found 5 page files in index
[GuideBookLoader] Successfully loaded page 'city' with 2 pages
```
