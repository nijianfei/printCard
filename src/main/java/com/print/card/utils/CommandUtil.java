package com.print.card.utils;

import com.alibaba.fastjson.JSON;
import com.print.card.PrintStatusEnum;
import com.print.card.dto.PrintResultDto;
import com.print.card.jna.DllLoadIn;
import com.sun.jna.WString;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;

@Slf4j
public class CommandUtil {
    public CommandUtil() {
    }

    public static String format_connect(String server, String application_id) {
        String command = "<command name=\"connect\" server=\"%s\" application_id=\"%s\" />";
        return String.format(command, server, application_id);
    }

    public static String parse_connect(String xmlStr) {
        try {
            Document rootElement = getElement(xmlStr);
            String connection_handle = rootElement.getRootElement().attributeValue("connection_handle");
            return connection_handle;
        } catch (Exception var3) {
            var3.printStackTrace();
            return null;
        }
    }

    public static String format_add_new_xml_job(String connection_handle, String group_name) {
        String command = "<command name=\"add_new_xml_job\" connection_handle=\"%s\"><job_data><general_data machine_name=\"local\" group_name=\"%s\"/><device_profiles><device_profile position=\"1\" type=\"Printer\"><printing_preferences></printing_preferences></device_profile></device_profiles><card_data><layout><front orientation=\"portait\"><cmy><full_bitmap  value=\"@front\" /></cmy></front><back orientation=\"portait\"><cmy><full_bitmap  value=\"@back\" /></cmy></back></layout></card_data></job_data></command>";
        return String.format(command, connection_handle, group_name);
    }

    public static String parse_add_new_xml_job(String xmlStr) {
        try {
            System.out.println("创建任务返回");
            System.out.println(xmlStr);
            Document document = getElement(xmlStr);
            return document.getRootElement().element("xml_job").attributeValue("job_id");
        } catch (Exception var2) {
            var2.printStackTrace();
            return null;
        }
    }

    public static String format_setting_source(String connection_handle, String job_id, String name) {
        String command = "<resource connection_handle=\"%s\" job_id=\"%s\" name=\"%s\" />";
        return String.format(command, connection_handle, job_id, name);
    }



    public static String parse_setting_source(String xmlStr) {
        try {
            System.out.println("响应元数据:" + xmlStr);
            Document document = getElement(xmlStr);
            return document.getRootElement().attributeValue("accepted");
        } catch (Exception var2) {
            var2.printStackTrace();
            return null;
        }
    }

    public static String format_position_card(String port, String port_number, String hardware_type, String destination) {
        String command = "<command name=\"device_command\"> <printer port=\"%s\" port_number=\"%s\" hardware_type=\"%s\" /><device_command name=\"position_card\" destination=\"%s\" /></command>";
        return String.format(command, port, port_number, hardware_type, destination);
    }

    public static String format_query_local_device_statuses() {
        String command = "<command name=\"query_local_device_statuses\" />";
        return command;
    }

    public static Map<String, String> parse_query_local_device_statuses(String xmlStr) {
        try {
            Document document = getElement(xmlStr);
            Element rootElement = document.getRootElement();
            Element printers = rootElement.element("printers");
            Element printer = printers.element("printer");
            List<Attribute> attributes = printer.attributes();
            HashMap<String, String> map = new HashMap();
            Iterator var7 = attributes.iterator();

            while(var7.hasNext()) {
                Attribute attribute = (Attribute)var7.next();
                String value = attribute.getValue();
                String name = attribute.getName();
                map.put(name, value);
            }

            return map;
        } catch (Exception var11) {
            var11.printStackTrace();
            return null;
        }
    }

    public static String format_read_mag(String port, String port_number, String hardware_type) {
        String command = "<command name=\"device_command\"><printer port=\"%s\" port_number=\"%s\" hardware_type=\"%s\" /><device_command name=\"read_mag\"><tracks><track1 format=\"iso6_79\" /><track2 format=\"iso4_37\" /><track3 format=\"iso4_104\" /></tracks></device_command></command>";
        return String.format(command, port, port_number, hardware_type);
    }

    public static String format_write_mag(String port, String port_number, String hardware_type) {
        String command = "<command name=\"device_command\"><printer port=\"%s\" port_number=\"%s\" hardware_type=\"%s\" /><device_command name=\"write_mag\"><tracks><track1 format=\"iso6_79\" data=\"ABCDEFGH1234\" /><track2 format=\"iso4_37\" data=\"12345\" /><track3 format=\"iso4_104\" data=\"67890\" /></tracks></device_command></command>";
        return String.format(command, port, port_number, hardware_type);
    }

    public static List<Map<String, String>> parse_read_mag(String xmlStr) {
        try {
            List<Map<String, String>> result = new ArrayList();
            Document document = getElement(xmlStr);
            Element rootElement = document.getRootElement();
            String accepted = rootElement.attributeValue("accepted");
            Element tracks = rootElement.element("tracks");
            Iterator<Element> elementIterator = tracks.elementIterator();

            while(elementIterator.hasNext()) {
                Element next = (Element)elementIterator.next();
                HashMap<String, String> map = new HashMap();
                List<Attribute> attributes = next.attributes();
                Iterator var10 = attributes.iterator();

                while(var10.hasNext()) {
                    Attribute attribute = (Attribute)var10.next();
                    String name = attribute.getName();
                    String value = attribute.getValue();
                    map.put(name, value);
                }

                result.add(map);
            }

            return result;
        } catch (Exception var14) {
            var14.printStackTrace();
            return null;
        }
    }

    public static String format_start_xml_job(String connection_handle, String job_id) {
        String command = "<command name=\"start_xml_job\" connection_handle=\"%s\" job_id=\"%s\"  />";
        String format = String.format(command, connection_handle, job_id);
        System.out.println(" 开始执行指令 组装");
        System.out.println(format);
        return format;
    }

    public static String format_resume_xml_job(String connection_handle, String job_id) {
        String command = "<resource connection_handle=\"%s\" job_id=\"%s\" name=\"resume_xml_job\" />";
        return String.format(command, connection_handle, job_id);
    }

    public static String format_disconnect(String connection_handle) {
        String command = "<command connection_handle=\"%s\" name=\"disconnect\" />";
        return String.format(command, connection_handle);
    }

    public static WString commandSubJoint(String name, Map<String, String> paramMap) {
        StringBuilder command = new StringBuilder();
        command.append("<command name=\"" + name + "\" ");
        paramMap.entrySet().forEach((entry) -> {
            command.append(" " + (String)entry.getKey() + "=\"" + (String)entry.getValue() + "\"");
        });
        command.append("/>");
        return new WString(command.toString());
    }

    private static Document getElement(String xmlStr) throws DocumentException {
        Document document = (new SAXReader()).read(new ByteArrayInputStream(xmlStr.getBytes()));
        return document;
    }

    private static byte[] getFileByte(String path) {
        File file = new File(path);

        try {
            InputStream inputStream = new FileInputStream(file);
            Throwable var3 = null;

            try {
                byte[] bytes = new byte[(int)file.length()];

                while(inputStream.read(bytes) != -1) {
                }

                byte[] var6 = bytes;
                return var6;
            } catch (Throwable var16) {
                var3 = var16;
                throw var16;
            } finally {
                if (inputStream != null) {
                    if (var3 != null) {
                        try {
                            inputStream.close();
                        } catch (Throwable var15) {
                            var3.addSuppressed(var15);
                        }
                    } else {
                        inputStream.close();
                    }
                }

            }
        } catch (Exception var18) {
            var18.printStackTrace();
            return null;
        }
    }

    public static void main12(String[] args) {
        String s = format_position_card("1", "1", "1", "PrintPosition");
        System.out.println(s);
    }

    public static void main1(String[] args) throws DocumentException {
        String connect = format_connect("127.0.0.1", "123123");
        String connection_handle = parse_connect(DllLoadIn.instance.callFunc(new WString(connect)).toString());
        System.out.println("=====第一步_创建连接_connection_handle=" + connection_handle);
        String job_id = parse_add_new_xml_job(DllLoadIn.instance.callFunc(new WString(format_add_new_xml_job(connection_handle, "test_group_name"))).toString());
        System.out.println("=====第二步_创建任务_job_id=" + job_id);
        String command_text_print = format_setting_source(connection_handle, job_id, "@printername");
        byte[] byteText = "文本资源".getBytes(StandardCharsets.UTF_8);
        String parse_print_source_text_response = parse_setting_source(DllLoadIn.instance.setResource(new WString(command_text_print), byteText, byteText.length).toString());
        System.out.println("=====第三步_第一个文本资源设置_parse_print_source_text_response=" + parse_print_source_text_response);
        String command_text_file = format_setting_source(connection_handle, job_id, "@front");
        byte[] byteFile = getFileByte("/Users/liyawei/Downloads/89140c9d2dbb41a79980e8c0e3300679.jpeg");
        String parse_print_source_file_response = parse_setting_source(DllLoadIn.instance.setResource(new WString(command_text_file), byteFile, byteFile.length).toString());
        System.out.println("=====第四步_第二个图像资源设置_parse_print_source_file_response=" + parse_print_source_file_response);
        String command_response_start = parse_setting_source(DllLoadIn.instance.callFunc(new WString(format_start_xml_job(connection_handle, job_id))).toString());
        System.out.println("=====第五步_开始执行指令_command_response_start=" + command_response_start);
        String command_response_dis = parse_setting_source(DllLoadIn.instance.callFunc(new WString(format_disconnect(connection_handle))).toString());
        System.out.println("=====第六步_断开连接_command_response_dis=" + command_response_dis);
    }

    public static String getRfid() throws InterruptedException {
        JFrame frame = new JFrame("rfid print");
        JTextField textField = new JTextField(20);
        frame.setDefaultCloseOperation(2);
        frame.setLayout(new FlowLayout());
        frame.add(textField);
        frame.setSize(300, 200);
        frame.setLocationRelativeTo((Component)null);
        frame.setVisible(true);
        Thread.sleep(5000L);
        String text = textField.getText();
        frame.dispose();
        return text;
    }

    private static String getClipboardContent(Clipboard clipboard) {
        Transferable transferable = clipboard.getContents((Object)null);
        if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                return (String)transferable.getTransferData(DataFlavor.stringFlavor);
            } catch (IOException | UnsupportedFlavorException var3) {
                var3.printStackTrace();
            }
        }

        return null;
    }



    public static void main(String[] args) throws DocumentException {
        String aa = "<result accepted=\"true\">\n" +
                "\t<status value=\"Ready\" paused=\"false\" />\n" +
                "\t<printer_plugin\n" +
                "\t\t\t\t\tname=\"xid8usb\"\n" +
                "\t\t\t\t\tcurrent=\"xid8usb\"\n" +
                "\t\t\t\t\tcommon_name=\"xid8common\" />\n" +
                "\t<hardware\n" +
                "\t\t\t  core=\"3000\"\n" +
                "\t\t\t  type=\"XID8300DS\" />\n" +
                "\t<custom\n" +
                "\t\t\tribbon_type=\"YMCK-PO\"\n" +
                "\t\t\tlaminator=\"not_connected\"\n" +
                "\t\t\tcard=\"Any Cards\"\n" +
                "\t\t\tink=\"32\"\n" +
                "\t\t\tink_max=\"50\"\n" +
                "\t\t\tfilm=\"4\"\n" +
                "\t\t\tfilm_max=\"10\"\n" +
                "\t\t\tadditional_sense_code=\"0\"\n" +
                "\t\t\tsense_key=\"0\" />\n" +
                "</result>";
        PrintResultDto statusResult = CommandUtil.parse_get_printer_status(aa);
        System.out.println("TaskServer_打印机状态：{}"+JSON.toJSONString(statusResult));
    }


    public static String query_printers_with_status(){
        return LabelUtil.buildLabel("command",null,Map.of("name","query_printers_with_status"));
    }

    public static Map<String, String> parse_query_printers_with_status(String xmlStr) throws DocumentException {
        //log.info("parse_query_printers_with_status-xmlStr:{}",xmlStr);
        Document element = getElement(xmlStr);
        Element rootElement = element.getRootElement();
        if (Boolean.parseBoolean(rootElement.attributeValue("accepted"))) {
            Element printers = rootElement.element("printers");
            Element printer = printers.element("printer");
            return Map.of("port_number",printer.attributeValue("port_number"),"hardware_type",printer.attributeValue("hardware_type"));
        }
        return null;
    }


    public static String get_printer_status(String portNumber,String hardwareType){
        return LabelUtil.buildLabel("command",LabelUtil.buildLabel("printer",null,Map.of("port_number",portNumber, "port","USB", "hardware_type",hardwareType)),
                Map.of("name","get_printer_status"));
    }
    public static PrintResultDto parse_get_printer_status(String xmlStr) throws DocumentException {
        //log.info("parse_get_printer_status-xmlStr:{}",xmlStr);
        Document element = getElement(xmlStr);
        Element rootElement = element.getRootElement();


        PrintResultDto pResult = new PrintResultDto();
        String accepted = rootElement.attributeValue("accepted");
        if (Boolean.parseBoolean(accepted)) {
            //<result accepted="true"><status value="Offline" paused="false" simulated="false" /><hardware core="3000" type="XID8300DS" /></result>

            //<result accepted="true"><status value="Ready" paused="false" simulated="false" /><hardware core="3000" type="XID8300DS" />
            //<custom ribbon_type="YMCK" dual_printing_enabled="true" laminator_connected="false" firmware="V11-01B " card="Any Cards" ink="25" ink_max="50" ink_panel_set_count="1000" ink_lot_number="YD4141" film="5" film_max="10" mg_option="None" additional_sense_code="0" sense_key="0" mac_address="90-3D-68-02-33-CA" door_status="Unlocked" is_print_mac_address_option_available="true" free_count="0" total_count="83" head_count="539" cleaning_count="83" error_count="0" />
            //</result>


            //<result accepted="true">
            //        <status value="Busy" paused="false" simulated="false">
            //        <sub_status value="Preheating" />  //正在预热
            //        </status>
            pResult.setAccepted(accepted);
            Element status = rootElement.element("status");
            pResult.setPrintStatus(status.attributeValue("value"));
            if (Objects.equals(pResult.getPrintStatus(), PrintStatusEnum.Busy.getCode())) {
                Element subStatus = status.element("sub_status");
                if (Objects.nonNull(subStatus)) {
                    pResult.setPrintSubStatusStatus(subStatus.attributeValue("value"));
                }
            }

            pResult.setErrorMsg(status.attributeValue("error"));
            pResult.setErrorCode(status.attributeValue("error_code"));
            return pResult;
        }else {
            //<result accepted="false" error_code="101035" error="Invalid XML structure" translated_error="Invalid XML structure." />
            pResult.setAccepted(accepted);
            pResult.setErrorCode(rootElement.attributeValue("error_code"));
            pResult.setErrorMsg(rootElement.attributeValue("error"));
            return pResult;
        }
    }
}
