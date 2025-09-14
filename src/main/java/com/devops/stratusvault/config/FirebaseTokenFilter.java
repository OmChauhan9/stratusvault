package com.devops.stratusvault.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * This filter intercepts every request, checks for a Firebase ID token in the
 * Authorization header, and if valid, sets the user's authentication context.
 */
public class FirebaseTokenFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        // If the Authorization header is missing or doesn't start with "Bearer ",
        // we continue the filter chain. Spring Security will then deny access.
        if(header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String idToken = header.substring(7); // Remove "Bearer " prefix

        try {
            // Use the Firebase Admin SDK to verify the token. This will throw an exception if invalid.
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decodedToken.getUid();

            // If the token is valid, create an authentication object and set it in the Spring Security context.
            // This tells Spring that the user is authenticated.
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken
                    (uid, null, new ArrayList<>());  // We use the UID as the principal
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            // If the token is invalid, we clear the context and send an unauthorized error.
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,"Invalid Firebase Token");
            return;
        }
        // Continue the filter chain to the controller.
        filterChain.doFilter(request, response);
    }
}
