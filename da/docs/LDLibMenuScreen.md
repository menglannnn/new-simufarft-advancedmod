# LDLibMenuScreen 技术文档

## 概述

`LDLibMenuScreen` 是一个自定义的 Minecraft Forge 1.20.1 屏幕基类，用于创建基于 LDLib (LowDragLib) 的菜单界面。它解决了 LDLib 界面被其他模组误识别为容器界面（如箱子）的问题，使得这些界面可以在主菜单等没有玩家实体的环境中安全使用。

## 问题背景

### 原始问题

在 Minecraft 模组开发中，LDLib 的 `ModularUIGuiContainer` 继承自 `AbstractContainerScreen`，这导致：

1. **被识别为容器界面** - 其他模组（如 Inventory Tweaks、Mouse Tweaks）会将 LDLib 界面识别为箱子等容器界面
2. **主菜单崩溃** - 当在主菜单使用 LDLib 界面时，这些模组会尝试访问玩家实体（`Minecraft.getInstance().player`），导致 NullPointerException
3. **无法安全使用** - 在玩家为 null 的环境（如主菜单、服务器列表）中无法使用 LDLib 界面

### 解决方案

`LDLibMenuScreen` 通过以下方式解决这些问题：

1. **继承原版 Screen** - 而非 `AbstractContainerScreen`，避免被识别为容器
2. **内部持有 ModularUI** - 保留 LDLib 的渲染能力
3. **假的容器实现** - 创建 `FakeModularUIGuiContainer` 满足 LDLib Widget 的需求
4. **正确的渲染状态** - 设置 `RenderSystem.disableDepthTest()` 确保文字正确渲染

## 核心原理

### 类继承结构

```
Screen (原版)
    └── LDLibMenuScreen (自定义基类)
            └── ConfigSelectionMenuScreen (具体实现)

AbstractContainerScreen (原版)
    └── ModularUIGuiContainer (LDLib)
            └── ModConfigScreenLDLib (原有实现)
```

### 关键组件

#### 1. LDLibMenuScreen

**文件位置**: `src/main/java/com/xiaoliang/simukraft/client/gui/ldlib/LDLibMenuScreen.java`

**核心功能**:
- 继承 `Screen` 而非 `AbstractContainerScreen`
- 内部持有 `ModularUI` 实例
- 手动转发鼠标/键盘事件到 LDLib Widget
- 设置正确的渲染状态

**关键代码**:
```java
@Override
public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
    this.renderBackground(graphics);
    
    if (modularUI != null) {
        // 设置渲染状态，与 ModularUIGuiContainer 保持一致
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        
        graphics.pose().pushPose();
        graphics.pose().translate(guiLeft, guiTop, 0);
        
        WidgetGroup mainGroup = modularUI.mainGroup;
        if (mainGroup != null) {
            mainGroup.drawInBackground(graphics, uiMouseX, uiMouseY, partialTicks);
            mainGroup.drawInForeground(graphics, uiMouseX, uiMouseY, partialTicks);
        }
        
        graphics.pose().popPose();
        
        // 恢复渲染状态
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }
    
    super.render(graphics, mouseX, mouseY, partialTicks);
}
```

#### 2. FakeModularUIGuiContainer

**文件位置**: `src/main/java/com/xiaoliang/simukraft/client/gui/ldlib/FakeModularUIGuiContainer.java`

**作用**: 满足 LDLib Widget 对 `getModularUIGui()` 的调用需求

**背景**: LDLib 的 Widget 在初始化时会调用 `gui.getModularUIGui()`，如果返回 null 会导致 NullPointerException。`FakeModularUIGuiContainer` 创建一个假的容器实例，设置到 `ModularUI` 中，但不实际显示。

**关键代码**:
```java
public FakeModularUIGuiContainer(ModularUI modularUI) {
    super(new FakeHolder(), 0);
    this.holder = new FakeUIHolder();
    this.modularUI = modularUI;
    modularUI.setModularUIGui(this);
}
```

#### 3. Mixin 类

**MixinScreen** (`src/main/java/com/xiaoliang/simukraft/mixin/MixinScreen.java`):
- 阻止其他模组在 LDLib 容器屏幕上检查玩家实体
- 解决 Inventory Tweaks 等模组在屏幕初始化时的崩溃问题

**MixinForgeHooksClient** (`src/main/java/com/xiaoliang/simukraft/mixin/MixinForgeHooksClient.java`):
- 处理 Forge 客户端钩子中的玩家实体访问
- 防止在主菜单等环境中出现空指针异常

## 使用方法

### 1. 创建子类

```java
package com.example.mod.client.gui;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.Size;
import com.xiaoliang.simukraft.client.gui.ldlib.LDLibMenuScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MyMenuScreen extends LDLibMenuScreen {
    
    // 颜色定义
    private static final int COLOR_WINDOW_BG = 0xE6202020;
    private static final int COLOR_WINDOW_BORDER = 0xFF4A90A4;
    private static final int COLOR_BUTTON_BG = 0xFF3A5A6A;
    private static final int COLOR_BUTTON_HOVER = 0xFF4A7A8A;
    private static final int COLOR_TEXT_TITLE = 0xFFFFFFFF;
    private static final int COLOR_TEXT_NORMAL = 0xFFE0E0E0;
    
    // 尺寸定义
    private static final int WINDOW_WIDTH = 280;
    private static final int WINDOW_HEIGHT = 200;
    
    public MyMenuScreen(Screen parent) {
        super(Component.literal("我的菜单"), parent);
    }
    
    @Override
    protected int getUIWidth() {
        return WINDOW_WIDTH;
    }
    
    @Override
    protected int getUIHeight() {
        return WINDOW_HEIGHT;
    }
    
    @Override
    protected ModularUI createModularUI() {
        // 见下文
    }
}
```

### 2. 实现 createModularUI()

```java
@Override
protected ModularUI createModularUI() {
    ModularUI modularUI = new ModularUI(
        new Size(WINDOW_WIDTH, WINDOW_HEIGHT), 
        new MyUIHolder(), 
        null
    );
    
    // 根组件
    WidgetGroup rootGroup = new WidgetGroup();
    rootGroup.setSelfPosition(0, 0);
    rootGroup.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
    
    // 窗口背景
    WidgetGroup windowGroup = new WidgetGroup();
    windowGroup.setSelfPosition(0, 0);
    windowGroup.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
    windowGroup.setBackground(new GuiTextureGroup(
        new ColorRectTexture(COLOR_WINDOW_BG).setRadius(10),
        new ColorBorderTexture(2, COLOR_WINDOW_BORDER).setRadius(10)
    ));
    rootGroup.addWidget(windowGroup);
    
    // 添加标题（使用 TextTexture + ImageWidget）
    TextTexture titleTexture = new TextTexture("菜单标题", COLOR_TEXT_TITLE);
    titleTexture.setType(TextTexture.TextType.NORMAL);
    titleTexture.setWidth(WINDOW_WIDTH - 4);
    windowGroup.addWidget(new ImageWidget(0, 12, WINDOW_WIDTH - 4, 16, titleTexture));
    
    // 添加按钮
    windowGroup.addWidget(createButton(40, 60, 200, 24, "点击我", 
        clickData -> onButtonClick()));
    
    modularUI.widget(rootGroup);
    return modularUI;
}
```

### 3. 创建按钮

```java
private ButtonWidget createButton(int x, int y, int width, int height,
                                   String text,
                                   java.util.function.Consumer<ClickData> callback) {
    ButtonWidget button = new ButtonWidget();
    button.setSelfPosition(x, y);
    button.setSize(width, height);
    button.setOnPressCallback(callback);
    
    // 正常状态
    TextTexture normalText = new TextTexture(text, COLOR_TEXT_NORMAL);
    normalText.setType(TextTexture.TextType.NORMAL);
    normalText.setWidth(width);
    button.setBackground(new GuiTextureGroup(
        new ColorRectTexture(COLOR_BUTTON_BG).setRadius(4),
        new ColorBorderTexture(1, COLOR_WINDOW_BORDER).setRadius(4),
        normalText
    ));
    
    // 悬停状态
    TextTexture hoverText = new TextTexture(text, COLOR_TEXT_TITLE);
    hoverText.setType(TextTexture.TextType.NORMAL);
    hoverText.setWidth(width);
    button.setHoverTexture(new GuiTextureGroup(
        new ColorRectTexture(COLOR_BUTTON_HOVER).setRadius(4),
        new ColorBorderTexture(1, COLOR_WINDOW_BORDER).setRadius(4),
        hoverText
    ));
    
    return button;
}
```

### 4. 实现 IUIHolder

```java
private static class MyUIHolder implements IUIHolder {
    
    @Override
    public ModularUI createUI(Player entityPlayer) {
        return createModularUI();
    }
    
    public ModularUI createModularUI() {
        // 返回上面创建的 UI
        return null; // 实际实现中返回创建的 UI
    }
    
    @Override
    public boolean isInvalid() { 
        return false; 
    }
    
    @Override
    public boolean isRemote() { 
        return true; 
    }
    
    @Override
    public void markAsDirty() {
        // 空实现
    }
}
```

### 5. 打开界面

```java
// 在游戏中打开
Minecraft.getInstance().setScreen(new MyMenuScreen(currentScreen));

// 在主菜单打开（安全，不会崩溃）
Minecraft.getInstance().setScreen(new MyMenuScreen(new TitleScreen()));
```

## 关键要点

### 文字渲染

**必须使用 `TextTexture`**:
- `TextTexture` 在 `drawInternal` 中直接调用 `graphics.drawString()`
- 不依赖于 `ModularUIGuiContainer` 的完整初始化
- 在没有玩家实体的环境中也能正常工作

**不要使用 `LabelWidget`**:
- `LabelWidget` 依赖于完整的 LDLib GUI 初始化
- 在 `LDLibMenuScreen` 环境中可能无法正确渲染

### 渲染状态

`LDLibMenuScreen` 已自动处理以下渲染状态：

```java
RenderSystem.disableDepthTest();
RenderSystem.depthMask(false);
// ... 渲染 LDLib UI ...
RenderSystem.enableDepthTest();
RenderSystem.depthMask(true);
```

这是确保文字正确显示的关键。

### 事件处理

`LDLibMenuScreen` 自动转发以下事件到 LDLib Widget：
- 鼠标点击 (`mouseClicked`)
- 鼠标释放 (`mouseReleased`)
- 鼠标拖动 (`mouseDragged`)
- 鼠标滚轮 (`mouseScrolled`)
- 键盘按下/释放 (`keyPressed`, `keyReleased`)
- 字符输入 (`charTyped`)

### 自动缩放支持

`LDLibMenuScreen` 内置 `GuiScaleManager` 自动缩放支持：

**默认行为**:
- 自动启用缩放（`enableAutoScale()` 返回 `true`）
- 首选 3x 缩放（`getPreferredScale()` 返回 `3`）
- 自动降级到 2x/1x 如果界面无法完整显示

**禁用缩放**:
```java
@Override
protected boolean enableAutoScale() {
    return false; // 禁用自动缩放
}
```

**调整首选缩放**:
```java
@Override
protected int getPreferredScale() {
    return 2; // 首选 2x 缩放
}
```

**缩放时机**:
- `init()`: 应用缩放
- `render()`: 保持缩放状态
- `onClose()`: 恢复原始缩放
- `ESC键`: 通过 `handleEscKey` 处理，确保恢复缩放

## 相关文件

| 文件 | 位置 | 说明 |
|------|------|------|
| LDLibMenuScreen | `client/gui/ldlib/LDLibMenuScreen.java` | 基类，继承 Screen |
| FakeModularUIGuiContainer | `client/gui/ldlib/FakeModularUIGuiContainer.java` | 假的容器实现 |
| ConfigSelectionMenuScreen | `client/gui/ldlib/ConfigSelectionMenuScreen.java` | 示例实现 |
| MixinScreen | `mixin/MixinScreen.java` | 阻止玩家检查 |
| MixinForgeHooksClient | `mixin/MixinForgeHooksClient.java` | Forge 钩子处理 |
| ModMenuIntegration | `client/config/ModMenuIntegration.java` | ModMenu 集成 |
| ConfigButtonHandler | `client/gui/ConfigButtonHandler.java` | 配置按钮处理 |

## 与原有实现的对比

| 特性 | LDLibMenuScreen | ModularUIGuiContainer |
|------|-----------------|----------------------|
| 继承 | `Screen` | `AbstractContainerScreen` |
| 被识别为容器 | ❌ 否 | ✅ 是 |
| 主菜单可用 | ✅ 是 | ❌ 否（会崩溃）|
| 需要玩家实体 | ❌ 否 | ❌ 否 |
| 缩放支持 | 需手动实现 | 内置支持 |
| 完整 LDLib 功能 | ⚠️ 部分 | ✅ 完整 |

## 注意事项

1. **不要访问玩家实体** - 虽然 `LDLibMenuScreen` 可以在主菜单使用，但你的代码仍不应假设玩家实体存在
2. **使用 TextTexture** - 始终使用 `TextTexture` 显示文字，不要使用 `LabelWidget`
3. **测试主菜单** - 在发布前务必在主菜单环境中测试
4. **Mixin 依赖** - 确保 Mixin 配置正确，否则其他模组仍可能导致崩溃

## 示例：完整实现

参见 `ConfigSelectionMenuScreen.java` 获取完整的实现示例，包括：
- 多按钮布局
- 禁用状态按钮
- 链接打开功能
- 界面导航
