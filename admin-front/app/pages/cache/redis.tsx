import { ProTable } from "@ant-design/pro-components";
import { Button } from "antd";
// import { getRedisCacheList, flushRedisCache } from "@/services/cacheApi";

export default function RedisCachePage() {
    return (
        <ProTable
            rowKey="key"
            // request={getRedisCacheList}
            columns={[
                { title: "Redis Key", dataIndex: "key" },
                { title: "TTL", dataIndex: "ttl" },
                { title: "类型", dataIndex: "type" },
            ]}
            toolBarRender={() => [
                <Button danger 
                // onClick={() => flushRedisCache()}
                >
                    清空Redis缓存
                </Button>,
            ]}
        />
    );
}
