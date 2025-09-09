package nnu.mnr.satellite.controller.minIO;

import jakarta.servlet.http.HttpServletResponse;
import nnu.mnr.satellite.model.dto.minIO.FileEditDTO;
import nnu.mnr.satellite.model.dto.minIO.FileUploadDTO;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.service.minIO.MinIOService;
import nnu.mnr.satellite.service.user.UserServiceV3;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/minIO")
public class MinIOController {

    private final MinIOService minIOService;

    public MinIOController(MinIOService minIOService) {
        this.minIOService = minIOService;
    }

    @GetMapping("/files/tree/{userId}")
    public ResponseEntity<CommonResultVO> getFileTree(@PathVariable("userId") String userId) throws Exception {
        return ResponseEntity.ok(minIOService.getUserMinIODirectoryTree(userId));
    }

    @PostMapping("/file")
    public ResponseEntity<CommonResultVO> getSingleFile(@RequestParam("filePath") String filePath) throws Exception {
        return ResponseEntity.ok(minIOService.getUserFileInfo(filePath));
    }

    @PostMapping("/file/upload")
    public ResponseEntity<CommonResultVO> uploadFile(@ModelAttribute FileUploadDTO fileUploadDTO) throws Exception {
        return ResponseEntity.ok(minIOService.uploadFile(fileUploadDTO));
    }

    @GetMapping("/file/download")
    public void downloadFile(@RequestParam("filePath") String fullPath,
                                                       HttpServletResponse response) throws Exception {
        minIOService.downloadFile(fullPath, response);
    }

    @DeleteMapping("/file/delete")
    public ResponseEntity<CommonResultVO> deleteFile(@RequestParam("filePath") String fullPath) throws Exception {
        return ResponseEntity.ok(minIOService.deleteFile(fullPath));
    }

    @PostMapping("/file/edit")
    public ResponseEntity<CommonResultVO> editFile(@RequestBody FileEditDTO fileEditDTO) throws Exception {
        return ResponseEntity.ok(minIOService.editFile(fileEditDTO));
    }
}
