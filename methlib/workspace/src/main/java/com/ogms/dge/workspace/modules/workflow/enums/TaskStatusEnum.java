package com.ogms.dge.workspace.modules.workflow.enums;

public enum TaskStatusEnum {
    PENDING(0, "待执行"), // 待执行
    IN_PROGRESS(1, "执行中"), // 执行中
    COMPLETED(2, "执行成功"), // 执行成功
    FAILED(3, "执行失败"), // 执行失败
    CANCELLED(4, "被取消"), // 被取消
    PAUSED(5, "被暂停"); // 被取消

    private Integer code;
    private String desc;

    TaskStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
