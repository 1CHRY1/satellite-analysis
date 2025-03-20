import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { message } from 'ant-design-vue'
import ezStore from '@/store/ezStore'

class HttpClient {
    private instance: AxiosInstance

    constructor(baseURL: string) {
        this.instance = axios.create({
            baseURL,
            timeout: 5000,
        })

        this.initializeInterceptors()
    }

    private initializeInterceptors() {
        /////// Request Interceptor //////////////////////////////////
        this.instance.interceptors.request.use(
            (config) => {
                const token = localStorage.getItem('token')
                if (token) {
                    config.headers['Authorization'] = `Bearer ${token}`
                }
                return config
            },
            (error) => {
                message.error('request error')
                console.warn(error)
                return Promise.reject(error)
            },
        )
        /////// Response Interceptor //////////////////////////////////
        this.instance.interceptors.response.use(
            (response) => {
                return response.data
            },
            (error) => {
                if (error.response) {
                    const status = error.response.status
                    const errorMsg = `请求失败 (${status}): ${error.response.data?.message || error.message}`
                    console.error(errorMsg, error)
                    message.error(errorMsg)
                } else if (error.request) {
                    const errorMsg = `请求已发出，但服务器无响应`
                    console.error(errorMsg, error)
                    message.error(errorMsg)
                } else {
                    const errorMsg = `错误: ${error.message}`
                    console.error(errorMsg, error)
                    message.error(errorMsg)
                }
                return Promise.reject(error)
            },
        )
    }

    public async get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
        return await this.instance.get(url, config)
    }

    public async post<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
        return await this.instance.post(url, data, config)
    }

    public async put<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
        return await this.instance.put(url, data, config)
    }

    public async delete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
        return await this.instance.delete(url, config)
    }
}

export default new HttpClient(ezStore.get('conf')['back_app'])
