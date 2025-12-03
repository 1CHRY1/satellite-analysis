# Cube级分析工具发布功能实现计划

目标：完成“Cube级分析工具”的发布与使用流程，使其与“景级分析工具”并行工作，创建时使用 codeEditor 模板，发布后在动态分析页按级别分区展示与调用。

---

## Phase 0 · 现状梳理与对齐
- 关键位置
  - `frontEnd/src/views/tools.vue:80` 起：创建工具表单（含 `level` 单选）。
  - `frontEnd/src/views/Analysis.vue:1`：在线编程页，承载 `codeEditor`。
  - `frontEnd/src/components/analysisComponents/codeEditor.vue:1530` 起：模板引导与“发布为工具”向导；已有 `scene_udf` 与 `cube_placeholder`。
  - `frontEnd/src/components/dataCenter/dynamicAnalysis/dynamicAnalysis.vue:240` 起：动态分析页，当前“发布的工具”仅按 scene 过滤并在两处使用同一列表。
  - `frontEnd/src/components/dataCenter/dynamicAnalysis/composables/useTool.ts:1`：仅生成“景级”动态工具目录与选择逻辑。
  - `frontEnd/src/components/dataCenter/thematic/DynamicServicePanel.vue:1`：景级动态工具运行面板（基于 mosaicUrl）。
  - `frontEnd/src/components/dataCenter/dynamicAnalysis/composables/useCube.ts:1`：Cube 列表/选择/渲染。
  - 后端创建模板：`backEnd/src/main/java/nnu/mnr/satellite/service/modeling/ModelCodingService.java:160` 起（仅背景信息，最终以前端模板为准）。

- 已知差距
  1) Cube 级仅占位模板；发布函数对 cube 显示“即将上线”。
  2) 动态分析页两处“发布的工具”展示相同（未分级）。
  3) 工具创建后强制套用的 bootstrap 仅有 `scene_udf` 入口，Cube 走占位逻辑。

- 验收标准
  - 能准确定位以上文件；本计划各 Phase 修改点均可在上述文件完成或新增文件实现。

---

## Phase 1 · 创建流改造：Cube 使用 codeEditor 模板
- 内容
  - tools.vue 创建后根据 `level` 传递不同的模板引导 Query：`?bootstrap=cube_udf`（新增标识）。
  - codeEditor 增加完整的 `cube_udf` 模板方法（替换 `cube_placeholder`），含：
    - 入口 `main()` 与 `Flask /run` 示例；
    - 请求体读取 Cube JSON（示例结构见需求补充）；
    - 将场景列表聚合/遍历的示例逻辑（可简化为返回固定 `tileTemplate` 的 Demo）；
    - 发布向导默认值（名称/分类/标签，调用方式优先 `http+tile`，参数可含表达式或其它示例参数）。
  - 兼容：仍保留菜单“模板→Cube级占位”项，但新增“Cube级 UDF（推荐）”。

- 验收标准
  - 步骤：在 Tools 页创建工具，选择“Cube 级”。
  - 打开在线编程后，编辑器首屏非空、非 `main_nontool.py`/占位注释，包含“Cube 级 UDF 模板”字样与 Flask `/run` 端点示例。
  - 打开“发布为工具”向导时，已预置名称/描述/调用方式/参数，无“即将上线”提示。

---

## Phase 2 · 发布注册：保留级别并持久化
- 内容
  - 取消 `codeEditor.vue` 中对 Cube 发布的拦截提示，允许与景级一致的注册流程。
  - `toolRegistry` 已支持 `level` 字段与 normalize；保证注册时写入 `level: 'cube'`。

- 验收标准
  - 发布一个景级与一个 Cube 级工具后，`localStorage["dynamic_tools_registry:<userId>"]` 中各自条目 `level` 分别为 `scene`、`cube`。

---

## Phase 3 · 动态分析页：分区展示
- 内容
  - dynamicAnalysis.vue：
    - 新增 `publishedSceneTools`、`publishedCubeTools` 两个 computed；
    - “景级分析→发布的工具”使用 `publishedSceneTools`；
    - “Cube 级分析→发布的工具”使用 `publishedCubeTools`；
    - 计数分别展示各自数量；点击“调用”时区分调用入口。

- 验收标准
  - 仅发布景级工具时：景级分区出现工具卡片，Cube 分区显示“暂无发布的工具”。
  - 同时发布景级与 Cube 工具时：两个分区分别仅显示对应级别工具，计数与卡片数量一致。

---

## Phase 4 · 运行面板：新增 DynamicCubeServicePanel
- 内容
  - 新组件：`frontEnd/src/components/dataCenter/thematic/DynamicCubeServicePanel.vue`
    - 表单渲染完全复用 `DynamicServicePanel` 的参数解析与校验逻辑；
    - 上下文从 `useCube()` 获取：
      - 若无选中/合成的 Cube，显示“请先在 Cube 面板选择/合成立方体”；
      - 将所选 Cube 的 JSON（或缓存 key 反查 JSON）作为 `{{cube}}`、`{{params}}` 注入；
      - 支持最小可用 `invoke.type`: `http+tile` 与 `http+geojson`（先不强制支持表达式拼接）。
    - 运行逻辑：
      - `http+tile`：服务返回 `tileTemplate`（或通过 `responsePath` 取出），叠加栅格。
      - `http+geojson`：返回 GeoJSON 叠加矢量。
  - dynamicAnalysis.vue：当点击 Cube 区“调用”时，右侧面板切换为 `DynamicCubeServicePanel` 并注入所点工具。

- 验收标准
  - 未选择/合成 Cube：面板“开始运行”按钮禁用或点击提示缺少 Cube 上下文。
  - 选择一个 Cube 后：能点击“开始运行”；若使用示例服务（返回固定 tileTemplate），地图成功叠加图层，无控制台错误。

---

## Phase 5 · codeEditor 发布向导：Cube 默认表单与参数
- 内容
  - 当 `projectLevel==='cube'`：
    - 默认 `invokeType = 'http+tile'`；
    - 默认参数示例：可选 `expression`/`color`/`pixel_method` 或 `cube_process_mode`；
    - `payloadTemplate` 默认形如：`{ cube: "{{cube}}", params: "{{params}}" }`；
    - 保存草稿逻辑与景级一致。

- 验收标准
  - 打开发布向导时：已出现上述默认配置；发布后 `toolMeta.invoke` 正确落库（localStorage）。

---

## Phase 6 · 端到端联调与手测用例
- 用例 1：场景工具回归
  - 创建“景级分析工具”→ 自动套用景级 UDF 模板 → 发布 → 动态分析“景级分析→发布的工具”中可见且可调用 → 地图叠加成功。
- 用例 2：Cube 工具全链路
  - 创建“Cube 级分析工具”→ 自动套用 Cube UDF 模板 → 发布。
  - 交互式探索页完成“获取格网→数据检索→合成立方体→确定”。
  - 动态分析“Cube 级分析→时序立方体”中出现合成项，勾选并点击 Square 按钮加载到地图。
  - “Cube 级分析→发布的工具”中选择刚发布的工具，点击“调用”→ 面板切换为 Cube 运行面板 → “开始运行”后地图叠加成功。

- 验收标准
  - 两条用例均通过；控制台无未处理异常；页面无“即将支持”提示。

---

## Phase 7 · 兼容性与回滚
- 兼容
  - `toolRegistry` 已对缺失 `level` 的历史数据做 normalize，旧数据默认归为 `scene`。
- 新增的 Cube 面板与场景面板互不影响，按所选工具级别动态切换。
- 回滚
  - 若 Cube 模板或面板出现故障，可临时：
    - 在 `codeEditor` 层恢复 `cube_placeholder` 模板入口；
    - 在动态分析页仅展示 `publishedSceneTools`（隐藏 Cube 区发布的工具）。

---

## 开发清单（按文件）
- 前端
  - 修改：`frontEnd/src/views/tools.vue`（创建后路由 Query：scene→`bootstrap=scene_udf`；cube→`bootstrap=cube_udf`）。
  - 修改：`frontEnd/src/components/analysisComponents/codeEditor.vue`（新增 `cube_udf` 模板、默认参数与发布逻辑；移除 cube 发布拦截）。
  - 修改：`frontEnd/src/components/dataCenter/dynamicAnalysis/dynamicAnalysis.vue`（分离 `publishedSceneTools` 与 `publishedCubeTools`，区分“调用”入口）。
  - 新增：`frontEnd/src/components/dataCenter/thematic/DynamicCubeServicePanel.vue`（Cube 工具运行面板）。
  - 修改：`frontEnd/src/components/dataCenter/dynamicAnalysis/composables/useTool.ts`（如需：对选中状态与动态面板切换做最小改造，或新增 `useCubeTool`）。
- 后端（可选，保守）
  - 仅保留当前按 `isTool + level` 选择默认 `main.py` 的逻辑作为兜底；真正生效以前端模板覆盖为准，无需强依赖后端模板变更。

---

## 里程碑与交付
- M1（Phase 1–3）：
  - 可创建/发布 Cube 工具；动态分析页分区展示，点击“调用”可打开面板。
- M2（Phase 4–5）：
  - 新面板支持最小可用 HTTP 工具（tile/geojson），具备参数与上下文（cube）注入。
- M3（Phase 6）：
  - 全链路 E2E 验收通过，交付使用说明与演示录屏。

---

## 风险与对策
- 服务契约不统一（Cube 工具返回何种结构）：
  - 先支持最小集合（http+tile / http+geojson），通过 `responsePath` 做抽取兼容；表达式/拼接等高级能力后续迭代。
- Cube 面板所需的 cube JSON 结构：
  - 以补充信息示例为准，新增最小上下文 `{{cube}}` 直接透传到服务端，避免前端拼装错误。

---

## 验收清单（最终）
- 创建 Cube 工具时首屏即为“Cube 级 UDF 模板”，非占位、非 main_nontool。
- 发布后 localStorage 中工具元数据 `level==='cube'`。
- 动态分析页两处“发布的工具”分区正确、计数正确。
- 勾选并加载一个 Cube 后，调用 Cube 工具能叠加图层或加载矢量，无报错。

