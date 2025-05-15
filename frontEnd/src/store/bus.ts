
type Handler = (...args: any[]) => void

class Bus {
  private events: Record<string, Handler[]> = {}

  on(event: string, handler: Handler) {
    (this.events[event] || (this.events[event] = [])).push(handler)
  }

  off(event: string, handler: Handler) {
    if (!this.events[event]) return
    this.events[event] = this.events[event].filter(h => h !== handler)
  }

  emit(event: string, ...args: any[]) {
    this.events[event]?.forEach(handler => handler(...args))
  }
}

const bus = new Bus()
export default bus