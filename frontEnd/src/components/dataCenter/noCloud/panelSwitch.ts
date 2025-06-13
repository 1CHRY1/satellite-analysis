import { ref, computed } from 'vue'

export function usePanelSwitchModule() {
    type PanelType = 'noCloud' | 'history'
    const currentPanel = ref<PanelType>('noCloud')

    const setCurrentPanel = (newVal: PanelType) => {
        currentPanel.value = newVal
        console.log('Panel changed to', newVal)
    }

    // 注意钩子函数中，如果以下变量被多次引用，那么他们会创建多个实例，而非共享!
    return {
        currentPanel,
        setCurrentPanel
    }
}