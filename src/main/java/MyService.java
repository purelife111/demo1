import org.jsoup.Jsoup;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyService extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        download(req,resp);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        download(req,resp);
    }

    public String download(HttpServletRequest request, HttpServletResponse response) {
        String url = request.getParameter("url");
        if (url == null || "".equals(url)){
            return "Invalid url";
        }
        String videoPath = removeWaterMark(url);
        String fileName = genVideoName(url);// 文件名
        if (fileName != null) {
            //设置文件路径
            File file = new File(videoPath);
            //File file = new File(realPath , fileName);
            if (file.exists()) {
                response.setContentType("video/mp4");// 设置强制下载不打开
                response.addHeader("Content-Disposition", "attachment;fileName=" + fileName);// 设置文件名
                byte[] buffer = new byte[1024];
                FileInputStream fis = null;
                BufferedInputStream bis = null;
                try {
                    fis = new FileInputStream(file);
                    bis = new BufferedInputStream(fis);
                    OutputStream os = response.getOutputStream();
                    int i = bis.read(buffer);
                    while (i != -1) {
                        os.write(buffer, 0, i);
                        i = bis.read(buffer);
                    }
                    return "下载成功";
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (bis != null) {
                        try {
                            bis.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return "下载失败";
    }


    public String removeWaterMark(String url) {
        try {
            //●抖音链接(使用手机分享功能,复制链接)
            //String url = "http://v.douyin.com/jrwXjw/";

            //1.利用Jsoup抓取抖音链接
            String htmls = Jsoup.connect(url).ignoreContentType(true).execute().body(); //抓取抖音网页
            //System.out.println(htmls); //做测试时使用

            //2.利用正则匹配可以抖音下载链接
            //playAddr: "https://aweme.snssdk.com/aweme/v1/playwm/?video_id=v0200ffc0000bfil6o4mavffbmroeo80&line=0",
            //具体匹配内容格式：「https://aweme.snssdk.com/aweme/...line=0」
            Pattern patternCompile = Pattern.compile("(?<=playAddr: \")https?://.+(?=\",)");
            //利用Pattern.compile("正则条件").matcher("匹配的字符串对象")方法可以将需要匹配的字段进行匹配封装 返回一个封装了匹配的字符串Matcher对象

            //3.匹配后封装成Matcher对象
            Matcher m = patternCompile.matcher(htmls);

            //4.①利用Matcher中的group方法获取匹配的特定字符串 ②利用String的replace方法替换特定字符,得到抖音的去水印链接
            String matchUrl = "";
            while (m.find()) {
                matchUrl = m.group(0).replaceAll("playwm", "play");
            }

            //5.将链接封装成流
            //注:由于抖音对请求头有限制,只能设置一个伪装手机浏览器请求头才可实现去水印下载
            Map<String, String> headers = new HashMap<>();
            headers.put("Connection", "keep-alive");
            headers.put("Host", "aweme.snssdk.com");
            headers.put("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 12_1_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/16D57 Version/12.0 Safari/604.1");

            //6.利用Joup获取视频对象,并作封装成一个输入流对象
            BufferedInputStream in = Jsoup.connect(matchUrl).headers(headers).timeout(10000).ignoreContentType(true).execute().bodyStream();

            //7.封装一个保存文件的路径对象

            String name = genVideoName(url);
            String videoPath = "video/".concat(name);
            File fileSavePath = new File(videoPath);

            //注:如果保存文件夹不存在,那么则创建该文件夹
            File fileParent = fileSavePath.getParentFile();
            if (!fileParent.exists()) {
                fileParent.mkdirs();
            }

            //8.新建一个输出流对象
            OutputStream out =
                    new BufferedOutputStream(
                            new FileOutputStream(fileSavePath));

            //9.遍历输出文件
            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }

            out.close();//关闭输出流
            in.close(); //关闭输入流

            //注:打印获取的链接
            System.out.println("-----抖音去水印链接-----\n" + matchUrl);
            System.out.println("\n-----视频保存路径-----\n" + fileSavePath.getAbsolutePath());
            return videoPath;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Invalid url!";
    }

    public String getUrlSuffix(String url) {
        String suffix = "";
        if (!"".equals(url) && url != null) {
            String arr[] = url.split("/");
            if (arr.length > 1) {
                suffix = arr[arr.length - 1];
            }
        }
        return suffix;
    }

    public String genVideoName(String url){
        StringBuilder videoName = new StringBuilder("douyin_").append(getUrlSuffix(url)).append("_").append(System.currentTimeMillis()).append(".mp4");
        return videoName.toString();
    }
}
