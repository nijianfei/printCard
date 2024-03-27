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
            log.info("userId:{},userName:{}. hook End {}", param.getUserId(), param.getUserName(), KeyHook.instance.installHook());
        }).start();
    }

    void checkParams(PrintDto param) {
        if (isCheckParams) {
            Assert.isTrue(TemplateEnum.isExist(param.getTemplateType()), "不支持的模板ID");
            Assert.isTrue(StringUtils.isNotBlank(param.getUserId()), "userId不能为空");
            Assert.isTrue(StringUtils.isNotBlank(param.getUserName()), "userName不能为空");
            Assert.isTrue(StringUtils.isNotBlank(param.getDeptName()), "deptName不能为空");
            Assert.isTrue(StringUtils.isNotBlank(param.getBase64Photo()), "base64Photo不能为空");
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
