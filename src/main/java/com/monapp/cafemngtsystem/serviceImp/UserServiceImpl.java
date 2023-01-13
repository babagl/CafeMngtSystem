package com.monapp.cafemngtsystem.serviceImp;

import com.monapp.cafemngtsystem.JWT.CostumerUserDetailsService;
import com.monapp.cafemngtsystem.JWT.JwtFilter;
import com.monapp.cafemngtsystem.JWT.JwtUtils;
import com.monapp.cafemngtsystem.POJO.User;
import com.monapp.cafemngtsystem.constents.CafeConstants;
import com.monapp.cafemngtsystem.dao.UserDao;
import com.monapp.cafemngtsystem.service.UserService;
import com.monapp.cafemngtsystem.utils.CafeUtils;
import com.monapp.cafemngtsystem.wrapper.UserWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserDao userDao;
    @Autowired
    AuthenticationManager authenticationManager;
    @Autowired
    CostumerUserDetailsService costumerUserDetailsService;
    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    JwtFilter jwtFilter;
    @Override
    public ResponseEntity<String> signUp(Map<String, String> requestMap) {
        try {
            log.info("inside signup{}", requestMap);
            if (validateSignUpMap(requestMap)) {
                User user = userDao.findByEmailId(requestMap.get("email"));
                if (Objects.isNull(user)) {
                    userDao.save(getUserFromMap(requestMap));
                    return CafeUtils.getResponseEntity("Sign up Successfull", HttpStatus.OK);
                } else {
                    return CafeUtils.getResponseEntity("email already exist. ", HttpStatus.BAD_REQUEST);
                }
            } else {
                return CafeUtils.getResponseEntity(CafeConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG,HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> login(Map<String, String> requestMap) {
        log.info("inside login");
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(requestMap.get("email"),requestMap.get("password"))
            );
            if (auth.isAuthenticated()){
                if (costumerUserDetailsService.getUserDetail().getStatus().equalsIgnoreCase("true")){
                    return new ResponseEntity<String>("{\"token\":\""+
                            jwtUtils.generatedToken(costumerUserDetailsService.getUserDetail().getEmail(),
                                    costumerUserDetailsService.getUserDetail().getRole()) +"\"}" ,HttpStatus.OK);
                }else {
                    return new ResponseEntity<String>("{\"message\":"+"wait the admin approuval"+"\"}",
                            HttpStatus.BAD_REQUEST);
                }
            }
        }catch (Exception ex){
            log.error("{}",ex);
        }
        return new ResponseEntity<String>("{\"message\":"+"Bad Credentiel"+"\"}",
                HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<List<UserWrapper>> getAllUser() {
        try {
            if (jwtFilter.isAdmin()){
                return new ResponseEntity<>(userDao.getAllUser(),HttpStatus.OK);
            }else {
                return new ResponseEntity<>(new ArrayList<>(),HttpStatus.UNAUTHORIZED);
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return new ResponseEntity<>(new ArrayList<>(),HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> update(Map<String, String> requestMap) {
        try {
            if (jwtFilter.isAdmin()){
                Optional<User> optionalUser = userDao.findById(Integer.parseInt(requestMap.get("id")));
                if (!optionalUser.isEmpty()){
                    userDao.updateStatus();
                }else {
                    CafeUtils.getResponseEntity("User id doesn't not exist",HttpStatus.OK);
                }
            }else {
             return CafeUtils.getResponseEntity(CafeConstants.UNAUTHORIZED_ACCESS,HttpStatus.UNAUTHORIZED);
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG,HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private boolean validateSignUpMap(Map<String, String> requestMap){
        if (requestMap.containsKey("name")
                && requestMap.containsKey("contactNumber")
                && requestMap.containsKey("email")
                && requestMap.containsKey("password")){
            return true;
        }else {
            return false;
        }

    }

    private User getUserFromMap(Map<String,String> requestMap){
        User user = new User();
        user.setName(requestMap.get("name"));
        user.setEmail(requestMap.get("email"));
        user.setPassword(requestMap.get("password"));
        user.setContactNumber(requestMap.get("contactNumber"));
        user.setStatus("false");
        user.setRole("user");
        return user;
    }
}
