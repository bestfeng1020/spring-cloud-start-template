package org.bestfeng.template.authorization.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.codec.binary.Base64;
import org.hswebframework.web.ThreadLocalUtils;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.AuthenticationHolder;
import org.hswebframework.web.authorization.AuthenticationSupplier;
import org.hswebframework.web.authorization.basic.web.ParsedToken;
import org.hswebframework.web.authorization.basic.web.UserTokenParser;
import org.hswebframework.web.authorization.builder.AuthenticationBuilderFactory;
import org.hswebframework.web.authorization.simple.builder.SimpleAuthenticationBuilderFactory;
import org.hswebframework.web.authorization.simple.builder.SimpleDataAccessConfigBuilderFactory;
import org.hswebframework.web.organizational.authorization.PersonnelAuthentication;
import org.hswebframework.web.organizational.authorization.PersonnelAuthenticationHolder;
import org.hswebframework.web.organizational.authorization.PersonnelAuthenticationSupplier;
import org.hswebframework.web.organizational.authorization.simple.SimplePersonnelAuthorizationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

/**
 * @author zhouhao
 * @since 3.0
 */
public class TemplateMicroServiceTokenParser implements UserTokenParser, AuthenticationSupplier, CustomerUserAuthenticationSupplier {


    @Autowired(required = false)
    AuthenticationBuilderFactory authenticationBuilderFactory
            = new SimpleAuthenticationBuilderFactory(new SimpleDataAccessConfigBuilderFactory());

    @PostConstruct
    public void init() {
        PersonnelAuthenticationHolder.addSupplier(new PersonnelAuthenticationSupplier() {
            @Override
            public PersonnelAuthentication getByPersonId(String personId) {
                throw new UnsupportedOperationException("不支持此操作");
            }

            @Override
            public PersonnelAuthentication getByUserId(String userId) {
                throw new UnsupportedOperationException("不支持此操作");
            }

            @Override
            public PersonnelAuthentication get() {
                return getPerson();
            }
        });
        AuthenticationHolder.addSupplier(this);
    }

    @Override
    public ParsedToken parseToken(HttpServletRequest request) {
        String base64Autz = request.getHeader("template-autz");
        if (StringUtils.isEmpty(base64Autz)) {
            return null;
        }
        String info = new String(Base64.decodeBase64(base64Autz));

        JSONObject jsonObject = JSON.parseObject(info);

        //基本权限
        Authentication authentication = authenticationBuilderFactory.create()
                .json(jsonObject.getString("user"))
                .build();

        ThreadLocalUtils.put("request-id", request.getHeader("request-id"));
        ThreadLocalUtils.put(Authentication.class.getName(), authentication);
        //组织架构的信息
        JSONObject object = jsonObject.getJSONObject("personnel");
        if (object != null) {
            ThreadLocalUtils.put(PersonnelAuthentication.class.getName(),
                    SimplePersonnelAuthorizationBuilder.fromMap(object));
        }
        //客户的信息
        JSONObject customer = jsonObject.getJSONObject("customer");
        if (customer != null) {
            ThreadLocalUtils.put(CustomerUserAuthentication.class.getName(),
                    customer.toJavaObject(SimpleCustomerUserAuthentication.class));
        }

        return new ParsedToken() {
            @Override
            public String getToken() {
                return base64Autz;
            }

            @Override
            public String getType() {
                return "template-autz";
            }
        };
    }

    @Override
    public Authentication get(String userId) {
        return null;
    }

    @Override
    public Authentication get() {
        return ThreadLocalUtils.get(Authentication.class.getName());
    }

    public PersonnelAuthentication getPerson() {
        return ThreadLocalUtils.get(PersonnelAuthentication.class.getName());
    }

    @Override
    public CustomerUserAuthentication getByCustomerUserId(String userId) {
        return null;
    }

    @Override
    public CustomerUserAuthentication current() {
        return ThreadLocalUtils.get(CustomerUserAuthentication.class.getName());
    }
}
