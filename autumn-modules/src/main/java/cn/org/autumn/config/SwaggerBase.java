package cn.org.autumn.config;

import springfox.documentation.builders.OAuthBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.service.*;
import springfox.documentation.spi.service.contexts.SecurityContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SwaggerBase {

    protected List<GrantType> grantTypes() {
        List<GrantType> grantTypes = new ArrayList<>();
        GrantType grantType = new ClientCredentialsGrant("/oauth2/token");
        grantTypes.add(grantType);
        return grantTypes;
    }

    /**
     * 功能描述：SecurityScheme配置全局参数
     * 1.SecurityScheme的ApiKey中增加一个名为“secrectKey”，type为“header”(请求头)的参数
     * 2.SecurityScheme的ApiKey中增加一个名为“accessToken”，type为“query”(请求参数)的参数
     */
    protected List<SecurityScheme> securitySchemes() {
        List<SecurityScheme> securitySchemes = new ArrayList<>();
        securitySchemes.add(securityScheme());
        return securitySchemes;
    }

    protected SecurityScheme securityScheme() {
        return new OAuthBuilder()
                .name("Authorization")
                .grantTypes(grantTypes())
                .scopes(Arrays.asList(scopes()))
                .build();
    }


    protected AuthorizationScope[] scopes() {
        return new AuthorizationScope[]{
                new AuthorizationScope("all", "All scope is trusted!")
        };
    }

    /**
     * 功能描述：securityContexts中通过正则表达式，设置需要使用参数的接口（或者说，是去除掉不需要使用参数的接口），
     * 如下所示，通过配置PathSelectors.regex("^(?!/user).*$")，即所有包含“/user”的接口不需要使用securitySchemes
     * 注意此处关于“/user”的填写，如果填写为“user”的话，则过滤匹配不会生效
     */
    protected List<SecurityContext> securityContexts() {
        List<SecurityContext> securityContexts = new ArrayList<>();
        securityContexts.add(
                SecurityContext.builder()
                        .securityReferences(defaultAuth())
                        .forPaths(PathSelectors.regex("^(?!/oauth2/token).*$"))
                        .build());
        return securityContexts;
    }

    /**
     * 功能描述：securityReferences中全局变量（2个）的作用范围，一般都是下述写法
     */
    protected List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        authorizationScopes[0] = authorizationScope;
        List<SecurityReference> securityReferences = new ArrayList<>();
        //特别提示：new SecurityReference() 构造参数中reference变量的值一定要和上述ApiKey中的key完全一致
        securityReferences.add(new SecurityReference("Authorization", authorizationScopes));
        return securityReferences;
    }
}
