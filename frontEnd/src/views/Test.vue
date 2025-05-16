<template>
    <!-- <div class="absolute top-0 left-0 h-screen w-screen">
        <div class="h-[92vh]" id="map"></div>
      
    </div> -->
    <div></div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { mapManager } from '@/util/map/mapManager'
import UPNG from '@pdf-lib/upng';

async function fetchImgArrayBuffer(url: string) {
    const res = await fetch(url)
    const buffer = await res.arrayBuffer()
    return buffer
}

function combineRGBBuffers(rBuffer: ArrayBuffer, gBuffer: ArrayBuffer, bBuffer: ArrayBuffer): ArrayBuffer | null {
    try {
        const rPng = UPNG.decode(rBuffer);
        const gPng = UPNG.decode(gBuffer);
        const bPng = UPNG.decode(bBuffer);
        console.log(rPng)

        if (rPng.width !== gPng.width || rPng.width !== bPng.width ||
            rPng.height !== gPng.height || rPng.height !== bPng.height) {
            throw new Error("RGB通道图像的尺寸不一致");
        }

        const width = rPng.width;
        const height = rPng.height;
        const totalPixels = width * height;
        const rgbaData = new Uint8Array(totalPixels * 4); // 每个像素 4 个字节 (R, G, B, A)


        const rPixels = new Uint8Array(UPNG.toRGBA8(rPng)[0])
        const gPixels = new Uint8Array(UPNG.toRGBA8(gPng)[0])
        const bPixels = new Uint8Array(UPNG.toRGBA8(bPng)[0])

        for (let i = 0; i < totalPixels; i++) {
            const index = i * 4;
            rgbaData[index] = rPixels[i * 4];     // R
            rgbaData[index + 1] = gPixels[i * 4]; // G
            rgbaData[index + 2] = bPixels[i * 4]; // B
            rgbaData[index + 3] = 255;
        }

        const TRUE_COLOR_WITH_ALPHA = 6
        const combinedPngBuffer = UPNG.encode([rgbaData], width, height, TRUE_COLOR_WITH_ALPHA);

        return combinedPngBuffer; // 返回合成后的 PNG 图像的 ArrayBuffer

    } catch (error) {
        console.error("合成 RGB 图像时出错:", error);
        return null;
    }
}


async function visualizeCombinedBuffer(combinedPngBuffer: ArrayBuffer) {
    if (combinedPngBuffer) {
        // 将 ArrayBuffer 转换为 Blob 对象 (MIME 类型为 image/png)
        const blob = new Blob([combinedPngBuffer], { type: 'image/png' });

        // 使用 URL.createObjectURL() 创建一个临时的 URL，指向该 Blob 对象
        const imageUrl = URL.createObjectURL(blob);

        // 创建一个 <img> 标签
        const imgElement = document.createElement('img');

        // 将 <img> 标签的 src 属性设置为刚才创建的 URL
        imgElement.src = imageUrl;

        // 将 <img> 标签添加到文档的 body 中，使其可见
        document.body.appendChild(imgElement);

        // 注意：当你不再需要这个图像 URL 时，应该使用 URL.revokeObjectURL() 来释放资源
        // 但对于简单的一次性可视化，通常浏览器会在页面卸载时自动释放。
        // 如果你需要在长时间运行的应用中频繁创建和销毁 URL 对象，请记得手动释放。
        // URL.revokeObjectURL(imageUrl);
    } else {
        console.error("combinedPngBuffer 为空，无法可视化。");
    }
}
async function mergeColor(rtif: string, gtif: string, btif: string) {

    console.time('mergeColor')

    const titler = 'http://localhost:8000/preview'
    const params = new URLSearchParams()
    params.append('format', 'png')
    params.append('url', rtif)
    params.append('max_size', '1024')

    const rurl = titler + '?' + params.toString()
    const rBuffer = fetchImgArrayBuffer(rurl)

    params.set('url', gtif)
    const gurl = titler + '?' + params.toString()
    const gBuffer = fetchImgArrayBuffer(gurl)

    params.set('url', btif)
    const burl = titler + '?' + params.toString()
    const bBuffer = fetchImgArrayBuffer(burl)

    const resultBuffer = combineRGBBuffers(await rBuffer, await gBuffer, await bBuffer)

    visualizeCombinedBuffer(resultBuffer!)

    console.timeEnd('mergeColor')
}


onMounted(() => {


    const list = [
        'D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B4.TIF',
        "D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B3.TIF",
        "D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B2.TIF",
    ] as [string, string, string]


    mergeColor(...list)



    // mapManager.init('map', 'vector', 'mercator').then((map) => {
    // map.zoomTo(10)

    // map.showTileBoundaries = true
    ///// rio-tiler-test /////////////////////////
    // if (true) {
    //     map.addSource('src', {
    //         type: 'raster',
    //         tiles: [
    //             // /tiles/WebMercatorQuad/9/428/208?scale=1&format=tif&url=http%3A%2F%2F223.2.43.228%3A30900%2Ftest-images%2Flandset8_test%252Flandset8_L2SP_test%252Ftif%252FLC08_L2SP_118038_20241201_20241203_02_T1%252FLC08_L2SP_118038_20241201_20241203_02_T1_SR_B6.TIF&bidx=1&unscale=false&resampling=nearest&reproject=nearest&return_mask=true
    //             // no cog
    //             // "http://223.2.32.242:8079/{z}/{x}/{y}.png?object=/test-images/landsat/landset7/tif/LE07_L1TP_122039_20210212_20210212_01_RT/LE07_L1TP_122039_20210212_20210212_01_RT_B1.TIF"
    //             'http://localhost:8000/tiles/WebMercatorQuad/{z}/{x}/{y}?scale=1&format=png&url=http://223.2.43.228:30900/test-images/qa/LC08_L2SP_120035_20250116_20250127_02_T1_SR_B1.TIF&bidx=1&unscale=false&resampling=nearest&reproject=nearest&return_mask=true',
    //             // "http://223.2.32.242:8079/{z}/{x}/{y}.png?object=/test-images/landset8_test/landset8_L2SP_test/tif/LC08_L2SP_118038_20241217_20241227_02_T1/LC08_L2SP_118038_20241217_20241227_02_T1_SR_B4.TIF"
    //         ],
    //     })
    //     map.addLayer({
    //         id: 'layer',
    //         type: 'raster',
    //         source: 'src',
    //         minzoom: 0,
    //     })
    // }
    // })
})
</script>
