import { defineStore } from 'pinia'

export interface UserInfo {
    name: string
    email: string
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
                email: '',
                title: '',
                organization: '',
                role: '',
            }
        })(),
    }),
    getters: {
        userInfo: (state) => `${state.user.name} <${state.user.email}>`,
    },
    actions: {
        updateUser(newUser: UserInfo) {
            this.user = { ...this.user, ...newUser }
        },
        // Mock login logic
        login(credentials: { name: string; email: string; id: string; title: string; organization: string }) {
            this.authenticated = true
            this.user.id = credentials.id
            this.user.name = credentials.name
            this.user.email = credentials.email
            this.user.title = credentials.title
            this.user.organization = credentials.organization
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
