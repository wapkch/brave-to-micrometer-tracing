package com.zhongan.rewrite;

import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Value;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

@Value
@EqualsAndHashCode(callSuper = false)
public class BraveToMicrometerTracingRecipe extends Recipe {

    @Override
    public String getDisplayName() {
        // language=markdown
        return "Migrate Brave API to Micrometer Tracing API.";
    }

    @Override
    public String getDescription() {
        return "Migrate Brave API to Micrometer Tracing API.";
    }

    private static MethodMatcher newTraceMethod = new MethodMatcher("brave.Tracer newTrace()");

    private static MethodMatcher withSpanInScopeMethod = new MethodMatcher("brave.Tracer withSpanInScope(..)");

    private static MethodMatcher traceIdStringMethod = new MethodMatcher("brave.propagation.TraceContext traceIdString()");

    private static MethodMatcher spanIdStringMethod = new MethodMatcher("brave.propagation.TraceContext spanIdString()");

    private static MethodMatcher parentIdStringMethod = new MethodMatcher("brave.propagation.TraceContext parentIdString()");

    private static MethodMatcher spanFinishMethod = new MethodMatcher("brave.Span finish()");

    private static MethodMatcher extraFieldPropagationGetMethod = new MethodMatcher("brave.propagation.ExtraFieldPropagation get(..)");

    private static MethodMatcher extraFieldPropagationGetAllMethod = new MethodMatcher("brave.propagation.ExtraFieldPropagation getAll()");

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                List<Expression> arguments = m.getArguments();

                if (newTraceMethod.matches(m)) {
                    m = JavaTemplate.builder("tracer.nextSpan()").build()
                        .apply(getCursor(), m.getCoordinates().replace());
                    return m;
                }
                if (withSpanInScopeMethod.matches(m)) {
                    Expression span = arguments.get(0);
                    m = JavaTemplate.builder("tracer.withSpan(#{any()})").build()
                        .apply(getCursor(), m.getCoordinates().replace(), span);
                    return m;
                }
                if (traceIdStringMethod.matches(m)) {
                    m = JavaTemplate.builder("tracer.currentSpan().context().traceId()").build()
                        .apply(getCursor(), m.getCoordinates().replace());
                    return m;
                }
                if (spanIdStringMethod.matches(m)) {
                    m = JavaTemplate.builder("tracer.currentSpan().context().spanId()").build()
                        .apply(getCursor(), m.getCoordinates().replace());
                    return m;
                }
                if (parentIdStringMethod.matches(m)) {
                    m = JavaTemplate.builder("tracer.currentSpan().context().parentId()").build()
                        .apply(getCursor(), m.getCoordinates().replace());
                    return m;
                }
                if (spanFinishMethod.matches(m)) {
                    m = JavaTemplate.builder("span.end()").build()
                        .apply(getCursor(), m.getCoordinates().replace());
                    return m;
                }
                if (extraFieldPropagationGetMethod.matches(m)) {
                    maybeRemoveImport("brave.propagation.ExtraFieldPropagation");
                    maybeAddImport("com.zatech.octopus.component.sleuth.TraceOp");
                    Expression baggageKey = arguments.get(1);
                    m = JavaTemplate.builder("TraceOp.getExtItem(#{any()})").build()
                        .apply(getCursor(), m.getCoordinates().replace(), baggageKey);
                    return m;
                }
                if (extraFieldPropagationGetAllMethod.matches(m)) {
                    maybeRemoveImport("brave.propagation.ExtraFieldPropagation");
                    maybeAddImport("com.zatech.octopus.component.sleuth.TraceOp");
                    m = JavaTemplate.builder("TraceOp.getExtItems()").build()
                        .apply(getCursor(), m.getCoordinates().replace());
                    return m;
                }

                return m;
            }
        };
    }
}
