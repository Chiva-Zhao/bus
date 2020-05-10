/*********************************************************************************
 *                                                                               *
 * The MIT License                                                               *
 *                                                                               *
 * Copyright (c) 2021 aoju.org and other contributors.                      *
 *                                                                               *
 * Permission is hereby granted, free of charge, to any person obtaining a copy  *
 * of this software and associated documentation files (the "Software"), to deal *
 * in the Software without restriction, including without limitation the rights  *
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell     *
 * copies of the Software, and to permit persons to whom the Software is         *
 * furnished to do so, subject to the following conditions:                      *
 *                                                                               *
 * The above copyright notice and this permission notice shall be included in    *
 * all copies or substantial portions of the Software.                           *
 *                                                                               *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR    *
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,      *
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE   *
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER        *
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, *
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN     *
 * THE SOFTWARE.                                                                 *
 ********************************************************************************/
package org.aoju.bus.oauth.provider;

import com.alibaba.fastjson.JSONObject;
import org.aoju.bus.cache.metric.ExtendCache;
import org.aoju.bus.core.lang.Normal;
import org.aoju.bus.core.lang.exception.AuthorizedException;
import org.aoju.bus.http.Httpx;
import org.aoju.bus.oauth.Builder;
import org.aoju.bus.oauth.Context;
import org.aoju.bus.oauth.Registry;
import org.aoju.bus.oauth.magic.AccToken;
import org.aoju.bus.oauth.magic.Callback;
import org.aoju.bus.oauth.magic.Message;
import org.aoju.bus.oauth.magic.Property;

/**
 * 抖音登录
 *
 * @author Kimi Liu
 * @version 5.9.0
 * @since JDK 1.8+
 */
public class DouyinProvider extends DefaultProvider {

    public DouyinProvider(Context context) {
        super(context, Registry.DOUYIN);
    }

    public DouyinProvider(Context context, ExtendCache extendCache) {
        super(context, Registry.DOUYIN, extendCache);
    }

    @Override
    protected AccToken getAccessToken(Callback Callback) {
        return this.getToken(accessTokenUrl(Callback.getCode()));
    }

    @Override
    protected Property getUserInfo(AccToken token) {
        JSONObject object = JSONObject.parseObject(doGetUserInfo(token));
        this.checkResponse(object);
        JSONObject dataObj = object.getJSONObject("data");
        return Property.builder()
                .uuid(dataObj.getString("union_id"))
                .username(dataObj.getString("nickname"))
                .nickname(dataObj.getString("nickname"))
                .avatar(dataObj.getString("avatar"))
                .remark(dataObj.getString("description"))
                .gender(Normal.Gender.getGender(dataObj.getString("gender")))
                .location(String.format("%s %s %s", dataObj.getString("country"), dataObj.getString("province"), dataObj.getString("city")))
                .token(token)
                .source(source.toString())
                .build();
    }

    @Override
    public Message refresh(AccToken oldToken) {
        return Message.builder()
                .errcode(Builder.ErrorCode.SUCCESS.getCode())
                .data(getToken(refreshTokenUrl(oldToken.getRefreshToken())))
                .build();
    }

    /**
     * 检查响应内容是否正确
     *
     * @param object 请求响应内容
     */
    private void checkResponse(JSONObject object) {
        String message = object.getString("message");
        JSONObject data = object.getJSONObject("data");
        int errorCode = data.getIntValue("error_code");
        if ("error".equals(message) || errorCode != 0) {
            throw new AuthorizedException(Normal.EMPTY + errorCode, data.getString("description"));
        }
    }

    /**
     * 获取token,适用于获取access_token和刷新token
     *
     * @param accessTokenUrl 实际请求token的地址
     * @return token对象
     */
    private AccToken getToken(String accessTokenUrl) {
        JSONObject object = JSONObject.parseObject(Httpx.post(accessTokenUrl));
        this.checkResponse(object);
        JSONObject dataObj = object.getJSONObject("data");
        return AccToken.builder()
                .accessToken(dataObj.getString("access_token"))
                .openId(dataObj.getString("open_id"))
                .expireIn(dataObj.getIntValue("expires_in"))
                .refreshToken(dataObj.getString("refresh_token"))
                .scope(dataObj.getString("scope"))
                .build();
    }

    /**
     * 返回带{@code state}参数的授权url,授权回调时会带上这个{@code state}
     *
     * @param state state 验证授权流程的参数,可以防止csrf
     * @return 返回授权地址
     */
    @Override
    public String authorize(String state) {
        return Builder.fromUrl(source.authorize())
                .queryParam("response_type", "code")
                .queryParam("client_key", context.getAppKey())
                .queryParam("redirect_uri", context.getRedirectUri())
                .queryParam("scope", "user_info")
                .queryParam("state", getRealState(state))
                .build();
    }

    /**
     * 返回获取accessToken的url
     *
     * @param code oauth的授权码
     * @return 返回获取accessToken的url
     */
    @Override
    protected String accessTokenUrl(String code) {
        return Builder.fromUrl(source.accessToken())
                .queryParam("code", code)
                .queryParam("client_key", context.getAppKey())
                .queryParam("client_secret", context.getAppSecret())
                .queryParam("grant_type", "authorization_code")
                .build();
    }

    /**
     * 返回获取userInfo的url
     *
     * @param token oauth返回的token
     * @return 返回获取userInfo的url
     */
    @Override
    protected String userInfoUrl(AccToken token) {
        return Builder.fromUrl(source.userInfo())
                .queryParam("access_token", token.getAccessToken())
                .queryParam("open_id", token.getOpenId())
                .build();
    }

    /**
     * 返回获取accessToken的url
     *
     * @param refreshToken oauth返回的refreshtoken
     * @return 返回获取accessToken的url
     */
    @Override
    protected String refreshTokenUrl(String refreshToken) {
        return Builder.fromUrl(source.refresh())
                .queryParam("client_key", context.getAppKey())
                .queryParam("refresh_token", refreshToken)
                .queryParam("grant_type", "refresh_token")
                .build();
    }

}
