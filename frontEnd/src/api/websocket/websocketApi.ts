import WebSocketManager from './websocket'

// 动态生成 WebSocket URL
const createWebSocketUrl = (userId: string, projectId: string) => {
    // 1. 获取当前页面的协议，如果是 https: 则使用 wss，否则使用 ws
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    
    // 2. 动态拼接协议和当前域名
    return `${protocol}//${window.location.host}/websocket/userId/${userId}/projectId/${projectId}`;
}

// 创建 WebSocket 实例
export const createWebSocket = (userId: string, projectId: string) => {
    const url = createWebSocketUrl(userId, projectId)
    return new WebSocketManager(url)
}
