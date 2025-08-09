import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface UserInfo {
    name: string
    phone: string
    province: string
    city: string
    email: string
    id: string
    title: string
    organization: string
    introduction: string
}

interface User {
  id: string
  name: string
  email: string
  title: string
  organization: string
  role: string
}

export const useUserStore = defineStore('user-store', {
    state: () => ({
        authenticated: !!localStorage.getItem('userId'), // temp 2025.3.11
        user: (() => {
            try {
                const storedUser = localStorage.getItem('user')
                if (storedUser) {
                    return {
                        ...JSON.parse(storedUser),
                        id: localStorage.getItem('userId'),
                    } // 尝试解析并返回用户数据
                }
            } catch (error) {
                console.error('Failed to parse user from localStorage:', error)
            }
            // 如果解析失败或没有数据，返回默认值
            return {
                id: localStorage.getItem('userId') || '???',
                name: '',
                phone:'',
                province:'',
                city:'',
                email: '',
                title: '',
                organization: '',
                role: '',
                introduction:''
            }
        })(),
    }),
    getters: {
        userInfo: (state) => `${state.user.name} <${state.user.email}>`,
    },
    actions: {
        updateUser(newUser: UserInfo) {
            this.user = { ...this.user, ...newUser }
            localStorage.setItem('user', JSON.stringify(this.user))
        },
        // Mock login logic
        login(credentials: {
            name: string
            phone: string
            province: string
            city: string
            email: string
            id: string
            title: string
            organization: string
            introduction: string
        }) {
            this.authenticated = true
            this.user.id = credentials.id
            this.user.name = credentials.name
            this.user.phone = credentials.phone
            this.user.province = credentials.province
            this.user.city = credentials.city
            this.user.email = credentials.email
            this.user.title = credentials.title
            this.user.organization = credentials.organization
            this.user.introduction = credentials.introduction
            this.user.role = 'user'
            localStorage.setItem('user', JSON.stringify(this.user))
        },
        logout() {
            this.authenticated = false
            this.user = { id: '', name: '', email: '', title: '', organization: '', role: 'guest' }
            localStorage.removeItem('token')
            localStorage.removeItem('refreshToken')
            localStorage.removeItem('userId')
        },
    },
})
