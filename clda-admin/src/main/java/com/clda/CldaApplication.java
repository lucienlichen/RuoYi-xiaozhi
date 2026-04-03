package com.clda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 启动程序
 * 
 * @author clda
 */
@EnableAsync
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class CldaApplication {

    public static void main(String[] args) {
        // 设置 JNA library path 以便 tess4j 找到 libtesseract 原生库
        String jnaPath = System.getProperty("jna.library.path", "");
        if (!jnaPath.contains("/usr/local/lib")) {
            System.setProperty("jna.library.path", jnaPath.isEmpty() ? "/usr/local/lib" : jnaPath + ":/usr/local/lib");
        }
        SpringApplication.run(CldaApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  若依小智启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
                " .-------.       ____     __        \n" +
                " |  _ _   \\      \\   \\   /  /    \n" +
                " | ( ' )  |       \\  _. /  '       \n" +
                " |(_ o _) /        _( )_ .'         \n" +
                " | (_,_).' __  ___(_ o _)'          \n" +
                " |  |\\ \\  |  ||   |(_,_)'         \n" +
                " |  | \\ `'   /|   `-'  /           \n" +
                " |  |  \\    /  \\      /           \n" +
                " ''-'   `'-'    `-..-'              ");
    }

}
