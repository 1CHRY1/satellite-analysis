import { defineStore } from 'pinia'

export const useAnalysisStore = defineStore('analysisStore',{
    state(){
        return{
          mosaicBucket : '' as string,
          mosaicPath : '' as string ,
          bandList: '' as string
        }
    },
    actions: {
        // 更新单字段
        updateField(field, value) {
        if (this.hasOwnProperty(field)) {
            this[field] = value;
        }
        },
        // 批量更新
        updateFields(load) {
        Object.keys(load).forEach(key => {
            if (this.hasOwnProperty(key)) {
            this[key] = load[key];
            }
        });
        },
    }
}
)