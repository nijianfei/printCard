package com.print.card;

import com.print.card.dto.PrintResultDto;
import com.print.card.jna.KeyHook;
import com.print.card.utils.CommandUtil;
import org.dom4j.DocumentException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.awt.*;
import java.util.Locale;

import static com.alibaba.fastjson.JSON.toJSONString;

@SpringBootTest
class CardApplicationTests {

	public static void main(String[] args) throws DocumentException {
		new Thread(()->{
			System.out.println("hook_end:"+KeyHook.instance.installHook());

		}).start();
		for (int i = 0; i < 10; i++) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			System.out.println("cardId:"+KeyHook.instance.getCard());
		}
		KeyHook.instance.unHook();
		System.out.println("kkkk");




		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		String[] availableFontFamilyNames = ge.getAvailableFontFamilyNames(Locale.CHINA);
		for (String fontName : availableFontFamilyNames) {
			System.out.println("\nfontName: "+fontName);
		}

//		Font[] allFonts = ge.getAllFonts();
//		for (Font font : allFonts) {
//			System.out.println("\nfontInfo: "+JSON.toJSONString(font));
//		}
		String testJson = "<result accepted=\"true\">\n" +
				"        <status value=\"Busy\" paused=\"false\" simulated=\"false\">\n" +
				"                <sub_status value=\"Printing\" />\n" +
				"        </status>\n" +
				"        <hardware core=\"3000\" type=\"XID8300DS\" />\n" +
				"        <custom ribbon_type=\"YMCK\" dual_printing_enabled=\"true\" laminator_connected=\"false\" firmware=\"V11-01B \" card=\"Any Cards\" ink=\"25\" ink_max=\"50\" ink_panel_set_count=\"1000\" ink_lot_number=\"YD4141\" film=\"5\" film_max=\"10\" mg_option=\"None\" additional_sense_code=\"0\" sense_key=\"0\" mac_address=\"90-3D-68-02-33-CA\" door_status=\"Unlocked\" is_print_mac_address_option_available=\"true\" free_count=\"0\" total_count=\"83\" head_count=\"539\" cleaning_count=\"83\" error_count=\"0\" />\n" +
				"</result>";
		PrintResultDto printResultDto = CommandUtil.parse_get_printer_status(testJson);
		System.out.println(toJSONString(printResultDto));
	}
	@Test
	void contextLoads() {
	}

}
