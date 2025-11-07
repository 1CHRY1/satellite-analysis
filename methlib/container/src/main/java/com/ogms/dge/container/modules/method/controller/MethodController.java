package com.ogms.dge.container.modules.method.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.ValidationMessage;
import com.ogms.dge.container.common.annotation.SysLog;
import com.ogms.dge.container.common.utils.PageUtils;
import com.ogms.dge.container.common.utils.R;
import com.ogms.dge.container.common.validator.ValidatorUtils;
import com.ogms.dge.container.modules.common.entity.UploadRecordEntity;
import com.ogms.dge.container.modules.common.enums.UploadRecordStatusEnum;
import com.ogms.dge.container.modules.common.service.impl.UploadRecordServiceImpl;
import com.ogms.dge.container.modules.data.service.ServiceService;
import com.ogms.dge.container.modules.data.service.SourceService;
import com.ogms.dge.container.modules.method.converter.MethodConverter;
import com.ogms.dge.container.modules.method.entity.MethodEntity;
import com.ogms.dge.container.modules.method.entity.TagEntity;
import com.ogms.dge.container.modules.method.service.MethodService;
import com.ogms.dge.container.modules.method.service.MethodTagService;
import com.ogms.dge.container.modules.method.service.TagService;
import com.ogms.dge.container.modules.method.vo.MethodVo;
import io.netty.util.internal.StringUtil;
import io.swagger.annotations.ApiOperation;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * 数据处理方法
 */
@RestController
@RequestMapping("method")
public class MethodController extends AbstractController {
    @Autowired
    private MethodService methodService;

    @Autowired
    private MethodTagService methodTagService;

    @Autowired
    private TagService tagService;

    private ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private MethodConverter methodConverter;

    @Autowired
    private UploadRecordServiceImpl uploadRecordService;

    @Autowired
    private ServiceService serviceService;

    @Autowired
    private SourceService sourceService;

    private final String categories = " - climate_tools\n" +
            " - contrib_perego\n" +
            " - db_odbc\n" +
            " - db_pgsql\n" +
            " - docs_html\n" +
            " - docs_pdf\n" +
            " - garden_fractals\n" +
            " - garden_webservices\n" +
            " - grid_analysis\n" +
            " - grid_calculus\n" +
            " - grid_calculus_bsl\n" +
            " - grid_filter\n" +
            " - grid_gridding\n" +
            " - grid_spline\n" +
            " - grid_tools\n" +
            " - grid_visualisation\n" +
            " - grids_tools\n" +
            " - imagery_classification\n" +
            " - imagery_isocluster\n" +
            " - imagery_maxent\n" +
            " - imagery_opencv\n" +
            " - imagery_photogrammetry\n" +
            " - imagery_segmentation\n" +
            " - imagery_svm\n" +
            " - imagery_tools\n" +
            " - imagery_vigra\n" +
            " - io_esri_e00\n" +
            " - io_gdal\n" +
            " - io_gps\n" +
            " - io_grid\n" +
            " - io_grid_image\n" +
            " - io_pdal\n" +
            " - io_riegl_rdb\n" +
            " - io_shapes\n" +
            " - io_table\n" +
            " - io_virtual\n" +
            " - io_webservices\n" +
            " - pj_georeference\n" +
            " - pj_geotrans\n" +
            " - pj_proj4\n" +
            " - pointcloud_tools\n" +
            " - shapes_grid\n" +
            " - shapes_lines\n" +
            " - shapes_points\n" +
            " - shapes_polygons\n" +
            " - shapes_tools\n" +
            " - shapes_transect\n" +
            " - sim_air_flow\n" +
            " - sim_cellular_automata\n" +
            " - sim_ecosystems_hugget\n" +
            " - sim_erosion\n" +
            " - sim_fire_spreading\n" +
            " - sim_geomorphology\n" +
            " - sim_hydrology\n" +
            " - sim_ihacres\n" +
            " - sim_landscape_evolution\n" +
            " - sim_qm_of_esp\n" +
            " - sim_rivflow\n" +
            " - statistics_grid\n" +
            " - statistics_kriging\n" +
            " - statistics_points\n" +
            " - statistics_regression\n" +
            " - ta_channels\n" +
            " - ta_cliffmetrics\n" +
            " - ta_compound\n" +
            " - ta_hydrology\n" +
            " - ta_lighting\n" +
            " - ta_morphometry\n" +
            " - ta_preprocessor\n" +
            " - ta_profiles\n" +
            " - ta_slope_stability\n" +
            " - table_calculus\n" +
            " - table_tools\n" +
            " - tin_tools\n" +
            " - vis_3d_viewer";

    @Value("${container.data.dsd}")
    private String data_dsd;

    /**
     * 列表
     */
    @RequestMapping("/list")
    // @RequiresPermissions("container:method:list")
    public R list(@RequestParam Map<String, Object> params) throws JsonProcessingException {
        params.put("createUserId", getUserId());
        PageUtils page = methodService.queryPage(params);
        return R.ok().put("page", page);
    }

    /**
     * 列表
     */
    @RequestMapping("/listWithStringTag")
    // @RequiresPermissions("container:method:list")
    public R listWithStringTag(@RequestParam Map<String, Object> params) throws JsonProcessingException {
        params.put("createUserId", getUserId());
        PageUtils page = methodService.queryPage(params);
        List<MethodVo> methodVoList = (List<MethodVo>) page.getList();
        List<Map<String, Object>> resultList = new ArrayList<>();
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        for (MethodVo methodVo : methodVoList) {
            Map<String, Object> result = objectMapper.convertValue(methodVo, Map.class);
            result.put("tagIdList", objectMapper.convertValue(methodVo.getTagIdList(), objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)));
            resultList.add(result);
        }
        page.setList(resultList);
        return R.ok().put("page", page);
    }

    /**
     * 列表带标签列表, 兼容移植工作接口
     */
    @RequestMapping("/listWithTag")
    // @RequiresPermissions("container:method:list")
    public R listWithTag(@RequestParam Map<String, Object> params) throws JsonProcessingException {
        params.put("createUserId", getUserId());
        PageUtils page = methodService.queryPageWithTag(params);
        return R.ok().put("page", page);
    }

    /**
     * getMethod
     */
    @SysLog("获取方法")
    @PostMapping("/infoByIds")
//	@RequiresPermissions("sys:user:delete")
    public R infoByIds(@RequestBody Long[] methodIds) {
        return R.ok().put("methods", methodService.getBaseMapper().selectBatchIds(Arrays.asList(methodIds)));
    }

    // 外部调用处理方法
    @PostMapping("/invoke/{id}")
    public R invoke(@PathVariable("id") Long id, @RequestBody Map<String, Object> params) throws IOException {
        MethodEntity method = methodService.getById(id);
        return (R) methodService.invoke(getUserId(), method, params);
    }

    // 数据服务容器调用处理方法
    @PostMapping("/run/{id}")
    public R run(@PathVariable("id") Long id, @RequestParam(required = true) String executionId, @RequestParam(required = false) String serviceUuid, @RequestBody Map<String, Object> params) throws IOException {
        MethodEntity method = methodService.getById(id);
        // serviceUuid为空，个人空间输入；serviceUuid非空，数据映射
        return (R) methodService.run(getUserId(), method, executionId, serviceUuid, params);
    }

    @GetMapping("/output/{executionId}")
    public R getOutput(@PathVariable("executionId") String executionId) {
        return R.ok().put("output", new ArrayList<>(methodService.getOutput(executionId)));
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
//    @RequiresPermissions("container:method:info")
    public R info(@PathVariable("id") Long id) throws JsonProcessingException {
        MethodEntity method = methodService.getById(id);
        MethodVo methodVo = methodConverter.po2Vo(method, methodTagService);

        return R.ok().put("method", methodVo);
    }

    /**
     * 信息
     */
    @RequestMapping("/infoByName/{name}")
//    @RequiresPermissions("container:method:info")
    public R info(@PathVariable("name") String name) throws JsonProcessingException {
        // 创建 LambdaQueryWrapper 实例
        LambdaQueryWrapper<MethodEntity> queryWrapper = new LambdaQueryWrapper<>();

        // 添加模糊查询条件
        queryWrapper.like(MethodEntity::getName, name);

        // 获取第一个匹配的结果
        MethodEntity method = methodService.getOne(queryWrapper, false);
        MethodVo methodVo = methodConverter.po2Vo(method, methodTagService);

        return R.ok().put("method", methodVo).put("paramType", methodService.getParamType(methodVo.getParams()));
    }

    @ApiOperation("上传方法服务包")
    @PostMapping("/upload")
    public R uploadMethod(/*HttpSession session,*/
            MultipartFile file,
            String fileName,
            Integer chunkIndex,
            Integer chunks) {

        // SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        Map<String, Object> result = methodService.uploadMethod(getUserId(), file, fileName, chunkIndex, chunks);
        return R.ok().put(result);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
//    @RequiresPermissions("container:method:save")
    public R save(@RequestParam Long recordId, @RequestBody MethodVo methodVo) throws JsonProcessingException {
        ValidatorUtils.validateEntity(methodVo);
        UploadRecordEntity record = uploadRecordService.getById(recordId);
        methodVo.setCreateUserId(getUserId());
        methodVo.setCreateTime(new Date());
        methodVo.setUuid(record.getPackageUuid());
        methodService.saveMethod(methodVo);

        record.setStatus(UploadRecordStatusEnum.SECOND_CONFIG.getCode());
        uploadRecordService.saveOrUpdate(record);

        return R.ok().put("uuid", record.getPackageUuid());
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
//    @RequiresPermissions("container:method:update")
    public R update(@RequestBody MethodVo methodVo) throws JsonProcessingException {
        methodService.update(methodVo);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
//    @RequiresPermissions("container:method:delete")
    public R delete(@RequestBody Long[] ids) {
        methodService.deleteBatch(ids);

        return R.ok();
    }

    private Map<String, Object> allType = new HashMap<>();
    private Integer nonTimes = 0;

    @RequestMapping("/write/C") // 开进程爬Saga到txt
    public R writeC() throws IOException, InterruptedException {
        String sagaCmdPath = "C:\\Users\\lkshi\\Desktop\\saga-9.7.0_x64\\saga_cmd.exe";
        String outputTxtDir = "C:\\Users\\lkshi\\Desktop\\saga-C.txt";
        List<String> categoryList = parseStringToList(categories);

        BufferedWriter writer = new BufferedWriter(new FileWriter(outputTxtDir, true));
        for (String category : categoryList) {
            for (int i = 0; i < 300; i++) {
                String cmd = sagaCmdPath + " -C " + category + " " + i;
                ProcessBuilder builder = new ProcessBuilder("cmd", "/c", cmd);
                Process process = builder.start();
                BufferedReader stdOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder stdInfo = new StringBuilder();

                // 线程读取标准输出
                Thread stdThread = new Thread(() -> {
                    String line;
                    try {
                        while ((line = stdOutput.readLine()) != null) {
                            stdInfo.append(line).append("\n");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                stdThread.start();

                int exitCode = process.waitFor();
                stdThread.join();

                String info = stdInfo.toString();

                /*// 如果命令执行失败，打印错误
                if (exitCode != 0) {
                    System.out.println("发生异常");
                    break;
                }*/

                // 如果 info 中包含特定内容，结束当前循环
                if (info.contains("[Error] ") || info.contains("type -h or --help for further information")) {
                    nonTimes++;
                    if (nonTimes >= 10) {
                        nonTimes = 0;
                        break;
                    }
                    continue;
                }
                System.out.println(category + i);

                // 将当前 info 写入文件，并使用分隔符区分不同的 info
                writer.append(info).append("\n---\n");
            }
        }

        // 关闭 BufferedWriter
        writer.close();
        return R.ok();
    }

    @RequestMapping("/saga")
    @Transactional(rollbackFor = Exception.class)
    public R saga() throws IOException {
        String sagaCmdPath = "C:\\Users\\lkshi\\Desktop\\saga-9.7.0_x64\\saga_cmd.exe";
        String outputJsonDir = "C:\\Users\\lkshi\\Desktop\\saga_json\\saga.json";
        String inputTxtDir = "C:\\Users\\lkshi\\Desktop\\saga-txt标准\\saga.txt";
        List<String> infoList = getInfoList(inputTxtDir);
        System.out.println("读取的所有 info:");
        List<MethodEntity> sagaMethodList = new ArrayList<>();
        for (String info : infoList) {
            if (info.contains("[Error] select a tool"))
                break;
            int nameIndex = info.indexOf("Name:");
            // 如果找到了 "Name:"，则从该位置开始截取
            if (nameIndex != -1) {
                info.substring(nameIndex);
            } else {
                continue;
            }
            String category = extractField(info, "library     : ");
            String categoryIndex = extractField(info, "library name: ");
            categoryIndex += " " + extractField(info, "identifier  : ");

            String name = extractField(info, "Name:");
            if (name.contains("(GUI)"))
                continue;
            System.out.println("Name: " + name);

            // 提取 copyright 信息
            String copyright = extractField(info, "Author:");
            String description = extractParagraph("Description:", "____________________________", info);
            String references = extractParagraph("References:", "____________________________", info);
            String longDesc = (description + "\n" + (references == null ? "" : references)).replace("\n\n", "\n");
            if (description.length() > 254) {
                description = description.substring(0, 250);
                if (description.lastIndexOf(" ") != -1)
                    description = description.substring(0, description.lastIndexOf(" "));
                description += "...";
            }

            String dataInputParams = "\n" + extractParagraph("Input:", "____________________________", info);
            String dataOutputParams = "\n" + extractParagraph("Output:", "____________________________", info);
            String paramInputParams = "\n" + extractParagraph("Options:", "____________________________", info);

            List<Map<String, Object>> paramList = extractParams(dataInputParams, "DataInput", name);
            paramList.addAll(extractParams(dataOutputParams, "DataOutput", name));
            paramList.addAll(extractParams(paramInputParams, "ParamInput", name));
            MethodEntity method = new MethodEntity();
            method.setUuid("saga");
            method.setName(name);
            method.setCategory(categoryIndex);
            method.setParams(objectMapper.writeValueAsString(paramList));
            method.setCreateTime(new Date());
            method.setExecution("exe");
            method.setCopyright(copyright);
            method.setCreateUserId(1L);
            method.setLongDesc(longDesc);
            method.setDescription(description);
            method.setType("refactoring");

            List<String> tagArray = Collections.singletonList(category);
            List<Long> tagIdList = new ArrayList<>();
            // 对于每一个tag字符串，如果数据库已有，记录id，没有的话增加它再记录id
            for (String tag : tagArray) {
                TagEntity tagEntity = tagService.findByName(tag);
                if (tagEntity == null) {
                    TagEntity containerTag = new TagEntity();
                    containerTag.setName(tag.trim());
                    containerTag.setCreateUserId(1L);
                    containerTag.setCreateTime(new Date());
                    tagService.save(containerTag);
                    tagIdList.add(containerTag.getId());
                } else {
                    tagIdList.add(tagEntity.getId());
                }
            }

            // 保存方法（里面自动保存方法与tag的关系）
            MethodVo methodVo = new MethodVo();
            BeanUtils.copyProperties(method, methodVo);
            methodVo.setParams(objectMapper.readValue(method.getParams(), new TypeReference<List<Map<String, Object>>>() {
            }));
            methodVo.setTagIdList(tagIdList);
            methodService.saveMethod(methodVo);
        }
        // List<String> categoryList = parseStringToList(categories);
        System.out.println(allType);
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputJsonDir, false));
        String json = objectMapper.writeValueAsString(sagaMethodList);
        writer.write(json);
        writer.close();
        return R.ok();
    }

    public List<String> getInfoList(String path) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(path));

        StringBuilder currentInfo = new StringBuilder();
        List<String> infoList = new ArrayList<>();
        String line;

        // 逐行读取文件
        while ((line = reader.readLine()) != null) {
            if (line.equals("---")) {
                if (currentInfo.length() > 0) {
                    infoList.add(currentInfo.toString().trim());
                    currentInfo.setLength(0);  // 清空当前 info
                }
            } else {
                currentInfo.append(line).append("\n");
            }
        }
        if (currentInfo.length() > 0) {
            infoList.add(currentInfo.toString().trim());
        }

        reader.close();
        return infoList;
    }

    @RequestMapping("/differ") // 寻找-C与-h的参数差异，修改到-h中
    public R differ() throws IOException {
        String inputTxtDir = "C:\\Users\\lkshi\\Desktop\\saga-C.txt";
        List<String> infoListC = getInfoList(inputTxtDir);
        inputTxtDir = "C:\\Users\\lkshi\\Desktop\\saga_updated.txt";
        List<String> infoListH = getInfoList(inputTxtDir);
        // 输出读取的每一段 info
        for (int i = 0; i < infoListC.size(); i++) {
            String infoC = infoListC.get(i);
            String infoH = infoListH.get(i);
            if (infoC.contains("[Error] select a tool"))
                break;
            int nameIndex = infoC.indexOf("tool        : ");

            // 如果找到了 "Name:"，则从该位置开始截取
            if (nameIndex != -1) {
                infoC.substring(nameIndex);
            } else {
                continue;
            }
            String nameC = extractField(infoC, "tool        : ");
            if (nameC.contains("(GUI)"))
                continue;

            String dataInputParams = "\n" + extractParagraph("Input:", "____________________________", infoH);
            String dataOutputParams = "\n" + extractParagraph("Output:", "____________________________", infoH);
            String paramInputParams = "\n" + extractParagraph("Options:", "____________________________", infoH);

            List<Map<String, Object>> paramList = extractParams(dataInputParams, "DataInput", nameC);
            paramList.addAll(extractParams(dataOutputParams, "DataOutput", nameC));
            paramList.addAll(extractParams(paramInputParams, "ParamInput", nameC));
            List<String> flagListH = new ArrayList<>();
            for (Map<String, Object> param : paramList) {
                String flag = ((List<String>) param.get("Flags")).get(0);
                flagListH.add(flag);
            }

            String usageInfo = extractParagraph("Usage: ", "\n", infoC);
            List<String> flagListC = extractArguments(usageInfo);
            boolean areEqual = new HashSet<>(flagListC).equals(new HashSet<>(flagListH));
            if (!areEqual) {
                List<String> uniqueToC = new ArrayList<>(flagListC);
                uniqueToC.removeAll(flagListH);
                List<String> uniqueToH = new ArrayList<>(flagListH);
                uniqueToH.removeAll(flagListC);
                List<String> commonToBoth = new ArrayList<>(flagListC);
                commonToBoth.retainAll(flagListH);
                for (String flag : uniqueToH) {
                    String flagWithoutDash = flag.substring(1);
                    int startIndex = infoH.indexOf(flagWithoutDash);
                    if (startIndex != -1) {
                        int prevSectionStart = infoH.lastIndexOf("\n_\n", startIndex);
                        if (prevSectionStart == -1) {
                            prevSectionStart = 0;  // In case the first section doesn't have "\n_\n"
                        }
                        int nextSectionStart = infoH.indexOf("\n_\n", startIndex);
                        if (nextSectionStart == -1) {
                            nextSectionStart = infoH.length();  // If no "\n_\n", remove until the end of string
                        }
                        String toRemove = infoH.substring(prevSectionStart, nextSectionStart);
                        infoH = infoH.replace(toRemove, "");
                    }
                }
                for (String cFlag : uniqueToC) {
                    int usageIndex = infoC.indexOf("Usage:");
                    String info = infoC;
                    if (usageIndex != -1) {
                        int nextLineStart = infoC.indexOf("\n", usageIndex); // 查找 "Usage:" 行的结束位置
                        if (nextLineStart != -1) {
                            info = infoC.substring(nextLineStart + 1).trim();
                        }
                    }
                    int startIndex = info.indexOf(cFlag);
                    if (startIndex != -1) {
                        // Find the next '-' to determine the end of the section
                        int nextFlagIndex = info.indexOf("  -", startIndex + 1);
                        if (nextFlagIndex == -1) {
                            nextFlagIndex = info.length();  // If there's no next flag, take until the end of the string
                        }
                        String result = info.substring(startIndex, nextFlagIndex).trim();
                        String flag = result.split(":")[0].substring(1);
                        String name = result.split(">")[1].split("\n")[0].trim();
                        String typeList = "";
                        if (result.split("\n").length < 2) {
                            /*String type = typeList.substring(result.indexOf("<") + 1, result.indexOf(">"));
                            if (type.equals("double")) {
                                typeList = "floating point number";
                            } else if (type.equals("num")) {
                                typeList = "integer number";
                            }
                            typeList = result.split("\n")[0].trim();*/
                            System.out.println(nameC);
                            break;
                        }
                        typeList = result.split("\n")[1].trim();
                        StringBuilder additionalInfo = new StringBuilder();
                        String[] lines = result.split("\n");
                        for (int k = 2; k < lines.length; k++) {
                            additionalInfo.append(lines[k].trim()).append("\n");
                        }
                        StringBuilder formattedResult = new StringBuilder();
                        formattedResult.append("\n_\n");
                        formattedResult.append(name).append("\n");
                        formattedResult.append(flag).append("\n");
                        formattedResult.append(typeList).append("\n\n");
                        formattedResult.append(additionalInfo.toString().trim().isEmpty() ? "\n" : additionalInfo.toString().trim());
                        infoH += formattedResult.toString();
                    }
                }
                infoListH.set(i, infoH);
            }
        }
        BufferedWriter writer = new BufferedWriter(new FileWriter("C:\\Users\\lkshi\\Desktop\\saga.txt", false));
        for (String s : infoListH) {
            writer.append(s).append("\n---\n");
        }
        writer.close();
        return R.ok();
    }

    // 方法：提取字段的值
    public static String extractField(String section, String field) {
        int fieldIndex = section.indexOf(field);
        if (fieldIndex != -1) {
            // 截取字段值后的部分
            String fieldValue = section.substring(fieldIndex + field.length()).trim();

            // 如果字段值后有换行符，截取直到下一个换行符
            int newlineIndex = fieldValue.indexOf("\n");
            if (newlineIndex != -1) {
                return fieldValue.substring(0, newlineIndex).trim();
            }
            return fieldValue.trim();
        }
        return null;
    }

    public static String extractAndTruncateDescription(String start, String end, String info) {
        // 提取 Description 后面的文本
        int startIndex = info.indexOf(start);
        int endIndex = info.indexOf(end, startIndex);

        // 如果找到描述部分
        if (startIndex != -1 && endIndex != -1) {
            String description = info.substring(startIndex + start.length(), endIndex).trim();

            // 如果描述内容超过 255 字符，按标点符号断句
            if (description.length() > 255) {
                description = truncateAtSentence(description, 255);
            }

            return description;
        }

        return "";  // 未找到 Description 或结束标识符
    }

    // 按句号、逗号、感叹号等标点符号断句
    public static String truncateAtSentence(String text, int maxLength) {
        int endIndex = Math.min(text.length(), maxLength);

        // 在最大长度范围内找到最近的标点符号
        int lastPeriod = text.lastIndexOf(".", endIndex);
        int lastComma = text.lastIndexOf(",", endIndex);
        int lastExclamation = text.lastIndexOf("!", endIndex);

        // 选取最靠近的标点符号
        int lastIndex = Math.max(lastPeriod, Math.max(lastComma, lastExclamation));

        // 如果找到标点符号，则在该位置截断
        if (lastIndex != -1) {
            return text.substring(0, lastIndex + 1).trim();
        }

        // 如果没有找到标点符号，则直接截断
        return text.substring(0, endIndex).trim();
    }

    public static String extractParagraph(String start, String end, String input) {
        // 查找 start 的位置
        int startIndex = input.indexOf(start);

        if (startIndex != -1) {
            // 截取从 start 后的内容
            String section = input.substring(startIndex + start.length()).trim();

            // 查找 end 的位置
            if (end != null && !end.isEmpty()) {
                int endIndex = section.indexOf(end);

                if (endIndex != -1) {
                    // 截取到 end 之前的部分
                    return section.substring(0, endIndex).trim();
                }
            }
            // 如果没有找到 end，返回从 start 后的所有内容
            return section;
        }
        return null;
    }

    public void mergeLists(List<String> typeList, Map<String, Object> allType, String methodName) {
        for (String type : typeList) {
            if (!allType.containsKey(type)) {
                allType.put(type, methodName);
            }
        }
    }

    public static List<String> extractArguments(String usage) {
        List<String> arguments = new ArrayList<>();

        // 定位每个'-'后到下一个空格之间的部分
        int startIndex = 0;
        while ((startIndex = usage.indexOf('-', startIndex)) != -1) {
            // 查找'-'后的下一个空格，确定参数的边界
            int endIndex = usage.indexOf(' ', startIndex);
            if (endIndex == -1) {
                endIndex = usage.length();  // 如果没有空格，表示该选项是最后一个
            }
            String argument = usage.substring(startIndex, endIndex);
            arguments.add(argument);
            startIndex = endIndex;  // 移动到下一个搜索起始位置
        }

        return arguments;
    }

    @RequestMapping("/remove") // 寻找-C与-h的方法差异,以-C为准，当然会遗留可视化的最后那几个方法有待删除
    public R removeExtra() throws IOException {
        String inputTxtDir = "C:\\Users\\lkshi\\Desktop\\saga-C.txt";
        List<String> infoListC = getInfoList(inputTxtDir);
        inputTxtDir = "C:\\Users\\lkshi\\Desktop\\saga_updated.txt";
        List<String> infoListH = getInfoList(inputTxtDir);
        int j = 0;
        for (int i = 0; i < infoListC.size(); i++) {
            String infoC = infoListC.get(i);
            String nameC = extractField(infoC, "tool        : ");
            String infoH = infoListH.get(j);
            String nameH = extractField(infoH, "tool        : ");
            while (!nameC.equals(nameH)) {
                infoListH.remove(j);
                infoH = infoListH.get(j);
                nameH = extractField(infoH, "tool        : ");
            }
            j++;
        }
        BufferedWriter writer = new BufferedWriter(new FileWriter(inputTxtDir, false));
        for (String s : infoListH) {
            writer.append(s).append("\n---\n");
        }
        writer.close();
        return R.ok();
    }

    // 提取 Options 中的参数信息
    public List<Map<String, Object>> extractParams(String input, String type, String methodName) {
        List<Map<String, Object>> params = new ArrayList<>();

        List<String> parts = splitString(input);
        for (String part : parts) {
            part = part.trim(); // 去掉两端的空白字符

            if (!part.isEmpty()) {
                // 按行分割每个参数块
                String[] lines = part.split("\n");
                if (lines.length >= 3) {
                    String name = lines[0].trim();
                    String flags = "-" + lines[1].trim();
                    List<String> typeList = Arrays.asList(lines[2].trim().split(",")).stream().map(String::trim).collect(Collectors.toList());
                    mergeLists(typeList, allType, methodName);
                    String description = "";
                    if (lines.length > 3)
                        description = lines[3].trim();
                    if (StringUtil.isNullOrEmpty(description)) {
                        description = name;
                    }

                    // 创建 Param 对象
                    Map<String, Object> param = new HashMap<>();
                    param.put("Name", name);
                    param.put("Flags", Collections.singletonList(flags));
                    param.put("Type", type);

                    // 添加 Constraints 信息
                    String constraints = "";
                    for (int i = 4; i < lines.length; i++) {
                        constraints += lines[i].trim() + "\n";
                    }
                    if (constraints.contains("Default:")) {
                        Object default_value = parseDefaultValue(extractParagraph("Default:", "\n_", constraints).trim());
                        param.put("default_value", default_value);
                    } else param.put("default_value", null);
                    if (typeList.contains("optional") || param.get("default_value") != null) {
                        param.put("Optional", true);
                    } else param.put("Optional", false);

                    if (typeList.contains("input")) {
                        param.put("Type", "DataInput");
                        param.put("parameter_type", getInputType(typeList));
                    } else if (typeList.contains("output")) {
                        param.put("Type", "DataOutput");
                        param.put("parameter_type", getOutputType(typeList));
                    } else {
                        if (typeList.contains("floating point number") || typeList.contains("degree") || typeList.contains("value range"))
                            param.put("parameter_type", "Float");
                        else if (typeList.contains("integer number") || typeList.contains("color"))
                            param.put("parameter_type", "Integer");
                        else if (typeList.contains("boolean"))
                            param.put("parameter_type", "Boolean");
                        else if (typeList.contains("date") || typeList.contains("text") || typeList.contains("colors") || typeList.contains("long text"))
                            param.put("parameter_type", "String");
                        else if (typeList.contains("table field")) {
                            Map<String, Object> tf = new HashMap<>();
                            tf.put("VectorAttributeField", new ArrayList<>());
                            param.put("parameter_type", tf);
                        } else if (typeList.contains("table fields") || typeList.contains("choices") || typeList.contains("parameters"))
                            param.put("parameter_type", "StringOrNumber");
                        else if (typeList.contains("data type") || typeList.contains("choice")) {
                            String choices = extractParagraph("Available Choices:\n", "\nDefault:", constraints);
                            Map<String, Object> subType = new HashMap<>();
                            subType.put("OptionList", parseChoices2ListString(choices));
                            param.put("parameter_type", subType);
                        } else if (typeList.contains("input")) {
                            param.put("Type", "DataInput");
                            param.put("parameter_type", getInputType(typeList));
                        } else if (typeList.contains("output")) {
                            param.put("Type", "Data Output");
                            param.put("parameter_type", getOutputType(typeList));
                        }
                        // static table
                    }
                    String choices = extractParagraph("Available Choices:\n", "\n_\n", constraints);
                    String defaultVal = extractParagraph("Default: ", "\n_\n", constraints);
                    if (!StringUtil.isNullOrEmpty(constraints)) {
                        description += "\n";
                        description += constraints.replace("Available Choices:\n" + choices, "").replace("Default: " + defaultVal, "");
                    }
                    description.replace("\n\n", "\n");
                    param.put("Description", description);
                    // 将提取的参数加入到列表中
                    params.add(param);
                }
            }
        }

        return params;
    }

    public static boolean isFileList(List<String> typeList) {
        String regex = "(?i).*list.*"; // "list" 不区分大小写
        int matchingIndex = -1;
        for (int i = 0; i < typeList.size(); i++) {
            if (Pattern.matches(regex, typeList.get(i))) {
                matchingIndex = i; // 找到匹配项，记录索引
                break; // 找到第一个匹配项后退出循环
            }
        }
        // 输出结果
        return matchingIndex != -1;
    }

    public static Integer isFileList(List<String> typeList, boolean isIndex) {
        String regex = "(?i).*list.*"; // "list" 不区分大小写
        int matchingIndex = -1;
        for (int i = 0; i < typeList.size(); i++) {
            if (Pattern.matches(regex, typeList.get(i))) {
                matchingIndex = i; // 找到匹配项，记录索引
                break; // 找到第一个匹配项后退出循环
            }
        }
        // 输出结果
        return matchingIndex;
    }

    public static List<String> parseChoices2ListString(String choices) {
        // 修改后的正则表达式，去除数字标识符，只匹配内容
        String regex = "\\[\\d+\\](.*?)((?=\\[\\d+\\])|(?=$))";
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(choices);

        // 存储结果的List
        List<String> result = new ArrayList<>();

        // 提取匹配的项
        while (matcher.find()) {
            result.add(matcher.group(1).trim()); // 只提取内容部分
        }
        return result;
    }

    public static Object getInputType(List<String> typeList) {
        Map<String, Object> existingFileType = new HashMap<>();
        if (typeList.contains("directory")) {
            return "Directory";
        } else if (typeList.contains("shapes list")) {
            Map<String, Object> subType = new HashMap<>();
            subType.put("Vector", "Any");
            existingFileType.put("FileList", subType);
        } else if (isFileList(typeList))
            existingFileType.put("FileList", capitalizeWords(typeList.get(isFileList(typeList, true)).split("list")[0].trim()));
        else if (typeList.contains("shapes")) {
            Map<String, Object> subType = new HashMap<>();
            subType.put("Vector", "Any");
            existingFileType.put("ExistingFile", subType);
        } else {
            // grid collection, static table, table, data object Grid
            existingFileType.put("ExistingFile", capitalizeWords(typeList.get(0)));
        }
        return existingFileType;
    }

    public static Object getOutputType(List<String> typeList) {
        Map<String, Object> newFileType = new HashMap<>();
        if (typeList.contains("directory")) {
            return "Directory";
        } else if (typeList.contains("shapes list") || typeList.contains("shapes")) {
            Map<String, Object> subType = new HashMap<>();
            subType.put("Vector", "Any");
            newFileType.put("NewFile", subType);
        } else {
            // grid collection, static table, table, data object Grid
            newFileType.put("NewFile", capitalizeWords(typeList.get(0)));
        }
        return newFileType;
    }

    public static String capitalizeWords(String sentence) {
        // 将句子分割成单词
        String[] words = sentence.split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            // 将每个单词的首字母大写，其他字母小写
            if (word.length() > 0) {
                result.append(word.substring(0, 1).toUpperCase())  // 首字母大写
                        .append(word.substring(1).toLowerCase())   // 剩余字母小写
                        .append(" ");
            }
        }
        return result.toString().trim();
    }

    public static List<String> splitString(String input) {
        List<String> result = new ArrayList<>();

        // 匹配前后都有换行符的 `_`
        String regex = "(?<=\n)_\n";
        String[] parts = input.split(regex);

        // 将分割结果添加到列表中
        result.addAll(Arrays.asList(parts));

        return result;
    }

    public static Object parseDefaultValue(String defaultValue) {
        if (defaultValue == null || defaultValue.isEmpty()) {
            return null; // 处理空值
        }

        defaultValue = defaultValue.trim();

        // 检查是否为布尔值
        if ("true".equalsIgnoreCase(defaultValue) || "false".equalsIgnoreCase(defaultValue)) {
            return Boolean.parseBoolean(defaultValue);
        }

        // 检查是否为数字（整数）
        try {
            return Integer.parseInt(defaultValue);  // 可以根据需要修改为 Long.parseLong()
        } catch (NumberFormatException e) {
            // 不是整数
        }

        // 检查是否为浮动数值
        try {
            return Double.parseDouble(defaultValue);  // 可以根据需要修改为 Float.parseFloat()
        } catch (NumberFormatException e) {
            // 不是浮动数值
        }

        // 默认返回字符串类型
        return defaultValue;
    }

    public static List<String> parseStringToList(String input) {
        // 按行分割输入
        String[] lines = input.split("\n");

        List<String> result = new ArrayList<>();

        for (String line : lines) {
            // 去掉每行的前后空格，然后去掉前面的 '-'
            String trimmed = line.trim().replaceFirst("^\\s*-\\s*", "");

            // 如果处理后的字符串不为空，则加入到结果列表
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }

        return result;
    }

    /**
     * @param
     * @return
     * @author: Lingkai Shi
     * @description: 临时接口, 用于创建Whitebox方法列表
     * @date: 8/13/2024 8:12 PM
     */
    @RequestMapping("/createUUID")
    public R tmpUUID() {

        String folderPath = "D:\\24PostGraduate\\opengmp\\methods";  // 替换为你的JSON文件夹路径
        String outputPath = "D:\\24PostGraduate\\opengmp\\dge\\methods";  // 替换为你想创建UUID文件夹的路径
        // 生成UUID
        String uuid = UUID.randomUUID().toString();
        File folder = new File(folderPath);
        File[] listOfJsonFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        if (listOfJsonFiles != null) {
            for (File file : listOfJsonFiles) {
                if (file.isFile()) {
                    try {
                        // 读取JSON文件内容
                        String content = new String(Files.readAllBytes(file.toPath()));
                        // String2Json
                        ObjectMapper objectMapper = new ObjectMapper();
                        Map<String, Object> resultMap = objectMapper.readValue(content, Map.class);

                        // 调用已封装的方法保存数据
                        MethodEntity method = new MethodEntity();
                        method.setCreateUserId(getUserId());
                        method.setUuid(uuid);
                        method.setName(resultMap.get("Name").toString());
                        method.setCopyright(resultMap.get("Copyright").toString());
                        method.setDescription(resultMap.get("Description").toString());
                        method.setParams(objectMapper.writeValueAsString(resultMap.get("Parameters")));
                        // TODO 保存tag/method与tag的关系/注意tag唯一值约束
                        List<String> tagArray = (List<String>) resultMap.get("Tags");
                        List<Long> tagIdList = new ArrayList<>();
                        // 对于每一个tag字符串，如果数据库已有，记录id，没有的话增加它再记录id
                        for (String tag : tagArray) {
                            TagEntity tagEntity = tagService.findByName(tag);
                            if (tagEntity == null) {
                                TagEntity containerTag = new TagEntity();
                                containerTag.setName(tag.trim());
                                containerTag.setCreateUserId(getUserId());
                                containerTag.setCreateTime(new Date());
                                tagService.save(containerTag);
                                tagIdList.add(containerTag.getId());
                            } else {
                                tagIdList.add(tagEntity.getId());
                            }
                        }
                        // 保存方法（里面自动保存方法与tag的关系）
                        MethodVo methodVo = new MethodVo();
                        BeanUtils.copyProperties(method, methodVo);
                        methodVo.setParams(objectMapper.readValue(method.getParams(), new TypeReference<List<Map<String, Object>>>() {
                        }));
                        methodVo.setTagIdList(tagIdList);
                        methodService.saveMethod(methodVo);

                        // 创建UUID文件夹
                        File uuidFolder = new File(outputPath + File.separator + uuid);
                        if (!uuidFolder.exists()) {
                            uuidFolder.mkdir();
                        }

                        // 复制原文件到UUID文件夹中（可选）
                        // Files.copy(file.toPath(), Paths.get(uuidFolder.getAbsolutePath(), file.getName()));

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            System.out.println("数据处理完成！");
            return R.ok();
        } else {
            return R.error("指定的文件夹中没有找到JSON文件。");
        }
    }

    @RequestMapping("/updateParamType")
    public R updateParamType() throws JsonProcessingException {
        List<MethodEntity> methodList = methodService.list();
        for (MethodEntity method : methodList) {
            List<Map<String, Object>> params = objectMapper.readValue(method.getParams(),
                    new TypeReference<List<Map<String, Object>>>() {
                    });
            for (int i = 0; i < params.size(); i++) {
                params.get(i).remove("type");
                // 获取 parameter_type 属性并分类处理
                Object parameterTypeObj = params.get(i).get("parameter_type");
                Map<String, Object> paramSpecs = params.get(i);
                String parameterType;
                // 如果parameterType是字符串
                if (parameterTypeObj instanceof String) {
                    // 字符串可能有Boolean、Float、Integer
                    parameterType = (String) parameterTypeObj;
                    // 直接处理字符串
                    if (parameterType.equals("Boolean") || parameterType.equals("Float")
                            || parameterType.equals("Integer") || parameterType.equals("String")
                            || parameterType.equals("StringOrNumber")) {
                        params.get(i).put("Type", "ParamInput");
                    } else if (parameterType.equals("Directory")) {
                        // 判断是输入还是输出Directory?
                        if (((List<String>) paramSpecs.get("Flags")).get(0).equals("--indir")) {
                            // 设置输入路径为工作路径，具体是拷贝用户选择的源文件夹内的所有文件到工作路径
                            // 在程序执行完后、拷贝最终结果至用户数据文件夹之前把这些文件删除
                            params.get(i).put("Type", "DataInput");
                        } else if (((List<String>) paramSpecs.get("Flags")).get(0).equals("--outdir")) {
                            // DirectoryOutputFlagInfo
                            // 获取 String
                            params.get(i).put("Type", "DataOutput");
                        }
                    }
                } // 如果parameterType是json
                else if (parameterTypeObj instanceof Map) {
                    // parameter_type 是一个JSON对象（反序列化后的Map）
                    // ExistingFile/NewFile
                    Map<String, Object> parameterTypeMap = (Map<String, Object>) parameterTypeObj;
                    if (parameterTypeMap.containsKey("ExistingFile")
                            || parameterTypeMap.containsKey("ExistingFileOrFloat")
                            || parameterTypeMap.containsKey("Vector") || parameterTypeMap.containsKey("FileList")) {
                        // 先判断是不是file
                        // String existingFileId = (String) params.get("val" + i);
                        params.get(i).put("Type", "DataInput");
                    } else if (parameterTypeMap.containsKey("NewFile")) {
                        // TODO 暂时不会有FileList
                        // 获取 NewFile
                        params.get(i).put("Type", "DataOutput");
                    } else if (parameterTypeMap.containsKey("OptionList")) {
                        // TODO 暂时不会有FileList
                        params.get(i).put("Type", "ParamInput");
                    } else if (parameterTypeMap.containsKey("VectorAttributeField")) {
                        params.get(i).put("Type", "ParamInput");
                    } else {
                        // CSV等
                        params.get(i).put("Type", "DataInput");
                    }
                    // 处理parameterTypeMap
                    parameterType = objectMapper.writeValueAsString(parameterTypeMap);
                } // 其他数据类型Whitebox没有
                else {
                    // 其他可能的类型处理，例如null值
                    parameterType = "Unknown type";
                    params.get(i).put("Type", "UnknownType");
                }
            }
            method.setParams(objectMapper.writeValueAsString(params));
            methodService.saveOrUpdate(method);
        }
        return R.ok();
    }

    @RequestMapping("/validate")
    public R validate(@RequestBody Map<String, Object> json) throws JsonProcessingException {
        List<ValidationMessage> list = methodService.validate(json);
        if (list.isEmpty()) {
            return R.ok();
        } else {
            return R.error(list.toString());
        }
    }

}
