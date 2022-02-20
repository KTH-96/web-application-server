package util;

import java.io.BufferedReader;
import java.io.IOException;

public class IOUtils {
    /**
     *  BufferedReader는 Request Body를 시작하는 시점이어야
     *  contentLength는 Request Header의 Content-Length 값이다.
     */
    public static String readData(BufferedReader br, int contentLength) throws IOException {
        char[] body = new char[contentLength];
        br.read(body, 0, contentLength);
        return String.copyValueOf(body);
    }
}
