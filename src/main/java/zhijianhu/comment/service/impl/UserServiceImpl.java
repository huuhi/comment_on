package zhijianhu.comment.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import zhijianhu.comment.domain.User;
import zhijianhu.comment.dto.LoginFormDTO;
import zhijianhu.comment.dto.Result;
import zhijianhu.comment.dto.UserDTO;
import zhijianhu.comment.mapper.TbUserMapper;
import zhijianhu.comment.service.IUserService;
import zhijianhu.comment.util.RegexUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static zhijianhu.comment.util.RedisConstants.*;
import static zhijianhu.comment.util.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<TbUserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public UserServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate=stringRedisTemplate;
    }

    @Override
    public Result sendCode(String phone, HttpSession session) {
//        校验手机号是否合法
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号码格式错误");
        }
//        没问题生成验证码
        String code = RandomUtil.randomNumbers(6);
//        存储到redis,有效期为2分钟
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);

//        session.setAttribute("code", code);
//        TODO 调用云服务发送验证码
        log.debug("发送验证码成功，验证码：{}", code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
//     先再次校验手机号，因为用户可能修改了手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号码格式错误");
        }
//      然后校验验证码是否正确
        String code = loginForm.getCode();
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        if(!code.equals(cacheCode)){
            return Result.fail("验证码错误");
        }
//       判断用户是否存在
        User user = query().eq("phone", phone).one();
        if(user==null){
//            用户不存在
            user = createUser(phone);
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
//      随机的key
        String key = UUID.randomUUID().toString(true);
//        自定义值的类型为String
        Map<String, Object> map = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true).setFieldValueEditor((f,v)->v.toString()));
        String token= LOGIN_USER_KEY+key;
        stringRedisTemplate.opsForHash().putAll(token,map);
//        30 分钟过期
        stringRedisTemplate.expire(token,LOGIN_USER_TTL,TimeUnit.MINUTES);
        return Result.ok(key);
    }

    private User createUser(String phone) {
        String nickname = USER_NICK_NAME_PREFIX + RandomUtil.randomString(5);
        User user = User.builder()
                .phone(phone)
                .nickName(nickname)
                .build();
        save(user);
        return user;
    }
}
