package cn.code.chameleon.service.impl;

import cn.code.chameleon.common.PageData;
import cn.code.chameleon.enums.ResultCodeEnum;
import cn.code.chameleon.enums.UserStatusEnum;
import cn.code.chameleon.exception.ChameleonException;
import cn.code.chameleon.mapper.UserMapper;
import cn.code.chameleon.mapper.UserRelationRoleMapper;
import cn.code.chameleon.pojo.Function;
import cn.code.chameleon.pojo.Role;
import cn.code.chameleon.pojo.User;
import cn.code.chameleon.pojo.UserExample;
import cn.code.chameleon.pojo.UserRelationRole;
import cn.code.chameleon.pojo.UserRelationRoleExample;
import cn.code.chameleon.service.RedisClient;
import cn.code.chameleon.service.RoleService;
import cn.code.chameleon.service.UserService;
import cn.code.chameleon.utils.Constants;
import cn.code.chameleon.utils.ConvertUtil;
import cn.code.chameleon.utils.EncryptUtil;
import cn.code.chameleon.utils.JsonUtils;
import cn.code.chameleon.utils.RequestUtil;
import cn.code.chameleon.utils.UserContext;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author liumingyu
 * @create 2018-05-07 下午5:11
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRelationRoleMapper userRelationRoleMapper;

    @Autowired
    private RoleService roleService;

    @Autowired
    private RedisClient redisClient;

    /**
     * 检查账号是否可用 true:可用;false:不可用
     *
     * @param account
     * @return
     * @throws ChameleonException
     */
    @Override
    public boolean checkAccount(String account) throws ChameleonException {
        if (account == null || "".equals(account)) {
            throw new ChameleonException(ResultCodeEnum.USER_ACCOUNT_PATTERN_ERROR);
        }
        UserExample example = new UserExample();
        UserExample.Criteria criteria = example.createCriteria();
        criteria.andEmailEqualTo(account);
        criteria.andIsDeleteEqualTo(false);
        List<User> users = userMapper.selectByExample(example);
        if (users == null || users.isEmpty()) {
            return true;
        }
        return false;
    }

    /**
     * 验证账号密码是否匹配 true:匹配; false:不匹配
     *
     * @param account
     * @param password
     * @return
     * @throws ChameleonException
     */
    @Override
    public boolean checkAccountAndPassword(String account, String password) throws ChameleonException {
        if (account == null || "".equals(account)) {
            throw new ChameleonException(ResultCodeEnum.USER_ACCOUNT_PATTERN_ERROR);
        }
        if (password == null || "".equals(password)) {
            throw new ChameleonException(ResultCodeEnum.USER_PASSWORD_LEN_ERROR);
        }
        UserExample example = new UserExample();
        UserExample.Criteria criteria = example.createCriteria();
        criteria.andIsDeleteEqualTo(false);
        criteria.andEmailEqualTo(account);
        criteria.andPasswordEqualTo(EncryptUtil.md5Digest(password));
        List<User> users = userMapper.selectByExample(example);
        if (users == null || users.isEmpty()) {
            return false;
        }
        return true;
    }

    /**
     * 创建用户
     *
     * @param user
     * @throws ChameleonException
     */
    @Override
    public void saveUser(User user) throws ChameleonException {
        if (user == null) {
            throw new ChameleonException(ResultCodeEnum.USER_DATA_EMPTY);
        }
        if (!checkAccount(user.getEmail())) {
            throw new ChameleonException(ResultCodeEnum.USER_ACCOUNT_HAS_EXISTED);
        }
        if (user.getPassword() == null || "".equals(user.getPassword()) || user.getPassword().length() < 6 || user.getPassword().length() > 18) {
            throw new ChameleonException(ResultCodeEnum.USER_PASSWORD_LEN_ERROR);
        }
        String password = EncryptUtil.md5Digest(user.getPassword());
        if (password == null || "".equals(password)) {
            throw new ChameleonException(ResultCodeEnum.USER_PASSWORD_MD5_FAILED);
        }
        user.setPassword(password);
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        user.setIsDelete(false);
        user.setStatus(UserStatusEnum.OFFLINE.getCode());
        userMapper.insert(user);
    }

    /**
     * 更新用户
     *
     * @param user
     * @throws ChameleonException
     */
    @Override
    public void updateUser(User user) throws ChameleonException {
        if (user == null) {
            throw new ChameleonException(ResultCodeEnum.USER_DATA_EMPTY);
        }
        if (user.getPassword() != null && !"".equals(user.getPassword())) {
            user.setPassword(EncryptUtil.md5Digest(user.getPassword()));
        }
        user.setUpdateTime(new Date());
        userMapper.updateByPrimaryKeySelective(user);
        User me = RequestUtil.getCurrentUser();
        if (me.getId().equals(user.getId())) {
            UserContext.removeCurrentUser();
            user.setPassword(null);
            UserContext.setCurrentUser(ConvertUtil.converUser2DTO(user));
        }
    }

    /**
     * 更新当前用户密码
     *
     * @param user
     * @param oldPassword
     * @param newPassword
     * @throws ChameleonException
     */
    @Override
    public void updatePassword(User user, String oldPassword, String newPassword) throws ChameleonException {
        if (newPassword == null || "".equals(newPassword) || newPassword.length() < 6 || newPassword.length() > 18) {
            throw new ChameleonException(ResultCodeEnum.USER_PASSWORD_LEN_ERROR);
        }
        if (!checkAccountAndPassword(user.getEmail(), oldPassword)) {
            throw new ChameleonException(ResultCodeEnum.USER_ACCOUNT_PASSWORD_ERROR);
        }
        String password = EncryptUtil.md5Digest(newPassword);
        if (password == null || "".equals(password)) {
            throw new ChameleonException(ResultCodeEnum.USER_PASSWORD_MD5_FAILED);
        }
        user.setPassword(password);
        user.setUpdateTime(new Date());
        userMapper.updateByPrimaryKeySelective(user);
    }

    /**
     * 启用/禁用用户
     *
     * @param userId
     * @param operatorId
     * @throws ChameleonException
     */
    @Override
    public void updateUserEnableStatus(Long userId, Long operatorId) throws ChameleonException {
        User user = userMapper.selectByPrimaryKey(userId);
        if (user == null) {
            throw new ChameleonException(ResultCodeEnum.USER_NOT_EXIST);
        }
        user.setStatus(user.getStatus().equals(UserStatusEnum.DISABLED.getCode()) ? UserStatusEnum.OFFLINE.getCode() : UserStatusEnum.DISABLED.getCode());
        user.setUpdateTime(new Date());
        userMapper.updateByPrimaryKeySelective(user);
    }

    /**
     * 根据id删除用户
     *
     * @param id
     * @param operatorId
     * @throws ChameleonException
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = {Exception.class, ChameleonException.class})
    @Override
    public void deleteUserById(Long id, Long operatorId) throws ChameleonException {
        User user = new User();
        user.setId(id);
        user.setIsDelete(true);
        user.setUpdateTime(new Date());
        userMapper.updateByPrimaryKeySelective(user);

        this.deleteUserRelationRoleByUserId(id, operatorId);
    }

    /**
     * 根据ID查询用户信息
     *
     * @param id
     * @return
     * @throws ChameleonException
     */
    @Override
    public User queryUserById(Long id) throws ChameleonException {
        return userMapper.selectByPrimaryKey(id);
    }

    /**
     * 分页查询用户列表
     *
     * @param keyword
     * @param page
     * @param size
     * @param orderField
     * @param orderType
     * @return
     * @throws ChameleonException
     */
    @Override
    public PageData<User> pageUsers(String keyword, Integer page, Integer size, String orderField, String orderType) throws ChameleonException {
        if (page == null || page <= 0) {
            page = 1;
        }
        if (size == null || size <= 0) {
            size = 20;
        }
        if (orderField == null || "".equals(orderField)) {
            orderField = "create_time";
        }
        if (orderType == null || "".equals(orderType)) {
            orderType = "desc";
        }
        PageHelper.startPage(page, size);
        UserExample example = new UserExample();
        UserExample.Criteria emailCriteria = example.createCriteria();
        UserExample.Criteria nameCriteria = example.createCriteria();
        if (keyword != null && !"".equals(keyword)) {
            emailCriteria.andEmailLike("%" + keyword + "%");
            nameCriteria.andNameLike("%" + keyword + "%");
        }
        emailCriteria.andIsDeleteEqualTo(false);
        nameCriteria.andIsDeleteEqualTo(false);
        example.or(nameCriteria);
        example.setOrderByClause(orderField + " " + orderType);

        List<User> users = userMapper.selectByExample(example);
        PageInfo<User> pageInfo = new PageInfo<>(users);

        return new PageData<>(pageInfo.getTotal(), users);
    }

    /**
     * 注销
     *
     * @param request
     * @throws ChameleonException
     */
    @Override
    public void logout(HttpServletRequest request) throws ChameleonException {
        String token = RequestUtil.getToken(request);
        if (token == null || "".equals(token)) {
            return;
        }
        this.logout(token);
    }

    /**
     * 注销
     *
     * @param token
     * @throws ChameleonException
     */
    @Override
    public void logout(String token) throws ChameleonException {
        String json = redisClient.get(Constants.USER_TOKEN_KEY + ":" + token);
        if (StringUtils.isBlank(json)) {
            throw new ChameleonException(ResultCodeEnum.TOKEN_IS_OUT_TIME);
        }
        redisClient.delete(Constants.USER_TOKEN_KEY + ":" + token);
        User user = JsonUtils.jsonToPojo(json, User.class);
        user.setStatus(UserStatusEnum.OFFLINE.getCode());
        userMapper.updateByPrimaryKeySelective(user);

        UserContext.removeCurrentUser();
    }

    /**
     * 检查用户和角色是否已经存在绑定关系 true:存在; false:不存在
     *
     * @param userId
     * @param roleId
     * @return
     * @throws ChameleonException
     */
    public boolean checkUserRelationRole(Long userId, Long roleId) throws ChameleonException {
        UserRelationRoleExample example = new UserRelationRoleExample();
        UserRelationRoleExample.Criteria criteria = example.createCriteria();
        criteria.andUserIdEqualTo(userId);
        criteria.andRoleIdEqualTo(roleId);
        criteria.andIsDeleteEqualTo(false);
        List<UserRelationRole> userRelationRoles = userRelationRoleMapper.selectByExample(example);
        if (userRelationRoles == null || userRelationRoles.isEmpty()) {
            return false;
        }
        return true;
    }

    /**
     * 绑定用户和角色的关系
     *
     * @param userId
     * @param roleId
     * @param operatorId
     * @throws ChameleonException
     */
    @Override
    public void saveUserRelationRole(Long userId, Long roleId, Long operatorId) throws ChameleonException {
        if (checkUserRelationRole(userId, roleId)) {
            return;
        }
        UserRelationRole userRelationRole = new UserRelationRole();
        userRelationRole.setUserId(userId);
        userRelationRole.setRoleId(roleId);
        userRelationRole.setCreateTime(new Date());
        userRelationRole.setUpdateTime(new Date());
        userRelationRole.setOperatorId(operatorId);
        userRelationRole.setIsDelete(false);
        userRelationRoleMapper.insert(userRelationRole);
    }

    /**
     * 根据用户ID和角色ID删除绑定关系
     *
     * @param userId
     * @param roleId
     * @param operatorId
     * @throws ChameleonException
     */
    @Override
    public void deleteUserRelationRole(Long userId, Long roleId, Long operatorId) throws ChameleonException {
        UserRelationRole userRelationRole = new UserRelationRole();
        userRelationRole.setUserId(userId);
        userRelationRole.setRoleId(roleId);
        userRelationRole.setIsDelete(true);
        userRelationRole.setUpdateTime(new Date());
        userRelationRole.setOperatorId(operatorId);

        UserRelationRoleExample example = new UserRelationRoleExample();
        UserRelationRoleExample.Criteria criteria = example.createCriteria();
        criteria.andUserIdEqualTo(userId);
        criteria.andRoleIdEqualTo(roleId);

        userRelationRoleMapper.updateByExampleSelective(userRelationRole, example);
    }

    /**
     * 根据用户ID删除角色的绑定关系
     *
     * @param userId
     * @param operatorId
     * @throws ChameleonException
     */
    @Override
    public void deleteUserRelationRoleByUserId(Long userId, Long operatorId) throws ChameleonException {
        UserRelationRole userRelationRole = new UserRelationRole();
        userRelationRole.setUserId(userId);
        userRelationRole.setIsDelete(true);
        userRelationRole.setUpdateTime(new Date());
        userRelationRole.setOperatorId(operatorId);

        UserRelationRoleExample example = new UserRelationRoleExample();
        UserRelationRoleExample.Criteria criteria = example.createCriteria();
        criteria.andUserIdEqualTo(userId);

        userRelationRoleMapper.updateByExampleSelective(userRelationRole, example);
    }

    /**
     * 根据角色ID删除用户的绑定关系
     *
     * @param roleId
     * @param operatorId
     * @throws ChameleonException
     */
    @Override
    public void deleteUserRelationRoleByRoleId(Long roleId, Long operatorId) throws ChameleonException {
        UserRelationRole userRelationRole = new UserRelationRole();
        userRelationRole.setRoleId(roleId);
        userRelationRole.setIsDelete(true);
        userRelationRole.setUpdateTime(new Date());
        userRelationRole.setOperatorId(operatorId);

        UserRelationRoleExample example = new UserRelationRoleExample();
        UserRelationRoleExample.Criteria criteria = example.createCriteria();
        criteria.andRoleIdEqualTo(roleId);

        userRelationRoleMapper.updateByExampleSelective(userRelationRole, example);
    }

    /**
     * 根据用户ID集合查询用户集合
     *
     * @param userIds
     * @return
     * @throws ChameleonException
     */
    @Override
    public List<User> queryUsersByIds(Set<Long> userIds) throws ChameleonException {
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<User> users = Lists.newArrayListWithCapacity(userIds.size());
        for (Long userId : userIds) {
            if (userId == null || userId <= 0) {
                continue;
            }
            User user = this.queryUserById(userId);
            if (user == null) {
                continue;
            }
            users.add(user);
        }
        return users;
    }

    /**
     * 根据角色ID查询绑定的用户数
     *
     * @param roleId
     * @return
     * @throws ChameleonException
     */
    @Override
    public int countUsersByRoleId(Long roleId) throws ChameleonException {
        UserRelationRoleExample example = new UserRelationRoleExample();
        UserRelationRoleExample.Criteria criteria = example.createCriteria();
        criteria.andRoleIdEqualTo(roleId);
        criteria.andIsDeleteEqualTo(false);
        return userRelationRoleMapper.countByExample(example);
    }

    /**
     * 根据角色ID查询用户列表
     *
     * @param roleId
     * @param page
     * @param size
     * @return
     * @throws ChameleonException
     */
    @Override
    public PageData<User> pageUsersByRoleId(Long roleId, Integer page, Integer size) throws ChameleonException {
        if (page == null || page <= 0) {
            page = 1;
        }
        if (size == null || size <= 0) {
            size = 20;
        }
        PageHelper.startPage(page, size);
        UserRelationRoleExample example = new UserRelationRoleExample();
        UserRelationRoleExample.Criteria criteria = example.createCriteria();
        criteria.andRoleIdEqualTo(roleId);
        criteria.andIsDeleteEqualTo(false);
        example.setOrderByClause("create_time desc");
        List<UserRelationRole> userRelationRoles = userRelationRoleMapper.selectByExample(example);
        if (userRelationRoles == null || userRelationRoles.isEmpty()) {
            return new PageData<>(0, new ArrayList<>());
        }
        PageInfo<UserRelationRole> pageInfo = new PageInfo<>(userRelationRoles);

        Set<Long> userIds = userRelationRoles.stream().map(userRelationRole -> userRelationRole.getUserId()).collect(Collectors.toSet());

        List<User> users = this.queryUsersByIds(userIds);
        return new PageData<>(pageInfo.getTotal(), users);
    }

    /**
     * 根据用户ID查询角色集合
     *
     * @param userId
     * @param page
     * @param size
     * @return
     * @throws ChameleonException
     */
    @Override
    public PageData<Role> pageRolesByUserId(Long userId, Integer page, Integer size) throws ChameleonException {
        if (page == null || page <= 0) {
            page = 1;
        }
        if (size == null || size <= 0) {
            size = 20;
        }
        PageHelper.startPage(page, size);
        UserRelationRoleExample example = new UserRelationRoleExample();
        UserRelationRoleExample.Criteria criteria = example.createCriteria();
        criteria.andUserIdEqualTo(userId);
        criteria.andIsDeleteEqualTo(false);
        example.setOrderByClause("create_time desc");
        List<UserRelationRole> userRelationRoles = userRelationRoleMapper.selectByExample(example);
        if (userRelationRoles == null || userRelationRoles.isEmpty()) {
            return new PageData<>(0, new ArrayList<>());
        }
        PageInfo<UserRelationRole> pageInfo = new PageInfo<>(userRelationRoles);

        Set<Long> roleIds = userRelationRoles.stream().map(userRelationRole -> userRelationRole.getRoleId()).collect(Collectors.toSet());

        List<Role> roles = roleService.queryRolesByIds(roleIds);
        return new PageData<>(pageInfo.getTotal(), roles);
    }

    /**
     * 根据用户ID查询绑定的角色ID集合
     *
     * @param userId
     * @return
     * @throws ChameleonException
     */
    @Override
    public Set<Long> queryRoleIdsByUserId(Long userId) throws ChameleonException {
        UserRelationRoleExample example = new UserRelationRoleExample();
        UserRelationRoleExample.Criteria criteria = example.createCriteria();
        criteria.andIsDeleteEqualTo(false);
        criteria.andUserIdEqualTo(userId);
        List<UserRelationRole> userRelationRoles = userRelationRoleMapper.selectByExample(example);
        if (userRelationRoles == null || userRelationRoles.isEmpty()) {
            return new HashSet<>();
        }
        Set<Long> roleIds = userRelationRoles.stream().map(userRelationRole -> userRelationRole.getRoleId()).collect(Collectors.toSet());
        return roleIds;
    }

    /**
     * 根据用户ID查询绑定的功能列表
     *
     * @param userId
     * @return
     * @throws ChameleonException
     */
    @Override
    public List<Function> queryFunctionsByUserId(Long userId) throws ChameleonException {
        Set<Long> roleId = this.queryRoleIdsByUserId(userId);
        if (roleId == null || roleId.isEmpty()) {
            return new ArrayList<>();
        }
        List<Function> functions = roleService.queryFunctionsByRoleIds(roleId);
        return functions;
    }

    /**
     * 根据用户ID查询功能码集合
     *
     * @param userId
     * @return
     * @throws ChameleonException
     */
    @Override
    public List<String> queryFunctionCodesByUserId(Long userId) throws ChameleonException {
        List<Function> functions = this.queryFunctionsByUserId(userId);
        if (functions == null || functions.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> functionCodes = functions.stream().map(function -> function.getCode()).collect(Collectors.toList());
        return functionCodes;
    }
}
