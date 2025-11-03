package com.ogms.dge.container.modules.common.enums;

public enum UploadRecordStatusEnum {
    UPLOAD_SUCCESS(0, "成功"),
    UPLOAD_TRANSFER(1, "转码中"),
    UPLOAD_FAIL(2, "失败"),
    FIRST_CONFIG(3, "第一步配置"),
    SECOND_CONFIG(4, "第二步配置"),
    SUCCESS(5, "成功");

    private final Integer code;
    private final String desc;

    UploadRecordStatusEnum(Integer code, String desc) {
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
