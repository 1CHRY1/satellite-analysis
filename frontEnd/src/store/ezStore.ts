class EzStore {
    private static instance: EzStore
    private store: Record<string, any>

    private constructor() {
        this.store = {}
    }

    public static getInstance(): EzStore {
        if (!EzStore.instance) {
            EzStore.instance = new EzStore()
        }
        return EzStore.instance
    }

    public get(key: string): any {
        return this.store[key]
    }

    public set(key: string, value: any): void {
        this.store[key] = value
    }
}

export default EzStore.getInstance()
