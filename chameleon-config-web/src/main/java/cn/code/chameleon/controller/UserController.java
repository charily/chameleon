package cn.code.chameleon.controller;

import cn.code.chameleon.common.UnifiedResponse;
import cn.code.chameleon.exception.ChameleonException;
import cn.code.chameleon.pojo.User;
import cn.code.chameleon.service.UserService;
import cn.code.chameleon.utils.RequestUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * @author liumingyu
 * @create 2018-05-07 下午5:33
 */
@RestController
@RequestMapping("/api/user")
@Api(value = "用户模块", tags = "用户模块")
public class UserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    private static final String LOG_PREFIX = "[用户模块] ";

    @Autowired
    private UserService userService;

    /**
     * 分页查询用户列表
     *
     * @param keyword
     * @param page
     * @param size
     * @param orderField
     * @param orderType
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "", method = RequestMethod.GET)
    @ApiOperation(value = "分页查询用户列表(刘明宇)", notes = "分页查询用户列表", response = UnifiedResponse.class)
    public UnifiedResponse pageUsers(@RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
                                     @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                                     @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
                                     @RequestParam(value = "orderField", required = false, defaultValue = "create_time") String orderField,
                                     @RequestParam(value = "orderType", required = false, defaultValue = "desc") String orderType) throws Exception {
        LOGGER.info("{} 分页查询用户列表 keyword = {}, page = {}, size = {}", LOG_PREFIX, keyword, page, size);
        return new UnifiedResponse(userService.pageUsers(keyword, page, size, orderField, orderType));
    }

    /**
     * 根据用户ID分页查询与之绑定的角色列表
     *
     * @param id
     * @param page
     * @param size
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/{id}/roles", method = RequestMethod.GET)
    @ApiOperation(value = "根据用户ID分页查询与之绑定的角色列表(刘明宇)", notes = "根据用户ID分页查询与之绑定的角色列表", response = UnifiedResponse.class)
    public UnifiedResponse pageRolesByUserId(@PathVariable("id") Long id,
                                             @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                                             @RequestParam(value = "size", required = false, defaultValue = "20") Integer size) throws Exception {
        LOGGER.info("{} 根据用户ID = {} 分页查询与之绑定的角色列表", LOG_PREFIX, id);
        return new UnifiedResponse(userService.pageRolesByUserId(id, page, size));
    }

    /**
     * 验证账号有效性 true:可用; false:不可用
     *
     * @param email
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/account", method = RequestMethod.GET)
    @ApiOperation(value = "验证账号有效性(刘明宇)", notes = "验证账号有效性", response = UnifiedResponse.class)
    public UnifiedResponse checkAccount(@RequestParam("email") String email) throws Exception {
        LOGGER.info("{} 验证账号 {} 有效性", LOG_PREFIX, email);
        return new UnifiedResponse(userService.checkAccount(email));
    }

    /**
     * 检查当前账号的旧密码是否匹配
     *
     * @param password
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/password", method = RequestMethod.GET)
    @ApiOperation(value = "检查当前账号的旧密码是否匹配(刘明宇)", notes = "检查当前账号的旧密码是否匹配", response = UnifiedResponse.class)
    public UnifiedResponse checkAccountAndPassword(@RequestParam("password") String password) throws Exception {
        String account = RequestUtil.getCurrentUserEmail();
        LOGGER.info("{} 检查当前账号 {} 的旧密码 {} 是否匹配", LOG_PREFIX, account, password);
        return new UnifiedResponse(userService.checkAccountAndPassword(account, password));
    }

    /**
     * 创建用户
     *
     * @param user
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "", method = RequestMethod.POST)
    @ApiOperation(value = "创建用户(刘明宇)", notes = "创建用户", response = UnifiedResponse.class)
    public UnifiedResponse saveUser(@RequestBody User user, HttpServletRequest request) throws Exception {
        Long operatorId = RequestUtil.getCurrentUserId();
        LOGGER.info("{} operatorId = {} 创建用户 user = {}", LOG_PREFIX, operatorId, user);
        try {
            userService.saveUser(user);
        } catch (ChameleonException e) {
            LOGGER.error("{} operatorId = {} 创建用户 user = {} 失败", LOG_PREFIX, operatorId, user);
            return new UnifiedResponse(e.getCode(), e.getMessage());
        }
        return new UnifiedResponse();
    }

    /**
     * 更新用户
     *
     * @param user
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "", method = RequestMethod.PUT)
    @ApiOperation(value = "更新用户(刘明宇)", notes = "更新用户", response = UnifiedResponse.class)
    public UnifiedResponse updateUser(@RequestBody User user, HttpServletRequest request) throws Exception {
        Long operatorId = RequestUtil.getCurrentUserId();
        LOGGER.info("{} operatorId = {} 更新用户 user = {}", LOG_PREFIX, operatorId, user);
        try {
            userService.updateUser(user);
        } catch (ChameleonException e) {
            LOGGER.error("{} operatorId = {} 更新用户 user = {} 失败", LOG_PREFIX, operatorId, user, e);
            return new UnifiedResponse(e.getCode(), e.getMessage());
        }
        return new UnifiedResponse();
    }

    /**
     * 当前用户修改密码
     *
     * @param oldPassword
     * @param newPassword
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/password", method = RequestMethod.PUT)
    @ApiOperation(value = "当前用户修改密码(刘明宇)", notes = "当前用户修改密码", response = UnifiedResponse.class)
    public UnifiedResponse updatePassword(@RequestParam("oldPassword") String oldPassword,
                                          @RequestParam("newPassword") String newPassword,
                                          HttpServletRequest request) throws Exception {
        User user = RequestUtil.getCurrentUser();
        LOGGER.info("{} 当前用户 {} 修改密码", LOG_PREFIX, user.getEmail());
        try {
            userService.updatePassword(user, oldPassword, newPassword);
        } catch (ChameleonException e) {
            LOGGER.error("{} 当前用户 {} 修改密码失败", LOG_PREFIX, user.getEmail(), e);
            return new UnifiedResponse(e.getCode(), e.getMessage());
        }
        return new UnifiedResponse();
    }

    /**
     * 启用或禁用用户
     *
     * @param id
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/{id}/status", method = RequestMethod.PUT)
    @ApiOperation(value = "启用或禁用用户(刘明宇)", notes = "启用或禁用用户", response = UnifiedResponse.class)
    public UnifiedResponse updateUserEnableStatus(@PathVariable("id") Long id, HttpServletRequest request) throws Exception {
        Long operatorId = RequestUtil.getCurrentUserId();
        LOGGER.info("{} operatorId = {} 启用／禁用 用户 id = {}", LOG_PREFIX, operatorId, id);
        try {
            userService.updateUserEnableStatus(id, operatorId);
        } catch (ChameleonException e) {
            LOGGER.error("{} 启用／禁用用户失败 id = {}", LOG_PREFIX, id);
            return new UnifiedResponse(e.getCode(), e.getMessage());
        }
        return new UnifiedResponse();
    }

    /**
     * 删除用户
     *
     * @param id
     * @param request
     * @return
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @ApiOperation(value = "删除用户(刘明宇)", notes = "删除用户", response = UnifiedResponse.class)
    public UnifiedResponse deleteUserById(@PathVariable("id") Long id, HttpServletRequest request) throws Exception {
        Long operatorId = RequestUtil.getCurrentUserId();
        LOGGER.info("{} operatorId = {} 删除用户id = {}", LOG_PREFIX, operatorId, id);
        try {
            userService.deleteUserById(id, operatorId);
        } catch (ChameleonException e) {
            LOGGER.error("{} operatorId = {} 删除用户 id = {} 失败", LOG_PREFIX, operatorId, id, e);
            return new UnifiedResponse(e.getCode(), e.getMessage());
        }
        return new UnifiedResponse();
    }

    /**
     * 根据用户ID查询用户信息
     *
     * @param id
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "根据用户ID查询用户信息(刘明宇)", notes = "根据用户ID查询用户信息", response = UnifiedResponse.class)
    public UnifiedResponse queryUserById(@PathVariable("id") Long id) throws Exception {
        LOGGER.info("{} 根据用户ID = {} 查询用户信息", LOG_PREFIX, id);
        return new UnifiedResponse(userService.queryUserById(id));
    }

    /**
     * 用户注销
     *
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/logout", method = RequestMethod.PUT)
    @ApiOperation(value = "用户注销(刘明宇)", notes = "用户注销", response = UnifiedResponse.class)
    public UnifiedResponse logout(HttpServletRequest request) throws Exception {
        LOGGER.info("{} 用户注销");
        try {
            userService.logout(request);
        } catch (ChameleonException e) {
            LOGGER.error("{} 用户注销失败", LOG_PREFIX, e);
            return new UnifiedResponse(e.getCode(), e.getMessage());
        }
        return new UnifiedResponse();
    }

    /**
     * 根据用户ID删除与之绑定的角色
     *
     * @param id
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/{id}/roles", method = RequestMethod.DELETE)
    @ApiOperation(value = "根据用户ID删除与之绑定的角色(刘明宇)", notes = "根据用户ID删除与之绑定的角色", response = UnifiedResponse.class)
    public UnifiedResponse deleteRolesByUserId(@PathVariable("id") Long id, HttpServletRequest request) throws Exception {
        Long operatorId = RequestUtil.getCurrentUserId();
        LOGGER.info("{} 根据用户ID = {} 删除与之绑定的角色", LOG_PREFIX, id);
        try {
            userService.deleteUserRelationRoleByUserId(id, operatorId);
        } catch (ChameleonException e) {
            LOGGER.error("{} 根据用户ID = {} 删除与之绑定的角色失败", LOG_PREFIX, id, e);
            return new UnifiedResponse(e.getCode(), e.getMessage());
        }
        return new UnifiedResponse();
    }

    /**
     * 根据用户ID查询功能列表
     *
     * @param id
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/{id}/functions", method = RequestMethod.GET)
    @ApiOperation(value = "根据用户ID查询功能列表(刘明宇)", notes = "根据用户ID查询功能列表", response = UnifiedResponse.class)
    public UnifiedResponse queryFunctionsByUserId(@PathVariable("id") Long id) throws Exception {
        LOGGER.info("{} 根据用户ID = {} 查询功能列表", LOG_PREFIX, id);
        return new UnifiedResponse(userService.queryFunctionsByUserId(id));
    }

    /**
     * 根据用户ID查询功能码列表
     *
     * @param id
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/{id}/function/codes", method = RequestMethod.GET)
    @ApiOperation(value = "根据用户ID查询功能码列表(刘明宇)", notes = "根据用户ID查询功能码列表", response = UnifiedResponse.class)
    public UnifiedResponse queryFunctionCodesByUserId(@PathVariable("id") Long id) throws Exception {
        LOGGER.info("{} 根据用户ID = {} 查询功能码列表", LOG_PREFIX, id);
        return new UnifiedResponse(userService.queryFunctionCodesByUserId(id));
    }
}
