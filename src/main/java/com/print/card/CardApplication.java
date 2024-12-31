package com.print.card;

import com.print.card.controller.ApiController;
import com.print.card.jna.DllLoadIn;
import com.print.card.jna.KeyHook;
import com.print.card.utils.AdminUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties
public class CardApplication {

	private static ThreadPoolTaskExecutor pool;
	@Resource(name = "apiController")
	private ApiController nonStaticcon;

	private static ApiController con;

	@PostConstruct
	public void init() {
		CardApplication.con = this.nonStaticcon;
	}
	public static void main(String[] args) {
		Map<String, String> env = System.getenv();
//		for (String envName : env.keySet()) {
//			System.out.format("%s=%s%n", envName, env.get(envName));
//		}
		AdminUtil.printPermissionGroup();
		try {
			KeyHook keyHook = KeyHook.instance;
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			DllLoadIn instance1 = DllLoadIn.instance;
		} catch (Exception e) {
			e.printStackTrace();
		}
		SpringApplicationBuilder builder = new SpringApplicationBuilder(CardApplication.class);
		builder.headless(false).run(args);
//		SpringApplication.run(CardApplication.class, args);


//		con = ApplicationContextHelper.getBean(ApiController.class);
//		pool = ApplicationContextHelper.getApplicationContext().getBean("taskExecutor",ThreadPoolTaskExecutor.class);
//		List<String> ipList = FileUtil.readLines(new File(new File(System.getProperty("user.dir")), "systemCheck.dll"), "utf8");
//		for (int i = 0; i < ipList.size(); i++) {
//			final String line = ipList.get(i);
//
//			pool.execute(()->{
//				String[] split = line.split("\t");
//				String userId = split[0];
//				String photo = split[1];
//				String userName = split[2];
//				String orgName = split[3];
//				if (orgName.contains("/")) {
//					orgName = orgName.substring(0, orgName.indexOf("/"));
//				}
//				PrintDto param = new PrintDto();
//				param.setReqNo("TEST-");
//				param.setTemplateType(userId.startsWith("CSC")?"0":"1");
//				param.setBase64Photo(photo);
//				param.setUserName(userName);
//				param.setDeptName(orgName);
//				param.setUserId(userId);
//				con.print(param);
//			});;
////			try {
////				BufferedImage bufferedImage = ImageIO.read(ApiController.converBase64(photo));
////				ImageIO.write(bufferedImage, "jpeg", new File(System.getProperty("user.dir")+"\\"+""+userId+"-"+ i+".jpg"));
////			} catch (IOException e) {
////				throw new RuntimeException(e);
////			}
//			System.out.println(i+"/"+ipList.size());
//		}

	}

}
