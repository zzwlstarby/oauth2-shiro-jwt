package com.askerlve.ums.controller.oauth2;

import com.askerlve.ums.controller.base.BaseController;
import com.askerlve.ums.controller.oauth2.dto.oauth.Oauth2JudgePermissionParams;
import com.askerlve.ums.controller.oauth2.dto.oauth.Oauth2ListPermissionParams;
import com.askerlve.ums.controller.oauth2.dto.oauth.Oauth2RefreshTokenParams;
import com.askerlve.ums.exception.AuthException;
import com.askerlve.ums.model.Application;
import com.askerlve.ums.model.Resource;
import com.askerlve.ums.model.User;
import com.askerlve.ums.service.IApplicationService;
import com.askerlve.ums.service.IResourceService;
import com.askerlve.ums.service.IUserService;
import com.askerlve.ums.utils.content.Config;
import com.askerlve.ums.web.oauth2.service.TokenService;
import com.askerlve.ums.web.result.ResultInfo;
import com.askerlve.ums.web.result.ResultUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@Api(tags = "基于Oauth2鉴权服务", description = "API接口")
@RequestMapping("/oauth/auth")
public class Oauth2AuthController extends BaseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(Oauth2AuthController.class);

    @Autowired
    private IUserService iUserService;

    @Autowired
    private IResourceService iResourceService;

    @Autowired
    private IApplicationService iApplicationService;

    @Autowired
    private TokenService tokenService;

    @PostMapping(value = "/judge/permission")
    @ApiOperation(value = "判断当前用户是否有权限")
    @ApiImplicitParams({
            @ApiImplicitParam(name = Config.JWT_CUSTOMER_TOKEN_NAME, value = "认证token", required = true, dataType = "string", paramType = "header")
    })
    public ResultInfo judgePermission(@RequestBody Oauth2JudgePermissionParams oauth2JudgePermissionParams, Authentication authentication) {

        ResultInfo resultInfo = ResultUtil.createSuccess(200, null);

        if (Objects.nonNull(oauth2JudgePermissionParams) && StringUtils.isNotBlank(oauth2JudgePermissionParams.getUrlAddress()) && StringUtils.isNotBlank(oauth2JudgePermissionParams.getApplicationKey())) {

            String userName = authentication.getName();
            User user = this.iUserService.getUserByUserName(userName);
            if (user == null) {
                throw new AuthException("User doesn't exist!", 101);
            }

            String applicationKey = oauth2JudgePermissionParams.getApplicationKey().trim();
            Application application = this.iApplicationService.getApplicationByAppkey(applicationKey);
            if (Objects.isNull(application)) {
                throw new AuthException("Application " + applicationKey + " doesn't exist!", 120);
            }

            String urlAddress = oauth2JudgePermissionParams.getUrlAddress().trim();
            List<Resource> resourceList = iResourceService.getResourcesByUserAndAppKey(user, applicationKey);
            boolean isAllowed = false;

            if (Objects.nonNull(resourceList) && resourceList.size() > 0) {
                for (Resource resource : resourceList) {
                    if (urlAddress.equals(resource.getUrl())) {
                        isAllowed = true;
                        break;
                    }
                }
            }

            Map<String, Boolean> stringBooleanMap = new HashMap<>();
            stringBooleanMap.put("isAllowed", isAllowed);
            resultInfo.setResult(stringBooleanMap);

        } else {
            throw new AuthException("Params invalid!", 504);
        }

        return resultInfo;
    }


    @PostMapping(value = "/list/permission")
    @ApiOperation(value = "获取权限列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = Config.JWT_CUSTOMER_TOKEN_NAME, value = "认证token", required = true, dataType = "string", paramType = "header")
    })
    public ResultInfo listPermission(Authentication authentication, @RequestBody Oauth2ListPermissionParams oauth2ListPermissionParams) {

        ResultInfo resultInfo = ResultUtil.createSuccess(200, null);

        if (Objects.nonNull(oauth2ListPermissionParams) && StringUtils.isNotBlank(oauth2ListPermissionParams.getApplicationKey())) {

            String userName = authentication.getName();
            User user = this.iUserService.getUserByUserName(userName);
            if (user == null) {
                throw new AuthException("User doesn't exist!", 101);
            }

            String applicationKey = oauth2ListPermissionParams.getApplicationKey();
            Application application = this.iApplicationService.getApplicationByAppkey(applicationKey);
            if (Objects.isNull(application)) {
                throw new AuthException("Application " + applicationKey + " doesn't exist!", 120);
            }

            List<Resource> resourceList = iResourceService.getResourcesByUserAndAppKey(user, applicationKey);
            resultInfo.setResult(resourceList);

        } else {
            throw new AuthException("Params invalid!", 504);
        }

        return resultInfo;
    }

    @PostMapping(value = "/refresh")
    @ApiOperation(value = "刷新token")
    @ApiImplicitParams({
            @ApiImplicitParam(name = Config.JWT_CUSTOMER_TOKEN_NAME, value = "basic Authorization token，Basic + \" \" + (客户端用户名 + \":\" + 客户端密码)进行base64编码", required = true, dataType = "string", paramType = "header")
    })
    public ResultInfo refreshToken(@RequestHeader("Authorization") String Authorization, @RequestBody Oauth2RefreshTokenParams oauth2RefreshTokenParams) throws IOException {

        if (Objects.isNull(oauth2RefreshTokenParams) || StringUtils.isBlank(oauth2RefreshTokenParams.getRefreshToken()) || Authorization == null || !Authorization.startsWith("Basic ")) {
            throw new AuthException("Params invalid!", 504);
        }

        OAuth2AccessToken token = this.tokenService.refreshAccessToken(Authorization, oauth2RefreshTokenParams.getRefreshToken());

        ResultInfo resultInfo = ResultUtil.createSuccess(200, token);

        return resultInfo;
    }

}
