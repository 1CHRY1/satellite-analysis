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
        <section class="py-16 md:py-24 bg-gradient-to-br from-slate-800 to-slate-900 text-white">
            <div class="container mx-auto px-4">
                <div class="grid md:grid-cols-2 gap-0 items-center">
                    <!-- <div class="flex items-center mx-10 justify-between"> -->
                    <div class="space-y-6">
                        <h1 class="text-4xl md:text-5xl font-bold leading-tight flex items-center">
                            卫星图像分析平台
                        </h1>
                        <p class="text-lg text-slate-300">
                            利用我们的综合分析工具和可视化功能，从卫星数据中获取强大的洞察力。
                        </p>
                        <div class="flex flex-col sm:flex-row space-y-3 sm:space-y-0 sm:space-x-4 pt-4">
                            <button
                                class="px-6 py-3 rounded-md bg-primary text-white font-medium hover:bg-primary/90 transition-colors">
                                探索平台
                            </button>
                            <button
                                class="px-6 py-3 rounded-md border border-slate-600 text-white hover:bg-white/10 transition-colors">
                                观看演示
                            </button>
                        </div>
                    </div>
                    <div class="relative">
                        <div class="absolute -top-6 -left-6 w-24 h-24 bg-primary/20 rounded-full blur-xl"></div>
                        <div class="absolute -bottom-10 -right-10 w-32 h-32 bg-blue-500/20 rounded-full blur-xl">
                        </div>
                        <img src="@/assets/image/home/banner.jpeg" alt="Satellite imagery visualization"
                            class="w-full h-auto rounded-lg shadow-2xl relative z-10" />
                    </div>
                </div>
            </div>
        </section>

        <!-- Features Section -->
        <section class="py-16 bg-white">
            <div class="container mx-auto px-4">
                <div class="text-center mb-16">
                    <h2 class="text-3xl font-bold text-slate-800">强大的分析功能</h2>
                    <p class="mt-4 text-slate-600 max-w-2xl mx-auto">
                        我们的平台为卫星图像处理、分析和可视化提供了全面的工具。
                    </p>
                </div>

                <div class="grid md:grid-cols-3 gap-8">
                    <div class="bg-slate-50 p-6 rounded-xl shadow-sm hover:shadow-md transition-shadow">
                        <div class="w-12 h-12 bg-primary/10 rounded-lg flex items-center justify-center mb-4">
                            <Search class="h-6 w-6 text-primary" />
                        </div>
                        <h3 class="text-xl font-semibold text-slate-800 mb-2">图像检索</h3>
                        <p class="text-slate-600">
                            利用高级过滤选项访问和检索多个来源的卫星图像。
                        </p>
                    </div>

                    <div class="bg-slate-50 p-6 rounded-xl shadow-sm hover:shadow-md transition-shadow">
                        <div class="w-12 h-12 bg-primary/10 rounded-lg flex items-center justify-center mb-4">
                            <bar-chart-2 class="h-6 w-6 text-primary" />
                        </div>
                        <h3 class="text-xl font-semibold text-slate-800 mb-2">数据可视化</h3>
                        <p class="text-slate-600">
                            将复杂的卫星数据转化为清晰、可操作的可视化信息和视角。
                        </p>
                    </div>

                    <div class="bg-slate-50 p-6 rounded-xl shadow-sm hover:shadow-md transition-shadow">
                        <div class="w-12 h-12 bg-primary/10 rounded-lg flex items-center justify-center mb-4">
                            <SquareChevronRight class="h-6 w-6 text-primary" />
                        </div>
                        <h3 class="text-xl font-semibold text-slate-800 mb-2">在线分析</h3>
                        <p class="text-slate-600">
                            利用强大的在线编程工具对卫星图像进行定制分析。
                        </p>
                    </div>
                </div>
            </div>
        </section>

        <!-- Visualization Preview -->
        <section class="py-16 bg-slate-50">
            <div class="container mx-auto px-4">
                <div class="grid md:grid-cols-2 gap-12 items-center">
                    <div class="order-2 md:order-1">
                        <div class="bg-white p-1 rounded-xl shadow-lg">
                            <img src="@/assets/image/home/dataInteraction.png" alt="Data visualization dashboard"
                                class="w-full h-auto rounded-lg" />
                        </div>
                    </div>
                    <div class="space-y-6 order-1 md:order-2">
                        <h2 class="text-3xl font-bold text-slate-800">交互式数据可视化</h2>
                        <p class="text-slate-600">
                            我们的平台提供强大的可视化工具，可将复杂的卫星数据转化为清晰、可操作的洞察信息。
                        </p>
                        <ul class="space-y-3">
                            <li class="flex items-start">
                                <check-circle class="h-6 w-6 text-green-500 mr-2 flex-shrink-0 mt-0.5" />
                                <span class="text-slate-700">实时数据处理和可视化</span>
                            </li>
                            <li class="flex items-start">
                                <check-circle class="h-6 w-6 text-green-500 mr-2 flex-shrink-0 mt-0.5" />
                                <span class="text-slate-700">可定制的仪表板和报告工具</span>
                            </li>
                            <li class="flex items-start">
                                <check-circle class="h-6 w-6 text-green-500 mr-2 flex-shrink-0 mt-0.5" />
                                <span class="text-slate-700">高级过滤和数据筛选功能</span>
                            </li>
                            <li class="flex items-start">
                                <check-circle class="h-6 w-6 text-green-500 mr-2 flex-shrink-0 mt-0.5" />
                                <span class="text-slate-700">空间数据的在线预览功能</span>
                            </li>
                        </ul>
                        <button
                            class="px-6 py-3 rounded-md bg-primary text-white font-medium hover:bg-primary/90 transition-colors">
                            探索可视化
                        </button>
                    </div>
                </div>
            </div>
        </section>

        <!-- Quick Access Tools -->
        <section class="py-16 bg-white">
            <div class="container mx-auto px-4">
                <div class="text-center mb-12">
                    <h2 class="text-3xl font-bold text-slate-800">快速访问工具</h2>
                    <p class="mt-4 text-slate-600 max-w-2xl mx-auto">
                        直接从仪表板访问我们最常用的工具。
                    </p>
                </div>

                <div class="grid grid-cols-2 md:grid-cols-4 gap-6">
                    <div
                        class="bg-slate-50 p-6 rounded-xl text-center hover:shadow-md transition-shadow cursor-pointer">
                        <div class="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-4">
                            <Map class="h-8 w-8 text-primary" />

                        </div>
                        <h3 class="text-lg font-semibold text-slate-800">地图</h3>
                    </div>

                    <div
                        class="bg-slate-50 p-6 rounded-xl text-center hover:shadow-md transition-shadow cursor-pointer">
                        <div class="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-4">
                            <layers class="h-8 w-8 text-primary" />
                        </div>
                        <h3 class="text-lg font-semibold text-slate-800">图层分析</h3>
                    </div>

                    <div
                        class="bg-slate-50 p-6 rounded-xl text-center hover:shadow-md transition-shadow cursor-pointer">
                        <div class="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-4">
                            <activity class="h-8 w-8 text-primary" />
                        </div>
                        <h3 class="text-lg font-semibold text-slate-800">变化检测</h3>
                    </div>

                    <div
                        class="bg-slate-50 p-6 rounded-xl text-center hover:shadow-md transition-shadow cursor-pointer">
                        <div class="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-4">
                            <download class="h-8 w-8 text-primary" />
                        </div>
                        <h3 class="text-lg font-semibold text-slate-800">数据探索</h3>
                    </div>
                </div>
            </div>
        </section>

        <!-- Recent Analysis Section -->
        <section class="py-16 bg-slate-50">
            <div class="container mx-auto px-4">
                <div class="flex justify-between items-center mb-8">
                    <h2 class="text-2xl font-bold text-slate-800">最新案例</h2>
                    <button class="text-primary font-medium flex items-center">
                        查看更多
                        <chevron-right class="h-5 w-5 ml-1" />
                    </button>
                </div>

                <div class="grid md:grid-cols-3 gap-6">
                    <div class="bg-white rounded-xl shadow-sm overflow-hidden hover:shadow-md transition-shadow">
                        <img src="@/assets/image/home/case1.jpeg" alt="Analysis preview"
                            class="w-full h-48 object-cover" />
                        <div class="p-5">
                            <div class="flex justify-between items-center mb-3">
                                <span class="text-xs font-medium text-slate-500">2023/3/15</span>
                                <span
                                    class="text-xs font-medium px-2 py-1 bg-green-100 text-green-800 rounded-full">已完成</span>
                            </div>
                            <h3 class="text-lg font-semibold text-slate-800 mb-2">城市发展分析</h3>
                            <p class="text-slate-600 text-sm mb-4">
                                利用时间序列卫星图像分析大都市地区的城市扩张。
                            </p>
                            <button class="text-primary text-sm font-medium">查阅细节</button>
                        </div>
                    </div>

                    <div class="bg-white rounded-xl shadow-sm overflow-hidden hover:shadow-md transition-shadow">
                        <img src="@/assets/image/home/case2.webp" alt="Analysis preview"
                            class="w-full h-48 object-cover" />
                        <div class="p-5">
                            <div class="flex justify-between items-center mb-3">
                                <span class="text-xs font-medium text-slate-500">2024/6/17</span>
                                <span
                                    class="text-xs font-medium px-2 py-1 bg-green-100 text-green-800 rounded-full">已完成</span>
                            </div>
                            <h3 class="text-lg font-semibold text-slate-800 mb-2">植被指数图</h3>
                            <p class="text-slate-600 text-sm mb-4">
                                对农业区域进行NDVI分析，以监测作物健康和生长模式。
                            </p>
                            <button class="text-primary text-sm font-medium">查阅细节</button>
                        </div>
                    </div>

                    <div class="bg-white rounded-xl shadow-sm overflow-hidden hover:shadow-md transition-shadow">
                        <img src="@/assets/image/home/case3.png" alt="Analysis preview"
                            class="w-full h-48 object-cover" />
                        <div class="p-5">
                            <div class="flex justify-between items-center mb-3">
                                <span class="text-xs font-medium text-slate-500">2025/3/19</span>
                                <span
                                    class="text-xs font-medium px-2 py-1 bg-blue-100 text-blue-800 rounded-full">进行中</span>
                            </div>
                            <h3 class="text-lg font-semibold text-slate-800 mb-2">海岸侵蚀研究</h3>
                            <p class="text-slate-600 text-sm mb-4">
                                海岸线变化的时间序列分析，以确定侵蚀模式和速率。
                            </p>
                            <button class="text-primary text-sm font-medium">查阅细节</button>
                        </div>
                    </div>
                </div>
            </div>
        </section>

        <!-- CTA Section -->
        <section class="py-16 bg-gradient-to-br from-slate-800 to-slate-900 text-white">
            <div class="container mx-auto px-4 text-center">
                <h2 class="text-3xl font-bold mb-6">准备好从卫星数据中解读最新信息了吗？</h2>
                <p class="text-slate-300 max-w-2xl mx-auto mb-8">
                    加入数以千计的研究人员、组织和政府机构的行列，使用我们的平台分析卫星图像并获得可行的见解。
                </p>
                <div class="flex flex-col sm:flex-row justify-center space-y-4 sm:space-y-0 sm:space-x-4">
                    <button
                        class="px-8 py-3 rounded-md bg-primary text-white font-medium hover:bg-primary/90 transition-colors">
                        开始行动
                    </button>
                    <button
                        class="px-8 py-3 rounded-md border border-slate-600 text-white hover:bg-white/10 transition-colors">
                        预约演示
                    </button>
                </div>
            </div>
        </section>

        <!-- Footer -->
        <footer class="bg-slate-900 text-slate-400 py-12">
            <div class="container mx-auto px-4">
                <div class="grid md:grid-cols-4 gap-8">
                    <div>
                        <div class="flex items-center space-x-2 mb-4">
                            <satellite class="h-8 w-8 text-primary" />
                            <span class="text-xl font-bold text-white">卫星图像分析平台</span>
                        </div>
                        <p class="text-sm">
                            为研究人员、组织和政府机构提供先进的卫星图像分析平台。
                        </p>
                    </div>

                    <div>
                        <h3 class="text-white font-semibold mb-4">平台</h3>
                        <ul class="space-y-2 text-sm">
                            <li><a href="#" class="hover:text-white transition-colors">平台1</a></li>
                            <li><a href="#" class="hover:text-white transition-colors">平台2</a></li>
                            <li><a href="#" class="hover:text-white transition-colors">平台3</a></li>
                            <li><a href="#" class="hover:text-white transition-colors">平台4</a></li>
                        </ul>
                    </div>

                    <div>
                        <h3 class="text-white font-semibold mb-4">资源</h3>
                        <ul class="space-y-2 text-sm">
                            <li><a href="#" class="hover:text-white transition-colors">资源1</a></li>
                            <li><a href="#" class="hover:text-white transition-colors">资源2</a></li>
                            <li><a href="#" class="hover:text-white transition-colors">资源3</a></li>
                            <li><a href="#" class="hover:text-white transition-colors">资源4</a></li>
                        </ul>
                    </div>

                    <div>
                        <h3 class="text-white font-semibold mb-4">我们</h3>
                        <ul class="space-y-2 text-sm">
                            <li><a href="#" class="hover:text-white transition-colors">关于我们</a></li>
                            <li><a href="#" class="hover:text-white transition-colors">联系我们</a></li>
                            <li><a href="#" class="hover:text-white transition-colors">隐私政策</a></li>
                        </ul>
                    </div>
                </div>

                <div
                    class="border-t border-slate-800 mt-12 pt-8 flex flex-col md:flex-row justify-between items-center">
                    <p class="text-sm">© 2025 卫星图像分析平台。 保留所有权利。</p>
                    <div class="flex space-x-4 mt-4 md:mt-0">
                        <a href="#" class="text-slate-400 hover:text-white transition-colors">
                            <!-- <twitter class="h-5 w-5" /> -->
                        </a>
                        <a href="#" class="text-slate-400 hover:text-white transition-colors">
                            <!-- <linkedin class="h-5 w-5" /> -->
                        </a>
                        <a href="#" class="text-slate-400 hover:text-white transition-colors">
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
    SquareChevronRight
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