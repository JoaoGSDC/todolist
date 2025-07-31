package br.com.joaogsdc.todolist.filter;

import java.io.IOException;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import at.favre.lib.crypto.bcrypt.BCrypt;
import br.com.joaogsdc.todolist.user.IUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class FilterTaskAuth extends OncePerRequestFilter {

    @Autowired
    private IUserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        var servletPath = request.getServletPath();
        var prefix = "/tasks/";

        if (!servletPath.startsWith(prefix)) {
            filterChain.doFilter(request, response);
            return;
        }

        var credentials = getAuthenticationCredentials(request);

        String username = credentials[0];
        String password = credentials[1];

        var user = this.userRepository.findByUsername(username);
        if (user == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        var passwordHash = user.getPassword();

        boolean passwordVerified = passwordVerify(password, passwordHash);

        if (!passwordVerified) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        var idUser = user.getId();

        request.setAttribute("idUser", idUser);
        filterChain.doFilter(request, response);
    }

    private String[] getAuthenticationCredentials(HttpServletRequest request) {
        var authorization = request.getHeader("Authorization");

        var authEncoded = authorization.substring("Basic".length()).trim();

        byte[] authDecode = Base64.getDecoder().decode(authEncoded);

        var authString = new String(authDecode);

        String[] credentials = authString.split(":");

        return credentials;
    }

    private boolean passwordVerify(String password, String hash) {
        var passwordHash = BCrypt.verifyer().verify(password.toCharArray(), hash);
        return passwordHash.verified;
    }
}