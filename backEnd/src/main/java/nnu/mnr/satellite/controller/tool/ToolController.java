package nnu.mnr.satellite.controller.tool;

import nnu.mnr.satellite.model.dto.tool.*;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.model.vo.tool.Code2ToolVO;
import nnu.mnr.satellite.model.vo.tool.ToolInfoVO;
import nnu.mnr.satellite.service.tool.ToolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tools")
public class ToolController {

    @Autowired
    ToolService toolService;


    // 创建工具
    @PostMapping("/publish")
    public ResponseEntity<CommonResultVO> publishCodeTool(@RequestBody Code2ToolDTO code2ToolDTO) {
        CommonResultVO result = toolService.publishTool(code2ToolDTO);
        return ResponseEntity.ok(result);
    }
    // 修改工具
    @PostMapping("/update")
    public ResponseEntity<CommonResultVO> updateToolById(@RequestBody ToolInfoDTO toolInfoDTO) {
        CommonResultVO result = toolService.updateToolById(toolInfoDTO);
        return ResponseEntity.ok(result);
    }

    // 删除工具
    @DeleteMapping("/delete/{toolId}")
    public ResponseEntity<CommonResultVO> deleteTool(@PathVariable String toolId) {
        CommonResultVO result = toolService.deleteToolById(toolId);
        return ResponseEntity.ok(result);
    }

    // 根据ID获取工具详情
    @GetMapping("/{toolId}")
    public ResponseEntity<ToolInfoVO> getToolById(@PathVariable String toolId) {
        ToolInfoVO toolInfoVO = toolService.getToolById(toolId);
        return ResponseEntity.ok(toolInfoVO);
    }

    // 根据ID获取工具详情
    @PostMapping("/all")
    public ResponseEntity<CommonResultVO> getToolPage(@RequestBody ToolPageDTO toolPageDTO) {
        CommonResultVO resultVO = toolService.getToolPage(toolPageDTO);
        return ResponseEntity.ok(resultVO);
    }

    // 3. 执行工具（如运行打包后的代码）
    @PostMapping("/execute")
    public ResponseEntity<CommonResultVO> executeTool(
            @RequestBody ExecutionInputDTO executionInputDTO) {
        CommonResultVO result = toolService.executeTool(executionInputDTO);
        return ResponseEntity.ok(result);
    }
}
