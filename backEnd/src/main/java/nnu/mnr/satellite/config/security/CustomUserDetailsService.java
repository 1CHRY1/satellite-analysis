//package nnu.mnr.satellite.config.security;
//
//import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
//import lombok.extern.slf4j.Slf4j;
//import nnu.mnr.satellite.model.po.common.User;
//import nnu.mnr.satellite.repository.modeling.IUserRepo;
//import org.jvnet.hk2.annotations.Service;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Component;
//
//import java.util.Collections;
//
///**
// * Created with IntelliJ IDEA.
// *
// * @Author: Chry
// * @Date: 2025/3/28 17:00
// * @Description:
// */
//
//@Component
//@Slf4j
//public class CustomUserDetailsService implements UserDetailsService {
//
//    private final IUserRepo userRepo;
//
//    public CustomUserDetailsService(IUserRepo userRepo) {
//        this.userRepo = userRepo;
//    }
//
//    @Override
//    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
//        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("user_name", userName);
//        User user = userRepo.selectOne(queryWrapper);
//        if (user == null) {
//            throw new UsernameNotFoundException("User not found: " + userName);
//        }
//        return new org.springframework.security.core.userdetails.User(
//                user.getUserName(),
//                user.getPassword(),
//                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
//        );
//    }
//}