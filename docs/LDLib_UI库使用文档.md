# LDLib Java UI/API 完整参考手册

> 基于 LDLib 官方文档整理，仅包含 Java 实现部分。
> 官方文档：https://low-drag-mc.github.io/LowDragMC-Doc/ldlib/

---

## 目录

1. [项目集成](#1-项目集成)
2. [UI 系统概述](#2-ui-系统概述)
3. [快速入门](#3-快速入门)
4. [从文件加载 UI](#4-从文件加载-ui)
5. [Widget 基类通用 API](#5-widget-基类通用-api)
6. [容器组件 - WidgetGroup](#6-容器组件---widgetgroup)
7. [按钮组件 - ButtonWidget](#7-按钮组件---buttonwidget)
8. [图片组件 - ImageWidget](#8-图片组件---imagewidget)
9. [文本标签 - LabelWidget](#9-文本标签---labelwidget)
10. [富文本组件 - TextTextureWidget](#10-富文本组件---texttexturewidget)
11. [物品槽 - SlotWidget](#11-物品槽---slotwidget)
12. [幻影物品槽 - PhantomSlotWidget](#12-幻影物品槽---phantomslotwidget)
13. [流体槽 - TankWidget](#13-流体槽---tankwidget)
14. [幻影流体槽 - PhantomTankWidget](#14-幻影流体槽---phantomtankwidget)
15. [开关组件 - SwitchWidget](#15-开关组件---switchwidget)
16. [下拉选择 - SelectorWidget](#16-下拉选择---selectorwidget)
17. [文本输入 - TextFieldWidget](#17-文本输入---textfieldwidget)
18. [进度条 - ProgressWidget](#18-进度条---progresswidget)
19. [GUI 纹理系统](#19-gui-纹理系统)
20. [SyncData 数据同步系统](#20-syncdata-数据同步系统)
21. [Compass 文档系统](#21-compass-文档系统)

---

## 1. 项目集成

### Maven 仓库

```groovy
repositories {
    maven {
        url "https://maven.firstdark.dev/snapshots"
    }
}
```

### Forge

```groovy
dependencies {
    implementation fg.deobf("com.lowdragmc.ldlib:ldlib-forge-{minecraft_version}:{latest_version}")
}
```

### Fabric

```groovy
dependencies {
    modImplementation "com.lowdragmc.ldlib:ldlib-fabric-{minecraft_version}:{latest_version}"
}
```

### Architectury-Common

```groovy
dependencies {
    modCompileOnly "com.lowdragmc.ldlib:ldlib-common-{minecraft_version}:{latest_version}"
}
```

---

## 2. UI 系统概述

LDLib 提供丰富的开箱即用组件，支持创建高级 GUI 控件。推荐工作流：

1. 使用 **UI Editor** 可视化设计界面布局
2. 使用 **Java** 代码绑定交互逻辑

也可纯代码创建 UI。

---

## 3. 快速入门

### 3.1 创建 UI 组件和布局

```java
public WidgetGroup createUI() {
    var root = new WidgetGroup();
    root.setSize(100, 100);
    root.setBackground(ResourceBorderTexture.BORDERED_BACKGROUND);

    var label = new LabelWidget();
    label.setSelfPosition(20, 20);
    label.setText("Hello, World!");

    var button = new ButtonWidget();
    button.setSelfPosition(20, 60);
    button.setSize(60, 20);

    var backgroundImage = ResourceBorderTexture.BUTTON_COMMON;
    var hoverImage = backgroundImage.copy().setColor(ColorPattern.CYAN.color);
    var textAbove = new TextTexture("Click me!");
    button.setButtonTexture(backgroundImage, textAbove);
    button.setClickedTexture(hoverImage, textAbove);

    root.addWidgets(label, button);
    return root;
}
```

### 3.2 绑定功能逻辑

```java
public WidgetGroup createUI() {
    // ... 创建组件代码 ...

    AtomicInteger counter = new AtomicInteger(0);
    button.setOnPressCallback(clickData -> {
        label.setText("Clicked " + counter.incrementAndGet() + " times!");
    });

    return root;
}
```

### 3.3 BlockEntity 中打开 UI

```java
public class TestBlockEntity extends BlockEntity implements IUIHolder {

    public void onPlayerUse(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            BlockEntityUIFactory.INSTANCE.openUI(this, serverPlayer);
        }
    }

    private WidgetGroup createUI() {
        // ... 构建 UI ...
    }

    @Override
    public ModularUI createUI(Player entityPlayer) {
        return new ModularUI(createUI(), this, entityPlayer);
    }
}
```

### 3.4 手持物品中打开 UI

```java
public class TestItem implements IUIHolder.Item {

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
            HeldItemUIFactory.INSTANCE.openUI(serverPlayer, context.getHand());
        }
        return InteractionResult.SUCCESS;
    }

    private WidgetGroup createUI() {
        // ... 构建 UI ...
    }

    @Override
    public ModularUI createUI(Player entityPlayer, HeldItemUIFactory.HeldItemHolder holder) {
        return new ModularUI(createUI(), holder, entityPlayer);
    }
}
```

---

## 4. 从文件加载 UI

使用 UI Editor 设计的界面可通过代码加载并绑定逻辑：

```java
// 加载 UI 文件（会缓存资源以提高性能）
var creator = UIProject.loadUIFromFile(new ResourceLocation("ldlib:test_ui"));
WidgetGroup root = creator.get();

// 通过 ID 查找组件
var button = root.getFirstWidgetById("button_id");   // 返回第一个匹配
var allButtons = root.getWidgetsById("button_.*");    // 正则匹配，返回所有

// 绑定逻辑
if (button != null) {
    button.setOnPressCallback(clickData -> {
        // 处理点击
    });
}
```

> **注意**：需要在 UI Editor 中为组件分配 ID，才能通过代码检索。

---

## 5. Widget 基类通用 API

所有 Widget 共享以下属性和方法：

### 基本属性

| 属性 | 类型 | 说明 |
|------|------|------|
| `id` | String | 标识符，可为空，无需唯一 |
| `selfPosition` | Position | 相对父组件的本地位置 |
| `parentPosition` | Position | 父组件的全局位置 |
| `position` | Position | 窗口中的全局位置 |
| `size` | Size | 组件尺寸，影响碰撞检测 |
| `isVisible` | boolean | 是否可见（仅影响渲染） |
| `isActive` | boolean | 逻辑是否运行 |
| `align` | Align | 相对父级的对齐方式 |
| `backgroundTexture` | IGuiTexture | 背景纹理 |
| `hoverTexture` | IGuiTexture | 鼠标悬停纹理 |
| `overlay` | IGuiTexture | 覆盖纹理 |
| `parent` | Widget | 父组件引用 |

### 通用方法

```java
// 设置悬停提示
widget.setHoverTooltips("this is a button");

// 设置背景纹理
widget.setBackground(new ResourceTexture("ldlib:textures/gui/icon.png"));

// 设置悬停纹理
widget.setHoverTexture(texture);

// 检测鼠标是否在组件上
boolean hover = widget.isMouseOverElement(mouseX, mouseY);

// 设置位置和尺寸
widget.setSelfPosition(x, y);
widget.setSize(width, height);
```

---

## 6. 容器组件 - WidgetGroup

管理子组件的容器。

### 属性

| 属性 | 说明 |
|------|------|
| `widgets` | 子组件列表（只读，不要直接操作） |
| `layout` | 子组件排列方式 |
| `layoutPadding` | 布局内边距 |

### API

```java
// 添加子组件
group.addWidgets(button, label);

// 移除子组件
var child = group.getFirstWidgetById("button_id");
group.removeWidget(child);

// 清空所有子组件
group.clearAllWidget();

// 异步添加/移除（下一tick在主线程处理，适用于迭代和多线程场景）
group.waitToAdded(newWidget);
group.waitToRemoved(oldWidget);

// 遍历子组件
for (Widget child : group.widgets) {
    // ...
}
```

> **重要**：不要直接操作 `group.widgets` 列表，必须使用 `addWidgets` / `removeWidget` 等方法。

---

## 7. 按钮组件 - ButtonWidget

可点击的按钮，支持自定义纹理和回调。

### 属性

| 属性 | 说明 |
|------|------|
| `isClicked` | 当前是否被按下 |

### API

```java
// 设置按钮纹理（等同于 setBackground）
button.setButtonTexture(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("Button"));

// 设置按下状态纹理
button.setClickedTexture(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("Clicked"));

// 绑定点击回调
button.setOnPressCallback(clickData -> {
    int mouseButton = clickData.button;      // 0=左键, 1=右键, 2=中键
    boolean shift = clickData.isShiftClick;  // Shift 是否按下
    boolean ctrl = clickData.isCtrlClick;    // Ctrl 是否按下
    boolean remote = clickData.isRemote;     // 是否远程端
});
```

---

## 8. 图片组件 - ImageWidget

显示 GUI 纹理图片。

### 属性

| 属性 | 说明 |
|------|------|
| `border` | 边框宽度（-100 ~ 100） |
| `borderColor` | 边框颜色 |

### API

```java
// 直接设置纹理
imageWidget.setImage(new ResourceTexture("ldlib:textures/gui/icon.png"));

// 动态纹理（Supplier）
imageWidget.setImage(() -> new ResourceTexture("ldlib:textures/gui/icon.png"));

// 获取当前纹理
var texture = imageWidget.getImage();

// 设置边框
imageWidget.setBorder(2, 0xFFFFFFFF); // 宽度, ARGB颜色
```

---

## 9. 文本标签 - LabelWidget

轻量级文本显示组件。固定文本高度和对齐方式；若需高级文本控制，使用 `TextTextureWidget`。

### 属性

| 属性 | 说明 |
|------|------|
| `color` | 文本颜色（int） |
| `dropShadow` | 是否启用阴影 |
| `lastTextValue` | 当前文本（只读） |

### API

```java
// 设置文本
label.setText("New Label Text");

// 设置 Component 文本
label.setComponent(Component.literal("Hello"));

// 动态文本（每tick刷新）
label.setTextProvider(() -> "Dynamic Text");

// 设置颜色（ARGB格式，会替换富文本样式）
label.setColor(0xFFFFFFFF);

// 启用/禁用阴影
label.setDropShadow(true);
```

---

## 10. 富文本组件 - TextTextureWidget

高级文本组件，内部包装 `TextTexture`，支持丰富的文本渲染自定义。

### 属性

| 属性 | 说明 |
|------|------|
| `lastComponent` | 当前显示文本（只读） |
| `textTexture` | 内部 TextTexture 实例（只读） |

### API

```java
// 修改内部纹理样式
textTextureWidget.textureStyle(texture -> {
    texture.setType(TextType.ROLL);
    texture.setRollSpeed(0.5);
});

// 设置字符串文本
textTextureWidget.setText("Hello World");

// 设置 Component 文本
textTextureWidget.setText(Component.literal("Hello World"));

// 动态文本
textTextureWidget.setText(() -> "dynamic text");
```

---

## 11. 物品槽 - SlotWidget

容器 GUI 中的交互式物品槽。**注意：不能修改 SlotWidget 的尺寸。**

### 属性

| 属性 | 说明 |
|------|------|
| `canTakeItems` | 是否允许取出物品 |
| `canPutItems` | 是否允许放入物品 |
| `drawHoverOverlay` | 鼠标悬停时是否绘制覆盖层 |
| `drawHoverTips` | 鼠标悬停时是否显示提示 |
| `lastItem` | 之前存储的物品 |

### API

```java
// 绑定到 Container 的指定索引（如玩家背包）
slotWidget.setContainerSlot(container, index);

// 绑定到 ItemTransfer handler
// Forge/Fabric handler 需要先通过 ItemTransferHelperImpl 转换
slotWidget.setHandlerSlot(itemTransfer, slotIndex);

// 设置物品
slotWidget.setItem(itemStack);          // 触发监听器
slotWidget.setItem(itemStack, false);   // 不触发监听器

// 获取物品
ItemStack item = slotWidget.getItem();

// 设置内容变化监听器
slotWidget.setChangeListener((previousItem, currentItem) -> {
    // 处理变化
});

// 控制放入/取出权限
slotWidget.canPutStack(true);
slotWidget.canTakeStack(true);

// 设置位置信息（影响 Shift+Click 行为）
slotWidget.setLocationInfo(isPlayerContainer, isPlayerHotbar);
```

---

## 12. 幻影物品槽 - PhantomSlotWidget

不与实际背包交互的虚拟物品槽，通常用于配方界面。继承 `SlotWidget` 的所有 API。

### 额外属性

| 属性 | 说明 |
|------|------|
| `maxStackSize` | 最大堆叠数 |
| `clearSlotOnRightClick` | 右键是否清空槽位 |

### 额外 API

```java
// 设置右键清空
phantomSlot.setClearSlotOnRightClick(true);

// 设置最大堆叠数
phantomSlot.setMaxStackSize(64);
```

### 鼠标交互

- **左键（空）**：放入物品
- **左键（有物品）**：替换物品
- **右键（有物品）**：减少堆叠数
- **Shift+Click**：动态调整数量
- **右键（空，如启用）**：清空槽位

兼容 JEI/EMI 幽灵物品系统。

---

## 13. 流体槽 - TankWidget

显示流体并支持流体交互（灌入/排出）的容器组件。

### 属性

| 属性 | 说明 |
|------|------|
| `fluidTank` | 流体存储/传输 handler |
| `tank` | 槽位索引 |
| `showAmount` | 是否显示流体量 |
| `allowClickFilled` | 点击已填充槽是否允许灌入容器 |
| `allowClickDrained` | 点击空槽是否允许排出容器 |
| `drawHoverOverlay` | 悬停覆盖层 |
| `drawHoverTips` | 悬停提示 |
| `fillDirection` | 渲染方向（如 DOWN_TO_UP） |
| `lastFluidInTank` | 之前的流体 |
| `lastTankCapacity` | 之前的容量 |

### API

```java
// 绑定到 IFluidStorage（默认 tank 索引 0）
tankWidget.setFluidTank(fluidStorage);

// 绑定到 IFluidTransfer 并指定 tank 索引
tankWidget.setFluidTank(fluidTransfer, 1);

// 设置流体
tankWidget.setFluid(fluidStack);          // 触发监听器
tankWidget.setFluid(fluidStack, false);   // 不触发

// 获取流体
FluidStack fluid = tankWidget.getFluid();

// 设置变化监听器
tankWidget.setChangeListener((previousFluid, currentFluid) -> {
    // 处理变化
});
```

---

## 14. 幻影流体槽 - PhantomTankWidget

虚拟流体槽，不进行实际流体传输。继承 `TankWidget` 的所有 API。

### 额外 API

```java
// 设置流体更新回调
phantomTank.setIFluidStackUpdater(fluid -> {
    System.out.println("New phantom fluid: " + fluid);
});
```

兼容 JEI/EMI/REI 拖放操作。

---

## 15. 开关组件 - SwitchWidget

在 ON/OFF 状态之间切换的按钮。

### 属性

| 属性 | 说明 |
|------|------|
| `isPressed` | 当前状态（默认 false） |

### API

```java
// 设置状态
switchWidget.setPressed(true);

// 绑定点击回调（接收 clickData 和 state 两个参数）
switchWidget.setOnPressCallback((clickData, state) -> {
    // state 为切换后的布尔值
});

// 绑定外部状态自动同步
switchWidget.setSupplier(() -> someExternalBooleanState);
```

---

## 16. 下拉选择 - SelectorWidget

下拉列表选择组件。

### 属性

| 属性 | 说明 |
|------|------|
| `currentValue` | 当前选中值 |

### API

```java
// 设置候选项
selectorWidget.setCandidates(List.of("OptionA", "OptionB", "OptionC"));

// 设置当前值（值不在候选列表中则不生效）
selectorWidget.setValue("OptionA");

// 设置滚动条出现前的最大可见数
selectorWidget.setMaxCount(3);

// 设置字体颜色
selectorWidget.setFontColor(0xFFFFFF);

// 设置按钮背景纹理
selectorWidget.setButtonBackground(myTexture);

// 选择变化回调
selectorWidget.setOnChanged(selected -> {
    System.out.println("Selected: " + selected);
});

// 动态候选项
selectorWidget.setCandidatesSupplier(() -> fetchDynamicOptions());

// 控制下拉显示/隐藏
selectorWidget.setShow(true);
```

---

## 17. 文本输入 - TextFieldWidget

可编辑的文本输入框。

### 属性

| 属性 | 说明 |
|------|------|
| `currentString` | 当前文本 |
| `maxStringLength` | 最大字符数 |
| `isBordered` | 是否显示边框 |
| `textColor` | 文本颜色 |
| `wheelDur` | 鼠标滚轮调整步长 |

### API

```java
// 动态文本源
textField.setTextSupplier(() -> "dynamic value");

// 文本变化回调
textField.setTextResponder(newText -> {
    System.out.println("Text changed: " + newText);
});

// 边框开关
textField.setBordered(true);

// 文本颜色
textField.setTextColor(0xFFFFFF);

// 最大长度
textField.setMaxStringLength(100);

// 自定义验证器
textField.setValidator(input -> sanitizedOutput);

// 输入限制
textField.setCompoundTagOnly();          // 仅允许 NBT 格式
textField.setResourceLocationOnly();     // 仅允许 ResourceLocation 格式
textField.setNumbersOnly(min, max);      // 仅允许数字（支持 int 和 float）

// 鼠标滚轮步长（数值输入时可滚轮增减）
textField.setWheelDur(1);
```

---

## 18. 进度条 - ProgressWidget

可视化进度条组件。

### 属性

| 属性 | 说明 |
|------|------|
| `lastProgressValue` | 最近的进度值 |

### API

```java
// 设置进度源（返回 0.0 ~ 1.0）
progressWidget.setProgressSupplier(() -> 0.3);

// 动态悬停提示
progressWidget.setDynamicHoverTips(progress ->
    String.format("Current progress: %.0f%%", progress * 100));
```

---

## 19. GUI 纹理系统

所有纹理继承 `TransformTexture`，共享以下通用方法：

```java
texture.rotate(45);         // 旋转角度
texture.scale(1.5);         // 缩放
texture.transform(10, 20);  // 偏移
var copy = texture.copy();  // 复制
```

### 19.1 ResourceTexture — 资源纹理

从资源路径加载纹理。

| 属性 | 说明 |
|------|------|
| `imageLocation` | 纹理资源路径 |
| `offsetX / offsetY` | 偏移（默认 0） |
| `imageWidth / imageHeight` | 缩放因子（默认 1） |
| `color` | 颜色覆盖（默认 -1） |

```java
// 创建纹理
var tex = ResourceTexture.createTexture("ldlib:textures/gui/icon.png");

// 获取子区域（归一化坐标 0.0~1.0）
var sub = tex.getSubTexture(0.2, 0.2, 0.5, 0.5);
```

### 19.2 ResourceBorderTexture — 九宫格边框纹理

继承 `ResourceTexture`，支持可配置的边框角/边/中心区域渲染。

| 属性 | 说明 |
|------|------|
| `boderSize` | 边框角尺寸 |
| `imageSize` | 整体纹理尺寸 |

```java
texture.setBoderSize(5, 5);
texture.setImageSize(200, 150);
```

**内置常量**：`ResourceBorderTexture.BORDERED_BACKGROUND`、`ResourceBorderTexture.BUTTON_COMMON`

### 19.3 ColorRectTexture — 纯色矩形

| 属性 | 说明 |
|------|------|
| `color` | 颜色 |
| `radiusLT/LB/RT/RB` | 四角圆角半径 |

```java
var rect = new ColorRectTexture(0xFF333333);
rect.setRadius(10);          // 统一圆角
rect.setLeftRadius(8);       // 左侧圆角
rect.setRightRadius(8);      // 右侧圆角
rect.setTopRadius(8);        // 上侧圆角
rect.setBottomRadius(8);     // 下侧圆角
```

### 19.4 ColorBorderTexture — 彩色边框

| 属性 | 说明 |
|------|------|
| `color` | 边框颜色 |
| `border` | 边框宽度 |

```java
var border = new ColorBorderTexture(2, 0xFFFFFFFF);
border.setBorder(3);
border.setColor(0xFF00FF00);
border.setRadius(5);          // 统一圆角
border.setLeftRadius(5);
border.setRightRadius(5);
border.setTopRadius(5);
border.setBottomRadius(5);
```

### 19.5 TextTexture — 文本纹理

将文本渲染为纹理，支持动态更新和多种显示模式。

**显示模式**（TextType）：
- 居中换行（默认）
- 隐藏溢出（居中）
- 滚动（悬停/始终）
- 左对齐 / 右对齐 变体

```java
var text = new TextTexture("Hello");
text.setSupplier(() -> "Dynamic");    // 动态文本
text.updateText("New text");          // 手动更新
text.setBackgroundColor(0xFFFF0000);  // 背景色
text.setDropShadow(true);            // 阴影
text.setWidth(100);                   // 最大宽度
text.setType(TextType.ROLL);         // 显示模式
```

### 19.6 AnimationTexture — 动画纹理

从精灵图表（Sprite Sheet）播放帧动画。

| 属性 | 说明 |
|------|------|
| `imageLocation` | 精灵图表资源路径 |
| `cellSize` | 网格划分数（边长） |
| `from / to` | 动画帧范围 |
| `animation` | 播放速度（tick/帧） |
| `color` | 颜色叠加 |

```java
var anim = new AnimationTexture();
anim.setTexture("ldlib:textures/gui/particles.png");
anim.setCellSize(8);
anim.setAnimation(32, 44);   // 帧范围
anim.setAnimation(1);        // 速度
anim.setColor(0xFF000000);
```

### 19.7 GuiTextureGroup — 纹理组

将多个纹理叠加显示。

```java
var group = new GuiTextureGroup(texture1, texture2);
group.setTextures(tex1, tex2, tex3);
group.setColor(0xFF00FF);   // 统一颜色
```

### 19.8 ItemStackTexture — 物品纹理

循环显示物品。

```java
var itemTex = new ItemStackTexture(item1, item2);
itemTex.setItems(item1, item2, item3);
itemTex.setColor(0xFFFF00FF);
```

### 19.9 ProgressTexture — 进度纹理

按方向填充的进度条纹理。

| 属性 | 说明 |
|------|------|
| `fillDirection` | 填充方向 |
| `emptyBarArea` | 空白区域纹理 |
| `filledBarArea` | 填充区域纹理 |
| `progress` | 当前进度（0.0 ~ 1.0） |

```java
var progressTex = new ProgressTexture(emptyTexture, filledTexture);
progressTex.setProgress(0.75);
progressTex.setFillDirection(FillDirection.RIGHT_TO_LEFT);
```

### 19.10 ShaderTexture — 着色器纹理

使用自定义 Shader 渲染纹理。

| 属性 | 说明 |
|------|------|
| `location` | Shader 资源路径 |
| `resolution` | 分辨率因子（影响 iResolution uniform） |
| `color` | 颜色叠加 |

```java
var shaderTex = new ShaderTexture(new ResourceLocation("modid:shaders/my_shader"));
shaderTex.setResolution(2.0);

// 设置 Uniform 参数
shaderTex.setUniformCache(cache -> {
    // 配置额外 uniform
});

// 更新 Shader
shaderTex.updateShader(newResourceLocation);

// 使用原始 Shader 代码
shaderTex.updateRawShader(rawShaderCode);

// ⚠️ 使用 Raw Shader 后必须释放资源！
shaderTex.dispose();
```

---

## 20. SyncData 数据同步系统

简化 BlockEntity 的数据同步与持久化，通过注解驱动。

### 20.1 基本设置

```java
public class MyBlockEntity extends BlockEntity
        implements IAsyncAutoSyncBlockEntity, IAutoPersistBlockEntity, IManaged {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER =
            new ManagedFieldHolder(MyBlockEntity.class);

    private final FieldManagedStorage syncStorage =
            new FieldManagedStorage(this);

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public IManagedStorage getSyncStorage() {
        return syncStorage;
    }

    @Override
    public void onChanged() {
        setChanged();
    }

    @Override
    public IManagedStorage getRootStorage() {
        return getSyncStorage();
    }
}
```

### 20.2 字段注解

- `@Persisted` — 标记需要同步 + 持久化的字段
- `@DropSaved` — 标记需要保存到 ItemStack 的字段

### 20.3 持久化到 ItemStack

```java
// 保存（例如掉落时）
if (getBlockEntity(level, pos) instanceof IAutoPersistBlockEntity dropSave) {
    dropSave.saveManagedPersistentData(itemStack.getOrCreateTag(), true);
}

// 加载（例如放置时）
@Override
public void setPlacedBy(Level level, BlockPos pos, BlockState state,
        @Nullable LivingEntity player, ItemStack stack) {
    if (!level.isClientSide) {
        if (getBlockEntity(level, pos) instanceof IAutoPersistBlockEntity dropSave) {
            CompoundTag tag = stack.getTag();
            if (tag != null) {
                dropSave.loadManagedPersistentData(tag);
            }
        }
    }
}
```

### 20.4 监听字段变化

```java
public class MyBlockEntity extends BlockEntity
        implements IAsyncAutoSyncBlockEntity, IAutoPersistBlockEntity, IManaged {

    @Persisted
    boolean shouldRenderOverlay;

    public MyBlockEntity(...) {
        if (LDLib.isRemote()) {
            addSyncUpdateListener("shouldRenderOverlay", this::fieldUpdated);
        }
    }

    protected void fieldUpdated(String fieldName, Object newValue, Object oldValue) {
        scheduleRenderUpdate();
    }
}
```

### 20.5 字段初始化注意事项

数据加载发生在区块加载期间（可能不安全），若需访问 `@Persisted` 字段值，应延迟到下一 tick：

```java
@Override
public void onLoad() {
    if (!LDLib.isRemote()) {
        getLevel().getServer().tell(new TickTask(0, this::initialize));
    }
}

public void initialize() {
    // 安全地访问 @Persisted 字段
}
```

---

## 21. Compass 文档系统

完全文件驱动的游戏内文档系统，融合 Ponder 和任务手册特点，无需 Java 代码。

### 文件结构

```
/assets/<mod_id>/compass/
├── sections/        # 分类定义（JSON）
├── nodes/           # 节点定义（JSON）
└── pages/           # 页面内容（XML，按语言分文件夹）
    ├── en_us/
    └── zh_cn/
```

### Section（分类）

```json
{
    "button_texture": { "type": "item", "value": "minecraft:compass" },
    "priority": 0,
    "background_texture": "modid:textures/gui/bg.png"
}
```

`section_id` 由文件路径自动确定，格式：`mod_id:文件名`

### Node（节点）

```json
{
    "section": "mod_id:section_name",
    "size": 24,
    "position": [100, 200],
    "pre_nodes": ["mod_id:parent_node"],
    "page": "mod_id:page_name",
    "items": ["minecraft:diamond"]
}
```

- `pre_nodes`：父节点列表（用于显示关联关系）
- `items`：长按 [C] 键可快速打开 Compass 的物品

### Page（页面）

使用 XML 格式，放置在对应语言文件夹下，系统自动识别语言。

---

## 附录：核心接口速查

| 接口 | 用途 |
|------|------|
| `IUIHolder` | BlockEntity 实现以支持 UI |
| `IUIHolder.Item` | Item 实现以支持 UI |
| `IAsyncAutoSyncBlockEntity` | 异步自动数据同步 |
| `IAutoPersistBlockEntity` | 自动 NBT 持久化 |
| `IManaged` | SyncData 管理接口 |

| 工厂类 | 用途 |
|--------|------|
| `BlockEntityUIFactory.INSTANCE` | 打开 BlockEntity UI |
| `HeldItemUIFactory.INSTANCE` | 打开手持物品 UI |
| `ModularUI` | UI 容器封装类 |
| `UIProject.loadUIFromFile()` | 从文件加载 UI |

---

*文档整理自 LDLib 官方教程，版本截至 2026-03-26*