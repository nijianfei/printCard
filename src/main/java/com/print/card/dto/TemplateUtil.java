package com.print.card.dto;

import cn.hutool.json.JSONUtil;
import com.freewayso.image.combiner.ImageCombiner;
import com.freewayso.image.combiner.element.TextElement;
import com.freewayso.image.combiner.enums.BaseLine;
import com.freewayso.image.combiner.enums.OutputFormat;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.groovy.util.Maps;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Type;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class TemplateUtil {
    //智能柜水墨屏测试demo
    public static void main(String[] args) {
//        Map<String, String> contentMap = Maps.of("templateId", "T1", "userName", "张小小", "userId", "CSC12345", "cabinetStatus", "可取状态", "controlTime", "存入03-21 12:00");
        Map<String, String> contentMap = Maps.of("templateId", "T1", "userName", "张小小", "userId", "CSC12345", "cabinetStatus", "", "controlTime", "");
        String pngBase64 = makeImageBase64(contentMap);
        System.out.println(pngBase64);

        Map<String, String> contentMap1 = Maps.of("templateId", "T2", "userName", "张甜甜", "userId", "CSC99999", "cabinetStatus", "管控状态", "controlTime", "存入03-21 12:00");
        String pngBase64_1 = makeImageBase64(contentMap1);
        System.out.println(pngBase64_1);

        Map<String, String> contentMap2 = Maps.of("templateId", "T3", "TOP", "存入存入存入存入存入存入存入存入", "MIDDLE", "空闲空闲空闲", "BOTTOM", "存入存入存入存入存入存入存入存入");
        String pngBase64_2 = makeImageBase64(contentMap2);
        System.out.println(pngBase64_2);
    }

    public static String makeImageBase64(Map<String,String> contentMap){
        return convertToBase64(makeImage(contentMap),"png",true);
    }
    public static BufferedImage makeImage(Map<String,String> contentMap){
        String jsonStr = "{\"T1\":[{\"name\":\"BK\",\"level\":0,\"type\":\"1\",\"color\":\"#FFFFFF\",\"width\":250,\"height\":122,\"x\":0,\"y\":0},{\"name\":\"userName\",\"level\":1,\"type\":\"0\",\"color\":\"#000000\",\"font\":\"方正等线\",\"fontSize\":46,\"fontSpace\":-0.1,\"fontStyle\":1,\"layout\":0,\"width\":119,\"height\":43,\"x\":18,\"y\":24},{\"name\":\"userId\",\"level\":1,\"type\":\"0\",\"color\":\"#000000\",\"font\":\"等线\",\"fontSize\":18,\"fontStyle\":0,\"layout\":2,\"width\":77,\"height\":13,\"x\":158,\"y\":52},{\"name\":\"cabinetStatus\",\"level\":1,\"type\":\"0\",\"backgroundColor\":\"#FF0000\",\"color\":\"#FFFFFF\",\"font\":\"等线\",\"fontSize\":18,\"fontStyle\":0,\"layout\":1,\"width\":76,\"height\":25,\"x\":18,\"y\":77},{\"name\":\"controlTime\",\"level\":1,\"type\":\"0\",\"color\":\"#000000\",\"font\":\"等线\",\"fontSize\":18,\"fontSpace\":-0.05,\"fontStyle\":0,\"layout\":2,\"width\":116,\"height\":17,\"x\":118,\"y\":82},{\"name\":\"BC1\",\"level\":1,\"type\":\"1\",\"color\":\"#FF0000\",\"width\":250,\"height\":8,\"x\":0,\"y\":0},{\"name\":\"BC2\",\"level\":1,\"type\":\"1\",\"color\":\"#FF0000\",\"width\":250,\"height\":8,\"x\":0,\"y\":114}],\"T2\":[{\"name\":\"BK\",\"level\":0,\"type\":\"1\",\"color\":\"#FFFFFF\",\"width\":250,\"height\":122,\"x\":0,\"y\":0},{\"name\":\"userName\",\"level\":1,\"type\":\"0\",\"color\":\"#000000\",\"font\":\"方正等线\",\"fontSize\":46,\"fontSpace\":-0.1,\"fontStyle\":1,\"layout\":0,\"width\":119,\"height\":43,\"x\":18,\"y\":24},{\"name\":\"userId\",\"level\":1,\"type\":\"0\",\"color\":\"#000000\",\"font\":\"等线\",\"fontSize\":18,\"fontStyle\":0,\"layout\":2,\"width\":77,\"height\":13,\"x\":158,\"y\":52},{\"name\":\"cabinetStatus\",\"level\":1,\"type\":\"0\",\"backgroundColor\":\"#000000\",\"color\":\"#FFFFFF\",\"font\":\"等线\",\"fontSize\":18,\"fontStyle\":0,\"layout\":1,\"width\":76,\"height\":25,\"x\":18,\"y\":77},{\"name\":\"controlTime\",\"level\":1,\"type\":\"0\",\"color\":\"#000000\",\"font\":\"等线\",\"fontSize\":18,\"fontSpace\":-0.05,\"fontStyle\":0,\"layout\":2,\"width\":116,\"height\":17,\"x\":118,\"y\":82},{\"name\":\"BC1\",\"level\":1,\"type\":\"1\",\"color\":\"#FF0000\",\"width\":250,\"height\":8,\"x\":0,\"y\":0},{\"name\":\"BC2\",\"level\":1,\"type\":\"1\",\"color\":\"#FF0000\",\"width\":250,\"height\":8,\"x\":0,\"y\":114}],\"T3\":[{\"name\":\"BK\",\"level\":0,\"type\":\"1\",\"color\":\"#FFFFFF\",\"width\":250,\"height\":122,\"x\":0,\"y\":0},{\"name\":\"TOP\",\"level\":1,\"type\":\"0\",\"color\":\"#000000\",\"font\":\"等线\",\"fontSize\":17,\"fontSpace\":-0.15,\"fontStyle\":0,\"layout\":1,\"width\":221,\"height\":16,\"x\":15,\"y\":16},{\"name\":\"MIDDLE\",\"level\":1,\"type\":\"0\",\"color\":\"#000000\",\"font\":\"方正等线\",\"fontSize\":42,\"fontSpace\":-0.12,\"fontStyle\":1,\"layout\":1,\"width\":213,\"height\":39,\"x\":18,\"y\":40},{\"name\":\"BOTTOM\",\"level\":1,\"type\":\"0\",\"color\":\"#000000\",\"font\":\"等线\",\"fontSize\":17,\"fontSpace\":-0.15,\"fontStyle\":0,\"layout\":1,\"width\":221,\"height\":16,\"x\":15,\"y\":91}]}";
        final Type type = new TypeToken<Map<String, List<CustomBlock>>>() {
        }.getType();
        Map<String, List<CustomBlock>> jsonMap = JSONUtil.toBean(jsonStr, type, true);
        String templateId = contentMap.get("templateId");
        List<CustomBlock> blockList = jsonMap.get(templateId);
        Map<Integer, List<CustomBlock>> blockMapByLevel = blockList.stream().collect(Collectors.groupingBy(CustomBlock::getLevel));
        List<Integer> sortedLevel = blockMapByLevel.keySet().stream().filter(k -> !k.equals(0)).sorted().collect(Collectors.toList());
        CustomBlock backBlock = blockMapByLevel.get(0).get(0);

        String printRecordFileName = "./images/";
        try {
            //获取背景色
            Color backColor = Color.decode(backBlock.getColor());
            //创建模板需要的照片大小的背景图（按照片的背景颜色）
            BufferedImage back = new BufferedImage(backBlock.getWidth(), backBlock.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = back.createGraphics();
            graphics.setColor(backColor);
            graphics.fillRect(0, 0, backBlock.getWidth(), backBlock.getHeight());
            graphics.dispose();

            ImageCombiner combiner = new ImageCombiner(back, OutputFormat.PNG);
            for (Integer level : sortedLevel) {
                for (CustomBlock block : blockMapByLevel.get(level)) {
                    String blockContent = contentMap.get(block.getName());
                    if (Objects.equals(block.getType(),"0") && StringUtils.isBlank(blockContent)) {
                        blockContent = " ";
                    }
                    switch (block.getType()) {
                        case "0": //文本
                            TextElement textElement = new TextElement(blockContent, getFont(block), block.getX(), block.getY()).setColor(Color.decode(block.getColor())).setBaseLine(BaseLine.Top);
                            if (Objects.equals(1, block.getLayout())) {
                                textElement.setCenter(true);
                            } else if (Objects.equals(2, block.getLayout())) {

                            }
                            if (Objects.nonNull(block.getFontSpace())) {
                                textElement.setSpace(block.getFontSpace());
                            }
                            if (StringUtils.isNotBlank(block.getBackgroundColor())) {
                                Color bgColor = Color.decode(block.getBackgroundColor());
                                if(Objects.equals("",blockContent.trim()) && !Objects.equals(backColor,Color.decode(block.getBackgroundColor()))){
                                    bgColor = backColor;
                                }
                                int diff = block.getHeight() - textElement.getHeight();
                                int r = diff % 2;
                                int autoY = (diff+r) / 2;
                                ImageCombiner cTemp = new ImageCombiner(block.getWidth(), block.getHeight()+r, bgColor, OutputFormat.PNG);
                                textElement.setY(autoY);
                                cTemp.addElement(textElement);
                                BufferedImage imageTemp = cTemp.combine();
                                combiner.addImageElement(imageTemp, block.getX(), block.getY());
                            } else {
                                combiner.addElement(textElement);
                            }
                            break;
                        case "1"://线条，色块
                            combiner.addRectangleElement(block.getX(), block.getY(), block.getWidth(), block.getHeight()).setColor(Color.decode(block.getColor()));
                            break;
                        case "2"://图片

                            break;
                    }
                }
            }
            BufferedImage combine = combiner.combine();
            combiner.save(printRecordFileName + templateId + ".png");
            return combine;
        } catch (Exception ex) {
            log.error("{},{}", ex.getMessage(), ex);
            throw new RuntimeException("保存预览文件异常：" + ex.getMessage());
        }
    }
    private static Font getFont(CustomBlock fontBlock) {
        return new Font(fontBlock.getFont(), fontBlock.getFontStyle(), fontBlock.getFontSize());
    }
    private static String convertToBase64(BufferedImage image, String formatName,boolean needHead) {
        String head = String.format("data:image/%s;base64,",formatName);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            // 将BufferedImage写入字节流
            ImageIO.write(image, formatName, outputStream);
            // 转换为字节数组并编码为Base64
            String imageBase64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            return needHead ? head + imageBase64 : imageBase64;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
