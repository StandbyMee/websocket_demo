package com.practice.websocket_demo.websocket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ClassName: WebSocket <br/>
 * Description: WebSocket配置类，包含单独发送消息、群发消息、监听上下线等等方法<br/>
 * date: 2021/6/23 9:10<br/>
 *
 * @author Administrator<br />
 * @since JDK 1.8
 */
public class WebSocket {
    //logger对象，用来打印日志信息
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    //在线人数
    public static int onlineNumber = 0;

    //以用户名为key, WebSocket对象为value的map
    private static Map<String, WebSocket> clients = new ConcurrentHashMap<String, WebSocket>();

    //WebSocket会话
    private Session session;

    //用户名
    private String userId;


    //连接建立
    //@PathParam 从URL中获取参数的值
    @OnOpen
    public void onOpen(@PathParam("userId") String userId, Session session){
        onlineNumber++;
        logger.info("现在来连接的客户id：" + session.getId() + " 用户名：" + userId);
        this.userId = userId;
        this.session = session;

        try {
            //messageType 1代表上线，2代表下线，3代表在线名单，4代表普通消息
            //先给所有人发送通知，说我上线了
            Map<String, Object> map1 = Maps.newHashMap();
            map1.put("messageType", 1);
            map1.put("userId", userId);
            sendMessageAll(JSON.toJSONString(map1), userId);

            //把自己的信息加入到map
            clients.put(userId, this);
            //给自己发一条消息，告诉自己有哪些人在线
            logger.info("有连接建立！ 当前在线人数：" + clients.size());
            Map<String, Object> map2 = Maps.newHashMap();
            map2.put("messageType", 3);
            Set<String> set = clients.keySet();
            map2.put("onlineUsers", set);
            sendMessageTo(JSON.toJSONString(map2), userId);
        } catch (IOException e) {
            logger.info(userId + "上线通知左右人时发生了错误");
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        logger.info("服务器端发生了错误" + error.getMessage());
    }

    //连接关闭
    @OnClose
    public void onClose() {
        onlineNumber--;
        clients.remove(userId);
        try {
            //messageType 1代表上线，2代表下线，3代表在线名单，4代表普通消息
            Map<String, Object> map1 = Maps.newHashMap();
            map1.put("messageType", 2);
            map1.put("onlineUsers", clients.keySet());
            map1.put("userId", userId);
            sendMessageAll(JSON.toJSONString(map1), userId);
        } catch (IOException e) {
            logger.info(userId + "下线通知所有人发生了错误");
        }
        logger.info("有连接关闭！当前在线人数：" + clients.size());
    }

    //收到客户端信息
    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            logger.info("来自客户端消息：" + message + "客户端的id是"
            + session.getId());

            System.out.println("-----------" + message);
            //将收到的整个message中的各个信息提取
            JSONObject jsonObject = JSON.parseObject(message);
            String textMessage = jsonObject.getString("message");
            String fromuserId = jsonObject.getString("userId");
            String touserId = jsonObject.getString("to");

            //messageType 1代表上线，2代表下线，3代表在线名单，4代表普通消息
            Map<String, Object> map1 = Maps.newHashMap();
            map1.put("messageType", 4);
            map1.put("textMessage", textMessage);
            map1.put("fromuserId", fromuserId);
            //判断是发给所有人还是某个用户，调用不同的方法
            if(touserId.equals("All")) {
                map1.put("touserId", "所有人");
                sendMessageAll(JSON.toJSONString(map1), fromuserId);
            } else {
                map1.put("touserId", touserId);
                System.out.println("开始推送消息给" + touserId);
                sendMessageTo(JSON.toJSONString(map1), touserId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("发生了错误");
        }
    }

    //发消息给指定人
    public void sendMessageTo(String message, String toUserId) throws IOException {
        for (WebSocket item : clients.values()) {
            if (item.userId.equals(toUserId)) {
                item.session.getAsyncRemote().sendText(message);
                break;
            }
        }

    }
    //发消息给所有人
    public void sendMessageAll(String message, String fromUserId) throws IOException {
        for (WebSocket item : clients.values()) {
            item.session.getAsyncRemote().sendText(message);
        }
    }

    public static synchronized int getOnlineNumber() {
        return onlineNumber;
    }
}
