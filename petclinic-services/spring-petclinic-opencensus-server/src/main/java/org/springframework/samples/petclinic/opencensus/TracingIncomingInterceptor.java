package org.springframework.samples.petclinic.opencensus;

import java.util.ArrayDeque;
import java.util.Deque;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.tuple.Triple;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import io.opencensus.common.Scope;

@Component
public class TracingIncomingInterceptor extends HandlerInterceptorAdapter {

    private final static String SCOPE_STACK = TracingIncomingInterceptor.class.getName() + ".scopeStack";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Scope tagContextScope = OpenCensusService.getInstance().createTagContextFromIncomingRequest(request);
        Scope tracingScope = OpenCensusService.getInstance().createSpanFromIncomingRequest(request);

        Triple<Scope,Long,Scope> scopeTriple = Triple.of(tagContextScope,System.currentTimeMillis(), tracingScope);
        Deque<Triple<Scope,Long,Scope>> stack = getScopeStack(request);
        stack.push(scopeTriple);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        super.afterCompletion(request, response, handler, ex);
        Deque<Triple<Scope,Long,Scope>> stack = getScopeStack(request);
        if(!stack.isEmpty()){
            Triple<Scope,Long,Scope> scopeTriple = stack.pop();
            long duration = System.currentTimeMillis() - scopeTriple.getMiddle();
            OpenCensusService.getInstance().writeMetric(new Double(duration));
            Scope tagContextScope = scopeTriple.getLeft();
            Scope tracingScope = scopeTriple.getRight();

            tracingScope.close();
            tagContextScope.close();
        }
    }

    private Deque<Triple<Scope,Long,Scope>> getScopeStack(HttpServletRequest request) {
        Deque<Triple<Scope,Long,Scope>> stack = (Deque<Triple<Scope,Long,Scope>>) request.getAttribute(SCOPE_STACK);
        if (stack == null) {
            stack = new ArrayDeque<>();
            request.setAttribute(SCOPE_STACK, stack);
        }
        return stack;
    }

}
