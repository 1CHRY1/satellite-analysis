import * as mapboxgl from 'mapbox-gl';
import { mapManager } from './mapManager'; // 引入 MapManager 单例

// 定义图层配置的接口 (不变)
export interface ManagedLayer {
    id: string; 
    name: string; // 用户友好的图层名称，这里我们默认使用ID，或者尝试从metadata获取
    type: mapboxgl.AnyLayer['type'];
    visibility: 'visible' | 'none';
    opacity: number; 
    isUserLayer: boolean; // 新增属性：区分用户添加的业务图层和底图/系统图层
}

class LayerManager {
    private static instance: LayerManager | null;
    private managedLayers: ManagedLayer[] = []; 
    private updateCallbacks: (() => void)[] = []; 
    private systemLayerIds: Set<string> = new Set(); // 存储底图和 MapboxDraw 等系统图层的ID
    private map: mapboxgl.Map | null = null;
    
    private constructor() {}

    public static getInstance(): LayerManager {
        if (!LayerManager.instance) {
            LayerManager.instance = new LayerManager();
        }
        return LayerManager.instance;
    }
    
    // --- 核心初始化方法 ---
    public async initializeObserver(): Promise<void> {
        if (this.map) return; // 避免重复初始化

        await mapManager.waitForInit();
        mapManager.withMap(map => {
            this.map = map;
            
            // 1. 记录系统图层ID (例如底图图层)
            this.captureInitialSystemLayers(map);

            // 2. 监听关键事件，在这些事件后同步图层状态
            map.on('styledata', this.syncLayers.bind(this)); // 样式变化，图层可能改变
            map.on('data', this.syncLayers.bind(this)); // 数据变化，以防万一
            
            // 3. 立即执行一次同步
            this.syncLayers();
        });
    }
    
    /**
     * 在地图加载时，记录当前所有图层 ID 为系统图层。
     */
    private captureInitialSystemLayers(map: mapboxgl.Map): void {
        this.map?.getStyle()?.layers.forEach(layer => {
            this.systemLayerIds.add(layer.id);
        });
        // 排除 Mapbox Draw 相关的图层，它们通常以 'gl-draw-' 开头
    }
    
    /**
     * 核心方法：同步 Mapbox 上的图层状态到管理器列表。
     */
    private syncLayers(): void {
        if (!this.map) return;
        
        const currentMapLayers: mapboxgl.AnyLayer[] = this.map.getStyle()?.layers || [];
        const newManagedLayers: ManagedLayer[] = [];
        
        currentMapLayers.forEach(layer => {
            // 排除 Mapbox Draw 相关的图层和系统底图图层
            if (layer.id.startsWith('gl-draw-') || this.systemLayerIds.has(layer.id)) {
                return;
            }
            
            // 1. 【关键改动】：从 layer.metadata 中读取 Label
            const userLabel = (layer.metadata as any)?.['user-label']; 
            
            // 2. 确定最终要显示的名称
            const layerName = userLabel || layer.id; // 优先使用 Label，其次使用 ID
    
            // 尝试在现有列表中查找该图层，以保留用户设置的名称等属性 (如果将来允许用户编辑名称)
            const existingLayer = this.managedLayers.find(l => l.id === layer.id);
    
            const managedLayer: ManagedLayer = {
                id: layer.id,
                // 【关键改动】：使用解析出的 layerName
                name: existingLayer?.name || layerName, 
                type: layer.type,
                visibility: this.getLayerVisibility(this.map!, layer.id),
                opacity: this.getLayerOpacity(this.map!, layer.id),
                isUserLayer: true, 
            };
            
            newManagedLayers.push(managedLayer);
        });
    
        // ... (省略检查图层删除和通知更新的逻辑) ...
        const removedLayerIds = this.managedLayers
            .map(l => l.id)
            .filter(id => !currentMapLayers.some(l => l.id === id));
            
        if (newManagedLayers.length !== this.managedLayers.length || removedLayerIds.length > 0 || 
            // 增加一个条件：检查名字是否变化
            newManagedLayers.some((nl, i) => nl.name !== this.managedLayers[i]?.name)) { 
            this.managedLayers = newManagedLayers;
            this.notifyUpdate();
        }
    }

    // --- 供 Vue 组件调用的接口 (部分不变) ---
    
    public registerUpdateCallback(callback: () => void): void {
        this.updateCallbacks.push(callback);
    }
    
    private notifyUpdate(): void {
        this.updateCallbacks.forEach(callback => callback());
    }

    public getLayers(): ManagedLayer[] {
        // 返回一个副本以防止外部直接修改内部状态，并且只返回用户图层
        return this.managedLayers.filter(l => l.isUserLayer).slice().reverse(); // 倒序显示
    }

    /**
     * 切换图层的可见性 (直接操作 Mapbox，不影响原有逻辑)
     */
    public async toggleVisibility(layerId: string): Promise<void> {
        const layer = this.managedLayers.find(l => l.id === layerId);
        if (!layer || !this.map) return;

        const newVisibility = layer.visibility === 'visible' ? 'none' : 'visible';
        this.map.setLayoutProperty(layerId, 'visibility', newVisibility);
        
        // 更新内部状态并通知组件
        layer.visibility = newVisibility;
        this.notifyUpdate();
    }

    /**
     * 调整图层的透明度 (直接操作 Mapbox)
     */
    public async setOpacity(layerId: string, opacity: number): Promise<void> {
        const layer = this.managedLayers.find(l => l.id === layerId);
        if (!layer || !this.map) return;

        layer.opacity = opacity;
        
        const mapboxLayer = this.map.getLayer(layerId);
        if (!mapboxLayer) return;

        const opacityProp = this.getOpacityPaintProperty(mapboxLayer.type);
        if (opacityProp) {
            this.map.setPaintProperty(layerId, opacityProp as any, opacity);
            this.notifyUpdate();
        }
    }

    /**
     * 调整图层顺序 (直接操作 Mapbox)
     */
    public async moveLayer(layerId: string, direction: 'up' | 'down'): Promise<void> {
        if (!this.map) return;
        
        const userLayers = this.managedLayers.filter(l => l.isUserLayer);
        const index = userLayers.findIndex(l => l.id === layerId);
        if (index === -1) return;

        let newIndex = index + (direction === 'up' ? -1 : 1);
        if (newIndex < 0 || newIndex >= userLayers.length) return;

        // 1. 在 Mapbox 地图上重新插入图层 (这是最关键的步骤)
        let beforeId: string | undefined = undefined;
        
        if (direction === 'up') {
            beforeId = userLayers[newIndex].id;
        } else { // direction === 'down'
            // 目标图层的下一个图层，作为 beforeId
            if (newIndex < userLayers.length - 1) {
                beforeId = userLayers[newIndex + 1].id;
            }
            // 如果是移动到最底层，beforeId 保持 undefined，Mapbox 会将其放在顶部图层的底部
        }
        
        this.map.moveLayer(layerId, beforeId);
        
        // 2. 更新管理列表的顺序 (确保下一次 syncLayers 保持一致)
        const [movedLayer] = this.managedLayers.splice(index, 1);
        this.managedLayers.splice(newIndex, 0, movedLayer);
        
        this.notifyUpdate();
    }

    /**
     * 从管理器和地图上删除图层 (直接操作 Mapbox)
     */
    public async removeLayer(layerId: string): Promise<void> {
        if (!this.map) return;
        
        const layer = this.map.getLayer(layerId);
        if (!layer) return;

        const sourceId = (layer as any).source;
        
        // 移除图层
        try {
            this.map.removeLayer(layerId);
        } catch(e) {
            console.error(`Error removing layer ${layerId}:`, e);
        }
        
        // 移除 Source (如果存在且不是系统 Source)
        if (sourceId && this.map.getSource(sourceId)) {
             try {
                 this.map.removeSource(sourceId);
             } catch(e) {
                 // 忽略 Mapbox 抛出的 'Source is in use' 错误
             }
        }

        // 从管理列表中移除 (由 syncLayers 负责)
        this.syncLayers();
    }
    
    // --- 辅助函数 ---
    
    private getLayerVisibility(map: mapboxgl.Map, layerId: string): 'visible' | 'none' {
        return map.getLayoutProperty(layerId, 'visibility') as 'visible' | 'none' || 'visible';
    }
    
    private getLayerOpacity(map: mapboxgl.Map, layerId: string): number {
        const layer = map.getLayer(layerId);
        if (!layer) return 1.0;
        
        const opacityProp = this.getOpacityPaintProperty(layer.type);
        if (!opacityProp) return 1.0;

        const opacity = map.getPaintProperty(layerId, opacityProp as any) as number | undefined;
        return opacity !== undefined ? opacity : 1.0;
    }
    
    private getOpacityPaintProperty(type: string): string | undefined {
        switch (type) {
            case 'raster':
                return 'raster-opacity';
            case 'fill':
                return 'fill-opacity';
            case 'line':
                return 'line-opacity';
            case 'symbol':
                return 'icon-opacity'; 
            case 'circle':
                return 'circle-opacity';
            default:
                return undefined;
        }
    }
}

export const layerManager = LayerManager.getInstance();