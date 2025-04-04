<template>
    <div class="min-h-screen bg-slate-50">
        <!-- Header Navigation -->
        <!-- <header class="bg-white shadow-sm">
            <div class="container mx-auto px-4 py-4">
                <div class="flex items-center justify-between">
                    <div class="flex items-center space-x-2">
                        <satellite class="h-8 w-8 text-primary" />
                        <span class="text-xl font-bold text-slate-800">SatelliteIQ</span>
                    </div>
                    <nav class="hidden md:flex space-x-8">
                        <a href="#" class="text-primary font-medium">Dashboard</a>
                        <a href="#" class="text-slate-600 hover:text-primary transition-colors">Image Library</a>
                        <a href="#" class="text-slate-600 hover:text-primary transition-colors">Analysis Tools</a>
                        <a href="#" class="text-slate-600 hover:text-primary transition-colors">Documentation</a>
                    </nav>
                    <div class="flex items-center space-x-4">
                        <button
                            class="hidden md:block px-4 py-2 rounded-md border border-slate-200 text-slate-700 hover:bg-slate-50 transition-colors">
                            Sign In
                        </button>
                        <button
                            class="hidden md:block px-4 py-2 rounded-md bg-primary text-white hover:bg-primary/90 transition-colors">
                            Get Started
                        </button>
                        <menu class="md:hidden text-slate-700 cursor-pointer">
                            <menu-icon class="h-6 w-6" />
                        </menu>
                    </div>
                </div>
            </div>
        </header> -->

        <!-- Hero Section -->
        <section class="bg-gradient-to-br from-slate-800 to-slate-900 py-16 text-white md:py-24">
            <div class="container mx-auto px-4">
                <div class="grid items-center gap-0 md:grid-cols-2">
                    <!-- <div class="flex items-center mx-10 justify-between"> -->
                    <div class="space-y-6">
                        <h1 class="flex items-center text-4xl leading-tight font-bold md:text-5xl">遥感ARD平台</h1>
                        <p class="text-lg text-slate-300">利用我们的综合分析工具和可视化功能，从卫星数据中获取强大的洞察力。</p>
                        <div class="flex flex-col space-y-3 pt-4 sm:flex-row sm:space-y-0 sm:space-x-4">
                            <button class="bg-primary hover:bg-primary/90 rounded-md px-6 py-3 font-medium text-white transition-colors">
                                探索平台
                            </button>
                            <button class="rounded-md border border-slate-600 px-6 py-3 text-white transition-colors hover:bg-white/10">
                                观看演示
                            </button>
                        </div>
                    </div>
                    <div class="relative">
                        <div class="bg-primary/20 absolute -top-6 -left-6 h-24 w-24 rounded-full blur-xl"></div>
                        <div class="absolute -right-10 -bottom-10 h-32 w-32 rounded-full bg-blue-500/20 blur-xl"></div>
                        <img
                            src="@/assets/image/home/banner.jpeg"
                            alt="Satellite imagery visualization"
                            class="relative z-10 h-auto w-full rounded-lg shadow-2xl"
                        />
                    </div>
                </div>
            </div>
        </section>

        <!-- Features Section -->
        <section class="bg-white py-16">
            <div class="container mx-auto px-4">
                <div class="mb-16 text-center">
                    <h2 class="text-3xl font-bold text-slate-800">强大的分析功能</h2>
                    <p class="mx-auto mt-4 max-w-2xl text-slate-600">我们的平台为卫星图像处理、分析和可视化提供了全面的工具。</p>
                </div>

                <div class="grid gap-8 md:grid-cols-3">
                    <div class="rounded-xl bg-slate-50 p-6 shadow-sm transition-shadow hover:shadow-md">
                        <div class="bg-primary/10 mb-4 flex h-12 w-12 items-center justify-center rounded-lg">
                            <Search class="text-primary h-6 w-6" />
                        </div>
                        <h3 class="mb-2 text-xl font-semibold text-slate-800">图像检索</h3>
                        <p class="text-slate-600">利用高级过滤选项访问和检索多个来源的卫星图像。</p>
                    </div>

                    <div class="rounded-xl bg-slate-50 p-6 shadow-sm transition-shadow hover:shadow-md">
                        <div class="bg-primary/10 mb-4 flex h-12 w-12 items-center justify-center rounded-lg">
                            <bar-chart-2 class="text-primary h-6 w-6" />
                        </div>
                        <h3 class="mb-2 text-xl font-semibold text-slate-800">数据可视化</h3>
                        <p class="text-slate-600">将复杂的卫星数据转化为清晰、可操作的可视化信息和视角。</p>
                    </div>

                    <div class="rounded-xl bg-slate-50 p-6 shadow-sm transition-shadow hover:shadow-md">
                        <div class="bg-primary/10 mb-4 flex h-12 w-12 items-center justify-center rounded-lg">
                            <SquareChevronRight class="text-primary h-6 w-6" />
                        </div>
                        <h3 class="mb-2 text-xl font-semibold text-slate-800">在线分析</h3>
                        <p class="text-slate-600">利用强大的在线编程工具对卫星图像进行定制分析。</p>
                    </div>
                </div>
            </div>
        </section>

        <!-- Visualization Preview -->
        <section class="bg-slate-50 py-16">
            <div class="container mx-auto px-4">
                <div class="grid items-center gap-12 md:grid-cols-2">
                    <div class="order-2 md:order-1">
                        <div class="rounded-xl bg-white p-1 shadow-lg">
                            <img src="@/assets/image/home/dataInteraction.png" alt="Data visualization dashboard" class="h-auto w-full rounded-lg" />
                        </div>
                    </div>
                    <div class="order-1 space-y-6 md:order-2">
                        <h2 class="text-3xl font-bold text-slate-800">交互式数据可视化</h2>
                        <p class="text-slate-600">我们的平台提供强大的可视化工具，可将复杂的卫星数据转化为清晰、可操作的洞察信息。</p>
                        <ul class="space-y-3">
                            <li class="flex items-start">
                                <check-circle class="mt-0.5 mr-2 h-6 w-6 flex-shrink-0 text-green-500" />
                                <span class="text-slate-700">实时数据处理和可视化</span>
                            </li>
                            <li class="flex items-start">
                                <check-circle class="mt-0.5 mr-2 h-6 w-6 flex-shrink-0 text-green-500" />
                                <span class="text-slate-700">可定制的仪表板和报告工具</span>
                            </li>
                            <li class="flex items-start">
                                <check-circle class="mt-0.5 mr-2 h-6 w-6 flex-shrink-0 text-green-500" />
                                <span class="text-slate-700">高级过滤和数据筛选功能</span>
                            </li>
                            <li class="flex items-start">
                                <check-circle class="mt-0.5 mr-2 h-6 w-6 flex-shrink-0 text-green-500" />
                                <span class="text-slate-700">空间数据的在线预览功能</span>
                            </li>
                        </ul>
                        <button class="bg-primary hover:bg-primary/90 rounded-md px-6 py-3 font-medium text-white transition-colors">
                            探索可视化
                        </button>
                    </div>
                </div>
            </div>
        </section>

        <!-- Quick Access Tools -->
        <section class="bg-white py-16">
            <div class="container mx-auto px-4">
                <div class="mb-12 text-center">
                    <h2 class="text-3xl font-bold text-slate-800">快速访问工具</h2>
                    <p class="mx-auto mt-4 max-w-2xl text-slate-600">直接从仪表板访问我们最常用的工具。</p>
                </div>

                <div class="grid grid-cols-2 gap-6 md:grid-cols-4">
                    <div class="cursor-pointer rounded-xl bg-slate-50 p-6 text-center transition-shadow hover:shadow-md">
                        <div class="bg-primary/10 mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full">
                            <Map class="text-primary h-8 w-8" />
                        </div>
                        <h3 class="text-lg font-semibold text-slate-800">地图</h3>
                    </div>

                    <div class="cursor-pointer rounded-xl bg-slate-50 p-6 text-center transition-shadow hover:shadow-md">
                        <div class="bg-primary/10 mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full">
                            <layers class="text-primary h-8 w-8" />
                        </div>
                        <h3 class="text-lg font-semibold text-slate-800">图层分析</h3>
                    </div>

                    <div class="cursor-pointer rounded-xl bg-slate-50 p-6 text-center transition-shadow hover:shadow-md">
                        <div class="bg-primary/10 mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full">
                            <activity class="text-primary h-8 w-8" />
                        </div>
                        <h3 class="text-lg font-semibold text-slate-800">变化检测</h3>
                    </div>

                    <div class="cursor-pointer rounded-xl bg-slate-50 p-6 text-center transition-shadow hover:shadow-md">
                        <div class="bg-primary/10 mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full">
                            <download class="text-primary h-8 w-8" />
                        </div>
                        <h3 class="text-lg font-semibold text-slate-800">数据探索</h3>
                    </div>
                </div>
            </div>
        </section>

        <!-- Recent Analysis Section -->
        <section class="bg-slate-50 py-16">
            <div class="container mx-auto px-4">
                <div class="mb-8 flex items-center justify-between">
                    <h2 class="text-2xl font-bold text-slate-800">最新案例</h2>
                    <button class="text-primary flex items-center font-medium">
                        查看更多
                        <chevron-right class="ml-1 h-5 w-5" />
                    </button>
                </div>

                <div class="grid gap-6 md:grid-cols-3">
                    <div class="overflow-hidden rounded-xl bg-white shadow-sm transition-shadow hover:shadow-md">
                        <img src="@/assets/image/home/case1.jpeg" alt="Analysis preview" class="h-48 w-full object-cover" />
                        <div class="p-5">
                            <div class="mb-3 flex items-center justify-between">
                                <span class="text-xs font-medium text-slate-500">2023/3/15</span>
                                <span class="rounded-full bg-green-100 px-2 py-1 text-xs font-medium text-green-800">已完成</span>
                            </div>
                            <h3 class="mb-2 text-lg font-semibold text-slate-800">城市发展分析</h3>
                            <p class="mb-4 text-sm text-slate-600">利用时间序列卫星图像分析大都市地区的城市扩张。</p>
                            <button class="text-primary text-sm font-medium">查阅细节</button>
                        </div>
                    </div>

                    <div class="overflow-hidden rounded-xl bg-white shadow-sm transition-shadow hover:shadow-md">
                        <img src="@/assets/image/home/case2.webp" alt="Analysis preview" class="h-48 w-full object-cover" />
                        <div class="p-5">
                            <div class="mb-3 flex items-center justify-between">
                                <span class="text-xs font-medium text-slate-500">2024/6/17</span>
                                <span class="rounded-full bg-green-100 px-2 py-1 text-xs font-medium text-green-800">已完成</span>
                            </div>
                            <h3 class="mb-2 text-lg font-semibold text-slate-800">植被指数图</h3>
                            <p class="mb-4 text-sm text-slate-600">对农业区域进行NDVI分析，以监测作物健康和生长模式。</p>
                            <button class="text-primary text-sm font-medium">查阅细节</button>
                        </div>
                    </div>

                    <div class="overflow-hidden rounded-xl bg-white shadow-sm transition-shadow hover:shadow-md">
                        <img src="@/assets/image/home/case3.png" alt="Analysis preview" class="h-48 w-full object-cover" />
                        <div class="p-5">
                            <div class="mb-3 flex items-center justify-between">
                                <span class="text-xs font-medium text-slate-500">2025/3/19</span>
                                <span class="rounded-full bg-blue-100 px-2 py-1 text-xs font-medium text-blue-800">进行中</span>
                            </div>
                            <h3 class="mb-2 text-lg font-semibold text-slate-800">海岸侵蚀研究</h3>
                            <p class="mb-4 text-sm text-slate-600">海岸线变化的时间序列分析，以确定侵蚀模式和速率。</p>
                            <button class="text-primary text-sm font-medium">查阅细节</button>
                        </div>
                    </div>
                </div>
            </div>
        </section>

        <!-- CTA Section -->
        <section class="bg-gradient-to-br from-slate-800 to-slate-900 py-16 text-white">
            <div class="container mx-auto px-4 text-center">
                <h2 class="mb-6 text-3xl font-bold">准备好从卫星数据中解读最新信息了吗？</h2>
                <p class="mx-auto mb-8 max-w-2xl text-slate-300">
                    加入数以千计的研究人员、组织和政府机构的行列，使用我们的平台分析卫星图像并获得可行的见解。
                </p>
                <div class="flex flex-col justify-center space-y-4 sm:flex-row sm:space-y-0 sm:space-x-4">
                    <button class="bg-primary hover:bg-primary/90 rounded-md px-8 py-3 font-medium text-white transition-colors">开始行动</button>
                    <button class="rounded-md border border-slate-600 px-8 py-3 text-white transition-colors hover:bg-white/10">预约演示</button>
                </div>
            </div>
        </section>

        <!-- Footer -->
        <footer class="bg-slate-900 py-12 text-slate-400">
            <div class="container mx-auto px-4">
                <div class="grid gap-8 md:grid-cols-4">
                    <div>
                        <div class="mb-4 flex items-center space-x-2">
                            <satellite class="text-primary h-8 w-8" />
                            <span class="text-xl font-bold text-white">遥感ARD平台</span>
                        </div>
                        <p class="text-sm">为研究人员、组织和政府机构提供先进的遥感ARD平台。</p>
                    </div>

                    <div>
                        <h3 class="mb-4 font-semibold text-white">平台</h3>
                        <ul class="space-y-2 text-sm">
                            <li>
                                <a href="#" class="transition-colors hover:text-white">平台1</a>
                            </li>
                            <li>
                                <a href="#" class="transition-colors hover:text-white">平台2</a>
                            </li>
                            <li>
                                <a href="#" class="transition-colors hover:text-white">平台3</a>
                            </li>
                            <li>
                                <a href="#" class="transition-colors hover:text-white">平台4</a>
                            </li>
                        </ul>
                    </div>

                    <div>
                        <h3 class="mb-4 font-semibold text-white">资源</h3>
                        <ul class="space-y-2 text-sm">
                            <li>
                                <a href="#" class="transition-colors hover:text-white">资源1</a>
                            </li>
                            <li>
                                <a href="#" class="transition-colors hover:text-white">资源2</a>
                            </li>
                            <li>
                                <a href="#" class="transition-colors hover:text-white">资源3</a>
                            </li>
                            <li>
                                <a href="#" class="transition-colors hover:text-white">资源4</a>
                            </li>
                        </ul>
                    </div>

                    <div>
                        <h3 class="mb-4 font-semibold text-white">我们</h3>
                        <ul class="space-y-2 text-sm">
                            <li>
                                <a href="#" class="transition-colors hover:text-white">关于我们</a>
                            </li>
                            <li>
                                <a href="#" class="transition-colors hover:text-white">联系我们</a>
                            </li>
                            <li>
                                <a href="#" class="transition-colors hover:text-white">隐私政策</a>
                            </li>
                        </ul>
                    </div>
                </div>

                <div class="mt-12 flex flex-col items-center justify-between border-t border-slate-800 pt-8 md:flex-row">
                    <p class="text-sm">© 2025 OpenGMS 遥感ARD平台。 保留所有权利。</p>
                    <div class="mt-4 flex space-x-4 md:mt-0">
                        <a href="#" class="text-slate-400 transition-colors hover:text-white">
                            <!-- <twitter class="h-5 w-5" /> -->
                        </a>
                        <a href="#" class="text-slate-400 transition-colors hover:text-white">
                            <!-- <linkedin class="h-5 w-5" /> -->
                        </a>
                        <a href="#" class="text-slate-400 transition-colors hover:text-white">
                            <!-- <github class="h-5 w-5" /> -->
                        </a>
                    </div>
                </div>
            </div>
        </footer>
    </div>
</template>

<script setup lang="ts">
import {
    Satellite,
    // MenuIcon,
    BarChart2,
    CheckCircle,
    Layers,
    Activity,
    Download,
    ChevronRight,
    Map,
    Search,
    SquareChevronRight,
} from 'lucide-vue-next'
</script>

<style>
:root {
    --color-primary: #3b82f6;
    --color-primary-dark: #2563eb;
}

.text-primary {
    color: var(--color-primary);
}

.bg-primary {
    background-color: var(--color-primary);
}

.bg-primary\/10 {
    background-color: rgba(59, 130, 246, 0.1);
}

.bg-primary\/20 {
    background-color: rgba(59, 130, 246, 0.2);
}

.hover\:bg-primary\/90:hover {
    background-color: rgba(59, 130, 246, 0.9);
}

.border-primary {
    border-color: var(--color-primary);
}
</style>
