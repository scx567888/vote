package cool.scx.vote;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cool.scx.util.Base64Utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * 验证码 类
 */
final class Captcha {

    public H h;

    public String r;

    /**
     * 缓存
     */
    @JsonIgnore
    public BufferedImage imageCache;

    private BufferedImage getImage0() throws IOException {
        var data = h.d().substring("data:image/png;base64,".length());
        var bytes = Base64Utils.decode(data);
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    public BufferedImage getImage() throws IOException {
        if (imageCache == null) {
            imageCache = getImage0();
        }
        return imageCache;
    }

}

