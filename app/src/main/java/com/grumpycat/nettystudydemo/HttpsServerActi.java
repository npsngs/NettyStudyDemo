package com.grumpycat.nettystudydemo;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.InputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslHandler;

/**
 * Created by cc.he on 2018/11/21
 */
public class HttpsServerActi extends Activity {
    private TextView tv_address, tv_accept;
    private Button btn;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acti_https_server);
        tv_address = findViewById(R.id.tv_address);
        tv_accept = findViewById(R.id.tv_accept);
        btn = findViewById(R.id.btn);

    }

    public void onClick(View v){
        if(isRunning){
            stopHttpsServer();
        }else{
            btn.setEnabled(false);
            new Thread(){
                @Override
                public void run() {
                    try {
                        startHttpsServer();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }finally {
                        isRunning = false;
                    }
                }
            }.start();
        }
    }

    private volatile boolean isRunning = false;
    private NioEventLoopGroup boss;
    private NioEventLoopGroup worker;
    private void startHttpsServer() throws Exception{
        boss = new NioEventLoopGroup(1);
        worker = new NioEventLoopGroup();
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        try {
            serverBootstrap
                    .channel(NioServerSocketChannel.class)
                    .group(boss, worker)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {

                            SSLEngine sslEngine = SSLContextFactory
                                    .getSslContext(getApplicationContext())
                                    .createSSLEngine();
                            sslEngine.setUseClientMode(false);

                            ch.pipeline().addLast("tcp", new TcpHandler());
                            ch.pipeline().addLast("ssl", new SslHandler(sslEngine));
                            ch.pipeline().addLast(new HttpServerCodec());
                            ch.pipeline().addLast(new HttpsSeverHandler());


                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture future = serverBootstrap.bind(8888).sync();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    isRunning = true;
                    tv_address.setText(StrUtil.ip2Str(Utils.getLocalIP())+":8888");
                    btn.setEnabled(true);
                }
            });
            future.channel().closeFuture().sync();
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    btn.setEnabled(true);
                }
            });
        }
    }

    private void stopHttpsServer(){
        boss.shutdownGracefully();
        worker.shutdownGracefully();
        boss = null;
        worker = null;
    }

    private class TcpHandler extends ChannelInboundHandlerAdapter{
        private int num = 0;
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if(num == 0){
                ByteBuf byteBuf = (ByteBuf) msg;
                byte b = byteBuf.readByte();
                byteBuf.readerIndex(0);
                if (b != 22) {
                    ctx.pipeline().remove("ssl");
                }
                num++;
            }
            super.channelRead(ctx, msg);
        }
    }


    public class HttpsSeverHandler extends ChannelInboundHandlerAdapter  {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof HttpRequest) {
                DefaultHttpRequest request = (DefaultHttpRequest) msg;
                boolean isKeepAlive = HttpUtil.isKeepAlive(request);
                final String reqStr = HttpUtil.getRequestString(ctx, request);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv_accept.setText(reqStr);
                    }
                });

                FullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                httpResponse.content().writeBytes("<p>gujiguji</p>".getBytes());
                httpResponse.headers().set("Content-Type", "text/html;charset=UTF-8");
                httpResponse.headers().set("Content-Length", httpResponse.content().readableBytes());
                if (isKeepAlive) {
                    httpResponse.headers().set("Connection ", "keep-alive");
                    ctx.write(httpResponse);
                } else {
                    ctx.write(httpResponse).addListener(ChannelFutureListener.CLOSE);
                }
                ctx.flush();
            }
        }
    }


    public static class SSLContextFactory {

        public static SSLContext getSslContext(Context context) throws Exception {
            char[] passArray = "123456".toCharArray();
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            String type = KeyStore.getDefaultType();
            KeyStore ks = KeyStore.getInstance(type);
            //加载keytool 生成的文件
            InputStream is = context.getResources().getAssets().open("demo.ks");
            ks.load(is, passArray);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, passArray);
            sslContext.init(kmf.getKeyManagers(), null, null);
            is.close();
            return sslContext;
        }
    }
}
