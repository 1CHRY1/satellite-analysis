import { defineStore } from 'pinia'

export interface UserInfo {
    name: string
    email: string
}

export const UserStore = defineStore('user-store', {
    state: () => ({
        counter: 0,
        authenticated: true, // temp 2025.3.11
        user: {
            name: 'John Doe',
            email: 'john@example.com',
            role: 'guest',
        },
    }),
    getters: {
        doubleCount: (state) => state.counter * 2,
        userInfo: (state) => `${state.user.name} <${state.user.email}>`,
    },
    actions: {
        increment() {
            this.counter++
        },
        updateUser(newUser: UserInfo) {
            this.user = { ...this.user, ...newUser }
        },
        // Mock login logic
        login(credentials: { email: string; password: string }) {
            this.authenticated = true
            this.user.role = 'user'
        },
        logout() {
            this.authenticated = false
            this.user.role = 'guest'
        },
    },
})
