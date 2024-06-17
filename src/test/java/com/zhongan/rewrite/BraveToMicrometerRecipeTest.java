package com.zhongan.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.ChangeType;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class BraveToMicrometerRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(
            new BraveToMicrometerTracingRecipe(),
            new ChangeType("brave.Span", "io.micrometer.tracing.Span", false),
            new ChangeType("brave.Tracer.SpanInScope", "io.micrometer.tracing.Tracer.SpanInScope", false),
            new ChangeType("brave.propagation.TraceContext", "io.micrometer.tracing.TraceContext", false),
            new ChangeType("brave.Tracer", "io.micrometer.tracing.Tracer", false)
        );
    }

    @Test
    void manuallyCreateSpan() {
        rewriteRun(
            // language=java
            java(
                """
                        import brave.Span;
                        import brave.Tracer;
                        import brave.propagation.TraceContext;
                        import java.util.Map;
                        import java.util.function.Supplier;
                        import org.springframework.util.Assert;

                        public class RenewTraceHandler {

                            final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RenewTraceHandler.class);

                            private Tracer tracer;

                            public <T> void processWithNewTrace(Supplier<T> supplier) {

                                Assert.notNull(supplier, "supplier can not be null");

                                Span span = tracer.newTrace();
                                String executeTraceId = null;
                                try (brave.Tracer.SpanInScope scope = tracer.withSpanInScope(span)) {
                                    TraceContext context = tracer.currentSpan().context();
                                    executeTraceId = context.traceIdString();
                                    supplier.get();
                                } catch (Exception e) {
                                    log.warn("Fail to process batch ,executeTraceId:{}", executeTraceId, e);
                                } finally {
                                    span.finish();
                                }
                            }
                        }
                    """,
                """
                    import io.micrometer.tracing.Span;
                    import io.micrometer.tracing.TraceContext;
                    import io.micrometer.tracing.Tracer;

                    import java.util.Map;
                    import java.util.function.Supplier;
                    import org.springframework.util.Assert;
                    
                    public class RenewTraceHandler {
                    
                        final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RenewTraceHandler.class);
                    
                        private Tracer tracer;
                    
                        public <T> void processWithNewTrace(Supplier<T> supplier) {
                    
                            Assert.notNull(supplier, "supplier can not be null");
                    
                            Span span = tracer.nextSpan();
                            String executeTraceId = null;
                            try (io.micrometer.tracing.Tracer.SpanInScope scope = tracer.withSpan(span)) {
                                TraceContext context = tracer.currentSpan().context();
                                executeTraceId = tracer.currentSpan().context().traceId();
                                supplier.get();
                            } catch (Exception e) {
                                log.warn("Fail to process batch ,executeTraceId:{}", executeTraceId, e);
                            } finally {
                                span.end();
                            }
                        }
                    }
                    """
            )
        );
    }

}