package com.ogms.dge.container.modules.common.enums;

public enum UploadRecordTypeEnum {
    METHOD(0, "方法"),
    DATA(1, "数据");

    private final Integer code;
    private final String desc;

    UploadRecordTypeEnum(Integer code, String desc) {
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
