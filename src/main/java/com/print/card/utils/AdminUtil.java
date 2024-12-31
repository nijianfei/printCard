package com.print.card.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Slf4j
public class AdminUtil {
    public static boolean isAdmin(){
        boolean isAdmin = false;
        try {
            // 尝试执行需要管理员权限的命令
            Process process = Runtime.getRuntime().exec("net session");
            int exitCode = process.waitFor();

            // 如果命令成功执行（退出码为0），则假设当前用户是管理员
            // 注意：这不是一个可靠的方法，因为退出码为0也可能表示命令以某种方式成功执行，但不一定意味着管理员权限
            if (exitCode == 0) {
                isAdmin = true;
            } else {
                // 命令执行失败，可能表示没有管理员权限
                // 但请注意，这也可能表示命令由于其他原因失败（例如，网络问题）
                log.error("Command executed with non-zero exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            // 捕获异常，可能表示没有权限执行命令或其他IO问题
            log.error("isAdmin check failed", e);
        }
        return isAdmin;
    }
    public static void printPermissionGroup(){
        try {
            // 构建命令和参数
            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", "whoami", "/groups");

            // 启动进程
            Process process = processBuilder.start();

            // 获取命令执行的输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(),"GBK"));
            String line;
            log.info("获取权限组列表:");
            while ((line = reader.readLine()) != null) {
                log.info(line);
            }
            // 关闭读取器
            reader.close();

            // 等待进程结束并获取退出值
            int exitCode = process.waitFor();
            log.info("Exited with code: " + exitCode);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
