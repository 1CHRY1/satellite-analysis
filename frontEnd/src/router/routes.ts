export const routes = [
    {
        path: '/test',
        component: () => import('@/views/Test.vue'),
    },
    {
        path: '/',
        redirect: '/home',
    },
    {
        path: '/home',
        // component: () => import('@/views/HomeView.vue'),
        // @ts-ignore
        component: () => import('@/views/home.vue'),
    },
    {
        path: '/models',
        component: () => import('@/views/models.vue'),
        meta: {
            requiresAuth: true,
        },
    },
    {
        path: '/data',
        component: () => import('@/views/DataView.vue'),
        meta: {
            requiresAuth: true,
        },
    },
    {
        path: '/explore',
        component: () => import('@/components/dataCenter/interactiveExplore.vue'),
        meta: {
            requiresAuth: true,
        },
    },
    {
        path: '/nocloud',
        component: () => import('@/components/dataCenter/noCloud/pictureOfNoCloud.vue'),
        meta: {
            requiresAuth: true,
        },
    },
    {
        path: '/analysis',
        component: () => import('@/components/dataCenter/dynamicAnalysis.vue'),
        meta: {
            requiresAuth: true,
        },
    },
    {
        path: '/projects',
        component: () => import('@/views/projects.vue'),
        meta: {
            requiresAuth: true,
        },
    },
    {
        path: '/project/:projectId',
        component: () => import('@/views/Analysis.vue'),
        meta: {
            requiresAuth: true,
        },
    },
    {
        path: '/analysis',
        component: () => import('@/views/Analysis.vue'),
        meta: {
            requiresAuth: true,
        },
    },
    {
        path: '/about',
        component: () => import('@/views/about.vue'),
    },
    {
        path: '/login',
        // @ts-ignore
        component: () => import('@/components/login/login.vue'),
        meta: {
            requiresGuest: true,
        },
    },
    {
        path: '/register',
        // @ts-ignore
        component: () => import('@/components/login/register.vue'),
        meta: {
            requiresGuest: true,
        },
    },
]
