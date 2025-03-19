package nnu.mnr.satellite.controller.test;

import nnu.mnr.satellite.model.po.common.DFileInfo;
import nnu.mnr.satellite.service.common.DockerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/16 10:23
 * @Description:
 */

@RestController
@RequestMapping("api/v1/test")
public class TestController {

    @Autowired
    DockerService dockerService;

    // dockerService测试接口
    @GetMapping("/docker/env")
    public ResponseEntity<String> testStartEnv() {
        dockerService.initEnv("tetete");
        return ResponseEntity.ok("Docker Env started");
    }

    @GetMapping("/docker/container")
    public ResponseEntity<String> testCreateContainer() {
        String containerId = dockerService.createContainer("318d67b5a508", "satelliteTest", "/home/vge/satellite/tetete", "/usr/local/coding");
        return ResponseEntity.ok(containerId);
    }

    @GetMapping("/docker/start/container/{containerId}")
    public ResponseEntity<String> testStartContainer(@PathVariable String containerId) throws InterruptedException {
        dockerService.startContainer(containerId);
        return ResponseEntity.ok("started");
    }

    @GetMapping("/docker/stop/container/{containerId}")
    public ResponseEntity<String> testStopContainer(@PathVariable String containerId) throws InterruptedException {
        dockerService.stopContainer(containerId);
        return ResponseEntity.ok("stopped");
    }

    @GetMapping("/docker/remove/container/{containerId}")
    public ResponseEntity<String> testRemoveContainer(@PathVariable String containerId) throws InterruptedException {
        dockerService.removeContainer(containerId);
        return ResponseEntity.ok("removed");
    }

    @GetMapping("/docker/container/status/{containerId}")
    public ResponseEntity<Boolean> testContainerStatus(@PathVariable String containerId) throws Exception {
        return ResponseEntity.ok(dockerService.checkContainerState(containerId));
    }

    @GetMapping("/docker/container/files/{containerId}")
    public ResponseEntity<List<DFileInfo>> testContainerFiles(@PathVariable String containerId) {
        List<DFileInfo> fileTree = new ArrayList<>();
        return ResponseEntity.ok(dockerService.getCurDirFiles(containerId, "tetete", "", fileTree));
    }

    @GetMapping("/docker/container/run/{containerId}")
    public ResponseEntity<String> testContainerRun(@PathVariable String containerId) {
        String common = "python main.py";
        dockerService.runCMDInContainer("tetete", containerId, common);
        return ResponseEntity.ok("run");
    }

}
