package com.harry.renthouse.service.auth.impl;

import com.harry.renthouse.base.ApiResponseEnum;
import com.harry.renthouse.util.AuthenticatedUserUtil;
import com.harry.renthouse.base.UserRoleEnum;
import com.harry.renthouse.entity.Role;
import com.harry.renthouse.entity.User;
import com.harry.renthouse.exception.BusinessException;
import com.harry.renthouse.repository.RoleRepository;
import com.harry.renthouse.repository.UserRepository;
import com.harry.renthouse.service.auth.UserService;
import com.harry.renthouse.util.RedisUtil;
import com.harry.renthouse.web.dto.UserDTO;
import com.harry.renthouse.web.form.UserBasicInfoForm;
import com.harry.renthouse.web.form.UserPhoneRegisterForm;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Harry Xu
 * @date 2020/5/18 18:40
 */
@Service
public class UserServiceImpl implements UserService {

    private static final String DEFAULT_NICk_NAME_PREFIX = "zfyh";

    // 重置密码令牌前缀
    private static final String RESET_PASS_WORD_TOKEN_PREFIX = "RESET:PASSWORD:TOKEN:";

    private static final int RESET_PASS_WORD_TOKEN_EXPIRE = 60 * 15;

    @Resource
    private UserRepository userRepository;

    @Resource
    private ModelMapper modelMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private RoleRepository roleRepository;

    @Resource
    private RedisUtil redisUtil;

    @Override
    public Optional<UserDTO> findUserById(Long id) {
        return userRepository.findById(id).map(item -> modelMapper.map(item, UserDTO.class));
    }

    @Override
    public Optional<UserDTO> findByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber).map(item -> modelMapper.map(item, UserDTO.class));
    }

    @Override
    @Transactional
    public void updateAvatar(String avatar) {
        Long userId = AuthenticatedUserUtil.getUserId();
        userRepository.updateAvatar(userId, avatar);
    }

    @Override
    @Transactional
    public UserDTO updateUserInfo(Long userId, UserBasicInfoForm userBasicInfoForm) {
        final User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ApiResponseEnum.USER_NOT_FOUND));
        user.setNickName(userBasicInfoForm.getNickName());
        user.setAvatar(userBasicInfoForm.getAvatar());
        user.setIntroduction(userBasicInfoForm.getIntroduction());
        userRepository.save(user);
        return modelMapper.map(user, UserDTO.class);
    }

    @Override
    @Transactional
    public UserDTO registerUserByPhone(UserPhoneRegisterForm phoneRegisterForm, List<UserRoleEnum> roleList) {
        // 判断手机号是否被注册
        userRepository.findByPhoneNumber(phoneRegisterForm.getPhoneNumber()).ifPresent(user -> {
            throw new BusinessException(ApiResponseEnum.PHONE_ALREADY_REGISTERED);
        });
        // 执行注册用户逻辑
        User user = new User();
        user.setPhoneNumber(phoneRegisterForm.getPhoneNumber());
        user.setName(DEFAULT_NICk_NAME_PREFIX + phoneRegisterForm.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(phoneRegisterForm.getPassword()));
        user.setNickName(DEFAULT_NICk_NAME_PREFIX + phoneRegisterForm.getPhoneNumber());
        User result = userRepository.save(user);
        // 获取用户id设置角色
        Long userId = result.getId();
        List<Role> roles = roleList.stream().map(item -> {
            Role role = new Role();
            role.setName(item.getValue());
            role.setUserId(userId);
            return role;
        }).collect(Collectors.toList());
        roles = roleRepository.saveAll(roles);
        Set<GrantedAuthority> authorities = new HashSet<>();
        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName())));
        user.setAuthorities(authorities);
        return modelMapper.map(user, UserDTO.class);
    }

    @Override
    public Optional<UserDTO> findByNickName(String nickName) {
        return  userRepository.findByNickName(nickName).map(user -> modelMapper.map(user, UserDTO.class));
    }

    @Transactional
    public UserDTO createByPhone(String phone){
        // 判断手机号是否被注册
        userRepository.findByPhoneNumber(phone).ifPresent(user -> {
            throw new BusinessException(ApiResponseEnum.PHONE_ALREADY_REGISTERED);
        });
        // 执行注册用户逻辑
        User user = new User();
        user.setPhoneNumber(phone);
        user.setName(DEFAULT_NICk_NAME_PREFIX + phone);
        user.setNickName(DEFAULT_NICk_NAME_PREFIX + phone);
        user = userRepository.save(user);
        // 获取用户id设置角色
        Long userId = user.getId();
        Role role = new Role();
        role.setUserId(userId);
        role.setName(UserRoleEnum.ADMIN.getValue());
        roleRepository.save(role);
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
        user.setAuthorities(authorities);
        return modelMapper.map(user, UserDTO.class);
    }

    @Override
    @Transactional
    public void updatePassword(String oldPassword, String newPassword) {
        User user = userRepository.findById(AuthenticatedUserUtil.getUserId()).orElseThrow(() -> new BusinessException(ApiResponseEnum.USER_NOT_FOUND));
        if(StringUtils.isNotBlank(user.getPassword())){
            if(StringUtils.isBlank(oldPassword)){
                throw new BusinessException(ApiResponseEnum.ORIGINAL_PASSWORD_EMPTY_ERROR);
            }
            if(!passwordEncoder.matches(oldPassword, user.getPassword())){
                throw new BusinessException(ApiResponseEnum.ORIGINAL_PASSWORD_ERROR);
            }
        }
        userRepository.updatePassword(user.getId(), passwordEncoder.encode(newPassword));
    }

    @Override
    public String generateResetPasswordToken(String phone) {
        userRepository.findByPhoneNumber(phone).orElseThrow(() -> new BusinessException(ApiResponseEnum.USER_NOT_FOUND));
        String token = UUID.randomUUID().toString();
        redisUtil.set(RESET_PASS_WORD_TOKEN_PREFIX + token, phone, RESET_PASS_WORD_TOKEN_EXPIRE);
        return token;
    }

    @Override
    @Transactional
    public void resetPasswordByToken(String password, String token) {
        Object phoneObj = redisUtil.get(RESET_PASS_WORD_TOKEN_PREFIX + token);
        redisUtil.del(RESET_PASS_WORD_TOKEN_PREFIX + token);
        if(phoneObj == null){
            throw new BusinessException(ApiResponseEnum.RESET_PASSWORD_INVALID_TOKEN);
        }
        String phone = (String)phoneObj;
        User user = userRepository.findByPhoneNumber(phone).orElseThrow(() -> new BusinessException(ApiResponseEnum.USER_NOT_FOUND));
        userRepository.updatePassword(user.getId(), passwordEncoder.encode(password));

    }
}
