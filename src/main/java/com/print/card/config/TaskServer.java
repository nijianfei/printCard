package com.print.card.config;

import com.alibaba.fastjson.JSON;
import com.print.card.PrintStatusEnum;
import com.print.card.dto.PrintResultDto;
import com.print.card.jna.DllLoadIn;
import com.print.card.utils.CommandUtil;
import com.sun.jna.WString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class TaskServer {
    private static boolean isReady = false;
    private static boolean isBusy = false;
    private static PrintStatusEnum pse = PrintStatusEnum.Offline;
    private static PrintResultDto statusResult;
    private static JLabel labelTitle = getLabel("打印工卡时,请勿进行任何键盘操作",20);
    private static JLabel labelStatus = getLabel("",16);
    private static JLabel labelResult = getLabel("",16);



    @Value("#{T(java.lang.Boolean).parseBoolean('${isPrint:true}')}")
    private Boolean isPrint;
    @Value("#{T(java.lang.Boolean).parseBoolean('${isCheckStatus:true}')}")
    private Boolean isCheckStatus;
    public static final JFrame frame = getJFrame();
    @Scheduled(cron = "0/2 * * * * ?")
    public void checkPrintStatus() {
        try {

            SwingUtilities.invokeLater(() -> {
                labelStatus.setText("打印机状态："+pse.getName());
            });

            if (!isPrint && !isCheckStatus ) {
                log.error("已开启测试模式！TaskServer - 不进行打印机状态获取！---------------------------------------------------》");
                return;
            }
            String printersXml = DllLoadIn.instance.callFunc(new WString(CommandUtil.query_printers_with_status())).toString();
            Map<String,String> printResult = CommandUtil.parse_query_printers_with_status(printersXml);
            log.info("TaskServer_打印机信息：-->\r\n{}", JSON.toJSONString(printResult));
            if (Objects.isNull(printResult)) {
                return;
            }

            String statusXml = DllLoadIn.instance.callFunc(new WString(CommandUtil.get_printer_status(printResult.get("port_number"),printResult.get("hardware_type")))).toString();
            statusResult = CommandUtil.parse_get_printer_status(statusXml);
            log.info("TaskServer_打印机状态    --->：\r\n--->{}", JSON.toJSONString(statusResult));
            if (statusResult.isAccepted()) {
                isReady = Objects.equals(PrintStatusEnum.Ready.getCode(), statusResult.getPrintStatus());
                isBusy = Objects.equals(PrintStatusEnum.Busy.getCode(), statusResult.getPrintStatus());
                pse = PrintStatusEnum.getByCode(statusResult.getPrintStatus());
            }else {
                isReady = Objects.equals(PrintStatusEnum.Ready.getCode(), statusResult.getPrintStatus());
                pse = PrintStatusEnum.getByCode(statusResult.getPrintStatus());
            }
        } catch (Exception e) {
            log.error("TaskServer_exc:{}",e.getMessage(),e);
        }catch (Error e){
            log.error("TaskServer_err:{}",e.getMessage(),e);
        }
    }

    public static boolean isReady(){
        return isReady;
    }

    public static boolean isBusy(){
        return isBusy;
    }
    public static PrintStatusEnum getStatus(){
        return pse;
    }

    public static PrintResultDto getStatusResult(){
        return statusResult;
    }

    private static JFrame getJFrame() {
        JFrame frame = new JFrame();
        frame.setSize(350, 200);
        frame.setResizable(false);
        frame.setExtendedState(JFrame.NORMAL);
        frame.setUndecorated(true);
        frame.setAlwaysOnTop(true);
        frame.setLayout(new GridLayout(2, 1));
        JPanel panelTitle = getJPanel(50,labelTitle);
        JPanel panelStatus = getJPanel(0,labelStatus);
//        JPanel panelResult = getJPanel(labelResult);


        frame.getContentPane().add(panelTitle);
        frame.getContentPane().add(panelStatus);
//        frame.getContentPane().add(panelResult);

        // 窗体居中
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Rectangle screenRectangle = new Rectangle(screenSize);
        frame.setLocation((int)(screenRectangle.getCenterX() - frame.getWidth() / 2),
                (int)(screenRectangle.getCenterY() - frame.getHeight() / 2));
        //关闭按钮不生效
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

//        frame.setVisible(true);
//        frame.requestFocus();
//        frame.toFront();

        return frame;
    }

    private static JPanel getJPanel(int top,JLabel label) {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(top, 10, 0, 10)); // 设置 JPanel 的边框作为外边距
        panel.add(label);
        return panel;
    }

    private static JLabel getLabel(String content, int fondSize) {
        JLabel label = new JLabel(content);
        label.setFont(new Font("微软雅黑", Font.BOLD, fondSize));
        return label;
    }
}
