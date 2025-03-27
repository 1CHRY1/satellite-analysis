<template>
    <div class="min-h-screen bg-slate-50">
        <!-- Header Navigation -->
        <header class="bg-white shadow-sm">
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
        </header>

        <!-- Hero Section -->
        <section class="py-16 md:py-24 bg-gradient-to-br from-slate-800 to-slate-900 text-white">
            <div class="container mx-auto px-4">
                <div class="grid md:grid-cols-2 gap-12 items-center">
                    <div class="space-y-6">
                        <h1 class="text-4xl md:text-5xl font-bold leading-tight">
                            Advanced Satellite Imagery Analysis Platform
                        </h1>
                        <p class="text-lg text-slate-300">
                            Unlock powerful insights from satellite data with our comprehensive analysis tools and
                            visualization capabilities.
                        </p>
                        <div class="flex flex-col sm:flex-row space-y-3 sm:space-y-0 sm:space-x-4 pt-4">
                            <button
                                class="px-6 py-3 rounded-md bg-primary text-white font-medium hover:bg-primary/90 transition-colors">
                                Explore Platform
                            </button>
                            <button
                                class="px-6 py-3 rounded-md border border-slate-600 text-white hover:bg-white/10 transition-colors">
                                Watch Demo
                            </button>
                        </div>
                    </div>
                    <div class="relative">
                        <div class="absolute -top-6 -left-6 w-24 h-24 bg-primary/20 rounded-full blur-xl"></div>
                        <div class="absolute -bottom-10 -right-10 w-32 h-32 bg-blue-500/20 rounded-full blur-xl"></div>
                        <img src="@/assets/placeholder.svg" alt="Satellite imagery visualization"
                            class="w-full h-auto rounded-lg shadow-2xl relative z-10" />
                    </div>
                </div>
            </div>
        </section>

        <!-- Features Section -->
        <section class="py-16 bg-white">
            <div class="container mx-auto px-4">
                <div class="text-center mb-16">
                    <h2 class="text-3xl font-bold text-slate-800">Powerful Analysis Capabilities</h2>
                    <p class="mt-4 text-slate-600 max-w-2xl mx-auto">
                        Our platform provides comprehensive tools for satellite image processing, analysis, and
                        visualization.
                    </p>
                </div>

                <div class="grid md:grid-cols-3 gap-8">
                    <div class="bg-slate-50 p-6 rounded-xl shadow-sm hover:shadow-md transition-shadow">
                        <div class="w-12 h-12 bg-primary/10 rounded-lg flex items-center justify-center mb-4">
                            <image class="h-6 w-6 text-primary" />
                        </div>
                        <h3 class="text-xl font-semibold text-slate-800 mb-2">Image Retrieval</h3>
                        <p class="text-slate-600">
                            Access and retrieve satellite imagery from multiple sources with advanced filtering options.
                        </p>
                    </div>

                    <div class="bg-slate-50 p-6 rounded-xl shadow-sm hover:shadow-md transition-shadow">
                        <div class="w-12 h-12 bg-primary/10 rounded-lg flex items-center justify-center mb-4">
                            <bar-chart-2 class="h-6 w-6 text-primary" />
                        </div>
                        <h3 class="text-xl font-semibold text-slate-800 mb-2">Data Visualization</h3>
                        <p class="text-slate-600">
                            Transform complex satellite data into clear, actionable visualizations and insights.
                        </p>
                    </div>

                    <div class="bg-slate-50 p-6 rounded-xl shadow-sm hover:shadow-md transition-shadow">
                        <div class="w-12 h-12 bg-primary/10 rounded-lg flex items-center justify-center mb-4">
                            <code class="h-6 w-6 text-primary" />
                        </div>
                        <h3 class="text-xl font-semibold text-slate-800 mb-2">Programming Analysis</h3>
                        <p class="text-slate-600">
                            Leverage powerful programming tools to perform custom analysis on satellite imagery.
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
                            <img src="@/assets/placeholder.svg" alt="Data visualization dashboard"
                                class="w-full h-auto rounded-lg" />
                        </div>
                    </div>
                    <div class="space-y-6 order-1 md:order-2">
                        <h2 class="text-3xl font-bold text-slate-800">Interactive Data Visualization</h2>
                        <p class="text-slate-600">
                            Our platform provides powerful visualization tools that transform complex satellite data
                            into clear, actionable insights.
                        </p>
                        <ul class="space-y-3">
                            <li class="flex items-start">
                                <check-circle class="h-6 w-6 text-green-500 mr-2 flex-shrink-0 mt-0.5" />
                                <span class="text-slate-700">Real-time data processing and visualization</span>
                            </li>
                            <li class="flex items-start">
                                <check-circle class="h-6 w-6 text-green-500 mr-2 flex-shrink-0 mt-0.5" />
                                <span class="text-slate-700">Customizable dashboards and reporting tools</span>
                            </li>
                            <li class="flex items-start">
                                <check-circle class="h-6 w-6 text-green-500 mr-2 flex-shrink-0 mt-0.5" />
                                <span class="text-slate-700">Advanced filtering and comparison capabilities</span>
                            </li>
                            <li class="flex items-start">
                                <check-circle class="h-6 w-6 text-green-500 mr-2 flex-shrink-0 mt-0.5" />
                                <span class="text-slate-700">Export options for presentations and reports</span>
                            </li>
                        </ul>
                        <button
                            class="px-6 py-3 rounded-md bg-primary text-white font-medium hover:bg-primary/90 transition-colors">
                            Explore Visualizations
                        </button>
                    </div>
                </div>
            </div>
        </section>

        <!-- Quick Access Tools -->
        <section class="py-16 bg-white">
            <div class="container mx-auto px-4">
                <div class="text-center mb-12">
                    <h2 class="text-3xl font-bold text-slate-800">Quick Access Tools</h2>
                    <p class="mt-4 text-slate-600 max-w-2xl mx-auto">
                        Access our most popular analysis tools directly from the dashboard.
                    </p>
                </div>

                <div class="grid grid-cols-2 md:grid-cols-4 gap-6">
                    <div
                        class="bg-slate-50 p-6 rounded-xl text-center hover:shadow-md transition-shadow cursor-pointer">
                        <div class="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-4">
                            <map class="h-8 w-8 text-primary" />
                        </div>
                        <h3 class="text-lg font-semibold text-slate-800">Mapping</h3>
                    </div>

                    <div
                        class="bg-slate-50 p-6 rounded-xl text-center hover:shadow-md transition-shadow cursor-pointer">
                        <div class="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-4">
                            <layers class="h-8 w-8 text-primary" />
                        </div>
                        <h3 class="text-lg font-semibold text-slate-800">Layer Analysis</h3>
                    </div>

                    <div
                        class="bg-slate-50 p-6 rounded-xl text-center hover:shadow-md transition-shadow cursor-pointer">
                        <div class="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-4">
                            <activity class="h-8 w-8 text-primary" />
                        </div>
                        <h3 class="text-lg font-semibold text-slate-800">Change Detection</h3>
                    </div>

                    <div
                        class="bg-slate-50 p-6 rounded-xl text-center hover:shadow-md transition-shadow cursor-pointer">
                        <div class="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-4">
                            <download class="h-8 w-8 text-primary" />
                        </div>
                        <h3 class="text-lg font-semibold text-slate-800">Data Export</h3>
                    </div>
                </div>
            </div>
        </section>

        <!-- Recent Analysis Section -->
        <section class="py-16 bg-slate-50">
            <div class="container mx-auto px-4">
                <div class="flex justify-between items-center mb-8">
                    <h2 class="text-2xl font-bold text-slate-800">Recent Analysis</h2>
                    <button class="text-primary font-medium flex items-center">
                        View All
                        <chevron-right class="h-5 w-5 ml-1" />
                    </button>
                </div>

                <div class="grid md:grid-cols-3 gap-6">
                    <div class="bg-white rounded-xl shadow-sm overflow-hidden hover:shadow-md transition-shadow">
                        <img src="@/assets/placeholder.svg" alt="Analysis preview" class="w-full h-48 object-cover" />
                        <div class="p-5">
                            <div class="flex justify-between items-center mb-3">
                                <span class="text-xs font-medium text-slate-500">March 15, 2023</span>
                                <span
                                    class="text-xs font-medium px-2 py-1 bg-green-100 text-green-800 rounded-full">Completed</span>
                            </div>
                            <h3 class="text-lg font-semibold text-slate-800 mb-2">Urban Development Analysis</h3>
                            <p class="text-slate-600 text-sm mb-4">
                                Analysis of urban expansion in metropolitan areas using time-series satellite imagery.
                            </p>
                            <button class="text-primary text-sm font-medium">View Details</button>
                        </div>
                    </div>

                    <div class="bg-white rounded-xl shadow-sm overflow-hidden hover:shadow-md transition-shadow">
                        <img src="@/assets/placeholder.svg" alt="Analysis preview" class="w-full h-48 object-cover" />
                        <div class="p-5">
                            <div class="flex justify-between items-center mb-3">
                                <span class="text-xs font-medium text-slate-500">March 10, 2023</span>
                                <span
                                    class="text-xs font-medium px-2 py-1 bg-green-100 text-green-800 rounded-full">Completed</span>
                            </div>
                            <h3 class="text-lg font-semibold text-slate-800 mb-2">Vegetation Index Mapping</h3>
                            <p class="text-slate-600 text-sm mb-4">
                                NDVI analysis of agricultural regions to monitor crop health and growth patterns.
                            </p>
                            <button class="text-primary text-sm font-medium">View Details</button>
                        </div>
                    </div>

                    <div class="bg-white rounded-xl shadow-sm overflow-hidden hover:shadow-md transition-shadow">
                        <img src="@/assets/placeholder.svg" alt="Analysis preview" class="w-full h-48 object-cover" />
                        <div class="p-5">
                            <div class="flex justify-between items-center mb-3">
                                <span class="text-xs font-medium text-slate-500">March 5, 2023</span>
                                <span class="text-xs font-medium px-2 py-1 bg-blue-100 text-blue-800 rounded-full">In
                                    Progress</span>
                            </div>
                            <h3 class="text-lg font-semibold text-slate-800 mb-2">Coastal Erosion Study</h3>
                            <p class="text-slate-600 text-sm mb-4">
                                Time-series analysis of coastline changes to identify erosion patterns and rates.
                            </p>
                            <button class="text-primary text-sm font-medium">View Details</button>
                        </div>
                    </div>
                </div>
            </div>
        </section>

        <!-- CTA Section -->
        <section class="py-16 bg-gradient-to-br from-slate-800 to-slate-900 text-white">
            <div class="container mx-auto px-4 text-center">
                <h2 class="text-3xl font-bold mb-6">Ready to unlock insights from satellite data?</h2>
                <p class="text-slate-300 max-w-2xl mx-auto mb-8">
                    Join thousands of researchers, organizations, and government agencies using our platform to analyze
                    satellite imagery and derive actionable insights.
                </p>
                <div class="flex flex-col sm:flex-row justify-center space-y-4 sm:space-y-0 sm:space-x-4">
                    <button
                        class="px-8 py-3 rounded-md bg-primary text-white font-medium hover:bg-primary/90 transition-colors">
                        Get Started for Free
                    </button>
                    <button
                        class="px-8 py-3 rounded-md border border-slate-600 text-white hover:bg-white/10 transition-colors">
                        Schedule a Demo
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
                            <span class="text-xl font-bold text-white">SatelliteIQ</span>
                        </div>
                        <p class="text-sm">
                            Advanced satellite imagery analysis platform for researchers, organizations, and government
                            agencies.
                        </p>
                    </div>

                    <div>
                        <h3 class="text-white font-semibold mb-4">Platform</h3>
                        <ul class="space-y-2 text-sm">
                            <li><a href="#" class="hover:text-white transition-colors">Dashboard</a></li>
                            <li><a href="#" class="hover:text-white transition-colors">Image Library</a></li>
                            <li><a href="#" class="hover:text-white transition-colors">Analysis Tools</a></li>
                            <li><a href="#" class="hover:text-white transition-colors">Data Export</a></li>
                        </ul>
                    </div>

                    <div>
                        <h3 class="text-white font-semibold mb-4">Resources</h3>
                        <ul class="space-y-2 text-sm">
                            <li><a href="#" class="hover:text-white transition-colors">Documentation</a></li>
                            <li><a href="#" class="hover:text-white transition-colors">API Reference</a></li>
                            <li><a href="#" class="hover:text-white transition-colors">Tutorials</a></li>
                            <li><a href="#" class="hover:text-white transition-colors">Case Studies</a></li>
                        </ul>
                    </div>

                    <div>
                        <h3 class="text-white font-semibold mb-4">Company</h3>
                        <ul class="space-y-2 text-sm">
                            <li><a href="#" class="hover:text-white transition-colors">About Us</a></li>
                            <li><a href="#" class="hover:text-white transition-colors">Contact</a></li>
                            <li><a href="#" class="hover:text-white transition-colors">Careers</a></li>
                            <li><a href="#" class="hover:text-white transition-colors">Privacy Policy</a></li>
                        </ul>
                    </div>
                </div>

                <div
                    class="border-t border-slate-800 mt-12 pt-8 flex flex-col md:flex-row justify-between items-center">
                    <p class="text-sm">© 2023 SatelliteIQ. All rights reserved.</p>
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
    MenuIcon,
    BarChart2,
    CheckCircle,
    Layers,
    Activity,
    Download,
    ChevronRight,
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