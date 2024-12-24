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
            String defaultFace = "data:image/jpeg;base64,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AAAAAAAKdmVjdG9yRGF0YWJvb2wBAAAAAFBnUHNlbnVtAAAAAFBnUHMAAAAAUGdQQwAAAABMZWZ0VW50RiNSbHQAAAAAAAAAAAAAAABUb3AgVW50RiNSbHQAAAAAAAAAAAAAAABTY2wgVW50RiNQcmNAWQAAAAAAAAAAABBjcm9wV2hlblByaW50aW5nYm9vbAAAAAAOY3JvcFJlY3RCb3R0b21sb25nAAAAAAAAAAxjcm9wUmVjdExlZnRsb25nAAAAAAAAAA1jcm9wUmVjdFJpZ2h0bG9uZwAAAAAAAAALY3JvcFJlY3RUb3Bsb25nAAAAAAA4QklNA+0AAAAAABAAX/ySAAEAAgBf/JIAAQACOEJJTQQmAAAAAAAOAAAAAAAAAAAAAD+AAAA4QklNBA0AAAAAAAQAAAAeOEJJTQQZAAAAAAAEAAAAHjhCSU0D8wAAAAAACQAAAAAAAAAAAQA4QklNJxAAAAAAAAoAAQAAAAAAAAACOEJJTQP1AAAAAABIAC9mZgABAGxmZgAGAAAAAAABAC9mZgABAKGZmgAGAAAAAAABADIAAAABAFoAAAAGAAAAAAABADUAAAABAC0AAAAGAAAAAAABOEJJTQP4AAAAAABwAAD/////////////////////////////A+gAAAAA/////////////////////////////wPoAAAAAP////////////////////////////8D6AAAAAD/////////////////////////////A+gAADhCSU0EAAAAAAAAAgABOEJJTQQCAAAAAAAEAAAAADhCSU0EMAAAAAAAAgEBOEJJTQQtAAAAAAAGAAEAAAADOEJJTQQIAAAAAAAQAAAAAQAAAkAAAAJAAAAAADhCSU0EHgAAAAAABAAAAAA4QklNBBoAAAAAAz8AAAAGAAAAAAAAAAAAAACtAAAAnAAAAAVRbHUoU2FZNFDPAAAAAQAAAAAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAACcAAAArQAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAABAAAAABAAAAAAAAbnVsbAAAAAIAAAAGYm91bmRzT2JqYwAAAAEAAAAAAABSY3QxAAAABAAAAABUb3AgbG9uZwAAAAAAAAAATGVmdGxvbmcAAAAAAAAAAEJ0b21sb25nAAAArQAAAABSZ2h0bG9uZwAAAJwAAAAGc2xpY2VzVmxMcwAAAAFPYmpjAAAAAQAAAAAABXNsaWNlAAAAEgAAAAdzbGljZUlEbG9uZwAAAAAAAAAHZ3JvdXBJRGxvbmcAAAAAAAAABm9yaWdpbmVudW0AAAAMRVNsaWNlT3JpZ2luAAAADWF1dG9HZW5lcmF0ZWQAAAAAVHlwZWVudW0AAAAKRVNsaWNlVHlwZQAAAABJbWcgAAAABmJvdW5kc09iamMAAAABAAAAAAAAUmN0MQAAAAQAAAAAVG9wIGxvbmcAAAAAAAAAAExlZnRsb25nAAAAAAAAAABCdG9tbG9uZwAAAK0AAAAAUmdodGxvbmcAAACcAAAAA3VybFRFWFQAAAABAAAAAAAAbnVsbFRFWFQAAAABAAAAAAAATXNnZVRFWFQAAAABAAAAAAAGYWx0VGFnVEVYVAAAAAEAAAAAAA5jZWxsVGV4dElzSFRNTGJvb2wBAAAACGNlbGxUZXh0VEVYVAAAAAEAAAAAAAlob3J6QWxpZ25lbnVtAAAAD0VTbGljZUhvcnpBbGlnbgAAAAdkZWZhdWx0AAAACXZlcnRBbGlnbmVudW0AAAAPRVNsaWNlVmVydEFsaWduAAAAB2RlZmF1bHQAAAALYmdDb2xvclR5cGVlbnVtAAAAEUVTbGljZUJHQ29sb3JUeXBlAAAAAE5vbmUAAAAJdG9wT3V0c2V0bG9uZwAAAAAAAAAKbGVmdE91dHNldGxvbmcAAAAAAAAADGJvdHRvbU91dHNldGxvbmcAAAAAAAAAC3JpZ2h0T3V0c2V0bG9uZwAAAAAAOEJJTQQoAAAAAAAMAAAAAj/wAAAAAAAAOEJJTQQRAAAAAAABAQA4QklNBBQAAAAAAAQAAAAGOEJJTQQMAAAAABUnAAAAAQAAAJAAAACgAAABsAABDgAAABULABgAAf/Y/+IMWElDQ19QUk9GSUxFAAEBAAAMSExpbm8CEAAAbW50clJHQiBYWVogB84AAgAJAAYAMQAAYWNzcE1TRlQAAAAASUVDIHNSR0IAAAAAAAAAAAAAAAEAAPbWAAEAAAAA0y1IUCAgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAARY3BydAAAAVAAAAAzZGVzYwAAAYQAAABsd3RwdAAAAfAAAAAUYmtwdAAAAgQAAAAUclhZWgAAAhgAAAAUZ1hZWgAAAiwAAAAUYlhZWgAAAkAAAAAUZG1uZAAAAlQAAABwZG1kZAAAAsQAAACIdnVlZAAAA0wAAACGdmlldwAAA9QAAAAkbHVtaQAAA/gAAAAUbWVhcwAABAwAAAAkdGVjaAAABDAAAAAMclRSQwAABDwAAAgMZ1RSQwAABDwAAAgMYlRSQwAABDwAAAgMdGV4dAAAAABDb3B5cmlnaHQgKGMpIDE5OTggSGV3bGV0dC1QYWNrYXJkIENvbXBhbnkAAGRlc2MAAAAAAAAAEnNSR0IgSUVDNjE5NjYtMi4xAAAAAAAAAAAAAAASc1JHQiBJRUM2MTk2Ni0yLjEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAFhZWiAAAAAAAADzUQABAAAAARbMWFlaIAAAAAAAAAAAAAAAAAAAAABYWVogAAAAAAAAb6IAADj1AAADkFhZWiAAAAAAAABimQAAt4UAABjaWFlaIAAAAAAAACSgAAAPhAAAts9kZXNjAAAAAAAAABZJRUMgaHR0cDovL3d3dy5pZWMuY2gAAAAAAAAAAAAAABZJRUMgaHR0cDovL3d3dy5pZWMuY2gAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAZGVzYwAAAAAAAAAuSUVDIDYxOTY2LTIuMSBEZWZhdWx0IFJHQiBjb2xvdXIgc3BhY2UgLSBzUkdCAAAAAAAAAAAAAAAuSUVDIDYxOTY2LTIuMSBEZWZhdWx0IFJHQiBjb2xvdXIgc3BhY2UgLSBzUkdCAAAAAAAAAAAAAAAAAAAAAAAAAAAAAGRlc2MAAAAAAAAALFJlZmVyZW5jZSBWaWV3aW5nIENvbmRpdGlvbiBpbiBJRUM2MTk2Ni0yLjEAAAAAAAAAAAAAACxSZWZlcmVuY2UgVmlld2luZyBDb25kaXRpb24gaW4gSUVDNjE5NjYtMi4xAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB2aWV3AAAAAAATpP4AFF8uABDPFAAD7cwABBMLAANcngAAAAFYWVogAAAAAABMCVYAUAAAAFcf521lYXMAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAAKPAAAAAnNpZyAAAAAAQ1JUIGN1cnYAAAAAAAAEAAAAAAUACgAPABQAGQAeACMAKAAtADIANwA7AEAARQBKAE8AVABZAF4AYwBoAG0AcgB3AHwAgQCGAIsAkACVAJoAnwCkAKkArgCyALcAvADBAMYAywDQANUA2wDgAOUA6wDwAPYA+wEBAQcBDQETARkBHwElASsBMgE4AT4BRQFMAVIBWQFgAWcBbgF1AXwBgwGLAZIBmgGhAakBsQG5AcEByQHRAdkB4QHpAfIB+gIDAgwCFAIdAiYCLwI4AkECSwJUAl0CZwJxAnoChAKOApgCogKsArYCwQLLAtUC4ALrAvUDAAMLAxYDIQMtAzgDQwNPA1oDZgNyA34DigOWA6IDrgO6A8cD0wPgA+wD+QQGBBMEIAQtBDsESARVBGMEcQR+BIwEmgSoBLYExATTBOEE8AT+BQ0FHAUrBToFSQVYBWcFdwWGBZYFpgW1BcUF1QXlBfYGBgYWBicGNwZIBlkGagZ7BowGnQavBsAG0QbjBvUHBwcZBysHPQdPB2EHdAeGB5kHrAe/B9IH5Qf4CAsIHwgyCEYIWghuCIIIlgiqCL4I0gjnCPsJEAklCToJTwlkCXkJjwmkCboJzwnlCfsKEQonCj0KVApqCoEKmAquCsUK3ArzCwsLIgs5C1ELaQuAC5gLsAvIC+EL+QwSDCoMQwxcDHUMjgynDMAM2QzzDQ0NJg1ADVoNdA2ODakNww3eDfgOEw4uDkkOZA5/DpsOtg7SDu4PCQ8lD0EPXg96D5YPsw/PD+wQCRAmEEMQYRB+EJsQuRDXEPURExExEU8RbRGMEaoRyRHoEgcSJhJFEmQShBKjEsMS4xMDEyMTQxNjE4MTpBPFE+UUBhQnFEkUahSLFK0UzhTwFRIVNBVWFXgVmxW9FeAWAxYmFkkWbBaPFrIW1hb6Fx0XQRdlF4kXrhfSF/cYGxhAGGUYihivGNUY+hkgGUUZaxmRGbcZ3RoEGioaURp3Gp4axRrsGxQbOxtjG4obshvaHAIcKhxSHHscoxzMHPUdHh1HHXAdmR3DHeweFh5AHmoelB6+HukfEx8+H2kflB+/H+ogFSBBIGwgmCDEIPAhHCFIIXUhoSHOIfsiJyJVIoIiryLdIwojOCNmI5QjwiPwJB8kTSR8JKsk2iUJJTglaCWXJccl9yYnJlcmhya3JugnGCdJJ3onqyfcKA0oPyhxKKIo1CkGKTgpaymdKdAqAio1KmgqmyrPKwIrNitpK50r0SwFLDksbiyiLNctDC1BLXYtqy3hLhYuTC6CLrcu7i8kL1ovkS/HL/4wNTBsMKQw2zESMUoxgjG6MfIyKjJjMpsy1DMNM0YzfzO4M/E0KzRlNJ402DUTNU01hzXCNf02NzZyNq426TckN2A3nDfXOBQ4UDiMOMg5BTlCOX85vDn5OjY6dDqyOu87LTtrO6o76DwnPGU8pDzjPSI9YT2hPeA+ID5gPqA+4D8hP2E/oj/iQCNAZECmQOdBKUFqQaxB7kIwQnJCtUL3QzpDfUPARANER0SKRM5FEkVVRZpF3kYiRmdGq0bwRzVHe0fASAVIS0iRSNdJHUljSalJ8Eo3Sn1KxEsMS1NLmkviTCpMcky6TQJNSk2TTdxOJU5uTrdPAE9JT5NP3VAnUHFQu1EGUVBRm1HmUjFSfFLHUxNTX1OqU/ZUQlSPVNtVKFV1VcJWD1ZcVqlW91dEV5JX4FgvWH1Yy1kaWWlZuFoHWlZaplr1W0VblVvlXDVchlzWXSddeF3JXhpebF69Xw9fYV+zYAVgV2CqYPxhT2GiYfViSWKcYvBjQ2OXY+tkQGSUZOllPWWSZedmPWaSZuhnPWeTZ+loP2iWaOxpQ2maafFqSGqfavdrT2una/9sV2yvbQhtYG25bhJua27Ebx5veG/RcCtwhnDgcTpxlXHwcktypnMBc11zuHQUdHB0zHUodYV14XY+dpt2+HdWd7N4EXhueMx5KnmJeed6RnqlewR7Y3vCfCF8gXzhfUF9oX4BfmJ+wn8jf4R/5YBHgKiBCoFrgc2CMIKSgvSDV4O6hB2EgITjhUeFq4YOhnKG14c7h5+IBIhpiM6JM4mZif6KZIrKizCLlov8jGOMyo0xjZiN/45mjs6PNo+ekAaQbpDWkT+RqJIRknqS45NNk7aUIJSKlPSVX5XJljSWn5cKl3WX4JhMmLiZJJmQmfyaaJrVm0Kbr5wcnImc951kndKeQJ6unx2fi5/6oGmg2KFHobaiJqKWowajdqPmpFakx6U4pammGqaLpv2nbqfgqFKoxKk3qamqHKqPqwKrdavprFys0K1ErbiuLa6hrxavi7AAsHWw6rFgsdayS7LCszizrrQltJy1E7WKtgG2ebbwt2i34LhZuNG5SrnCuju6tbsuu6e8IbybvRW9j74KvoS+/796v/XAcMDswWfB48JfwtvDWMPUxFHEzsVLxcjGRsbDx0HHv8g9yLzJOsm5yjjKt8s2y7bMNcy1zTXNtc42zrbPN8+40DnQutE80b7SP9LB00TTxtRJ1MvVTtXR1lXW2Ndc1+DYZNjo2WzZ8dp22vvbgNwF3IrdEN2W3hzeot8p36/gNuC94UThzOJT4tvjY+Pr5HPk/OWE5g3mlucf56noMui86Ubp0Opb6uXrcOv77IbtEe2c7ijutO9A78zwWPDl8XLx//KM8xnzp/Q09ML1UPXe9m32+/eK+Bn4qPk4+cf6V/rn+3f8B/yY/Sn9uv5L/tz/bf///+0ADEFkb2JlX0NNAAL/7gAOQWRvYmUAZIAAAAAB/9sAhAAMCAgICQgMCQkMEQsKCxEVDwwMDxUYExMVExMYEQwMDAwMDBEMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMAQ0LCw0ODRAODhAUDg4OFBQODg4OFBEMDAwMDBERDAwMDAwMEQwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAz/wAARCACgAJADASIAAhEBAxEB/90ABAAJ/8QBPwAAAQUBAQEBAQEAAAAAAAAAAwABAgQFBgcICQoLAQABBQEBAQEBAQAAAAAAAAABAAIDBAUGBwgJCgsQAAEEAQMCBAIFBwYIBQMMMwEAAhEDBCESMQVBUWETInGBMgYUkaGxQiMkFVLBYjM0coLRQwclklPw4fFjczUWorKDJkSTVGRFwqN0NhfSVeJl8rOEw9N14/NGJ5SkhbSVxNTk9KW1xdXl9VZmdoaWprbG1ub2N0dXZ3eHl6e3x9fn9xEAAgIBAgQEAwQFBgcHBgU1AQACEQMhMRIEQVFhcSITBTKBkRShsUIjwVLR8DMkYuFygpJDUxVjczTxJQYWorKDByY1wtJEk1SjF2RFVTZ0ZeLys4TD03Xj80aUpIW0lcTU5PSltcXV5fVWZnaGlqa2xtbm9ic3R1dnd4eXp7fH/9oADAMBAAIRAxEAPwD1VJJJJSkkkklKSSSSUpJJRssrqaX2ODGDlziAB8ykpkkqo6r0smBmUE+HqM/8krLXBwDmkEHUEaiElLpJJJKUkkkkpSSSSSlJJJJKf//Q9VSSSSUpJJJJSkkli/Wvq7umdO20u25WSTXSRy0R+kt/623/AKaSmh9YvrecS1+F00NdkMO229wlrCPpVsb+fb/0GLj8nJycuz1cu599n71hmP6rfoMb/JYhJJIW2t8B9ys4WfnYDxZhXvpMyWgywn/hKj7Hqukip7fB+vOCcVhz2PryhIsbU0uYY+i9uv5/7q1undf6V1J2zFvBt/0L5Y/v9Fj/AKf0f8GvMk4Lmua9jix7CHMe0kOaRw5jh7muQVb66ksj6s9Yd1Xp++4g5VB9O+BEn82yP+EatdJKkkkklKSSSSU//9H1VJJJJSkkkklKXnn1xyzkdcsrn2YrG1NHmR6tn/VsXoa8r6tY6zqua92pORYNdfouLB/0WpKLVSSSRQpJJJJSkkkklPRfUbJNfVrMcmG5FRMeLmGR/wBFz13i82+qpI+sOHHfeP8AoOXpKCVJJJJKUkkkkp//0vVUkkklKSSSSUpeX9cp9DrWdV4XOcPg/wDS/wDf16gvNvrRk15PXcl9bQ0VkVEj85zBDn/536P/AK2kouUkkkihSSSSSlJJJJKdf6p1uf8AWDGgSGCx7vIBpb/1T16OvPPqbeyrrtbXN3G+t9bXfukD1P8ApbF6GgkKSSSSUpJJJJT/AP/T9VSSSSUpJJJJSl5j9YKTR1zNrPe0vHweBZ/35enLlPrn0O/ILOpYlZssY3ZkVsEuLRqyxrfzvTSU8YkkkihSSSSSlJJJEgCToByUlOv9Uqn2fWDGLRIrD3v8htLP+qevR1zf1O6JZhY787KZsvyQAxjh7mV8+7919n0l0iCVJJJJKUkkkkp//9T1VJJJJSkkkklKSSSSU+b/AFpwTh9byAARXkfp6ye+/wDneP8Aht6yV6J9aujO6n0/dSJysWbKR3cI/SUf9d2t/wCuMYvO/wAPI8pIUkkkipS2fqn0853Wa3ubuoxB61s8btW47P8Atz9L/wBYWNrwASToAOST2C9I+rXR/wBldOFdkfabj6mQR+8RDax/JqZ7UFB1kkkkkqSSSSUpJJJJT//V9VSSSSUpJJRfYytjrLHBjGAuc5xgADlznFJTJJc71D679LxjsxGuzXjks9tf/brx7/8ArbHsXP5f1y65kSK3sxmntW2T/n2bklPd5ubj4OLZlZDgyusSSe57Nb/KcvKnvNj32OEGxznkeG47kTJzMzLO7KvsvM7gHuJAP7zWfQagpIUkkkipsdOyW4fUMbKeNzKLGvcPIH3L1Om6q+pt1Lg+t43Mc3UEFeSKzidS6hhCMTJspbr7WmW66n9G7cxBT6qkuCxPrv1eiBkNrymaTI2Ogfyme3/oLoOnfXHpGYRXc44dx023aNJ/k3D9H/256aSXdSTcp0lKSSSSU//W9VSSVLrHUmdM6ddmOG5zBFbD+c93trb/AJ30/wDg0lNfrn1hxOj1gPHrZTxNeO0wSP37He7061wXU+r9Q6rZuzLS5gMsobpW3wiv853/AAln6RV7778m9+RkPNl9p3WPPc/+Rb+Y1DSQpJJJFSkkkklKSSSSUpJJJJSkkkklOn0j6w9R6S4Npd62N3xrCdsT/gXf4F3/AIH/AMGu86T1nC6tjm7GcQ5ullT4D2H+W0T9L81y8wVjAz8np2WzMxTFjNC3s9v51T/5LkFPqySDh5dObi1ZdBmq5oe3xg/mu/lN/ORkkv8A/9f1Vcx9fXuHT8Zg+i66SPMNdt/6pdOsn6x9Ff1jEZVVYKran72lwlp0Ldjo/wCqSU+bpLoP+Y/Wv38f/Od/5BL/AJj9a/fx/wDOd/5BJDz6S6D/AJj9a/fx/wDOd/5BL/mP1r9/H/znf+QSU8+kug/5j9a/fx/853/kEv8AmP1r9/H/AM53/kElPPpLoP8AmP1r9/H/AM53/kEv+Y/Wv38f/Od/5BJTz6S6D/mP1r9/H/znf+QS/wCY/Wv38f8Aznf+QSU8+kug/wCY/Wv38f8Aznf+QS/5j9a/fx/853/kElPPpLoP+Y/Wv38f/Od/5BSZ9ResF4D7aGNPLgXOI/s7WpKeh+ppJ+rmLP71w+QuuC21U6V06rpnT6sGpxe2oGXu5LnudbY7+1Y9ytpJf//Q9VSSSSUpJJJJSkkkklKSSSSUpJJJJSkkkklKSSSSUpJJJJSkkkklP//ZADhCSU0EIQAAAAAAVQAAAAEBAAAADwBBAGQAbwBiAGUAIABQAGgAbwB0AG8AcwBoAG8AcAAAABMAQQBkAG8AYgBlACAAUABoAG8AdABvAHMAaABvAHAAIABDAFMANgAAAAEAOEJJTQQGAAAAAAAHAAQAAAABAQD/4Q4AaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLwA8P3hwYWNrZXQgYmVnaW49Iu+7vyIgaWQ9Ilc1TTBNcENlaGlIenJlU3pOVGN6a2M5ZCI/PiA8eDp4bXBtZXRhIHhtbG5zOng9ImFkb2JlOm5zOm1ldGEvIiB4OnhtcHRrPSJBZG9iZSBYTVAgQ29yZSA1LjMtYzAxMSA2Ni4xNDU2NjEsIDIwMTIvMDIvMDYtMTQ6NTY6MjcgICAgICAgICI+IDxyZGY6UkRGIHhtbG5zOnJkZj0iaHR0cDovL3d3dy53My5vcmcvMTk5OS8wMi8yMi1yZGYtc3ludGF4LW5zIyI+IDxyZGY6RGVzY3JpcHRpb24gcmRmOmFib3V0PSIiIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIgeG1sbnM6ZGM9Imh0dHA6Ly9wdXJsLm9yZy9kYy9lbGVtZW50cy8xLjEvIiB4bWxuczpwaG90b3Nob3A9Imh0dHA6Ly9ucy5hZG9iZS5jb20vcGhvdG9zaG9wLzEuMC8iIHhtbG5zOnhtcE1NPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvbW0vIiB4bWxuczpzdEV2dD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wL3NUeXBlL1Jlc291cmNlRXZlbnQjIiB4bXA6Q3JlYXRvclRvb2w9IkFkb2JlIFBob3Rvc2hvcCBDUzYgKFdpbmRvd3MpIiB4bXA6Q3JlYXRlRGF0ZT0iMjAyNC0wMy0xOFQxNDozNTo1MyswODowMCIgeG1wOk1vZGlmeURhdGU9IjIwMjQtMDMtMThUMTU6MjE6NDkrMDg6MDAiIHhtcDpNZXRhZGF0YURhdGU9IjIwMjQtMDMtMThUMTU6MjE6NDkrMDg6MDAiIGRjOmZvcm1hdD0iaW1hZ2UvanBlZyIgcGhvdG9zaG9wOkNvbG9yTW9kZT0iMyIgeG1wTU06SW5zdGFuY2VJRD0ieG1wLmlpZDpENjQ4RDkzRkYxRTRFRTExOEM0REQ1NDVCMDYzQjVEQSIgeG1wTU06RG9jdW1lbnRJRD0ieG1wLmRpZDpENTQ4RDkzRkYxRTRFRTExOEM0REQ1NDVCMDYzQjVEQSIgeG1wTU06T3JpZ2luYWxEb2N1bWVudElEPSJ4bXAuZGlkOkQ1NDhEOTNGRjFFNEVFMTE4QzRERDU0NUIwNjNCNURBIj4gPHhtcE1NOkhpc3Rvcnk+IDxyZGY6U2VxPiA8cmRmOmxpIHN0RXZ0OmFjdGlvbj0iY3JlYXRlZCIgc3RFdnQ6aW5zdGFuY2VJRD0ieG1wLmlpZDpENTQ4RDkzRkYxRTRFRTExOEM0REQ1NDVCMDYzQjVEQSIgc3RFdnQ6d2hlbj0iMjAyNC0wMy0xOFQxNDozNTo1MyswODowMCIgc3RFdnQ6c29mdHdhcmVBZ2VudD0iQWRvYmUgUGhvdG9zaG9wIENTNiAoV2luZG93cykiLz4gPHJkZjpsaSBzdEV2dDphY3Rpb249ImNvbnZlcnRlZCIgc3RFdnQ6cGFyYW1ldGVycz0iZnJvbSBpbWFnZS9wbmcgdG8gaW1hZ2UvanBlZyIvPiA8cmRmOmxpIHN0RXZ0OmFjdGlvbj0ic2F2ZWQiIHN0RXZ0Omluc3RhbmNlSUQ9InhtcC5paWQ6RDY0OEQ5M0ZGMUU0RUUxMThDNERENTQ1QjA2M0I1REEiIHN0RXZ0OndoZW49IjIwMjQtMDMtMThUMTU6MjE6NDkrMDg6MDAiIHN0RXZ0OnNvZnR3YXJlQWdlbnQ9IkFkb2JlIFBob3Rvc2hvcCBDUzYgKFdpbmRvd3MpIiBzdEV2dDpjaGFuZ2VkPSIvIi8+IDwvcmRmOlNlcT4gPC94bXBNTTpIaXN0b3J5PiA8L3JkZjpEZXNjcmlwdGlvbj4gPC9yZGY6UkRGPiA8L3g6eG1wbWV0YT4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICA8P3hwYWNrZXQgZW5kPSJ3Ij8+/+4ADkFkb2JlAGQAAAAAAf/bAIQABgQEBAUEBgUFBgkGBQYJCwgGBggLDAoKCwoKDBAMDAwMDAwQDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwM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";
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
