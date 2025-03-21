import { type CascaderProps } from 'ant-design-vue'
export const districtOption: CascaderProps['options'] = [
    {
        value: 'beijing',
        label: '北京市',
        children: [
            {
                value: 'beijing',
                label: '北京市',
                children: [
                    { value: 'dongcheng', label: '东城区' },
                    { value: 'xicheng', label: '西城区' },
                    { value: 'chaoyang', label: '朝阳区' },
                    { value: 'haidian', label: '海淀区' },
                    { value: 'fengtai', label: '丰台区' },
                    { value: 'shijingshan', label: '石景山区' },
                ],
            },
        ],
    },
    {
        value: 'shanghai',
        label: '上海市',
        children: [
            {
                value: 'shanghai',
                label: '上海市',
                children: [
                    { value: 'pudong', label: '浦东新区' },
                    { value: 'huangpu', label: '黄浦区' },
                    { value: 'jing_an', label: '静安区' },
                    { value: 'xuhui', label: '徐汇区' },
                    { value: 'changning', label: '长宁区' },
                    { value: 'putuo', label: '普陀区' },
                ],
            },
        ],
    },
    {
        value: 'zhejiang',
        label: '浙江省',
        children: [
            {
                value: 'hangzhou',
                label: '杭州市',
                children: [
                    { value: 'xihu', label: '西湖区' },
                    { value: 'shangcheng', label: '上城区' },
                    { value: 'gongshu', label: '拱墅区' },
                    { value: 'jianggan', label: '江干区' },
                    { value: 'binjiang', label: '滨江区' },
                ],
            },
            {
                value: 'ningbo',
                label: '宁波市',
                children: [
                    { value: 'haishu', label: '海曙区' },
                    { value: 'jiangbei', label: '江北区' },
                    { value: 'beilun', label: '北仑区' },
                    { value: 'yinzhou', label: '鄞州区' },
                    { value: 'zhenhai', label: '镇海区' },
                ],
            },
            {
                value: 'wenzhou',
                label: '温州市',
                children: [
                    { value: 'lucheng', label: '鹿城区' },
                    { value: 'longwan', label: '龙湾区' },
                    { value: 'ouhai', label: '瓯海区' },
                ],
            },
        ],
    },
    {
        value: 'jiangsu',
        label: '江苏省',
        children: [
            {
                value: 'nanjing',
                label: '南京市',
                children: [
                    { value: 'xuanwu', label: '玄武区' },
                    { value: 'qinhuai', label: '秦淮区' },
                    { value: 'jianye', label: '建邺区' },
                    { value: 'gulou', label: '鼓楼区' },
                    { value: 'pukou', label: '浦口区' },
                ],
            },
            {
                value: 'suzhou',
                label: '苏州市',
                children: [
                    { value: 'gusu', label: '姑苏区' },
                    { value: 'xiangcheng', label: '相城区' },
                    { value: 'wuzhong', label: '吴中区' },
                    { value: 'huqiu', label: '虎丘区' },
                    { value: 'wujiang', label: '吴江区' },
                ],
            },
            {
                value: 'wuxi',
                label: '无锡市',
                children: [
                    { value: 'liangxi', label: '梁溪区' },
                    { value: 'xinwu', label: '新吴区' },
                    { value: 'binhu', label: '滨湖区' },
                    { value: 'huishan', label: '惠山区' },
                ],
            },
        ],
    },
    {
        value: 'guangdong',
        label: '广东省',
        children: [
            {
                value: 'guangzhou',
                label: '广州市',
                children: [
                    { value: 'tianhe', label: '天河区' },
                    { value: 'yuexiu', label: '越秀区' },
                    { value: 'haizhu', label: '海珠区' },
                    { value: 'liwan', label: '荔湾区' },
                    { value: 'panyu', label: '番禺区' },
                ],
            },
            {
                value: 'shenzhen',
                label: '深圳市',
                children: [
                    { value: 'futian', label: '福田区' },
                    { value: 'luohu', label: '罗湖区' },
                    { value: 'nanshan', label: '南山区' },
                    { value: 'baoan', label: '宝安区' },
                    { value: 'longgang', label: '龙岗区' },
                ],
            },
            {
                value: 'dongguan',
                label: '东莞市',
                children: [
                    { value: 'dongcheng', label: '东城街道' },
                    { value: 'nancheng', label: '南城街道' },
                    { value: 'wanjiang', label: '万江街道' },
                    { value: 'guancheng', label: '莞城街道' },
                ],
            },
        ],
    },
    // ... 其他省份
]
