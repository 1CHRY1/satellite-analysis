package com.ogms.dge.container.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ogms.dge.container.modules.data.dto.FileNodeDto;
import com.ogms.dge.container.modules.method.entity.MethodEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @name: FileUtils
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 8/19/2024 10:34 PM
 * @version: 1.0
 */
public class FileUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String calculateFileMd5(File file) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] dataBytes = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, bytesRead);
            }
        }
        byte[] digestBytes = md.digest();
        BigInteger bigInt = new BigInteger(1, digestBytes);
        return bigInt.toString(16).toUpperCase();
    }

    public static String getFileSuffix(String fileName) {
        if (fileName == null)
            return "";
        if (fileName.isEmpty())
            return "";
        int lastDotIndex = fileName.lastIndexOf('.');
        return (lastDotIndex == -1) ? "" : fileName.substring(lastDotIndex + 1);
    }

    public static String getFileNameWithoutSuffix(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return (lastDotIndex == -1) ? fileName : fileName.substring(0, lastDotIndex);
    }

    public static void copyDirectory(Path sourceDirectory, Path targetDirectory) throws IOException {
        // 创建目标目录
        Files.createDirectories(targetDirectory);

        // 遍历源目录中的所有文件和子目录
        Files.walk(sourceDirectory)
                .forEach(sourcePath -> {
                    try {
                        Path targetPath = targetDirectory.resolve(sourceDirectory.relativize(sourcePath));
                        // 如果是目录，创建目录
                        if (Files.isDirectory(sourcePath)) {
                            Files.createDirectories(targetPath);
                        } else {
                            // 如果是文件，复制文件
                            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    // 方法：将文件拷贝到目标文件夹，并返回目标文件夹中的文件列表
    public static List<File> copyFilesToDirectory(List<File> files, String targetDir) throws IOException {
        List<File> targetFiles = new ArrayList<>();
        for (File file : files) {
            Path targetPath = Paths.get(targetDir, file.getName());
            Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            targetFiles.add(targetPath.toFile());
        }
        return targetFiles;
    }

    // 方法：删除目标文件夹中的文件
    public static void deleteFilesFromDirectory(List<File> files) throws IOException {
        for (File file : files) {
            Files.deleteIfExists(file.toPath());
        }
    }

    // 方法：删除目标文件夹中的所有内容，除了 exceptFiles
    public static void deleteAllExcept(File directory, List<File> exceptFiles) throws IOException {
        if (directory != null && directory.isDirectory()) {
            // 遍历文件夹中的所有文件和子文件夹
            for (File file : directory.listFiles()) {
                // 检查当前文件是否在 exceptFiles 列表中
                if (exceptFiles == null || !exceptFiles.contains(file)) {
                    // 递归删除文件夹
                    if (file.isDirectory()) {
                        deleteAllExcept(file, exceptFiles); // 递归调用
                    }
                    // 删除文件或空文件夹
                    Files.deleteIfExists(file.toPath());
                }
            }
        }
    }

    public static List<File> getAllFilesFromDirectory(String directoryPath) {
        List<File> fileList = new ArrayList<>();
        File directory = new File(directoryPath);

        if (directory.exists() && directory.isDirectory()) {
            // Call the recursive method to add files
            addFilesRecursively(directory, fileList);
        } else {
            System.out.println("The provided path is not a valid directory.");
        }

        return fileList;
    }

    private static void addFilesRecursively(File directory, List<File> fileList) {
        // Get all the files and directories in the current directory
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // If it's a directory, recurse into it
                    addFilesRecursively(file, fileList);
                } else {
                    // If it's a file, add it to the list
                    fileList.add(file);
                }
            }
        }
    }

    public static boolean deleteDirectory(String directoryPath) {
        File directory = new File(directoryPath);

        if (!directory.exists()) {
            System.out.println("目录不存在：" + directoryPath);
            return false;
        }

        // 确保是一个目录
        if (!directory.isDirectory()) {
            System.out.println("指定的路径不是目录：" + directoryPath);
            return false;
        }

        // 删除目录下的所有文件和子目录
        boolean success = deleteContents(directory);

        // 最后删除目录本身
        if (success) {
            success = directory.delete();
            if (!success) {
                System.out.println("无法删除目录：" + directoryPath);
            }
        }

        return success;
    }

    private static boolean deleteContents(File directory) {
        File[] files = directory.listFiles();
        if (files == null) {
            return true; // 空目录，返回true
        }

        boolean success = true;
        for (File file : files) {
            if (file.isDirectory()) {
                // 递归删除子目录
                success &= deleteContents(file);
            }
            // 删除文件或空目录
            if (!file.delete()) {
                System.out.println("无法删除文件或目录：" + file.getAbsolutePath());
                success = false;
            }
        }
        return success;
    }

    public static List<File> unzip(String zipFilePath, File destDir) throws IOException {
        List<File> fileList = new ArrayList<>();

        // 打开ZIP文件
        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry = zipIn.getNextEntry();

            // 遍历ZIP中的每个条目
            while (entry != null) {
                File filePath = new File(destDir, entry.getName());

                if (!entry.isDirectory()) {
                    // 如果条目不是目录，解压文件
                    extractFile(zipIn, filePath);
                    fileList.add(filePath);  // 将解压后的文件添加到列表中
                } else {
                    // 如果条目是目录，创建目录
                    filePath.mkdirs();
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }

        return fileList;  // 返回解压后的文件列表
    }

    private static void extractFile(ZipInputStream zipIn, File filePath) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
            byte[] bytesIn = new byte[4096];
            int read;
            while ((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }
    }

    // 方法：找到第一个具有多个扩展名中的任一扩展名的文件
    public static File findFirstFileWithExtensions(File folder, String[] extensions) {
        File[] files = folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                String lowerCaseName = name.toLowerCase();
                for (String ext : extensions) {
                    if (lowerCaseName.endsWith("." + ext)) {
                        return true;
                    }
                }
                return false;
            }
        });

        if (files != null && files.length > 0) {
            return files[0]; // 返回第一个匹配的文件
        } else {
            return null; // 没有找到文件
        }
    }

    // 方法：找到第一个具有多个扩展名中的任一扩展名的文件
    public static File findFirstFileWithName(File folder, String fileName) {
        File[] files = folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.equals(fileName);
            }
        });

        if (files != null && files.length > 0) {
            return files[0]; // 返回第一个匹配的文件
        } else {
            return null; // 没有找到文件
        }
    }

    // 解析 JSON 文件并将值赋给 MethodEntity
    public static MethodEntity parseJsonFile(File jsonFile) throws IOException {
        try {
            // 创建 ObjectMapper 实例
            JsonNode rootNode = objectMapper.readTree(jsonFile);  // 读取 JSON 文件

            // 创建 MethodEntity 实例
            MethodEntity methodEntity = new MethodEntity();

            // 解析并赋值
            methodEntity.setName(rootNode.path("Name").asText(null));
            methodEntity.setDescription(rootNode.path("Description").asText(null));
            methodEntity.setCopyright(rootNode.path("Copyright").asText(null));
            methodEntity.setExecution(rootNode.path("Execution").asText(null));
            methodEntity.setCategory(null);
            methodEntity.setType(rootNode.path("Type").asText(null));

            // 解析 Parameters 数组并将其转为字符串
            JsonNode parametersNode = rootNode.path("Parameters");
            if (!validateParameters(objectMapper.writeValueAsString(parametersNode))) {
                return null;
            }
            if (parametersNode.isArray()) {
                // 将 JSON 数组转换为字符串
                String paramsAsString = objectMapper.writeValueAsString(parametersNode);
                methodEntity.setParams(paramsAsString);  // 设置为字符串
            } else {
                methodEntity.setParams(null);
            }
            return methodEntity;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @param parameters
     * @return boolean
     * @author: Lingkai Shi
     * @description: 验证用户上传的json实体
     * @date: 9/6/2024 11:13 PM
     */
    public static boolean validateParameters(String parameters) throws JsonProcessingException {
        // 创建 ObjectMapper 实例
        List<Map<String, Object>> params = objectMapper.readValue(parameters, new TypeReference<List<Map<String, Object>>>() {
        });
        for (int i = 0; i < params.size(); i++) {
            if (!(params.get(i).containsKey("Name") || params.get(i).containsKey("Flags") || params.get(i).containsKey("Description") || params.get(i).containsKey("parameter_type") || params.get(i).containsKey("default_value") || params.get(i).containsKey("Optional"))) {
                return false;
            }
            Object parameterTypeObj = params.get(i).get("parameter_type");
            String parameterType;
            if (parameterTypeObj instanceof String) {
                // 字符串可能有Boolean、Float、Integer等
                parameterType = (String) parameterTypeObj;
                if (!(parameterType.equals("Boolean") || parameterType.equals("Float")
                        || parameterType.equals("Integer") || parameterType.equals("String")
                        || parameterType.equals("StringOrNumber") || parameterType.equals("Directory"))) {
                    return false;
                }
            } else if (parameterTypeObj instanceof Map) {
                // parameter_type 是一个JSON对象（反序列化后的Map）
                // ExistingFile/NewFile
                Map<String, Object> parameterTypeMap = (Map<String, Object>) parameterTypeObj;
                if (!(parameterTypeMap.containsKey("ExistingFile")
                        || parameterTypeMap.containsKey("ExistingFileOrFloat")
                        || parameterTypeMap.containsKey("Vector")
                        || parameterTypeMap.containsKey("FileList")
                        || parameterTypeMap.containsKey("OptionList")
                        || parameterTypeMap.containsKey("NewFile")
                        || parameterTypeMap.containsKey("VectorAttributeField"))) {
                    return false;
                } else {
                    Object fileTypeObj;
                    if (parameterTypeMap.containsKey("FileList") || parameterTypeMap.containsKey("ExistingFile") || parameterTypeMap.containsKey("ExistingFileOrFloat")) {
                        if (parameterTypeMap.containsKey("FileList")) {
                            fileTypeObj = parameterTypeMap.get("FileList");
                        } else if (parameterTypeMap.containsKey("ExistingFile")) {
                            fileTypeObj = parameterTypeMap.get("ExistingFile");
                        } else {
                            fileTypeObj = parameterTypeMap.get("ExistingFileOrFloat");
                        }
                        // FileList或ExistingFile里面可能还有json
                        if (fileTypeObj instanceof Map) {
                            Map<String, Object> fileTypeMap = (Map<String, Object>) fileTypeObj;
                            if (!(fileTypeMap.containsKey("Vector")
                                    || fileTypeMap.containsKey("RasterAndVector"))) {
                                return false;
                            }
                        } else {
                            // 普通文件
                        }
                    } else if (parameterTypeMap.containsKey("NewFile")) {
                        // TODO 暂时不会有FileList
                        fileTypeObj = parameterTypeMap.get("NewFile");
                        if (fileTypeObj instanceof Map) {
                            // Vector会自动加扩展名
                            Map<String, Object> fileTypeMap = (Map<String, Object>) fileTypeObj;
                            if (!(fileTypeMap.containsKey("Vector")
                                    || fileTypeMap.containsKey("RasterAndVector"))) {
                                return false;
                            }
                        }
                    } else if (!(parameterTypeMap.containsKey("OptionList") || parameterTypeMap.containsKey("VectorAttributeField"))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static FileNodeDto buildFileTree(File directory, String basePath) {
        FileNodeDto node = new FileNodeDto();
        // 检查 basePath 是否比当前路径短
        String fullPath = directory.getPath();
        String relativePath;
        if (fullPath.length() > basePath.length()) {
            relativePath = fullPath.substring(basePath.length() + 1);
        } else {
            relativePath = fullPath; // 如果异常，保留完整路径作为 fallback
        }
        node.setName(directory.getName());
        node.setPath(relativePath);
        node.setDirectory(directory.isDirectory());

        if (directory.isDirectory()) {
            List<FileNodeDto> children = new ArrayList<>();
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    children.add(buildFileTree(file, basePath));
                }
            }
            node.setChildren(children);
        }
        return node;
    }
}
