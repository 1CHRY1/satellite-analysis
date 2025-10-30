import { getImage } from "~/apis/https/image/image.admin";
import type { ImageRequest } from "~/apis/https/image/image.type";

export const getSceneImages = async (sceneId: ImageRequest) => {
    const res = await getImage(sceneId);
    if (res.status === 1) {
        return {
            data: res.data || [],
            success: true,
            total: res.data?.length || 0,
        };
    } else {
        return {
            data: [],
            success: false,
            total: 0,
        };
    }
};