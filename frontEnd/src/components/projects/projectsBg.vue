<template>
    <div class=" ">
        <div class="layer1"></div>
        <div class="layer2"></div>
        <div class="layer3"></div>
    </div>
</template>

<script setup lang="ts"></script>

<style scoped lang="scss">
@use 'sass:math';
@use 'sass:string';

html {
    height: 100%;
    background: radial-gradient(ellipse at bottom, #1b2735 0%, #090a0f 100%);
    overflow: hidden;
}

.title {
    position: absolute;
    top: 50%;
    left: 0;
    right: 0;
    color: #fff;
    text-align: center;
    font-family: 'lato', sans-serif;
    font-weight: 300;
    font-size: 50px;
    letter-spacing: 10px;
    margin-top: -60px;
    padding-left: 10px;
    background: linear-gradient(white, #38495a);
    background-clip: text;
    -webkit-background-clip: text;

    color: transparent;
}

@function createShadow($n) {
    $shadow: '#{math.random(100)}vw #{math.random(100)}vh #fff';

    @for $i from 2 through $n {
        $shadow: '#{$shadow}, #{math.random(100)}vw #{math.random(100)}vh #fff';
    }

    @return string.unquote($shadow);
}

$count: 1000;
$duration: 400s;

@for $i from 1 through 3 {
    $count: math.floor(calc($count / 2));
    $duration: math.floor(calc($duration / 2));

    // @debug 'count: #{$count}';
    // @debug 'duration: #{$duration}';

    .layer#{$i} {
        $size: #{$i}px;
        position: fixed;
        width: $size;
        height: $size;
        border-radius: 50%;
        left: 0;
        top: 0;
        box-shadow: createShadow($count);
        animation: moveUp $duration linear infinite;

        &::after {
            content: '';
            position: fixed;
            left: 0;
            top: 100vh;
            width: inherit;
            height: inherit;
            border-radius: inherit;
            box-shadow: inherit;
        }
    }
}

@keyframes moveUp {
    100% {
        transform: translateY(-100vh);
    }
}
</style>
