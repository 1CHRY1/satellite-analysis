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
        path:'/home',
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
        path: '/tool',
        component: () => import('@/views/tool.vue'),
        meta: {
            requiresAuth: true,
        },
    },
    {
        path: '/explore',
        component: () => import('@/components/dataCenter/interactiveExplore/interactiveExplore.vue'),
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
        component: () => import('@/components/dataCenter/dynamicAnalysis/dynamicAnalysis.vue'),
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
        path: '/tools',
        component: () => import('@/views/tools.vue'),
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
        component: () => import('@/components/login/login_window.vue'),
        meta: {
            requiresGuest: true,
        },
    },
    {
        isBar: false,
        path: "/user",
        name: "个人空间",
        component: () => import("../components/user/usercenter.vue"),
        meta: {
          requireAuth: true,
        },
        children: [
          {
            isBar: false,
            path: "",
            name: "概览",
            component: () =>
              import("../components/user/userFunctionCollection/overview.vue"),
            meta: {
              showUserActions: true,
            },
          },
          {
            isBar: false,
            path: "projects",
            name: "我的项目",
            component: () =>
              import("../components/user/userFunctionCollection/userProject.vue"),
          },
          {
            isBar: false,
            path: "data",
            name: "我的数据",
            component: () =>
              import("../components/user/userFunctionCollection/userData.vue"),
          },
        ],
      },
    // {
    //     path: '/register',
    //     // @ts-ignore
    //     component: () => import('@/components/login/register.vue'),
    //     meta: {
    //         requiresGuest: true,
    //     },
    // },
]
