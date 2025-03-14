

export function blobDownload(blob: Blob, name: string) {
    if (!blob) return
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.style.display = 'none'
    link.href = url
    link.download = name
    link.click()

    URL.revokeObjectURL(url)
}