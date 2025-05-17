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

        if (rPng.width !== gPng.width || rPng.width !== bPng.width ||
            rPng.height !== gPng.height || rPng.height !== bPng.height) {
            throw new Error("RGB通道图像的尺寸不一致");
        }

        const width = rPng.width;
        const height = rPng.height;
        const totalPixels = width * height;
        const rgbaData = new Uint8Array(totalPixels * 4); // 每个像素 4 个字节 (R, G, B, A)

        const rPixels = new Uint8Array(UPNG.toRGBA8(rPng)[0]);
        const gPixels = new Uint8Array(UPNG.toRGBA8(gPng)[0]);
        const bPixels = new Uint8Array(UPNG.toRGBA8(bPng)[0]);

        for (let i = 0; i < totalPixels; i++) {
            const index = i * 4;
            rgbaData[index] = rPixels[index];         // R
            rgbaData[index + 1] = gPixels[index];     // G
            rgbaData[index + 2] = bPixels[index];     // B
            rgbaData[index + 3] = rPixels[index + 3]; // A - 使用第一个 PNG 的 Alpha
        }

        const TRUE_COLOR_WITH_ALPHA = 6;
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

    console.time('start mergeColor')

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

self.onmessage = async (event) => {
    const { urls } = event.data;
    if (urls && urls.length === 3) { // 假设每次处理三个 URL 来合成 RGB
        const [rUrl, gUrl, bUrl] = urls;
        const rBuffer = await fetchImgArrayBuffer(rUrl);
        const gBuffer = await fetchImgArrayBuffer(gUrl);
        const bBuffer = await fetchImgArrayBuffer(bUrl);
        const resultBuffer = combineRGBBuffers(rBuffer, gBuffer, bBuffer);
        self.postMessage({
            id: event.data.id,
            combinedPngBuffer: resultBuffer
        });
    } else if (urls && urls.length > 3) {
        console.warn("Worker 接收到超过三个 URL，需要调整处理逻辑。");
    } else {
        console.error("Worker 接收到无效的消息。");
    }
}