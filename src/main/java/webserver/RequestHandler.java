package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import db.DataBase;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream();
             OutputStream out = connection.getOutputStream()) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line = br.readLine();
            String url = HttpRequestUtils.getUrl(line);

            Map<String, String> headers = new HashMap<>();
            while (!"".equals(line)) {
                line = br.readLine();
                String[] headerTokens = line.split(": ");
                if (headerTokens.length == 2) {
                    headers.put(headerTokens[0], headerTokens[1]);
                }
            }

            if (url.startsWith("/user/create")) {
                String requestBody = IOUtils.readData(br, Integer.parseInt(headers.get("Content-Length")));
                Map<String, String> stringData = HttpRequestUtils.parseQueryString(requestBody);
                User user = new User(stringData.get("userId"),stringData.get("password"),
                        stringData.get("name"), stringData.get("email"));
                DataBase.addUser(user);
                log.info("User = {}", user);
                DataOutputStream dos = new DataOutputStream(out);
                response302Header(dos);
            } else if (url.equals("/user/login")) {
                String requestBody = IOUtils.readData(br, Integer.parseInt(headers.get("Content-Length")));
                Map<String, String> stringData = HttpRequestUtils.parseQueryString(requestBody);
                log.info("userId = {}, password = {}", stringData.get("userId"), stringData.get("password"));

                User user = DataBase.findUserById(stringData.get("userId"));
                if (user == null) {
                    log.info("User Not Found");
                    DataOutputStream dos = new DataOutputStream(out);
                    response302Header(dos);
                } else if (user.getPassword().equals(stringData.get("password"))) {
                    log.info("Login success");
                    DataOutputStream dos = new DataOutputStream(out);
                    response302HeaderWithCookie(dos, "login = true");
                }else {
                    log.info("Password Mismatch");
                    DataOutputStream dos = new DataOutputStream(out);
                    response302Header(dos);
                }
            } else if (url.endsWith(".css")) {
                DataOutputStream dos = new DataOutputStream(out);
                byte[] body = Files.readAllBytes(Paths.get("./webapp" + url));
                response200HeaderWithCss(dos, body.length);
                responseBody(dos, body);
            } else {
                DataOutputStream dos = new DataOutputStream(out);
                byte[] body = Files.readAllBytes(Paths.get("./webapp" + url));
                response200Header(dos, body.length);
                responseBody(dos, body);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302HeaderWithCookie(DataOutputStream dos, String cookie) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: /index.html\r\n");
            dos.writeBytes("Set-Cookie: " + cookie + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302Header(DataOutputStream dos) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: /index.html\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response200HeaderWithCss(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/css;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
