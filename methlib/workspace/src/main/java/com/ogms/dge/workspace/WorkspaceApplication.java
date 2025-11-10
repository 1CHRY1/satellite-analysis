package com.ogms.dge.workspace;

import com.ogms.dge.workspace.modules.workflow.service.TaskSchedulerService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import springfox.documentation.oas.annotations.EnableOpenApi;

@EnableOpenApi
@SpringBootApplication
public class WorkspaceApplication {

    public static void main(String[] args) {

        SpringApplication.run(WorkspaceApplication.class, args);
    }

}
