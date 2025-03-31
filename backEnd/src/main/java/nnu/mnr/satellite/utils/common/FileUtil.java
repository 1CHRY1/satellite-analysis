package nnu.mnr.satellite.utils.common;

import com.alibaba.fastjson2.JSONObject;
import org.springframework.http.MediaType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/24 16:00
 * @Description:
 */

public class FileUtil {

    public static MediaType setMediaType(String type) {
        return switch (type.toLowerCase()) {
            // Office
            case "xlsx" -> MediaType.valueOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            case "xls" -> MediaType.valueOf("application/vnd.ms-excel");
            case "pptx" -> MediaType.valueOf("application/vnd.openxmlformats-officedocument.presentationml.presentation");
            case "ppt" -> MediaType.valueOf("application/vnd.ms-powerpoint");
            case "docx" -> MediaType.valueOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            case "doc" -> MediaType.valueOf("application/msword");
            // Common
            case "csv" -> MediaType.valueOf("text/csv");
            case "pdf" -> MediaType.valueOf("application/pdf");
            case "zip" -> MediaType.valueOf("application/zip");
            case "rar" -> MediaType.valueOf("application/x-rar-compressed");
            case "txt" -> MediaType.valueOf("text/plain");
            // Image
            case "gif" -> MediaType.valueOf("image/gif");
            case "jpg", "jpeg" -> MediaType.valueOf("image/jpeg");
            case "png" -> MediaType.valueOf("image/png");
            // media
            case "mp3" -> MediaType.valueOf("audio/mpeg");
            case "wav" -> MediaType.valueOf("audio/wav");
            case "wma" -> MediaType.valueOf("audio/x-ms-wma");
            // vedio
            case "mp4" -> MediaType.valueOf("video/mp4");
            case "avi" -> MediaType.valueOf("video/x-msvideo");
            case "wmv" -> MediaType.valueOf("video/x-ms-wmv");
            case "mpg" -> MediaType.valueOf("video/mpeg");
            // code
            case "py" -> MediaType.valueOf("text/x-python");
            // spatial
            case "shp" -> MediaType.valueOf("application/x-shapefile"); // Shapefile，无官方 MIME 类型
            case "tif", "tiff" -> MediaType.valueOf("image/tiff");     // GeoTIFF，通常用 image/tiff
            case "geojson" -> MediaType.valueOf("application/geo+json"); // GeoJSON
            case "kml" -> MediaType.valueOf("application/vnd.google-earth.kml+xml"); // KML
            case "kmz" -> MediaType.valueOf("application/vnd.google-earth.kmz");     // KMZ
            case "gml" -> MediaType.valueOf("application/gml+xml");     // Geography Markup Language
            case "gdb" -> MediaType.valueOf("application/x-filegdb");   // Esri File Geodatabase，无官方 MIME

            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

    public static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFolder(file);
                } else {
                    file.delete();
                }
            }
        }
        folder.delete();
    }

    public static void saveAsJsonFile(JSONObject geojson, String filePath) throws IOException {
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(geojson.toString());
            fileWriter.flush();
        }
    }

}
