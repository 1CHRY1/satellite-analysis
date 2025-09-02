export default{
  nav: {
    title: "Muti-resource RS Application Cloud Platform",
    home: "Home",
    source: "Resource Center",
    models: "Model Center",
    data: "Data Center",
    tools: "Tools",
    about: "About Us",

    button: {
      login: "Sign in",
      signup: "Sign up",
      logout:"Log out"
    },

    disabled_message:"This Page is currently unavailable. Please complete the interactive exploration first"
  },

  homepage: {
    title:{
      maintitle: "Muti-resource RS Application Cloud Platform",
      subtitle_tool:"An integrated analytical tool",
      subtitle_explore:"Interactive data exploration and visualisation",
      subtitle_program:"Online programming for satellite data analysis",
    } ,
    text : {
      hero_description: "By integrating analytics tools and visualisation capabilities, we are able to deeply discover the insights in satellite data and explore every possible change in the Earth.",

      tool_description: "Providing comprehensive support for the management, analysis and visualisation of satellite imagery.",
      tool_search:" Image Retrieval",
      tool_search_description:"Access and retrieve satellite imagery from multiple sources using advanced filtering options.",
      tool_visualisation:"Data Visualisation",
      tool_visualisation_description:"Convert complex satellite data into clear and actionable visual-information and perspectives.",
      tool_analysis:"Online Analysis",
      tool_analysis_description:"Customised analysis of satellite imagery using flexible online programming tools.",
      
      explore_description:"Provides intuitive and interactive tools to help you explore satellite data easily.",
      explore_visualisation:"Real-time data processing and visualisation",
      explore_fliter:"Advanced Filtering and Data Filtering Fuctions",
      explore_onlineview:"Online preview function for spatial data",

      program_description:"Provide a powerful online programming environment for analysing and processing satellite data and generating visualisations in real time.",
      program_base:"A rich set of built-in tools and libraries for analysing satellite data",
      program_language:"Support multiple programming languages",
      program_feedback:"Real-time data processing and visual feedback",

    },

    case:{
      title:"Case",

      urbananalysis:"Urban Development Anaylsis",
      urban_description:"Using timeing satellite imagery to analyse urban sprawl in metropolitan areas.",

      indexanalysis:"Vegetation Index Anaylsis",
      index_description:"Monitor crop health and growth patterns through NDVI analyses of agricultural areas.",

      erosionstudy:"Coastal Erosion Study",
      erosion_description:"A timeing analysis of shoreline change to determine erosion patterns and rates."

    },

    CTA:{
      title:"Interpreting the latest information from satellite data",
      CTAtext:"Join the team with thousands of researchers, organisations and government agencies, using the Remote Sensing ARD platform to analyse satellite imagery to obtain conclusions and solutions."
    },

    Footer:{
      logo:"Remote Sensing ARD Platform",
      text_forgroups:"Advanced Remote Sensing ARD Programme for Researchers, Organisations and Government Agencies",
      subtitle_OpenGMS:"OpenGMS System",
      OpenGMS_platform:"OpenGMS platform",
      platform_open:"Open modelling platform",
      platform_geo:"Geographic problem-solving platform",
      platform_comp:"Model Comparison Platform",
      platform_changsanjiao:"Yangtze River Delta Virtual Geography Experimental Platform",

      subtitle_about:"About Us",
      about:"About OpenGMS",
      join:"Join us",

      subtitle_contact:"Contact Us",
      wechat:"WeChat Public"

    },

    button:{
      explore:"🚀Explore Data",
      analysis:"🧪Start analysis",
      exploremore:"Explore more"
    },
  }, 
   
  modelpage:{
    title:"Model catalogue",

    search:"Search",
    detail: "Details",
    
    treedata:{
        label_appli:"Application-oriented classification",
        subleb_nature:"Nature Perspectives",
        //陆地圈的翻译？ 
        nature_land:"Lithosphere",
        nature_sea:"Oceanosphere",
        nature_ice:"Cryosphere",
        nature_atmosphere:"Atmosphere",
        nature_space:"Space-Earth",
        nature_earth:"Solid Earth",

        subleb_human:"Human Perspectives",
        human_dev:"Development activities",
        human_soc:"Socal acticities",
        human_eco:"Economics activities",

        subleb_comprehensive:"Comprehensive Perspective",
        general_glo:"Global scale",
        general_reg:"Regional scale",

        label_method:"Method-oriented classification",
        subleb_data:"Data Perspectives",
        data_geo:"geographic information analysis",
        data_rem:"Remote Sensing Anaylsis",
        data_geostat:"Geostatistical Analysis",
        data_sma:"Smart Computing Analysis",

        subleb_process:"Process Perspectives",
        process_phy:"Physical process calculations",
        process_chm:"Chemical process calculations",
        process_bio:"Biological process calculations",
        process_hun:"Human activity calculations"

    }

    
  },

  projectpage:{
    searchbar:"Search by tool name, creator, or tool information, and display all tools if the search term is empty",
    search:"Search",
    createtool:"Create tools",
    mytool:"My tools",
    alltool:"All tools",
    toolname:"Tool name",
    envir: "operating environment",

    button:{
        create:"Create",
        cancel:"Cancel",
    },

    message:{
        error:{
            entererr:"You have no permission to access this tool, please contact the tool creator",
            create_name_empty:"The tool name cannot be empty",
            create_description_empty:"Description cannot be empty",
            createrr_name_exist:"This tool already exists, please change the tool name",
        },

        success:"Create Successfully"

    }
  },

  login:{
    title:"Sign in",
    title_description: "Please sign in with your email (or username) and password!",
    username:"Email / Username",
    intext_email: "Please enter your email address",
    password:"Password",
    intext_password:"Please enter your password",

    button:{
        submit:" Sign in ",
        register:"Sign up"
    },

    message:{
        error_wrongdetail: "Incorrect email or password",
        error_fail: "Fail to sign in, please consult the webmaster",
        success:"Sign in successfully"
    }

  },

  signup:{
    pagetitle:"Sign up",
    descript:"Please fill out the following information to complete your registration!",
    email:"Email",
    intext_email:"Please enter your email",
    password:"Password",
    intext_password:"Please enter your password",
    repassword:"Confirm Password",
    intext_repassword:"Please re-enter your password ",
    username:"Username",
    intext_name:"Please enter your username",
    title:"Title",
    intext_title:"Please choose your title",
    organization:"Orgnisation",
    intext_organization:"Please enter your orgnisation",

    button:{
        register:"Sign up",
        back:"Back"
    },

    message:{
        error:{
            unmatch:"The passwords entered  do not match!",
            fail:"Failed to register, please retry."
        },
        seccuss:"Register Successfully"
    }

  },

  datapage:{
    title_explore:"Interactive Exploration",
    title_nocloud:"Cloud-free basemap",
    title_analysis:"Dynamic Display Analysis",

    mapcomp:{
        vector:"Vector basemap",
        imagery:"Imagery basemap"
    },

    analysis:{
        section1:{
            subtitle:"Research region selection",
            area:"Administrative Divisions",
        },

        section2:{
            subtitle:"Topic selection:",
            clear:"Clear image layers",
        },

        optionallab:{
            task_DSM:"DSM Analysis",
            task_DEM:"DEM Analysis",
            task_red_green:"Red-green anaglyph stereoscopy",
            task_rate:"Deformation rate",
            task_NDVI:"NDVI timing calculation",
            task_spectral:"Spectral analysis",
        },

        message:{
            region:"Please select the correct research region"

        }
    },

    optional_thematic:{
      rate:{
        title:"Deformation rate analysis",
        set:"Deformation rate imagery set",
        no_ima:"No abailable deformation rate imagery in the selected area.",
        space:"Spatial selection",
        map:"Map point selection",
        point:"Line-based point sampling",
        button:"Execute Analysis",
        result:"Result",
        no_result:"no result",
        fullview:"Fullscreen",

        message:{
            info_point:"Draw research points on the map",
            info_line:"Draw research lines on the map",
            load:"Imagery loading...",
            info_start:"Luanch deformation rate analysis",
            calerror:"Processing error",
            success:"Deformation rate calculation completed",
            info_retry:"Deformation rate calculation failed. Please retry.",
            info_space:"Please do the spatial selection first.",
            info_noima:"No detectalbe deformation rate data found in the selected area, Please adjust the research region "
        }
      },

      DEM:{
        title:"DEM analysis",
        set:"DEM imagery set",
        noimage:"No available DEM imagery in the selected area",
        space:"Spatial selection",
        map_point:"Map point selection",
        line_point:"Line-based point sampling",
        button:"Execute Analysis",
        result:"Result",
        noresult:"no result",
        fullview:"Fullscreen",

        message:{
            info_point:"Draw research points on the map",
            info_line:"Draw research lines on the map",
            load:"Imagery loading...",
            info_start:"Luanch DEM analysis",
            calerror:"Processing error",
            success:"DEM calculation competed",
            info_retry:"DEM calculation failed. Please retry.",
            info_space:"Please do the spatial selection first.",
            info_noima:"No detectalbe DEM data found in the selected area, Please adjust the research region "
        }

      },

      DSM:{
        title:"DSM analysis",
        set:"DSM imagery set",
        noimage:"No available DSM imagery in the selected area",
        space:"Spatial selection",
        map_point:"Map point selection",
        line_point:"Line-based point sampling",
        button:"Execute Analysis",
        result:"Result",
        noresult:"no result",
        fullview:"Fullscreen",

        message:{
            info_point:"Draw research points on the map",
            info_line:"Draw research lines on the map",
            load:"Imagery loading...",
            info_start:"Luanch DSM analysis",
            calerror:"Processing error",
            success:"DSM calculation completed",
            info_retry:"DSM calculation failed,please retry",
            info_space:"Please do the spatial selection first.",
            info_noima:"No detectalbe DSM data found in the selected area, Please adjust the research region "
        }
      },

      NDVI:{
        title:"NDVI timing calculation",
        space:"Spatial selection",
        map_point:"Map point selection",
        line_point:"Line-based point sampling",
        add:"Add supporting polygons:",
        button:"Execute Analysis",
        result:"Result",
        noresult:"no result",
        fullview:"Fullscreen",

        message:{
            info_point:"Draw research points on the map",
            info_line:"Draw research lines on the map",
            info_choose:"Select analysis region",
            load:"Luanch ndvi timing analysis",
            info_start:"Luanch NDVI analysis",
            calerror:"Processing error,please retry.",
            success:"NDVI calculation completed",
            info_retry:"NDVI calculation failed,please retry",
            success_poin:"Imagery boundaries have been loaded, please select points from the overlap area between imagery and administrative boundaries ",
            info_fail:"Failed to load imagery boundary"
        }
      },

      RG:{
        title:"Red-green anaglyph stereoscopy",
        set:"RG stereoscopic imagery set ",
        noimage:"No available RG stereoscopic imagery in the selected area",

        load:"Imagery loading..."
        
      },

      spectrum:{
        title:"Spectral analysis",
        wait:" Unprocessed imagery",
        select:"Imagery selection:",
        op_select:"Please select imagery",
        space:"Spatial selection",
        map_point:"Map point selection",
        line_point:"Line-based point sampling",
        button:"Execute Analysis",
        result:"Result",
        noresult:"no result",
        fullview:"Fullscreen",

        message:{
            info_point:"Draw research points on the map",
            info_line:"Draw research lines on the map",
            info_noima:"No available spectral imagery in the seletced area ",
            info_reg:"Select analysis region",
            info_ima:"Select analysis imagery",
            info_start:"Luanch spectral analysis",
            calerror:"Processing error,please retry.",
            success:"Spectral analysis processing completed",
            info_retry:"Spectral analysis processingfe failed,please retry",
            success_poin:"Imagery boundaries have been loaded, please select points from the overlap area between imagery and administrative boundaries。",
            info_fail:"Failed to load imagery boundary"
        }
    }
    },

    analysisback:{
        calarea:"Computing area: ",
        customisation:"customisation",
        dian:"Map point selection",
        dianbutton:"Start selection",

        regionchoosde:"",

        imagselect:"Image selection:",
        selectoption:"Please select an image",

        latitude:"latitude",
        longitude:"longitude",

        revolution:"Grid resolution",
        timerange:"Covered time period",
        quyingxiang:"imagery of the research area",
        percent:"image coverage",

        calbutton:"Start calculation",
        calresult:"Result",

        imgselect:"The selected image is:",

        jingwei:"Latitude and longitude::",

        fullviewbutton:"Full screen view",

        optionallab:{
            task_NDVI:"NDVI timing calculation",
            task_spectral:"Spectral analysis",
            task_landslide:"Landslide Probability Calculation",
            task_flood:"High frequency flood area calculations",
        },

        message:{
            imageerror:"Failed to load image boundaries.",
            imageryuccess:"Image boundaries have been loaded, please select points within the intersection of the image and the borough.",

            calRegerror1:"Please select the area you want to calculate",
            calImgerror2:"Please select the image you want to calculate",
            calerror3:"Failed to calculate, please retry.",

            guangpuseccess:"Spectral analysis calculations completed",
            gunagpuerror:"Faided to calculate spectral analysis, please retry",

            ndvisuccess:"NDVI calculation completed",
            ndvierror:"Failed to calculated NDVI, please retry"
        }

    },

    explore:{
        section1:{
            sectiontitle:"Administrative divisions and grid resolution",
            subtitle1:"Research region selection",
            admin:"Administrative division",
            intext_choose:"'Province Selection', 'City Selection', 'County Selection'",
            intext_POI:"Please enter the POI keyword",

            subtitle2:"Grid resolution",
            resolution:"Grid resolution selection:",
            advice:" We recommend that the grid resolution of the provincial administrative units should not be more than 30km",

            button:"Access Grid",
        },

        section_time:{
            sectiontitle:"Filter by time",
            subtitle1:"Time range",
            intext_date:"'date of start', 'date of end'",
            button:"Data retrieval",

            subtitle2:"Statistical information",
            resolution:"Grid resolution",
            time:"Covered time range",
            search:"Currently retrieved",
            
        },

        section_interactive:{
            sectiontitle:"Interactive Exploration",
            clear:"Clear image layers",
            subtitle:" resolution image set",
            subtitle1:"(ARD data only)",
            resolutiontype:{
                yami:"Sub-metre",
                twom:"2m",
                tenm:"10m",
                thirtym:"30m",
                others: "others",
            },
            intext1:"Pending calculations",
            chooseimag: "Image Selection:",
            select:"Please select an image",
            light:"Brightness Stretch:",
            button: "Image Visualisation",

            guochan:"National imagery",
            guowai:"Overseas imagery",
            guaxue:"optical image",
            sar:"SAR imagery",
            yuanshi:"Original Image",
            ard:"ARD Image",

            scene:"Scene Selection:",
            choose:"Please select"

        },

        include:"Include",
        scene:"-Scene Imagery",
        percent:"Image coverage",
        scene_choose:"Select sensor：",

        message:{
            ordererror:"Please complete the data retrieval first, then generate the cloud-free basemap",
            filtererror_choose:"Please select the administrative division and access the grid first",
            filtererror_grid:"Please access the grid first",
            sceneerror_recondition:"No imagery matching the requirements , please reset the conditions",
            scene_searched:"Retrieved {count} -scene imagery, please refine your image selection criteria",
            imagery_error:"Please select valid imagery",
            load:"Loading imagery...",
            typyerror_condition:"Please set the filter conditions",
            typyerror_filter:"Please retrieve the data first",
            typyerror_source:"Please select the data source you need",
            typyerror_type:"Please select the type of sensor you need",
            typyerror_data:"Please select the data level you need",

            POIerror:"Please select administrative division or POI"
        }
    },

  

    nocloud:{
        title:"Cloud-free basemap reconstruction",

        section_chinese:{
            subtitle:"National Optical Image",

            text_national_image:"national sub-metre level imagery(ARD data only)",
            text_national2m:"Using national 2m level image",

            resolution :"Grid Resolution",
            timerange:"Covered time period",

            text_national_research:"Sub-metre level imagery of the research areas ",
            text_national_coverage:"Sub-metre national image coverage",

            intext_calcu:"Pending calculations",

            text_research2m:"National imagery of the research ares(2m超分后)",
            text_coverage2m:"Image coverage(2m超分后)",
        },

        section_international:{
            subtitle:"Integrating international optical imagery",

            text_preview:"Preview of grid filling effects using international imagery",
            text_overseaimage:"Incorporating international imagery for reconstruction",
            text_research:"International imagery of the research area",
            text_coverage:"Cumulative imagery coverage",

            intext_calcu:"Pending calculations",

        },

        section_SAR:{
            subtitle:"SAR imagery integration",
            intext_descript:"勾选将使用雷达数据进行色彩变换，与光学数据配准，并补充重构。",

            text_preview:"Preview of grid filling effects using SAR imagery",
            text_SARtrans:"Using SAR image colour transformations to participate in reconstruction",
            text_SARresearch:"SAR imagery of the research areas",
            text_coverage:"Cumulative image coverage",

            intext_calcu:"Pending calculations"

        },

        section4:{
            button:"Basemap reconstruction",
        },

        section5:{
            subtitle:"Reconstruction Information",
        },

        choose: "Select Priority Sensor:",
        button_choose:"Choose",

        message:{
            load:"Loading cloud-free basemap, please wait...",
            calerror:"Calculation failed, please retry.",
            closuccess:"Cloud-free basemap generation completed",
            cloerror:"Cloud-free basemap generation failed. Please retry.",
            guochanload:"National sub-meter imagery loaded."

        }
    },

    history:{
        his_recon:"Reconstruction Archive",
        back:"Back",
        his_noclo:"Cloud-Free Basemap Archive",
        wait:"Task initialization in progress, please wait...",
        no_task:"No tasks currently running", 
        refresh:"Refresh all",
        resolution:"Grid resolution",
        sta_time:"Start Time",
        data:"Utilized Data",
        condition:"Filter Criteria",
        create_time:"Generation Time",
        choose:"Please select",
        admin:"Administrative Division",
        admin_choose:"'Province Selection', 'City Selection', 'County Selection'",
        clear:"Reset",
        fliter:"Filter",
        preview:"Preview",
        
        process:"Processing",
        finish:"Finished",
        onehour:"Past 1 hour",
        today:"Today",
        sevenday:"Past 7 days",
        thirtyday:"Past 1 month",
        threemonth:"Past 3 months",
        ealier:"Older"
      }

  },
  userpage: {
    change: "change",
    introduction: "There is no brief introduction.",
    edit: "Edit",
    logout: "Logout",
    name: "name",
    selfIntroduction: "self-introduction",
    cancel: "Cancel",
    confirm: "Confirm",
    userFunction: {
    overview: "Overview",
    projects: "Projects",
    data: "Data",
    hot: "Recent used",
    dynamic: "Activity Timeline",
    load: "Load more...",
    last: "Last updated",
    add: "Add new members",
    bulk: "Bulk import",
    back: "Back",
    refresh: "Refresh",
    upload: "Upload data",
    newfolder: "New Folder",
    details: "Data details",
    id: "ID",
    time: "Create time",
    type: "Create type",
    size: "Data size",
    new: "New folder",
    edit: "Edit data",
    move: "Move data",
    join: "Join project",
    down: "Download data",
    delete: "Delete data ",
    emptyItem: "No Item",
    emptyState: "No State",
    noSelect: "Please select data",
    addData: "Add to Private"
    },
    data: {
      new: "New Data",
      description: "Data Description:",
      state: "Open State:",
      public: "Public",
      private: "Private",
      select: "Select Data",
      drag: "Drag the file here or",
      upload: "click to upload",
      start: "Start Uploading",
      name: "Data Name",
      openness: "Openness",
      profile: "Data Profile",
      move: "Move to",
      moveto: "Move to parent directory",
      multiple: "Multiple Data",
      or: "Drag to this or ",
      addto: "Add data to the project",
      add: "Select the project you want to add to"
    }
  },
}
