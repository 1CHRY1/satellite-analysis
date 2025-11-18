import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { message } from 'antd'
import { getUserContext } from '~/features/user/provider'
import type { UserContextType } from '~/features/user/context'
// import ezStore from '@/store/ezStore'
// import router from '@/router'
// import { useUserStore } from '@/store/userStore'

const logout = (msg?: string) => {
    const {dispatch} = getUserContext() as UserContextType
    // 没有 refreshToken，跳转登录页
    dispatch( {
        type: "LOGOUT"
    } )
    localStorage.removeItem("token")
    localStorage.removeItem("refreshToken")
    localStorage.removeItem("userId")
    if (msg !== undefined)
        message.warning(msg)
    window.location.href = "/login";
}

class HttpClient {
    private instance: AxiosInstance

    constructor(baseURL: string) {
        this.instance = axios.create({
            baseURL,
            // timeout: 10000,
            timeout: 10000000,
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
            async (error) => {
                if (error.response?.status === 401) {
                    // 🚨 Token 过期，尝试刷新
                    const refreshToken = localStorage.getItem('refreshToken')
                    if (!refreshToken) {
                        logout('登录已过期，请重新登录')
                        return Promise.reject(error)
                    }
                    try {
                        //  发送请求获取新 token
                        // console.log('刷新 Token ', refreshToken)

                        const res = await axios.post(
                            '/api/user/refresh',
                            {},
                            {
                                headers: {
                                    'Refresh-Token': refreshToken,
                                },
                            },
                        )
                        // console.log('刷新 Token 成功', res.data)
                        console.log(res.data)
                        if (!!res.data.data.accessToken) {
                            //  存储新 token
                            localStorage.setItem('token', res.data.data.accessToken)
                            //  重新请求失败的 API
                            error.config.headers.Authorization = `Bearer ${res.data.data.accessToken}`
                            return this.instance(error.config)
                        }
                    } catch (err) {
                        console.error('刷新 Token 失败', err)
                        logout()
                        return Promise.reject(err)
                    }
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
export const clientV1 = new HttpClient('/api')
export const clientV3 = new HttpClient('/api3')
