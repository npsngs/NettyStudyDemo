package com.grumpycat.nettystudydemo;

import java.net.InetSocketAddress;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;

/**
 * Created by cc.he on 2018/11/21
 */
public class HttpUtil {
    public static boolean isKeepAlive(HttpRequest request){
        String value = request.headers().get("Connection ");
        return "keep-alive".equals(value);
    }

    public static String getRequestString(ChannelHandlerContext ctx, HttpRequest request){
        String req = request.uri();

        Channel channel = ctx.channel();
        if(channel instanceof NioSocketChannel){
            NioSocketChannel nsc = (NioSocketChannel) channel;
            req = print(nsc.remoteAddress()) + req;
        }

        return req;
    }

    public static String print(InetSocketAddress address){
        return StrUtil.ip2Str(address.getAddress().getAddress())+":"+address.getPort();
    }
}
