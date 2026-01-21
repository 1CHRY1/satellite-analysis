<template>
    <div class="mission-control-container">
      <!-- Header -->
      <header class="control-header">
        <div class="header-decoration"></div>
        
        <div class="header-content">
          <!-- <div class="console-title">
            <div class="status-indicator">
              <span class="pulse-dot"></span>
            </div>
            <span class="title-text">任务控制台</span>
          </div> -->
          
          <nav class="nav-links">
            <router-link
              to="/user"
              class="nav-item"
              :class="{ 'active': route.name === '概览' }"
            >
              <div class="nav-icon-wrapper">
                <font-awesome-icon :icon="['far', 'eye']" class="nav-icon" />
              </div>
              <span class="nav-text">{{ t("userpage.userFunction.overview") }}</span>
              <div class="nav-glow"></div>
              <div class="active-indicator"></div>
            </router-link>
            
            <div class="nav-divider">
              <span class="divider-dot"></span>
            </div>
            
            <router-link
              to="/user/projects"
              class="nav-item"
              :class="{ 'active': route.name === '我的项目' }"
            >
              <div class="nav-icon-wrapper">
                <font-awesome-icon :icon="['fas', 'bars-progress']" class="nav-icon" />
              </div>
              <span class="nav-text">{{ t("userpage.userFunction.projects") }}</span>
              <div class="nav-glow"></div>
              <div class="active-indicator"></div>
            </router-link>
            
            <div class="nav-divider">
              <span class="divider-dot"></span>
            </div>
            
            <router-link
              to="/user/data"
              class="nav-item"
              :class="{ 'active': route.name === '我的数据' }"
            >
              <div class="nav-icon-wrapper">
                <font-awesome-icon :icon="['fas', 'database']" class="nav-icon" />
              </div>
              <span class="nav-text">{{ t("userpage.userFunction.data") }}</span>
              <div class="nav-glow"></div>
              <div class="active-indicator"></div>
            </router-link>
          </nav>
          
          <!-- <div class="header-status">
            <div class="signal-bars">
              <span class="bar bar-1"></span>
              <span class="bar bar-2"></span>
              <span class="bar bar-3"></span>
              <span class="bar bar-4"></span>
            </div>
            <span class="status-text">ONLINE</span>
          </div> -->
        </div>
      </header>
  
      <main class="control-main">
        <router-view :key="route.fullPath" />
      </main>
    </div>
  </template>
  
  <script setup lang="ts">
  import { useRoute } from "vue-router";
  import { useI18n } from "vue-i18n";
  
  const route = useRoute();
  const { t } = useI18n();
  </script>
  
  <style scoped>
  /* Mission Control Container */
  .mission-control-container {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
  }

  /* Control Header */
  .control-header {
    position: relative;
    background: rgba(15, 23, 42, 0.8);
    backdrop-filter: blur(20px);
    border-bottom: 1px solid rgba(14, 165, 233, 0.3);
    padding: 0;
    overflow: hidden;
  }

  .header-decoration {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 2px;
    background: linear-gradient(90deg, 
      transparent, 
      rgba(14, 165, 233, 0.8), 
      rgba(16, 185, 129, 0.8), 
      transparent
    );
  }

  .header-content {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 1rem 1.5rem;
    gap: 2rem;
  }

  /* Console Title */
  .console-title {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    flex-shrink: 0;
  }

  .status-indicator {
    position: relative;
    width: 12px;
    height: 12px;
  }

  .pulse-dot {
    position: absolute;
    width: 100%;
    height: 100%;
    background: #10B981;
    border-radius: 50%;
    box-shadow: 0 0 10px rgba(16, 185, 129, 0.6);
    animation: pulse 2s ease-in-out infinite;
  }

  .pulse-dot::before {
    content: '';
    position: absolute;
    inset: -4px;
    border: 2px solid rgba(16, 185, 129, 0.4);
    border-radius: 50%;
    animation: pulse-ring 2s ease-in-out infinite;
  }

  @keyframes pulse {
    0%, 100% { opacity: 1; transform: scale(1); }
    50% { opacity: 0.7; transform: scale(0.9); }
  }

  @keyframes pulse-ring {
    0%, 100% { transform: scale(1); opacity: 1; }
    50% { transform: scale(1.5); opacity: 0; }
  }

  .title-text {
    font-size: 1rem;
    font-weight: 700;
    color: #CBD5E1;
    text-transform: uppercase;
    letter-spacing: 2px;
    text-shadow: 0 0 10px rgba(14, 165, 233, 0.3);
  }

  /* Navigation Links */
  .nav-links {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    flex: 1;
    justify-content: center;
  }

  .nav-item {
    position: relative;
    display: flex;
    align-items: center;
    gap: 0.75rem;
    padding: 0.75rem 1.5rem;
    background: rgba(30, 41, 59, 0.5);
    border: 1px solid rgba(14, 165, 233, 0.2);
    border-radius: 12px;
    color: #94A3B8;
    text-decoration: none;
    font-weight: 600;
    font-size: 0.95rem;
    transition: all 0.3s ease;
    overflow: hidden;
  }

  .nav-item:hover {
    background: rgba(14, 165, 233, 0.15);
    border-color: rgba(14, 165, 233, 0.4);
    color: #E2E8F0;
    transform: translateY(-2px);
    box-shadow: 0 8px 25px rgba(14, 165, 233, 0.2);
  }

  .nav-item.active {
    background: rgba(14, 165, 233, 0.2);
    border-color: rgba(14, 165, 233, 0.6);
    color: #F8FAFC;
    box-shadow: 
      0 0 20px rgba(14, 165, 233, 0.3),
      inset 0 1px 0 rgba(255, 255, 255, 0.1);
  }

  .nav-icon-wrapper {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 28px;
    background: rgba(14, 165, 233, 0.2);
    border-radius: 8px;
    transition: all 0.3s ease;
  }

  .nav-item:hover .nav-icon-wrapper {
    background: rgba(14, 165, 233, 0.4);
    transform: rotate(5deg);
  }

  .nav-item.active .nav-icon-wrapper {
    background: linear-gradient(135deg, #0EA5E9, #0284C7);
    box-shadow: 0 0 15px rgba(14, 165, 233, 0.5);
  }

  .nav-icon {
    font-size: 0.9rem;
    color: inherit;
    transition: all 0.3s ease;
  }

  .nav-item.active .nav-icon {
    color: white;
  }

  .nav-text {
    position: relative;
    z-index: 1;
  }

  .nav-glow {
    position: absolute;
    top: 50%;
    left: 50%;
    width: 0;
    height: 0;
    background: radial-gradient(circle, rgba(14, 165, 233, 0.4) 0%, transparent 70%);
    border-radius: 50%;
    transform: translate(-50%, -50%);
    transition: all 0.4s ease;
    pointer-events: none;
  }

  .nav-item:hover .nav-glow {
    width: 200%;
    height: 200%;
  }

  .active-indicator {
    position: absolute;
    bottom: 0;
    left: 50%;
    width: 0;
    height: 3px;
    background: linear-gradient(90deg, #0EA5E9, #10B981);
    border-radius: 3px 3px 0 0;
    transform: translateX(-50%);
    transition: all 0.3s ease;
    box-shadow: 0 0 10px rgba(14, 165, 233, 0.5);
  }

  .nav-item.active .active-indicator {
    width: 60%;
  }

  /* Navigation Divider */
  .nav-divider {
    display: flex;
    align-items: center;
    padding: 0 0.25rem;
  }

  .divider-dot {
    width: 4px;
    height: 4px;
    background: rgba(14, 165, 233, 0.4);
    border-radius: 50%;
    box-shadow: 0 0 6px rgba(14, 165, 233, 0.3);
  }

  /* Header Status */
  .header-status {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    flex-shrink: 0;
    padding: 0.5rem 1rem;
    background: rgba(16, 185, 129, 0.1);
    border: 1px solid rgba(16, 185, 129, 0.3);
    border-radius: 20px;
  }

  .signal-bars {
    display: flex;
    align-items: flex-end;
    gap: 2px;
    height: 16px;
  }

  .bar {
    width: 3px;
    background: #10B981;
    border-radius: 1px;
    animation: signal 1.5s ease-in-out infinite;
  }

  .bar-1 { height: 4px; animation-delay: 0s; }
  .bar-2 { height: 8px; animation-delay: 0.2s; }
  .bar-3 { height: 12px; animation-delay: 0.4s; }
  .bar-4 { height: 16px; animation-delay: 0.6s; }

  @keyframes signal {
    0%, 100% { opacity: 0.4; }
    50% { opacity: 1; }
  }

  .status-text {
    font-size: 0.75rem;
    font-weight: 700;
    color: #10B981;
    letter-spacing: 1px;
    text-shadow: 0 0 10px rgba(16, 185, 129, 0.5);
  }

  /* Control Main */
  .control-main {
    flex: 1;
    overflow: auto;
  }

  /* Responsive Design */
  @media (max-width: 900px) {
    .header-content {
      flex-wrap: wrap;
      gap: 1rem;
    }

    .console-title {
      order: 1;
    }

    .header-status {
      order: 2;
    }

    .nav-links {
      order: 3;
      width: 100%;
      justify-content: center;
    }
  }

  @media (max-width: 600px) {
    .nav-item {
      padding: 0.6rem 1rem;
      font-size: 0.85rem;
    }

    .nav-text {
      display: none;
    }

    .nav-icon-wrapper {
      width: 32px;
      height: 32px;
    }

    .nav-icon {
      font-size: 1rem;
    }

    .title-text {
      font-size: 0.85rem;
      letter-spacing: 1px;
    }

    .header-status {
      padding: 0.4rem 0.75rem;
    }

    .status-text {
      font-size: 0.65rem;
    }
  }
  </style>