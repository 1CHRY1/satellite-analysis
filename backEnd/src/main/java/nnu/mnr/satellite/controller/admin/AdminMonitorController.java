package nnu.mnr.satellite.controller.admin;

import com.alibaba.druid.stat.DruidStatManagerFacade;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import com.alibaba.druid.support.http.stat.WebAppStatManager;

@RestController
@RequestMapping("admin/api/v1/monitor")
public class AdminMonitorController {

    // 数据源
    @GetMapping("/datasource")
    public Object getDataSourceStats() {
        return DruidStatManagerFacade.getInstance().getDataSourceStatDataList();
    }
    // 数据源(单个)
    @GetMapping("/datasource/{datasourceId}")
    public Object getDataSourceStats(@PathVariable Integer datasourceId) {
        return DruidStatManagerFacade.getInstance().getDataSourceStatData(datasourceId);
    }
    // SQL监控(所有)
    @GetMapping("/sql")
    public Object getSqlStats() {
        return DruidStatManagerFacade.getInstance().getSqlStatDataList(null);
    }
    // SQL监控(单个数据库)
    @GetMapping("/sql/{datasourceId}")
    public Object getSqlStats(@PathVariable Integer datasourceId) {
        return DruidStatManagerFacade.getInstance().getSqlStatDataList(datasourceId);
    }
    // SQL监控(单个sql语句)
    @GetMapping("/sql/detail/{sqlId}")
    public Object getSqlDetailStats(@PathVariable Integer sqlId) {
        return DruidStatManagerFacade.getInstance().getSqlStatData(sqlId);
    }
    // SQL防火墙（单个）
    @GetMapping("/wall/{datasourceId}")
    public Object getSqlFirewallStat(@PathVariable Integer datasourceId) {
        return DruidStatManagerFacade.getInstance().getWallStatMap(datasourceId);
    }
    // SQL防火墙（动态，没啥意义）
    @GetMapping("/wall")
    public Object getSqlFirewallStat() {
        return DruidStatManagerFacade.getInstance().getWallStatMap(null);
    }
    // Web应用（根路径）
    @GetMapping("/web")
    public Object getWebStats() {
        return WebAppStatManager.getInstance().getWebAppStatData();
    }
    // Web应用（指定路径）
    @GetMapping("/web/path")
    public Object getWebStats(@RequestParam String path) {
        return WebAppStatManager.getInstance().getWebAppStat(path);
    }
    // URI监控(所有)
    @GetMapping("/uri")
    public Object getUriStats() {
        return WebAppStatManager.getInstance().getURIStatData();
    }
    // URI监控(单个)
    @GetMapping("/uri/path")
    public Object getUriStats(@RequestParam String path) {
        return WebAppStatManager.getInstance().getURIStatData(path);
    }
    // Session监控(所有)
    @GetMapping("/session")
    public Object getSessionStats() {
        return WebAppStatManager.getInstance().getSessionStatData();
    }
    // Session监控(单个)
    @GetMapping("/session/{sessionId}")
    public Object getSessionStats(@PathVariable String sessionId) {
        return WebAppStatManager.getInstance().getSessionStat(sessionId);
    }
    @GetMapping("/druid")
    public ModelAndView druid() {
        // 使用 ModelAndView 进行重定向
        return new ModelAndView("redirect:/druid/index.html");
    }
    @GetMapping("/test-session")
    public String testSession(HttpSession session) {
        session.setAttribute("testKey", "testValue");
        return "Session set: " + session.getId();
    }
}
