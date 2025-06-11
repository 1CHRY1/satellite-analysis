package nnu.mnr.satellite.config.dt.converters;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/4/11 17:41
 * @Description:
 */

@ReadingConverter
public class String2DateConverter implements Converter<String, Date> {

    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

    @Override
    public Date convert(String source) {
        try {
            return DATE_FORMAT.get().parse(source);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("无法将字符串[%s]转换为Date类型", source), e);
        }
    }

}
