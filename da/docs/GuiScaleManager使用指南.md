# GUI缩放管理指南

## 概述

`GuiScaleManager` 是一个统一管理 Minecraft GUI 缩放的工具类，用于在配置界面中临时使用 3x 缩放，退出时恢复原始缩放。

## 核心类

### GuiScaleManager

位置：`com.xiaoliang.simukraft.client.gui.GuiScaleManager`

#### 主要方法

| 方法 | 说明 |
|------|------|
| `apply3x()` | 应用 3x 缩放，自动保存原始值 |
| `applyBestFitScale(int, int)` | 优先 3x，放不下自动降级为 2x/1x |
| `applyBestFitScale(int, int, int, int)` | 自定义首选缩放和边距，自动选择最佳缩放 |
| `restore()` | 恢复原始缩放 |
| `forceRestore()` | 强制恢复原始缩放并重置状态 |
| `reset()` | 重置状态（用于返回到非3x界面时） |
| `handleEscKey(int, Runnable)` | 统一处理ESC键返回 |
| `readOriginalScale()` | 从 options.txt 读取原始缩放值 |
| `setScale(int scale)` | 设置指定缩放值 |

## 使用方式

### 1. 固定 3x 缩放（传统用法）

在 LDLib 实现的界面中使用固定 3x 缩放：

```java
public class ConfigScreen extends ModularUIGuiContainer {
    private final Screen parent;
    private static ConfigScreen currentInstance;

    public ConfigScreen(Screen parent) {
        super(createHolderAndUI(parent), 0);
        this.parent = parent;
        currentInstance = this;
        // 构造函数中应用 3x 缩放
        GuiScaleManager.apply3x();
    }

    @Override
    public void onClose() {
        // 关闭时恢复原始缩放
        GuiScaleManager.restore();
        currentInstance = null;
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 使用GuiScaleManager统一处理ESC键，确保恢复缩放
        if (GuiScaleManager.handleEscKey(keyCode, this::onClose)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // 渲染时保持 3x 缩放
        GuiScaleManager.apply3x();
        super.render(graphics, mouseX, mouseY, partialTicks);
    }
}
```

### 2. 自适应最佳缩放（推荐用于大界面）

对于内容尺寸较大的界面，使用自适应缩放避免超出屏幕：

```java
public class LargeConfigScreen extends Screen {
    private static final int CONTENT_WIDTH = 800;
    private static final int CONTENT_HEIGHT = 600;

    public LargeConfigScreen(Screen parent) {
        super(Component.literal("Large Config"));
        this.parent = parent;
        // 自动选择能完整显示内容的最大缩放（优先3x，放不下则降级）
        GuiScaleManager.applyBestFitScale(CONTENT_WIDTH, CONTENT_HEIGHT);
    }

    @Override
    public void onClose() {
        GuiScaleManager.restore();
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // 保持最佳缩放
        GuiScaleManager.applyBestFitScale(CONTENT_WIDTH, CONTENT_HEIGHT);
        super.render(graphics, mouseX, mouseY, partialTicks);
    }
}
```

### 3. 自定义首选缩放和边距

```java
// 首选 4x 缩放，内容尺寸 1000x700，边距 20
GuiScaleManager.applyBestFitScale(4, 1000, 700, 20);
```

### 4. 返回按钮处理

确保返回按钮也触发缩放恢复：

```java
private void closeScreen() {
    // 调用 onClose 来恢复缩放
    if (currentInstance != null) {
        currentInstance.onClose();
    }
}
```

## 关键点

### 1. 静态实例变量

```java
private static ConfigScreen currentInstance;
```

- 供内部类访问外部实例
- 在构造函数中设置：`currentInstance = this`
- 在 onClose 中清除：`currentInstance = null`

### 2. 缩放设置时机

| 时机 | 操作 | 说明 |
|------|------|------|
| 构造函数 | `apply3x()` / `applyBestFitScale()` | 界面打开立即应用缩放 |
| render | `apply3x()` / `applyBestFitScale()` | 保持缩放状态 |
| onClose | `restore()` | 恢复原始缩放 |
| ESC键 | `handleEscKey()` | 确保调用onClose恢复缩放 |

### 3. ESC键处理

**必须**重写 `keyPressed` 方法并使用 `handleEscKey`：

```java
@Override
public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (GuiScaleManager.handleEscKey(keyCode, this::onClose)) {
        return true;
    }
    return super.keyPressed(keyCode, scanCode, modifiers);
}
```

**注意**：如果不处理ESC键，直接 `setScreen(parent)` 会导致 `onClose()` 不被调用，缩放无法恢复。

### 4. 原始值读取

从 `options.txt` 读取 `guiScale` 配置：

```java
private int readOriginalScale() {
    File optionsFile = new File(Minecraft.getInstance().gameDirectory, "options.txt");
    // 解析 guiScale:xxx 行
}
```

### 5. 自适应缩放算法

`applyBestFitScale` 的工作原理：

1. 保存原始缩放值（仅首次调用时）
2. 从首选缩放（默认 3）开始向下尝试
3. 计算当前缩放级别下窗口的可用尺寸：`windowSize / scale`
4. 检查内容尺寸（含边距）是否能放入可用尺寸
5. 返回能完整放下界面的最大缩放级别
6. 如果所有级别都放不下，最终返回 1x

边距说明：
- 默认边距为 16 像素
- 边距用于预留窗口边框、标题栏等空间
- 可通过 `applyBestFitScale(int, int, int, int)` 自定义

## 现有实现

### 使用3x缩放的界面

以下界面都使用 `GuiScaleManager` 管理缩放：

1. **ModConfigScreenLDLib** - 客户端配置界面
2. **ModMenuIntegration.ConfigSelectionScreenLDLib** - ModMenu配置选择界面
3. **ServerConfigScreen** - 服务器配置界面
4. **ConfigurableListScreenLDLib** - 列表配置界面基类
   - BlockBlacklistScreenLDLib - 方块黑名单
   - BasicMaterialsScreenLDLib - 基础材料
   - ExpertModeSkipListScreenLDLib - 专家模式跳过列表
5. **MaterialCategoryGroupsScreenLDLib** - 材料分类组配置
6. **UpdateScreenLDLib** - 更新检查界面
7. **ConfigSelectionMenuScreen** (LDLibMenuScreen) - 配置选择界面

### HUDPositionEditorScreen

**保持原始比例**，不参与3x缩放管理。在构造函数中恢复原始缩放：
```java
public HUDPositionEditorScreen(Screen parent) {
    // ...
    GuiScaleManager.restore(); // 恢复原始缩放
}
```

## 注意事项

1. **构造函数中立即应用**：确保界面打开就是目标缩放
2. **所有关闭方式走 onClose()**：ESC 和返回按钮行为一致
3. **必须处理ESC键**：重写 `keyPressed` 并使用 `handleEscKey`
4. **从 options.txt 读取原始值**：确保恢复正确的缩放
5. **HUDPositionEditorScreen 保持原始比例**：不参与 3x 缩放管理
6. **大界面使用自适应缩放**：避免固定 3x 导致内容超出屏幕
7. **合理设置边距**：确保界面元素不会被窗口边框遮挡

## 示例流程

假设设置文件中 `guiScale:6`：

### 基本流程（固定3x）

```
1. 打开 ModMenuIntegration
   - 读取原始值 6
   - 设置 scale=3
   
2. 打开 ServerConfigScreen
   - 设置 scale=3
   
3. 关闭 ServerConfigScreen
   - 恢复 scale=6
   
4. 关闭 ModMenuIntegration
   - 恢复 scale=6
```

### 自适应缩放流程

```
1. 打开 LargeConfigScreen（窗口 1920x1080，内容 800x600）
   - 读取原始值 6
   - 尝试 scale=3: 可用 640x360，放不下 800x600 → 跳过
   - 尝试 scale=2: 可用 960x540，放得下 800x600 → 使用 2x
   
2. 关闭 LargeConfigScreen
   - 恢复 scale=6
```

### HUD编辑器流程

```
1. 打开 ModConfigScreenLDLib
   - 读取原始值 6
   - 设置 scale=3
   
2. 打开 HUDPositionEditorScreen
   - 调用 restore()
   - 恢复 scale=6
   - HUD编辑器保持原始比例
   
3. 关闭 HUDPositionEditorScreen
   - 返回 ModConfigScreenLDLib
   - render() 中 apply3x()
   - 设置 scale=3
   
4. 关闭 ModConfigScreenLDLib
   - 恢复 scale=6
```

### LDLibMenuScreen 流程

```
1. 打开 ConfigSelectionMenuScreen (继承 LDLibMenuScreen)
   - init() 中自动调用 applyBestFitScale(3, 280, 320, 16)
   - 读取原始值 6
   - 尝试 scale=3: 检查 280x320 是否能放入当前窗口
   - 设置 scale=3 (或降级到 2x/1x)
   
2. 渲染时
   - render() 中保持缩放状态
   
3. 按 ESC 关闭
   - keyPressed() 中 handleEscKey 调用 onClose()
   - onClose() 中 restore() 恢复原始缩放
   - 恢复 scale=6
```

**LDLibMenuScreen 特点**:
- 自动管理缩放，无需手动调用 `apply3x()`
- 支持 `enableAutoScale()` 和 `getPreferredScale()` 自定义
- ESC 键自动处理，确保缩放恢复
