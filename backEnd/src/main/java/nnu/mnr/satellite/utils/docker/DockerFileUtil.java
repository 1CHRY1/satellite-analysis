package nnu.mnr.satellite.utils.docker;

import com.github.dockerjava.core.MediaType;
import nnu.mnr.satellite.enums.common.FileType;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/18 20:29
 * @Description:
 */
public class DockerFileUtil {

    public static FileType setDataType(String suffix) {
        return switch (suffix) {
            case "xlsx" -> FileType.excel;
            case "xls" -> FileType.excel;
            case "pptx" -> FileType.ppt;
            case "ppt" -> FileType.ppt;
            case "docx" -> FileType.word;
            case "doc" -> FileType.word;
            case "csv" -> FileType.csv;
            case "pdf" -> FileType.pdf;
            case "zip" -> FileType.zip;
            case "rar" -> FileType.zip;
            case "txt" -> FileType.txt;
            case "gif" -> FileType.gif;
            case "shp" -> FileType.shp;
            case "tif" -> FileType.tif;
            case "tiff" -> FileType.tiff;
            case "TIF" -> FileType.TIF;
            case "mp3" -> FileType.audio;
            case "wav" -> FileType.audio;
            case "wma" -> FileType.audio;
            case "mp4" -> FileType.video;
            case "avi" -> FileType.video;
            case "wmv" -> FileType.video;
            case "mpg" -> FileType.video;
            case "jpg" -> FileType.image;
            case "png" -> FileType.image;
            case "jpeg" -> FileType.image;
            case "py" -> FileType.python;
            default -> FileType.unknown;
        };
    }

    public static Date parseFileLastModified(String[] fileDetails) {
        try {
            if (fileDetails.length >= 8) {
                // 获取最后修改时间字符串
                String lastModifiedDateStr = fileDetails[5] + " " + fileDetails[6].split("\\.")[0];

                // 定义 SimpleDateFormat 来解析日期字符串
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
                // 将解析的时间字符串转化为 Date 对象
                return dateFormat.parse(lastModifiedDateStr);
            }
        } catch (ParseException e) {
            // e.printStackTrace();
            return null;
        }
        return null;
    }

    public static String getImageByName(String env) {
        return switch (env) {
            case "Python3_9" -> "python39:satellite";
            case "Python3_8" -> "python38:latest";
            case "Python2_7" -> "python27:latest";
            default -> "";
        };
    }

}
