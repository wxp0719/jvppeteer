package com.ruiyun.jvppeteer.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.ruiyun.jvppeteer.Constant;
import com.ruiyun.jvppeteer.events.BrowserListenerWrapper;
import com.ruiyun.jvppeteer.events.EventEmitter;
import com.ruiyun.jvppeteer.events.browser.impl.DefaultBrowserListener;
import com.ruiyun.jvppeteer.protocol.runtime.ExceptionDetails;
import com.ruiyun.jvppeteer.transport.websocket.CDPSession;
import sun.misc.BASE64Decoder;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * һЩ��������
 */
public class Helper implements Constant {
    public static String createProtocolError(JsonNode node) {
        JsonNode methodNode = node.get(RECV_MESSAGE_METHOD_PROPERTY);
        JsonNode errNode = node.get(RECV_MESSAGE_ERROR_PROPERTY);
        JsonNode errorMsg = errNode.get(RECV_MESSAGE_ERROR_MESSAGE_PROPERTY);
        String message = "Protocol error "+methodNode.asText()+": "+errorMsg;
        JsonNode dataNode = errNode.get(RECV_MESSAGE_ERROR_DATA_PROPERTY);
        if(dataNode != null) {
            message += " "+dataNode.asText();
        }
        return message;
    }

    public static final boolean isWin64(){
        String arch = System.getProperty("os.arch");
        if(arch.contains("64")){
            return true;
        }
        return false;
    }

    public static final String paltform(){
        return System.getProperty("os.name");
    }

    public static final void chmod(String path,String perms) throws IOException {
        if(StringUtil.isEmpty(path))
            throw new IllegalArgumentException("Path must not be empty");

        char[] chars = perms.toCharArray();
        if(chars.length != 3) throw new IllegalArgumentException("perms length must be 3");

        Path path1 = Paths.get(path);
        Set<PosixFilePermission> permissions = new HashSet<>();
        //own
        if('1' == chars[0]){
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
        }else if('2' == chars[0]){
            permissions.add(PosixFilePermission.OWNER_WRITE);
        }else if('3' == chars[0]){
            permissions.add(PosixFilePermission.OWNER_WRITE);
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
        }else if('4' == chars[0]){
            permissions.add(PosixFilePermission.OWNER_READ);
        }else if('5' == chars[0]){
            permissions.add(PosixFilePermission.OWNER_READ);
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
        }else if('6' == chars[0]){
            permissions.add(PosixFilePermission.OWNER_READ);
            permissions.add(PosixFilePermission.OWNER_WRITE);
        }else if('7' == chars[0]){
            permissions.add(PosixFilePermission.OWNER_READ);
            permissions.add(PosixFilePermission.OWNER_WRITE);
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
        }
        //group
        if('1' == chars[1]){
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
        }else if('2' == chars[1]){
            permissions.add(PosixFilePermission.OWNER_WRITE);
        }else if('3' == chars[1]){
            permissions.add(PosixFilePermission.OWNER_WRITE);
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
        }else if('4' == chars[1]){
            permissions.add(PosixFilePermission.OWNER_READ);
        }else if('5' == chars[1]){
            permissions.add(PosixFilePermission.OWNER_READ);
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
        }else if('6' == chars[1]){
            permissions.add(PosixFilePermission.OWNER_READ);
            permissions.add(PosixFilePermission.OWNER_WRITE);
        }else if('7' == chars[1]){
            permissions.add(PosixFilePermission.OWNER_READ);
            permissions.add(PosixFilePermission.OWNER_WRITE);
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
        }
        //other
        if('1' == chars[2]){
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
        }else if('2' == chars[2]){
            permissions.add(PosixFilePermission.OWNER_WRITE);
        }else if('3' == chars[2]){
            permissions.add(PosixFilePermission.OWNER_WRITE);
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
        }else if('4' == chars[2]){
            permissions.add(PosixFilePermission.OWNER_READ);
        }else if('5' == chars[2]){
            permissions.add(PosixFilePermission.OWNER_READ);
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
        }else if('6' == chars[2]){
            permissions.add(PosixFilePermission.OWNER_READ);
            permissions.add(PosixFilePermission.OWNER_WRITE);
        }else if('7' == chars[2]){
            permissions.add(PosixFilePermission.OWNER_READ);
            permissions.add(PosixFilePermission.OWNER_WRITE);
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
        }

        Files.setPosixFilePermissions(path1,permissions);
    }

    public static final String  join(String root,String...args){
       return  Paths.get(root,args).toString().toString();
    }

    /**
     * read stream from protocol : example -> tracing  file
     * @param client CDPSession
     * @param handler ���͸�websocket�Ĳ���
     * @param path �ļ���ŵ�·��
     */
    public static final void readProtocolStream(CDPSession client, JsonNode handler,String path) throws IOException {
        boolean eof = false;
        File file = null;
        BufferedOutputStream writer = null;
        if(StringUtil.isNotEmpty(path)){
            file = new File(path);
        }else{
            throw new IllegalArgumentException("Path can't be null");
        }
        Map<String,Object>  params = new HashMap<>();
        Iterator<String> fieldNames = handler.fieldNames();
        while (fieldNames.hasNext()){
            String name = fieldNames.next();
            params.put(name,handler.get(name).asText());
        }
        try {
            writer = new BufferedOutputStream(new FileOutputStream(file));
            BASE64Decoder decoder = new BASE64Decoder();
            while (!eof){
                JsonNode response = client.send("IO.read", params,true);
                JsonNode eofNode = response.get(RECV_MESSAGE_STREAM_EOF_PROPERTY);
                JsonNode base64EncodedNode = response.get(RECV_MESSAGE_BASE64ENCODED_PROPERTY);
                JsonNode dataNode = response.get(RECV_MESSAGE_STREAM_DATA_PROPERTY);
                if(dataNode != null){
                    byte[] bytes;
                    if(base64EncodedNode != null){
                        bytes = decoder.decodeBuffer(dataNode.toString());
                    }else {
                        bytes = dataNode.toString().getBytes();
                    }
                    writer.write(bytes);
                }
                eof = eofNode.asBoolean();
            }
            writer.flush();
            client.send("IO.close", params,false);
        } finally {
            StreamUtil.closeStream(writer);
        }
    }


    public static String getExceptionMessage(ExceptionDetails exceptionDetails) {
        return  null;
    }

    public static final Set<DefaultBrowserListener> getConcurrentSet() {
        return new CopyOnWriteArraySet<DefaultBrowserListener>();
    }

    public static final <T> BrowserListenerWrapper<T> addEventListener(EventEmitter emitter, String eventName, DefaultBrowserListener<T> handler){
        emitter.on(eventName,handler);
        return new BrowserListenerWrapper<T>(emitter,eventName,handler);
    }

    public static final void removeEventListeners(List<BrowserListenerWrapper> eventListeners) {
       if(ValidateUtil.isEmpty(eventListeners)){
           return;
        }

        for (int i = 0; i < eventListeners.size(); i++) {
            BrowserListenerWrapper wrapper = eventListeners.get(i);
            wrapper.getEmitter().removeListener(wrapper.getEventName(),wrapper.getHandler());
        }
    }
}