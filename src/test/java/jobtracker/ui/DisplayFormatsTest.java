package jobtracker.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DisplayFormatsTest {

    @Test
    void 改行をスペースに置き換えて1行にする() {
        assertEquals("一次面接 オンライン 二次は対面", DisplayFormats.singleLine("一次面接\nオンライン\r\n\r\n二次は対面"));
    }

    @Test
    void nullは空文字にする() {
        assertEquals("", DisplayFormats.singleLine(null));
    }
}
