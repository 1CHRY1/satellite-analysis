import WebSocketManager from './websocket'

// 动态生成 WebSocket URL
const createWebSocketUrl = (userId: string, projectId: string) => {
    // return `ws://223.2.43.228:31992/model/websocket/userId/${userId}/projectId/${projectId}`
    // return `ws://223.2.47.202:9001/model/websocket/userId/${userId}/projectId/${projectId}`
    return `ws://${window.location.host}/model/websocket/userId/${userId}/projectId/${projectId}`
}

// 创建 WebSocket 实例
export const createWebSocket = (userId: string, projectId: string) => {
    const url = createWebSocketUrl(userId, projectId)
    return new WebSocketManager(url)
}
