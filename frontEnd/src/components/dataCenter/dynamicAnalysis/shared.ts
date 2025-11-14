import { useTaskStore } from "@/store";
import type { RawFileNode } from "@/type/file";
import { ref } from "vue";

export const thematicConfig = ref({})
export const selectedResult = ref(null);
export const platformDataFile = ref<RawFileNode[]>([])
export const currentPanel = ref('analysis')
export const setCurrentPanel = (panel: string) => {
    currentPanel.value = panel
}
// 任务存储
export const taskStore = useTaskStore()
// 计算任务
export const calTask = ref({
    calState: 'start',
    taskId: '',
})