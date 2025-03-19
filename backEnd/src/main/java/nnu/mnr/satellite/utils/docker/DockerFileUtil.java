package nnu.mnr.satellite.utils.docker;

import nnu.mnr.satellite.enums.common.FileType;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

    public static Date parseFileLastModified(String [] fileDetails){
        try {
            if (fileDetails.length >= 9) {
                // 获取最后修改时间字符串
                String lastModifiedDateStr = fileDetails[5] + " " + fileDetails[6] + " " + fileDetails[7];

                // 定义 SimpleDateFormat 来解析日期字符串
                SimpleDateFormat dateFormat;

                if (fileDetails[7].contains(":")) {
                    // 时间格式是 `月 日 时间` (e.g., "Aug 15 12:34")
                    dateFormat = new SimpleDateFormat("MMM dd HH:mm", Locale.ENGLISH);
                } else {
                    // 时间格式是 `月 日 年` (e.g., "Aug 15 2023")
                    dateFormat = new SimpleDateFormat("MMM dd yyyy", Locale.ENGLISH);
                }

                // 将解析的时间字符串转化为 Date 对象
                Date lastModifiedDate = dateFormat.parse(lastModifiedDateStr);

                // 如果解析的是当年的时间，加上当前年份
                if (fileDetails[7].contains(":")) {
                    // 获取当前年份
                    SimpleDateFormat currentYearFormat = new SimpleDateFormat("yyyy");
                    String currentYear = currentYearFormat.format(new Date());
                    lastModifiedDateStr = fileDetails[5] + " " + fileDetails[6] + " " + currentYear + " " + fileDetails[7];
                    dateFormat = new SimpleDateFormat("MMM dd yyyy HH:mm", Locale.ENGLISH);
                    lastModifiedDate = dateFormat.parse(lastModifiedDateStr);
                }

                return lastModifiedDate;
            }
        } catch (ParseException e) {
//            e.printStackTrace();
            return null;
        }
        return null;
    }

}
