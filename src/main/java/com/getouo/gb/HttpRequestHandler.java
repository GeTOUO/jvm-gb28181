package com.getouo.gb;

import com.getouo.gb.scl.util.LogSupport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import scala.Option;

import java.net.SocketAddress;

import static io.netty.handler.codec.http.HttpUtil.isKeepAlive;

public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    InternalLogger logger  = InternalLoggerFactory.getInstance(this.getClass());
//    private static WebSocketServerHandshakerFactory handShakerFactory = new WebSocketServerHandshakerFactory(
//            "ws://localhost:8081/websocket", null, false);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) throws Exception {
        logger.warn("----------- wswsws ---------------");
        logger.warn(fullHttpRequest.toString());
        logger.warn("----------- ^^^^^^ ---------------");
        HttpHeaders headers = fullHttpRequest.headers();
        if ("Upgrade".equalsIgnoreCase(headers.get(HttpHeaderNames.CONNECTION)) && "WebSocket".equalsIgnoreCase(headers.get(HttpHeaderNames.UPGRADE))) {
            handleHandshake(ctx, fullHttpRequest);
        } else {
            sendHttpResponse(ctx, fullHttpRequest, new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
        }


//        if (!fullHttpRequest.decoderResult().isSuccess() || (!"websocket".equals(fullHttpRequest.headers().get("Upgrade")))) {
//            sendHttpResponse(ctx, fullHttpRequest, new DefaultFullHttpResponse(
//                    HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
//        } else {
//            WebSocketServerHandshaker handShaker = handShakerFactory.newHandshaker(fullHttpRequest);
//            if (handShaker == null) {
//                WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
//            } else {
//                ctx.channel().attr(Attributes.HANDSHAKER).set(handShaker);
//                handShaker.handshake(ctx.channel(), fullHttpRequest);
//                ctx.pipeline().remove(this);
//            }
//        }

    }

    protected void handleHandshake(ChannelHandlerContext ctx, FullHttpRequest req) {

        WebSocketServerHandshakerFactory wsFactory =
                new WebSocketServerHandshakerFactory(getWebSocketURL(req), req.headers().get(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL), true);
        WebSocketServerHandshaker handShaker = wsFactory.newHandshaker(req);
        if (handShaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            ChannelFuture handshakeFuture = handShaker.handshake(ctx.channel(), req);
            handshakeFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (!future.isSuccess()) {
                        ctx.fireExceptionCaught(future.cause());
                    } else {
                        // Kept for compatibility
                        ctx.pipeline().fireUserEventTriggered(WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE);
                        ctx.pipeline().fireUserEventTriggered(new CustomHandshakeComplete(req.uri(), req.headers(), handShaker.selectedSubprotocol()));
//                        ctx.fireUserEventTriggered(WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE);
//                        ctx.fireUserEventTriggered(new CustomHandshakeComplete(req.uri(), req.headers(), handShaker.selectedSubprotocol()));
                    }
                }
            });
//            ctx.channel().attr(Attributes.HAND_SHAKER).set(handShaker);
            ctx.pipeline().remove(this);
        }
    }
    private String getWebSocketURL(FullHttpRequest req) {
        logger.debug("Req URI : " + req.uri());
        String url =  "ws://" + req.headers().get("Host") + req.uri() ;
        logger.debug("Constructed URL : " + url);
        return url;
    }

    /**
     * 本应用目前只接受websocket消息, 因此对于http只是用来升级的。其它的视为非法请求
     * 拒绝不合法的请求，并返回错误信息
     * @param ctx
     * @param req
     * @param res
     */
    private static void sendHttpResponse(ChannelHandlerContext ctx,
                                         FullHttpRequest req, DefaultFullHttpResponse res) {
        // 返回应答给客户端
        if (res.status().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
        }
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        // 如果是非Keep-Alive，关闭连接
        if (!isKeepAlive(req) || res.status().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }


    public static final class CustomHandshakeComplete {
        private final String requestUri;
        private final HttpHeaders requestHeaders;
        private final String selectedSubProtocol;

        CustomHandshakeComplete(String requestUri, HttpHeaders requestHeaders, String selectedSubProtocol) {
            this.requestUri = requestUri;
            this.requestHeaders = requestHeaders;
            this.selectedSubProtocol = selectedSubProtocol;
        }

        public String requestUri() {
            return requestUri;
        }

        public HttpHeaders requestHeaders() {
            return requestHeaders;
        }

        public String selectedSubProtocol() {
            return selectedSubProtocol;
        }
    }

    public static final ChannelGroup actors  = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof HttpRequestHandler.CustomHandshakeComplete) {
            HttpRequestHandler.CustomHandshakeComplete handshakeComplete = (HttpRequestHandler.CustomHandshakeComplete) evt;

            Channel channel = ctx.channel();
            actors.add(channel);

            channel.closeFuture().addListener(future -> {
                SocketAddress remote = channel.remoteAddress();
                // 做一些清理工作
                logger.warn("连接: {} 关闭了---ctx.channel.isActive:{}", remote, channel.isActive());
            });
            logger.warn("握手成功： {} -- {}", handshakeComplete.requestUri(), handshakeComplete.selectedSubProtocol());
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
