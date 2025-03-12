import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { message } from 'ant-design-vue'

class HttpClient {
    private instance: AxiosInstance

    constructor(baseURL: string) {
        this.instance = axios.create({
            baseURL,
            timeout: 5000,
            headers: {
                'Content-Type': 'application/json',
            },
        })

        this.initializeInterceptors()
    }

    private initializeInterceptors() {
        this.instance.interceptors.request.use(
            (config) => {
                const token = localStorage.getItem('token')
                if (token) {
                    config.headers['Authorization'] = `Bearer ${token}`
                }
                return config
            },
            (error) => {
                return Promise.reject(error)
            },
        )

        this.instance.interceptors.response.use(
            (response) => {
                return response.data
            },
            (error) => {
                if (error.response) {
                    switch (error.response.status) {
                        case 401:
                            console.warn('401 unauthorized')
                            message.error('401 not authorized')
                            break
                        case 404:
                            console.warn('404 not found')
                            message.error('404 not found')
                            break
                        default:
                            console.warn('unknow error: ' + error.response.status)
                            message.error('unknow error')
                            break
                    }
                }
                return Promise.reject(error)
            },
        )
    }

    public async get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
        return this.instance.get(url, config)
    }

    public async post<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
        return this.instance.post(url, data, config)
    }

    public async put<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
        return this.instance.put(url, data, config)
    }

    public async delete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
        return this.instance.delete(url, config)
    }
}

export default new HttpClient('https://api.example.com')
