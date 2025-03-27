class WebSocketManager {
    private socket: WebSocket | null = null
    private url: string
    private listeners: { [key: string]: Function[] } = {}
    private heartbeatInterval: number | null = null // 心跳定时器

    constructor(url: string) {
        this.url = url
    }

    // 初始化 WebSocket 连接
    connect() {
        if (this.socket) return

        this.socket = new WebSocket(this.url)

        this.socket.onopen = () => {
            console.log('WebSocket 连接已建立')
            this.emit('open')
            this.startHeartbeat() // 连接成功后启动心跳
        }

        this.socket.onmessage = (event) => {
            // const data = JSON.parse(event.data)
            const data = event.data
            // console.log('收到服务器消息:', data)
            this.emit('message', data)
        }

        this.socket.onerror = (error) => {
            console.error('WebSocket 错误:', error)
            this.emit('error', error)
        }

        this.socket.onclose = () => {
            console.log('WebSocket 连接已关闭')
            this.stopHeartbeat() // 连接关闭时停止心跳
            this.emit('close')
        }
    }

    // 发送消息
    send(data: any) {
        if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
            console.error('WebSocket 未连接，无法发送消息')
            return
        }
        this.socket.send(JSON.stringify(data))
    }

    // 关闭连接
    close() {
        if (this.socket) {
            this.socket.close()
            this.socket = null
        }
    }
    // 启动心跳
    private startHeartbeat() {
        this.heartbeatInterval = setInterval(() => {
            if (this.socket && this.socket.readyState === WebSocket.OPEN) {
                console.log('发送心跳包...')
                this.send({ type: 'heartbeat' }) // 发送心跳消息
            }
        }, 30000) // 每 30 秒发送一次心跳
    }

    // 停止心跳
    private stopHeartbeat() {
        if (this.heartbeatInterval) {
            clearInterval(this.heartbeatInterval)
            this.heartbeatInterval = null
        }
    }

    // 注册事件监听器
    on(event: string, callback: Function) {
        if (!this.listeners[event]) {
            this.listeners[event] = []
        }
        this.listeners[event].push(callback)
    }

    // 触发事件
    private emit(event: string, data?: any) {
        if (this.listeners[event]) {
            this.listeners[event].forEach((callback) => callback(data))
        }
    }
}

export default WebSocketManager
