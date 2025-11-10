package com.ogms.dge.workspace.modules.workflow.enums;

public enum TaskTypeEnum {
    DATA_SYNC(0, "数据同步"),
    METHOD_SYNC(1, "方法同步");

    private Integer code;
    private String desc;

    TaskTypeEnum(Integer code, String desc) {
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
