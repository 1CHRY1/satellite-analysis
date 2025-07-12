export default{
    nav: {
        title: "多源遥感应用支撑云平台",
        home: "首页",
        models: "模型中心",
        data: "数据中心",
        tools: "工具发布",
        about: "关于我们",
    
        button: {
            login: "登录",
            signup: "注册",
            logout:"退出登录"
        },
        disabled_message:"该功能当前不可用,请先完成交互探索"
    },

    homepage: {
        title: {
            maintitle: "遥感ARD平台",
            subtitle_tool:"一体化集成分析工具",
            subtitle_explore:"交互式数据探索与可视化",
            subtitle_program:"在线编程分析卫星数据",
        } ,
        text : {
            hero_description: "通过集成分析工具与可视化功能，深度挖掘卫星数据中的洞察力，探索地球的每一寸变化。",

            tool_description:"为卫星图像的管理、分析与可视化提供全面支持",
            tool_search:"图像检索",
            tool_search_description:"利用高级过滤选项访问和检索多个来源的卫星图像。",
            tool_visualisation:"数据可视化",
            tool_visualisation_description:"将复杂的卫星数据转化为清晰、可操作的可视化信息和视角。",
            tool_analysis:"在线分析",
            tool_analysis_description:"利用灵活的在线编程工具对卫星图像进行定制分析。",

            explore_description:"提供直观的交互式工具，帮助您轻松探索卫星数据。",
            explore_visualisation:"实时数据处理和可视化",
            explore_fliter:"高级过滤和数据筛选功能",
            explore_onlineview:"空间数据的在线预览功能",

            program_description:"提供强大的在线编程环境，用于分析和处理卫星数据，并实时生成可视化结果。",
            program_base:"内置丰富的卫星数据分析工具和库",
            program_language:"支持多种编程语言",
            program_feedback:"实时数据处理与可视化反馈",
        
        },

        case:{
            title:"参考案例",

            urbananalysis:"城市发展分析",
            urban_description:"利用时间序列卫星图像分析大都市地区的城市扩张。",

            indexanalysis:"植被指数分析",
            index_description:"对农业区域进行NDVI分析,以监测作物健康和生长模式。",

            erosionstudy:"海岸侵蚀研究",
            erosion_description:"海岸线变化的时间序列分析，以确定侵蚀模式和速率。"

        },

        CTA:{
            title:"从卫星数据中解读最新信息",
            CTAtext:"加入数以千计的研究人员、组织和政府机构的行列,使用遥感ARD平台分析卫星图像获得结论与解决方案。"
        },

        Footer:{
            logo:"遥感ARD平台",
            text_forgroups:"为研究人员、组织和政府机构提供先进的遥感ARD方案",
            subtitle_OpenGMS:"OpenGMS系统",
            OpenGMS_platform:"OpenGMS门户网站",
            platform_open:"开放式建模平台",
            platform_geo:"地理问题求解平台",
            platform_comp:"模型比较平台",
            platform_changsanjiao:"长三角虚拟地理实验平台",

            subtitle_about:"关于我们",
            about:"关于OpenGMS",
            join:"加入我们",

            subtitle_contact:"联系我们",
            wechat:"微信公众号"

        },

        button:{
            explore:"🚀探索数据",
            analysis:"🧪开始分析",
            exploremore:"探索更多"
        }
    },

  modelpage:{
    title:"模型目录",

    search:"搜索",
    detail: "详细信息",
    
    treedata:{
        label_appli:"面向应用的分类",
        subleb_nature:"自然视角",
        nature_land:"陆地圈",
        nature_sea:"海洋圈",
        nature_ice:"冰冻圈",
        nature_atmosphere:"大气圈",
        nature_space:"太空-地球",
        nature_earth:"固体地球",

        subleb_human:"人文视角",
        human_dev:"发展活动",
        human_soc:"社会活动",
        human_eco:"经济活动",

        subleb_comprehensive:"综合视角",
        general_glo:"全球尺度",
        general_reg:"区域尺度",

        label_method:"面向方法的分类",
        subleb_data:"数据视角",
        data_geo:"地理信息分析",
        data_rem:"遥感分析",
        data_geostat:"地统计分析",
        data_sma:"智能计算分析",

        subleb_process:"过程视角",
        process_phy:"物理过程计算",
        process_chm:"化学过程计算",
        process_bio:"生物过程计算",
        process_hun:"人类活动计算"

    }

    
  },

  projectpage:{
    searchbar:"根据工具名称、创建人或工具信息进行搜索，搜索词为空则显示所有工具",
    search:"搜索",
    createtool:"创建工具",
    canceltool:"取消创建",
    mytool:"我的工具",
    alltool:"所有工具",
    toolname:"工具名称",
    envir: "运行环境",

    button:{
        create:"创建",
        cancel:"取消",
    },

    message:{
        error:{
            entererr:"您没有访问该工具的权限，请联系工具创建者",
            create_name_empty:"工具名称不能为空",
            create_description_empty:"描述不能为空",
            createrr_name_exist:"该工具已存在，请更换工具名称",
        },

        success:"创建成功"

    }
  },

  login:{
    title:"登录",
    title_description: "请使用您的邮箱（或用户名）和密码登录！",
    username:"邮 箱/用户名",
    intext_email: "请输入您的邮箱地址",
    password:"密 码",
    intext_password:"请输入您的密码",

    button:{
        submit:" 登 录",
        register:"去 注 册"
    },

    message:{
        error_wrongdetail: "邮箱或密码错误",
        error_fail: "登录失败,请咨询网站管理员",
        success:"登录成功"
    }

  },

  signup:{
    PaymentMethodChangeEventtitle:"注 册",
    descript:"请填写以下信息完成注册！",
    email:"邮 箱",
    intext_email:"请输入您的邮箱",
    password:"密 码",
    intext_password:"请输入密码",
    repassword:"确认密码",
    intext_repassword:"请再次输入密码",
    username:"用户名",
    intext_name:"请输入您的用户名",
    title:"称 谓",
    intext_title:"请输入您的头衔",
    organization:"组 织",
    intext_organization:"请输入您的组织",

    button:{
        register:"注 册",
        back:"返 回"
    },

    message:{
        error:{
            unmatch:"密码和确认密码不匹配！",
            fail:"注册失败，请重试"
        },
        seccuss:"注册成功"
    }

  },

  datapage:{
    title_explore:"交互式探索",
    title_nocloud:"无云一版图",
    title_analysis:"动态展示分析",

    mapcomp:{
        vector:"矢量底图",
        imagery:"影像底图"
    },

    analysis:{
        section1:{
            subtitle:"研究区域选择",
            area:"行政区",
        },

        section2:{
            subtitle:"专题选择：",
            clear:"清空影像图层",
        },

        optionallab:{
            task_DSM:"DSM分析",
            task_DEM:"DEM分析",
            task_red_green:"红绿立体",
            task_rate:"形变速率",
            task_NDVI:"NDVI时序计算",
            task_spectral:"光谱分析",
        },

        message:{
            region:"请选择正确的研究区域"

        }
    },

    optional_thematic:{
      rate:{
        title:"形变速率分析",
        set:"形变速率影像集",
        no_ima:"该区域暂无形变速率影像",
        space:"空间选择",
        map:"地图选择",
        point:"划线采点",
        button:"开始分析",
        result:"计算结果",
        no_result:"暂无结果",
        fullview:"全屏查看",

        message:{
            info_point:"请在地图上绘制研究点",
            info_line:"请在地图上绘制研究线",
            load:"正在为您加载影像...",
            info_start:"开始形变速率分析",
            calerror:"计算服务出错",
            success:"形变速率计算完成",
            info_retry:"形变速率计算失败，请重试",
            info_space:"请先完成空间选择",
            info_noima:"该区域未检出形变速率数据，请更换研究区"
        }
      },

      DEM:{
        title:"DEM分析",
        set:"DEM影像集",
        noimage:"该区域暂无DEM影像",
        space:"空间选择",
        map_point:"地图选点",
        line_point:"划线采点",
        button:"开始分析",
        result:"计算结果",
        noresult:"暂无计算结果",
        fullview:"全屏查看",

        message:{
            info_point:"请在地图上绘制研究点",
            info_line:"请在地图上绘制研究线",
            load:"正在为您加载影像...",
            info_start:"开始DEM分析。",
            calerror:"计算服务出错",
            success:"DEM计算完成",
            info_retry:"DEM计算失败,请重试",
            info_space:"请先完成空间选择",
            info_noima:"该区域未检出DEM数据,请更换研究区"
        }

      },

      DSM:{
        title:"DSM分析",
        set:"DSM影像集",
        noimage:"该区域暂无DSM影像",
        space:"空间选择",
        map_point:"地图选点",
        line_point:"划线采点",
        button:"开始分析",
        result:"计算结果",
        noresult:"暂无计算结果",
        fullview:"全屏查看",

        message:{
            info_point:"请在地图上绘制研究点",
            info_line:"请在地图上绘制研究线",
            load:"正在为您加载影像...",
            info_start:"开始DES分析。",
            calerror:"计算服务出错",
            success:"DSM计算完成",
            info_retry:"DSM计算失败,请重试",
            info_space:"请先完成空间选择",
            info_noima:"该区域未检出DSM数据,请更换研究区"
        }
      },

      NDVI:{
        title:"NDVI时序计算",
        space:"空间选择",
        map_point:"地图选点",
        line_point:"划线采点",
        add:"添加辅助图斑：",
        button:"开始分析",
        result:"计算结果",
        noresult:"暂无计算结果",
        fullview:"全屏查看",

        message:{
            info_point:"请在地图上绘制研究点",
            info_line:"请在地图上绘制研究线",
            info_choose:"请选择您要计算的区域",
            load:"开始ndvi时序分析",
            info_start:"开始NDVI分析。",
            calerror:"计算失败，请重试",
            success:"NDVI计算完成",
            info_retry:"NDVI计算失败,请重试",
            success_poin:"已加影像边界，请在影像与行政区的交集内选点。",
            info_fail:"加载影像边界失败。"
        }
      },

      RG:{
        title:"红绿立体",
        set:"红绿立体影像集",
        noimage:"该区域暂无红绿立体影像",

        load:"正在为您加载影像..."
        
      },

      spectrum:{
        title:"光谱分析",
        wait:"待分析影像",
        select:"影像选择：",
        op_select:"请选择影像",
        space:"空间选择",
        map_point:"地图选点",
        line_point:"划线采点",
        button:"开始分析",
        result:"计算结果",
        noresult:"暂无计算结果",
        fullview:"全屏查看",

        message:{
            info_point:"请在地图上绘制研究点",
            info_line:"请在地图上绘制研究线",
            info_noima:"该区域暂无高光谱影像",
            info_reg:"请先选择您要计算的区域",
            info_ima:"请先选择您要计算的影像",
            info_start:"开始高光谱分析。",
            calerror:"计算失败，请重试",
            success:"光谱分析计算完成",
            info_retry:"光谱分析计算失败,请重试",
            success_poin:"已加影像边界，请在影像与行政区的交集内选点。",
            info_fail:"加载影像边界失败。"
        }
      }
    },

    analysisback:{
        calarea:"计算领域：",
        customisation:"自定义",
        dian:"地图选点",
        dianbutton:"开始选点",

        regionchoosde:"",

        imagselect:"影像选择：",
        selectoption:"请选择影像",

        latitude:"纬度",
        longitude:"经度",

        revolution:"格网分辨率",
        timerange:"涵盖时间范围",
        quyingxiang:"研究区影像",
        percent:"覆盖率",

        calbutton:"开始计算",
        calresult:"计算结果",

        imgselect:"所选影像为：",

        jingwei:"经纬度：",

        fullviewbutton:"全屏查看",

        optionallab:{
            task1:"NDVI时序计算",
            task2:"光谱分析",
            task3:"滑坡概率计算",
            task4:"洪水频发风险区域计算",
        },

        message:{
            imageerror:"加载影像边界失败。",
            imagesuccess:"已加影像边界，请在影像与行政区的交集内选点。",

            calRegerror1:"请先选择您要计算的区域",
            calImgerror2:"请先选择您要计算的影像",
            calerror3:"计算失败，请重试",

            guangpuseccess:"光谱分析计算完成",
            gunagpuerror:"光谱分析计算失败，请重试",

            ndvisuccess:"NDVI计算完成",
            ndvierror:"NDVI计算失败,请重试"
        }

    },

    explore:{
        section1:{
            sectiontitle:"行政区划与格网分辨率",
            admin:"行政区",
            subtitle1:"研究区选择",
            intext_choose:"'选择省份', '选择城市', '选择区县'",
            intext_POI:"请输入 POI 关键词",

            subtitle2:"格网分辨率",
            resolution:"格网分辨率选择：",
            advice:" 建议省级行政单位格网分辨率不超过30km",

            button:"获取格网",
        },

        section_time:{
            sectiontitle:"按时间筛选",
            subtitle1:"时间范围",
            intext_date:"'开始日期', '结束日期'",
            button:"数据检索",

            subtitle2:"统计信息",
            resolution:"格网分辨率",
            time:"涵盖时间范围",
            search:"当前已检索到",
            
        },

        section_interactive:{
            sectiontitle:"交互探索",
            clear:"清空影像图层",
            subtitle:"分辨率影像集",
            subtitle1:"(仅含ARD数据)",
            resolutiontype:{
                yami:"亚米",
                twom:"2米",
                tenm:"10米",
                thirtym:"30米",
                others: "其他",
            },
            intext1:"待计算",
            chooseimag: "选择影像：",
            select:"请选择影像",
            light:"亮度拉伸:",
            button: "影像可视化",

            guochan:"国产影像",
            guowai:"国外影像",
            guaxue:"光学影像",
            sar:"SAR影像",
            yuanshi:"原始影像",
            ard:"ARD影像",

            scene:"选择传感器：",
            choose:"请选择"

        },

        include:"包含",
        scene:"景影像",
        percent:"覆盖率",
        scene_choose:"选择传感器：",

        message:{
            ordererror:"请完成数据检索后再计算无云一版图",
            filtererror_choose:"请先选择行政区并获取格网",
            filtererror_grid:"请先获取格网",
            sceneerror_recondition:"未筛选出符合要求的影像，请重新设置条件",
            scene_searched:"已检索到{count}景影像，请进一步筛选您所需的影像",
            imagery_error:"请选择有效影像",
            load:"正在加载影像...",
            typyerror_condition:"请设置筛选条件",
            typyerror_filter:"请先进行数据检索",
            typyerror_source:"请选择您需要的数据来源",
            typyerror_type:"请选择您需要的传感器类型",
            typyerror_data:"请选择您需要的数据级别",

            POIerror:"请选择行政区或POI"
        }
    },

  

    nocloud:{
        title:"无云一版图重构",

        section_chinese:{
            subtitle:"国产光学影像",

            text_national_image:"国产亚米级影像(仅含ARD数据)",
            text_national2m:"使用国产2m级影像",

            resolution :"格网分辨率",
            timerange:"涵盖时间范围",

            text_national_research:"研究区亚米级国产影像",
            text_national_coverage:"亚米级国产影像覆盖率",

            intext_calcu:"待计算",

            text_research2m:"研究区国产影像(2m超分后)",
            text_coverage2m:"影像覆盖率(2m超分后)",
        },

        section_international:{
            subtitle:"融合国外光学影像",

            text_preview:"国外影像填补格网效果预览",
            text_overseaimage:"使用国外影像数据参与重构",
            text_research:"研究区国外影像",
            text_coverage:"累积影像覆盖率",

            intext_calcu:"待计算",

        },

        section_SAR:{
            subtitle:"融合SAR影像",
            intext_descript:"勾选将使用雷达数据进行色彩变换，与光学数据配准，并补充重构。",

            text_preview:"SAR影像填补格网效果预览",
            text_SARtrans:"使用SAR影像色彩变换参与重构",
            text_SARresearch:"研究区SAR影像",
            text_coverage:"累积影像覆盖率",

            intext_calcu:"待计算"

        },

        section4:{
            button:"一版图重构",
        },

        section5:{
            subtitle:"重构信息",
        },

        choose: "选择优先使用的传感器",

        message:{
            load:"正在加载无云一版图，请稍后...",
            calerror:"计算失败，请重试",
            closuccess:"无云一版图计算完成",
            cloerror:"无云一版图计算失败，请重试",
            guochanload:"国产亚米级影像加载完毕"

        }
      },

      history:{
        his_recon:"历史记录重构",
        back:"返回",
        his_noclo:"历史无云一版图",
        wait:"任务初始化中，请稍后...",
        no_task:"暂无运行中的任务", 
        refresh:"刷新全部",
        resolution:"格网分辨率",
        sta_time:"开始时间",
        data:"使用数据",
        condition:"筛选条件",
        create_time:"生成时间",
        choose:"请选择",
        admin:"行政区划",
        admin_choose:"['选择省份', '选择城市', '选择区县']",
        clear:"重置",
        fliter:"筛选",
        preview:"预览",

        process:"运行中",
        finish:"已完成",
        onehour:"最近一小时",
        today:"今天",
        sevenday:"最近7天",
        thirtyday:"最近30天",
        threemonth:"最近3个月",
        ealier:"更早之前"
      }

    },
  }
