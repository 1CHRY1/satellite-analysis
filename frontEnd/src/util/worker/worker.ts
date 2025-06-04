import UPNG from '@pdf-lib/upng';
interface WorkerPoolTask {
    id: number | string;
    urls: [string, string, string]; // 假设每次合成需要三个 URL
}
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

// async function mergeColor(rtif: string, gtif: string, btif: string) {

//     console.time('start mergeColor')

//     const titler = 'http://localhost:8000/preview'
//     const params = new URLSearchParams()
//     params.append('format', 'png')
//     params.append('url', rtif)
//     params.append('max_size', '1024')

//     const rurl = titler + '?' + params.toString()
//     const rBuffer = fetchImgArrayBuffer(rurl)

//     params.set('url', gtif)
//     const gurl = titler + '?' + params.toString()
//     const gBuffer = fetchImgArrayBuffer(gurl)

//     params.set('url', btif)
//     const burl = titler + '?' + params.toString()
//     const bBuffer = fetchImgArrayBuffer(burl)

//     const resultBuffer = combineRGBBuffers(await rBuffer, await gBuffer, await bBuffer)

//     console.timeEnd('mergeColor')
// }

self.onmessage = async (event: any) => {
    const { id, urls } = event.data as WorkerPoolTask;
    if (urls && urls.length === 3) { // 假设每次处理三个 URL 来合成 RGB
        const [rUrl, gUrl, bUrl] = urls;
        const rBuffer = await fetchImgArrayBuffer(rUrl);
        const gBuffer = await fetchImgArrayBuffer(gUrl);
        const bBuffer = await fetchImgArrayBuffer(bUrl);
        const resultBuffer = combineRGBBuffers(rBuffer, gBuffer, bBuffer);
        self.postMessage({
            id: id,
            combinedPngBuffer: resultBuffer
        });
    } else if (urls && urls.length > 3) {
        console.warn("Worker 接收到超过三个 URL，需要调整处理逻辑。");
    } else {
        console.error("Worker 接收到无效的消息。");
    }
}