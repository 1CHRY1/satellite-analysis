# 工具发布（景级/Cube级）多阶段实施计划（含可验证验收标准）

目标：完善“工具发布”功能，使景级分析工具可以成功发布到动态分析页面并被使用；以可扩展的方式为未来 Cube 级工具保留位置与流程。目前仅完成景级工具的端到端打通，Cube 级仅放置占位与接口设计。

---

## 阶段一：数据模型与后端 API 对齐（level 字段）

变更点
- 在项目创建/查询 API 中贯通 `project_table.level`（scene/cube）。
- 创建工具时将 `level` 持久化；查询项目时返回 `level`。

涉及文件（后端，示例）
- backEnd/.../controller/ProjectController.java（创建、查询接口）
- backEnd/.../service/ProjectService.java（业务层）
- backEnd/.../dao/ProjectMapper.xml（SQL 映射，新增/回填 level 字段）

验收标准
- 通过接口调用创建项目时传入 `level=scene`，数据库中可见对应记录 level=scene。
- GET 项目列表时，返回 JSON 中每个项目含 `level` 字段，值与库一致。
- 兼容历史数据：旧项目未填充时后端默认回传 `level="scene"`（不影响现网）。

---

## 阶段二：前端工具发布入口（tools.vue）增加“工具级别”选项

变更点
- 在 `frontEnd/src/views/tools.vue` 的“创建工具”弹层中，新增单选：
  - 工具级别：景级（scene，默认）/ Cube 级（cube）。
- 创建时携带 `level` 字段调用 `createProject`。
- UI 仅提示 Cube 级“即将支持”，但允许被选择（占位）。

涉及文件（前端）
- frontEnd/src/views/tools.vue:1（新增单选 UI、`newProject.level` 默认 scene、`create()` 透传 level）
- frontEnd/src/api/http/analysis.ts（如需调整 createProject DTO 类型）

验收标准
- 打开“创建工具”弹窗，能看到“工具级别”单选，默认选中“景级”。
- 点击创建后，网络请求体包含 `level: "scene"`；后端成功返回并在项目列表中显示。
- 选择“Cube 级”创建成功，但后续仅作为占位，进入编辑页不报错（模板为空或占位说明）。

---

## 阶段三：编辑器默认模板（按 level 加载，保证 100% 可运行）

变更点
- `frontEnd/src/components/analysisComponents/codeEditor.vue` 获取当前项目 `level`：
  - 若 `isToolProject === true` 且代码为空（或仅错误占位），则：
    - level=scene：自动填充“景级 UDF”参考模板（Flask + CORS，直接返回 tileTemplate，零额外依赖）。
    - level=cube：暂时填充占位注释“TODO: Cube 级模板即将支持”。
- 在“模板”下拉中保留“表达式（无需服务）/Flask（HTTP 瓦片）”，并新增“景级 UDF（推荐）”快捷填充到编辑器。

参考模板（景级 UDF，确保可运行）
```
from flask import Flask, jsonify, request
from flask_cors import CORS
from urllib.parse import quote_plus
import json

app = Flask(__name__)
CORS(app, resources={r"/*": {"origins": "*"}})

@app.route("/run", methods=["POST"])
def run():
    body = request.get_json(force=True, silent=True) or {}
    mosaic_url = body.get("mosaicUrl")
    params = body.get("params") or {}

    if isinstance(params, str):
        try:
            params = json.loads(params)
        except json.JSONDecodeError:
            params = {}

    if not isinstance(params, dict):
        params = {}
            
    
    if not mosaic_url:
        return jsonify({"error": "mosaicUrl is required"}), 400

    expression = params.get("expression") or "(b3-b5)/(b3+b5)"
    color = params.get("color") or "rdylgn"
    pixel_method = params.get("pixel_method") or "first"

    tile_template = ("/tiler/mosaic/analysis/{z}/{x}/{y}.png?"f"mosaic_url={quote_plus(mosaic_url)}"f"&expression={quote_plus(expression)}"f"&pixel_method={quote_plus(pixel_method)}"f"&color={quote_plus(color)}")
    return jsonify({"tileTemplate": tile_template})

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=20080, debug=False)
```

涉及文件（前端）
- frontEnd/src/components/analysisComponents/codeEditor.vue:1（按 level 自动填充；模板菜单新增项）
- frontEnd/src/api/http/analysis.ts（若需要补充获取当前项目 level 的接口使用）

验收标准
- 新建“景级工具”后首次进入编辑器，代码自动填充为上方模板；点击保存不报错。
- 选择“模板/景级 UDF（推荐）”也能随时一键填充相同模板（若编辑器有代码，会提示覆盖确认）。
- 选择“Cube 级”创建后进入编辑器，展示占位注释，模板菜单中 Cube 级项为灰态或弹出“即将支持”。

---

## 阶段四：发布向导扩展与动态分析接入（仅景级）

变更点
- 动态工具元信息增加 `level` 字段（保持向后兼容：无 level 视为 scene）：
  - frontEnd/src/store/toolRegistry.ts:1 新增 `level: 'scene' | 'cube'`。
  - 注册工具时将当前项目的 `level` 一并写入。
- 发布向导默认参数/schema：
  - invokeType 默认 `http+tile`；参数包含 `expression`、`color`、`pixel_method`，默认值与模板一致。
  - 发布后若检测到后端返回了运行中 URL，自动把 `serviceEndpoint` 补全为 `${url}/run`。
- 动态分析页面仅展示景级工具并可调用：
  - frontEnd/src/components/dataCenter/dynamicAnalysis/dynamicAnalysis.vue:1 对 `publishedTools` 做 `tool.level==='scene'` 过滤。
  - 动态调用组件 `DynamicServicePanel.vue` 继续使用 `mosaicUrl`（来自“前序数据”），按 `invoke.type` 分发。

验收标准
- 在“发布为工具”对话框中发布景级工具后，动态分析页面“发布的工具”出现该工具。
- 选择“前序数据”后，点击该工具 → 弹出参数面板，保留默认参数即可运行，地图叠加瓦片图层成功（无报错）。
- 对无 `level` 的历史工具，仍可显示与调用（默认按 scene 处理）。

---

## 阶段五：端到端流程自测（手工验证步骤）

场景 A：发布景级工具并使用
1. 进入 `tools` 页面，点击“创建工具”，选择“景级”，完成必填项后创建。
2. 进入在线编程页，确认编辑器自动填充“景级 UDF 模板”，保存代码。
3. 启动/发布服务（后端返回运行 URL），发布为工具时向导自动补齐 `serviceEndpoint=/run`。
4. 进入“动态分析”页面 → 打开“前序数据” → 选择任一完成的数据集。
5. 在“发布的工具”列表中点击新工具 → 默认参数直接运行 → 地图出现叠加瓦片层。

通过标准
- 第 2 步编辑器内代码与“参考模板”一致。
- 第 3 步能看到运行 URL，发布成功提示。
- 第 5 步地图出现彩色渲染层；控制台无错误日志；可重复运行不同参数生效。

---

## 阶段六：为 Cube 级发布预留扩展（本期占位）

设计约束与占位实现
- `project_table.level=cube` 的项目完整创建与进入编辑器流程可用，但模板为占位。
- ToolRegistry 的 `level` 字段一并保留；动态分析页面默认过滤掉 `level=cube` 的工具（另两个页面将接入）。
- 未来 Cube 级工具参数面板将以“CacheKey/立方体摘要”为输入源，倾向于 `http+mosaic` 或 `http+geojson` 的返回类型。

验收标准（本期）
- 能创建 `level=cube` 的工具并进入编辑器；发布向导可打开但提示“即将支持”，不允许发布。
- 动态分析页面不展示 `level=cube` 的工具（避免误用）。

---

## 非功能与回归检查

- 兼容性：老项目/老工具不含 `level` 时，前端默认按 `scene` 处理，不影响使用。
- 安全：发布模板统一开启 CORS，方法仅暴露 `/run`；不引入额外依赖包，降低失败概率。
- 观测：关键路径加日志提示（创建、自动填充、发布成功/失败、运行结果加载）。

---

## 风险与缓解

- 依赖后端返回 `level`：若后端暂未改造，前端默认 scene 并允许手动选择模板；待后端上线后自动切换。
- 运行容器环境差异：模板无第三方依赖，但如需端口占用策略，发布接口需支持指定/自动分配端口并回传 URL（现有接口已满足）。
- 历史数据一致性：建议一次性数据修复脚本将历史工具项目 `level` 置为 `scene`，避免前端判空逻辑波动。

---

## 变更清单（代码落点索引）

- frontEnd/src/views/tools.vue:1 新增“工具级别”单选，创建时透传 `level`。
- frontEnd/src/components/analysisComponents/codeEditor.vue:1
  - 进入时按 `level` 自动填充模板；模板菜单新增“景级 UDF（推荐）”。
  - 发布向导注册工具时写入 `level`。
- frontEnd/src/store/toolRegistry.ts:1 `DynamicToolMeta` 扩展 `level` 字段；注册/读取保持兼容。
- frontEnd/src/components/dataCenter/dynamicAnalysis/dynamicAnalysis.vue:1 “发布的工具”列表过滤 `level==='scene'`。
- frontEnd/src/components/dataCenter/thematic/DynamicServicePanel.vue:1 保持现有 `mosaicUrl` 解析与运行流程。
- backEnd/...（项目创建/查询）贯通 `level` 字段；默认值 scene；老数据回填策略。

---

## 完成定义（Definition of Done）

- 景级工具：从创建 → 自动填充模板 → 发布 → 动态分析调用 全流程跑通，具备回归用例。
- Cube 级：创建可选占位；动态分析不展示；发布入口提示“即将支持”。
- 文档：在 README 或内部 Wiki 同步“如何创建景级 UDF 工具”的简明指引（含模板与示例参数）。

