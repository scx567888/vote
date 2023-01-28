package cool.scx.vote;

import com.fasterxml.jackson.core.JsonProcessingException;
import cool.scx.config.ScxEnvironment;
import cool.scx.enumeration.HttpMethod;
import cool.scx.http_client.ScxHttpClientHelper;
import cool.scx.http_client.ScxHttpClientRequest;
import cool.scx.http_client.body.JsonBody;
import cool.scx.logging.ScxLoggerFactory;
import cool.scx.logging.ScxLoggingLevel;
import cool.scx.logging.ScxLoggingType;
import cool.scx.util.Base64Utils;
import cool.scx.util.ObjectUtils;
import cool.scx.util.ansi.Ansi;
import cool.scx.util.ansi.AnsiStyle;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

final class Vote {

    static {
        //初始化 logger
        initLogger();
    }

    static final Logger logger = LoggerFactory.getLogger(Vote.class);

    //投票 url
    static final String VOTE_URL = "https://apinext.sanook.com/season/v1/votes/topoftheyear2022/";
    //验证码 url
    static final String CAPTCHA_URL = "https://apinext.sanook.com/captcha/captchas/site/340f231e-772f-4281-9a80-aebd91b9525f";
    //固定代码 (可能代表哪次投票)
    static final String CODE = "sn-cc-2022";
    // 轮询 id (不知道是干啥的,反正每次都是1)
    static final String POLL_ID = "1";
    //初始化 ocr 工具包 这里不使用静态成员是因为 多线程共享一个 TESSERACT 会报错,这里采取每个线程执行一个单独的 Vote 来实现多线程 , 具体可以搜索 tess4j 多线程
    final Tesseract TESSERACT = initOCR();
    final boolean showInfo;

    public Vote(boolean showInfo) {
        this.showInfo = showInfo;
    }

    /**
     * @param choice_id 选手 id
     * @return 投票是否成功
     * @throws IOException e
     */
    public boolean vote(String choice_id) throws Exception {
        //获取验证码
        logger.debug("获取验证码中...");
        var captcha = getCaptcha();
        logger.debug("获取验证码成功!!!");

        //读取验证码
        logger.debug("读取验证码中...");
        var captchaValue = readFromImage(captcha.getImage());
        if (showInfo) {
            Ansi.out().brightBlue("读取验证码成功 !!! : ").brightYellow(captchaValue, AnsiStyle.BOLD).println();
        }

        //拼接 token 规则如下 {验证码的随机值 r}:{验证码值}{固定代码}
        //之后进行 base64 编码生成 真正的 token
        var _token = captcha.r + ":" + captchaValue + CODE;
        var token = Base64Utils.encodeToString(_token);
        token = token.replaceFirst("x", "sax")
                .replaceFirst("y", "noy")
                .replaceFirst("z", "okz");

        //发起投票请求
        logger.debug("发起投票请求...");
        var res = ScxHttpClientHelper.post(VOTE_URL, new JsonBody(Map.of(
                "choice_id", choice_id,
                "poll_id", POLL_ID,
                "token", token
        )));
        // 201 为投票成功
        if (res.statusCode() == 201) {
            if (showInfo) {
                Ansi.out().brightGreen("投票成功 !!! -> " + res.statusCode() + " : " + res.body()).println();
            }
            return true;
        } else {
            if (showInfo) {
                Ansi.out().brightRed("投票失败 !!! -> " + res.statusCode() + " : " + res.body()).println();
            }
            return false;
        }
    }

    private static Tesseract initOCR() {
        var senv = new ScxEnvironment(Vote.class);
        var traineddata = senv.getPathByAppRoot("AppRoot:traineddata");
        var t = new Tesseract();
        t.setLanguage("eng");
        t.setDatapath(traineddata.toString());
        return t;
    }

    /**
     * 获取验证码
     *
     * @return a
     * @throws JsonProcessingException a
     */
    private static Captcha getCaptcha() throws Exception {
        var res = ScxHttpClientHelper.request(new ScxHttpClientRequest()
                        .method(HttpMethod.POST)
                        .uri(URI.create(CAPTCHA_URL))
                        .body(new JsonBody(Map.of("width", 75, "height", 25)))
                        .setHeader("sec-ch-ua", "\"Not?A_Brand\";v=\"8\", \"Chromium\";v=\"108\", \"Microsoft Edge\";v=\"108\"")
                        .setHeader("Accept", "application/json, text/plain, */*")
                        .setHeader("sec-ch-ua-mobile", "?0")
                        .setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36 Edg/108.0.1462.54")
                        .setHeader("sec-ch-ua-platform", "\"Windows\"")
                        .setHeader("Origin", "https://season.sanook.com")
                        .setHeader("Sec-Fetch-Site", "same-site")
                        .setHeader("Sec-Fetch-Mode", "cors")
                        .setHeader("Sec-Fetch-Dest", " empty")
                        .setHeader("Referer", "https://season.sanook.com/")
                        .setHeader("Accept-Encoding", "gzip, deflate, br")
                        .setHeader("Accept-Language", "zh-CN,zh;q=0.9")
        );
        return ObjectUtils.jsonMapper().readValue(res.body().toBytes(), Captcha.class);
    }

    /**
     * 从验证码中读取字符
     *
     * @param image a
     * @return a
     */
    private String readFromImage(BufferedImage image) throws TesseractException {
        var s = TESSERACT.doOCR(image);
        //移除多余的 空白与换行
        return s.replaceAll("[\n ]", "");
    }

    /**
     * 初始化日志配置
     */
    public static void initLogger() {
//        ScxLoggerFactory.setLevel(Vote.class, ScxLoggingLevel.DEBUG);
        ScxLoggerFactory.setLevel(Vote.class, ScxLoggingLevel.ERROR);
        ScxLoggerFactory.setDefaultType(ScxLoggingType.CONSOLE);
    }

}
