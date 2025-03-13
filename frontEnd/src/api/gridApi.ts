import client from './clientHttp'
const MOCK = true


export const httpDownloadGrids = (gridIDList: Array<string>): Promise<Blob> => {
    if (MOCK) {



        if (gridIDList.length === 1)
            return client.get(`/tif/${gridIDList[0]}`, {
                responseType: 'blob',
            })

        else if (gridIDList.length >= 1)
            return client.post('/merge', {
                'id_list': gridIDList
            }, {
                responseType: 'blob'
            })

        else {
            throw new Error('gridIDList is empty')
        }


    } else {
        return client.get('/images')
    }
}