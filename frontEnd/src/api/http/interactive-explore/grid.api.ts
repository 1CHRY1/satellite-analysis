import http from "@/api/axiosClient/clientHttp3"
import type { Grid } from "./grid.type"

export async function getScenesInGrid(param: Grid.GridRequest): Promise<Grid.SceneDetailStatsResponse> {
    return http.post<Grid.SceneDetailStatsResponse>(`data/grid/scene`, param)
}
export async function getThemesInGrid(param: Grid.GridRequest): Promise<Grid.ThemeDetailStatsResponse> {
    return http.post<Grid.ThemeDetailStatsResponse>(`data/grid/theme`, param)
}