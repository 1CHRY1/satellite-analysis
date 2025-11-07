package nnu.mnr.satellite.utils.dt;

import com.alibaba.fastjson2.JSONObject;
import io.minio.*;
import io.minio.admin.MinioAdminClient;
import io.minio.admin.messages.BucketUsageInfo;
import io.minio.admin.messages.info.MemStats;
import io.minio.admin.messages.info.Message;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import jakarta.servlet.http.HttpServletResponse;
import nnu.mnr.satellite.model.pojo.modeling.MinioProperties;
import nnu.mnr.satellite.model.vo.admin.StatsReportVO;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import io.minio.admin.messages.info.ServerProperties;

import java.io.*;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/12 15:26
 * @Description:
 */

@Component
public class MinioUtil {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioAdminClient minioAdminClient;

    @Autowired
    private MinioProperties configuration;

    /**
     * 判断bucket是否存在，不存在则创建
     */
    public boolean existBucket(String bucketName) {
        boolean exists;
        try {
            exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                exists = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            exists = false;
        }
        return exists;
    }

    /**
     * 删除bucket
     */
    public Boolean removeBucket(String bucketName) {
        try {
            minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    /**
     * 上传文件
     */
    public void upload(MultipartFile file, String fileName, String bucket) {
        // 使用putObject上传一个文件到存储桶中。
        InputStream inputStream = null;
        try {
            inputStream = file.getInputStream();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(fileName)
                    // TODO
                    .stream(inputStream, file.getSize(), 5 * 1024 * 1024)
                    .contentType(file.getContentType())
                    .build());
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取文件访问地址（有过期时间）
     */
    public String getExpireFileUrl(String fileName, int time, TimeUnit timeUnit, String bucket) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(fileName)
                    .expiry(time, timeUnit).build());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取文件访问地址
     */
    public String getFileUrl(String fileName, String bucket) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(fileName)
                    .build()
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 下载文件
     */
    public void download(HttpServletResponse response, String fileName, String bucket) {
        InputStream in = null;
        try {
            // 获取对象信息
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder().bucket(bucket).object(fileName).build());
            response.setContentType(stat.contentType());
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));
            // 文件下载
            in = minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(fileName).build());
            IOUtils.copy(in, response.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public InputStream getObjectStream(String bucket, String fileName) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(fileName)
                    .build());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String readJsonFile(String bucket, String fileName) {
        InputStream inputStream = null;
        BufferedReader reader = null;
        try {
            // 获取文件输入流
            inputStream = getObjectStream(bucket, fileName);
            if (inputStream == null) {
                return null;
            }

            // 读取输入流并转换为字符串
            reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder jsonString = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonString.append(line);
            }

            // 将字符串转换为JSONObject
            return jsonString.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            // 关闭资源
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public byte[] downloadByte(String bucket, String fileName) {
        InputStream in = null;
        ByteArrayOutputStream out = null;
        try {
            // 获取对象信息
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder().bucket(bucket).object(fileName).build());
            // 文件下载
            in = minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(fileName).build());
            out = new ByteArrayOutputStream();
            IOUtils.copy(in, out);
            return out.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 删除文件
     */
    public void delete(String fileName, String bucket) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(fileName).build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 复制
    public void copyObject(
            String sourceBucket, String sourceKey,
            String targetBucket, String targetKey
    ) throws Exception {
        // 1. 参数校验与默认值处理
        if (sourceKey == null || targetKey == null) {
            throw new IllegalArgumentException("源Key和目标Key不能为空");
        }

        // 2. 检查源对象是否存在
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(sourceBucket)
                            .object(sourceKey)
                            .build()
            );
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                throw new RuntimeException("源对象不存在: " + sourceBucket + "/" + sourceKey);
            }
            throw e;
        }

        // 3. 构建复制参数
        CopySource.Builder copySourceBuilder = CopySource.builder()
                .bucket(sourceBucket)
                .object(sourceKey);


        CopyObjectArgs.Builder copyArgsBuilder = CopyObjectArgs.builder()
                .source(copySourceBuilder.build())
                .bucket(targetBucket)
                .object(targetKey);

        // 4. 执行复制
        minioClient.copyObject(copyArgsBuilder.build());
    }

    public JSONObject getServerStorageInfo() throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        List<ServerProperties> servers = minioAdminClient.getServerInfo().servers();
        Set<BigDecimal> totalSpaceSet = new HashSet<>();
        Set<BigDecimal> availSpaceSet = new HashSet<>();
        Set<BigDecimal> usedSpaceSet = new HashSet<>();
        servers.forEach(server -> {
            server.disks().forEach(disk -> {
                totalSpaceSet.add(disk.totalspace());
                availSpaceSet.add(disk.availspace());
                usedSpaceSet.add(disk.usedspace());
            });
        });
        BigDecimal totalSpace = totalSpaceSet.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal availSpace = availSpaceSet.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal usedSpace = usedSpaceSet.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        JSONObject serverStorageInfo = new JSONObject();
        serverStorageInfo.put("totalSpace", totalSpace);
        serverStorageInfo.put("availSpace", availSpace);
        serverStorageInfo.put("usedSpace", usedSpace);
        return serverStorageInfo;
    }

    public List<JSONObject> getBucketsInfo() throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        Map<String, BucketUsageInfo> bucketUsageInfo =  minioAdminClient.getDataUsageInfo().bucketsUsageInfo();
        List<JSONObject> bucketsInfo = new ArrayList<>();
        bucketUsageInfo.forEach((key, value) -> {
            JSONObject bucketInfo = new JSONObject();
            Long bucketUsedSize = value.size();
            Long objectsCount = value.objectsCount();
            bucketInfo.put("bucketName", key);
            bucketInfo.put("bucketUsedSize", bucketUsedSize);
            bucketInfo.put("objectsCount", objectsCount);
            bucketsInfo.add(bucketInfo);
        });
        return bucketsInfo;
    }

}
