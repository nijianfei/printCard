package com.print.card.controller;

import com.alibaba.fastjson.JSON;
import com.freewayso.image.combiner.ImageCombiner;
import com.freewayso.image.combiner.enums.OutputFormat;
import com.freewayso.image.combiner.enums.ZoomMode;
import com.print.card.CardApplication;
import com.print.card.config.TaskServer;
import com.print.card.dto.Block;
import com.print.card.dto.Template;
import com.print.card.dto.TemplateConfig;
import com.print.card.enums.TemplateEnum;
import com.print.card.jna.DllLoadIn;
import com.print.card.jna.KeyHook;
import com.print.card.model.PrintDto;
import com.print.card.model.ResponseModel;
import com.print.card.utils.AdminUtil;
import com.print.card.utils.CommandUtil;
import com.sun.jna.WString;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

@RestController
@CrossOrigin
@RequestMapping({"api"})
public class ApiController {
    private static final Logger log = LoggerFactory.getLogger(ApiController.class);
    private String printRecordPath = System.getProperty("user.dir") + "\\print_recode\\%s\\%s\\%s.png";
    @Value("#{T(java.lang.Integer).parseInt('${wait.result.tryCount}')}")
    private Integer tryCount;
    @Autowired
    private TemplateConfig templateConfig;
    @Value("#{T(java.lang.Boolean).parseBoolean('${isPrint:true}')}")
    private Boolean isPrint;
    @Value("#{T(java.lang.Boolean).parseBoolean('${isCheckStatus:true}')}")
    private Boolean isCheckStatus;

    @Value("#{T(java.lang.Boolean).parseBoolean('${isCheckParams:true}')}")
    private Boolean isCheckParams;

    @PostMapping({"print"})
    public ResponseModel print(@RequestBody PrintDto param) {
        log.info("读卡打印_入参:{}", JSON.toJSONString(param));
        log.info("isAdmin:{}", AdminUtil.isAdmin());
        ResponseModel res = null;
        try {
            checkParams(param);
            String type = param.getTemplateType();
            Template template = templateConfig.getTemplateMap().get(type);
            String base64Photo = param.getBase64Photo();
            String userName = param.getUserName();
            if (!TaskServer.isReady() && (isPrint || isCheckStatus)) {
                ResponseModel fail = ResponseModel.fail(param.getReqNo(), "打印机状态：" + getErrInfo());
                log.error("ApiController:{}", JSON.toJSONString(fail));
                return fail;
            }

            String userId = param.getUserId();
            String deptName = param.getDeptName();
            String deptName2 = param.getDeptName2();
            String printRecordFileName = null;
            try {
                String dateTime = DateFormatUtils.format(new Date(), "yyyyMMdd-HHmmss");
                String[] dateSplit = dateTime.split("-");
                printRecordFileName = String.format(printRecordPath, type, dateSplit[0], userId + "_" + dateSplit[1]);
                File absoluteFile = new File(printRecordFileName).getParentFile();
                if (!absoluteFile.exists()) {
                    absoluteFile.mkdirs();
                }
            } catch (Exception ex) {
                log.error("{},{}", ex.getMessage(), ex);
                throw new RuntimeException("创建预览文件目录异常:" + ex.getMessage());
            }

            BufferedImage photoImage = null;
            try {
                //处理人像
                photoImage = getPhotoImage(base64Photo, template.getPhoto(), param.getTemplateType());
            } catch (Exception ex) {
                log.error("{},{}", ex.getMessage(), ex);
                throw new RuntimeException("人脸照片解析异常:" + ex.getMessage());
            }

            BufferedImage backIn = null;
            BufferedImage frontIn = null;
            try {
                backIn = readImage(template.getBackgroundImage());
                frontIn = readImage(template.getForegroundImage());
            } catch (Exception ex) {
                log.error("{},{}", ex.getMessage(), ex);
                throw new RuntimeException("读取模板背景图异常：" + ex.getMessage());
            }

            ImageCombiner combiner = null;
            try {
                combiner = new ImageCombiner(frontIn, OutputFormat.PNG);
                Block photoBlock = template.getPhoto();
                combiner.addImageElement(photoImage, photoBlock.getX(), photoBlock.getY(), photoBlock.getWidth(), photoBlock.getHeight(), ZoomMode.Origin).setCenter(true).setRoundCorner(photoBlock.getRoundCorner());
                //姓名
                Block userNameBlock = template.getUserName();
                combiner.addTextElement(userName, getFont(userNameBlock), userNameBlock.getX(), userNameBlock.getY()).setCenter(true).setColor(getColor(userNameBlock));
                //工号
                Block userIdBlock = template.getUserId();
                combiner.addTextElement(userId, getFont(userIdBlock), userIdBlock.getX(), userIdBlock.getY()).setCenter(true).setColor(getColor(userIdBlock));
                //部门
                Block deptNameBlock = template.getDeptName();
                combiner.addTextElement(deptName, getFont(deptNameBlock), deptNameBlock.getX(), deptNameBlock.getY()).setCenter(true).setColor(getColor(deptNameBlock));
                if (StringUtils.isNotBlank(deptName2)) {
                    Block deptNameBlock2 = template.getDeptName2();
                    combiner.addTextElement(deptName2, getFont(deptNameBlock2), deptNameBlock2.getX(), deptNameBlock2.getY()).setCenter(true).setColor(getColor(deptNameBlock2));
                }
                combiner.combine();
                combiner.save(printRecordFileName);
            } catch (Exception ex) {
                log.error("{},{}", ex.getMessage(), ex);
                throw new RuntimeException("保存预览文件异常：" + ex.getMessage());
            }
            try {
//                TaskServer.frame.setVisible(true);
//                TaskServer.frame.setAlwaysOnTop(true);
                hook(param);
                if (isPrint) {
                    //打印卡片
                    execPrint(processStream(combiner.getCombinedImage()), processStream(backIn));
                } else {
                    if (isCheckStatus) {
                        try {
                            getRdidDataMapList();
                            sleep(2000);
                        } finally {
                            tuika();
                        }
                    } else {
                        KeyHook.instance.setOutTime(5000);
                    }
                }
                ResponseModel result = waitPrintResult(param);
                log.info("API返回结果：{}", JSON.toJSONString(result));
                return result;
            } catch (Exception ex) {
                log.error("ApiController_Exception_print:{}", ex.getMessage(), ex);
                throw new RuntimeException("打印异常：" + ex.getMessage());
            } finally {
//                TaskServer.frame.setVisible(false);
                KeyHook.instance.unHook();
                TaskServer.param = null;
            }
        } catch (Exception ex) {
            log.error("ApiController_Exception_print:{}", ex.getMessage(), ex);
            res = ResponseModel.fail(param.getReqNo(), ex.getMessage());
        } catch (Error ex) {
            log.error("ApiController_Error:{}", ex.getMessage(), ex);
            res = ResponseModel.fail(param.getReqNo(), ex.getMessage());
        }
        return res;
    }

    private void hook(PrintDto param) {
        new Thread(() -> {
            log.info("userId:{},userName:{}. hook End ,stat: {}", param.getUserId(), param.getUserName(), KeyHook.instance.installHook());
        }).start();
        sleep(1000);
    }

    void checkParams(PrintDto param) {
        if (isCheckParams) {
            Assert.isTrue(TemplateEnum.isExist(param.getTemplateType()), "不支持的模板ID");
            Assert.isTrue(StringUtils.isNotBlank(param.getUserId()), "userId不能为空");
            Assert.isTrue(StringUtils.isNotBlank(param.getUserName()), "userName不能为空");
            Assert.isTrue(StringUtils.isNotBlank(param.getDeptName()), "deptName不能为空");
            Assert.isTrue(StringUtils.isNotBlank(param.getBase64Photo()), "base64Photo不能为空");
        }else{
            if (StringUtils.isBlank(param.getTemplateType())) {
                param.setTemplateType("4");
            }
            if (param.getUserId() == null) {
                param.setUserId("");
            }
            if (param.getUserName() == null) {
                param.setUserName("");
            }
            if (param.getDeptName() == null) {
                param.setDeptName("");
            }
            String defaultFace = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAJwAAACtCAIAAADK/IESAAAACXBIWXMAAAsTAAALEwEAmpwYAAAFyWlUWHRYTUw6Y29tLmFkb2JlLnhtcAAAAAAAPD94cGFja2V0IGJlZ2luPSLvu78iIGlkPSJXNU0wTXBDZWhpSHpyZVN6TlRjemtjOWQiPz4gPHg6eG1wbWV0YSB4bWxuczp4PSJhZG9iZTpuczptZXRhLyIgeDp4bXB0az0iQWRvYmUgWE1QIENvcmUgOS4xLWMwMDIgNzkuYTFjZDEyZiwgMjAyNC8xMS8xMS0xOTowODo0NiAgICAgICAgIj4gPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4gPHJkZjpEZXNjcmlwdGlvbiByZGY6YWJvdXQ9IiIgeG1sbnM6eG1wPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvIiB4bWxuczpkYz0iaHR0cDovL3B1cmwub3JnL2RjL2VsZW1lbnRzLzEuMS8iIHhtbG5zOnBob3Rvc2hvcD0iaHR0cDovL25zLmFkb2JlLmNvbS9waG90b3Nob3AvMS4wLyIgeG1sbnM6eG1wTU09Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iIHhtbG5zOnN0RXZ0PSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvc1R5cGUvUmVzb3VyY2VFdmVudCMiIHhtcDpDcmVhdG9yVG9vbD0iQWRvYmUgUGhvdG9zaG9wIDI2LjIgKFdpbmRvd3MpIiB4bXA6Q3JlYXRlRGF0ZT0iMjAyNC0xMi0yNVQxMjoxMjo0NiswODowMCIgeG1wOk1vZGlmeURhdGU9IjIwMjQtMTItMjVUMTI6MTU6MzQrMDg6MDAiIHhtcDpNZXRhZGF0YURhdGU9IjIwMjQtMTItMjVUMTI6MTU6MzQrMDg6MDAiIGRjOmZvcm1hdD0iaW1hZ2UvcG5nIiBwaG90b3Nob3A6Q29sb3JNb2RlPSIzIiB4bXBNTTpJbnN0YW5jZUlEPSJ4bXAuaWlkOmVjMmVjNzE3LWFlZTAtN2U0OC1iZDAxLTEzZjRiMzVlZjc4YSIgeG1wTU06RG9jdW1lbnRJRD0i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";
            if (StringUtils.isBlank(param.getBase64Photo()) || !param.getBase64Photo().startsWith("data:image")) {
                param.setBase64Photo(defaultFace);
            }
        }
        buildDeptName(param);
        TaskServer.param = param;
    }

    private void buildDeptName(PrintDto param) {
        int singleMaxLength = 13;
        double proportion = 0.40;
        String[] split = param.getDeptName().split("/");
        if (split.length >=2) {
            param.setDeptName(split[split.length-1]);
        }
        String deptName = param.getDeptName();
        if (deptName.length() <= singleMaxLength) {
            return;
        }
        if (deptName.length() <= singleMaxLength * 2) {
            int upLength = (int) (deptName.length() * proportion);
            while (deptName.length() - upLength > singleMaxLength) {
                proportion += 0.01;
                upLength = (int) (deptName.length() * proportion);
            }
            String upStr = deptName.substring(0, upLength);
            String belowStr = deptName.substring(upLength, deptName.length());
            param.setDeptName(upStr);
            param.setDeptName2(belowStr);
            return;
        }
        String upStr = deptName.substring(0, singleMaxLength);
        String belowStr = deptName.substring(singleMaxLength, singleMaxLength * 2);
        param.setDeptName(upStr);
        param.setDeptName2(belowStr);
    }

    private String getErrInfo() {
        String errorMsg = TaskServer.getStatusResult().getErrorMsg();
        String printSubStatusStatus = TaskServer.getStatusResult().getPrintSubStatusStatus();
        return TaskServer.getStatus().getName() + (Objects.nonNull(errorMsg) ? " - " + errorMsg : "") + (Objects.nonNull(printSubStatusStatus) ? " - " + printSubStatusStatus : "");
    }

    private Color getColor(Block colorBlock) {
        String[] colors = colorBlock.getColor().split(",");
        return new Color(Integer.parseInt(colors[0]), Integer.parseInt(colors[1]), Integer.parseInt(colors[2]));
    }

    private Font getFont(Block fontBlock) {
        return new Font(fontBlock.getFont(), fontBlock.getStyle(), fontBlock.getSize());
    }

    private BufferedImage getPhotoImage(String base64Photo, Block photoBlock, String type) throws Exception {
        InputStream inputStream = converBase64(base64Photo);
        BufferedImage bufferedImage = ImageIO.read(inputStream);
        int widthDiff = bufferedImage.getWidth() - photoBlock.getWidth();
        int heightDiff = bufferedImage.getHeight() - photoBlock.getHeight();
        if (Objects.equals(type, TemplateEnum.T4.getCode())) {
            //高度基准
            if (widthDiff - heightDiff > 0) {
                bufferedImage = Thumbnails.of(bufferedImage).width(photoBlock.getWidth()).asBufferedImage();//按宽度缩放
            } else {
                bufferedImage = Thumbnails.of(bufferedImage).height(photoBlock.getHeight()).asBufferedImage();//按高度缩放
            }
        } else {
            //高度基准
            if (widthDiff - heightDiff > 0) {
                bufferedImage = Thumbnails.of(bufferedImage).height(photoBlock.getHeight()).asBufferedImage();//按高度缩放
            } else {
                //宽度基准
                bufferedImage = Thumbnails.of(bufferedImage).width(photoBlock.getWidth()).asBufferedImage();//按宽度缩放
            }
        }
        bufferedImage = getBackImg(photoBlock, bufferedImage);//填补背景
        return Thumbnails.of(bufferedImage).sourceRegion(Positions.CENTER, photoBlock.getWidth(), photoBlock.getHeight()).scale(1.0).asBufferedImage();//裁剪;
    }

    private BufferedImage getBackImg(Block photoBlock, BufferedImage bufferedImage) throws Exception {
        //获取背景色
        int rgb = bufferedImage.getRGB(10, 10);
        //创建模板需要的照片大小的背景图（按照片的背景颜色）
        BufferedImage back = new BufferedImage(photoBlock.getWidth(), photoBlock.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = back.createGraphics();
        graphics.setColor(new Color(rgb));
        graphics.fillRect(0, 0, photoBlock.getWidth(), photoBlock.getHeight());
        graphics.dispose();

        ImageCombiner combiner = new ImageCombiner(back, OutputFormat.PNG);
        int x = (photoBlock.getWidth() - bufferedImage.getWidth()) / 2;
        int y = (photoBlock.getHeight() - bufferedImage.getHeight()) / 2;
        //将照片居中画在背景图上
        combiner.addImageElement(bufferedImage, x, y, bufferedImage.getWidth(), bufferedImage.getHeight(), ZoomMode.Origin).setCenter(true);
        combiner.combine();
        return combiner.getCombinedImage();
    }

    private ResponseModel waitPrintResult(PrintDto param) {
        sleep(2000);
        int tCount = tryCount;
        while (tCount-- > 0) {
            if (TaskServer.isReady()) {
                log.info("打印机处于就绪状态：{}  --》返回结果", JSON.toJSONString(TaskServer.getStatusResult()));
                return ResponseModel.success(param.getReqNo(), KeyHook.instance.getCard());
            } else {
                if (isPrint || isCheckStatus) {
                    if (TaskServer.isBusy()) {
                        sleep(2000);
                    } else {
                        log.error("打印机处于异常状态：{} ", JSON.toJSONString(TaskServer.getStatusResult()));
                        break;
                    }
                }else{
                    log.error("已开启测试模式！不会打印卡片，只做图片预览！延时5秒返回结果---------------------------------------------------》");
                    sleep(5000);
                    return ResponseModel.success(param.getReqNo(), KeyHook.instance.getCard());
                }
            }
        }
        log.error("打印机在规定时间内未获得结果，超时-----------------------------------------------------------------》》");
        return ResponseModel.fail(param.getReqNo(), "打印机状态：" + TaskServer.getStatus().getName() + " " + TaskServer.getStatusResult().getErrorMsg());
    }

    private void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private String getRdidDataMapList() {
        Map<String, String> deviceStatuses = CommandUtil.parse_query_local_device_statuses(DllLoadIn.instance.callFunc(new WString(CommandUtil.format_query_local_device_statuses())).toString());
        CommandUtil.parse_setting_source(DllLoadIn.instance.callFunc(new WString(CommandUtil.format_position_card(deviceStatuses.get("port"), deviceStatuses.get("port_number"), deviceStatuses.get("hardware_type"), "PrintPosition"))).toString());
        return null;
    }

    private void tuika() {
        try {
            Map<String, String> deviceStatuses = CommandUtil.parse_query_local_device_statuses(DllLoadIn.instance.callFunc(new WString(CommandUtil.format_query_local_device_statuses())).toString());
            CommandUtil.parse_setting_source(DllLoadIn.instance.callFunc(new WString(CommandUtil.format_position_card( deviceStatuses.get("port"),  deviceStatuses.get("port_number"), deviceStatuses.get("hardware_type"), "RejectPosition"))).toString());
        } catch (Exception e) {
            log.error("ApiController_tuika:{}", e.getMessage(), e);
        }
    }

    private void execPrint(byte[] bytesFront, byte[] bytesBack) {

        String connect = CommandUtil.format_connect("127.0.0.1", "123123");
        String connection_handle = CommandUtil.parse_connect(DllLoadIn.instance.callFunc(new WString(connect)).toString());
        log.info("=====第一步_创建连接_connection_handle=" + connection_handle);
        String job_id = CommandUtil.parse_add_new_xml_job(DllLoadIn.instance.callFunc(new WString(CommandUtil.format_add_new_xml_job(connection_handle, "test_group_name"))).toString());
        log.info("=====第二步_创建任务_job_id=" + job_id);
        String command_text_front = CommandUtil.format_setting_source(connection_handle, job_id, "@front");
        String parse_print_source_front_response = CommandUtil.parse_setting_source(DllLoadIn.instance.setResource(new WString(command_text_front), bytesFront, bytesFront.length).toString());
        log.info("=====第三步_正面图像资源设置_parse_print_source_file_response=" + parse_print_source_front_response);
        String command_text_back = CommandUtil.format_setting_source(connection_handle, job_id, "@back");
        String parse_print_source_back_response = CommandUtil.parse_setting_source(DllLoadIn.instance.setResource(new WString(command_text_back), bytesBack, bytesBack.length).toString());
        log.info("=====第四步_反面图像资源设置_parse_print_source_file_response=" + parse_print_source_back_response);
        String command_response_start = CommandUtil.parse_setting_source(DllLoadIn.instance.callFunc(new WString(CommandUtil.format_start_xml_job(connection_handle, job_id))).toString());
        log.info("=====第五步_开始执行指令_command_response_start=" + command_response_start);
        String command_response_dis = CommandUtil.parse_setting_source(DllLoadIn.instance.callFunc(new WString(CommandUtil.format_disconnect(connection_handle))).toString());
        log.info("=====第六步_断开连接_command_response_dis=" + command_response_dis);
    }

    public static InputStream converBase64(String base64String) {
        String faceBase64Str = base64String;
        if (base64String.contains(",")) {
            String[] split = base64String.split(",");
            faceBase64Str = split[1];
        }
        byte[] decodedBytes = Base64.getDecoder().decode(faceBase64Str);
        InputStream inputStream = new ByteArrayInputStream(decodedBytes);
        return inputStream;
    }

    private byte[] processStream(BufferedImage bufferedImage) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Thumbnails.of(bufferedImage).rotate(90.0).size(1036, 664).outputFormat("bmp").toOutputStream(bos);
        return bos.toByteArray();
    }

    public BufferedImage readImage(String fileName) throws IOException {
        return ImageIO.read(CardApplication.class.getClassLoader().getResource("images/" + fileName));
    }
}
