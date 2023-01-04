package cool.scx.vote;

import cool.scx.util.ansi.Ansi;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class VoteTest {

    static final AtomicInteger a = new AtomicInteger();
    static final AtomicInteger b = new AtomicInteger();
    static final AtomicInteger c = new AtomicInteger();

    public static void main(String[] args) {
        var threadList = IntStream.range(0, 10).mapToObj(c -> new Vote(true)).map(v -> new Thread(() -> {
            try {
                test1(v);
            } catch (Exception e) {
                e.printStackTrace();
            }
        })).toList();
        threadList.forEach(Thread::start);
    }

    public static void test1(Vote vote) throws Exception {
        //投 10 次
        while (true) {
            //选手编号
            var success = vote.vote("2");
            if (success) {
                a.set(a.get() + 1);
            } else {
                b.set(b.get() + 1);
            }
            c.set(c.get() + 1);
            if (c.get() % 20 == 0) {
                Ansi.out()
                        .brightMagenta("共投票 : " + c + " 次 !!!").ln()
                        .brightGreen("成功 : " + a + " 次 !!").ln()
                        .brightRed("失败 : " + b + " 次 !!!").println();
            }
        }

    }

}
