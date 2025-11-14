import type { RawFileNode } from "@/type/file";
import { ref } from "vue";

export const thematicConfig = ref({})
export const selectedResult = ref(null);
export const platformDataFile = ref<RawFileNode[]>([])